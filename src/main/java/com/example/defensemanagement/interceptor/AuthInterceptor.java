package com.example.defensemanagement.interceptor;

import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private AuthService authService;

    @Autowired
    private DefenseGroupTeacherMapper defenseGroupTeacherMapper;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {
        // IMPORTANT: request.getRequestURI() includes contextPath (e.g.
        // "/app/admin/...") when deployed under a prefix.
        // Normalize to an application-relative path so all permission checks work in
        // both root and non-root deployments.
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = requestURI;
        if (contextPath != null && !contextPath.isEmpty() && requestURI.startsWith(contextPath)) {
            path = requestURI.substring(contextPath.length());
        }

        // 排除不需要权限验证的路径
        if (path.equals("/login") || path.equals("/captcha") || path.startsWith("/css/") ||
                path.startsWith("/js/") || path.startsWith("/images/") || path.equals("/image.png")) {
            return true;
        }

        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("currentUser");
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");

        // 检查是否已登录
        if (currentUser == null && currentTeacher == null) {
            response.sendRedirect("/login");
            return false;
        }

        // 检查特定权限
        if (path.startsWith("/admin/")) {
            // Allow GET requests for listing data
            if ((path.equals("/admin/users/list")
                    || path.equals("/admin/users/search")
                    || path.equals("/admin/departments/list")
                    || path.equals("/admin/roles/list"))
                    && "GET".equalsIgnoreCase(request.getMethod())) {
                return true;
            }

            // Allow user management operations; fine-grained permission is checked in
            // AdminController/PermissionService
            if ((path.startsWith("/admin/users/") || path.startsWith("/admin/user/"))
                    && ("POST".equalsIgnoreCase(request.getMethod())
                            || "DELETE".equalsIgnoreCase(request.getMethod())
                            || "PUT".equalsIgnoreCase(request.getMethod()))) {
                return true;
            }

            // Allow user save operations for users with proper permissions (checked in
            // controller)
            if (path.equals("/admin/users/save") && "POST".equalsIgnoreCase(request.getMethod())) {
                return true; // Permission will be checked in the controller
            }

            // Allow config API access for admins (SUPER_ADMIN and DEPT_ADMIN)
            if (path.startsWith("/admin/config/")) {
                if (currentUser != null && currentUser.getRole() != null) {
                    String roleName = currentUser.getRole().getName();
                    if ("SUPER_ADMIN".equals(roleName) || "DEPT_ADMIN".equals(roleName)) {
                        return true;
                    }
                }
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "权限不足");
                return false;
            }

            // For all other /admin/ paths (like department management), only super admins
            // are allowed
            if (currentUser == null || currentUser.getRole() == null || !"SUPER_ADMIN".equals(currentUser.getRole().getName())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "权限不足");
                return false;
            }
        }

        if (path.startsWith("/student/")) {
            if (currentUser != null && currentUser.getRole() != null &&
                    "STUDENT".equals(currentUser.getRole().getName())) {
                return true;
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "权限不足");
            return false;
        }

        if (path.startsWith("/teacher/volunteer")) {
            if (currentTeacher != null) {
                return true;
            }
            if (currentUser != null && currentUser.getRole() != null) {
                String roleName = currentUser.getRole().getName();
                if ("TEACHER".equals(roleName) || "DEFENSE_LEADER".equals(roleName)
                        || "SUPER_ADMIN".equals(roleName) || "DEPT_ADMIN".equals(roleName)) {
                    return true;
                }
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
            return false;
        }

        if (path.startsWith("/teacher/profile")) {
            if (currentTeacher != null) {
                return true;
            }
            if (currentUser != null && currentUser.getRole() != null) {
                String roleName = currentUser.getRole().getName();
                if ("TEACHER".equals(roleName) || "DEFENSE_LEADER".equals(roleName)) {
                    return true;
                }
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
            return false;
        }

        if (path.startsWith("/department/")) {
            // 学生管理：超级管理员、院系管理员可以管理，教师可以查看自己指导的学生
            if (path.startsWith("/department/student")) {
                // 教师专用接口：允许教师、答辩组长、超级管理员和院系管理员访问
                if (path.startsWith("/department/student/teacher/")) {
                    // 检查是否是教师（通过 currentTeacher 或 currentUser 的角色）
                    if (currentTeacher != null) {
                        return true;
                    }
                    if (currentUser != null && currentUser.getRole() != null) {
                        String roleName = currentUser.getRole().getName();
                        // 允许所有角色：超级管理员、院系管理员、答辩组长、教师
                        if ("SUPER_ADMIN".equals(roleName) || "DEPT_ADMIN".equals(roleName) || 
                            "TEACHER".equals(roleName) || "DEFENSE_LEADER".equals(roleName)) {
                            return true;
                        }
                    }
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "需要登录");
                    return false;
                }
                
                // 答辩组长专用接口：允许答辩组长访问
                if (path.startsWith("/department/student/leader/")) {
                    if (currentTeacher != null) {
                        // 方式1：defense_leader 表中有记录
                        if (authService.isDefenseLeader(currentTeacher.getId(), null)) {
                            return true;
                        }
                        // 方式2：defense_group_teacher 表中 is_leader=1（小组组长）
                        DefenseGroupTeacher gt = defenseGroupTeacherMapper.findByTeacherId(currentTeacher.getId());
                        if (gt != null && gt.getIsLeader() != null && gt.getIsLeader() == 1) {
                            return true;
                        }
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "需要答辩组长权限");
                        return false;
                    }
                    if (currentUser != null && currentUser.getRole() != null) {
                        String roleName = currentUser.getRole().getName();
                        if ("DEFENSE_LEADER".equals(roleName) || "SUPER_ADMIN".equals(roleName)) {
                            return true;
                        }
                    }
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "需要答辩组长权限");
                    return false;
                }
                
                // 允许教师访问（查看自己指导的学生）
                if (currentTeacher != null) {
                    // 教师可以访问 /list 接口查看自己指导的学生，以及获取当前年份和小组列表
                    // 注意：request.getRequestURI()不包含查询参数，所以直接匹配路径即可
                    if (path.equals("/department/student/list")
                            || path.equals("/department/student/currentYear")
                            || path.equals("/department/student/groups")) {
                        return true;
                    }
                    // 其他操作需要管理员权限
                    if (currentUser == null || !authService.hasPermission(currentUser, "MANAGE_STUDENTS")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "权限不足");
                        return false;
                    }
                } else if (currentUser != null) {
                    String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
                    // 教师角色（TEACHER）也可以查看学生列表
                    if ("TEACHER".equals(roleName) && 
                        (path.equals("/department/student/list")
                            || path.equals("/department/student/currentYear")
                            || path.equals("/department/student/groups"))) {
                        return true;
                    }
                    // 管理员可以管理学生
                    if (!authService.hasPermission(currentUser, "MANAGE_STUDENTS")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "权限不足");
                        return false;
                    }
                } else {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "权限不足");
                    return false;
                }
            } else if (path.startsWith("/department/group") || path.startsWith("/department/teachers")
                    || path.startsWith("/department/defenseLeader")) {
                // 小组教师管理、教师管理、答辩组长管理需要 MANAGE_TEACHERS 权限
                if (currentUser == null || !authService.hasPermission(currentUser, "MANAGE_TEACHERS")) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "权限不足");
                    return false;
                }
            } else if (path.startsWith("/department/volunteer")) {
                if (currentUser == null || currentUser.getRole() == null) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
                    return false;
                }
                String roleName = currentUser.getRole().getName();
                if (!"DEPT_ADMIN".equals(roleName) && !"SUPER_ADMIN".equals(roleName)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
                    return false;
                }

            } else {
                // 其他 /department/ 路径（如果有）需要相应权限，暂时允许通过，由 Controller 内部检查
                // 如果后续有新的 /department/ 路径，需要在这里添加相应的权限检查
                return true;
            }
        }

        if (path.startsWith("/defense/")) {
            // 教师小组打分和大组答辩相关 API，允许教师和管理员访问
            if (path.startsWith("/defense/score/teacher/") || path.startsWith("/defense/score/largegroup/")) {
                // 检查是否是教师或管理员（通过 currentTeacher 或 currentUser 的角色）
                if (currentTeacher != null) {
                    return true;
                }
                if (currentUser != null && currentUser.getRole() != null) {
                    String roleName = currentUser.getRole().getName();
                    if ("TEACHER".equals(roleName) || "DEFENSE_LEADER".equals(roleName) 
                        || "SUPER_ADMIN".equals(roleName) || "DEPT_ADMIN".equals(roleName)) {
                        return true;
                    }
                }
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "需要教师权限");
                return false;
            }
            
            // 评语相关 API，允许所有已登录用户访问（包括超级管理员、院系管理员、答辩组长、教师）
            if (path.startsWith("/defense/comment/")) {
                // 如果有教师会话，允许访问
                if (currentTeacher != null) {
                    return true;
                }
                // 如果有用户会话，检查角色
                if (currentUser != null && currentUser.getRole() != null) {
                    String roleName = currentUser.getRole().getName();
                    // 允许所有角色：超级管理员、院系管理员、答辩组长、教师
                    if ("SUPER_ADMIN".equals(roleName) || "DEPT_ADMIN".equals(roleName) || 
                        "TEACHER".equals(roleName) || "DEFENSE_LEADER".equals(roleName)) {
                        return true;
                    }
                }
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "需要登录");
                return false;
            }
            
            // 其他 /defense/ 路径需要 MANAGE_DEFENSE 权限或答辩组长权限
            boolean hasPermission = false;
            if (currentUser != null && authService.hasPermission(currentUser, "MANAGE_DEFENSE")) {
                hasPermission = true;
            } else if (currentTeacher != null && authService.isDefenseLeader(currentTeacher.getId(), null)) {
                hasPermission = true;
            }

            if (!hasPermission) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "权限不足");
                return false;
            }
        }

        return true;
    }
}
