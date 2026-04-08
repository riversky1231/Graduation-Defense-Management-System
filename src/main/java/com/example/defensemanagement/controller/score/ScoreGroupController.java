package com.example.defensemanagement.controller.score;

import com.example.defensemanagement.common.ApiResponse;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.service.AuthService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 小组汇总与答辩教师视图端点。
 */
@RestController
@RequestMapping("/defense/score")
public class ScoreGroupController extends AbstractScoreController {

    public ScoreGroupController(
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

    @PostMapping("/group/finalize")
    public ApiResponse<Void> finalizeGroup(
            @RequestParam Long groupId,
            @RequestParam Integer year,
            @RequestParam(required = false) Integer largeGroupScore) {
        try {
            scoreService.finalizeGroupScores(groupId, year, largeGroupScore);
            return successResponse("汇总成功");
        } catch (Exception e) {
            return errorResponse(safeMessage(e, "小组汇总失败"));
        }
    }

    @GetMapping("/group/{groupId}/adjustmentFactor")
    public ApiResponse<Map<String, Object>> getAdjustmentFactor(@PathVariable Long groupId, @RequestParam Integer year) {
        try {
            return successResponse("查询成功", scoreService.getGroupAdjustmentFactor(groupId, year));
        } catch (Exception e) {
            return errorResponse(safeMessage(e, "获取调节系数失败"));
        }
    }

    @GetMapping("/teacher/group/students")
    @ResponseBody
    public ApiResponse<Map<String, Object>> getTeacherGroupStudents(HttpSession session) {
        User currentUser = getCurrentUser(session);
        boolean isSuperAdmin = currentUser != null
                && currentUser.getRole() != null
                && "SUPER_ADMIN".equals(currentUser.getRole().getName());
        boolean isDeptAdmin = currentUser != null
                && currentUser.getRole() != null
                && "DEPT_ADMIN".equals(currentUser.getRole().getName());

        Teacher teacher = getTeacherFromSession(session);
        Integer year = getCurrentDefenseYear();

        Map<String, Object> result;
        if (isSuperAdmin) {
            result = scoreService.getAllGroupStudentsForSuperAdmin(year);
            result.put("teacherId", null);
            result.put("teacherName", "超级管理员");
        } else if (isDeptAdmin) {
            result = scoreService.getAllGroupStudentsForSuperAdmin(year);
            Long departmentId = currentUser.getDepartmentId();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> allGroups = (List<Map<String, Object>>) result.get("groups");
            if (allGroups != null) {
                List<Map<String, Object>> deptGroups = allGroups.stream()
                        .filter(group -> {
                            Object groupDeptId = group.get("departmentId");
                            return groupDeptId != null && groupDeptId.equals(departmentId);
                        })
                        .collect(Collectors.toList());
                result.put("groups", deptGroups);
            }
            result.put("teacherId", null);
            result.put("teacherName", "院系管理员");
            result.put("isDeptAdmin", true);
        } else {
            if (teacher == null) {
                return errorResponse("请先登录教师账号");
            }
            result = scoreService.getTeacherGroupStudents(teacher.getId(), year);
            result.put("teacherId", teacher.getId());
            result.put("teacherName", teacher.getName());
        }

        result.put("year", year);
        return successResponse("查询成功", result);
    }
}
