package com.jiayi.dataagent.service;

import com.jiayi.dataagent.exception.DatasetException;
import com.jiayi.dataagent.mapper.DatasetMapper;
import com.jiayi.dataagent.model.DatasetMetadata;
import com.jiayi.dataagent.service.DatasetFileStorage.StoredFile;
import com.jiayi.dataagent.utils.CsvUtils;
import com.jiayi.dataagent.utils.CsvUtils.CsvData;
import com.jiayi.dataagent.utils.CsvUtils.CsvSummary;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Profile("mysql")
public class DatasetPersistenceService {

    private static final int MAX_PREVIEW_ROWS = 100;

    private final DatasetMapper datasetMapper;
    private final DatasetService datasetService;
    private final DatasetFileStorage fileStorage;

    public DatasetPersistenceService(
            DatasetMapper datasetMapper,
            DatasetService datasetService,
            DatasetFileStorage fileStorage) {
        this.datasetMapper = datasetMapper;
        this.datasetService = datasetService;
        this.fileStorage = fileStorage;
    }

    @Transactional
    public Map<String, Object> create(MultipartFile file) {
        CsvSummary summary = datasetService.summarizeCsv(file);
        StoredFile storedFile = fileStorage.store(file);

        try {
            DatasetMetadata dataset = new DatasetMetadata();
            dataset.setOriginalFilename(file.getOriginalFilename());
            dataset.setStoredFilename(storedFile.storedFilename());
            dataset.setFilePath(storedFile.filePath());
            dataset.setFileSize(file.getSize());
            dataset.setRowCount(summary.rows());
            dataset.setColumnCount(summary.columnNames().size());
            dataset.setStatus("READY");
            datasetMapper.insert(dataset);

            for (int position = 0; position < summary.columnNames().size(); position++) {
                datasetMapper.insertColumn(dataset.getId(), position, summary.columnNames().get(position));
            }
            return findById(dataset.getId());
        } catch (RuntimeException exception) {
            fileStorage.delete(storedFile.filePath());
            throw exception;
        }
    }

    public Map<String, Object> findById(long id) {
        DatasetMetadata dataset = requireDataset(id);
        dataset.setColumnNames(datasetMapper.findColumnNames(id));
        return toResponse(dataset);
    }

    public Map<String, Object> preview(long id, int limit) {
        if (limit < 1 || limit > MAX_PREVIEW_ROWS) {
            throw new DatasetException("Preview limit must be between 1 and " + MAX_PREVIEW_ROWS);
        }

        DatasetMetadata dataset = requireDataset(id);
        try (InputStream inputStream = fileStorage.open(dataset.getFilePath())) {
            CsvData csvData = CsvUtils.read(inputStream, limit);
            Map<String, Object> response = toResponse(dataset);
            response.put("columnNames", csvData.columnNames());
            response.put("preview", csvData.preview());
            return response;
        } catch (IOException | IllegalArgumentException exception) {
            throw new DatasetException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read stored dataset", exception);
        }
    }

    private DatasetMetadata requireDataset(long id) {
        DatasetMetadata dataset = datasetMapper.findById(id);
        if (dataset == null) {
            throw new DatasetException(HttpStatus.NOT_FOUND, "Dataset not found: " + id);
        }
        return dataset;
    }

    private Map<String, Object> toResponse(DatasetMetadata dataset) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", dataset.getId());
        response.put("filename", dataset.getOriginalFilename());
        response.put("fileSize", dataset.getFileSize());
        response.put("rows", dataset.getRowCount());
        response.put("columns", dataset.getColumnCount());
        if (dataset.getColumnNames() != null) {
            response.put("columnNames", dataset.getColumnNames());
        }
        response.put("status", dataset.getStatus());
        response.put("createdAt", dataset.getCreatedAt());
        response.put("updatedAt", dataset.getUpdatedAt());
        return response;
    }
}
