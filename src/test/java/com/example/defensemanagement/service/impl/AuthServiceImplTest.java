package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseLeaderMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    private static final String DEFAULT_SEED_HASH =
            "$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba";

    private AuthServiceImpl authService;
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl();
        userMapper = mock(UserMapper.class);

        ReflectionTestUtils.setField(authService, "userMapper", userMapper);
        ReflectionTestUtils.setField(authService, "teacherMapper", mock(TeacherMapper.class));
        ReflectionTestUtils.setField(authService, "defenseLeaderMapper", mock(DefenseLeaderMapper.class));
    }

    @Test
    void login_allowsSeededDeptAdminWithConfiguredBootstrapSecret() {
        User user = buildUser("cs_admin", "DEPT_ADMIN", DEFAULT_SEED_HASH);
        when(userMapper.findByUsername("cs_admin")).thenReturn(user);
        ReflectionTestUtils.setField(authService, "initialPrivilegedPassword", "bootstrap-secret");

        User result = authService.login("cs_admin", "bootstrap-secret");

        assertSame(user, result);
    }

    @Test
    void login_rejectsSeededPrivilegedAccountWithoutBootstrapSecret() {
        User user = buildUser("cs_admin", "DEPT_ADMIN", DEFAULT_SEED_HASH);
        when(userMapper.findByUsername("cs_admin")).thenReturn(user);
        ReflectionTestUtils.setField(authService, "initialPrivilegedPassword", "");

        User result = authService.login("cs_admin", "whatever");

        assertNull(result);
    }

    @Test
    void login_acceptsRegularPasswordForNonPrivilegedUser() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        User user = buildUser("2021CS001", "STUDENT", encoder.encode("2021CS001"));
        when(userMapper.findByUsername("2021CS001")).thenReturn(user);

        User result = authService.login("2021CS001", "2021CS001");

        assertSame(user, result);
    }

    private User buildUser(String username, String roleName, String passwordHash) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordHash);
        user.setStatus(1);

        Role role = new Role();
        role.setName(roleName);
        user.setRole(role);
        return user;
    }
}
