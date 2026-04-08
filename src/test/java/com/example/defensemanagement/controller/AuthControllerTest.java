package com.example.defensemanagement.controller;

import com.example.defensemanagement.common.ApiResponse;
import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.impl.RedisLoginFailureService;
import com.example.defensemanagement.util.ClientIpResolver;
import com.example.defensemanagement.util.PasswordSecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private static final String DEFAULT_SEED_HASH =
            "$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba";

    private AuthController controller;
    private AuthService authService;
    private RedisLoginFailureService loginFailureService;
    private ClientIpResolver clientIpResolver;

    @BeforeEach
    void setUp() {
        controller = new AuthController();
        authService = mock(AuthService.class);
        loginFailureService = mock(RedisLoginFailureService.class);
        clientIpResolver = mock(ClientIpResolver.class);

        ReflectionTestUtils.setField(controller, "authService", authService);
        ReflectionTestUtils.setField(controller, "loginFailureService", loginFailureService);
        ReflectionTestUtils.setField(controller, "clientIpResolver", clientIpResolver);
    }

    @Test
    void login_redirectsToForcedPasswordChangeWhenSeedPasswordHashIsUsed() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("captcha", "ok");
        MockHttpServletRequest request = new MockHttpServletRequest();
        ExtendedModelMap model = new ExtendedModelMap();

        User user = buildUser("admin", "SUPER_ADMIN", DEFAULT_SEED_HASH);
        when(clientIpResolver.resolveClientIp(any())).thenReturn("1.2.3.4");
        when(loginFailureService.isLocked("1.2.3.4")).thenReturn(false);
        when(authService.login("admin", "123456")).thenReturn(user);

        String result = controller.login("admin", "123456", "SUPER_ADMIN", "ok", session, request, model);

        assertEquals("redirect:/force-password-change", result);
        assertSame(user, session.getAttribute("currentUser"));
        assertEquals("USER", session.getAttribute("userType"));
        assertEquals(Boolean.TRUE, session.getAttribute(PasswordSecurityUtils.FORCE_PASSWORD_CHANGE_SESSION_KEY));
        verify(loginFailureService).clearFailure("1.2.3.4");
    }

    @Test
    void login_redirectsToForcedPasswordChangeWhenUserUsesIdentifierAsPassword() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("captcha", "ok");
        MockHttpServletRequest request = new MockHttpServletRequest();
        ExtendedModelMap model = new ExtendedModelMap();

        User user = buildUser("2021CS001", "STUDENT", "$2a$10$otherHashThatIsNotSeed");
        when(clientIpResolver.resolveClientIp(any())).thenReturn("1.2.3.4");
        when(loginFailureService.isLocked("1.2.3.4")).thenReturn(false);
        when(authService.login("2021CS001", "2021CS001")).thenReturn(user);

        String result = controller.login("2021CS001", "2021CS001", "STUDENT", "ok", session, request, model);

        assertEquals("redirect:/force-password-change", result);
        assertEquals(Boolean.TRUE, session.getAttribute(PasswordSecurityUtils.FORCE_PASSWORD_CHANGE_SESSION_KEY));
    }

    @Test
    void login_redirectsToForcedPasswordChangeWhenTeacherUsesTeacherNoAsPassword() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("captcha", "ok");
        MockHttpServletRequest request = new MockHttpServletRequest();
        ExtendedModelMap model = new ExtendedModelMap();

        Teacher teacher = new Teacher();
        teacher.setId(2L);
        teacher.setTeacherNo("T001");
        teacher.setName("张三教授");
        teacher.setPassword("$2a$10$otherHashThatIsNotSeed");

        when(clientIpResolver.resolveClientIp(any())).thenReturn("1.2.3.4");
        when(loginFailureService.isLocked("1.2.3.4")).thenReturn(false);
        when(authService.teacherLogin("T001", "T001")).thenReturn(teacher);

        String result = controller.login("T001", "T001", "TEACHER", "ok", session, request, model);

        assertEquals("redirect:/force-password-change", result);
        assertEquals(Boolean.TRUE, session.getAttribute(PasswordSecurityUtils.FORCE_PASSWORD_CHANGE_SESSION_KEY));
        assertSame(teacher, session.getAttribute("currentTeacher"));
    }

    @Test
    void login_rejectsPasswordLongerThanPolicy() {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest request = new MockHttpServletRequest();
        ExtendedModelMap model = new ExtendedModelMap();
        String tooLongPassword = "a".repeat(PasswordSecurityUtils.MAX_PASSWORD_LENGTH + 1);

        when(clientIpResolver.resolveClientIp(any())).thenReturn("1.2.3.4");

        String result = controller.login("admin", tooLongPassword, "SUPER_ADMIN", "ok", session, request, model);

        assertEquals("login", result);
        assertEquals("密码长度不合法。", model.getAttribute("error"));
        verifyNoInteractions(authService);
    }

    @Test
    void changePassword_rejectsNonAjaxRequest() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userType", "USER");
        session.setAttribute("currentUser", buildUser("admin", "SUPER_ADMIN", DEFAULT_SEED_HASH));

        ApiResponse<Map<String, String>> response = controller.changePassword(
                "old-password",
                "new-password-123",
                session,
                new MockHttpServletRequest()
        );

        assertFalse(response.isSuccess());
        assertEquals("非法请求", response.getMessage());
    }

    @Test
    void changePassword_clearsForcedPasswordChangeFlagOnSuccess() {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Requested-With", "XMLHttpRequest");

        User user = buildUser("student", "STUDENT", DEFAULT_SEED_HASH);
        session.setAttribute("userType", "STUDENT");
        session.setAttribute("currentUser", user);
        session.setAttribute(PasswordSecurityUtils.FORCE_PASSWORD_CHANGE_SESSION_KEY, Boolean.TRUE);

        when(authService.changeUserPassword(eq(user.getId()), eq("old-password"), eq("new-password-123"))).thenReturn(true);

        ApiResponse<Map<String, String>> response = controller.changePassword(
                "old-password",
                "new-password-123",
                session,
                request
        );

        assertTrue(response.isSuccess());
        assertNull(session.getAttribute(PasswordSecurityUtils.FORCE_PASSWORD_CHANGE_SESSION_KEY));
    }

    @Test
    void changePassword_rejectsIdentifierAsNewPassword() {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Requested-With", "XMLHttpRequest");

        User user = buildUser("2021CS001", "STUDENT", DEFAULT_SEED_HASH);
        session.setAttribute("userType", "STUDENT");
        session.setAttribute("currentUser", user);

        ApiResponse<Map<String, String>> response = controller.changePassword(
                "old-password",
                "2021CS001",
                session,
                request
        );

        assertFalse(response.isSuccess());
        assertEquals("新密码不能与账号相同", response.getMessage());
        verify(authService, never()).changeUserPassword(any(), any(), any());
    }

    private User buildUser(String username, String roleName, String passwordHash) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setRealName(username);
        user.setPassword(passwordHash);
        Role role = new Role();
        role.setName(roleName);
        user.setRole(role);
        return user;
    }
}
