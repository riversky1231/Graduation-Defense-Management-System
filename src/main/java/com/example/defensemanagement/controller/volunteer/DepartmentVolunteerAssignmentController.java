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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 院系志愿分配端点。
 */
@Controller
@RequestMapping("/department/volunteer")
public class DepartmentVolunteerAssignmentController extends AbstractDepartmentVolunteerController {

    public DepartmentVolunteerAssignmentController(
            ConfigService configService,
            StudentPreferenceMapper studentPreferenceMapper,
            StudentMapper studentMapper,
            TeacherMapper teacherMapper,
            TeacherProfileMapper teacherProfileMapper,
            StudentService studentService,
            VolunteerMatchService volunteerMatchService) {
        super(
                configService,
                studentPreferenceMapper,
                studentMapper,
                teacherMapper,
                teacherProfileMapper,
                studentService,
                volunteerMatchService);
    }

    @PostMapping("/assign")
    @ResponseBody
    public Map<String, Object> saveAdminAssignment(@RequestBody Map<String, Object> payload, HttpSession session) {
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

        Integer year = getCurrentYear();
        Student student = studentMapper.findById(studentId);
        if (student == null || student.getDepartmentId() == null || !student.getDepartmentId().equals(departmentId)) {
            result.put("error", "学生不存在或不在本院系");
            return result;
        }
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

        StudentPreference preference = studentPreferenceMapper.findByStudentIdAndYear(studentId, year);
        if (preference == null) {
            preference = new StudentPreference();
            preference.setStudentId(studentId);
            preference.setYear(year);
            preference.setStatus(0);
            preference.setAdminAssignType(assignType);
            preference.setAdminAssignedTeacherId(assignedTeacherId);
            studentPreferenceMapper.insert(preference);
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
            for (Teacher teacher : teachers) {
                teacherMap.put(teacher.getId(), teacher);
                int currentCount = studentMapper.countByAdvisorAndYear(teacher.getId(), year);
                assignedCounts.put(teacher.getId(), currentCount);
                remaining.put(teacher.getId(), Math.max(0, maxStudents - currentCount));
                teacherProfileMap.put(teacher.getId(), teacherProfileMapper.findByTeacherId(teacher.getId()));
            }
        }

        List<Map<String, Object>> candidates =
                studentPreferenceMapper.findByDepartmentAndYear(departmentId, year, true);
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
            Long bestTeacherId = selectBestTeacher(
                    student,
                    teacherMap,
                    teacherProfileMap,
                    remaining,
                    assignedCounts,
                    maxStudents,
                    preference,
                    relevanceCache);
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
}
