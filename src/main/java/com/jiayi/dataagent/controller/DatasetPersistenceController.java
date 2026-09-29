package com.jiayi.dataagent.controller;

import com.jiayi.dataagent.service.DatasetPersistenceService;

import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/datasets")
@Profile("mysql")
public class DatasetPersistenceController {

    private final DatasetPersistenceService datasetPersistenceService;

    public DatasetPersistenceController(DatasetPersistenceService datasetPersistenceService) {
        this.datasetPersistenceService = datasetPersistenceService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(datasetPersistenceService.create(file));
    }

    @GetMapping("/{id}")
    public Map<String, Object> findById(@PathVariable long id) {
        return datasetPersistenceService.findById(id);
    }

    @GetMapping("/{id}/preview")
    public Map<String, Object> preview(
            @PathVariable long id,
            @RequestParam(defaultValue = "10") int limit) {
        return datasetPersistenceService.preview(id, limit);
    }
}
