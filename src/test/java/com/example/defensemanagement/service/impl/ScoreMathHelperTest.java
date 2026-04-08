package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.StudentFinalScore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ScoreMathHelperTest {

    @Test
    void computeAdjustmentFactorReturnsNullForNonPositiveInputs() {
        assertNull(ScoreMathHelper.computeAdjustmentFactor((Double) null, 80.0));
        assertNull(ScoreMathHelper.computeAdjustmentFactor(95.0, null));
        assertNull(ScoreMathHelper.computeAdjustmentFactor(0.0, 80.0));
        assertNull(ScoreMathHelper.computeAdjustmentFactor(95.0, 0.0));
    }

    @Test
    void computeAdjustmentFactorRoundsToThreeDecimals() {
        assertEquals(1.056, ScoreMathHelper.computeAdjustmentFactor(95.0, 90.0));
    }

    @Test
    void computeFinalDefenseScoreRoundsToSingleDecimal() {
        assertEquals(73.9, ScoreMathHelper.computeFinalDefenseScore(70.0, 1.056));
        assertEquals(88.6, ScoreMathHelper.computeFinalDefenseScore(88.55, null));
    }

    @Test
    void refreshTotalGradeUsesConfiguredWeights() {
        StudentFinalScore finalScore = new StudentFinalScore();
        finalScore.setAdvisorScore(80);
        finalScore.setReviewerScore(85);

        ScoreMathHelper.refreshTotalGrade(finalScore, 95.0);

        assertEquals(87.5, finalScore.getTotalGrade());
    }
}
