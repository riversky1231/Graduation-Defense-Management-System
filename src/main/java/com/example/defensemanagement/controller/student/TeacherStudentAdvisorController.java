package com.example.defensemanagement.controller.student;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.ScoreService;
import com.example.defensemanagement.service.StudentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

/**
 * 教师指导操作 Controller。
 * 处理教师分配评阅人、更新学生题目与摘要等接口。
 */
@RestController
@RequestMapping("/department/student")
public class TeacherStudentAdvisorController extends AbstractTeacherStudentController {

    private static final Logger log = LoggerFactory.getLogger(TeacherStudentAdvisorController.class);
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_SUMMARY_LENGTH = 2000;

    public TeacherStudentAdvisorController(
            StudentService studentService,
            ConfigService configService,
            TeacherMapper teacherMapper,
            StudentFinalScoreMapper studentFinalScoreMapper,
            ScoreService scoreService,
            TeacherScoreRecordMapper teacherScoreRecordMapper) {
        super(studentService, configService, teacherMapper, studentFinalScoreMapper, scoreService, teacherScoreRecordMapper);
    }

    @PostMapping("/teacher/assignReviewer")
    @ResponseBody
    public String assignReviewerByTeacher(
            @RequestParam Long studentId,
            @RequestParam Long reviewerId,
            HttpSession session) {
        Teacher teacher = getTeacherFromSession(session);
        if (teacher == null) {
            return "error:请先登录教师账号";
        }

        Student student = studentService.findById(studentId);
        if (student == null) {
            return "error:学生不存在";
        }
        if (student.getAdvisorTeacherId() == null || !student.getAdvisorTeacherId().equals(teacher.getId())) {
            return "error:您不是该学生的指导教师";
        }

        try {
            studentService.assignReviewer(studentId, reviewerId);
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @PostMapping("/teacher/updateStudentInfo")
    @ResponseBody
    public String updateStudentInfoByTeacher(
            @RequestParam Long studentId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String summary,
            HttpSession session) {
        Teacher teacher = getTeacherFromSession(session);
        if (teacher == null) {
            return "error:请先登录教师账号";
        }
        if (title != null && title.length() > MAX_TITLE_LENGTH) {
            return "error:题目不能超过" + MAX_TITLE_LENGTH + "个字符";
        }
        if (summary != null && summary.length() > MAX_SUMMARY_LENGTH) {
            return "error:摘要不能超过" + MAX_SUMMARY_LENGTH + "个字符";
        }

        try {
            Student student = studentService.findById(studentId);
            if (student == null) {
                return "error:学生不存在";
            }
            if (student.getAdvisorTeacherId() == null || !student.getAdvisorTeacherId().equals(teacher.getId())) {
                return "error:您不是该学生的指导教师";
            }

            Student update = new Student();
            update.setId(studentId);
            update.setTitle(title);
            update.setSummary(summary);
            studentService.saveStudent(update);
            return "success";
        } catch (DataAccessException e) {
            log.error("更新学生题目摘要时发生数据库错误，studentId={}", studentId, e);
            return "error:数据库操作失败，请稍后重试";
        } catch (Exception e) {
            log.error("更新学生题目摘要时发生未知错误，studentId={}", studentId, e);
            return "error:系统错误，请稍后重试";
        }
    }
}
