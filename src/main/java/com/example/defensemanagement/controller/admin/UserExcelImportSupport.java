package com.example.defensemanagement.controller.admin;

import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.mapper.RoleMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.UserService;
import org.apache.poi.ss.usermodel.Cell;
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
import java.util.List;

/**
 * 用户 Excel 导入支持类。
 */
@Component
public class UserExcelImportSupport {

    private static final Logger log = LoggerFactory.getLogger(UserExcelImportSupport.class);
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    private final UserService userService;
    private final TeacherMapper teacherMapper;
    private final RoleMapper roleMapper;
    private final DepartmentMapper departmentMapper;

    public UserExcelImportSupport(
            UserService userService,
            TeacherMapper teacherMapper,
            RoleMapper roleMapper,
            DepartmentMapper departmentMapper) {
        this.userService = userService;
        this.teacherMapper = teacherMapper;
        this.roleMapper = roleMapper;
        this.departmentMapper = departmentMapper;
    }

    public String importUsers(MultipartFile file) {
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
            int successCount = 0;
            int failCount = 0;
            StringBuilder errorMessages = new StringBuilder();

            for (int rowIndex = context.startRowIndex; rowIndex < sheet.getPhysicalNumberOfRows(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                try {
                    String username = readCell(row, context.usernameCol);
                    String realName = readCell(row, context.realNameCol);
                    String roleName = readCell(row, context.roleCol);
                    String departmentText = readCell(row, context.departmentCol);
                    String statusText = readCell(row, context.statusCol);

                    if (username.isEmpty() && realName.isEmpty() && roleName.isEmpty()) {
                        failCount++;
                        errorMessages.append("第").append(rowIndex + 1).append("行：必填字段为空；");
                        continue;
                    }
                    if (!username.isEmpty() && userService.findByUsername(username) != null) {
                        failCount++;
                        errorMessages.append("第").append(rowIndex + 1).append("行：用户名 ")
                                .append(username).append(" 已存在；");
                        continue;
                    }
                    if (username.isEmpty()) {
                        username = realName.isEmpty()
                                ? "user_" + System.currentTimeMillis()
                                : realName + "_" + System.currentTimeMillis();
                    }

                    Role role = roleName.isEmpty() ? null : resolveRole(roleName);
                    if (!roleName.isEmpty() && role == null) {
                        failCount++;
                        errorMessages.append("第").append(rowIndex + 1).append("行：角色 ")
                                .append(roleName).append(" 不存在；");
                        continue;
                    }

                    Long departmentId = resolveDepartmentId(departmentText);
                    if (!departmentText.isEmpty() && departmentId == null) {
                        failCount++;
                        errorMessages.append("第").append(rowIndex + 1).append("行：院系 ")
                                .append(departmentText).append(" 不存在；");
                        continue;
                    }

                    boolean isTeacher = role != null && "TEACHER".equals(role.getName());
                    if (isTeacher && teacherMapper.findByTeacherNo(username) != null) {
                        failCount++;
                        errorMessages.append("第").append(rowIndex + 1).append("行：教师编号 ")
                                .append(username).append(" 已存在；");
                        continue;
                    }

                    User user = new User();
                    user.setUsername(username);
                    user.setPassword(username);
                    user.setRealName(realName.isEmpty() ? null : realName);
                    user.setRoleId(role != null ? role.getId() : null);
                    user.setDepartmentId(departmentId);
                    user.setStatus(parseStatus(statusText));
                    userService.saveUser(user);

                    if (isTeacher && user.getId() != null) {
                        Teacher teacher = teacherMapper.findByUserId(user.getId());
                        if (teacher != null) {
                            teacher.setName(realName.isEmpty() ? username : realName);
                            teacher.setDepartmentId(departmentId);
                            teacher.setStatus(user.getStatus());
                            teacher.setPassword(user.getPassword());
                            teacherMapper.update(teacher);
                        }
                    }

                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    errorMessages.append("第").append(rowIndex + 1).append("行：")
                            .append(e.getMessage()).append("；");
                }
            }

            return buildImportResult(successCount, failCount, errorMessages);
        } catch (Exception e) {
            log.warn("导入用户 Excel 失败: {}", e.getMessage(), e);
            return "error:导入失败：" + e.getMessage();
        }
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
                String value = getCellValueAsString(cell).trim();
                if (value.contains("用户名") || value.equalsIgnoreCase("username")) {
                    context.usernameCol = i;
                    context.hasHeader = true;
                } else if (value.contains("真实姓名") || value.equalsIgnoreCase("realname")) {
                    context.realNameCol = i;
                    context.hasHeader = true;
                } else if (value.contains("角色") || value.equalsIgnoreCase("role")) {
                    context.roleCol = i;
                    context.hasHeader = true;
                } else if (value.contains("院系") || value.equalsIgnoreCase("department")) {
                    context.departmentCol = i;
                    context.hasHeader = true;
                } else if (value.contains("状态") || value.equalsIgnoreCase("status")) {
                    context.statusCol = i;
                    context.hasHeader = true;
                }
            }
        }

        if (!context.hasHeader || (context.usernameCol == -1 && context.realNameCol == -1 && context.roleCol == -1)) {
            context.usernameCol = 0;
            context.realNameCol = 1;
            context.roleCol = 2;
            context.departmentCol = 3;
            context.statusCol = 4;
            context.startRowIndex = 0;
            return context;
        }

        context.startRowIndex = 1;
        return context;
    }

    private Role resolveRole(String name) {
        String upper = name.toUpperCase();
        if (upper.contains("SUPER_ADMIN") || upper.contains("超级管理员")) {
            return roleMapper.findByName("SUPER_ADMIN");
        }
        if (upper.contains("DEPT_ADMIN") || upper.contains("院系管理员")) {
            return roleMapper.findByName("DEPT_ADMIN");
        }
        if (upper.contains("TEACHER") || upper.contains("教师")) {
            return roleMapper.findByName("TEACHER");
        }
        if (upper.contains("STUDENT") || upper.contains("学生")) {
            return roleMapper.findByName("STUDENT");
        }
        if (upper.contains("DEFENSE_LEADER") || upper.contains("答辩组长")) {
            return roleMapper.findByName("DEFENSE_LEADER");
        }
        return roleMapper.findByName(name);
    }

    private Long resolveDepartmentId(String departmentText) {
        if (departmentText == null || departmentText.isEmpty()) {
            return null;
        }

        Department department = departmentMapper.findByCode(departmentText);
        if (department == null) {
            List<Department> allDepartments = userService.getAllDepartments();
            for (Department item : allDepartments) {
                if (item.getName() != null && item.getName().equals(departmentText)) {
                    department = item;
                    break;
                }
            }
        }
        return department != null ? department.getId() : null;
    }

    private Integer parseStatus(String statusText) {
        if (statusText == null || statusText.isEmpty()) {
            return 1;
        }
        try {
            int value = Integer.parseInt(statusText.trim());
            return (value == 0 || value == 1) ? value : 1;
        } catch (NumberFormatException e) {
            if (statusText.contains("启用") || statusText.contains("激活")) {
                return 1;
            }
            if (statusText.contains("禁用") || statusText.contains("停用")) {
                return 0;
            }
            return 1;
        }
    }

    private String readCell(Row row, int columnIndex) {
        if (columnIndex == -1) {
            return "";
        }
        Cell cell = row.getCell(columnIndex);
        return cell == null ? "" : getCellValueAsString(cell).trim();
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

    private String buildImportResult(int successCount, int failCount, StringBuilder errorMessages) {
        StringBuilder result = new StringBuilder("success:成功导入").append(successCount).append("条");
        if (failCount > 0) {
            String message = errorMessages.toString();
            if (message.length() > MAX_ERROR_MESSAGE_LENGTH) {
                message = message.substring(0, MAX_ERROR_MESSAGE_LENGTH) + "...";
            }
            result.append("，失败").append(failCount).append("条。错误详情：").append(message);
        }
        return result.toString();
    }

    private static final class ImportContext {
        private int usernameCol = -1;
        private int realNameCol = -1;
        private int roleCol = -1;
        private int departmentCol = -1;
        private int statusCol = -1;
        private int startRowIndex = 0;
        private boolean hasHeader = false;
    }
}
