package com.example.defensemanagement.controller.admin;

import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 院系管理页面 Controller。
 */
@Controller
@RequestMapping("/admin")
public class AdminDepartmentPageController extends AbstractAdminDepartmentController {

    public AdminDepartmentPageController(
            UserService userService,
            AuthService authService,
            DepartmentMapper departmentMapper) {
        super(userService, authService, departmentMapper);
    }

    @GetMapping("/departments")
    public String departmentManagement(Model model, HttpSession session) {
        if (!canManageDepartments(session)) {
            return "redirect:/";
        }

        List<Department> departments = userService.getAllDepartments();
        model.addAttribute("departments", departments);
        return "admin/departments";
    }
}
