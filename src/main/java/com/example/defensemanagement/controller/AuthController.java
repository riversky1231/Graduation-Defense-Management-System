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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Tag(name = "认证", description = "登录、登出、修改密码")
@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final Set<String> ALLOWED_ROLES = Set.of(
            "SUPER_ADMIN", "DEPT_ADMIN", "DEFENSE_LEADER", "TEACHER", "STUDENT");

    /** 登录失败限流：最大失败次数 */
    private static final int MAX_FAILURES = 5;
    /** 锁定时长（毫秒）：15分钟 */
    private static final long LOCK_DURATION_MS = 15 * 60 * 1000L;

    /** key=IP, value=[失败次数, 首次失败时间戳] */
    private final ConcurrentHashMap<String, long[]> loginFailures = new ConcurrentHashMap<>();

    @Autowired
    private AuthService authService;

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isLocked(String ip) {
        long[] record = loginFailures.get(ip);
        if (record == null) return false;
        long failures = record[0];
        long firstFailTime = record[1];
        if (failures >= MAX_FAILURES) {
            if (Instant.now().toEpochMilli() - firstFailTime < LOCK_DURATION_MS) {
                return true;
            }
            // 锁定已过期，清除记录
            loginFailures.remove(ip);
        }
        return false;
    }

    private void recordFailure(String ip) {
        loginFailures.compute(ip, (k, v) -> {
            if (v == null) return new long[]{1, Instant.now().toEpochMilli()};
            v[0]++;
            return v;
        });
    }

    private void clearFailure(String ip) {
        loginFailures.remove(ip);
    }

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
                        HttpServletRequest request,
                        Model model) {
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedRole = role == null ? "" : role.trim().toUpperCase();
        String normalizedCaptcha = captcha == null ? "" : captcha.trim().toLowerCase();
        String clientIp = getClientIp(request);

        if (normalizedUsername.isEmpty() || password == null || password.isBlank() || !ALLOWED_ROLES.contains(normalizedRole)) {
            model.addAttribute("error", "用户名、密码或角色不合法。");
            return "login";
        }

        // 检查 IP 是否被锁定
        if (isLocked(clientIp)) {
            log.warn("Login blocked for locked IP: {}", clientIp);
            model.addAttribute("error", "登录失败次数过多，请15分钟后再试。");
            return "login";
        }

        // Validate captcha
        String sessionCaptcha = (String) session.getAttribute("captcha");
        session.removeAttribute("captcha");
        if (sessionCaptcha == null || !normalizedCaptcha.equals(sessionCaptcha)) {
            model.addAttribute("error", "验证码错误。");
            return "login";
        }

        log.info("Login attempt: username={}, role={}, ip={}", normalizedUsername, normalizedRole, clientIp);

        if ("TEACHER".equals(normalizedRole)) {
            Teacher teacher = authService.teacherLogin(normalizedUsername, password);
            if (teacher != null) {
                clearFailure(clientIp);
                session.setAttribute("currentTeacher", teacher);
                session.setAttribute("userType", "TEACHER");
                return "redirect:/?login=success";
            }
        } else {
            User user = authService.login(normalizedUsername, password);
            if (user != null && user.getRole() != null && normalizedRole.equals(user.getRole().getName())) {
                clearFailure(clientIp);
                session.setAttribute("currentUser", user);
                session.setAttribute("userType", "STUDENT".equals(normalizedRole) ? "STUDENT" : "USER");
                return "redirect:/?login=success";
            }
        }

        // Login failed
        recordFailure(clientIp);
        long[] record = loginFailures.get(clientIp);
        long remaining = record != null ? MAX_FAILURES - record[0] : 0;
        log.warn("Login failed: username={}, ip={}, remaining attempts={}", normalizedUsername, clientIp, remaining);
        if (remaining <= 0) {
            model.addAttribute("error", "登录失败次数过多，账号已锁定15分钟。");
        } else {
            model.addAttribute("error", "用户名或密码错误，还剩 " + remaining + " 次尝试机会。");
        }
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
