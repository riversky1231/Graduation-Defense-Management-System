package com.example.defensemanagement.controller.admin;

import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.UserService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 院系增删改 Controller。
 */
@RestController
@RequestMapping("/admin")
public class AdminDepartmentCrudController extends AbstractAdminDepartmentController {

    public AdminDepartmentCrudController(
            UserService userService,
            AuthService authService,
            DepartmentMapper departmentMapper) {
        super(userService, authService, departmentMapper);
    }

    @PostMapping("/department/create")
    @ResponseBody
    public String createDepartment(
            @RequestParam String name,
            @RequestParam String code,
            @RequestParam String description,
            HttpSession session) {
        String permissionError = checkDepartmentPermission(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            userService.createDepartment(name, code, description);
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @PostMapping("/department/update")
    @ResponseBody
    public String updateDepartment(@RequestBody Department department, HttpSession session) {
        String permissionError = checkDepartmentPermission(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            return userService.updateDepartment(department) ? "success" : "error:更新失败";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @DeleteMapping("/department/{id}")
    @ResponseBody
    public String deleteDepartment(@PathVariable Long id, HttpSession session) {
        String permissionError = checkDepartmentPermission(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            String validationError = validateDepartmentDeletion(id);
            if (validationError != null) {
                return "error:" + validationError;
            }
            return userService.deleteDepartment(id) ? "success" : "error:删除失败";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @PostMapping("/departments/batch-delete")
    @ResponseBody
    public String batchDeleteDepartments(@RequestBody List<Long> ids, HttpSession session) {
        String permissionError = checkDepartmentPermission(session);
        if (permissionError != null) {
            return permissionError;
        }
        if (ids == null || ids.isEmpty()) {
            return "error:请选择要删除的院系";
        }

        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMessages = new StringBuilder();

        for (Long id : ids) {
            try {
                String validationError = validateDepartmentDeletion(id);
                String departmentName = resolveDepartmentName(id);
                if (validationError != null) {
                    failCount++;
                    errorMessages.append("院系[").append(departmentName).append("]").append(validationError).append("；");
                    continue;
                }

                if (userService.deleteDepartment(id)) {
                    successCount++;
                } else {
                    failCount++;
                    errorMessages.append("院系[").append(departmentName).append("]删除失败；");
                }
            } catch (Exception e) {
                failCount++;
                errorMessages.append("院系[").append(resolveDepartmentName(id)).append("]删除失败：")
                        .append(e.getMessage()).append("；");
            }
        }

        if (failCount == 0) {
            return "success";
        }

        String errorMsg = errorMessages.toString();
        if (errorMsg.length() > 500) {
            errorMsg = errorMsg.substring(0, 500) + "...";
        }
        return "error:成功删除" + successCount + "个，失败" + failCount + "个。" + errorMsg;
    }
}
