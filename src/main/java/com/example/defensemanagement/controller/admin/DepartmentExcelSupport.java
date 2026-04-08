package com.example.defensemanagement.controller.admin;

import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.mapper.DepartmentMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * 院系 Excel 模板与导入支持类。
 */
@Component
public class DepartmentExcelSupport {

    private static final Logger log = LoggerFactory.getLogger(DepartmentExcelSupport.class);
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    private final DepartmentMapper departmentMapper;

    public DepartmentExcelSupport(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    public byte[] buildTemplateBytes() throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("院系导入模板");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "院系名称", "院系代码", "描述"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            createExampleRow(sheet, 1, "1", "计算机科学与技术学院", "CS", "计算机相关专业");
            createExampleRow(sheet, 2, "2", "软件学院", "SE", "软件工程相关专业");
            createExampleRow(sheet, 3, "3", "信息与通信工程学院", "ICE", "通信工程相关专业");

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

    public String importDepartments(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "error:请选择Excel文件";
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null
                || (!fileName.toLowerCase().endsWith(".xlsx") && !fileName.toLowerCase().endsWith(".xls"))) {
            return "error:请上传Excel文件（.xlsx或.xls格式）";
        }

        try (InputStream inputStream = file.getInputStream();
                Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 1) {
                return "error:Excel文件为空";
            }

            ImportContext context = detectColumns(sheet);
            if (context.idCol == -1 && context.nameCol == -1 && context.codeCol == -1 && context.descriptionCol == -1) {
                return "error:Excel文件必须包含至少一列有效数据（ID、院系名称、院系代码、描述中的任意一个）";
            }

            int successCount = 0;
            int failCount = 0;
            StringBuilder errorMessages = new StringBuilder();

            for (int rowIndex = context.startRowIndex; rowIndex < sheet.getPhysicalNumberOfRows(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                try {
                    String idStr = readCell(row, context.idCol);
                    String name = readCell(row, context.nameCol);
                    String code = readCell(row, context.codeCol);
                    String description = readCell(row, context.descriptionCol);

                    if (idStr.isEmpty() && name.isEmpty() && code.isEmpty() && description.isEmpty()) {
                        failCount++;
                        errorMessages.append("第").append(rowIndex + 1)
                                .append("行：ID、院系名称、院系代码、描述不能同时为空；");
                        continue;
                    }

                    Long departmentId = null;
                    if (!idStr.isEmpty()) {
                        try {
                            departmentId = Long.parseLong(idStr);
                            Department existingDept = departmentMapper.findById(departmentId);
                            if (existingDept != null) {
                                failCount++;
                                errorMessages.append("第").append(rowIndex + 1).append("行：ID ")
                                        .append(departmentId).append("已存在；");
                                continue;
                            }
                        } catch (NumberFormatException e) {
                            failCount++;
                            errorMessages.append("第").append(rowIndex + 1).append("行：ID格式无效；");
                            continue;
                        }
                    }

                    if (!code.isEmpty()) {
                        Department existingDeptByCode = departmentMapper.findByCode(code);
                        if (existingDeptByCode != null) {
                            failCount++;
                            errorMessages.append("第").append(rowIndex + 1).append("行：院系代码 ")
                                    .append(code).append("已存在；");
                            continue;
                        }
                    }

                    Department department = new Department();
                    if (departmentId != null) {
                        department.setId(departmentId);
                    }
                    department.setName(name.isEmpty() ? null : name);
                    department.setCode(code.isEmpty() ? null : code);
                    department.setDescription(description.isEmpty() ? null : description);
                    departmentMapper.insert(department);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    errorMessages.append("第").append(rowIndex + 1).append("行：").append(e.getMessage()).append("；");
                }
            }

            return buildImportResult(successCount, failCount, errorMessages);
        } catch (Exception e) {
            log.warn("导入院系 Excel 失败: {}", e.getMessage(), e);
            return "error:导入失败：" + e.getMessage();
        }
    }

    private void createExampleRow(Sheet sheet, int rowIndex, String id, String name, String code, String description) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(id);
        row.createCell(1).setCellValue(name);
        row.createCell(2).setCellValue(code);
        row.createCell(3).setCellValue(description);
    }

    private ImportContext detectColumns(Sheet sheet) {
        ImportContext context = new ImportContext();
        Row firstRow = sheet.getRow(0);
        if (firstRow != null) {
            for (int i = 0; i < firstRow.getPhysicalNumberOfCells(); i++) {
                Cell cell = firstRow.getCell(i);
                if (cell == null) {
                    continue;
                }
                String cellValue = getCellValueAsString(cell).trim();
                if (cellValue.contains("ID") || cellValue.contains("id") || cellValue.equalsIgnoreCase("id")) {
                    context.idCol = i;
                    context.hasHeader = true;
                } else if (cellValue.contains("院系名称") || cellValue.contains("名称")
                        || cellValue.equalsIgnoreCase("name")) {
                    context.nameCol = i;
                    context.hasHeader = true;
                } else if (cellValue.contains("院系代码") || cellValue.contains("代码")
                        || cellValue.equalsIgnoreCase("code")) {
                    context.codeCol = i;
                    context.hasHeader = true;
                } else if (cellValue.contains("描述") || cellValue.equalsIgnoreCase("description")
                        || cellValue.equalsIgnoreCase("desc")) {
                    context.descriptionCol = i;
                    context.hasHeader = true;
                }
            }
        }

        if (context.hasHeader && (context.idCol != -1 || context.nameCol != -1
                || context.codeCol != -1 || context.descriptionCol != -1)) {
            context.startRowIndex = 1;
            return context;
        }

        context.idCol = 0;
        context.nameCol = 1;
        context.codeCol = 2;
        context.descriptionCol = 3;
        context.startRowIndex = 0;
        return context;
    }

    private String readCell(Row row, int columnIndex) {
        if (columnIndex == -1) {
            return "";
        }
        Cell cell = row.getCell(columnIndex);
        return cell == null ? "" : getCellValueAsString(cell).trim();
    }

    private String buildImportResult(int successCount, int failCount, StringBuilder errorMessages) {
        StringBuilder result = new StringBuilder("success:成功导入").append(successCount).append("条");
        if (failCount > 0) {
            result.append("，失败").append(failCount).append("条");
            if (errorMessages.length() > 0) {
                String errorMsg = errorMessages.toString();
                if (errorMsg.length() > MAX_ERROR_MESSAGE_LENGTH) {
                    errorMsg = errorMsg.substring(0, MAX_ERROR_MESSAGE_LENGTH) + "...";
                }
                result.append("。错误详情：").append(errorMsg);
            }
        }
        return result.toString();
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double numericValue = cell.getNumericCellValue();
                if (numericValue == (long) numericValue) {
                    return String.valueOf((long) numericValue);
                }
                return String.valueOf(numericValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private static final class ImportContext {
        private int idCol = -1;
        private int nameCol = -1;
        private int codeCol = -1;
        private int descriptionCol = -1;
        private int startRowIndex = 0;
        private boolean hasHeader = false;
    }
}
