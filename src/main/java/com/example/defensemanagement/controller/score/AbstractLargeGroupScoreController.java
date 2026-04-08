package com.example.defensemanagement.controller.score;

import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.ScoreService;

import javax.servlet.http.HttpSession;

abstract class AbstractLargeGroupScoreController extends AbstractScoreControllerSupport {

    protected final ScoreService scoreService;
    protected final DefenseGroupMapper defenseGroupMapper;

    protected AbstractLargeGroupScoreController(
            ScoreService scoreService,
            TeacherMapper teacherMapper,
            StudentMapper studentMapper,
            DefenseGroupMapper defenseGroupMapper,
            ConfigService configService) {
        super(teacherMapper, studentMapper, configService);
        this.scoreService = scoreService;
        this.defenseGroupMapper = defenseGroupMapper;
    }

    protected boolean isRole(User user, String roleName) {
        return user != null && user.getRole() != null && roleName.equals(user.getRole().getName());
    }

    protected boolean sameDepartment(Object groupDeptId, Long expectedDeptId) {
        if (groupDeptId == null || expectedDeptId == null || !(groupDeptId instanceof Number)) {
            return false;
        }
        return expectedDeptId.equals(((Number) groupDeptId).longValue());
    }
}
