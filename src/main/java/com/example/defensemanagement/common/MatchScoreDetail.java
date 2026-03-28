package com.example.defensemanagement.common;

public class MatchScoreDetail {

    private final double totalScore;
    private final double preferenceScore;
    private final double capacityScore;
    private final double balanceScore;
    private final double relevanceScore;
    private final String relevanceSource;
    private final String relevanceReason;
    private final String materialSource;
    private final String materialReason;

    public MatchScoreDetail(double totalScore,
                            double preferenceScore,
                            double capacityScore,
                            double balanceScore,
                            double relevanceScore,
                            String relevanceSource,
                            String relevanceReason,
                            String materialSource,
                            String materialReason) {
        this.totalScore = totalScore;
        this.preferenceScore = preferenceScore;
        this.capacityScore = capacityScore;
        this.balanceScore = balanceScore;
        this.relevanceScore = relevanceScore;
        this.relevanceSource = relevanceSource;
        this.relevanceReason = relevanceReason;
        this.materialSource = materialSource;
        this.materialReason = materialReason;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public double getPreferenceScore() {
        return preferenceScore;
    }

    public double getCapacityScore() {
        return capacityScore;
    }

    public double getBalanceScore() {
        return balanceScore;
    }

    public double getRelevanceScore() {
        return relevanceScore;
    }

    public String getRelevanceSource() {
        return relevanceSource;
    }

    public String getRelevanceReason() {
        return relevanceReason;
    }

    public String getMaterialSource() {
        return materialSource;
    }

    public String getMaterialReason() {
        return materialReason;
    }
}
