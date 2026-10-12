package com.jiayi.dataagent.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jiayi.dataagent.exception.DatasetException;
import com.jiayi.dataagent.exception.GlobalExceptionHandler;
import com.jiayi.dataagent.model.AiAnalysisResponse;
import com.jiayi.dataagent.service.AiAnalysisService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AiAnalysisControllerTests {

    @Mock
    private AiAnalysisService aiAnalysisService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AiAnalysisController(aiAnalysisService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsFriendlyResponseWhenModelIsNotConfigured() throws Exception {
        when(aiAnalysisService.analyze(7L)).thenReturn(
                AiAnalysisResponse.notConfigured(7L, "sales.csv"));

        mockMvc.perform(post("/api/datasets/7/ai-analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasetId").value(7))
                .andExpect(jsonPath("$.filename").value("sales.csv"))
                .andExpect(jsonPath("$.configured").value(false))
                .andExpect(jsonPath("$.message").value("AI 模型尚未配置"))
                .andExpect(jsonPath("$.report").doesNotExist());
    }

    @Test
    void returnsCompletedAnalysisReport() throws Exception {
        when(aiAnalysisService.analyze(7L)).thenReturn(
                AiAnalysisResponse.completed(7L, "sales.csv", "## 数据整体概况\n共有 10 行数据。"));

        mockMvc.perform(post("/api/datasets/7/ai-analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasetId").value(7))
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.message").value("AI 分析完成"))
                .andExpect(jsonPath("$.report").value("## 数据整体概况\n共有 10 行数据。"));
    }

    @Test
    void keepsNotFoundStatusForUnknownDataset() throws Exception {
        when(aiAnalysisService.analyze(99L)).thenThrow(
                new DatasetException(HttpStatus.NOT_FOUND, "Dataset not found: 99"));

        mockMvc.perform(post("/api/datasets/99/ai-analysis"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Dataset not found: 99"))
                .andExpect(jsonPath("$.path").value("/api/datasets/99/ai-analysis"));
    }
}
