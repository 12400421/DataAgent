package com.jiayi.dataagent.service;

import com.jiayi.dataagent.utils.CsvUtils;
import com.jiayi.dataagent.utils.CsvUtils.CsvSummary;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DatasetService {

    public Map<String, Object> analyzeCsv(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV file must not be empty");
        }

        CsvSummary csvSummary;
        try {
            csvSummary = CsvUtils.readSummary(file.getInputStream());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("filename", file.getOriginalFilename());
        result.put("rows", csvSummary.rows());
        result.put("columns", csvSummary.columnNames().size());
        result.put("columnNames", csvSummary.columnNames());
        return result;
    }
}
