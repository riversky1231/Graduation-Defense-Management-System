package com.example.defensemanagement.controller.score;

import com.example.defensemanagement.common.ApiResponse;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.ScoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/defense/score")
public class LargeGroupCandidateController extends AbstractLargeGroupScoreController {

    public LargeGroupCandidateController(
            ScoreService scoreService,
            TeacherMapper teacherMapper,
            StudentMapper studentMapper,
            DefenseGroupMapper defenseGroupMapper,
            ConfigService configService) {
        super(scoreService, teacherMapper, studentMapper, defenseGroupMapper, configService);
    }

    @GetMapping("/largegroup/candidates")
    @ResponseBody
    public ApiResponse<Map<String, Object>> getLargeGroupCandidates(HttpSession session) {
        User currentUser = getCurrentUser(session);
        boolean isSuperAdmin = isRole(currentUser, "SUPER_ADMIN");
        boolean isDeptAdmin = isRole(currentUser, "DEPT_ADMIN");
        Teacher teacher = getTeacherFromSession(session);
        Integer year = getCurrentDefenseYear();

        Map<String, Object> result = new HashMap<>();
        if (isSuperAdmin) {
            result.put("candidates", scoreService.getLargeGroupCandidates(year, null));
            result.put("teacherId", null);
            result.put("teacherName", "超级管理员");
        } else if (isDeptAdmin) {
            List<Map<String, Object>> deptCandidates = scoreService.getLargeGroupCandidates(year, null)
                    .stream()
                    .filter(candidate -> sameDepartment(candidate.get("departmentId"), currentUser.getDepartmentId()))
                    .collect(Collectors.toList());
            result.put("candidates", deptCandidates);
            result.put("teacherId", null);
            result.put("teacherName", "院系管理员");
            result.put("isDeptAdmin", true);
        } else {
            if (teacher == null) {
                return errorResponse("请先登录教师账号");
            }

            List<Map<String, Object>> allCandidates = scoreService.getLargeGroupCandidates(year, teacher.getId());
            List<Map<String, Object>> deptCandidates = teacher.getDepartmentId() != null
                    ? allCandidates.stream()
                    .filter(candidate -> sameDepartment(candidate.get("departmentId"), teacher.getDepartmentId()))
                    .collect(Collectors.toList())
                    : allCandidates;
            result.put("candidates", deptCandidates);
            result.put("teacherId", teacher.getId());
            result.put("teacherName", teacher.getName());
        }

        result.put("year", year);
        return successResponse("查询成功", result);
    }
}
