package com.example.defensemanagement.controller.admin;

import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.mapper.DepartmentMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentExcelSupportTest {

    @Mock
    private DepartmentMapper departmentMapper;

    private DepartmentExcelSupport departmentExcelSupport;

    @BeforeEach
    void setUp() {
        departmentExcelSupport = new DepartmentExcelSupport(departmentMapper);
    }

    @Test
    void buildTemplateBytesContainsExpectedHeader() throws Exception {
        byte[] bytes = departmentExcelSupport.buildTemplateBytes();

        try (Workbook workbook = WorkbookFactory.create(new java.io.ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertEquals("院系导入模板", sheet.getSheetName());
            assertEquals("ID", header.getCell(0).getStringCellValue());
            assertEquals("院系名称", header.getCell(1).getStringCellValue());
            assertEquals("院系代码", header.getCell(2).getStringCellValue());
            assertEquals("描述", header.getCell(3).getStringCellValue());
        }
    }

    @Test
    void importDepartmentsReadsWorkbookAndInsertsDepartment() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "departments.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                createWorkbookBytes());

        when(departmentMapper.findByCode(eq("CS"))).thenReturn(null);
        when(departmentMapper.findById(anyLong())).thenReturn(null);

        String result = departmentExcelSupport.importDepartments(file);

        assertTrue(result.startsWith("success:成功导入1条"));
        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentMapper).insert(captor.capture());
        Department department = captor.getValue();
        assertEquals(1L, department.getId());
        assertEquals("计算机学院", department.getName());
        assertEquals("CS", department.getCode());
        assertEquals("说明", department.getDescription());
    }

    @Test
    void importDepartmentsRejectsNonExcelFile() {
        MockMultipartFile file = new MockMultipartFile("file", "departments.txt", "text/plain", "bad".getBytes());

        String result = departmentExcelSupport.importDepartments(file);

        assertEquals("error:请上传Excel文件（.xlsx或.xls格式）", result);
    }

    private byte[] createWorkbookBytes() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("导入");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("院系名称");
            header.createCell(2).setCellValue("院系代码");
            header.createCell(3).setCellValue("描述");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(1);
            row.createCell(1).setCellValue("计算机学院");
            row.createCell(2).setCellValue("CS");
            row.createCell(3).setCellValue("说明");

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
