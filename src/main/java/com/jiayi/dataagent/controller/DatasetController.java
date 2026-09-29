package com.jiayi.dataagent.controller;

import com.jiayi.dataagent.service.DatasetService;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/dataset")
public class DatasetController {

    private final DatasetService datasetService;

    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
        return datasetService.analyzeCsv(file);
    }

    @PostMapping("/preview")
    public Map<String, Object> preview(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "10") int limit) {
        return datasetService.previewCsv(file, limit);
    }
}
