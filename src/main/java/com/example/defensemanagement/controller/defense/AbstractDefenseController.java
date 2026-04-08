package com.example.defensemanagement.controller.defense;

import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.DefenseService;

import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;

/**
 * 答辩模块共享基类。
 */
abstract class AbstractDefenseController {

    protected final DefenseService defenseService;
    protected final DefenseGroupTeacherMapper defenseGroupTeacherMapper;
    protected final DefenseGroupMapper defenseGroupMapper;
    protected final TeacherMapper teacherMapper;
    protected final DepartmentMapper departmentMapper;

    protected AbstractDefenseController(
            DefenseService defenseService,
            DefenseGroupTeacherMapper defenseGroupTeacherMapper,
            DefenseGroupMapper defenseGroupMapper,
            TeacherMapper teacherMapper,
            DepartmentMapper departmentMapper) {
        this.defenseService = defenseService;
        this.defenseGroupTeacherMapper = defenseGroupTeacherMapper;
        this.defenseGroupMapper = defenseGroupMapper;
        this.teacherMapper = teacherMapper;
        this.departmentMapper = departmentMapper;
    }

    protected User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("currentUser");
    }

    protected Teacher resolveCurrentTeacher(HttpSession session) {
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
        if (currentTeacher != null) {
            return currentTeacher;
        }

        User currentUser = getCurrentUser(session);
        if (currentUser != null && currentUser.getRole() != null) {
            String roleName = currentUser.getRole().getName();
            if ("TEACHER".equals(roleName) || "DEFENSE_LEADER".equals(roleName)) {
                Teacher teacher = teacherMapper.findByUserId(currentUser.getId());
                if (teacher != null) {
                    session.setAttribute("currentTeacher", teacher);
                }
                return teacher;
            }
        }
        return null;
    }

    protected boolean isDefenseLeader(Long teacherId) {
        if (teacherId == null) {
            return false;
        }
        List<DefenseGroupTeacher> allGroups = defenseGroupTeacherMapper.findAll();
        if (allGroups == null) {
            return false;
        }
        for (DefenseGroupTeacher groupTeacher : allGroups) {
            if (groupTeacher.getTeacherId() != null
                    && groupTeacher.getTeacherId().equals(teacherId)
                    && groupTeacher.getIsLeader() != null
                    && groupTeacher.getIsLeader() == 1) {
                return true;
            }
        }
        return false;
    }

    protected List<DefenseGroup> resolveVisibleGroups(User currentUser, Teacher currentTeacher) {
        if (currentUser != null && currentUser.getRole() != null) {
            String roleName = currentUser.getRole().getName();
            if ("SUPER_ADMIN".equals(roleName)) {
                return defenseService.getAllGroups();
            }
            if ("DEPT_ADMIN".equals(roleName)) {
                return findGroupsByDepartment(currentUser.getDepartmentId());
            }
            return findGroupsByDepartment(currentTeacher != null ? currentTeacher.getDepartmentId() : null);
        }
        if (currentTeacher != null) {
            return findGroupsByDepartment(currentTeacher.getDepartmentId());
        }
        return Collections.emptyList();
    }

    protected String getRoleName(User user) {
        return user != null && user.getRole() != null ? user.getRole().getName() : null;
    }

    protected List<DefenseGroup> findGroupsByDepartment(Long departmentId) {
        if (departmentId == null) {
            return Collections.emptyList();
        }
        List<DefenseGroup> groups = defenseGroupMapper.findByDepartmentId(departmentId);
        return groups != null ? groups : Collections.emptyList();
    }
}
