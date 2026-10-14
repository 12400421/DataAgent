package com.jiayi.dataagent.model;

public record AiQuestionResponse(
        Long datasetId,
        String question,
        String answer,
        boolean configured,
        String message) {

    public static AiQuestionResponse notConfigured(long datasetId, String question) {
        return new AiQuestionResponse(
                datasetId, question, null, false, "AI 模型尚未配置");
    }

    public static AiQuestionResponse completed(long datasetId, String question, String answer) {
        return new AiQuestionResponse(
                datasetId, question, answer, true, "AI 问答完成");
    }
}
