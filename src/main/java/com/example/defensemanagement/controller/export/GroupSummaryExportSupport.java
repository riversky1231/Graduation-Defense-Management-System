package com.example.defensemanagement.controller.export;

import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 小组统分表导出支持类。
 */
@Component
public class GroupSummaryExportSupport {

    private static final Logger log = LoggerFactory.getLogger(GroupSummaryExportSupport.class);

    private final TeacherScoreRecordMapper teacherScoreRecordMapper;
    private final StudentFinalScoreMapper studentFinalScoreMapper;
    private final DefenseGroupTeacherMapper defenseGroupTeacherMapper;
    private final ConfigService configService;
    private final UserService userService;
    private final PaperExportController paperExportController;

    public GroupSummaryExportSupport(
            TeacherScoreRecordMapper teacherScoreRecordMapper,
            StudentFinalScoreMapper studentFinalScoreMapper,
            DefenseGroupTeacherMapper defenseGroupTeacherMapper,
            ConfigService configService,
            UserService userService,
            PaperExportController paperExportController) {
        this.teacherScoreRecordMapper = teacherScoreRecordMapper;
        this.studentFinalScoreMapper = studentFinalScoreMapper;
        this.defenseGroupTeacherMapper = defenseGroupTeacherMapper;
        this.configService = configService;
        this.userService = userService;
        this.paperExportController = paperExportController;
    }

    public PreparedGroupSummary prepareGroupSummary(
            Long groupId,
            List<Student> students,
            String groupName,
            Integer year) {
        Map<String, String> placeholders = new HashMap<>();
        DateParts dateParts = getDateParts("DEFENSE_DATE");
        placeholders.put("{{YEAR}}", dateParts.year);
        placeholders.put("{{MONTH}}", dateParts.month);
        placeholders.put("{{DAY}}", dateParts.day);
        placeholders.put("{{GROUP_NAME}}", groupName);
        placeholders.put("{{DEPT_NAME}}", resolveDepartmentName(students));
        placeholders.put("{{GROUP_ID}}", String.valueOf(groupId));

        int maxJudges = resolveMaxJudges(students, year);
        placeholders.put("{{MAX_JUDGES}}", String.valueOf(maxJudges));

        int rowNum = 1;
        for (Student student : students) {
            List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(student.getId(), year);
            StudentFinalScore finalScore = studentFinalScoreMapper.findByStudentIdAndYear(student.getId(), year);

            double averageScore = 0.0;
            int teacherCount = 0;
            List<Double> judgeScores = new ArrayList<>();
            if (records != null) {
                for (TeacherScoreRecord record : records) {
                    if (record.getTotalScore() == null) {
                        continue;
                    }
                    averageScore += record.getTotalScore();
                    teacherCount++;
                    judgeScores.add(record.getTotalScore().doubleValue());
                }
            }
            if (teacherCount > 0) {
                averageScore = averageScore / teacherCount;
            }

            double factor = finalScore != null && finalScore.getAdjustmentFactor() != null
                    ? finalScore.getAdjustmentFactor()
                    : 1.0;
            double weightedScore = averageScore * factor;

            placeholders.put("{{STU_NAME_" + rowNum + "}}", nvl(student.getName()));
            placeholders.put("{{ROW_" + rowNum + "_NAME}}", nvl(student.getName()));
            for (int judgeIndex = 1; judgeIndex <= maxJudges; judgeIndex++) {
                String score = judgeIndex <= judgeScores.size()
                        ? formatInt(judgeScores.get(judgeIndex - 1))
                        : "-";
                placeholders.put("{{JUDGE_" + judgeIndex + "_SCORE_" + rowNum + "}}", score);
            }
            placeholders.put("{{DEFENSE_SCORE_" + rowNum + "}}", format1(averageScore));
            placeholders.put("{{ROW_" + rowNum + "_AVG}}", format1(averageScore));
            placeholders.put("{{FACTOR_" + rowNum + "}}", String.format(java.util.Locale.ROOT, "%.3f", factor));
            placeholders.put("{{ROW_" + rowNum + "_FACTOR}}", String.format(java.util.Locale.ROOT, "%.3f", factor));
            placeholders.put("{{FINAL_SCORE_" + rowNum + "}}", format1(weightedScore));
            placeholders.put("{{ROW_" + rowNum + "_FINAL}}", format1(weightedScore));
            placeholders.put("{{ROW_" + rowNum + "_SCORES}}", joinJudgeScores(judgeScores));
            rowNum++;
        }
        placeholders.put("{{TOTAL_ROWS}}", String.valueOf(rowNum - 1));

        Map<String, byte[]> images = new HashMap<>();
        loadGroupSignatures(groupId, images);
        return new PreparedGroupSummary(placeholders, images);
    }

