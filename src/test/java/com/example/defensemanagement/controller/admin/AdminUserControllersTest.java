package com.example.defensemanagement.controller.admin;

import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.PermissionService;
import com.example.defensemanagement.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllersTest {

    @Mock
    private UserService userService;
    @Mock
    private AuthService authService;
    @Mock
    private PermissionService permissionService;
    @Mock
    private TeacherMapper teacherMapper;
    @Mock
    private UserExcelImportSupport userExcelImportSupport;
    @Mock
    private UserImportTemplateSupport userImportTemplateSupport;

    private AdminUserQueryController queryController;
    private AdminUserCrudController crudController;
    private AdminUserImportController importController;

    @BeforeEach
    void setUp() {
        queryController = new AdminUserQueryController(userService, authService, teacherMapper);
        crudController = new AdminUserCrudController(userService, authService, teacherMapper, permissionService);
        importController = new AdminUserImportController(
                userService,
                authService,
                teacherMapper,
                userExcelImportSupport,
                userImportTemplateSupport);
    }

    @Test
    void searchUsesDepartmentScopeForDeptAdmin() {
        MockHttpSession session = sessionWithUser(1L, "DEPT_ADMIN", 6L);
        when(userService.searchUsers("张", 2, 3, 6L)).thenReturn(List.of(new User(), new User()));
        when(userService.countUsers("张", 6L)).thenReturn(5);

        Map<String, Object> result = queryController.search("张", 2, 3, session);

        assertEquals(5, result.get("total"));
        assertEquals(2, result.get("currentPage"));
        assertEquals(3, result.get("pageSize"));
        assertEquals(2, result.get("totalPages"));
        assertEquals(2, ((List<?>) result.get("users")).size());
    }

    @Test
    void roleListThrowsWhenUnauthenticated() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> queryController.roleList(new MockHttpSession()));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void saveRejectsUnauthorizedCreate() {
        MockHttpSession session = sessionWithUser(1L, "DEPT_ADMIN", 6L);
        User target = new User();
        when(permissionService.canCreateUser(any(), eq(target))).thenReturn(false);

        String result = crudController.save(target, session);

        assertEquals("error:权限不足，无法创建该角色的用户", result);
    }

    @Test
    void deleteRejectsSelfDeletion() {
        MockHttpSession session = sessionWithUser(8L, "SUPER_ADMIN", null);
        User target = new User();
        target.setId(8L);
        when(userService.findById(8L)).thenReturn(target);
        when(permissionService.canEditUser(any(), eq(target))).thenReturn(true);

        String result = crudController.delete(8L, session);

        assertEquals("error:不能删除自己", result);
    }

    @Test
    void batchDeleteReturnsSummaryWhenPartiallySuccessful() {
        MockHttpSession session = sessionWithUser(1L, "SUPER_ADMIN", null);
        User target = new User();
        target.setId(2L);
        target.setUsername("teacher02");
        when(userService.findById(2L)).thenReturn(target);
        when(userService.findById(3L)).thenReturn(null);
        when(permissionService.canEditUser(any(), eq(target))).thenReturn(true);
        when(userService.deleteUser(2L)).thenReturn(true);

        String result = crudController.batchDelete(List.of(1L, 2L, 3L), session);

        assertTrue(result.startsWith("error:成功删除1个，失败2个。"));
        assertTrue(result.contains("不能删除自己"));
        assertTrue(result.contains("不存在"));
    }

    @Test
    void downloadTemplateReturnsAttachmentWhenAuthorized() throws Exception {
        MockHttpSession session = sessionWithUser(1L, "SUPER_ADMIN", null);
        when(authService.hasPermission(any(User.class), eq("CREATE_USER"))).thenReturn(true);
        when(userImportTemplateSupport.buildTemplateBytes()).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> response = importController.downloadTemplate(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertTrue(disposition.contains("attachment; filename=\""));
        assertTrue(disposition.endsWith(".xlsx\""));
        assertEquals(3, response.getBody().length);
    }

    @Test
    void importExcelDelegatesWhenAuthorized() {
        MockHttpSession session = sessionWithUser(1L, "SUPER_ADMIN", null);
        MockMultipartFile file = new MockMultipartFile("file", "users.xlsx", "application/octet-stream", new byte[]{1});
        when(authService.hasPermission(any(User.class), eq("CREATE_USER"))).thenReturn(true);
        when(userExcelImportSupport.importUsers(file)).thenReturn("success:成功导入1条");

        String result = importController.importExcel(file, session);

        assertEquals("success:成功导入1条", result);
        verify(userExcelImportSupport).importUsers(file);
    }

    @Test
    void isDefenseLeaderReturnsTrueForTeacherUser() {
        MockHttpSession session = sessionWithUser(1L, "SUPER_ADMIN", null);
        User target = new User();
        Role role = new Role();
        role.setName("TEACHER");
        target.setRole(role);
        Teacher teacher = new Teacher();
        teacher.setId(12L);
        when(userService.findById(7L)).thenReturn(target);
        when(teacherMapper.findByUserId(7L)).thenReturn(teacher);
        when(authService.isDefenseLeader(12L, LocalDate.now().getYear())).thenReturn(true);

        boolean result = queryController.isDefenseLeader(7L, session);

        assertTrue(result);
    }

    private MockHttpSession sessionWithUser(Long id, String roleName, Long departmentId) {
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setId(id);
        user.setDepartmentId(departmentId);
        Role role = new Role();
        role.setName(roleName);
        user.setRole(role);
        session.setAttribute("currentUser", user);
        return session;
    }
}
