package com.example.defensemanagement.interceptor.auth;

import com.example.defensemanagement.service.AuthService;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;

@Component
public class DefensePathValidator implements PathAccessValidator {

    private final AuthService authService;

    public DefensePathValidator(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public AccessDecision validate(RequestAccessContext context) {
        String path = context.getPath();
        if (!path.startsWith("/defense/")) {
            return AccessDecision.abstain();
        }

        if (path.startsWith("/defense/score/teacher/") || path.startsWith("/defense/score/largegroup/")) {
            if (context.hasCurrentTeacher()
                    || context.hasAnyRole("TEACHER", "DEFENSE_LEADER", "SUPER_ADMIN", "DEPT_ADMIN")) {
                return AccessDecision.allow();
            }
            return AccessDecision.deny(HttpServletResponse.SC_FORBIDDEN, "需要教师权限");
        }

        if (path.startsWith("/defense/comment/")) {
            if (context.hasCurrentTeacher()
                    || context.hasAnyRole("SUPER_ADMIN", "DEPT_ADMIN", "TEACHER", "DEFENSE_LEADER")) {
                return AccessDecision.allow();
            }
            return AccessDecision.deny(HttpServletResponse.SC_FORBIDDEN, "需要登录");
        }

        boolean hasPermission = context.getCurrentUser() != null
                && authService.hasPermission(context.getCurrentUser(), "MANAGE_DEFENSE");
        if (!hasPermission && context.hasCurrentTeacher()) {
            hasPermission = authService.isDefenseLeader(context.getCurrentTeacher().getId(), null);
        }

        if (hasPermission) {
            return AccessDecision.allow();
        }
        return AccessDecision.deny(HttpServletResponse.SC_FORBIDDEN, "权限不足");
    }
}
