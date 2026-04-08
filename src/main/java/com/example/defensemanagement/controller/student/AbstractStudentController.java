package com.example.defensemanagement.controller.student;

import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.TeacherMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

/**
 * 学生管理 Controller 基类。
 * 提供公共的权限检查和工具方法。
 */
@RestController
public abstract class AbstractStudentController {

    @Autowired
    protected TeacherMapper teacherMapper;

    /**
     * 检查当前用户是否为部门管理员（SUPER_ADMIN 或 DEPT_ADMIN）。
     * @return null 表示通过，返回错误信息字符串表示权限不足
     */
    protected String checkDeptAdmin(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "error:权限不足";
        }
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
        if ("SUPER_ADMIN".equals(roleName) || "DEPT_ADMIN".equals(roleName)) {
            return null;
        }
        return "error:权限不足";
    }

    /**
     * 根据用户ID查找对应的教师ID。
     */
    protected Long findTeacherIdByUserId(Long userId) {
        Teacher teacher = teacherMapper.findByUserId(userId);
        return teacher != null ? teacher.getId() : null;
    }
}
