package com.example.defensemanagement.controller.score;

import com.example.defensemanagement.common.ApiResponse;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;

/**
 * Score controllers shared support for session resolution, response wrapping and
 * safe error messages.
 */
abstract class AbstractScoreControllerSupport {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final TeacherMapper teacherMapper;
    protected final StudentMapper studentMapper;
    protected final ConfigService configService;

    protected AbstractScoreControllerSupport(
            TeacherMapper teacherMapper,
            StudentMapper studentMapper,
            ConfigService configService) {
        this.teacherMapper = teacherMapper;
        this.studentMapper = studentMapper;
        this.configService = configService;
    }

    protected User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("currentUser");
    }

    protected Teacher getTeacherFromSession(HttpSession session) {
        Teacher teacher = (Teacher) session.getAttribute("currentTeacher");
        if (teacher != null) {
            log.debug("Resolved teacher from currentTeacher session attribute, teacherId={}", teacher.getId());
            return teacher;
        }

        User user = getCurrentUser(session);
        if (user == null) {
            log.debug("No currentUser session attribute available for teacher resolution");
            log.warn("Unable to resolve teacher from session");
            return null;
        }

        log.debug("Resolving teacher from currentUser session attribute, username={}", user.getUsername());
        if (user.getRole() != null) {
            String roleName = user.getRole().getName();
            log.debug("Current user role for teacher resolution={}", roleName);
            if ("TEACHER".equals(roleName) || "DEFENSE_LEADER".equals(roleName)) {
                teacher = teacherMapper.findByTeacherNo(user.getUsername());
                log.debug("Resolved teacher by username={}, teacherId={}",
                        user.getUsername(),
                        teacher != null ? teacher.getId() : null);
                return teacher;
            }
        }

        log.warn("Unable to resolve teacher from session");
        return null;
    }

    protected Integer getCurrentDefenseYear() {
        String yearStr = configService.getConfigValue("CURRENT_DEFENSE_YEAR");
        if (yearStr != null && !yearStr.isEmpty()) {
            try {
                return Integer.parseInt(yearStr);
            } catch (NumberFormatException e) {
                log.warn("CURRENT_DEFENSE_YEAR 配置非法: {}", yearStr);
            }
        }
        return LocalDate.now().getYear();
    }

    protected <T> ApiResponse<T> successResponse(String message, T data) {
        return ApiResponse.success(message, data);
    }

    protected ApiResponse<Void> successResponse(String message) {
        return ApiResponse.success(message);
    }

    protected <T> ApiResponse<T> errorResponse(String message) {
        return ApiResponse.error(message);
    }

    protected String safeMessage(Exception e, String fallbackMessage) {
        if (e instanceof IllegalArgumentException) {
            return e.getMessage();
        }
        if (e instanceof ResponseStatusException) {
            String reason = ((ResponseStatusException) e).getReason();
            return reason != null && !reason.isBlank() ? reason : fallbackMessage;
        }
        log.error("Unhandled score controller exception", e);
        return fallbackMessage;
    }
}
