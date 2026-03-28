package com.example.defensemanagement.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class SessionAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String path = request.getRequestURI();
        boolean apiRequest = "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || path.startsWith("/admin/")
                || path.startsWith("/department/")
                || path.startsWith("/defense/")
                || path.startsWith("/student/")
                || path.startsWith("/teacher/")
                || path.startsWith("/signature/")
                || path.startsWith("/health")
                || path.startsWith("/changePassword");

        if (apiRequest) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已失效");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/login");
    }
}
