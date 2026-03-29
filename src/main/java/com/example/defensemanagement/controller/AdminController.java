package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.service.UserService;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.mapper.DepartmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpSession;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private DepartmentMapper departmentMapper;

    @GetMapping("/departments")
    public String departmentManagement(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "CREATE_DEPARTMENT")) {
            return "redirect:/";
        }

        List<Department> departments = userService.getAllDepartments();
        model.addAttribute("departments", departments);
        return "admin/departments";
    }

    @PostMapping("/department/create")
    @ResponseBody
    public String createDepartment(@RequestParam String name,
            @RequestParam String code,
            @RequestParam String description,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "CREATE_DEPARTMENT")) {
            return "error:权限不足";
        }

        try {
            userService.createDepartment(name, code, description);
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @PostMapping("/department/update")
    @ResponseBody
    public String updateDepartment(@RequestBody Department department, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "CREATE_DEPARTMENT")) {
            return "error:权限不足";
        }

        try {
            if (userService.updateDepartment(department)) {
                return "success";
            } else {
                return "error:更新失败";
            }
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @DeleteMapping("/department/{id}")
    @ResponseBody
    public String deleteDepartment(@PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "CREATE_DEPARTMENT")) {
            return "error:权限不足";
        }

        try {
            // 检查是否有用户或学生关联到此院系
            List<User> users = userService.getAllUsers(id);
            if (users != null && !users.isEmpty()) {
                return "error:该院系下还有用户，无法删除";
            }

            // 删除院系
            if (userService.deleteDepartment(id)) {
                return "success";
            } else {
                return "error:删除失败";
            }
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    /**
     * 批量删除院系
     * POST /admin/departments/batch-delete
     */
    @PostMapping("/departments/batch-delete")
    @ResponseBody
    public String batchDeleteDepartments(@RequestBody List<Long> ids, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "CREATE_DEPARTMENT")) {
            return "error:权限不足";
        }

        if (ids == null || ids.isEmpty()) {
            return "error:请选择要删除的院系";
        }

        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMessages = new StringBuilder();

        for (Long id : ids) {
            try {
                // 检查是否有用户或学生关联到此院系
                List<User> users = userService.getAllUsers(id);
                if (users != null && !users.isEmpty()) {
                    failCount++;
                    Department dept = departmentMapper.findById(id);
                    String deptName = dept != null ? dept.getName() : "ID:" + id;
                    errorMessages.append("院系[").append(deptName).append("]下还有用户，无法删除；");
                    continue;
                }

                // 删除院系
                if (userService.deleteDepartment(id)) {
                    successCount++;
                } else {
                    failCount++;
                    Department dept = departmentMapper.findById(id);
                    String deptName = dept != null ? dept.getName() : "ID:" + id;
                    errorMessages.append("院系[").append(deptName).append("]删除失败；");
                }
            } catch (Exception e) {
                failCount++;
                Department dept = departmentMapper.findById(id);
                String deptName = dept != null ? dept.getName() : "ID:" + id;
                errorMessages.append("院系[").append(deptName).append("]删除失败：").append(e.getMessage()).append("；");
            }
        }

        // 构建返回消息
        if (failCount == 0) {
            return "success";
        } else {
            String errorMsg = errorMessages.toString();
            if (errorMsg.length() > 500) {
                errorMsg = errorMsg.substring(0, 500) + "...";
            }
            return "error:成功删除" + successCount + "个，失败" + failCount + "个。" + errorMsg;
        }
    }

    @GetMapping("/departments/list")
    @ResponseBody
    public List<Department> getDepartmentList(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return userService.getAllDepartments();
    }

    /**
     * 搜索院系（支持分页）
     * GET /admin/departments/search?keyword=xxx&page=1&pageSize=10
     */
    @GetMapping("/departments/search")
    @ResponseBody
    public Map<String, Object> searchDepartments(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }

        int offset = (page - 1) * pageSize;
        List<Department> departments = departmentMapper.searchDepartments(keyword, offset, pageSize);
        int total = departmentMapper.countDepartments(keyword);
        int totalPages = (int) Math.ceil((double) total / pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("departments", departments);
        result.put("total", total);
        result.put("currentPage", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", totalPages);

        return result;
    }

    /**
     * 下载院系Excel导入模板
     * GET /admin/departments/template/download
     */
    @GetMapping("/departments/template/download")
    public ResponseEntity<byte[]> downloadDepartmentTemplate(HttpSession session) {
        // 权限检查：只有超级管理员和院系管理员可以下载模板
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "CREATE_DEPARTMENT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            // 创建Excel工作簿
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("院系导入模板");

            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // 创建表头行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "院系名称", "院系代码", "描述"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 添加示例数据行
            Row exampleRow1 = sheet.createRow(1);
            exampleRow1.createCell(0).setCellValue("1");
            exampleRow1.createCell(1).setCellValue("计算机科学与技术学院");
            exampleRow1.createCell(2).setCellValue("CS");
            exampleRow1.createCell(3).setCellValue("计算机相关专业");

            Row exampleRow2 = sheet.createRow(2);
            exampleRow2.createCell(0).setCellValue("2");
            exampleRow2.createCell(1).setCellValue("软件学院");
            exampleRow2.createCell(2).setCellValue("SE");
            exampleRow2.createCell(3).setCellValue("软件工程相关专业");

            Row exampleRow3 = sheet.createRow(3);
            exampleRow3.createCell(0).setCellValue("3");
            exampleRow3.createCell(1).setCellValue("信息与通信工程学院");
            exampleRow3.createCell(2).setCellValue("ICE");
            exampleRow3.createCell(3).setCellValue("通信工程相关专业");

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // 设置最小列宽
                if (sheet.getColumnWidth(i) < 3000) {
                    sheet.setColumnWidth(i, 3000);
                }
            }

            // 将工作簿写入字节数组
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            byte[] bytes = outputStream.toByteArray();
            outputStream.close();

            // 设置响应头
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            responseHeaders.setContentDispositionFormData("attachment", "院系导入模板.xlsx");
            responseHeaders.setContentLength(bytes.length);

            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(bytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 从Excel文件导入院系数据
     * POST /admin/departments/import/excel
     * Excel文件应包含以下列：ID、院系名称、院系代码、描述
     * 每行至少需要填写一个字段才能插入
     */
    @PostMapping("/departments/import/excel")
    @ResponseBody
    public String importDepartmentsFromExcel(@RequestParam("file") MultipartFile file, HttpSession session) {
        // 权限检查：只有超级管理员和院系管理员可以导入
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "CREATE_DEPARTMENT")) {
            return "error:权限不足";
        }

        if (file == null || file.isEmpty()) {
            return "error:请选择Excel文件";
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null
                || (!fileName.toLowerCase().endsWith(".xlsx") && !fileName.toLowerCase().endsWith(".xls"))) {
            return "error:请上传Excel文件（.xlsx或.xls格式）";
        }

        try {
            InputStream inputStream = file.getInputStream();
            Workbook workbook;

            if (fileName.toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(inputStream);
            } else {
                workbook = new HSSFWorkbook(inputStream);
            }

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 1) {
                workbook.close();
                inputStream.close();
                return "error:Excel文件为空";
            }

            int successCount = 0;
            int failCount = 0;
            StringBuilder errorMessages = new StringBuilder();

            // 识别表头
            int idCol = -1;
            int nameCol = -1;
            int codeCol = -1;
            int descriptionCol = -1;
            int startRowIndex = 0;

            // 首先尝试识别表头
            boolean hasHeader = false;
            Row firstRow = sheet.getRow(0);
            if (firstRow != null) {
                for (int i = 0; i < firstRow.getPhysicalNumberOfCells(); i++) {
                    Cell cell = firstRow.getCell(i);
                    if (cell != null) {
                        String cellValue = getCellValueAsString(cell).trim();
                        if (cellValue.contains("ID") || cellValue.contains("id") || cellValue.equalsIgnoreCase("id")) {
                            idCol = i;
                            hasHeader = true;
                        } else if (cellValue.contains("院系名称") || cellValue.contains("名称")
                                || cellValue.equalsIgnoreCase("name")) {
                            nameCol = i;
                            hasHeader = true;
                        } else if (cellValue.contains("院系代码") || cellValue.contains("代码")
                                || cellValue.equalsIgnoreCase("code")) {
                            codeCol = i;
                            hasHeader = true;
                        } else if (cellValue.contains("描述") || cellValue.equalsIgnoreCase("description")
                                || cellValue.equalsIgnoreCase("desc")) {
                            descriptionCol = i;
                            hasHeader = true;
                        }
                    }
                }
            }

            // 如果找到了表头，数据从第二行开始
            if (hasHeader && (idCol != -1 || nameCol != -1 || codeCol != -1 || descriptionCol != -1)) {
                startRowIndex = 1;
            } else {
                // 如果没有找到表头，假设第一列是ID，第二列是院系名称，第三列是院系代码，第四列是描述
                idCol = 0;
                nameCol = 1;
                codeCol = 2;
                descriptionCol = 3;
                startRowIndex = 0;
            }

            // 验证至少有一个有效列
            if (idCol == -1 && nameCol == -1 && codeCol == -1 && descriptionCol == -1) {
                workbook.close();
                inputStream.close();
                return "error:Excel文件必须包含至少一列有效数据（ID、院系名称、院系代码、描述中的任意一个）";
            }

            // 遍历数据行
            for (int rowIndex = startRowIndex; rowIndex < sheet.getPhysicalNumberOfRows(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                try {
                    // 读取各列数据
                    String idStr = "";
                    String name = "";
                    String code = "";
                    String description = "";

                    if (idCol != -1) {
                        Cell idCell = row.getCell(idCol);
                        if (idCell != null) {
                            idStr = getCellValueAsString(idCell).trim();
                        }
                    }

                    if (nameCol != -1) {
                        Cell nameCell = row.getCell(nameCol);
                        if (nameCell != null) {
                            name = getCellValueAsString(nameCell).trim();
                        }
                    }

                    if (codeCol != -1) {
                        Cell codeCell = row.getCell(codeCol);
                        if (codeCell != null) {
                            code = getCellValueAsString(codeCell).trim();
                        }
                    }

                    if (descriptionCol != -1) {
                        Cell descCell = row.getCell(descriptionCol);
                        if (descCell != null) {
                            description = getCellValueAsString(descCell).trim();
                        }
                    }

                    // 验证：至少有一个字段有数据
                    if (idStr.isEmpty() && name.isEmpty() && code.isEmpty() && description.isEmpty()) {
                        failCount++;
                        errorMessages.append("第").append(rowIndex + 1).append("行：ID、院系名称、院系代码、描述不能同时为空；");
                        continue;
                    }

                    // 如果提供了ID，检查是否已存在
                    Long departmentId = null;
                    if (!idStr.isEmpty()) {
                        try {
                            departmentId = Long.parseLong(idStr);
                            Department existingDept = departmentMapper.findById(departmentId);
                            if (existingDept != null) {
                                failCount++;
                                errorMessages.append("第").append(rowIndex + 1).append("行：ID ").append(departmentId)
                                        .append("已存在；");
                                continue;
                            }
                        } catch (NumberFormatException e) {
                            failCount++;
                            errorMessages.append("第").append(rowIndex + 1).append("行：ID格式无效；");
                            continue;
                        }
                    }

                    // 如果提供了院系代码，检查是否已存在
                    if (!code.isEmpty()) {
                        Department existingDeptByCode = departmentMapper.findByCode(code);
                        if (existingDeptByCode != null) {
                            failCount++;
                            errorMessages.append("第").append(rowIndex + 1).append("行：院系代码 ").append(code)
                                    .append("已存在；");
                            continue;
                        }
                    }

                    // 创建院系对象
                    Department department = new Department();
                    if (departmentId != null) {
                        department.setId(departmentId);
                    }
                    department.setName(name.isEmpty() ? null : name);
                    department.setCode(code.isEmpty() ? null : code);
                    department.setDescription(description.isEmpty() ? null : description);

                    // 保存院系
                    departmentMapper.insert(department);
                    successCount++;

                } catch (Exception e) {
                    failCount++;
                    errorMessages.append("第").append(rowIndex + 1).append("行：").append(e.getMessage()).append("；");
                }
            }

            workbook.close();
            inputStream.close();

            // 构建返回消息
            StringBuilder result = new StringBuilder("success:成功导入").append(successCount).append("条");
            if (failCount > 0) {
                result.append("，失败").append(failCount).append("条");
                if (errorMessages.length() > 0) {
                    String errorMsg = errorMessages.toString();
                    // 限制错误消息长度
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

    /**
     * 辅助方法：获取单元格的字符串值
     */
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
                    // 处理数字，避免科学计数法
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
}

