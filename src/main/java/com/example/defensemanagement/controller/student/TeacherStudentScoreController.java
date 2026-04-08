package com.example.defensemanagement.controller.student;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.ScoreService;
import com.example.defensemanagement.service.StudentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 教师评分 Controller。
 * 处理指导/评阅打分、成绩记录读取与明细更新。
 */
@RestController
@RequestMapping("/department/student")
public class TeacherStudentScoreController extends AbstractTeacherStudentController {

    public TeacherStudentScoreController(
            StudentService studentService,
            ConfigService configService,
            TeacherMapper teacherMapper,
            StudentFinalScoreMapper studentFinalScoreMapper,
            ScoreService scoreService,
            TeacherScoreRecordMapper teacherScoreRecordMapper) {
        super(studentService, configService, teacherMapper, studentFinalScoreMapper, scoreService, teacherScoreRecordMapper);
    }

    @PostMapping("/teacher/setAdvisorScore")
    @ResponseBody
    public String setAdvisorScoreByTeacher(
            @RequestParam Long studentId,
            @RequestParam Integer score,
            HttpSession session) {
        return setScoreByTeacher(studentId, score, session, true);
    }

    @PostMapping("/teacher/setReviewerScore")
    @ResponseBody
    public String setReviewerScoreByTeacher(
            @RequestParam Long studentId,
            @RequestParam Integer score,
            HttpSession session) {
        return setScoreByTeacher(studentId, score, session, false);
    }

    @GetMapping("/teacher/scoreRecord")
    @ResponseBody
    public Map<String, Object> getTeacherScoreRecord(
            @RequestParam Long studentId,
            @RequestParam String type,
            HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User currentUser = getCurrentUser(session);
        Teacher currentTeacher = getTeacherFromSession(session);
        Integer currentYear = configService.getCurrentDefenseYear();
        if (currentYear == null) {
            result.put("error", "请先设置当前答辩年份");
            return result;
        }

        Student student = studentService.findById(studentId);
        if (student == null) {
            result.put("error", "学生不存在");
            return result;
        }

        Long teacherId = resolveTeacherIdByType(student, type);
        if (teacherId == null) {
            if (!"advisor".equals(type) && !"reviewer".equals(type)) {
                result.put("error", "类型参数错误，应为advisor或reviewer");
            } else {
                result.put("error", "该学生未分配" + resolveTeacherTypeLabel(type));
            }
            return result;
        }

        if (!isSuperAdmin(currentUser)) {
            if (currentTeacher == null) {
                result.put("error", "权限不足：请先登录教师账号");
                return result;
            }
            if (!currentTeacher.getId().equals(teacherId)) {
                result.put("error", "权限不足：您不是该学生的" + resolveTeacherTypeLabel(type));
                return result;
            }
        }

        TeacherScoreRecord record = teacherScoreRecordMapper.findByStudentIdAndTeacherIdAndYear(
                studentId, teacherId, currentYear);
        if (record == null) {
            record = createEmptyScoreRecord(student, teacherId, currentYear);
        }

        result.put("record", record);
        result.put("student", student);
        Teacher teacher = teacherMapper.findById(teacherId);
        if (teacher != null) {
            result.put("teacherName", teacher.getName());
        }
        return result;
    }

    @PostMapping("/teacher/updateScoreRecord")
    @ResponseBody
    public String updateTeacherScoreRecord(
            @RequestParam Long studentId,
            @RequestParam String type,
            @RequestParam(required = false) Integer item1,
            @RequestParam(required = false) Integer item2,
            @RequestParam(required = false) Integer item3,
            @RequestParam(required = false) Integer item4,
            @RequestParam(required = false) Integer item5,
            @RequestParam(required = false) Integer item6,
            HttpSession session) {
        User currentUser = getCurrentUser(session);
        Teacher currentTeacher = getTeacherFromSession(session);
        Integer currentYear = configService.getCurrentDefenseYear();
        if (currentYear == null) {
            return "error:请先设置当前答辩年份";
        }

        Student student = studentService.findById(studentId);
        if (student == null) {
            return "error:学生不存在";
        }

        Long teacherId = resolveTeacherIdByType(student, type);
        if (teacherId == null) {
            if (!"advisor".equals(type) && !"reviewer".equals(type)) {
                return "error:类型参数错误";
            }
            return "error:该学生未分配" + resolveTeacherTypeLabel(type);
        }

        if (!isAdmin(currentUser)) {
            if (currentTeacher == null) {
                return "error:权限不足：请先登录教师账号";
            }
            if (!currentTeacher.getId().equals(teacherId)) {
                return "error:权限不足：您不是该学生的" + resolveTeacherTypeLabel(type);
            }
        }

        TeacherScoreRecord record = teacherScoreRecordMapper.findByStudentIdAndTeacherIdAndYear(
                studentId, teacherId, currentYear);
        boolean isNewRecord = false;
        if (record == null) {
            record = createEmptyScoreRecord(student, teacherId, currentYear);
            record.setSubmitTime(LocalDateTime.now());
            isNewRecord = true;
        }

        applyScoreItems(record, item1, item2, item3, item4, item5, item6);
        int total = calculateTotalScore(record);
        record.setTotalScore(total);

        try {
            if (isNewRecord) {
                teacherScoreRecordMapper.insert(record);
            } else {
                teacherScoreRecordMapper.update(record);
            }
            if ("advisor".equals(type)) {
                scoreService.setAdvisorScore(studentId, currentYear, total);
            } else {
                scoreService.setReviewerScore(studentId, currentYear, total);
            }
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    private String setScoreByTeacher(Long studentId, Integer score, HttpSession session, boolean advisor) {
        User currentUser = getCurrentUser(session);
        Teacher teacher = getTeacherFromSession(session);
        if (teacher == null && !isSuperAdmin(currentUser)) {
            return "error:请先登录教师账号";
        }

        Integer currentYear = configService.getCurrentDefenseYear();
        if (currentYear == null) {
            return "error:请先设置当前答辩年份";
        }

        Student student = studentService.findById(studentId);
        if (student == null) {
            return "error:学生不存在";
        }

        Long ownerTeacherId = advisor ? student.getAdvisorTeacherId() : student.getReviewerTeacherId();
        if (!isSuperAdmin(currentUser)
                && teacher != null
                && (ownerTeacherId == null || !ownerTeacherId.equals(teacher.getId()))) {
            return "error:您不是该学生的" + (advisor ? "指导教师" : "评阅人");
        }

        try {
            if (advisor) {
                scoreService.setAdvisorScore(studentId, currentYear, score);
            } else {
                scoreService.setReviewerScore(studentId, currentYear, score);
            }
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }
}
