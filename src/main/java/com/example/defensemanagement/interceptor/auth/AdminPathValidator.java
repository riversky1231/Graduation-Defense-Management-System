package com.example.defensemanagement.interceptor.auth;

import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;

@Component
public class AdminPathValidator implements PathAccessValidator {

    @Override
    public AccessDecision validate(RequestAccessContext context) {
        String path = context.getPath();
        if (!path.startsWith("/admin/")) {
            return AccessDecision.abstain();
        }

        if ((path.equals("/admin/users/list")
                || path.equals("/admin/users/search")
                || path.equals("/admin/departments/list")
                || path.equals("/admin/roles/list"))
                && "GET".equalsIgnoreCase(context.getMethod())) {
            return AccessDecision.allow();
        }

        if ((path.startsWith("/admin/users/") || path.startsWith("/admin/user/"))
                && ("POST".equalsIgnoreCase(context.getMethod())
                        || "DELETE".equalsIgnoreCase(context.getMethod())
                        || "PUT".equalsIgnoreCase(context.getMethod()))) {
            return AccessDecision.allow();
        }

        if (path.equals("/admin/users/save") && "POST".equalsIgnoreCase(context.getMethod())) {
            return AccessDecision.allow();
        }

        if (path.startsWith("/admin/config/")) {
            if (context.hasAnyRole("SUPER_ADMIN", "DEPT_ADMIN")) {
                return AccessDecision.allow();
            }
            return AccessDecision.deny(HttpServletResponse.SC_FORBIDDEN, "权限不足");
        }

        if (context.hasAnyRole("SUPER_ADMIN")) {
            return AccessDecision.allow();
        }
        return AccessDecision.deny(HttpServletResponse.SC_FORBIDDEN, "权限不足");
    }
}
