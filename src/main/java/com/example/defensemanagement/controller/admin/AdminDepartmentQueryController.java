package com.example.defensemanagement.controller.admin;

import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 院系列表与搜索 Controller。
 */
@RestController
@RequestMapping("/admin")
public class AdminDepartmentQueryController extends AbstractAdminDepartmentController {

    public AdminDepartmentQueryController(
            UserService userService,
            AuthService authService,
            DepartmentMapper departmentMapper) {
        super(userService, authService, departmentMapper);
    }

    @GetMapping("/departments/list")
    @ResponseBody
    public List<Department> getDepartmentList(HttpSession session) {
        requireLogin(session);
        return userService.getAllDepartments();
    }

    @GetMapping("/departments/search")
    @ResponseBody
    public Map<String, Object> searchDepartments(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpSession session) {
        requireLogin(session);

        int offset = (page - 1) * pageSize;
        List<Department> departments = departmentMapper.searchDepartments(keyword, offset, pageSize);
        int total = departmentMapper.countDepartments(keyword);
        int totalPages = (int) Math.ceil((double) total / pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("departments", departments);
        result.put("total", total);
        result.put("currentPage", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", totalPages);
        return result;
    }
}
