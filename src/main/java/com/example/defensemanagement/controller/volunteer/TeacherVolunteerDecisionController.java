package com.example.defensemanagement.controller.volunteer;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentPreference;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.StudentPreferenceMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherProfileMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.StudentService;
import com.example.defensemanagement.service.VolunteerMatchService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

/**
 * 教师志愿录取操作 Controller。
 * 处理录取和取消录取接口。
 */
@RestController
@RequestMapping("/teacher/volunteer")
public class TeacherVolunteerDecisionController extends AbstractTeacherVolunteerController {

    public TeacherVolunteerDecisionController(
            TeacherMapper teacherMapper,
            StudentPreferenceMapper studentPreferenceMapper,
            StudentMapper studentMapper,
            StudentService studentService,
            ConfigService configService,
            TeacherProfileMapper teacherProfileMapper,
            VolunteerMatchService volunteerMatchService) {
        super(
                teacherMapper,
                studentPreferenceMapper,
                studentMapper,
                studentService,
                configService,
                teacherProfileMapper,
                volunteerMatchService);
    }

    @PostMapping("/accept")
    public String acceptVolunteer(@RequestParam Long studentId,
                                  @RequestParam Integer round,
                                  HttpSession session) {
        Teacher teacher = getCurrentTeacher(session);
        if (teacher == null) {
            return "error:未登录或非教师身份";
        }

        String roundError = validateRound(round);
        if (roundError != null) {
            return "error:" + roundError;
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

        StudentPreference preference = studentPreferenceMapper.findByStudentIdAndYear(studentId, year);
        if (preference == null) {
            return "error:学生未提交志愿";
        }

        VolunteerRoundSelection selection = resolveSelection(preference, round);
        if (selection.getTeacherId() == null || !selection.getTeacherId().equals(teacher.getId())) {
            return "error:该学生未选择您作为本轮志愿导师";
        }

        int maxStudents = getMaxStudents();
        int assigned = studentMapper.countByAdvisorAndYear(teacher.getId(), year);
        if (assigned >= maxStudents) {
            return "error:已达到可带学生上限";
        }

        try {
            studentService.assignAdvisor(studentId, teacher.getId());
            return SUCCESS;
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @PostMapping("/cancel")
    public String cancelVolunteer(@RequestParam Long studentId,
                                  @RequestParam Integer round,
                                  HttpSession session) {
        Teacher teacher = getCurrentTeacher(session);
        if (teacher == null) {
            return "error:未登录或非教师身份";
        }

        String roundError = validateRound(round);
        if (roundError != null) {
            return "error:" + roundError;
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

        StudentPreference preference = studentPreferenceMapper.findByStudentIdAndYear(studentId, year);
        if (preference == null) {
            return "error:学生未提交志愿";
        }

        VolunteerRoundSelection selection = resolveSelection(preference, round);
        if (selection.getTeacherId() == null || !selection.getTeacherId().equals(teacher.getId())) {
            return "error:该学生未选择您作为本轮志愿导师";
        }

        try {
            studentService.unassignAdvisor(studentId);
            return SUCCESS;
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }
}