    private void loadGroupSignatures(Long groupId, Map<String, byte[]> images) {
        try {
            DefenseGroupTeacher leader = defenseGroupTeacherMapper.findLeaderByGroupId(groupId);
            Long leaderTeacherId = leader != null ? leader.getTeacherId() : null;
            if (leaderTeacherId != null) {
                byte[] leaderSignature = paperExportController.loadSignature("teacher_" + leaderTeacherId);
                if (leaderSignature != null) {
                    images.put("{{SIGN_LEADER}}", leaderSignature);
                    images.put("{{SIGN_GROUP_LEADER}}", leaderSignature);
                } else {
                    log.warn("Group {} leader signature not found: teacher_{}", groupId, leaderTeacherId);
                }
            } else {
                log.warn("Group {} leader not found", groupId);
            }

            int judgeIndex = 1;
            List<DefenseGroupTeacher> groupTeachers = defenseGroupTeacherMapper.findByGroupId(groupId);
            if (groupTeachers != null) {
                for (DefenseGroupTeacher groupTeacher : groupTeachers) {
                    Long teacherId = groupTeacher.getTeacherId();
                    if (teacherId == null || teacherId.equals(leaderTeacherId)) {
                        continue;
                    }

                    byte[] judgeSignature = paperExportController.loadSignature("teacher_" + teacherId);
                    if (judgeSignature == null) {
                        log.warn("Group {} judge signature not found: teacher_{}", groupId, teacherId);
                    } else {
                        images.put("{{SIGN_JUDGE_" + judgeIndex + "}}", judgeSignature);
                        if (judgeIndex == 1) {
                            images.put("{{SIGN_JUDGE}}", judgeSignature);
                        }
                    }

                    judgeIndex++;
                    if (judgeIndex > 5) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load group signatures for group {}", groupId, e);
        }
    }

    private int resolveMaxJudges(List<Student> students, Integer year) {
        int maxJudges = 0;
        for (Student student : students) {
            List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(student.getId(), year);
            if (records != null) {
                maxJudges = Math.max(maxJudges, records.size());
            }
        }
        return Math.max(1, Math.min(maxJudges, 10));
    }

    private String resolveDepartmentName(List<Student> students) {
        if (students.isEmpty() || students.get(0).getDepartmentId() == null) {
            return "未知院系";
        }
        try {
            Department department = userService.getAllDepartments().stream()
                    .filter(item -> item.getId().equals(students.get(0).getDepartmentId()))
                    .findFirst()
                    .orElse(null);
            return department != null ? department.getName() : "未知院系";
        } catch (Exception e) {
            log.warn("Failed to resolve department name for department {}", students.get(0).getDepartmentId(), e);
            return "未知院系";
        }
    }

    private DateParts getDateParts(String prefix) {
        String year = configService.getDefenseDatePart(prefix + "_YEAR");
        String month = configService.getDefenseDatePart(prefix + "_MONTH");
        String day = configService.getDefenseDatePart(prefix + "_DAY");
        LocalDate now = LocalDate.now();
        return new DateParts(
                nvl(year, String.valueOf(now.getYear())),
                nvl(month, String.valueOf(now.getMonthValue())),
                nvl(day, String.valueOf(now.getDayOfMonth())));
    }

    private String joinJudgeScores(List<Double> judgeScores) {
        if (judgeScores.isEmpty()) {
            return "-";
        }

        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < judgeScores.size(); i++) {
            if (i > 0) {
                joined.append('、');
            }
            joined.append(formatInt(judgeScores.get(i)));
        }
        return joined.toString();
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }

    private String nvl(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String format1(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private String formatInt(double value) {
        return String.valueOf(Math.round(value));
    }

    public static final class PreparedGroupSummary {
        private final Map<String, String> placeholders;
        private final Map<String, byte[]> images;

        public PreparedGroupSummary(Map<String, String> placeholders, Map<String, byte[]> images) {
            this.placeholders = placeholders;
            this.images = images;
        }

        public Map<String, String> getPlaceholders() {
            return placeholders;
        }

        public Map<String, byte[]> getImages() {
            return images;
        }
    }

    private static final class DateParts {
        private final String year;
        private final String month;
        private final String day;

        private DateParts(String year, String month, String day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }
    }
}
