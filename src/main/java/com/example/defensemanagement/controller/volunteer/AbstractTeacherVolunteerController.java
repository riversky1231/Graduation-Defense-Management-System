package com.example.defensemanagement.controller.volunteer;

import com.example.defensemanagement.common.MatchScoreDetail;
import com.example.defensemanagement.common.RelevanceAnalysisResult;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentPreference;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.TeacherProfile;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.StudentPreferenceMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherProfileMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.StudentService;
import com.example.defensemanagement.service.VolunteerMatchService;
import com.example.defensemanagement.service.impl.ConfigServiceImpl;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 教师志愿管理共享基类。
 * 负责教师会话解析、配置读取、志愿条目组装和轮次辅助逻辑。
 */
abstract class AbstractTeacherVolunteerController {

    protected static final String SUCCESS = "success";
    private static final DateTimeFormatter DEADLINE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    protected final TeacherMapper teacherMapper;
    protected final StudentPreferenceMapper studentPreferenceMapper;
    protected final StudentMapper studentMapper;
    protected final StudentService studentService;
    protected final ConfigService configService;
    protected final TeacherProfileMapper teacherProfileMapper;
    protected final VolunteerMatchService volunteerMatchService;

    protected AbstractTeacherVolunteerController(
            TeacherMapper teacherMapper,
            StudentPreferenceMapper studentPreferenceMapper,
            StudentMapper studentMapper,
            StudentService studentService,
            ConfigService configService,
            TeacherProfileMapper teacherProfileMapper,
            VolunteerMatchService volunteerMatchService) {
        this.teacherMapper = teacherMapper;
        this.studentPreferenceMapper = studentPreferenceMapper;
        this.studentMapper = studentMapper;
        this.studentService = studentService;
        this.configService = configService;
        this.teacherProfileMapper = teacherProfileMapper;
        this.volunteerMatchService = volunteerMatchService;
    }

