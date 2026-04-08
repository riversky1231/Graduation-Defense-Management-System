package com.example.defensemanagement.controller;

import com.example.defensemanagement.common.ApiResponse;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.impl.RedisLoginFailureService;
import com.example.defensemanagement.util.ClientIpResolver;
import com.example.defensemanagement.util.PasswordSecurityUtils;
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
import java.util.Map;
import java.util.Set;

@Tag(name = "认证", description = "登录、登出、修改密码")
@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final Set<String> ALLOWED_ROLES = Set.of(
            "SUPER_ADMIN", "DEPT_ADMIN", "DEFENSE_LEADER", "TEACHER", "STUDENT");

    @Autowired
    private AuthService authService;

    @Autowired
    private RedisLoginFailureService loginFailureService;

    @Autowired
    private ClientIpResolver clientIpResolver;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/force-password-change")
    public String forcePasswordChangePage(HttpSession session, Model model) {
        if (!Boolean.TRUE.equals(session.getAttribute(PasswordSecurityUtils.FORCE_PASSWORD_CHANGE_SESSION_KEY))) {
            return "redirect:/";
        }

        User currentUser = (User) session.getAttribute("currentUser");
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
        if (currentUser == null && currentTeacher == null) {
            return "redirect:/login";
        }

        String displayName = currentUser != null ? currentUser.getRealName() : currentTeacher.getName();
        model.addAttribute("displayName", displayName);
        return "force-password-change";
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
        String clientIp = clientIpResolver.resolveClientIp(request);

        if (normalizedUsername.isEmpty() || password == null || password.isBlank() || !ALLOWED_ROLES.contains(normalizedRole)) {
            model.addAttribute("error", "用户名、密码或角色不合法。");
            return "login";
        }
        if (!PasswordSecurityUtils.isPasswordLengthAllowed(password)) {
            model.addAttribute("error", "密码长度不合法。");
            return "login";
        }

        // Check if IP is locked
        if (loginFailureService.isLocked(clientIp)) {
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
                loginFailureService.clearFailure(clientIp);
                return completeTeacherLogin(session, teacher, password);
            }
        } else {
            User user = authService.login(normalizedUsername, password);
            if (user != null && user.getRole() != null && normalizedRole.equals(user.getRole().getName())) {
                loginFailureService.clearFailure(clientIp);
                return completeUserLogin(session, user, "STUDENT".equals(normalizedRole) ? "STUDENT" : "USER", password);
            }
        }

        // Login failed
        loginFailureService.recordFailure(clientIp);
        int remaining = loginFailureService.remainingAttempts(clientIp);
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
                                                           HttpSession session,
                                                           HttpServletRequest request) {
        if (!isAjaxRequest(request)) {
            return ApiResponse.error("非法请求");
        }

        String userType = (String) session.getAttribute("userType");
        if (userType == null) {
            return ApiResponse.error("未登录");
        }

        if (oldPassword == null || oldPassword.isBlank() || newPassword == null || newPassword.isBlank()) {
            return ApiResponse.error("密码不能为空");
        }
        if (!PasswordSecurityUtils.isPasswordLengthAllowed(oldPassword)
                || !PasswordSecurityUtils.isPasswordLengthAllowed(newPassword)) {
            return ApiResponse.error("密码长度不能超过 " + PasswordSecurityUtils.MAX_PASSWORD_LENGTH + " 位");
        }
        if (!PasswordSecurityUtils.isNewPasswordLengthValid(newPassword)) {
            return ApiResponse.error("新密码至少需要 " + PasswordSecurityUtils.MIN_PASSWORD_LENGTH + " 位");
        }
        if (PasswordSecurityUtils.constantTimeEquals(newPassword, oldPassword)) {
            return ApiResponse.error("新密码不能与旧密码相同");
        }
        if (isDefaultIdentifierPasswordForCurrentSession(session, newPassword)) {
            return ApiResponse.error("新密码不能与账号相同");
        }

        if ("USER".equals(userType) || "STUDENT".equals(userType)) {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser != null) {
                boolean result = authService.changeUserPassword(currentUser.getId(), oldPassword, newPassword);
                if (result) {
                    session.removeAttribute(PasswordSecurityUtils.FORCE_PASSWORD_CHANGE_SESSION_KEY);
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
                    session.removeAttribute(PasswordSecurityUtils.FORCE_PASSWORD_CHANGE_SESSION_KEY);
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

    private String completeUserLogin(HttpSession session, User user, String userType, String rawPassword) {
        session.setAttribute("currentUser", user);
        session.removeAttribute("currentTeacher");
        session.setAttribute("userType", userType);

        if (shouldRequirePasswordChange(user.getPassword(), rawPassword, user.getUsername())) {
            session.setAttribute(PasswordSecurityUtils.FORCE_PASSWORD_CHANGE_SESSION_KEY, Boolean.TRUE);
            return "redirect:/force-password-change";
        }

        session.removeAttribute(PasswordSecurityUtils.FORCE_PASSWORD_CHANGE_SESSION_KEY);
        return "redirect:/?login=success";
    }

    private String completeTeacherLogin(HttpSession session, Teacher teacher, String rawPassword) {
        session.setAttribute("currentTeacher", teacher);
        session.removeAttribute("currentUser");
        session.setAttribute("userType", "TEACHER");

        if (shouldRequirePasswordChange(teacher.getPassword(), rawPassword, teacher.getTeacherNo())) {
            session.setAttribute(PasswordSecurityUtils.FORCE_PASSWORD_CHANGE_SESSION_KEY, Boolean.TRUE);
            return "redirect:/force-password-change";
        }

        session.removeAttribute(PasswordSecurityUtils.FORCE_PASSWORD_CHANGE_SESSION_KEY);
        return "redirect:/?login=success";
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
    }

    private boolean shouldRequirePasswordChange(String storedPasswordHash, String rawPassword, String identifier) {
        return PasswordSecurityUtils.isDefaultSeedPasswordHash(storedPasswordHash)
                || PasswordSecurityUtils.isIdentifierDefaultPassword(rawPassword, identifier);
    }

    private boolean isDefaultIdentifierPasswordForCurrentSession(HttpSession session, String password) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null && PasswordSecurityUtils.isIdentifierDefaultPassword(password, currentUser.getUsername())) {
            return true;
        }

        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
        return currentTeacher != null
                && PasswordSecurityUtils.isIdentifierDefaultPassword(password, currentTeacher.getTeacherNo());
    }
}
