package com.example.defensemanagement.controller.export;

import com.example.defensemanagement.entity.EvaluationItem;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.service.AiCommentService;
import com.example.defensemanagement.service.ConfigService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ScorePlaceholderHelper {

    private final ConfigService configService;
    private final AiCommentService aiCommentService;

    public ScorePlaceholderHelper(ConfigService configService, AiCommentService aiCommentService) {
        this.configService = configService;
        this.aiCommentService = aiCommentService;
    }

    Map<String, String> buildCommonPlaceholders(Student stu, ExportHelper.DateParts dp, boolean isPaper) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{{NAME}}", nvl(stu.getName()));
        placeholders.put("{{STUDENT_NO}}", nvl(stu.getStudentNo()));
        placeholders.put("{{TITLE}}", nvl(stu.getTitle()));
        placeholders.put("{{YEAR}}", dp.year);
        placeholders.put("{{MONTH}}", dp.month);
        placeholders.put("{{DAY}}", dp.day);
        if (!isPaper) {
            placeholders.put("{{SUMMARY}}", nvl(stu.getSummary()));
        }
        return placeholders;
    }

    void fillPaperScorePlaceholders(
            Map<String, String> ph,
            List<TeacherScoreRecord> records,
            double factor,
            Student stu) {
        ExportHelper.AvgScores avg = avgPaper(records);
        double item1Scaled = avg.item1Scaled(factor);
        double item2Scaled = avg.item2Scaled(factor);
        double item3Scaled = avg.item3Scaled(factor);
        double totalFromItems = item1Scaled + item2Scaled + item3Scaled;

        List<EvaluationItem> items = configService.getEvaluationItems("PAPER");

        ph.put("{{ITEM1}}", formatInt(item1Scaled));
        ph.put("{{ITEM2}}", formatInt(item2Scaled));
        ph.put("{{ITEM3}}", formatInt(item3Scaled));
        ph.put("{{TOTAL}}", format1(totalFromItems));

        if (items != null && items.size() >= 3) {
            ph.put("{{ITEM1_NAME}}", nvl(items.get(0).getItemName(), "论文质量"));
            ph.put("{{ITEM1_DESC}}", nvl(items.get(0).getDescription()));
            ph.put("{{ITEM1_MAX}}", strOrDefault(items.get(0).getMaxScore(), "50"));

            ph.put("{{ITEM2_NAME}}", nvl(items.get(1).getItemName(), "答辩的自述报告"));
            ph.put("{{ITEM2_DESC}}", nvl(items.get(1).getDescription()));
            ph.put("{{ITEM2_MAX}}", strOrDefault(items.get(1).getMaxScore(), "25"));

            ph.put("{{ITEM3_NAME}}", nvl(items.get(2).getItemName(), "回答问题的情况"));
            ph.put("{{ITEM3_DESC}}", nvl(items.get(2).getDescription()));
            ph.put("{{ITEM3_MAX}}", strOrDefault(items.get(2).getMaxScore(), "25"));
        } else {
            ph.put("{{ITEM1_NAME}}", "论文质量");
            ph.put("{{ITEM1_DESC}}", "");
            ph.put("{{ITEM1_MAX}}", "50");
            ph.put("{{ITEM2_NAME}}", "答辩的自述报告");
            ph.put("{{ITEM2_DESC}}", "");
            ph.put("{{ITEM2_MAX}}", "25");
            ph.put("{{ITEM3_NAME}}", "回答问题的情况");
            ph.put("{{ITEM3_DESC}}", "");
            ph.put("{{ITEM3_MAX}}", "25");
        }

        ph.put("{{COMMENT}}", generateComment("PAPER_PROMPT", stu, totalFromItems, factor));
    }

    void fillDesignScorePlaceholders(
            Map<String, String> ph,
            List<TeacherScoreRecord> records,
            double factor,
            Student stu) {
        ExportHelper.AvgScores avg = avgDesign(records);
        double item1Scaled = avg.item1Scaled(factor);
        double item2Scaled = avg.item2Scaled(factor);
        double item3Scaled = avg.item3Scaled(factor);
        double item4Scaled = avg.item4Scaled(factor);
        double item5Scaled = avg.item5Scaled(factor);
        double item6Scaled = avg.item6Scaled(factor);
        double totalFromItems = item1Scaled + item2Scaled + item3Scaled + item4Scaled + item5Scaled + item6Scaled;

        List<EvaluationItem> items = configService.getEvaluationItems("DESIGN");

        ph.put("{{ITEM1}}", formatInt(item1Scaled));
        ph.put("{{ITEM2}}", formatInt(item2Scaled));
        ph.put("{{ITEM3}}", formatInt(item3Scaled));
        ph.put("{{ITEM4}}", formatInt(item4Scaled));
        ph.put("{{ITEM5}}", formatInt(item5Scaled));
        ph.put("{{ITEM6}}", formatInt(item6Scaled));
        ph.put("{{TOTAL}}", format1(totalFromItems));

        String[] defaultNames = {
                "设计质量1", "设计质量2", "设计质量3", "答辩的自述报告成绩", "回答问题的情况1", "回答问题的情况2"
        };
        int[] defaultMaxScores = {15, 15, 15, 25, 15, 15};

        if (items != null && items.size() >= 6) {
            for (int i = 0; i < 6; i++) {
                EvaluationItem item = items.get(i);
                int itemNum = i + 1;
                ph.put("{{ITEM" + itemNum + "_NAME}}", nvl(item.getItemName(), "指标" + itemNum));
                ph.put("{{ITEM" + itemNum + "_DESC}}", nvl(item.getDescription()));
                ph.put("{{ITEM" + itemNum + "_MAX}}",
                        strOrDefault(item.getMaxScore(), String.valueOf(defaultMaxScores[i])));
            }
        } else {
            for (int i = 0; i < 6; i++) {
                int itemNum = i + 1;
                ph.put("{{ITEM" + itemNum + "_NAME}}", defaultNames[i]);
                ph.put("{{ITEM" + itemNum + "_DESC}}", "");
                ph.put("{{ITEM" + itemNum + "_MAX}}", String.valueOf(defaultMaxScores[i]));
            }
        }

        ph.put("{{COMMENT}}", generateComment("DESIGN_PROMPT", stu, totalFromItems, factor));
    }

    void fillGradePlaceholders(
            Map<String, String> ph,
            StudentFinalScore fs,
            boolean isPaper,
            List<TeacherScoreRecord> records,
            Student stu) {
        int advisorScore = fs.getAdvisorScore() == null ? 0 : fs.getAdvisorScore();
        int reviewerScore = fs.getReviewerScore() == null ? 0 : fs.getReviewerScore();
        double defenseScore = fs.getFinalDefenseScore() == null ? 0.0 : fs.getFinalDefenseScore();

        ph.put("{{ADVISOR_SCORE}}", String.valueOf(advisorScore));
        ph.put("{{REVIEWER_SCORE}}", String.valueOf(reviewerScore));
        ph.put("{{DEFENSE_SCORE}}", format1(defenseScore));
        ph.put("{{ADVISOR_30}}", format1(advisorScore * 0.3));
        ph.put("{{REVIEWER_30}}", format1(reviewerScore * 0.3));
        ph.put("{{DEFENSE_40}}", format1(defenseScore * 0.4));
        ph.put("{{TOTAL_GRADE}}", format1(fs.getTotalGrade() == null ? 0 : fs.getTotalGrade()));

        double factor2 = fs.getAdjustmentFactor() != null ? fs.getAdjustmentFactor() : 1.0;
        if (isPaper) {
            ExportHelper.AvgScores avg = avgPaper(records);
            double totalFromItems = avg.item1Scaled(factor2) + avg.item2Scaled(factor2) + avg.item3Scaled(factor2);
            ph.put("{{COMMENT}}", generateComment("PAPER_PROMPT", stu, totalFromItems, factor2));
            return;
        }
        ph.put("{{COMMENT}}", generateComment("DESIGN_PROMPT", stu, defenseScore, factor2));
    }

    private ExportHelper.AvgScores avgPaper(List<TeacherScoreRecord> records) {
        ExportHelper.AvgScores scores = new ExportHelper.AvgScores();
        if (records == null || records.isEmpty()) {
            return scores;
        }

        int validCount = 0;
        for (TeacherScoreRecord record : records) {
            if (record.getTotalScore() == null) {
                continue;
            }
            validCount++;
            scores.item1 += nz(record.getItem1Score());
            scores.item2 += nz(record.getItem2Score());
            scores.item3 += nz(record.getItem3Score());
            scores.total += nz(record.getTotalScore());
        }

        if (validCount > 0) {
            scores.divide(validCount);
        }
        return scores;
    }

    private ExportHelper.AvgScores avgDesign(List<TeacherScoreRecord> records) {
        ExportHelper.AvgScores scores = new ExportHelper.AvgScores();
        if (records == null || records.isEmpty()) {
            return scores;
        }

        int validCount = 0;
        for (TeacherScoreRecord record : records) {
            if (record.getTotalScore() == null) {
                continue;
            }
            validCount++;
            scores.item1 += nz(record.getItem1Score());
            scores.item2 += nz(record.getItem2Score());
            scores.item3 += nz(record.getItem3Score());
            scores.item4 += nz(record.getItem4Score());
            scores.item5 += nz(record.getItem5Score());
            scores.item6 += nz(record.getItem6Score());
            scores.total += nz(record.getTotalScore());
        }

        if (validCount > 0) {
            scores.divide(validCount);
        }
        return scores;
    }

    private String generateComment(String promptKey, Student stu, double scaledTotal, double factor) {
        String context = "学生:" + nvl(stu.getName())
                + "，题目:" + nvl(stu.getTitle())
                + "，摘要:" + nvl(stu.getSummary())
                + "，调节系数:" + format1(factor)
                + "，小组加权总分:" + format1(scaledTotal);
        String response = aiCommentService.generateComment(promptKey, context);
        return response == null ? "" : response;
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }

    private String nvl(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String strOrDefault(Integer value, String defaultValue) {
        return value != null ? String.valueOf(value) : defaultValue;
    }

    private int nz(Integer value) {
        return value == null ? 0 : value;
    }

    private String format1(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private String formatInt(double value) {
        return String.valueOf(Math.round(value));
    }
}
