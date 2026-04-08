package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.TeacherScoreRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.DoubleSummaryStatistics;
import java.util.List;

/**
 * Pure-math helpers for score computation. No database access, no state.
 * All rounding uses HALF_UP as required by the domain.
 */
final class ScoreMathHelper {

    // ─── Precision constants ─────────────────────────────────────────────────
    /** Integer scale used by persisted whole-number score fields. */
    static final int INTEGER_SCALE = 0;

    /** Default rounding scale for scores (1 decimal place). */
    static final int DEFAULT_SCALE = 1;

    /** Rounding scale for adjustment factor (3 decimal places). */
    static final int ADJUSTMENT_SCALE = 3;

    /** Rounding scale for final defense score (1 decimal place). */
    static final int DEFENSE_SCALE = 1;

    // ─── Weight constants ─────────────────────────────────────────────────────
    static final BigDecimal ADVISOR_WEIGHT = new BigDecimal("0.3");
    static final BigDecimal REVIEWER_WEIGHT = new BigDecimal("0.3");
    static final BigDecimal DEFENSE_WEIGHT = new BigDecimal("0.4");

    private ScoreMathHelper() {}

    /**
     * Returns the average of all non-null total scores, or null if no valid scores exist.
     */
    static Double averageTotalScore(List<TeacherScoreRecord> records) {
        if (records == null || records.isEmpty()) {
            return null;
        }
        DoubleSummaryStatistics stats = records.stream()
                .filter(record -> record.getTotalScore() != null)
                .mapToDouble(TeacherScoreRecord::getTotalScore)
                .summaryStatistics();
        return stats.getCount() == 0 ? null : stats.getAverage();
    }

    /**
     * Refreshes the total grade using the formula:
     * total_grade = advisor_score * 0.3 + reviewer_score * 0.3 + final_defense_score * 0.4
     *
     * Uses BigDecimal for precise arithmetic.
     */
    static void refreshTotalGrade(StudentFinalScore finalScore, double finalDefenseScore) {
        if (finalScore.getAdvisorScore() == null || finalScore.getReviewerScore() == null) {
            return;
        }
        BigDecimal advisorBd   = BigDecimal.valueOf(finalScore.getAdvisorScore());
        BigDecimal reviewerBd  = BigDecimal.valueOf(finalScore.getReviewerScore());
        BigDecimal defenseBd   = BigDecimal.valueOf(finalDefenseScore);

        BigDecimal totalGrade = advisorBd.multiply(ADVISOR_WEIGHT)
                .add(reviewerBd.multiply(REVIEWER_WEIGHT))
                .add(defenseBd.multiply(DEFENSE_WEIGHT))
                .setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);

        finalScore.setTotalGrade(totalGrade.doubleValue());
    }

    /**
     * Rounds using HALF_UP mode at the specified scale.
     */
    static double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Rounds to the default scale (1 decimal place).
     */
    static double roundDefault(double value) {
        return round(value, DEFAULT_SCALE);
    }

    /**
     * Rounds a score into an integer value for fields stored as whole numbers.
     */
    static int roundToIntegerScore(double value) {
        return BigDecimal.valueOf(value).setScale(INTEGER_SCALE, RoundingMode.HALF_UP).intValue();
    }

    /**
     * Computes the adjustment factor: largeGroupScore / groupAvgScore.
     * Returns null if either argument is null or zero.
     */
    static Double computeAdjustmentFactor(Integer largeGroupScore, Double groupAvgScore) {
        return largeGroupScore == null ? null : computeAdjustmentFactor(largeGroupScore.doubleValue(), groupAvgScore);
    }

    /**
     * Computes the adjustment factor: largeGroupScore / groupAvgScore.
     * Returns null if either argument is null or zero.
     */
    static Double computeAdjustmentFactor(Double largeGroupScore, Double groupAvgScore) {
        if (largeGroupScore == null || groupAvgScore == null || largeGroupScore <= 0 || groupAvgScore <= 0) {
            return null;
        }
        return round(
                BigDecimal.valueOf(largeGroupScore)
                        .divide(BigDecimal.valueOf(groupAvgScore), ADJUSTMENT_SCALE, RoundingMode.HALF_UP)
                        .doubleValue(),
                ADJUSTMENT_SCALE
        );
    }

    /**
     * Computes the final defense score: groupAvgScore * adjustmentFactor.
     */
    static double computeFinalDefenseScore(double groupAvgScore, Double adjustmentFactor) {
        if (adjustmentFactor == null) {
            return roundDefault(groupAvgScore);
        }
        return round(
                BigDecimal.valueOf(groupAvgScore)
                        .multiply(BigDecimal.valueOf(adjustmentFactor))
                        .setScale(DEFENSE_SCALE, RoundingMode.HALF_UP)
                        .doubleValue(),
                DEFENSE_SCALE
        );
    }

    static String resolveDepartmentName(Student student) {
        if (student.getDepartment() != null && student.getDepartment().getName() != null
                && !student.getDepartment().getName().isEmpty()) {
            return student.getDepartment().getName();
        }
        return null;
    }
}
