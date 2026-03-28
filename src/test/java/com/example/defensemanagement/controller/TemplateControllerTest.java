package com.example.defensemanagement.controller;

import com.example.defensemanagement.common.ApiResponse;
import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateControllerTest {

    @Mock
    private FileStorageService fileStorageService;

    private TemplateController controller;

    @BeforeEach
    void setUp() {
        controller = new TemplateController();
        ReflectionTestUtils.setField(controller, "fileStorageService", fileStorageService);
    }

    @Test
    void superAdminUploadsTemplateToGlobalDirectory() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", user("SUPER_ADMIN", null));
        MockMultipartFile file = file();
        when(fileStorageService.save(file, "templates", "paper-grade")).thenReturn("templates/paper-grade.docx");

        ApiResponse<Map<String, String>> response = controller.uploadTemplate("paper-grade", file, session);

        assertTrue(response.isSuccess());
        assertEquals("templates/paper-grade.docx", response.getData().get("path"));
        verify(fileStorageService).save(file, "templates", "paper-grade");
    }

    @Test
    void deptAdminUploadsTemplateToDepartmentDirectory() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", user("DEPT_ADMIN", 7L));
        MockMultipartFile file = file();
        when(fileStorageService.save(file, "templates/dept_7", "design-grade"))
                .thenReturn("templates/dept_7/design-grade.docx");

        ApiResponse<Map<String, String>> response = controller.uploadTemplate("design-grade", file, session);

        assertTrue(response.isSuccess());
        assertEquals("templates/dept_7/design-grade.docx", response.getData().get("path"));
        verify(fileStorageService).save(file, "templates/dept_7", "design-grade");
    }

    @Test
    void nonAdminCannotUploadTemplate() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", user("TEACHER", null));

        ApiResponse<Map<String, String>> response = controller.uploadTemplate("paper-grade", file(), session);

        assertFalse(response.isSuccess());
        assertEquals("权限不足", response.getMessage());
    }

    private User user(String roleName, Long departmentId) {
        User user = new User();
        user.setDepartmentId(departmentId);
        Role role = new Role();
        role.setName(roleName);
        user.setRole(role);
        return user;
    }

    private MockMultipartFile file() {
        return new MockMultipartFile(
                "file",
                "template.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "doc".getBytes());
    }
}
