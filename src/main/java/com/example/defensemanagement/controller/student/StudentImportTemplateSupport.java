package com.example.defensemanagement.controller.student;

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
 * 学生导入模板生成支持类。
 */
@Component
public class StudentImportTemplateSupport {

    public byte[] buildTemplateBytes() throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("学生导入模板");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            String[] headers = {"学号", "姓名", "班级", "联系电话", "邮箱", "答辩日期", "类型", "题目", "所属院系"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            createExampleRow(sheet, 1,
                    "20210001", "张三", "计科2101", "13800000001", "student01@example.com",
                    "2024-06-18", "论文", "基于深度学习的图像识别研究", "计算机科学与技术学院");
            createExampleRow(sheet, 2,
                    "20210002", "李四", "软工2102", "13800000002", "student02@example.com",
                    "2024-06-19", "设计", "智能家居控制系统设计", "CS");

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
            String studentNo,
            String name,
            String classInfo,
            String phone,
            String email,
            String defenseDate,
            String defenseType,
            String title,
            String department) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(studentNo);
        row.createCell(1).setCellValue(name);
        row.createCell(2).setCellValue(classInfo);
        row.createCell(3).setCellValue(phone);
        row.createCell(4).setCellValue(email);
        row.createCell(5).setCellValue(defenseDate);
        row.createCell(6).setCellValue(defenseType);
        row.createCell(7).setCellValue(title);
        row.createCell(8).setCellValue(department);
    }
}
