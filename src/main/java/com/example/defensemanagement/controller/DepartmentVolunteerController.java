package com.example.defensemanagement.controller;

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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.*;

@Controller
@RequestMapping("/department/volunteer")
public class DepartmentVolunteerController {

    private static final int ASSIGN_RANDOM = 0;
    private static final int ASSIGN_SPECIFIED = 1;

    @Autowired
    private ConfigService configService;

    @Autowired
    private StudentPreferenceMapper studentPreferenceMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private TeacherProfileMapper teacherProfileMapper;

    @Autowired
    private StudentService studentService;

    @Autowired
    private VolunteerMatchService volunteerMatchService;

    private User getCurrentDeptAdmin(HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null || user.getRole() == null) {
            return null;
        }
        String role = user.getRole().getName();
        if (!"DEPT_ADMIN".equals(role) && !"SUPER_ADMIN".equals(role)) {
            return null;
        }
        return user;
    }

    private Integer getCurrentYear() {
        Integer year = configService.getCurrentDefenseYear();
        return year != null ? year : java.time.LocalDate.now().getYear();
    }

    private Integer getTeacherMaxStudents() {
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

    @GetMapping("/teachers")
    @ResponseBody
    public Object listDepartmentTeachers(HttpSession session) {
        User admin = getCurrentDeptAdmin(session);
        if (admin == null) {
            return Collections.singletonMap("error", "无权限");
        }
        Long departmentId = admin.getDepartmentId();
        if (departmentId == null) {
            return Collections.singletonMap("error", "未绑定院系");
        }
        List<Teacher> teachers = teacherMapper.findByDepartmentId(departmentId);
        List<Map<String, Object>> list = new ArrayList<>();
        if (teachers != null) {
            for (Teacher t : teachers) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", t.getId());
                item.put("teacherNo", t.getTeacherNo());
                item.put("name", t.getName());
                item.put("title", t.getTitle());
                list.add(item);
            }
        }
        return list;
    }

    @GetMapping("/students")
    @ResponseBody
    public Map<String, Object> listDepartmentStudents(@RequestParam(required = false) Boolean unassignedOnly,
                                                      HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User admin = getCurrentDeptAdmin(session);
        if (admin == null) {
            result.put("error", "无权限");
            return result;
        }
        Long departmentId = admin.getDepartmentId();
        if (departmentId == null) {
            result.put("error", "未绑定院系");
            return result;
        }
        Integer year = getCurrentYear();
        List<Map<String, Object>> students = studentPreferenceMapper
                .findByDepartmentAndYear(departmentId, year, unassignedOnly);
        result.put("students", students);
        result.put("year", year);
        result.put("maxStudents", getTeacherMaxStudents());
        return result;
    }

    @PostMapping("/assign")
    @ResponseBody
    public Map<String, Object> saveAdminAssignment(@RequestBody Map<String, Object> payload,
                                                   HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User admin = getCurrentDeptAdmin(session);
        if (admin == null) {
            result.put("error", "无权限");
            return result;
        }
        Long departmentId = admin.getDepartmentId();
        if (departmentId == null) {
            result.put("error", "未绑定院系");
            return result;
        }

        Long studentId = getLong(payload, "studentId");
        Integer assignType = getInt(payload, "adminAssignType");
        Long assignedTeacherId = getLong(payload, "adminAssignedTeacherId");

        if (studentId == null) {
            result.put("error", "studentId不能为空");
            return result;
        }
        if (assignType == null) {
            assignType = ASSIGN_RANDOM;
        }
        if (assignType != ASSIGN_RANDOM && assignType != ASSIGN_SPECIFIED) {
            result.put("error", "adminAssignType不合法");
            return result;
        }
        if (assignType == ASSIGN_SPECIFIED && assignedTeacherId == null) {
            result.put("error", "指定分配需要选择导师");
            return result;
        }

        Student student = studentMapper.findById(studentId);
        if (student == null || student.getDepartmentId() == null
                || !student.getDepartmentId().equals(departmentId)) {
            result.put("error", "学生不存在或不在本院系");
            return result;
        }
        Integer year = getCurrentYear();
        if (student.getDefenseYear() == null || !student.getDefenseYear().equals(year)) {
            result.put("error", "学生不在当前答辩年份");
            return result;
        }

        if (assignedTeacherId != null) {
            Teacher teacher = teacherMapper.findById(assignedTeacherId);
            if (teacher == null || teacher.getDepartmentId() == null
                    || !teacher.getDepartmentId().equals(departmentId)) {
                result.put("error", "指定导师不存在或不在本院系");
                return result;
            }
        }

        StudentPreference pref = studentPreferenceMapper.findByStudentIdAndYear(studentId, year);
        if (pref == null) {
            pref = new StudentPreference();
            pref.setStudentId(studentId);
            pref.setYear(year);
            pref.setStatus(0);
            pref.setAdminAssignType(assignType);
            pref.setAdminAssignedTeacherId(assignedTeacherId);
            studentPreferenceMapper.insert(pref);
        } else {
            studentPreferenceMapper.updateAdminAssignment(studentId, year, assignType, assignedTeacherId);
        }

        result.put("success", true);
        return result;
    }

    @PostMapping("/allocate")
    @ResponseBody
    public Map<String, Object> allocateVolunteerAssignments(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User admin = getCurrentDeptAdmin(session);
        if (admin == null) {
            result.put("error", "无权限");
            return result;
        }
        Long departmentId = admin.getDepartmentId();
        if (departmentId == null) {
            result.put("error", "未绑定院系");
            return result;
        }
        Integer maxStudents = getTeacherMaxStudents();
        if (maxStudents == null || maxStudents <= 0) {
            result.put("error", "请先设置导师可带学生上限");
            return result;
        }

        Integer year = getCurrentYear();
        List<Teacher> teachers = teacherMapper.findByDepartmentId(departmentId);
        Map<Long, Integer> remaining = new HashMap<>();
        Map<Long, Integer> assignedCounts = new HashMap<>();
        Map<Long, Teacher> teacherMap = new HashMap<>();
        Map<Long, TeacherProfile> teacherProfileMap = new HashMap<>();
        if (teachers != null) {
            for (Teacher t : teachers) {
                teacherMap.put(t.getId(), t);
                int currentCount = studentMapper.countByAdvisorAndYear(t.getId(), year);
                assignedCounts.put(t.getId(), currentCount);
                remaining.put(t.getId(), Math.max(0, maxStudents - currentCount));
                teacherProfileMap.put(t.getId(), teacherProfileMapper.findByTeacherId(t.getId()));
            }
        }

        List<Map<String, Object>> candidates = studentPreferenceMapper
                .findByDepartmentAndYear(departmentId, year, true);

        List<Map<String, Object>> specifiedList = new ArrayList<>();
        List<Map<String, Object>> randomList = new ArrayList<>();
        for (Map<String, Object> item : candidates) {
            Integer assignType = getInt(item, "admin_assign_type", "adminAssignType");
            Long assignedTeacherId = getLong(item, "admin_assigned_teacher_id", "adminAssignedTeacherId");
            if (assignType != null && assignType == ASSIGN_SPECIFIED && assignedTeacherId != null) {
                specifiedList.add(item);
            } else {
                randomList.add(item);
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
                String s = ((String) value).trim();
                if (!s.isEmpty()) {
                    try {
                        return Long.parseLong(s);
                    } catch (NumberFormatException ignored) {
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
                String s = ((String) value).trim();
                if (!s.isEmpty()) {
                    try {
                        return Integer.parseInt(s);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return null;
    }
}
