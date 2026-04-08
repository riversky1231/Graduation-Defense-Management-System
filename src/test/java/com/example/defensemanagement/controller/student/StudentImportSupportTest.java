package com.example.defensemanagement.controller.student;

import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.service.StudentService;
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

import java.io.ByteArrayOutputStream;
import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentImportSupportTest {

    @Mock
    private StudentService studentService;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private DepartmentMapper departmentMapper;

    @Test
    void buildTemplateBytesCreatesExpectedSheet() throws Exception {
        StudentImportTemplateSupport support = new StudentImportTemplateSupport();

        byte[] bytes = support.buildTemplateBytes();

        try (Workbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(bytes))) {
            assertEquals("学生导入模板", workbook.getSheetAt(0).getSheetName());
            assertEquals("学号", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("所属院系", workbook.getSheetAt(0).getRow(0).getCell(8).getStringCellValue());
        }
    }

    @Test
    void importStudentsSavesParsedStudent() throws Exception {
        StudentExcelImportSupport support = new StudentExcelImportSupport(studentService, studentMapper, departmentMapper);
        Department department = new Department();
        department.setId(5L);
        department.setCode("CS");
        department.setName("计算机科学与技术学院");
        when(departmentMapper.findAll()).thenReturn(List.of(department));
        when(departmentMapper.findByCode("CS")).thenReturn(department);
        when(studentMapper.findByStudentNoAndYear("20210001", 2026)).thenReturn(null);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                createWorkbookBytes("20210001", "张三", "计科2101", "13800000001",
                        "student01@example.com", "2026-06-18", "论文", "题目A", "CS"));

        String result = support.importStudents(file, 9L, 2026);

        assertEquals("success:成功导入1条", result);
        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentService).saveStudent(captor.capture());
        Student saved = captor.getValue();
        assertEquals("20210001", saved.getStudentNo());
        assertEquals("张三", saved.getName());
        assertEquals("计科2101", saved.getClassInfo());
        assertEquals("13800000001", saved.getPhone());
        assertEquals("student01@example.com", saved.getEmail());
        assertEquals("PAPER", saved.getDefenseType());
        assertEquals("题目A", saved.getTitle());
        assertEquals(2026, saved.getDefenseYear());
        assertEquals(5L, saved.getDepartmentId());
        assertEquals(Date.valueOf("2026-06-18"), saved.getDefenseDate());
    }

    @Test
    void importStudentsRejectsUnknownDepartment() throws Exception {
        StudentExcelImportSupport support = new StudentExcelImportSupport(studentService, studentMapper, departmentMapper);
        when(departmentMapper.findAll()).thenReturn(List.of());
        when(departmentMapper.findByCode("UNKNOWN")).thenReturn(null);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                createWorkbookBytes("20210002", "李四", null, null, null, null, "设计", "题目B", "UNKNOWN"));

        String result = support.importStudents(file, 9L, 2026);

        assertTrue(result.contains("失败1条"));
        assertTrue(result.contains("院系 UNKNOWN不存在"));
        verify(studentService, never()).saveStudent(any());
    }

    private byte[] createWorkbookBytes(
            String studentNo,
            String name,
            String classInfo,
            String phone,
            String email,
            String defenseDate,
            String defenseType,
            String title,
            String department) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("students");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("学号");
            header.createCell(1).setCellValue("姓名");
            header.createCell(2).setCellValue("班级");
            header.createCell(3).setCellValue("联系电话");
            header.createCell(4).setCellValue("邮箱");
            header.createCell(5).setCellValue("答辩日期");
            header.createCell(6).setCellValue("类型");
            header.createCell(7).setCellValue("题目");
            header.createCell(8).setCellValue("所属院系");

            Row row = sheet.createRow(1);
            if (studentNo != null) {
                row.createCell(0).setCellValue(studentNo);
            }
            if (name != null) {
                row.createCell(1).setCellValue(name);
            }
            if (classInfo != null) {
                row.createCell(2).setCellValue(classInfo);
            }
            if (phone != null) {
                row.createCell(3).setCellValue(phone);
            }
            if (email != null) {
                row.createCell(4).setCellValue(email);
            }
            if (defenseDate != null) {
                row.createCell(5).setCellValue(defenseDate);
            }
            if (defenseType != null) {
                row.createCell(6).setCellValue(defenseType);
            }
            if (title != null) {
                row.createCell(7).setCellValue(title);
            }
            if (department != null) {
                row.createCell(8).setCellValue(department);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
