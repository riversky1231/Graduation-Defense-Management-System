package com.example.defensemanagement.interceptor.auth;

import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.service.AuthService;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;

@Component
public class DepartmentPathValidator implements PathAccessValidator {

    private final AuthService authService;
    private final DefenseGroupTeacherMapper defenseGroupTeacherMapper;

    public DepartmentPathValidator(AuthService authService, DefenseGroupTeacherMapper defenseGroupTeacherMapper) {
        this.authService = authService;
        this.defenseGroupTeacherMapper = defenseGroupTeacherMapper;
    }

    @Override
    public AccessDecision validate(RequestAccessContext context) {
        String path = context.getPath();
        if (!path.startsWith("/department/")) {
            return AccessDecision.abstain();
        }

        if (path.startsWith("/department/student/teacher/")) {
            if (context.hasCurrentTeacher()
                    || context.hasAnyRole("SUPER_ADMIN", "DEPT_ADMIN", "TEACHER", "DEFENSE_LEADER")) {
                return AccessDecision.allow();
            }
            return AccessDecision.deny(HttpServletResponse.SC_FORBIDDEN, "需要登录");
        }

        if (path.startsWith("/department/student/leader/")) {
            if (context.hasCurrentTeacher()) {
                Long teacherId = context.getCurrentTeacher().getId();
                if (authService.isDefenseLeader(teacherId, null)) {
                    return AccessDecision.allow();
                }
                DefenseGroupTeacher groupTeacher = defenseGroupTeacherMapper.findByTeacherId(teacherId);
                if (groupTeacher != null && groupTeacher.getIsLeader() != null && groupTeacher.getIsLeader() == 1) {
                    return AccessDecision.allow();
                }
                return AccessDecision.deny(HttpServletResponse.SC_FORBIDDEN, "需要答辩组长权限");
            }

            if (context.hasAnyRole("DEFENSE_LEADER", "SUPER_ADMIN")) {
                return AccessDecision.allow();
            }
            return AccessDecision.deny(HttpServletResponse.SC_FORBIDDEN, "需要答辩组长权限");
        }

        if (path.startsWith("/department/student")) {
            if (context.hasCurrentTeacher()) {
                if (isTeacherStudentReadPath(path)) {
                    return AccessDecision.allow();
                }
                if (context.getCurrentUser() != null
                        && authService.hasPermission(context.getCurrentUser(), "MANAGE_STUDENTS")) {
                    return AccessDecision.allow();
                }
                return AccessDecision.deny(HttpServletResponse.SC_FORBIDDEN, "权限不足");
            }

            if (context.getCurrentUser() == null) {
                return AccessDecision.deny(HttpServletResponse.SC_FORBIDDEN, "权限不足");
            }

            if (context.hasAnyRole("TEACHER") && isTeacherStudentReadPath(path)) {
                return AccessDecision.allow();
            }

            if (authService.hasPermission(context.getCurrentUser(), "MANAGE_STUDENTS")) {
                return AccessDecision.allow();
            }
            return AccessDecision.deny(HttpServletResponse.SC_FORBIDDEN, "权限不足");
        }

        if (path.startsWith("/department/group")
                || path.startsWith("/department/teachers")
                || path.startsWith("/department/defenseLeader")) {
            if (context.getCurrentUser() != null
                    && authService.hasPermission(context.getCurrentUser(), "MANAGE_TEACHERS")) {
                return AccessDecision.allow();
            }
            return AccessDecision.deny(HttpServletResponse.SC_FORBIDDEN, "权限不足");
        }

        if (path.startsWith("/department/volunteer")) {
            if (context.hasAnyRole("DEPT_ADMIN", "SUPER_ADMIN")) {
                return AccessDecision.allow();
            }
            return AccessDecision.deny(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
        }

        return AccessDecision.allow();
    }

    private boolean isTeacherStudentReadPath(String path) {
        return path.equals("/department/student/list")
                || path.equals("/department/student/currentYear")
                || path.equals("/department/student/groups");
    }
}
