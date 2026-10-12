package com.jiayi.dataagent.model;

public record AiAnalysisResponse(
        long datasetId,
        String filename,
        boolean configured,
        String message,
        String report) {

    public static AiAnalysisResponse notConfigured(long datasetId, String filename) {
        return new AiAnalysisResponse(
                datasetId, filename, false, "AI 模型尚未配置", null);
    }

    public static AiAnalysisResponse completed(long datasetId, String filename, String report) {
        return new AiAnalysisResponse(
                datasetId, filename, true, "AI 分析完成", report);
    }
}
