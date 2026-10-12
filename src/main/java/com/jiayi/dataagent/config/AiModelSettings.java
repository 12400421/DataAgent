package com.jiayi.dataagent.config;

public final class AiModelSettings {

    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";

    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public AiModelSettings(String apiKey, String model, String baseUrl) {
        this.apiKey = normalize(apiKey);
        this.model = normalize(model);
        String configuredBaseUrl = normalize(baseUrl);
        this.baseUrl = removeTrailingSlash(
                configuredBaseUrl.isEmpty() ? DEFAULT_BASE_URL : configuredBaseUrl);
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty() && !model.isEmpty();
    }

    public String apiKey() {
        return apiKey;
    }

    public String model() {
        return model;
    }

    public String chatCompletionsEndpoint() {
        return baseUrl + "/chat/completions";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String removeTrailingSlash(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }

    @Override
    public String toString() {
        return "AiModelSettings[model=" + model + ", baseUrl=" + baseUrl
                + ", apiKeyConfigured=" + !apiKey.isEmpty() + "]";
    }
}
