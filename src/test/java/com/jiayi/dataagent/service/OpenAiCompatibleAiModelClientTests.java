package com.jiayi.dataagent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.jiayi.dataagent.config.AiModelSettings;
import com.jiayi.dataagent.exception.DatasetException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiCompatibleAiModelClientTests {

    @Test
    void sendsPromptAndParsesModelReport() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiCompatibleAiModelClient client = new OpenAiCompatibleAiModelClient(
                builder, new AiModelSettings("test-secret", "analysis-model", "https://ai.example/v1/"));

        server.expect(requestTo("https://ai.example/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-secret"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("analysis-model")))
                .andExpect(content().string(containsString("quality and profile prompt")))
                .andRespond(withSuccess(
                        """
                        {"choices":[{"message":{"content":"## 数据整体概况\\n分析完成"}}]}
                        """,
                        MediaType.APPLICATION_JSON));

        String report = client.generateAnalysis("quality and profile prompt");

        assertThat(report).isEqualTo("## 数据整体概况\n分析完成");
        server.verify();
    }

    @Test
    void convertsProviderHttpFailureToFriendlyBadGateway() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiCompatibleAiModelClient client = new OpenAiCompatibleAiModelClient(
                builder, new AiModelSettings("private-key", "analysis-model", "https://ai.example/v1"));

        server.expect(requestTo("https://ai.example/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"rate limited\"}"));

        assertThatThrownBy(() -> client.generateAnalysis("prompt"))
                .isInstanceOf(DatasetException.class)
                .satisfies(exception -> {
                    DatasetException datasetException = (DatasetException) exception;
                    assertThat(datasetException.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(datasetException.getMessage())
                            .isEqualTo("AI 分析服务暂时不可用，请稍后重试")
                            .doesNotContain("private-key")
                            .doesNotContain("rate limited");
                });
        server.verify();
    }

    @Test
    void rejectsEmptyModelResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiCompatibleAiModelClient client = new OpenAiCompatibleAiModelClient(
                builder, new AiModelSettings("test-secret", "analysis-model", "https://ai.example/v1"));

        server.expect(requestTo("https://ai.example/v1/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generateAnalysis("prompt"))
                .isInstanceOf(DatasetException.class)
                .satisfies(exception -> {
                    DatasetException datasetException = (DatasetException) exception;
                    assertThat(datasetException.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(datasetException.getMessage())
                            .isEqualTo("AI 分析服务返回了空结果，请稍后重试");
                });
        server.verify();
    }
}
