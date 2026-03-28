package com.example.defensemanagement.interceptor;

import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component
public class AdminAuditInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger("AUDIT_LOG");

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return;
        }

        HttpSession session = request.getSession(false);
        String operator = "anonymous";
        String operatorType = "unknown";

        if (session != null) {
            User currentUser = (User) session.getAttribute("currentUser");
            Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
            if (currentUser != null) {
                operator = currentUser.getUsername() != null ? currentUser.getUsername() : String.valueOf(currentUser.getId());
                operatorType = currentUser.getRole() != null ? currentUser.getRole().getName() : "USER";
            } else if (currentTeacher != null) {
                operator = currentTeacher.getTeacherNo() != null ? currentTeacher.getTeacherNo() : String.valueOf(currentTeacher.getId());
                operatorType = "TEACHER";
            }
        }

        log.info("operator={} operatorType={} method={} path={} status={} ip={} error={}",
                operator,
                operatorType,
                method,
                request.getRequestURI(),
                response.getStatus(),
                request.getRemoteAddr(),
                ex != null ? ex.getClass().getSimpleName() : "none");
    }
}
