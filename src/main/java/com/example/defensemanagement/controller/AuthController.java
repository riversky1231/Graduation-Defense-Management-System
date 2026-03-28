package com.example.defensemanagement.controller;

import com.example.defensemanagement.common.ApiResponse;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.Set;

@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final Set<String> ALLOWED_ROLES = Set.of(
            "SUPER_ADMIN", "DEPT_ADMIN", "DEFENSE_LEADER", "TEACHER", "STUDENT");

    @Autowired
    private AuthService authService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        @RequestParam String role,
                        @RequestParam String captcha,
                        HttpSession session,
                        Model model) {
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedRole = role == null ? "" : role.trim().toUpperCase();
        String normalizedCaptcha = captcha == null ? "" : captcha.trim().toLowerCase();

        if (normalizedUsername.isEmpty() || password == null || password.isBlank() || !ALLOWED_ROLES.contains(normalizedRole)) {
            model.addAttribute("error", "Invalid username, password, or role.");
            return "login";
        }

        // Validate captcha
        String sessionCaptcha = (String) session.getAttribute("captcha");
        session.removeAttribute("captcha");
        if (sessionCaptcha == null || !normalizedCaptcha.equals(sessionCaptcha)) {
            model.addAttribute("error", "Invalid captcha.");
            return "login";
        }

        log.info("Login attempt: username={}, role={}", normalizedUsername, normalizedRole);

        if ("TEACHER".equals(normalizedRole)) {
            Teacher teacher = authService.teacherLogin(normalizedUsername, password);
            if (teacher != null) {
                session.setAttribute("currentTeacher", teacher);
                session.setAttribute("userType", "TEACHER");
                return "redirect:/?login=success";
            }
        } else {
            User user = authService.login(normalizedUsername, password);
            if (user != null && user.getRole() != null && normalizedRole.equals(user.getRole().getName())) {
                session.setAttribute("currentUser", user);
                session.setAttribute("userType", "STUDENT".equals(normalizedRole) ? "STUDENT" : "USER");
                return "redirect:/?login=success";
            }
        }

        // Login failed
        model.addAttribute("error", "Invalid username, password, or role.");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }

    @PostMapping("/changePassword")
    @ResponseBody
    public ApiResponse<Map<String, String>> changePassword(@RequestParam String oldPassword,
                                                           @RequestParam String newPassword,
                                                           HttpSession session) {
        String userType = (String) session.getAttribute("userType");
        if (userType == null) {
            return ApiResponse.error("未登录");
        }

        if (oldPassword == null || oldPassword.isBlank() || newPassword == null || newPassword.isBlank()) {
            return ApiResponse.error("密码不能为空");
        }
        if (newPassword.length() < 8) {
            return ApiResponse.error("新密码至少需要 8 位");
        }
        if (newPassword.equals(oldPassword)) {
            return ApiResponse.error("新密码不能与旧密码相同");
        }

        if ("USER".equals(userType) || "STUDENT".equals(userType)) {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser != null) {
                boolean result = authService.changeUserPassword(currentUser.getId(), oldPassword, newPassword);
                if (result) {
                    return ApiResponse.success("密码修改成功", Map.of("userType", userType));
                } else {
                    return ApiResponse.error("旧密码错误或用户不存在");
                }
            } else {
                return ApiResponse.error("当前用户不存在");
            }
        } else if ("TEACHER".equals(userType)) {
            Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
            if (currentTeacher != null) {
                boolean result = authService.changeTeacherPassword(currentTeacher.getId(), oldPassword, newPassword);
                if (result) {
                    return ApiResponse.success("密码修改成功", Map.of("userType", userType));
                } else {
                    return ApiResponse.error("旧密码错误或教师不存在");
                }
            } else {
                return ApiResponse.error("当前教师不存在");
            }
        }

        return ApiResponse.error("未知用户类型");
    }
}
