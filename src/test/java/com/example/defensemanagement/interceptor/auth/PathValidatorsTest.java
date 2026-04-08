package com.example.defensemanagement.interceptor.auth;

import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PathValidatorsTest {

    @Mock
    private AuthService authService;

    @Mock
    private DefenseGroupTeacherMapper defenseGroupTeacherMapper;

    private AdminPathValidator adminPathValidator;
    private StudentPathValidator studentPathValidator;
    private TeacherVolunteerPathValidator teacherVolunteerPathValidator;
    private TeacherProfilePathValidator teacherProfilePathValidator;
    private DepartmentPathValidator departmentPathValidator;
    private DefensePathValidator defensePathValidator;

    @BeforeEach
    void setUp() {
        adminPathValidator = new AdminPathValidator();
        studentPathValidator = new StudentPathValidator();
        teacherVolunteerPathValidator = new TeacherVolunteerPathValidator();
        teacherProfilePathValidator = new TeacherProfilePathValidator();
        departmentPathValidator = new DepartmentPathValidator(authService, defenseGroupTeacherMapper);
        defensePathValidator = new DefensePathValidator(authService);
    }

    @Test
    void adminValidatorHandlesAbstainAllowAndDenyCases() {
        assertFalse(adminPathValidator.validate(context("/student/profile", "GET", null, null)).isMatched());
        assertTrue(adminPathValidator.validate(context("/admin/users/list", "GET", null, null)).isAllowed());
        assertTrue(adminPathValidator.validate(context("/admin/users/1", "DELETE", null, null)).isAllowed());
        assertTrue(adminPathValidator.validate(context("/admin/users/save", "POST", null, null)).isAllowed());
        assertTrue(adminPathValidator.validate(context("/admin/config/system", "GET", user("DEPT_ADMIN"), null)).isAllowed());

        AccessDecision deniedConfig = adminPathValidator.validate(context("/admin/config/system", "GET", user("TEACHER"), null));
        assertFalse(deniedConfig.isAllowed());
        assertEquals(HttpServletResponse.SC_FORBIDDEN, deniedConfig.getStatusCode());

        assertTrue(adminPathValidator.validate(context("/admin/other", "GET", user("SUPER_ADMIN"), null)).isAllowed());
        assertFalse(adminPathValidator.validate(context("/admin/other", "GET", user("DEPT_ADMIN"), null)).isAllowed());
    }

    @Test
    void studentValidatorHandlesAbstainAllowAndDeny() {
        assertFalse(studentPathValidator.validate(context("/teacher/profile", "GET", null, null)).isMatched());
        assertTrue(studentPathValidator.validate(context("/student/profile", "GET", user("STUDENT"), null)).isAllowed());
        assertFalse(studentPathValidator.validate(context("/student/profile", "GET", user("TEACHER"), null)).isAllowed());
    }

    @Test
    void teacherVolunteerValidatorAcceptsTeacherSessionAndAllowedRoles() {
        assertFalse(teacherVolunteerPathValidator.validate(context("/student/profile", "GET", null, null)).isMatched());
        assertTrue(teacherVolunteerPathValidator.validate(context("/teacher/volunteer/list", "GET", null, teacher(1L))).isAllowed());
        assertTrue(teacherVolunteerPathValidator.validate(context("/teacher/volunteer/list", "GET", user("DEFENSE_LEADER"), null)).isAllowed());
        assertFalse(teacherVolunteerPathValidator.validate(context("/teacher/volunteer/list", "GET", user("STUDENT"), null)).isAllowed());
    }

    @Test
    void teacherProfileValidatorAcceptsTeacherContextAndRejectsOthers() {
        assertFalse(teacherProfilePathValidator.validate(context("/student/profile", "GET", null, null)).isMatched());
        assertTrue(teacherProfilePathValidator.validate(context("/teacher/profile/view", "GET", null, teacher(1L))).isAllowed());
        assertTrue(teacherProfilePathValidator.validate(context("/teacher/profile/view", "GET", user("TEACHER"), null)).isAllowed());
        assertFalse(teacherProfilePathValidator.validate(context("/teacher/profile/view", "GET", user("DEPT_ADMIN"), null)).isAllowed());
    }

    @Test
    void departmentValidatorAllowsTeacherStudentEndpointsForTeacherRoles() {
        assertTrue(departmentPathValidator.validate(context("/department/student/teacher/advised", "GET", null, teacher(1L))).isAllowed());
        assertTrue(departmentPathValidator.validate(context("/department/student/teacher/advised", "GET", user("TEACHER"), null)).isAllowed());
        assertFalse(departmentPathValidator.validate(context("/department/student/teacher/advised", "GET", user("STUDENT"), null)).isAllowed());
    }

    @Test
    void departmentValidatorAllowsLeaderEndpointForRecognizedDefenseLeader() {
        when(authService.isDefenseLeader(9L, null)).thenReturn(true);

        AccessDecision decision = departmentPathValidator.validate(
                context("/department/student/leader/group/scores", "GET", null, teacher(9L)));

        assertTrue(decision.isAllowed());
    }

    @Test
    void departmentValidatorFallsBackToGroupLeaderMapper() {
        DefenseGroupTeacher groupTeacher = new DefenseGroupTeacher();
        groupTeacher.setTeacherId(8L);
        groupTeacher.setIsLeader(1);
        when(authService.isDefenseLeader(8L, null)).thenReturn(false);
        when(defenseGroupTeacherMapper.findByTeacherId(8L)).thenReturn(groupTeacher);

        AccessDecision decision = departmentPathValidator.validate(
                context("/department/student/leader/group/scores", "GET", null, teacher(8L)));

        assertTrue(decision.isAllowed());
    }

    @Test
    void departmentValidatorRejectsLeaderEndpointWithoutLeaderRights() {
        when(authService.isDefenseLeader(7L, null)).thenReturn(false);
        when(defenseGroupTeacherMapper.findByTeacherId(7L)).thenReturn(null);

        AccessDecision decision = departmentPathValidator.validate(
                context("/department/student/leader/group/scores", "GET", null, teacher(7L)));

        assertFalse(decision.isAllowed());
        assertEquals("需要答辩组长权限", decision.getMessage());
    }

    @Test
    void departmentValidatorAllowsStudentReadListForTeacherAndTeacherRole() {
        assertTrue(departmentPathValidator.validate(context("/department/student/list", "GET", null, teacher(3L))).isAllowed());
        assertTrue(departmentPathValidator.validate(context("/department/student/groups", "GET", user("TEACHER"), null)).isAllowed());
    }

    @Test
    void departmentValidatorAllowsManageStudentsPermissionAndRejectsOthers() {
        User manager = user("DEPT_ADMIN");
        when(authService.hasPermission(manager, "MANAGE_STUDENTS")).thenReturn(true);

        assertTrue(departmentPathValidator.validate(context("/department/student/save", "POST", manager, null)).isAllowed());
        assertFalse(departmentPathValidator.validate(context("/department/student/save", "POST", user("STUDENT"), null)).isAllowed());
        assertFalse(departmentPathValidator.validate(context("/department/student/save", "POST", null, null)).isAllowed());
    }

    @Test
    void departmentValidatorChecksTeacherManagementPermission() {
        User manager = user("DEPT_ADMIN");
        when(authService.hasPermission(manager, "MANAGE_TEACHERS")).thenReturn(true);

        assertTrue(departmentPathValidator.validate(context("/department/group/list", "GET", manager, null)).isAllowed());
        assertFalse(departmentPathValidator.validate(context("/department/teachers", "GET", user("TEACHER"), null)).isAllowed());
    }

    @Test
    void departmentValidatorRestrictsVolunteerEndpointsAndAllowsUnknownDepartmentPaths() {
        assertTrue(departmentPathValidator.validate(context("/department/volunteer/list", "GET", user("SUPER_ADMIN"), null)).isAllowed());
        assertFalse(departmentPathValidator.validate(context("/department/volunteer/list", "GET", user("TEACHER"), null)).isAllowed());
        assertTrue(departmentPathValidator.validate(context("/department/other", "GET", user("STUDENT"), null)).isAllowed());
    }

    @Test
    void defenseValidatorHandlesScoreCommentAndPermissionChecks() {
        assertFalse(defensePathValidator.validate(context("/student/profile", "GET", null, null)).isMatched());
        assertTrue(defensePathValidator.validate(context("/defense/score/teacher/1", "GET", null, teacher(1L))).isAllowed());
        assertFalse(defensePathValidator.validate(context("/defense/score/teacher/1", "GET", user("STUDENT"), null)).isAllowed());
        assertTrue(defensePathValidator.validate(context("/defense/comment/generate", "POST", user("DEPT_ADMIN"), null)).isAllowed());
        assertFalse(defensePathValidator.validate(context("/defense/comment/generate", "POST", user("STUDENT"), null)).isAllowed());
    }

    @Test
    void defenseValidatorAllowsManageDefensePermissionAndLeaderFallback() {
        User manager = user("DEPT_ADMIN");
        when(authService.hasPermission(manager, "MANAGE_DEFENSE")).thenReturn(true);
        assertTrue(defensePathValidator.validate(context("/defense/panel", "GET", manager, null)).isAllowed());

        when(authService.isDefenseLeader(5L, null)).thenReturn(true);
        assertTrue(defensePathValidator.validate(context("/defense/panel", "GET", null, teacher(5L))).isAllowed());

        when(authService.isDefenseLeader(6L, null)).thenReturn(false);
        assertFalse(defensePathValidator.validate(context("/defense/panel", "GET", null, teacher(6L))).isAllowed());
    }

    private RequestAccessContext context(String path, String method, User user, Teacher teacher) {
        return new RequestAccessContext(path, method, user, teacher);
    }

    private User user(String roleName) {
        User user = new User();
        Role role = new Role();
        role.setName(roleName);
        user.setRole(role);
        return user;
    }

    private Teacher teacher(Long id) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        return teacher;
    }
}
