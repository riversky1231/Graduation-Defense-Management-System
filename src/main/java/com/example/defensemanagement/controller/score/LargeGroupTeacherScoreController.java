package com.example.defensemanagement.controller.score;

import com.example.defensemanagement.common.ApiResponse;
import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.ScoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/defense/score")
public class LargeGroupTeacherScoreController extends AbstractLargeGroupScoreController {

    private static final String ARCHIVED_KEY = "LARGE_GROUP_ARCHIVED";
    private static final String DEADLINE_KEY = "LARGE_GROUP_DEADLINE";

    public LargeGroupTeacherScoreController(
            ScoreService scoreService,
            TeacherMapper teacherMapper,
            StudentMapper studentMapper,
            DefenseGroupMapper defenseGroupMapper,
            ConfigService configService) {
        super(scoreService, teacherMapper, studentMapper, defenseGroupMapper, configService);
    }

    @PostMapping("/largegroup/save")
    @ResponseBody
    public ApiResponse<Void> saveLargeGroupScore(
            @RequestParam Long studentId,
            @RequestParam Integer score,
            HttpSession session) {
        Teacher teacher = getTeacherFromSession(session);
        if (teacher == null) {
            return errorResponse("请先登录教师账号");
        }
        if ("1".equals(configService.getConfigValue(ARCHIVED_KEY))) {
            return errorResponse("大组成绩已归档，无法再打分");
        }
        if (isDeadlineExpired(configService.getConfigValue(DEADLINE_KEY))) {
            return errorResponse("大组打分已截止，无法再打分");
        }
        if (!canScoreStudentInTeacherDepartment(studentId, teacher.getDepartmentId())) {
            return errorResponse("您只能给本院系的学生打分");
        }

        try {
            scoreService.saveLargeGroupScore(studentId, teacher.getId(), getCurrentDefenseYear(), score);
            return successResponse("保存成功");
        } catch (Exception e) {
            return errorResponse(safeMessage(e, "保存大组评分失败"));
        }
    }

    @GetMapping("/largegroup/student/{studentId}/scores")
    @ResponseBody
    public ApiResponse<Map<String, Object>> getLargeGroupStudentScores(@PathVariable Long studentId, HttpSession session) {
        Teacher teacher = getTeacherFromSession(session);
        if (teacher == null) {
            return errorResponse("请先登录教师账号");
        }
        if (!canScoreStudentInTeacherDepartment(studentId, teacher.getDepartmentId())) {
            return errorResponse("您只能查看本院系学生的大组评分");
        }
        return successResponse("查询成功", scoreService.getLargeGroupStudentScores(studentId, getCurrentDefenseYear()));
    }

    private boolean isDeadlineExpired(String deadlineStr) {
        if (deadlineStr == null || deadlineStr.trim().isEmpty()) {
            return false;
        }
        try {
            LocalDateTime deadline = LocalDateTime.parse(
                    deadlineStr.trim(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return LocalDateTime.now().isAfter(deadline);
        } catch (Exception e) {
            log.warn("大组打分截止时间格式错误: {}", deadlineStr);
            return false;
        }
    }

    private boolean canScoreStudentInTeacherDepartment(Long studentId, Long teacherDeptId) {
        if (teacherDeptId == null) {
            return false;
        }
        Student student = studentMapper.findById(studentId);
        if (student == null || student.getDefenseGroupId() == null) {
            return false;
        }
        DefenseGroup group = defenseGroupMapper.findById(student.getDefenseGroupId());
        return group != null
                && group.getDepartmentId() != null
                && teacherDeptId.equals(group.getDepartmentId());
    }
}
