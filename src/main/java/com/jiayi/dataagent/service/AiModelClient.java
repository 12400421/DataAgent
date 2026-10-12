package com.jiayi.dataagent.service;

public interface AiModelClient {

    boolean isConfigured();

    String generateAnalysis(String prompt);
}
