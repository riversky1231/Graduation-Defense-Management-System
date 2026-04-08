package com.example.defensemanagement.controller.admin;

import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 院系管理 Controller 共享基类。
 * 提供权限检查和删除前校验等公共逻辑。
 */
public abstract class AbstractAdminDepartmentController {

    protected final UserService userService;
    protected final AuthService authService;
    protected final DepartmentMapper departmentMapper;

    protected AbstractAdminDepartmentController(
            UserService userService,
            AuthService authService,
            DepartmentMapper departmentMapper) {
        this.userService = userService;
        this.authService = authService;
        this.departmentMapper = departmentMapper;
    }

    protected User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("currentUser");
    }

    protected boolean canManageDepartments(HttpSession session) {
        User currentUser = getCurrentUser(session);
        return currentUser != null && authService.hasPermission(currentUser, "CREATE_DEPARTMENT");
    }

    protected String checkDepartmentPermission(HttpSession session) {
        return canManageDepartments(session) ? null : "error:权限不足";
    }

    protected User requireLogin(HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return currentUser;
    }

    protected String validateDepartmentDeletion(Long departmentId) {
        List<User> users = userService.getAllUsers(departmentId);
        if (users != null && !users.isEmpty()) {
            return "该院系下还有用户，无法删除";
        }
        return null;
    }

    protected String resolveDepartmentName(Long departmentId) {
        Department department = departmentMapper.findById(departmentId);
        return department != null ? department.getName() : "ID:" + departmentId;
    }
}
