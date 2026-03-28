package com.example.defensemanagement.common;

public class RelevanceAnalysisResult {

    public static final String SOURCE_MODEL = "MODEL";
    public static final String SOURCE_FALLBACK = "FALLBACK";
    public static final String SOURCE_EMPTY_INPUT = "EMPTY_INPUT";

    private final double score;
    private final String source;
    private final String reason;

    public RelevanceAnalysisResult(double score, String source, String reason) {
        this.score = score;
        this.source = source;
        this.reason = reason;
    }

    public double getScore() {
        return score;
    }

    public String getSource() {
        return source;
    }

    public String getReason() {
        return reason;
    }
}
