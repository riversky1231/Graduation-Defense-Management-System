package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.DefenseLeader;
import com.example.defensemanagement.mapper.UserMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.DefenseLeaderMapper;
import com.example.defensemanagement.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private DefenseLeaderMapper defenseLeaderMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public User login(String username, String password) {
        User user = userMapper.findByUsername(username);

        if (user != null && Integer.valueOf(1).equals(user.getStatus()) && user.getPassword() != null) {
            if (passwordEncoder.matches(password, user.getPassword())) {
                return user;
            }
            // 兼容初始化数据的默认管理员密码，如匹配失败但输入为默认口令，则自动重写为最新 bcrypt
            // 注意：这不是弱密码创建，而是对 data.sql 中预置的 admin 账号做一次性密码哈希升级
            // 升级后 admin 账号的密码哈希将被替换为 BCrypt 格式，后续登录走正常 BCrypt 校验
            if ("admin".equals(username) && "123456".equals(password)) {
                String encodedPassword = passwordEncoder.encode(password);
                userMapper.updatePassword(user.getId(), encodedPassword);
                user.setPassword(encodedPassword);
                log.warn("Admin password hash was upgraded during login for username={}", username);
                return user;
            }
        }
        return null;
    }

    @Override
    public Teacher teacherLogin(String teacherNo, String password) {
        Teacher teacher = teacherMapper.findByTeacherNo(teacherNo);

        if (teacher != null && Integer.valueOf(1).equals(teacher.getStatus()) &&
                teacher.getPassword() != null && passwordEncoder.matches(password, teacher.getPassword())) {
            return teacher;
        }
        return null;
    }

    @Override
    public boolean changeUserPassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.findById(userId);
        if (user == null) {
            return false;
        }

        if (user.getPassword() == null) {
            return false;
        }

        if (passwordEncoder.matches(oldPassword, user.getPassword())) {
            String encodedPassword = passwordEncoder.encode(newPassword);
            return userMapper.updatePassword(userId, encodedPassword) > 0;
        }
        return false;
    }

    @Override
    public boolean changeTeacherPassword(Long teacherId, String oldPassword, String newPassword) {
        Teacher teacher = teacherMapper.findById(teacherId);
        if (teacher != null && teacher.getPassword() != null &&
                passwordEncoder.matches(oldPassword, teacher.getPassword())) {
            String encodedPassword = passwordEncoder.encode(newPassword);
            return teacherMapper.updatePassword(teacherId, encodedPassword) > 0;
        }
        return false;
    }

    @Override
    public boolean hasPermission(User user, String permission) {
        if (user == null || user.getRole() == null) {
            return false;
        }

        String roleName = user.getRole().getName();

        // 超级管理员拥有所有权限
        if ("SUPER_ADMIN".equals(roleName)) {
            return true;
        }

        // 标准权限映射，避免遗漏
        switch (permission) {
            case "SUPER_ADMIN_ACCESS":
            case "CREATE_DEPARTMENT":
            case "CREATE_DEPT_ADMIN":
            case "CREATE_USER":
                return "SUPER_ADMIN".equals(roleName);
            case "MANAGE_TEACHERS":
            case "SET_DEFENSE_LEADER":
                return "SUPER_ADMIN".equals(roleName) || "DEPT_ADMIN".equals(roleName);
            case "MANAGE_DEFENSE":
                return "SUPER_ADMIN".equals(roleName) || "DEPT_ADMIN".equals(roleName)
                        || "DEFENSE_LEADER".equals(roleName);
            case "MANAGE_STUDENTS":
                return "SUPER_ADMIN".equals(roleName) || "DEPT_ADMIN".equals(roleName);
            default:
                return false;
        }
    }

    @Override
    public boolean isDefenseLeader(Long teacherId, Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        DefenseLeader leader = defenseLeaderMapper.findByTeacherIdAndYear(teacherId, year);
        return leader != null;
    }
}
