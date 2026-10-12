package com.jiayi.dataagent.controller;

import com.jiayi.dataagent.model.AiAnalysisResponse;
import com.jiayi.dataagent.service.AiAnalysisService;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/datasets")
@Profile("mysql")
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    public AiAnalysisController(AiAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    @PostMapping("/{id}/ai-analysis")
    public AiAnalysisResponse analyze(@PathVariable long id) {
        return aiAnalysisService.analyze(id);
    }
}
