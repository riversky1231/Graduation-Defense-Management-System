package com.example.defensemanagement.security;

import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionAuthenticationFilterTest {

    private final SessionAuthenticationFilter filter = new SessionAuthenticationFilter();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPopulateAuthenticationFromUserSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        Role role = new Role();
        role.setName("SUPER_ADMIN");
        user.setRole(role);
        request.getSession(true).setAttribute("currentUser", user);

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("admin", authentication.getName());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority())));
    }

    @Test
    void shouldPopulateAuthenticationFromTeacherSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        Teacher teacher = new Teacher();
        teacher.setId(2L);
        teacher.setTeacherNo("T1001");
        request.getSession(true).setAttribute("currentTeacher", teacher);

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("T1001", authentication.getName());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_TEACHER".equals(authority.getAuthority())));
    }
}
