package com.jiayi.dataagent.controller;

import com.jiayi.dataagent.model.AiQuestionRequest;
import com.jiayi.dataagent.model.AiQuestionResponse;
import com.jiayi.dataagent.service.DatasetAiQuestionService;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/datasets")
@Profile("mysql")
public class AiQuestionController {

    private final DatasetAiQuestionService datasetAiQuestionService;

    public AiQuestionController(DatasetAiQuestionService datasetAiQuestionService) {
        this.datasetAiQuestionService = datasetAiQuestionService;
    }

    @PostMapping("/{id}/ai-question")
    public AiQuestionResponse ask(
            @PathVariable long id,
            @RequestBody AiQuestionRequest request) {
        return datasetAiQuestionService.ask(id, request.question());
    }
}
