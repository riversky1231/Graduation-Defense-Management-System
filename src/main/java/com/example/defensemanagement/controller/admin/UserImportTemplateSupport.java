package com.example.defensemanagement.controller.admin;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

/**
 * 用户导入模板支持类。
 */
@Component
public class UserImportTemplateSupport {

    public byte[] buildTemplateBytes() throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("用户导入模板");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            String[] headers = {"用户名", "真实姓名", "角色", "院系", "状态"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            createExampleRow(sheet, 1, "admin001", "张三", "SUPER_ADMIN", "CS", "1");
            createExampleRow(sheet, 2, "T001", "李四", "TEACHER", "计算机科学与技术学院", "启用");
            createExampleRow(sheet, 3, "dept_admin", "王五", "院系管理员", "CS", "1");
            createExampleRow(sheet, 4, "20210001", "赵六", "STUDENT", "计算机科学与技术学院", "启用");

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 3000) {
                    sheet.setColumnWidth(i, 3000);
                }
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void createExampleRow(
            Sheet sheet,
            int rowIndex,
            String username,
            String realName,
            String role,
            String department,
            String status) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(username);
        row.createCell(1).setCellValue(realName);
        row.createCell(2).setCellValue(role);
        row.createCell(3).setCellValue(department);
        row.createCell(4).setCellValue(status);
    }
}
