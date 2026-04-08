package com.example.defensemanagement.controller.score;

import com.example.defensemanagement.common.ApiResponse;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.ScoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/defense/score")
public class LargeGroupAdminScoreController extends AbstractLargeGroupScoreController {

    public LargeGroupAdminScoreController(
            ScoreService scoreService,
            TeacherMapper teacherMapper,
            StudentMapper studentMapper,
            DefenseGroupMapper defenseGroupMapper,
            ConfigService configService) {
        super(scoreService, teacherMapper, studentMapper, defenseGroupMapper, configService);
    }

    @GetMapping("/largegroup/student/{studentId}/scores/admin")
    @ResponseBody
    public ApiResponse<Map<String, Object>> getLargeGroupStudentScoresForAdmin(@PathVariable Long studentId, HttpSession session) {
        if (!isRole(getCurrentUser(session), "SUPER_ADMIN")) {
            return errorResponse("权限不足：只有超级管理员可以查看");
        }
        return successResponse("查询成功", scoreService.getLargeGroupStudentScoresForAdmin(studentId, getCurrentDefenseYear()));
    }

    @PostMapping("/largegroup/update")
    @ResponseBody
    public ApiResponse<Void> updateLargeGroupScore(@RequestBody Map<String, Object> request, HttpSession session) {
        if (!isRole(getCurrentUser(session), "SUPER_ADMIN")) {
            return errorResponse("权限不足：只有超级管理员可以修改");
        }

        try {
            Long scoreId = longValue(request.get("scoreId"));
            Long studentId = longValue(request.get("studentId"));
            Long teacherId = longValue(request.get("teacherId"));
            Integer score = intValue(request.get("score"));

            if (studentId == null || teacherId == null || score == null) {
                return errorResponse("参数不完整");
            }
            if (score < 0 || score > 100) {
                return errorResponse("分数必须在0-100之间");
            }

            scoreService.updateLargeGroupScore(scoreId, studentId, teacherId, getCurrentDefenseYear(), score);
            return successResponse("保存成功");
        } catch (Exception e) {
            return errorResponse(safeMessage(e, "更新大组评分失败"));
        }
    }

    private Long longValue(Object value) {
        return value != null ? Long.valueOf(value.toString()) : null;
    }

    private Integer intValue(Object value) {
        return value != null ? Integer.valueOf(value.toString()) : null;
    }
}
