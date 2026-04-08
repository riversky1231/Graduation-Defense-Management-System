package com.example.defensemanagement.controller.export;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * 导出功能公共辅助类。
 * 仅保留控制器共享的薄封装，具体实现拆分到独立 helper。
 */
public abstract class ExportHelper {

    @Autowired
    protected ScorePlaceholderHelper scorePlaceholderHelper;

    @Autowired
    protected SignatureHelper signatureHelper;

    @Autowired
    protected TemplateHelper templateHelper;

    protected final Map<String, String> buildCommonPlaceholders(Student stu, DateParts dp, boolean isPaper) {
        return scorePlaceholderHelper.buildCommonPlaceholders(stu, dp, isPaper);
    }

    protected final void fillPaperScorePlaceholders(
            Map<String, String> ph,
            List<TeacherScoreRecord> records,
            double factor,
            Student stu) {
        scorePlaceholderHelper.fillPaperScorePlaceholders(ph, records, factor, stu);
    }

    protected final void fillDesignScorePlaceholders(
            Map<String, String> ph,
            List<TeacherScoreRecord> records,
            double factor,
            Student stu) {
        scorePlaceholderHelper.fillDesignScorePlaceholders(ph, records, factor, stu);
    }

    protected final void fillGradePlaceholders(
            Map<String, String> ph,
            StudentFinalScore fs,
            boolean isPaper,
            List<TeacherScoreRecord> records,
            Student stu) {
        scorePlaceholderHelper.fillGradePlaceholders(ph, fs, isPaper, records, stu);
    }

    protected final void fillSignatures(
            Map<String, byte[]> img,
            Student stu,
            boolean isGradeForm,
            boolean isProcessForm) {
        signatureHelper.fillSignatures(img, stu, isGradeForm, isProcessForm);
    }

    public final byte[] loadSignature(String namePrefix) {
        return signatureHelper.loadSignature(namePrefix);
    }

    public final String resolveTemplate(String key, String defaultClasspath, Long departmentId) {
        return templateHelper.resolveTemplate(key, defaultClasspath, departmentId);
    }

    protected final DateParts getDateParts(String prefix) {
        return templateHelper.getDateParts(prefix);
    }

    public final String encode(String name) {
        return templateHelper.encode(name);
    }

    protected static class DateParts {
        final String year;
        final String month;
        final String day;

        DateParts(String year, String month, String day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }
    }

    protected static class AvgScores {
        double item1;
        double item2;
        double item3;
        double item4;
        double item5;
        double item6;
        double total;

        void divide(int n) {
            item1 /= n;
            item2 /= n;
            item3 /= n;
            item4 /= n;
            item5 /= n;
            item6 /= n;
            total /= n;
        }

        double item1Scaled(double f) {
            return item1 * f;
        }

        double item2Scaled(double f) {
            return item2 * f;
        }

        double item3Scaled(double f) {
            return item3 * f;
        }

        double item4Scaled(double f) {
            return item4 * f;
        }

        double item5Scaled(double f) {
            return item5 * f;
        }

        double item6Scaled(double f) {
            return item6 * f;
        }
    }
}
