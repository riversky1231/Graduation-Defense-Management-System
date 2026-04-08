package com.example.defensemanagement.controller.admin;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.PermissionService;
import com.example.defensemanagement.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 用户增删改端点。
 */
@Controller
@RequestMapping("/admin")
public class AdminUserCrudController extends AbstractAdminUserController {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    private final PermissionService permissionService;

    public AdminUserCrudController(
            UserService userService,
            AuthService authService,
            TeacherMapper teacherMapper,
            PermissionService permissionService) {
        super(userService, authService, teacherMapper);
        this.permissionService = permissionService;
    }

    @PostMapping("/users/save")
    @ResponseBody
    public String save(@RequestBody User user, HttpSession session) {
        Object currentOperator = getCurrentOperator(session);
        if (currentOperator == null) {
            return "error:未登录";
        }

        if (user.getId() == null) {
            if (!permissionService.canCreateUser(currentOperator, user)) {
                return "error:权限不足，无法创建该角色的用户";
            }
        } else {
            User target = userService.findById(user.getId());
            if (target == null) {
                return "error:目标用户不存在";
            }
            if (!permissionService.canEditUser(currentOperator, target)) {
                return "error:权限不足";
            }
        }

        try {
            userService.saveUser(user);
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @PostMapping("/user/{id}/status")
    @ResponseBody
    public String updateStatus(@PathVariable Long id, @RequestParam Integer status, HttpSession session) {
        Object currentOperator = getCurrentOperator(session);
        if (currentOperator == null) {
            return "error:未登录";
        }

        User target = userService.findById(id);
        if (target == null) {
            return "error:目标用户不存在";
        }
        if (!permissionService.canEditUser(currentOperator, target)) {
            return "error:权限不足";
        }
        return userService.updateUserStatus(id, status) ? "success" : "error:更新失败";
    }

    @DeleteMapping("/user/{id}")
    @ResponseBody
    public String delete(@PathVariable Long id, HttpSession session) {
        Object currentOperator = getCurrentOperator(session);
        if (currentOperator == null) {
            return "error:未登录";
        }

        User target = userService.findById(id);
        if (target == null) {
            return "error:目标用户不存在";
        }
        if (!permissionService.canEditUser(currentOperator, target)) {
            return "error:权限不足";
        }
        if (currentOperator instanceof User && ((User) currentOperator).getId().equals(id)) {
            return "error:不能删除自己";
        }

        try {
            return userService.deleteUser(id) ? "success" : "error:删除失败";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @PostMapping("/users/batch-delete")
    @ResponseBody
    public String batchDelete(@RequestBody List<Long> ids, HttpSession session) {
        Object currentOperator = getCurrentOperator(session);
        if (currentOperator == null) {
            return "error:未登录";
        }
        if (ids == null || ids.isEmpty()) {
            return "error:请选择要删除的用户";
        }

        Long currentUserId = currentOperator instanceof User ? ((User) currentOperator).getId() : null;
        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMessages = new StringBuilder();

        for (Long id : ids) {
            try {
                if (currentUserId != null && currentUserId.equals(id)) {
                    failCount++;
                    errorMessages.append("用户[ID:").append(id).append("]不能删除自己；");
                    continue;
                }

                User target = userService.findById(id);
                if (target == null) {
                    failCount++;
                    errorMessages.append("用户[ID:").append(id).append("]不存在；");
                    continue;
                }
                if (!permissionService.canEditUser(currentOperator, target)) {
                    failCount++;
                    errorMessages.append("用户[").append(target.getUsername()).append("]权限不足，无法删除；");
                    continue;
                }

                if (userService.deleteUser(id)) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                failCount++;
                errorMessages.append("用户[ID:").append(id).append("]删除失败：")
                        .append(e.getMessage()).append("；");
            }
        }

        if (failCount == 0) {
            return "success";
        }

        String message = errorMessages.toString();
        if (message.length() > MAX_ERROR_MESSAGE_LENGTH) {
            message = message.substring(0, MAX_ERROR_MESSAGE_LENGTH) + "...";
        }
        return "error:成功删除" + successCount + "个，失败" + failCount + "个。" + message;
    }
}