    protected Teacher getCurrentTeacher(HttpSession session) {
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
        if (currentTeacher != null) {
            return currentTeacher;
        }

        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null && currentUser.getRole() != null) {
            String roleName = currentUser.getRole().getName();
            if ("TEACHER".equals(roleName) || "DEFENSE_LEADER".equals(roleName)) {
                return teacherMapper.findByUserId(currentUser.getId());
            }
        }
        return null;
    }

    protected Integer getCurrentYear() {
        Integer year = configService.getCurrentDefenseYear();
        return year != null ? year : Year.now().getValue();
    }

    protected int getMaxStudents() {
        String value = configService.getConfigValue(ConfigServiceImpl.KEY_TEACHER_MAX_STUDENTS);
        if (value == null || value.trim().isEmpty()) {
            return 5;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 5;
        }
    }

    protected String getVolunteerDeadline() {
        return configService.getConfigValue(ConfigServiceImpl.KEY_VOLUNTEER_DEADLINE);
    }

    protected boolean isDeadlinePassed() {
        String deadline = getVolunteerDeadline();
        if (deadline == null || deadline.trim().isEmpty()) {
            return false;
        }
        try {
            LocalDateTime end = LocalDateTime.parse(deadline.trim(), DEADLINE_FORMATTER);
            return LocalDateTime.now().isAfter(end);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    protected String validateRound(Integer round) {
        return round == null || round < 1 || round > 3 ? "round参数必须为1-3" : null;
    }

    protected Map<String, Object> errorResult(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("error", message);
        return result;
    }

    protected List<Map<String, Object>> buildVolunteerItems(
            List<Map<String, Object>> rows,
            Teacher teacher,
            Integer year,
            Integer fixedRound,
            int assigned,
            int maxStudents,
            boolean deadlinePassed) {
        int remaining = Math.max(0, maxStudents - assigned);
        TeacherProfile teacherProfile = teacherProfileMapper.findByTeacherId(teacher.getId());
        Map<String, RelevanceAnalysisResult> relevanceCache = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();

        if (rows == null) {
            return items;
        }
        for (Map<String, Object> row : rows) {
            items.add(buildVolunteerItem(
                    row,
                    teacher,
                    year,
                    fixedRound,
                    assigned,
                    maxStudents,
                    deadlinePassed,
                    remaining,
                    teacherProfile,
                    relevanceCache));
        }
        sortVolunteerItems(items);
        return items;
    }

    protected VolunteerRoundSelection resolveSelection(StudentPreference preference, Integer round) {
        if (preference == null || round == null) {
            return new VolunteerRoundSelection(null, null);
        }
        if (round == 1) {
            return new VolunteerRoundSelection(preference.getChoice1TeacherId(), preference.getFile1Path());
        }
        if (round == 2) {
            return new VolunteerRoundSelection(preference.getChoice2TeacherId(), preference.getFile2Path());
        }
        return new VolunteerRoundSelection(preference.getChoice3TeacherId(), preference.getFile3Path());
    }

    protected boolean isVolunteerUploadPath(String filePath) {
        return filePath != null && filePath.replace("\\", "/").startsWith("uploads/volunteer/");
    }

    protected Long getLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    protected double getDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private Map<String, Object> buildVolunteerItem(
            Map<String, Object> row,
            Teacher teacher,
            Integer year,
            Integer fixedRound,
            int assigned,
            int maxStudents,
            boolean deadlinePassed,
            int remaining,
            TeacherProfile teacherProfile,
            Map<String, RelevanceAnalysisResult> relevanceCache) {
        Map<String, Object> item = new HashMap<>();
        Long studentId = getLong(row.get("student_id"));
        item.put("studentId", studentId);
        item.put("studentNo", row.get("student_no"));
        item.put("studentName", row.get("student_name"));
        item.put("classInfo", row.get("class_info"));
        item.put("defenseType", row.get("defense_type"));
        item.put("title", row.get("title"));
        item.put("summary", row.get("summary"));
        item.put("advisorTeacherId", row.get("advisor_teacher_id"));
        item.put("advisorName", row.get("advisor_name"));

        String avatarPath = (String) row.get("student_avatar_path");
        item.put("avatarPath", avatarPath);
        item.put("avatarUrl", avatarPath != null ? "/avatar/view?path=" + avatarPath : null);

        int volunteerRound = fixedRound != null ? fixedRound : resolveVolunteerRound(row, teacher.getId());
        if (fixedRound == null) {
            item.put("volunteerRound", volunteerRound);
        }
        item.put("filePath", resolveVolunteerFilePath(row, volunteerRound));

        Object advisorTeacherId = row.get("advisor_teacher_id");
        boolean assignedToSomeone = advisorTeacherId != null;
        boolean assignedToMe = assignedToSomeone
                && String.valueOf(advisorTeacherId).equals(String.valueOf(teacher.getId()));
        item.put("assignedToMe", assignedToMe);
        item.put("assignedToSomeone", assignedToSomeone);
        item.put("canAccept", !deadlinePassed && !assignedToSomeone && assigned < maxStudents);
        item.put("canCancel", !deadlinePassed && assignedToMe);

        if (studentId != null) {
            Student student = studentMapper.findById(studentId);
            StudentPreference preference = studentPreferenceMapper.findByStudentIdAndYear(studentId, year);
            MatchScoreDetail matchDetail = volunteerMatchService.calculateMatchDetail(
                    preference,
                    student,
                    teacher,
                    teacherProfile,
                    remaining,
                    assigned,
                    maxStudents,
                    relevanceCache);
            applyMatchDetail(item, matchDetail);
        } else {
            applyMissingStudentMatchDetail(item);
        }
        return item;
    }

    private void sortVolunteerItems(List<Map<String, Object>> items) {
        items.sort((left, right) -> {
            int scoreCompare = Double.compare(getDouble(right.get("matchScore")), getDouble(left.get("matchScore")));
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            return String.valueOf(left.getOrDefault("studentNo", ""))
                    .compareTo(String.valueOf(right.getOrDefault("studentNo", "")));
        });
    }

    private int resolveVolunteerRound(Map<String, Object> row, Long teacherId) {
        if (teacherId == null) {
            return 0;
        }
        if (teacherId.equals(getLong(row.get("choice1_teacher_id")))) {
            return 1;
        }
        if (teacherId.equals(getLong(row.get("choice2_teacher_id")))) {
            return 2;
        }
        if (teacherId.equals(getLong(row.get("choice3_teacher_id")))) {
            return 3;
        }
        return 0;
    }

    private String resolveVolunteerFilePath(Map<String, Object> row, int volunteerRound) {
        if (volunteerRound == 1) {
            return (String) row.get("file1_path");
        }
        if (volunteerRound == 2) {
            return (String) row.get("file2_path");
        }
        if (volunteerRound == 3) {
            return (String) row.get("file3_path");
        }
        return null;
    }

    private void applyMatchDetail(Map<String, Object> item, MatchScoreDetail matchDetail) {
        item.put("matchScore", roundScore(matchDetail.getTotalScore()));
        item.put("relevanceScore", roundScore(matchDetail.getRelevanceScore()));
        item.put("relevanceSource", matchDetail.getRelevanceSource());
        item.put("relevanceReason", matchDetail.getRelevanceReason());
        item.put("materialSource", matchDetail.getMaterialSource());
        item.put("materialReason", matchDetail.getMaterialReason());
    }

    private void applyMissingStudentMatchDetail(Map<String, Object> item) {
        item.put("matchScore", 0.0);
        item.put("relevanceScore", 0.0);
        item.put("relevanceSource", RelevanceAnalysisResult.SOURCE_EMPTY_INPUT);
        item.put("relevanceReason", "student_id_missing");
        item.put("materialSource", "EMPTY");
        item.put("materialReason", "student_id_missing");
    }

    private double roundScore(double score) {
        return Math.round(score * 100.0) / 100.0;
    }

    protected static final class VolunteerRoundSelection {
        private final Long teacherId;
        private final String filePath;

        private VolunteerRoundSelection(Long teacherId, String filePath) {
            this.teacherId = teacherId;
            this.filePath = filePath;
        }

        protected Long getTeacherId() {
            return teacherId;
        }

        protected String getFilePath() {
            return filePath;
        }
    }
}
