package com.example.defensemanagement.interceptor.auth;

import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;

@Component
public class TeacherProfilePathValidator implements PathAccessValidator {

    @Override
    public AccessDecision validate(RequestAccessContext context) {
        if (!context.getPath().startsWith("/teacher/profile")) {
            return AccessDecision.abstain();
        }

        if (context.hasCurrentTeacher() || context.hasAnyRole("TEACHER", "DEFENSE_LEADER")) {
            return AccessDecision.allow();
        }
        return AccessDecision.deny(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
    }
}
