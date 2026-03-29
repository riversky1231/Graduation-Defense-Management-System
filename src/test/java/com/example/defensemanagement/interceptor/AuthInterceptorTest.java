package com.example.defensemanagement.interceptor;

import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    @Mock
    private AuthService authService;

    @Mock
    private com.example.defensemanagement.mapper.DefenseGroupTeacherMapper defenseGroupTeacherMapper;

    private AuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new AuthInterceptor();
        ReflectionTestUtils.setField(interceptor, "authService", authService);
        ReflectionTestUtils.setField(interceptor, "defenseGroupTeacherMapper", defenseGroupTeacherMapper);
    }

    @Test
    void studentRoleCanAccessStudentEndpoints() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/student/profile");
        request.getSession(true).setAttribute("currentUser", userWithRole("STUDENT"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
        assertEquals(200, response.getStatus());
    }

    @Test
    void deptAdminCanAccessAdminConfigEndpoints() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/config/volunteer/get");
        request.getSession(true).setAttribute("currentUser", userWithRole("DEPT_ADMIN"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
        assertEquals(200, response.getStatus());
    }

    @Test
    void ordinaryTeacherCannotAccessLeaderOnlyStudentEndpoints() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/department/student/leader/group/scores");
        Teacher teacher = new Teacher();
        teacher.setId(10L);
        request.getSession(true).setAttribute("currentTeacher", teacher);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(authService.isDefenseLeader(10L, null)).thenReturn(false);

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(403, response.getStatus());
    }

    @Test
    void defenseLeaderRoleCanAccessLeaderOnlyStudentEndpoints() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/department/student/leader/group/scores");
        request.getSession(true).setAttribute("currentUser", userWithRole("DEFENSE_LEADER"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
        assertEquals(200, response.getStatus());
    }

    private User userWithRole(String roleName) {
        User user = new User();
        Role role = new Role();
        role.setName(roleName);
        user.setRole(role);
        return user;
    }
}
