package com.example.defensemanagement.controller.student;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.ScoreService;
import com.example.defensemanagement.service.StudentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 教师侧学生查询 Controller。
 * 处理指导学生、评阅学生和评阅人候选列表等只读接口。
 */
@RestController
@RequestMapping("/department/student")
public class TeacherStudentQueryController extends AbstractTeacherStudentController {

    public TeacherStudentQueryController(
            StudentService studentService,
            ConfigService configService,
            TeacherMapper teacherMapper,
            StudentFinalScoreMapper studentFinalScoreMapper,
            ScoreService scoreService,
            TeacherScoreRecordMapper teacherScoreRecordMapper) {
        super(studentService, configService, teacherMapper, studentFinalScoreMapper, scoreService, teacherScoreRecordMapper);
    }

    @GetMapping("/teacher/advised")
    @ResponseBody
    public Map<String, Object> getAdvisedStudentsWithScores(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User currentUser = getCurrentUser(session);
        Teacher teacher = getTeacherFromSession(session);
        Integer currentYear = configService.getCurrentDefenseYear();

        List<Student> students;
        if (isSuperAdmin(currentUser)) {
            students = currentYear == null ? studentService.findAll() : studentService.findByYear(currentYear);
            result.put("teacherId", null);
            result.put("teacherName", "超级管理员");
        } else {
            if (teacher == null) {
                result.put("error", "请先登录教师账号");
                return result;
            }
            if (currentYear == null) {
                result.put("error", "请先设置当前答辩年份");
                return result;
            }
            students = studentService.getStudentsByAdvisor(teacher.getId(), currentYear);
            result.put("teacherId", teacher.getId());
            result.put("teacherName", teacher.getName());
        }

        result.putAll(buildStudentListResponse(students, currentYear, false, true));
        return result;
    }

    @GetMapping("/teacher/reviewed")
    @ResponseBody
    public Map<String, Object> getReviewedStudentsWithScores(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User currentUser = getCurrentUser(session);
        Teacher teacher = getTeacherFromSession(session);
        Integer currentYear = configService.getCurrentDefenseYear();

        List<Student> students;
        if (isSuperAdmin(currentUser)) {
            students = currentYear == null ? studentService.findAll() : studentService.findByYear(currentYear);
            result.put("teacherId", null);
            result.put("teacherName", "超级管理员");
        } else if (isDeptAdmin(currentUser)) {
            students = studentService.findByDepartmentAndYear(currentUser.getDepartmentId(), currentYear);
            result.put("teacherId", null);
            result.put("teacherName", "院系管理员");
            result.put("isDeptAdmin", true);
        } else {
            if (teacher == null) {
                result.put("error", "请先登录教师账号");
                return result;
            }
            if (currentYear == null) {
                result.put("error", "请先设置当前答辩年份");
                return result;
            }
            students = studentService.getStudentsByReviewer(teacher.getId(), currentYear);
            result.put("teacherId", teacher.getId());
            result.put("teacherName", teacher.getName());
        }

        result.putAll(buildStudentListResponse(students, currentYear, true, true));
        return result;
    }

    @GetMapping("/teacher/reviewerCandidates")
    @ResponseBody
    public List<Map<String, Object>> getReviewerCandidates(HttpSession session) {
        Teacher currentTeacher = getTeacherFromSession(session);
        User currentUser = getCurrentUser(session);
        Long departmentId = currentTeacher != null ? currentTeacher.getDepartmentId()
                : currentUser != null ? currentUser.getDepartmentId() : null;

        List<Map<String, Object>> candidates = new ArrayList<>();
        List<Teacher> teachers = teacherMapper.findByDepartmentId(departmentId);
        if (teachers == null) {
            return candidates;
        }

        for (Teacher teacher : teachers) {
            if (currentTeacher != null && teacher.getId().equals(currentTeacher.getId())) {
                continue;
            }
            Map<String, Object> info = new HashMap<>();
            info.put("id", teacher.getId());
            info.put("teacherNo", teacher.getTeacherNo());
            info.put("name", teacher.getName());
            info.put("title", teacher.getTitle());
            candidates.add(info);
        }
        return candidates;
    }
}
