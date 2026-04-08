package com.example.defensemanagement.controller.student;

import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.service.StudentService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.sql.Date;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 学生 Excel 导入支持类。
 */
@Component
public class StudentExcelImportSupport {

    private static final Logger log = LoggerFactory.getLogger(StudentExcelImportSupport.class);
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    private final StudentService studentService;
    private final StudentMapper studentMapper;
    private final DepartmentMapper departmentMapper;

    public StudentExcelImportSupport(
            StudentService studentService,
            StudentMapper studentMapper,
            DepartmentMapper departmentMapper) {
        this.studentService = studentService;
        this.studentMapper = studentMapper;
        this.departmentMapper = departmentMapper;
    }

    public String importStudents(MultipartFile file, Long defaultDepartmentId, Integer currentYear) {
        if (file == null || file.isEmpty()) {
            return "error:请选择Excel文件";
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.toLowerCase().endsWith(".xlsx") && !fileName.toLowerCase().endsWith(".xls"))) {
            return "error:文件格式不正确，请上传.xlsx或.xls格式的Excel文件";
        }

        int defenseYear = currentYear != null ? currentYear : Year.now().getValue();
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 1) {
                return "error:Excel文件不能为空";
            }

            Row firstRow = sheet.getRow(0);
            if (firstRow == null) {
                return "error:Excel文件第一行不能为空";
            }

            ImportContext context = detectColumns(firstRow);
            if (!context.hasAnyDataColumn()) {
                return "error:Excel文件必须包含以下列之一：学号、姓名、类型、题目、所属院系（或使用表头标识，或按顺序：第1列学号，第2列姓名，第3列类型，第4列题目，第5列所属院系）";
            }

            List<Department> allDepartments = departmentMapper.findAll();
            int successCount = 0;
            int failCount = 0;
            StringBuilder errorMessages = new StringBuilder();

            for (int rowIndex = context.startRowIndex; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                try {
                    StudentImportRow importRow = readStudentRow(row, context, rowIndex);
                    String validationError = validateRequiredFields(importRow, rowIndex);
                    if (validationError != null) {
                        failCount++;
                        errorMessages.append(validationError);
                        continue;
                    }

                    populateFallbacks(importRow, rowIndex);
                    Long finalDepartmentId = resolveDepartmentId(importRow.departmentText, defaultDepartmentId, allDepartments);
                    if (importRow.departmentText != null && finalDepartmentId == null) {
                        failCount++;
                        errorMessages.append("第").append(rowIndex + 1).append("行：院系 ")
                                .append(importRow.departmentText).append("不存在；");
                        continue;
                    }
                    if (studentMapper.findByStudentNoAndYear(importRow.studentNo, defenseYear) != null) {
                        failCount++;
                        errorMessages.append("第").append(rowIndex + 1).append("行：学号").append(importRow.studentNo)
                                .append("在").append(defenseYear).append("年已存在；");
                        continue;
                    }

                    Student student = new Student();
                    student.setStudentNo(importRow.studentNo);
                    student.setName(importRow.name);
                    student.setClassInfo(importRow.classInfo);
                    student.setPhone(importRow.phone);
                    student.setEmail(importRow.email);
                    student.setDefenseDate(importRow.defenseDate);
                    student.setDefenseType(importRow.defenseType);
                    student.setTitle(importRow.title);
                    student.setDefenseYear(defenseYear);
                    student.setDepartmentId(finalDepartmentId);
                    studentService.saveStudent(student);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    errorMessages.append("第").append(rowIndex + 1).append("行：").append(e.getMessage()).append("；");
                }
            }
            return buildImportResult(successCount, failCount, errorMessages);
        } catch (Exception e) {
            log.warn("导入学生 Excel 失败: {}", e.getMessage(), e);
            return "error:导入失败：" + e.getMessage();
        }
    }

    private ImportContext detectColumns(Row firstRow) {
        ImportContext context = new ImportContext();
        for (int columnIndex = 0; columnIndex < firstRow.getPhysicalNumberOfCells(); columnIndex++) {
            Cell cell = firstRow.getCell(columnIndex);
            if (cell == null) {
                continue;
            }
            String cellValue = getCellValueAsString(cell).trim();
            if (cellValue.contains("学号") || cellValue.equalsIgnoreCase("studentNo")
                    || cellValue.equalsIgnoreCase("student_no")) {
                context.studentNoCol = columnIndex;
                context.hasHeader = true;
            } else if (cellValue.contains("姓名") || cellValue.equalsIgnoreCase("name")) {
                context.nameCol = columnIndex;
                context.hasHeader = true;
            } else if (cellValue.contains("班级") || cellValue.equalsIgnoreCase("class")
                    || cellValue.equalsIgnoreCase("classInfo") || cellValue.equalsIgnoreCase("class_info")) {
                context.classInfoCol = columnIndex;
                context.hasHeader = true;
            } else if (cellValue.contains("联系电话") || cellValue.contains("电话")
                    || cellValue.equalsIgnoreCase("phone") || cellValue.equalsIgnoreCase("mobile")) {
                context.phoneCol = columnIndex;
                context.hasHeader = true;
            } else if (cellValue.contains("邮箱") || cellValue.equalsIgnoreCase("email")) {
                context.emailCol = columnIndex;
                context.hasHeader = true;
            } else if (cellValue.contains("答辩日期") || cellValue.contains("日期")
                    || cellValue.equalsIgnoreCase("defenseDate") || cellValue.equalsIgnoreCase("defense_date")) {
                context.defenseDateCol = columnIndex;
                context.hasHeader = true;
            } else if (cellValue.contains("类型") || cellValue.equalsIgnoreCase("type")
                    || cellValue.equalsIgnoreCase("defenseType")) {
                context.typeCol = columnIndex;
                context.hasHeader = true;
            } else if (cellValue.contains("题目") || cellValue.equalsIgnoreCase("title")) {
                context.titleCol = columnIndex;
                context.hasHeader = true;
            } else if (cellValue.contains("所属院系") || cellValue.contains("院系")
                    || cellValue.equalsIgnoreCase("department") || cellValue.equalsIgnoreCase("dept")) {
                context.departmentCol = columnIndex;
                context.hasHeader = true;
            }
        }

        if (context.hasHeader && context.hasAnyDataColumn()) {
            context.startRowIndex = 1;
            return context;
        }

        context.studentNoCol = 0;
        context.nameCol = 1;
        context.typeCol = 2;
        context.titleCol = 3;
        context.departmentCol = 4;
        context.startRowIndex = 0;
        return context;
    }

    private StudentImportRow readStudentRow(Row row, ImportContext context, int rowIndex) {
        StudentImportRow importRow = new StudentImportRow();
        importRow.rowIndex = rowIndex;
        importRow.studentNo = readTrimmedCell(row, context.studentNoCol);
        importRow.name = readTrimmedCell(row, context.nameCol);
        importRow.classInfo = readTrimmedCell(row, context.classInfoCol);
        importRow.phone = readTrimmedCell(row, context.phoneCol);
        importRow.email = readTrimmedCell(row, context.emailCol);
        importRow.defenseDate = parseSqlDateFromCell(row.getCell(context.defenseDateCol));
        importRow.defenseType = normalizeDefenseType(readTrimmedCell(row, context.typeCol));
        importRow.title = readTrimmedCell(row, context.titleCol);
        importRow.departmentText = readTrimmedCell(row, context.departmentCol);
        return importRow;
    }

    private String validateRequiredFields(StudentImportRow importRow, int rowIndex) {
        if (isBlank(importRow.studentNo)
                && isBlank(importRow.name)
                && isBlank(importRow.defenseType)
                && isBlank(importRow.title)
                && isBlank(importRow.departmentText)) {
            return "第" + (rowIndex + 1) + "行：学号、姓名、类型、题目、所属院系至少需要填写一个；";
        }
        if (isBlank(importRow.studentNo) && isBlank(importRow.name)) {
            return "第" + (rowIndex + 1) + "行：学号和姓名不能同时为空；";
        }
        return null;
    }

    private void populateFallbacks(StudentImportRow importRow, int rowIndex) {
        if (isBlank(importRow.studentNo)) {
            String baseName = !isBlank(importRow.name) ? importRow.name : "STU";
            importRow.studentNo = baseName + "_" + System.currentTimeMillis() + "_" + rowIndex;
        }
        if (isBlank(importRow.name)) {
            importRow.name = "学生_" + importRow.studentNo;
        }
    }

    private Long resolveDepartmentId(String departmentText, Long defaultDepartmentId, List<Department> allDepartments) {
        if (isBlank(departmentText)) {
            return defaultDepartmentId;
        }

        Department department = departmentMapper.findByCode(departmentText);
        if (department == null && allDepartments != null) {
            for (Department item : allDepartments) {
                if (item.getName() != null && item.getName().equals(departmentText)) {
                    department = item;
                    break;
                }
            }
        }
        return department != null ? department.getId() : null;
    }

    private String buildImportResult(int successCount, int failCount, StringBuilder errorMessages) {
        StringBuilder result = new StringBuilder("success:成功导入").append(successCount).append("条");
        if (failCount > 0) {
            result.append("，失败").append(failCount).append("条");
            if (errorMessages.length() > 0) {
                String errorMessage = errorMessages.toString();
                if (errorMessage.length() > MAX_ERROR_MESSAGE_LENGTH) {
                    errorMessage = errorMessage.substring(0, MAX_ERROR_MESSAGE_LENGTH) + "...";
                }
                result.append("。错误详情：").append(errorMessage);
            }
        }
        return result.toString();
    }

    private String readTrimmedCell(Row row, int columnIndex) {
        if (row == null || columnIndex == -1) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        String value = getCellValueAsString(cell).trim();
        return value.isEmpty() ? null : value;
    }

    private String normalizeDefenseType(String defenseType) {
        if (isBlank(defenseType)) {
            return null;
        }
        if ("论文".equals(defenseType)) {
            return "PAPER";
        }
        if ("设计".equals(defenseType)) {
            return "DESIGN";
        }
        String normalized = defenseType.toUpperCase();
        return "PAPER".equals(normalized) || "DESIGN".equals(normalized) ? normalized : null;
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

    private Date parseSqlDateFromCell(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            java.util.Date date = cell.getDateCellValue();
            return date != null ? new Date(date.getTime()) : null;
        }
        String text = getCellValueAsString(cell).trim();
        if (text.isEmpty()) {
            return null;
        }
        DateTimeFormatter[] formatters = new DateTimeFormatter[] {
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("yyyy.MM.dd")
        };
        for (DateTimeFormatter formatter : formatters) {
            try {
                LocalDate localDate = LocalDate.parse(text, formatter);
                return Date.valueOf(localDate);
            } catch (DateTimeParseException ignored) {
                // try next formatter
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class ImportContext {
        private int studentNoCol = -1;
        private int nameCol = -1;
        private int classInfoCol = -1;
        private int phoneCol = -1;
        private int emailCol = -1;
        private int defenseDateCol = -1;
        private int typeCol = -1;
        private int titleCol = -1;
        private int departmentCol = -1;
        private int startRowIndex = 0;
        private boolean hasHeader = false;

        private boolean hasAnyDataColumn() {
            return studentNoCol != -1 || nameCol != -1 || typeCol != -1 || titleCol != -1
                    || departmentCol != -1 || classInfoCol != -1 || phoneCol != -1
                    || emailCol != -1 || defenseDateCol != -1;
        }
    }

    private static final class StudentImportRow {
        private int rowIndex;
        private String studentNo;
        private String name;
        private String classInfo;
        private String phone;
        private String email;
        private Date defenseDate;
        private String defenseType;
        private String title;
        private String departmentText;
    }
}
