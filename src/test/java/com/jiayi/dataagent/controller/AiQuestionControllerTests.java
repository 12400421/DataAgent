package com.jiayi.dataagent.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jiayi.dataagent.exception.DatasetException;
import com.jiayi.dataagent.exception.GlobalExceptionHandler;
import com.jiayi.dataagent.model.AiQuestionResponse;
import com.jiayi.dataagent.service.DatasetAiQuestionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AiQuestionControllerTests {

    @Mock
    private DatasetAiQuestionService datasetAiQuestionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AiQuestionController(datasetAiQuestionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsAiAnswer() throws Exception {
        when(datasetAiQuestionService.ask(7L, "哪个字段缺失最多？"))
                .thenReturn(AiQuestionResponse.completed(
                        7L, "哪个字段缺失最多？", "amount 字段缺失最多。"));

        mockMvc.perform(post("/api/datasets/7/ai-question")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"哪个字段缺失最多？\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasetId").value(7))
                .andExpect(jsonPath("$.question").value("哪个字段缺失最多？"))
                .andExpect(jsonPath("$.answer").value("amount 字段缺失最多。"))
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.message").value("AI 问答完成"));
    }

    @Test
    void returnsFriendlyResponseWhenModelIsNotConfigured() throws Exception {
        when(datasetAiQuestionService.ask(7L, "数据质量如何？"))
                .thenReturn(AiQuestionResponse.notConfigured(7L, "数据质量如何？"));

        mockMvc.perform(post("/api/datasets/7/ai-question")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"数据质量如何？\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(false))
                .andExpect(jsonPath("$.message").value("AI 模型尚未配置"))
                .andExpect(jsonPath("$.answer").doesNotExist());
    }

    @Test
    void returnsBadRequestForBlankQuestion() throws Exception {
        when(datasetAiQuestionService.ask(7L, "   "))
                .thenThrow(new DatasetException("问题不能为空"));

        mockMvc.perform(post("/api/datasets/7/ai-question")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("问题不能为空"))
                .andExpect(jsonPath("$.path").value("/api/datasets/7/ai-question"));
    }

    @Test
    void keepsNotFoundStatusForUnknownDataset() throws Exception {
        when(datasetAiQuestionService.ask(99L, "数据质量如何？"))
                .thenThrow(new DatasetException(HttpStatus.NOT_FOUND, "Dataset not found: 99"));

        mockMvc.perform(post("/api/datasets/99/ai-question")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"数据质量如何？\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Dataset not found: 99"))
                .andExpect(jsonPath("$.path").value("/api/datasets/99/ai-question"));
    }
}
