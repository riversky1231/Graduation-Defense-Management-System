package com.example.defensemanagement.controller.group;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.TeacherService;

import javax.servlet.http.HttpSession;

/**
 * 答辩小组分配共享基类。
 */
abstract class AbstractGroupAssignmentController {

    protected static final String GROUP_MAX_STUDENTS_KEY = "GROUP_MAX_STUDENTS";
    protected static final int DEFAULT_MAX_STUDENTS = 10;

    protected final AuthService authService;
    protected final TeacherService teacherService;
    protected final DefenseGroupMapper defenseGroupMapper;
    protected final DefenseGroupTeacherMapper defenseGroupTeacherMapper;
    protected final DepartmentMapper departmentMapper;
    protected final ConfigService configService;
    protected final StudentMapper studentMapper;

    protected AbstractGroupAssignmentController(
            AuthService authService,
            TeacherService teacherService,
            DefenseGroupMapper defenseGroupMapper,
            DefenseGroupTeacherMapper defenseGroupTeacherMapper,
            DepartmentMapper departmentMapper,
            ConfigService configService,
            StudentMapper studentMapper) {
        this.authService = authService;
        this.teacherService = teacherService;
        this.defenseGroupMapper = defenseGroupMapper;
        this.defenseGroupTeacherMapper = defenseGroupTeacherMapper;
        this.departmentMapper = departmentMapper;
        this.configService = configService;
        this.studentMapper = studentMapper;
    }

    protected User requireDeptAdmin(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "MANAGE_TEACHERS")) {
            return null;
        }
        return currentUser;
    }

    protected boolean isSuperAdmin(User user) {
        return user != null && user.getRole() != null && "SUPER_ADMIN".equals(user.getRole().getName());
    }

    protected boolean isDeptAdmin(User user) {
        return user != null && user.getRole() != null && "DEPT_ADMIN".equals(user.getRole().getName());
    }

    protected int getGroupMaxStudents() {
        String value = configService.getConfigValue(GROUP_MAX_STUDENTS_KEY);
        if (value == null) {
            return DEFAULT_MAX_STUDENTS;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return DEFAULT_MAX_STUDENTS;
        }
    }
}
