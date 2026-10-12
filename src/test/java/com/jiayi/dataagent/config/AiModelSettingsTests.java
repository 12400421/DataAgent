package com.jiayi.dataagent.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiModelSettingsTests {

    @Test
    void requiresBothApiKeyAndModel() {
        assertThat(new AiModelSettings(null, null, null).isConfigured()).isFalse();
        assertThat(new AiModelSettings("secret", "  ", null).isConfigured()).isFalse();
        assertThat(new AiModelSettings("secret", "analysis-model", null).isConfigured()).isTrue();
    }

    @Test
    void normalizesBaseUrlWithoutExposingApiKey() {
        AiModelSettings settings = new AiModelSettings(
                "private-secret", " analysis-model ", " https://ai.example/v1/ ");

        assertThat(settings.model()).isEqualTo("analysis-model");
        assertThat(settings.chatCompletionsEndpoint())
                .isEqualTo("https://ai.example/v1/chat/completions");
        assertThat(settings.toString())
                .contains("apiKeyConfigured=true")
                .doesNotContain("private-secret");
    }
}
