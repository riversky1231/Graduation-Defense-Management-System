package com.example.defensemanagement.controller.student;

import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.service.ConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentImportControllersTest {

    @Mock
    private StudentExcelImportSupport studentExcelImportSupport;
    @Mock
    private StudentImportTemplateSupport studentImportTemplateSupport;
    @Mock
    private ConfigService configService;

    @Test
    void importStudentsFromExcelRejectsNonAdmin() {
        StudentImportExcelController controller = new StudentImportExcelController(studentExcelImportSupport, configService);

        String result = controller.importStudentsFromExcel(new MockMultipartFile("file", new byte[] {1}), new MockHttpSession());

        assertEquals("error:权限不足", result);
    }

    @Test
    void importStudentsFromExcelDelegatesWithDepartmentAndYear() {
        StudentImportExcelController controller = new StudentImportExcelController(studentExcelImportSupport, configService);
        MockHttpSession session = adminSession();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[] {1, 2, 3});
        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(studentExcelImportSupport.importStudents(file, 9L, 2026)).thenReturn("success:成功导入1条");

        String result = controller.importStudentsFromExcel(file, session);

        assertEquals("success:成功导入1条", result);
        verify(studentExcelImportSupport).importStudents(file, 9L, 2026);
    }

    @Test
    void downloadStudentTemplateRejectsNonAdmin() {
        StudentImportTemplateController controller = new StudentImportTemplateController(studentImportTemplateSupport);

        ResponseEntity<byte[]> response = controller.downloadStudentTemplate(new MockHttpSession());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void downloadStudentTemplateReturnsAttachment() throws Exception {
        StudentImportTemplateController controller = new StudentImportTemplateController(studentImportTemplateSupport);
        when(studentImportTemplateSupport.buildTemplateBytes()).thenReturn(new byte[] {1, 2, 3});

        ResponseEntity<byte[]> response = controller.downloadStudentTemplate(adminSession());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().length);
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("学生导入模板.xlsx"));
    }

    private MockHttpSession adminSession() {
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setDepartmentId(9L);
        Role role = new Role();
        role.setName("DEPT_ADMIN");
        user.setRole(role);
        session.setAttribute("currentUser", user);
        return session;
    }
}
