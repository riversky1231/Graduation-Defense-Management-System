package com.example.defensemanagement.controller;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/teacher/volunteer")
public class TeacherVolunteerController {

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private StudentPreferenceMapper studentPreferenceMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private StudentService studentService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private TeacherProfileMapper teacherProfileMapper;

    @Autowired
    private VolunteerMatchService volunteerMatchService;

    private Teacher getCurrentTeacher(HttpSession session) {
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
        if (currentTeacher != null) {
            return currentTeacher;
        }
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null && currentUser.getRole() != null) {
            String role = currentUser.getRole().getName();
            if ("TEACHER".equals(role) || "DEFENSE_LEADER".equals(role)) {
                return teacherMapper.findByUserId(currentUser.getId());
            }
        }
        return null;
    }

    private Integer getCurrentYear() {
        Integer year = configService.getCurrentDefenseYear();
        return year != null ? year : java.time.Year.now().getValue();
    }

    private Integer getMaxStudents() {
        String v = configService.getConfigValue(ConfigServiceImpl.KEY_TEACHER_MAX_STUDENTS);
        if (v == null || v.trim().isEmpty()) {
            return 5;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return 5;
        }
    }

    private boolean isDeadlinePassed() {
        String deadline = configService.getConfigValue(ConfigServiceImpl.KEY_VOLUNTEER_DEADLINE);
        if (deadline == null || deadline.trim().isEmpty()) {
            return false;
        }
        try {
            LocalDateTime end = LocalDateTime.parse(deadline.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return LocalDateTime.now().isAfter(end);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    @GetMapping("/config")
    @ResponseBody
    public Map<String, Object> getVolunteerConfig(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Teacher teacher = getCurrentTeacher(session);
        if (teacher == null) {
            result.put("error", "未登录或非教师身份");
            return result;
        }
        Integer year = getCurrentYear();
        int assigned = studentMapper.countByAdvisorAndYear(teacher.getId(), year);
        result.put("year", year);
        result.put("maxStudents", getMaxStudents());
        result.put("deadline", configService.getConfigValue(ConfigServiceImpl.KEY_VOLUNTEER_DEADLINE));
        result.put("assignedCount", assigned);
        result.put("deadlinePassed", isDeadlinePassed());
        return result;
    }

    @GetMapping("/list")
    @ResponseBody
    public Map<String, Object> getVolunteerList(@RequestParam Integer round, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Teacher teacher = getCurrentTeacher(session);
        if (teacher == null) {
            result.put("error", "未登录或非教师身份");
            return result;
        }
        if (round == null || round < 1 || round > 3) {
            result.put("error", "round参数必须为1-3");
            return result;
        }

        Integer year = getCurrentYear();
        List<Map<String, Object>> rows = studentPreferenceMapper.findByTeacherAndYearAndRound(teacher.getId(), year, round);
        int maxStudents = getMaxStudents();
        int assigned = studentMapper.countByAdvisorAndYear(teacher.getId(), year);
        boolean deadlinePassed = isDeadlinePassed();
        int remaining = Math.max(0, maxStudents - assigned);
        TeacherProfile teacherProfile = teacherProfileMapper.findByTeacherId(teacher.getId());
        Map<String, RelevanceAnalysisResult> relevanceCache = new HashMap<>();

        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> item = new HashMap<>();
            Long studentId = getLong(r.get("student_id"));
            item.put("studentId", studentId);
            item.put("studentNo", r.get("student_no"));
            item.put("studentName", r.get("student_name"));
            item.put("classInfo", r.get("class_info"));
            item.put("defenseType", r.get("defense_type"));
            item.put("title", r.get("title"));
            item.put("summary", r.get("summary"));
            item.put("advisorTeacherId", r.get("advisor_teacher_id"));
            item.put("advisorName", r.get("advisor_name"));

            // 学生头像
            String avatarPath = (String) r.get("student_avatar_path");
            item.put("avatarPath", avatarPath);
            item.put("avatarUrl", avatarPath != null ? "/avatar/view?path=" + avatarPath : null);

            String filePath = null;
            if (round == 1) filePath = (String) r.get("file1_path");
            if (round == 2) filePath = (String) r.get("file2_path");
            if (round == 3) filePath = (String) r.get("file3_path");
            item.put("filePath", filePath);

            boolean assignedToSomeone = r.get("advisor_teacher_id") != null;
            boolean assignedToMe = assignedToSomeone && String.valueOf(r.get("advisor_teacher_id"))
                    .equals(String.valueOf(teacher.getId()));
            boolean canAccept = !deadlinePassed
                    && !assignedToSomeone
                    && assigned < maxStudents;
            boolean canCancel = !deadlinePassed
                    && assignedToMe;

            item.put("assignedToMe", assignedToMe);
            item.put("assignedToSomeone", assignedToSomeone);
            item.put("canAccept", canAccept);
            item.put("canCancel", canCancel);

            if (studentId != null) {
                Student student = studentMapper.findById(studentId);
                StudentPreference preference = studentPreferenceMapper.findByStudentIdAndYear(studentId, year);
                MatchScoreDetail matchDetail = volunteerMatchService.calculateMatchDetail(preference, student, teacher,
                        teacherProfile, remaining, assigned, maxStudents, relevanceCache);
                item.put("matchScore", Math.round(matchDetail.getTotalScore() * 100.0) / 100.0);
                item.put("relevanceScore", Math.round(matchDetail.getRelevanceScore() * 100.0) / 100.0);
                item.put("relevanceSource", matchDetail.getRelevanceSource());
                item.put("relevanceReason", matchDetail.getRelevanceReason());
                item.put("materialSource", matchDetail.getMaterialSource());
                item.put("materialReason", matchDetail.getMaterialReason());
            } else {
                item.put("matchScore", 0.0);
                item.put("relevanceScore", 0.0);
                item.put("relevanceSource", RelevanceAnalysisResult.SOURCE_EMPTY_INPUT);
                item.put("relevanceReason", "student_id_missing");
                item.put("materialSource", "EMPTY");
                item.put("materialReason", "student_id_missing");
            }
            list.add(item);
        }

        list.sort((a, b) -> {
            int cmp = Double.compare(getDouble(b.get("matchScore")), getDouble(a.get("matchScore")));
            if (cmp != 0) return cmp;
            return String.valueOf(a.getOrDefault("studentNo", "")).compareTo(String.valueOf(b.getOrDefault("studentNo", "")));
        });

        result.put("year", year);
        result.put("round", round);
        result.put("assignedCount", assigned);
        result.put("maxStudents", maxStudents);
        result.put("deadlinePassed", deadlinePassed);
        result.put("items", list);
        return result;
    }

    /** 合并三个志愿，按 matchScore 排序，前端统一展示 */
    @GetMapping("/all")
    @ResponseBody
    public Map<String, Object> getAllVolunteers(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Teacher teacher = getCurrentTeacher(session);
        if (teacher == null) {
            result.put("error", "未登录或非教师身份");
            return result;
        }
        Integer year = getCurrentYear();
        List<Map<String, Object>> rows = studentPreferenceMapper.findAllByTeacherAndYear(teacher.getId(), year);
        int maxStudents = getMaxStudents();
        int assigned = studentMapper.countByAdvisorAndYear(teacher.getId(), year);
        boolean deadlinePassed = isDeadlinePassed();
        int remaining = Math.max(0, maxStudents - assigned);
        TeacherProfile teacherProfile = teacherProfileMapper.findByTeacherId(teacher.getId());
        Map<String, RelevanceAnalysisResult> relevanceCache = new HashMap<>();

        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> item = new HashMap<>();
            Long studentId = getLong(r.get("student_id"));
            item.put("studentId", studentId);
            item.put("studentNo", r.get("student_no"));
            item.put("studentName", r.get("student_name"));
            item.put("classInfo", r.get("class_info"));
            item.put("defenseType", r.get("defense_type"));
            item.put("title", r.get("title"));
            item.put("summary", r.get("summary"));
            item.put("advisorTeacherId", r.get("advisor_teacher_id"));
            item.put("advisorName", r.get("advisor_name"));

            String avatarPath = (String) r.get("student_avatar_path");
            item.put("avatarPath", avatarPath);
            item.put("avatarUrl", avatarPath != null ? "/avatar/view?path=" + avatarPath : null);

            // 判断是哪个志愿，并取对应文件路径
            Long c1 = getLong(r.get("choice1_teacher_id"));
            Long c2 = getLong(r.get("choice2_teacher_id"));
            Long c3 = getLong(r.get("choice3_teacher_id"));
            int volunteerRound = 0;
            String filePath = null;
            if (teacher.getId().equals(c1)) { volunteerRound = 1; filePath = (String) r.get("file1_path"); }
            else if (teacher.getId().equals(c2)) { volunteerRound = 2; filePath = (String) r.get("file2_path"); }
            else if (teacher.getId().equals(c3)) { volunteerRound = 3; filePath = (String) r.get("file3_path"); }
            item.put("volunteerRound", volunteerRound);
            item.put("filePath", filePath);

            boolean assignedToSomeone = r.get("advisor_teacher_id") != null;
            boolean assignedToMe = assignedToSomeone && String.valueOf(r.get("advisor_teacher_id"))
                    .equals(String.valueOf(teacher.getId()));
            boolean canAccept = !deadlinePassed
                    && !assignedToSomeone
                    && assigned < maxStudents;
            boolean canCancel = !deadlinePassed
                    && assignedToMe;
            item.put("assignedToMe", assignedToMe);
            item.put("assignedToSomeone", assignedToSomeone);
            item.put("canAccept", canAccept);
            item.put("canCancel", canCancel);

            if (studentId != null) {
                Student student = studentMapper.findById(studentId);
                StudentPreference preference = studentPreferenceMapper.findByStudentIdAndYear(studentId, year);
                MatchScoreDetail matchDetail = volunteerMatchService.calculateMatchDetail(preference, student, teacher,
                        teacherProfile, remaining, assigned, maxStudents, relevanceCache);
                item.put("matchScore", Math.round(matchDetail.getTotalScore() * 100.0) / 100.0);
                item.put("relevanceScore", Math.round(matchDetail.getRelevanceScore() * 100.0) / 100.0);
                item.put("relevanceSource", matchDetail.getRelevanceSource());
                item.put("relevanceReason", matchDetail.getRelevanceReason());
                item.put("materialSource", matchDetail.getMaterialSource());
                item.put("materialReason", matchDetail.getMaterialReason());
            } else {
                item.put("matchScore", 0.0);
                item.put("relevanceScore", 0.0);
                item.put("relevanceSource", RelevanceAnalysisResult.SOURCE_EMPTY_INPUT);
                item.put("relevanceReason", "student_id_missing");
                item.put("materialSource", "EMPTY");
                item.put("materialReason", "student_id_missing");
            }
            list.add(item);
        }
        list.sort((a, b) -> {
            int cmp = Double.compare(getDouble(b.get("matchScore")), getDouble(a.get("matchScore")));
            if (cmp != 0) return cmp;
            return String.valueOf(a.getOrDefault("studentNo", "")).compareTo(String.valueOf(b.getOrDefault("studentNo", "")));
        });

        result.put("year", year);
        result.put("assignedCount", assigned);
        result.put("maxStudents", maxStudents);
        result.put("deadlinePassed", deadlinePassed);
        result.put("items", list);
        return result;
    }

    @PostMapping("/accept")
    @ResponseBody
    public String acceptVolunteer(@RequestParam Long studentId,
                                  @RequestParam Integer round,
                                  HttpSession session) {
        Teacher teacher = getCurrentTeacher(session);
        if (teacher == null) {
            return "error:未登录或非教师身份";
        }
        if (round == null || round < 1 || round > 3) {
            return "error:round参数必须为1-3";
        }
        if (isDeadlinePassed()) {
            return "error:志愿录取已截止";
        }
        Integer year = getCurrentYear();
        Student student = studentMapper.findById(studentId);
        if (student == null || !year.equals(student.getDefenseYear())) {
            return "error:学生不存在或年份不匹配";
        }
        if (student.getAdvisorTeacherId() != null) {
            return "error:该学生已被导师录取";
        }

        StudentPreference pref = studentPreferenceMapper.findByStudentIdAndYear(studentId, year);
        if (pref == null) {
            return "error:学生未提交志愿";
        }
        Long expectedTeacherId = null;
        if (round == 1) expectedTeacherId = pref.getChoice1TeacherId();
        if (round == 2) expectedTeacherId = pref.getChoice2TeacherId();
        if (round == 3) expectedTeacherId = pref.getChoice3TeacherId();
        if (expectedTeacherId == null || !expectedTeacherId.equals(teacher.getId())) {
            return "error:该学生未选择您作为本轮志愿导师";
        }

        int maxStudents = getMaxStudents();
        int assigned = studentMapper.countByAdvisorAndYear(teacher.getId(), year);
        if (assigned >= maxStudents) {
            return "error:已达到可带学生上限";
        }

        try {
            studentService.assignAdvisor(studentId, teacher.getId());
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @PostMapping("/cancel")
    @ResponseBody
    public String cancelVolunteer(@RequestParam Long studentId,
                                  @RequestParam Integer round,
                                  HttpSession session) {
        Teacher teacher = getCurrentTeacher(session);
        if (teacher == null) {
            return "error:未登录或非教师身份";
        }
        if (round == null || round < 1 || round > 3) {
            return "error:round参数必须为1-3";
        }
        if (isDeadlinePassed()) {
            return "error:志愿录取已截止";
        }
        Integer year = getCurrentYear();
        Student student = studentMapper.findById(studentId);
        if (student == null || !year.equals(student.getDefenseYear())) {
            return "error:学生不存在或年份不匹配";
        }
        if (student.getAdvisorTeacherId() == null || !student.getAdvisorTeacherId().equals(teacher.getId())) {
            return "error:该学生并非由您录取";
        }

        StudentPreference pref = studentPreferenceMapper.findByStudentIdAndYear(studentId, year);
        if (pref == null) {
            return "error:学生未提交志愿";
        }
        Long expectedTeacherId = null;
        if (round == 1) expectedTeacherId = pref.getChoice1TeacherId();
        if (round == 2) expectedTeacherId = pref.getChoice2TeacherId();
        if (round == 3) expectedTeacherId = pref.getChoice3TeacherId();
        if (expectedTeacherId == null || !expectedTeacherId.equals(teacher.getId())) {
            return "error:该学生未选择您作为本轮志愿导师";
        }

        try {
            studentService.unassignAdvisor(studentId);
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @GetMapping("/file")
    public ResponseEntity<byte[]> downloadVolunteerFile(@RequestParam Long studentId,
                                                        @RequestParam Integer round,
                                                        HttpSession session) {
        Teacher teacher = getCurrentTeacher(session);
        if (teacher == null) {
            return ResponseEntity.status(403).build();
        }
        if (round == null || round < 1 || round > 3) {
            return ResponseEntity.badRequest().build();
        }
        Integer year = getCurrentYear();
        StudentPreference pref = studentPreferenceMapper.findByStudentIdAndYear(studentId, year);
        if (pref == null) {
            return ResponseEntity.notFound().build();
        }

        Long expectedTeacherId;
        String filePath;
        if (round == 1) {
            expectedTeacherId = pref.getChoice1TeacherId();
            filePath = pref.getFile1Path();
        } else if (round == 2) {
            expectedTeacherId = pref.getChoice2TeacherId();
            filePath = pref.getFile2Path();
        } else {
            expectedTeacherId = pref.getChoice3TeacherId();
            filePath = pref.getFile3Path();
        }

        if (expectedTeacherId == null || !expectedTeacherId.equals(teacher.getId())) {
            return ResponseEntity.status(403).build();
        }
        if (filePath == null || filePath.trim().isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!filePath.replace("\\", "/").startsWith("uploads/volunteer/")) {
            return ResponseEntity.status(403).build();
        }

        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return ResponseEntity.notFound().build();
            }
            byte[] bytes = Files.readAllBytes(path);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", path.getFileName().toString());
            headers.setContentLength(bytes.length);
            return ResponseEntity.ok().headers(headers).body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    private Long getLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private double getDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble(((String) value).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0.0;
    }
}
