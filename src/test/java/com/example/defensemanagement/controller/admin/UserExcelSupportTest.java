package com.example.defensemanagement.controller.admin;

import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.mapper.RoleMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.UserService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserExcelSupportTest {

    @Mock
    private UserService userService;
    @Mock
    private TeacherMapper teacherMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private DepartmentMapper departmentMapper;

    @Test
    void buildTemplateBytesCreatesExpectedSheet() throws Exception {
        UserImportTemplateSupport support = new UserImportTemplateSupport();

        byte[] bytes = support.buildTemplateBytes();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals("用户导入模板", workbook.getSheetAt(0).getSheetName());
            assertEquals("用户名", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("状态", workbook.getSheetAt(0).getRow(0).getCell(4).getStringCellValue());
        }
    }

    @Test
    void importUsersCreatesTeacherAndUpdatesTeacherProfile() throws Exception {
        UserExcelImportSupport support = new UserExcelImportSupport(userService, teacherMapper, roleMapper, departmentMapper);
        Role teacherRole = new Role();
        teacherRole.setId(3L);
        teacherRole.setName("TEACHER");
        Department department = new Department();
        department.setId(5L);
        department.setCode("CS");
        Teacher teacher = new Teacher();

        when(userService.findByUsername("T001")).thenReturn(null);
        when(roleMapper.findByName("TEACHER")).thenReturn(teacherRole);
        when(departmentMapper.findByCode("CS")).thenReturn(department);
        when(teacherMapper.findByTeacherNo("T001")).thenReturn(null);
        doAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(88L);
            return null;
        }).when(userService).saveUser(any(User.class));
        when(teacherMapper.findByUserId(88L)).thenReturn(teacher);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                createWorkbookBytes("T001", "李四", "TEACHER", "CS", "禁用"));

        String result = support.importUsers(file);

        assertEquals("success:成功导入1条", result);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).saveUser(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("T001", saved.getUsername());
        assertEquals("T001", saved.getPassword());
        assertEquals("李四", saved.getRealName());
        assertEquals(3L, saved.getRoleId());
        assertEquals(5L, saved.getDepartmentId());
        assertEquals(0, saved.getStatus());
        verify(teacherMapper).update(teacher);
        assertEquals("李四", teacher.getName());
        assertEquals(5L, teacher.getDepartmentId());
    }

    @Test
    void importUsersRejectsUnknownDepartment() throws Exception {
        UserExcelImportSupport support = new UserExcelImportSupport(userService, teacherMapper, roleMapper, departmentMapper);
        Role studentRole = new Role();
        studentRole.setId(4L);
        studentRole.setName("STUDENT");

        when(userService.findByUsername("20210001")).thenReturn(null);
        when(roleMapper.findByName("STUDENT")).thenReturn(studentRole);
        when(departmentMapper.findByCode("UNKNOWN")).thenReturn(null);
        when(userService.getAllDepartments()).thenReturn(List.of());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                createWorkbookBytes("20210001", "张三", "STUDENT", "UNKNOWN", "启用"));

        String result = support.importUsers(file);

        assertTrue(result.contains("失败1条"));
        assertTrue(result.contains("院系 UNKNOWN 不存在"));
        verify(userService, never()).saveUser(any(User.class));
    }

    private byte[] createWorkbookBytes(
            String username,
            String realName,
            String role,
            String department,
            String status) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("users");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("用户名");
            header.createCell(1).setCellValue("真实姓名");
            header.createCell(2).setCellValue("角色");
            header.createCell(3).setCellValue("院系");
            header.createCell(4).setCellValue("状态");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(username);
            row.createCell(1).setCellValue(realName);
            row.createCell(2).setCellValue(role);
            row.createCell(3).setCellValue(department);
            row.createCell(4).setCellValue(status);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
