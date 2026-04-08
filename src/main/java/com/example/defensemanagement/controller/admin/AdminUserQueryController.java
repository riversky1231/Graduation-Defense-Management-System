package com.example.defensemanagement.controller.admin;

import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户查询相关端点。
 */
@Controller
@RequestMapping("/admin")
public class AdminUserQueryController extends AbstractAdminUserController {

    public AdminUserQueryController(
            UserService userService,
            AuthService authService,
            com.example.defensemanagement.mapper.TeacherMapper teacherMapper) {
        super(userService, authService, teacherMapper);
    }

    @GetMapping("/users/list")
    @ResponseBody
    public List<User> list(HttpSession session) {
        Long departmentId = getDepartmentIdIfDeptAdmin(session);
        return userService.getAllUsers(departmentId);
    }

    @GetMapping("/users/search")
    @ResponseBody
    public Map<String, Object> search(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int pageSize,
            HttpSession session) {
        Long departmentId = getDepartmentIdIfDeptAdmin(session);
        List<User> users = userService.searchUsers(keyword, page, pageSize, departmentId);
        int total = userService.countUsers(keyword, departmentId);
        int totalPages = (int) Math.ceil((double) total / pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("users", users);
        result.put("total", total);
        result.put("currentPage", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", totalPages);
        return result;
    }

    @GetMapping("/roles/list")
    @ResponseBody
    public List<Role> roleList(HttpSession session) {
        User currentUser = requireCurrentUser(session);
        return userService.getManagableRoles(currentUser);
    }

    @GetMapping("/user/{userId}/isDefenseLeader")
    @ResponseBody
    public boolean isDefenseLeader(@PathVariable Long userId, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return false;
        }

        User target = userService.findById(userId);
        if (target == null || target.getRole() == null || !"TEACHER".equals(target.getRole().getName())) {
            return false;
        }

        Teacher teacher = teacherMapper.findByUserId(userId);
        if (teacher == null) {
            return false;
        }
        return authService.isDefenseLeader(teacher.getId(), LocalDate.now().getYear());
    }
}
