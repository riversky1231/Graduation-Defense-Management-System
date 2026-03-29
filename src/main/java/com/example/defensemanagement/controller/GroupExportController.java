package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.DocTemplateService;
import com.example.defensemanagement.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/export")
public class GroupExportController {

    private static final Logger log = LoggerFactory.getLogger(GroupExportController.class);
    private static final String GROUP_SUMMARY_TEMPLATE = "templates/docx/group-summary.docx";

    @Autowired
    private ExportController exportController;

    @Autowired
    private DocTemplateService docTemplateService;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeacherScoreRecordMapper teacherScoreRecordMapper;

    @Autowired
    private StudentFinalScoreMapper studentFinalScoreMapper;

    @Autowired
    private DefenseGroupTeacherMapper defenseGroupTeacherMapper;

    @Autowired
    private DefenseGroupMapper defenseGroupMapper;

    @Autowired
    private ConfigService configService;

    @Autowired
    private UserService userService;

    @GetMapping("/group/{groupId}/summary")
    public ResponseEntity<byte[]> exportGroupSummary(@PathVariable Long groupId) {
        List<Student> students = studentMapper.findByDefenseGroupId(groupId);
        if (students == null || students.isEmpty()) {
            throw new RuntimeException("小组无学生");
        }

        Integer year = students.get(0).getDefenseYear();
        if (year == null) {
            throw new RuntimeException("学生未设置答辩年份");
        }

        com.example.defensemanagement.entity.DefenseGroup group = defenseGroupMapper.findById(groupId);
        String groupName = group != null ? group.getName() : "小组" + groupId;
        List<DefenseGroupTeacher> groupTeachers = defenseGroupTeacherMapper.findByGroupId(groupId);

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
        for (Student stu : students) {
            List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(stu.getId(), year);
            StudentFinalScore finalScore = studentFinalScoreMapper.findByStudentIdAndYear(stu.getId(), year);

            double avgScore = 0.0;
            int teacherCount = 0;
            List<Double> judgeScores = new ArrayList<>();

            if (records != null && !records.isEmpty()) {
                for (TeacherScoreRecord record : records) {
                    if (record.getTotalScore() == null) {
                        continue;
                    }
                    avgScore += record.getTotalScore();
                    teacherCount++;
                    judgeScores.add(record.getTotalScore().doubleValue());
                }
                if (teacherCount > 0) {
                    avgScore = avgScore / teacherCount;
                }
            }

            double factor = finalScore != null && finalScore.getAdjustmentFactor() != null
                    ? finalScore.getAdjustmentFactor() : 1.0;
            double weightedScore = avgScore * factor;

            placeholders.put("{{STU_NAME_" + rowNum + "}}", nvl(stu.getName()));
            placeholders.put("{{ROW_" + rowNum + "_NAME}}", nvl(stu.getName()));
            for (int j = 1; j <= maxJudges; j++) {
                String score = j <= judgeScores.size() ? formatInt(judgeScores.get(j - 1)) : "-";
                placeholders.put("{{JUDGE_" + j + "_SCORE_" + rowNum + "}}", score);
            }
            placeholders.put("{{DEFENSE_SCORE_" + rowNum + "}}", format1(avgScore));
            placeholders.put("{{ROW_" + rowNum + "_AVG}}", format1(avgScore));
            placeholders.put("{{FACTOR_" + rowNum + "}}", String.format(java.util.Locale.ROOT, "%.3f", factor));
            placeholders.put("{{ROW_" + rowNum + "_FACTOR}}", String.format(java.util.Locale.ROOT, "%.3f", factor));
            placeholders.put("{{FINAL_SCORE_" + rowNum + "}}", format1(weightedScore));
            placeholders.put("{{ROW_" + rowNum + "_FINAL}}", format1(weightedScore));
            placeholders.put("{{ROW_" + rowNum + "_SCORES}}", joinJudgeScores(judgeScores));
            rowNum++;
        }
        placeholders.put("{{TOTAL_ROWS}}", String.valueOf(rowNum - 1));

        Map<String, byte[]> images = new HashMap<>();
        loadGroupSignatures(groupId, groupTeachers, images);

        String filename = exportController.encode("毕业论文(设计)答辩小组统分表-" + groupName + ".docx");
        Long departmentId = students.get(0).getDepartmentId();
        byte[] doc = docTemplateService.renderDoc(
                exportController.resolveTemplate("group-summary", GROUP_SUMMARY_TEMPLATE, departmentId),
                placeholders,
                images
        );
        return attachment(doc, filename);
    }

    @GetMapping("/group/{groupId}/zip")
    public ResponseEntity<byte[]> exportGroupZip(@PathVariable Long groupId) {
        List<Student> students = studentMapper.findByDefenseGroupId(groupId);
        if (students == null || students.isEmpty()) {
            throw new RuntimeException("小组无学生");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Student stu : students) {
                boolean isPaper = "PAPER".equalsIgnoreCase(stu.getDefenseType());
                ResponseEntity<byte[]> response = exportController.buildScoreDoc(stu.getId(), isPaper);
                String fallback = (isPaper ? "本科毕业论文答辩成绩表-" : "本科毕业设计答辩成绩表-") + stu.getName() + ".docx";
                zos.putNextEntry(new ZipEntry(extractFilename(response, fallback)));
                zos.write(response.getBody());
                zos.closeEntry();
            }
            zos.finish();
            byte[] zipBytes = baos.toByteArray();
            String zipName = exportController.encode("group-" + groupId + "-scores.zip");
            MediaType octet = MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipName + "\"")
                    .contentType(octet)
                    .body(zipBytes);
        } catch (Exception e) {
            throw new RuntimeException("打包导出失败: " + e.getMessage(), e);
        }
    }

    private void loadGroupSignatures(Long groupId, List<DefenseGroupTeacher> groupTeachers, Map<String, byte[]> images) {
        try {
            DefenseGroupTeacher leader = defenseGroupTeacherMapper.findLeaderByGroupId(groupId);
            Long leaderTeacherId = leader != null ? leader.getTeacherId() : null;
            if (leaderTeacherId != null) {
                byte[] leaderSignature = exportController.loadSignature("teacher_" + leaderTeacherId);
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
            if (groupTeachers != null) {
                for (DefenseGroupTeacher groupTeacher : groupTeachers) {
                    Long teacherId = groupTeacher.getTeacherId();
                    if (teacherId == null || teacherId.equals(leaderTeacherId)) {
                        continue;
                    }

                    byte[] judgeSignature = exportController.loadSignature("teacher_" + teacherId);
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
        for (Student stu : students) {
            List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(stu.getId(), year);
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
            com.example.defensemanagement.entity.Department department = userService.getAllDepartments().stream()
                    .filter(d -> d.getId().equals(students.get(0).getDepartmentId()))
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
                nvl(day, String.valueOf(now.getDayOfMonth()))
        );
    }

    private ResponseEntity<byte[]> attachment(byte[] bytes, String filename) {
        MediaType octet = MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(octet)
                .body(bytes);
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

    private String extractFilename(ResponseEntity<byte[]> response, String fallback) {
        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        if (disposition != null && disposition.contains("filename=\"")) {
            int idx = disposition.indexOf("filename=\"") + 10;
            int end = disposition.indexOf("\"", idx);
            if (end > idx) {
                return disposition.substring(idx, end);
            }
        }
        return fallback;
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

    private static class DateParts {
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
