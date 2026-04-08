package com.example.defensemanagement.interceptor.auth;

import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;

@Component
public class StudentPathValidator implements PathAccessValidator {

    @Override
    public AccessDecision validate(RequestAccessContext context) {
        if (!context.getPath().startsWith("/student/")) {
            return AccessDecision.abstain();
        }

        if (context.hasAnyRole("STUDENT")) {
            return AccessDecision.allow();
        }
        return AccessDecision.deny(HttpServletResponse.SC_FORBIDDEN, "权限不足");
    }
}
