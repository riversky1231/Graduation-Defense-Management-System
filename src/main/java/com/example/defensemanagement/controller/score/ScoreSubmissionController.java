package com.example.defensemanagement.controller.score;

import com.example.defensemanagement.common.ApiResponse;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.ScoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 分数录入与教师侧查询端点。
 */
@RestController
@RequestMapping("/defense/score")
public class ScoreSubmissionController extends AbstractScoreController {

    public ScoreSubmissionController(
            ScoreService scoreService,
            AuthService authService,
            TeacherScoreRecordMapper teacherScoreRecordMapper,
            DefenseGroupTeacherMapper defenseGroupTeacherMapper,
            TeacherMapper teacherMapper,
            StudentMapper studentMapper,
            ConfigService configService) {
        super(scoreService, authService, teacherScoreRecordMapper, defenseGroupTeacherMapper,
                teacherMapper, studentMapper, configService);
    }

    @PostMapping("/teacher/save")
    public ApiResponse<Map<String, Object>> saveTeacherScore(@RequestBody TeacherScoreRecord record, HttpSession session) {
        Teacher teacher = getTeacherFromSession(session);
        if (teacher == null) {
            return errorResponse("请先登录教师账号");
        }
        if (!isTeacherScoringOwnGroup(teacher.getId(), record.getStudentId())) {
            return errorResponse("您只能给本答辩小组的学生打分");
        }

        try {
            Student student = studentMapper.findById(record.getStudentId());
            record.setTeacherId(teacher.getId());
            record.setYear(getCurrentDefenseYear());
            record.setDefenseGroupId(student != null ? student.getDefenseGroupId() : null);
            scoreService.saveTeacherScore(record);
            return successResponse("保存成功", Map.of(
                    "studentId", record.getStudentId(),
                    "teacherId", teacher.getId(),
                    "year", record.getYear()));
        } catch (Exception e) {
            return errorResponse(safeMessage(e, "保存教师评分失败"));
        }
    }

    @PostMapping("/teacher/group/score")
    @ResponseBody
    public ApiResponse<Map<String, Object>> saveTeacherGroupScore(
            @RequestParam Long studentId,
            @RequestParam Integer totalScore,
            @RequestParam(required = false) Integer item1,
            @RequestParam(required = false) Integer item2,
            @RequestParam(required = false) Integer item3,
            @RequestParam(required = false) Integer item4,
            @RequestParam(required = false) Integer item5,
            @RequestParam(required = false) Integer item6,
            HttpSession session) {
        Teacher teacher = getTeacherFromSession(session);
        if (teacher == null) {
            return errorResponse("请先登录教师账号");
        }
        if (!isTeacherScoringOwnGroup(teacher.getId(), studentId)) {
            return errorResponse("您只能给本答辩小组的学生打分");
        }

        try {
            TeacherScoreRecord record = new TeacherScoreRecord();
            record.setStudentId(studentId);
            record.setTeacherId(teacher.getId());
            record.setYear(getCurrentDefenseYear());
            record.setItem1Score(item1);
            record.setItem2Score(item2);
            record.setItem3Score(item3);
            record.setItem4Score(item4);
            record.setItem5Score(item5);
            record.setItem6Score(item6);
            record.setTotalScore(totalScore);

            Student student = studentMapper.findById(studentId);
            if (student != null && student.getDefenseGroupId() != null) {
                record.setDefenseGroupId(student.getDefenseGroupId());
            }

            scoreService.saveTeacherScore(record);
            return successResponse("保存成功", Map.of(
                    "studentId", studentId,
                    "teacherId", teacher.getId(),
                    "year", record.getYear()));
        } catch (Exception e) {
            return errorResponse(safeMessage(e, "保存教师评分失败"));
        }
    }

    @PostMapping("/design/autoSplit")
    public ApiResponse<Map<String, Object>> autoSplitDesign(
            @RequestParam Long studentId,
            @RequestParam Long teacherId,
            @RequestParam Integer year,
            @RequestParam Integer totalScore,
            @RequestParam(required = false) Long defenseGroupId,
            HttpSession session) {
        Teacher teacher = getTeacherFromSession(session);
        if (teacher == null) {
            return errorResponse("请先登录教师账号");
        }
        if (!isTeacherScoringOwnGroup(teacher.getId(), studentId)) {
            return errorResponse("您只能给本答辩小组的学生打分");
        }

        try {
            Student student = studentMapper.findById(studentId);
            scoreService.autoSplitDesignScore(
                    studentId,
                    teacher.getId(),
                    getCurrentDefenseYear(),
                    totalScore,
                    student != null ? student.getDefenseGroupId() : null);
            return successResponse("保存成功", Map.of(
                    "studentId", studentId,
                    "teacherId", teacher.getId(),
                    "year", getCurrentDefenseYear()));
        } catch (Exception e) {
            return errorResponse(safeMessage(e, "自动拆分并保存评分失败"));
        }
    }

    @PostMapping("/teacher/auto-split")
    @ResponseBody
    public ApiResponse<Map<String, Integer>> autoSplitItems(
            @RequestParam String defenseType,
            @RequestParam Integer totalScore,
            HttpSession session) {
        Teacher teacher = getTeacherFromSession(session);
        User currentUser = getCurrentUser(session);
        if (teacher == null && currentUser == null) {
            return errorResponse("未登录");
        }

        try {
            return successResponse("分项生成成功", scoreService.autoSplitScoreItems(defenseType, totalScore));
        } catch (Exception e) {
            return errorResponse(safeMessage(e, "分项生成失败"));
        }
    }

    @PostMapping("/advisor/set")
    public ApiResponse<Void> setAdvisorScore(
            @RequestParam Long studentId,
            @RequestParam Integer year,
            @RequestParam Integer score,
            HttpSession session) {
        if (!hasSuperAdminAccess(session)) {
            return errorResponse("权限不足");
        }
        try {
            scoreService.setAdvisorScore(studentId, year, score);
            return successResponse("保存成功");
        } catch (Exception e) {
            return errorResponse(safeMessage(e, "保存指导教师评分失败"));
        }
    }

    @PostMapping("/reviewer/set")
    public ApiResponse<Void> setReviewerScore(
            @RequestParam Long studentId,
            @RequestParam Integer year,
            @RequestParam Integer score,
            HttpSession session) {
        if (!hasSuperAdminAccess(session)) {
            return errorResponse("权限不足");
        }
        try {
            scoreService.setReviewerScore(studentId, year, score);
            return successResponse("保存成功");
        } catch (Exception e) {
            return errorResponse(safeMessage(e, "保存评阅教师评分失败"));
        }
    }

    @GetMapping("/teacher/current")
    @ResponseBody
    public ApiResponse<Map<String, Object>> getCurrentTeacher(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Teacher teacher = getTeacherFromSession(session);
        if (teacher != null) {
            result.put("teacherId", teacher.getId());
            result.put("teacherNo", teacher.getTeacherNo());
            result.put("teacherName", teacher.getName());
        } else {
            result.put("teacherId", null);
        }
        return successResponse("查询成功", result);
    }
}
