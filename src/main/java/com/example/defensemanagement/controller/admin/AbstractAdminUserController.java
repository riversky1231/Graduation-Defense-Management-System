package com.example.defensemanagement.controller.admin;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpSession;

/**
 * 用户管理 Controller 共享基类。
 */
public abstract class AbstractAdminUserController {

    protected final UserService userService;
    protected final AuthService authService;
    protected final TeacherMapper teacherMapper;

    protected AbstractAdminUserController(
            UserService userService,
            AuthService authService,
            TeacherMapper teacherMapper) {
        this.userService = userService;
        this.authService = authService;
        this.teacherMapper = teacherMapper;
    }

    protected User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("currentUser");
    }

    protected Object getCurrentOperator(HttpSession session) {
        Object currentUser = session.getAttribute("currentUser");
        return currentUser != null ? currentUser : session.getAttribute("currentTeacher");
    }

    protected User requireCurrentUser(HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return currentUser;
    }

    protected boolean hasCreateUserPermission(HttpSession session) {
        User currentUser = getCurrentUser(session);
        return currentUser != null && authService.hasPermission(currentUser, "CREATE_USER");
    }

    protected Long getDepartmentIdIfDeptAdmin(HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser != null && currentUser.getRole() != null
                && "DEPT_ADMIN".equals(currentUser.getRole().getName())) {
            return currentUser.getDepartmentId();
        }
        return null;
    }
}
