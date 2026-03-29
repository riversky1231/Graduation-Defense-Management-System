package com.example.defensemanagement.controller.student;

import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.StudentService;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 学生导入与模板下载端点，从 StudentController 中拆分。
 */
@Controller
@RequestMapping("/department/student")
public class StudentImportController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @PostMapping("/import/excel")
    @ResponseBody
    public String importStudentsFromExcel(@RequestParam("file") MultipartFile file, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        if (file == null || file.isEmpty()) {
            return "error:请选择Excel文件";
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
            return "error:文件格式不正确，请上传.xlsx或.xls格式的Excel文件";
        }

        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMessages = new StringBuilder();

        try {
            InputStream inputStream = file.getInputStream();
            Workbook workbook;

            if (fileName.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(inputStream);
            } else {
                workbook = new HSSFWorkbook(inputStream);
            }

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 1) {
                workbook.close();
                return "error:Excel文件不能为空";
            }

            User currentUser = (User) session.getAttribute("currentUser");
            Long departmentId = null;
            if (currentUser != null && currentUser.getDepartmentId() != null) {
                departmentId = currentUser.getDepartmentId();
            }

            Integer currentYear = configService.getCurrentDefenseYear();
            if (currentYear == null) {
                currentYear = java.time.Year.now().getValue();
            }

            Row firstRow = sheet.getRow(0);
            if (firstRow == null) {
                workbook.close();
                return "error:Excel文件第一行不能为空";
            }

            int studentNoCol = -1;
            int nameCol = -1;
            int classInfoCol = -1;
            int phoneCol = -1;
            int emailCol = -1;
            int defenseDateCol = -1;
            int typeCol = -1;
            int titleCol = -1;
            int departmentCol = -1;
            int startRowIndex = 0;

            boolean hasHeader = false;
            for (int i = 0; i < firstRow.getPhysicalNumberOfCells(); i++) {
                Cell cell = firstRow.getCell(i);
                if (cell != null) {
                    String cellValue = getCellValueAsString(cell).trim();
                    if (cellValue.contains("学号") || cellValue.equalsIgnoreCase("studentNo")
                            || cellValue.equalsIgnoreCase("student_no") || cellValue.equalsIgnoreCase("学号")) {
                        studentNoCol = i;
                        hasHeader = true;
                    } else if (cellValue.contains("姓名") || cellValue.equalsIgnoreCase("name")
                            || cellValue.equalsIgnoreCase("姓名")) {
                        nameCol = i;
                        hasHeader = true;
                    } else if (cellValue.contains("班级") || cellValue.equalsIgnoreCase("class")
                            || cellValue.equalsIgnoreCase("classInfo") || cellValue.equalsIgnoreCase("class_info")) {
                        classInfoCol = i;
                        hasHeader = true;
                    } else if (cellValue.contains("联系电话") || cellValue.contains("电话")
                            || cellValue.equalsIgnoreCase("phone") || cellValue.equalsIgnoreCase("mobile")) {
                        phoneCol = i;
                        hasHeader = true;
                    } else if (cellValue.contains("邮箱") || cellValue.equalsIgnoreCase("email")) {
                        emailCol = i;
                        hasHeader = true;
                    } else if (cellValue.contains("答辩日期") || cellValue.contains("日期")
                            || cellValue.equalsIgnoreCase("defenseDate")
                            || cellValue.equalsIgnoreCase("defense_date")) {
                        defenseDateCol = i;
                        hasHeader = true;
                    } else if (cellValue.contains("类型") || cellValue.equalsIgnoreCase("type")
                            || cellValue.equalsIgnoreCase("defenseType")) {
                        typeCol = i;
                        hasHeader = true;
                    } else if (cellValue.contains("题目") || cellValue.equalsIgnoreCase("title")) {
                        titleCol = i;
                        hasHeader = true;
                    } else if (cellValue.contains("所属院系") || cellValue.contains("院系")
                            || cellValue.equalsIgnoreCase("department") || cellValue.equalsIgnoreCase("dept")) {
                        departmentCol = i;
                        hasHeader = true;
                    }
                }
            }

            if (hasHeader && (studentNoCol != -1 || nameCol != -1 || typeCol != -1 || titleCol != -1
                    || departmentCol != -1)) {
                startRowIndex = 1;
            } else {
                studentNoCol = 0;
                nameCol = 1;
                typeCol = 2;
                titleCol = 3;
                departmentCol = 4;
                startRowIndex = 0;
            }

            if (studentNoCol == -1 && nameCol == -1 && typeCol == -1 && titleCol == -1 && departmentCol == -1
                    && classInfoCol == -1 && phoneCol == -1 && emailCol == -1 && defenseDateCol == -1) {
                workbook.close();
                return "error:Excel文件必须包含以下列之一：学号、姓名、类型、题目、所属院系（或使用表头标识，或按顺序：第1列学号，第2列姓名，第3列类型，第4列题目，第5列所属院系）";
            }

            List<Department> allDepartments = departmentMapper.findAll();

            for (int rowIndex = startRowIndex; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                try {
                    String studentNo = null;
                    String name = null;
                    String classInfo = null;
                    String phone = null;
                    String email = null;
                    java.sql.Date defenseDate = null;
                    String defenseType = null;
                    String title = null;
                    String departmentStr = null;

                    if (studentNoCol != -1) {
                        Cell cell = row.getCell(studentNoCol);
                        if (cell != null) {
                            studentNo = getCellValueAsString(cell).trim();
                            if (studentNo != null && studentNo.isEmpty()) {
                                studentNo = null;
                            }
                        }
                    }

                    if (nameCol != -1) {
                        Cell cell = row.getCell(nameCol);
                        if (cell != null) {
                            name = getCellValueAsString(cell).trim();
                            if (name != null && name.isEmpty()) {
                                name = null;
                            }
                        }
                    }

                    if (classInfoCol != -1) {
                        Cell cell = row.getCell(classInfoCol);
                        if (cell != null) {
                            classInfo = getCellValueAsString(cell).trim();
                            if (classInfo != null && classInfo.isEmpty()) {
                                classInfo = null;
                            }
                        }
                    }

                    if (phoneCol != -1) {
                        Cell cell = row.getCell(phoneCol);
                        if (cell != null) {
                            phone = getCellValueAsString(cell).trim();
                            if (phone != null && phone.isEmpty()) {
                                phone = null;
                            }
                        }
                    }

                    if (emailCol != -1) {
                        Cell cell = row.getCell(emailCol);
                        if (cell != null) {
                            email = getCellValueAsString(cell).trim();
                            if (email != null && email.isEmpty()) {
                                email = null;
                            }
                        }
                    }

                    if (defenseDateCol != -1) {
                        Cell cell = row.getCell(defenseDateCol);
                        defenseDate = parseSqlDateFromCell(cell);
                    }

                    if (typeCol != -1) {
                        Cell cell = row.getCell(typeCol);
                        if (cell != null) {
                            defenseType = getCellValueAsString(cell).trim();
                            if (defenseType != null && !defenseType.isEmpty()) {
                                if ("论文".equals(defenseType)) {
                                    defenseType = "PAPER";
                                } else if ("设计".equals(defenseType)) {
                                    defenseType = "DESIGN";
                                } else {
                                    defenseType = defenseType.toUpperCase();
                                }
                                if (!"PAPER".equals(defenseType) && !"DESIGN".equals(defenseType)) {
                                    defenseType = null;
                                }
                            } else {
                                defenseType = null;
                            }
                        }
                    }

                    if (titleCol != -1) {
                        Cell cell = row.getCell(titleCol);
                        if (cell != null) {
                            title = getCellValueAsString(cell).trim();
                            if (title != null && title.isEmpty()) {
                                title = null;
                            }
                        }
                    }

                    if (departmentCol != -1) {
                        Cell cell = row.getCell(departmentCol);
                        if (cell != null) {
                            departmentStr = getCellValueAsString(cell).trim();
                            if (departmentStr != null && departmentStr.isEmpty()) {
                                departmentStr = null;
                            }
                        }
                    }

                    if ((studentNo == null || studentNo.isEmpty())
                            && (name == null || name.isEmpty())
                            && (defenseType == null || defenseType.isEmpty())
                            && (title == null || title.isEmpty())
                            && (departmentStr == null || departmentStr.isEmpty())) {
                        failCount++;
                        errorMessages.append("第").append(rowIndex + 1).append("行：学号、姓名、类型、题目、所属院系至少需要填写一个；");
                        continue;
                    }

                    if ((studentNo == null || studentNo.isEmpty()) && (name == null || name.isEmpty())) {
                        failCount++;
                        errorMessages.append("第").append(rowIndex + 1).append("行：学号和姓名不能同时为空；");
                        continue;
                    }

                    if (studentNo == null || studentNo.isEmpty()) {
                        String baseName = (name != null && !name.isEmpty()) ? name : "STU";
                        studentNo = baseName + "_" + System.currentTimeMillis() + "_" + rowIndex;
                    }

                    if (name == null || name.isEmpty()) {
                        name = "学生_" + studentNo;
                    }

                    Long finalDepartmentId = departmentId;
                    if (departmentStr != null && !departmentStr.isEmpty()) {
                        Department dept = departmentMapper.findByCode(departmentStr);
                        if (dept == null) {
                            for (Department d : allDepartments) {
                                if (d.getName() != null && d.getName().equals(departmentStr)) {
                                    dept = d;
                                    break;
                                }
                            }
                        }

                        if (dept == null) {
                            failCount++;
                            errorMessages.append("第").append(rowIndex + 1).append("行：院系 ").append(departmentStr)
                                    .append("不存在；");
                            continue;
                        }
                        finalDepartmentId = dept.getId();
                    }

                    if (studentNo != null && !studentNo.isEmpty()) {
                        Student existingStudent = studentMapper.findByStudentNoAndYear(studentNo, currentYear);
                        if (existingStudent != null) {
                            failCount++;
                            errorMessages.append("第").append(rowIndex + 1).append("行：学号").append(studentNo)
                                    .append("在").append(currentYear).append("年已存在；");
                            continue;
                        }
                    }

                    Student student = new Student();
                    student.setStudentNo(studentNo);
                    student.setName(name);
                    student.setDefenseType(defenseType);
                    student.setTitle(title);
                    student.setDefenseYear(currentYear);
                    student.setDepartmentId(finalDepartmentId);
                    student.setClassInfo(classInfo);
                    student.setPhone(phone);
                    student.setEmail(email);
                    student.setDefenseDate(defenseDate);

                    studentService.saveStudent(student);
                    successCount++;

                } catch (Exception e) {
                    failCount++;
                    errorMessages.append("第").append(rowIndex + 1).append("行：").append(e.getMessage()).append("；");
                }
            }

            workbook.close();
            inputStream.close();

            StringBuilder result = new StringBuilder("success:成功导入").append(successCount).append("条");
            if (failCount > 0) {
                result.append("，失败").append(failCount).append("条");
                if (errorMessages.length() > 0) {
                    String errorMsg = errorMessages.toString();
                    if (errorMsg.length() > 500) {
                        errorMsg = errorMsg.substring(0, 500) + "...";
                    }
                    result.append("。错误详情：").append(errorMsg);
                }
            }

            return result.toString();

        } catch (Exception e) {
            return "error:导入失败：" + e.getMessage();
        }
    }

    @GetMapping("/template/download")
    public ResponseEntity<byte[]> downloadStudentTemplate(HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("学生导入模板");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"学号", "姓名", "班级", "联系电话", "邮箱", "答辩日期", "类型", "题目", "所属院系"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            Row exampleRow1 = sheet.createRow(1);
            exampleRow1.createCell(0).setCellValue("20210001");
            exampleRow1.createCell(1).setCellValue("张三");
            exampleRow1.createCell(2).setCellValue("计科2101");
            exampleRow1.createCell(3).setCellValue("13800000001");
            exampleRow1.createCell(4).setCellValue("student01@example.com");
            exampleRow1.createCell(5).setCellValue("2024-06-18");
            exampleRow1.createCell(6).setCellValue("论文");
            exampleRow1.createCell(7).setCellValue("基于深度学习的图像识别研究");
            exampleRow1.createCell(8).setCellValue("计算机科学与技术学院");

            Row exampleRow2 = sheet.createRow(2);
            exampleRow2.createCell(0).setCellValue("20210002");
            exampleRow2.createCell(1).setCellValue("李四");
            exampleRow2.createCell(2).setCellValue("软工2102");
            exampleRow2.createCell(3).setCellValue("13800000002");
            exampleRow2.createCell(4).setCellValue("student02@example.com");
            exampleRow2.createCell(5).setCellValue("2024-06-19");
            exampleRow2.createCell(6).setCellValue("设计");
            exampleRow2.createCell(7).setCellValue("智能家居控制系统设计");
            exampleRow2.createCell(8).setCellValue("CS");

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 3000) {
                    sheet.setColumnWidth(i, 3000);
                }
            }

            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            byte[] bytes = outputStream.toByteArray();
            outputStream.close();

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            responseHeaders.setContentDispositionFormData("attachment", "学生导入模板.xlsx");
            responseHeaders.setContentLength(bytes.length);

            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(bytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String checkDeptAdmin(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "error:权限不足";
        }
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
        if ("SUPER_ADMIN".equals(roleName) || "DEPT_ADMIN".equals(roleName)) {
            return null;
        }
        return "error:权限不足";
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
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private java.sql.Date parseSqlDateFromCell(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            java.util.Date date = cell.getDateCellValue();
            return date != null ? new java.sql.Date(date.getTime()) : null;
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
                return java.sql.Date.valueOf(localDate);
            } catch (DateTimeParseException ignored) {
                // try next formatter
            }
        }
        return null;
    }
}
