package com.example.defensemanagement.controller.admin;

import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.service.AuthService;
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
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDepartmentControllersTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private DepartmentExcelSupport departmentExcelSupport;

    private AdminDepartmentPageController pageController;
    private AdminDepartmentCrudController crudController;
    private AdminDepartmentQueryController queryController;
    private AdminDepartmentImportController importController;

    @BeforeEach
    void setUp() {
        pageController = new AdminDepartmentPageController(userService, authService, departmentMapper);
        crudController = new AdminDepartmentCrudController(userService, authService, departmentMapper);
        queryController = new AdminDepartmentQueryController(userService, authService, departmentMapper);
        importController = new AdminDepartmentImportController(
                userService, authService, departmentMapper, departmentExcelSupport);
    }

    @Test
    void departmentManagementRedirectsWithoutPermission() {
        MockHttpSession session = sessionWithUser(1L);
        Model model = new ExtendedModelMap();
        when(authService.hasPermission(any(User.class), eq("CREATE_DEPARTMENT"))).thenReturn(false);

        String view = pageController.departmentManagement(model, session);

        assertEquals("redirect:/", view);
    }

    @Test
    void createDepartmentDelegatesWhenAuthorized() {
        MockHttpSession session = sessionWithUser(1L);
        when(authService.hasPermission(any(User.class), eq("CREATE_DEPARTMENT"))).thenReturn(true);

        String result = crudController.createDepartment("计算机学院", "CS", "desc", session);

        assertEquals("success", result);
        verify(userService).createDepartment("计算机学院", "CS", "desc");
    }

    @Test
    void deleteDepartmentRejectsWhenUsersExist() {
        MockHttpSession session = sessionWithUser(1L);
        when(authService.hasPermission(any(User.class), eq("CREATE_DEPARTMENT"))).thenReturn(true);
        when(userService.getAllUsers(2L)).thenReturn(List.of(new User()));

        String result = crudController.deleteDepartment(2L, session);

        assertEquals("error:该院系下还有用户，无法删除", result);
    }

    @Test
    void searchDepartmentsReturnsPaginationInfo() {
        MockHttpSession session = sessionWithUser(1L);
        when(departmentMapper.searchDepartments("计", 0, 2)).thenReturn(List.of(new Department(), new Department()));
        when(departmentMapper.countDepartments("计")).thenReturn(5);

        Map<String, Object> result = queryController.searchDepartments("计", 1, 2, session);

        assertEquals(5, result.get("total"));
        assertEquals(3, result.get("totalPages"));
        assertEquals(2, ((List<?>) result.get("departments")).size());
    }

    @Test
    void getDepartmentListThrowsWhenUnauthenticated() {
        MockHttpSession session = new MockHttpSession();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> queryController.getDepartmentList(session));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void downloadTemplateReturnsAttachmentWhenAuthorized() throws Exception {
        MockHttpSession session = sessionWithUser(1L);
        when(authService.hasPermission(any(User.class), eq("CREATE_DEPARTMENT"))).thenReturn(true);
        when(departmentExcelSupport.buildTemplateBytes()).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> response = importController.downloadDepartmentTemplate(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("院系导入模板.xlsx"));
        assertEquals(3, response.getBody().length);
    }

    @Test
    void importDepartmentsRejectsWithoutPermission() {
        MockHttpSession session = sessionWithUser(1L);
        when(authService.hasPermission(any(User.class), eq("CREATE_DEPARTMENT"))).thenReturn(false);

        String result = importController.importDepartmentsFromExcel(org.mockito.Mockito.mock(MultipartFile.class), session);

        assertEquals("error:权限不足", result);
    }

    private MockHttpSession sessionWithUser(Long userId) {
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setId(userId);
        Role role = new Role();
        role.setName("SUPER_ADMIN");
        user.setRole(role);
        session.setAttribute("currentUser", user);
        return session;
    }
}
