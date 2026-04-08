package com.example.defensemanagement.interceptor;

import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.interceptor.auth.AccessDecision;
import com.example.defensemanagement.interceptor.auth.AdminPathValidator;
import com.example.defensemanagement.interceptor.auth.DefensePathValidator;
import com.example.defensemanagement.interceptor.auth.DepartmentPathValidator;
import com.example.defensemanagement.interceptor.auth.PathAccessValidator;
import com.example.defensemanagement.interceptor.auth.RequestAccessContext;
import com.example.defensemanagement.interceptor.auth.StudentPathValidator;
import com.example.defensemanagement.interceptor.auth.TeacherProfilePathValidator;
import com.example.defensemanagement.interceptor.auth.TeacherVolunteerPathValidator;
import com.example.defensemanagement.util.PasswordSecurityUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final List<PathAccessValidator> validators;

    public AuthInterceptor(
            AdminPathValidator adminPathValidator,
            StudentPathValidator studentPathValidator,
            TeacherVolunteerPathValidator teacherVolunteerPathValidator,
            TeacherProfilePathValidator teacherProfilePathValidator,
            DepartmentPathValidator departmentPathValidator,
            DefensePathValidator defensePathValidator) {
        this.validators = List.of(
                adminPathValidator,
                studentPathValidator,
                teacherVolunteerPathValidator,
                teacherProfilePathValidator,
                departmentPathValidator,
                defensePathValidator);
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {
        String path = normalizePath(request);
        if (isPublicPath(path)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        User currentUser = (User) session.getAttribute("currentUser");
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
        if (currentUser == null && currentTeacher == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        if (Boolean.TRUE.equals(session.getAttribute(PasswordSecurityUtils.FORCE_PASSWORD_CHANGE_SESSION_KEY))
                && !isAllowedDuringForcedPasswordChange(path, request.getMethod())) {
            response.sendRedirect(request.getContextPath() + "/force-password-change");
            return false;
        }

        RequestAccessContext context = new RequestAccessContext(path, request.getMethod(), currentUser, currentTeacher);
        for (PathAccessValidator validator : validators) {
            AccessDecision decision = validator.validate(context);
            if (!decision.isMatched()) {
                continue;
            }
            if (decision.isAllowed()) {
                return true;
            }
            response.sendError(decision.getStatusCode(), decision.getMessage());
            return false;
        }

        return true;
    }

    private String normalizePath(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && requestURI.startsWith(contextPath)) {
            return requestURI.substring(contextPath.length());
        }
        return requestURI;
    }

    private boolean isPublicPath(String path) {
        return path.equals("/login")
                || path.equals("/captcha")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.equals("/image.png");
    }

    private boolean isAllowedDuringForcedPasswordChange(String path, String method) {
        return path.equals("/force-password-change")
                || path.equals("/logout")
                || (path.equals("/changePassword") && "POST".equalsIgnoreCase(method));
    }
}
