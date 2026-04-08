package com.example.defensemanagement.controller.group;

import com.example.defensemanagement.common.RelevanceAnalysisResult;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentPreference;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.TeacherProfile;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.StudentPreferenceMapper;
import com.example.defensemanagement.mapper.TeacherProfileMapper;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.StudentService;
import com.example.defensemanagement.service.TeacherService;
import com.example.defensemanagement.service.VolunteerMatchService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 院系管理员志愿分配 Controller。
 */
@RestController
@RequestMapping("/department/group")
public class GroupVolunteerAllocationController extends AbstractGroupAssignmentController {

    private final StudentPreferenceMapper studentPreferenceMapper;
    private final TeacherProfileMapper teacherProfileMapper;
    private final StudentService studentService;
    private final VolunteerMatchService volunteerMatchService;

    public GroupVolunteerAllocationController(
            AuthService authService,
            TeacherService teacherService,
            DefenseGroupMapper defenseGroupMapper,
            DefenseGroupTeacherMapper defenseGroupTeacherMapper,
            DepartmentMapper departmentMapper,
            ConfigService configService,
            StudentMapper studentMapper,
            StudentPreferenceMapper studentPreferenceMapper,
            TeacherProfileMapper teacherProfileMapper,
            StudentService studentService,
            VolunteerMatchService volunteerMatchService) {
        super(authService, teacherService, defenseGroupMapper, defenseGroupTeacherMapper, departmentMapper, configService, studentMapper);
        this.studentPreferenceMapper = studentPreferenceMapper;
        this.teacherProfileMapper = teacherProfileMapper;
        this.studentService = studentService;
        this.volunteerMatchService = volunteerMatchService;
    }

    @PostMapping("/allocate")
    public Map<String, Object> allocateVolunteerAssignments(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User admin = requireDeptAdmin(session);
        if (admin == null) {
            result.put("error", "权限不足");
            return result;
        }
        Long departmentId = admin.getDepartmentId();
        if (departmentId == null) {
            result.put("error", "未绑定院系");
            return result;
        }

        int maxStudents = getGroupMaxStudents();
        Integer year = configService.getCurrentDefenseYear();
        if (year == null) {
            year = java.time.LocalDate.now().getYear();
        }

        List<Teacher> teachers = teacherService.findByDepartmentId(departmentId);
        Map<Long, Integer> remaining = new HashMap<>();
        Map<Long, Integer> assignedCounts = new HashMap<>();
        Map<Long, Teacher> teacherMap = new HashMap<>();
        Map<Long, TeacherProfile> teacherProfileMap = new HashMap<>();
        if (teachers != null) {
            for (Teacher teacher : teachers) {
                teacherMap.put(teacher.getId(), teacher);
                int currentCount = studentMapper.countByAdvisorAndYear(teacher.getId(), year);
                assignedCounts.put(teacher.getId(), currentCount);
                remaining.put(teacher.getId(), Math.max(0, maxStudents - currentCount));
                teacherProfileMap.put(teacher.getId(), teacherProfileMapper.findByTeacherId(teacher.getId()));
            }
        }

        List<Map<String, Object>> candidates = studentPreferenceMapper.findByDepartmentAndYear(departmentId, year, true);
        List<Map<String, Object>> specifiedList = new ArrayList<>();
        List<Map<String, Object>> randomList = new ArrayList<>();
        if (candidates != null) {
            for (Map<String, Object> item : candidates) {
                Integer assignType = getInt(item, "admin_assign_type", "adminAssignType");
                Long assignedTeacherId = getLong(item, "admin_assigned_teacher_id", "adminAssignedTeacherId");
                if (assignType != null && assignType == 1 && assignedTeacherId != null) {
                    specifiedList.add(item);
                } else {
                    randomList.add(item);
                }
            }
        }

        int assignedCount = 0;
        int specifiedAssigned = 0;
        int randomAssigned = 0;
        List<Map<String, Object>> failures = new ArrayList<>();
        Map<String, RelevanceAnalysisResult> relevanceCache = new HashMap<>();

        for (Map<String, Object> item : specifiedList) {
            Long studentId = getLong(item, "student_id", "studentId");
            Long teacherId = getLong(item, "admin_assigned_teacher_id", "adminAssignedTeacherId");
            if (studentId == null || teacherId == null) {
                continue;
            }
            if (!teacherMap.containsKey(teacherId)) {
                failures.add(buildFailure(studentId, "指定导师不在本院系"));
                continue;
            }
            int remain = remaining.getOrDefault(teacherId, 0);
            if (remain <= 0) {
                failures.add(buildFailure(studentId, "指定导师名额已满"));
                continue;
            }
            studentService.assignAdvisor(studentId, teacherId);
            remaining.put(teacherId, remain - 1);
            assignedCounts.put(teacherId, assignedCounts.getOrDefault(teacherId, 0) + 1);
            assignedCount++;
            specifiedAssigned++;
        }

        for (Map<String, Object> item : randomList) {
            Long studentId = getLong(item, "student_id", "studentId");
            if (studentId == null) {
                continue;
            }
            Student student = studentMapper.findById(studentId);
            if (student == null) {
                failures.add(buildFailure(studentId, "学生不存在"));
                continue;
            }

            StudentPreference preference = studentPreferenceMapper.findByStudentIdAndYear(studentId, year);
            Long bestTeacherId = selectBestTeacher(student, teacherMap, teacherProfileMap, remaining,
                    assignedCounts, maxStudents, preference, relevanceCache);
            if (bestTeacherId == null) {
                failures.add(buildFailure(studentId, "无可用导师名额"));
                continue;
            }
            studentService.assignAdvisor(studentId, bestTeacherId);
            remaining.put(bestTeacherId, remaining.get(bestTeacherId) - 1);
            assignedCounts.put(bestTeacherId, assignedCounts.getOrDefault(bestTeacherId, 0) + 1);
            assignedCount++;
            randomAssigned++;
        }

        result.put("assignedCount", assignedCount);
        result.put("specifiedAssigned", specifiedAssigned);
        result.put("randomAssigned", randomAssigned);
        result.put("failures", failures);
        return result;
    }

    private Long selectBestTeacher(Student student,
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

            double score = volunteerMatchService.calculateMatchScore(preference, student, entry.getValue(),
                    teacherProfileMap.get(teacherId), remain, assignedCounts.getOrDefault(teacherId, 0), maxStudents,
                    relevanceCache);

            if (score > bestScore) {
                bestScore = score;
                bestTeacherId = teacherId;
            }
        }
        return bestTeacherId;
    }

    private Map<String, Object> buildFailure(Long studentId, String reason) {
        Map<String, Object> fail = new HashMap<>();
        fail.put("studentId", studentId);
        fail.put("reason", reason);
        return fail;
    }

    private Long getLong(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            if (value instanceof String) {
                String text = ((String) value).trim();
                if (!text.isEmpty()) {
                    try {
                        return Long.parseLong(text);
                    } catch (NumberFormatException ignored) {
                        // try next key
                    }
                }
            }
        }
        return null;
    }

    private Integer getInt(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            if (value instanceof String) {
                String text = ((String) value).trim();
                if (!text.isEmpty()) {
                    try {
                        return Integer.parseInt(text);
                    } catch (NumberFormatException ignored) {
                        // try next key
                    }
                }
            }
        }
        return null;
    }
}
