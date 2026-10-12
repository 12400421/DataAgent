package com.jiayi.dataagent.service;

import com.jiayi.dataagent.config.AiModelSettings;
import com.jiayi.dataagent.exception.DatasetException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

@Service
@Profile("mysql")
public class OpenAiCompatibleAiModelClient implements AiModelClient {

    private static final String SYSTEM_MESSAGE = """
            你是 DataAgent 的数据分析助手。请仅根据用户提供的结构化数据集统计信息进行分析，
            不要臆测未提供的原始数据、业务背景或因果关系。输出应清晰、具体并使用中文。
            """;

    private final RestClient restClient;
    private final AiModelSettings settings;

    public OpenAiCompatibleAiModelClient(
            @Qualifier("aiRestClientBuilder")
            RestClient.Builder restClientBuilder,
            AiModelSettings settings) {
        this.restClient = restClientBuilder.build();
        this.settings = settings;
    }

    @Override
    public boolean isConfigured() {
        return settings.isConfigured();
    }

    @Override
    public String generateAnalysis(String prompt) {
        if (!isConfigured()) {
            throw new IllegalStateException("AI model is not configured");
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", settings.model());
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_MESSAGE),
                Map.of("role", "user", "content", prompt)));
        requestBody.put("temperature", 0.2);

        try {
            JsonNode response = restClient.post()
                    .uri(settings.chatCompletionsEndpoint())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + settings.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            String report = response == null
                    ? ""
                    : response.path("choices")
                            .path(0)
                            .path("message")
                            .path("content")
                            .asString("")
                            .trim();
            if (report.isEmpty()) {
                throw new DatasetException(
                        HttpStatus.BAD_GATEWAY, "AI 分析服务返回了空结果，请稍后重试");
            }
            return report;
        } catch (DatasetException exception) {
            throw exception;
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new DatasetException(
                    HttpStatus.BAD_GATEWAY, "AI 分析服务暂时不可用，请稍后重试", exception);
        }
    }
}
