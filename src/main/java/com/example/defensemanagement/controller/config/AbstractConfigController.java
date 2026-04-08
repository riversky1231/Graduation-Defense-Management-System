package com.example.defensemanagement.controller.config;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.service.ConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpSession;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Set;

abstract class AbstractConfigController {

    protected static final int MIN_YEAR = 2000;
    protected static final int MAX_YEAR = 2100;
    protected static final Set<String> ALLOWED_DATE_KEY_PREFIXES = Set.of("DEFENSE_DATE", "GRADE_DATE");
    protected static final Set<String> ALLOWED_TEMPLATE_KEYS = Set.of("PAPER_PROMPT", "DESIGN_PROMPT");

    protected final ConfigService configService;

    protected AbstractConfigController(ConfigService configService) {
        this.configService = configService;
    }

    protected String checkAdmin(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "error:未登录";
        }
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
        if (!"SUPER_ADMIN".equals(roleName) && !"DEPT_ADMIN".equals(roleName)) {
            return "error:权限不足";
        }
        return null;
    }

    protected void requireAdmin(HttpSession session) {
        if (checkAdmin(session) != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "权限不足");
        }
    }

    protected boolean isAllowedYear(Integer year) {
        return year != null && year >= MIN_YEAR && year <= MAX_YEAR;
    }

    protected boolean isValidCalendarDate(Integer year, Integer month, Integer day) {
        if (!isAllowedYear(year) || month == null || day == null || month < 1 || month > 12 || day < 1 || day > 31) {
            return false;
        }
        try {
            LocalDate.of(year, month, day);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }

    protected String normalizeUpperCase(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
