package com.jiayi.dataagent.service;

import com.jiayi.dataagent.exception.DatasetException;
import com.jiayi.dataagent.utils.CsvUtils;
import com.jiayi.dataagent.utils.CsvUtils.CsvData;
import com.jiayi.dataagent.utils.CsvUtils.CsvSummary;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DatasetService {

    private static final int MAX_PREVIEW_ROWS = 100;

    public Map<String, Object> analyzeCsv(MultipartFile file) {
        validateCsvFile(file);
        try {
            CsvSummary csvSummary = CsvUtils.readSummary(file.getInputStream());
            return createMetadata(file, csvSummary.rows(), csvSummary.columnNames());
        } catch (IOException | IllegalArgumentException exception) {
            throw new DatasetException(messageOf(exception), exception);
        }
    }

    public Map<String, Object> previewCsv(MultipartFile file, int limit) {
        validateCsvFile(file);
        if (limit < 1 || limit > MAX_PREVIEW_ROWS) {
            throw new DatasetException("Preview limit must be between 1 and " + MAX_PREVIEW_ROWS);
        }

        try {
            CsvData csvData = CsvUtils.read(file.getInputStream(), limit);
            Map<String, Object> result = createMetadata(file, csvData.rows(), csvData.columnNames());
            result.put("preview", csvData.preview());
            return result;
        } catch (IOException | IllegalArgumentException exception) {
            throw new DatasetException(messageOf(exception), exception);
        }
    }

    private void validateCsvFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new DatasetException("CSV file must not be empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new DatasetException("Only CSV files are supported");
        }
    }

    private Map<String, Object> createMetadata(
            MultipartFile file, long rows, List<String> columnNames) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("filename", file.getOriginalFilename());
        result.put("rows", rows);
        result.put("columns", columnNames.size());
        result.put("columnNames", columnNames);
        return result;
    }

    private String messageOf(Exception exception) {
        return exception.getMessage() == null ? "Failed to read CSV file" : exception.getMessage();
    }
}
