package com.example.defensemanagement.controller.volunteer;

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
import java.util.HashMap;
import java.util.Map;

/**
 * 院系志愿分配 Controller 共享基类。
 */
public abstract class AbstractDepartmentVolunteerController {

    protected static final int ASSIGN_RANDOM = 0;
    protected static final int ASSIGN_SPECIFIED = 1;

    protected final ConfigService configService;
    protected final StudentPreferenceMapper studentPreferenceMapper;
    protected final StudentMapper studentMapper;
    protected final TeacherMapper teacherMapper;
    protected final TeacherProfileMapper teacherProfileMapper;
    protected final StudentService studentService;
    protected final VolunteerMatchService volunteerMatchService;

    protected AbstractDepartmentVolunteerController(
            ConfigService configService,
            StudentPreferenceMapper studentPreferenceMapper,
            StudentMapper studentMapper,
            TeacherMapper teacherMapper,
            TeacherProfileMapper teacherProfileMapper,
            StudentService studentService,
            VolunteerMatchService volunteerMatchService) {
        this.configService = configService;
        this.studentPreferenceMapper = studentPreferenceMapper;
        this.studentMapper = studentMapper;
        this.teacherMapper = teacherMapper;
        this.teacherProfileMapper = teacherProfileMapper;
        this.studentService = studentService;
        this.volunteerMatchService = volunteerMatchService;
    }

    protected User getCurrentDeptAdmin(HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null || user.getRole() == null) {
            return null;
        }
        String roleName = user.getRole().getName();
        if (!"DEPT_ADMIN".equals(roleName) && !"SUPER_ADMIN".equals(roleName)) {
            return null;
        }
        return user;
    }

    protected Integer getCurrentYear() {
        Integer year = configService.getCurrentDefenseYear();
        return year != null ? year : java.time.LocalDate.now().getYear();
    }

    protected Integer getTeacherMaxStudents() {
        String value = configService.getConfigValue(ConfigServiceImpl.KEY_TEACHER_MAX_STUDENTS);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected Long selectBestTeacher(
            Student student,
            Map<Long, Teacher> teacherMap,
            Map<Long, TeacherProfile> teacherProfileMap,
            Map<Long, Integer> remaining,
            Map<Long, Integer> assignedCounts,
            int maxStudents,
            StudentPreference preference,
            Map<String, RelevanceAnalysisResult> relevanceCache) {
        Long bestTeacherId = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Map.Entry<Long, Teacher> entry : teacherMap.entrySet()) {
            Long teacherId = entry.getKey();
            Integer remain = remaining.get(teacherId);
            if (remain == null || remain <= 0) {
                continue;
            }

            double score = volunteerMatchService.calculateMatchScore(
                    preference,
                    student,
                    entry.getValue(),
                    teacherProfileMap.get(teacherId),
                    remain,
                    assignedCounts.getOrDefault(teacherId, 0),
                    maxStudents,
                    relevanceCache);
            if (score > bestScore) {
                bestScore = score;
                bestTeacherId = teacherId;
            }
        }

        return bestTeacherId;
    }

    protected Map<String, Object> buildFailure(Long studentId, String reason) {
        Map<String, Object> failure = new HashMap<>();
        failure.put("studentId", studentId);
        failure.put("reason", reason);
        return failure;
    }

    protected Long getLong(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            if (value instanceof String) {
                String text = ((String) value).trim();
                if (!text.isEmpty()) {
                    try {
                        return Long.parseLong(text);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return null;
    }

    protected Integer getInt(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            if (value instanceof String) {
                String text = ((String) value).trim();
                if (!text.isEmpty()) {
                    try {
                        return Integer.parseInt(text);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return null;
    }
}
