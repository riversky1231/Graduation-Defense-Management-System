package com.example.defensemanagement.controller.admin;

import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.mapper.RoleMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.PermissionService;
import com.example.defensemanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import javax.servlet.http.HttpSession;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 用户管理端点（从 AdminController 拆分）。
 */
@Controller
@RequestMapping("/admin")
public class AdminUserController {

    @Autowired private UserService userService;
    @Autowired private AuthService authService;
    @Autowired private PermissionService permissionService;
    @Autowired private TeacherMapper teacherMapper;
    @Autowired private RoleMapper roleMapper;
    @Autowired private DepartmentMapper departmentMapper;
    @GetMapping("/users/list")
    @ResponseBody
    public List<User> list(HttpSession session) {
        Long deptId = getDepartmentIdIfDeptAdmin(session);
        return userService.getAllUsers(deptId);
    }

    @GetMapping("/users/search")
    @ResponseBody
    public Map<String, Object> search(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int pageSize,
            HttpSession session) {
        Long deptId = getDepartmentIdIfDeptAdmin(session);
        List<User> users = userService.searchUsers(keyword, page, pageSize, deptId);
        int total = userService.countUsers(keyword, deptId);
        int totalPages = (int) Math.ceil((double) total / pageSize);
        Map<String, Object> result = new HashMap<>();
        result.put("users", users); result.put("total", total);
        result.put("currentPage", page); result.put("pageSize", pageSize);
        result.put("totalPages", totalPages);
        return result;
    }

    @GetMapping("/roles/list")
    @ResponseBody
    public List<Role> roleList(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return userService.getManagableRoles(currentUser);
    }

    @PostMapping("/users/save")
    @ResponseBody
    public String save(@RequestBody User user, HttpSession session) {
        Object currentUserObj = session.getAttribute("currentUser");
        if (currentUserObj == null) currentUserObj = session.getAttribute("currentTeacher");
        if (currentUserObj == null) return "error:未登录";

        if (user.getId() == null) {
            if (!permissionService.canCreateUser(currentUserObj, user)) {
                return "error:权限不足，无法创建该角色的用户";
            }
        } else {
            User target = userService.findById(user.getId());
            if (target == null) return "error:目标用户不存在";
            if (!permissionService.canEditUser(currentUserObj, target)) {
                return "error:权限不足";
            }
        }
        try {
            userService.saveUser(user);
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @PostMapping("/user/{id}/status")
    @ResponseBody
    public String updateStatus(@PathVariable Long id, @RequestParam Integer status, HttpSession session) {
        Object currentUserObj = session.getAttribute("currentUser");
        if (currentUserObj == null) currentUserObj = session.getAttribute("currentTeacher");
        if (currentUserObj == null) return "error:未登录";

        User target = userService.findById(id);
        if (target == null) return "error:目标用户不存在";
        if (!permissionService.canEditUser(currentUserObj, target)) return "error:权限不足";
        return userService.updateUserStatus(id, status) ? "success" : "error:更新失败";
    }

    @DeleteMapping("/user/{id}")
    @ResponseBody
    public String delete(@PathVariable Long id, HttpSession session) {
        Object currentUserObj = session.getAttribute("currentUser");
        if (currentUserObj == null) currentUserObj = session.getAttribute("currentTeacher");
        if (currentUserObj == null) return "error:未登录";

        User target = userService.findById(id);
        if (target == null) return "error:目标用户不存在";
        if (!permissionService.canEditUser(currentUserObj, target)) return "error:权限不足";

        if (currentUserObj instanceof User && ((User) currentUserObj).getId().equals(id)) {
            return "error:不能删除自己";
        }
        try {
            return userService.deleteUser(id) ? "success" : "error:删除失败";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @PostMapping("/users/batch-delete")
    @ResponseBody
    public String batchDelete(@RequestBody List<Long> ids, HttpSession session) {
        Object currentUserObj = session.getAttribute("currentUser");
        if (currentUserObj == null) currentUserObj = session.getAttribute("currentTeacher");
        if (currentUserObj == null) return "error:未登录";
        if (ids == null || ids.isEmpty()) return "error:请选择要删除的用户";

        Long currentUserId = (currentUserObj instanceof User) ? ((User) currentUserObj).getId() : null;
        int success = 0, fail = 0;
        StringBuilder errors = new StringBuilder();

        for (Long id : ids) {
            try {
                if (currentUserId != null && currentUserId.equals(id)) {
                    fail++; errors.append("用户[ID:").append(id).append("]不能删除自己；");
                    continue;
                }
                User target = userService.findById(id);
                if (target == null) { fail++; errors.append("用户[ID:").append(id).append("]不存在；"); continue; }
                if (!permissionService.canEditUser(currentUserObj, target)) {
                    fail++; errors.append("用户[").append(target.getUsername()).append("]权限不足，无法删除；"); continue;
                }
                if (userService.deleteUser(id)) success++; else fail++;
            } catch (Exception e) {
                fail++; errors.append("用户[ID:").append(id).append("]删除失败：").append(e.getMessage()).append("；");
            }
        }
        if (fail == 0) return "success";
        String msg = errors.length() > 500 ? errors.substring(0, 500) + "..." : errors.toString();
        return "error:成功删除" + success + "个，失败" + fail + "个。" + msg;
    }

    @PostMapping("/users/import/excel")
    @ResponseBody
    public String importExcel(@RequestParam("file") MultipartFile file, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "CREATE_USER")) {
            return "error:权限不足";
        }
        if (file == null || file.isEmpty()) return "error:请选择Excel文件";
        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.toLowerCase().endsWith(".xlsx") && !fileName.toLowerCase().endsWith(".xls"))) {
            return "error:请上传Excel文件（.xlsx或.xls格式）";
        }

        try (InputStream is = file.getInputStream();
             Workbook wb = fileName.toLowerCase().endsWith(".xlsx")
                     ? new XSSFWorkbook(is) : new HSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 1) {
                return "error:Excel文件为空";
            }

            int usernameCol = -1, realNameCol = -1, roleCol = -1, deptCol = -1, statusCol = -1, startRow = 0;
            boolean hasHeader = false;
            Row firstRow = sheet.getRow(0);
            if (firstRow != null) {
                for (int i = 0; i < firstRow.getPhysicalNumberOfCells(); i++) {
                    Cell cell = firstRow.getCell(i);
                    if (cell == null) continue;
                    String val = getCellValue(cell).trim();
                    if (val.contains("用户名") || val.equalsIgnoreCase("username")) { usernameCol = i; hasHeader = true; }
                    else if (val.contains("真实姓名") || val.equalsIgnoreCase("realname")) { realNameCol = i; hasHeader = true; }
                    else if (val.contains("角色") || val.equalsIgnoreCase("role")) { roleCol = i; hasHeader = true; }
                    else if (val.contains("院系") || val.equalsIgnoreCase("department")) { deptCol = i; hasHeader = true; }
                    else if (val.contains("状态") || val.equalsIgnoreCase("status")) { statusCol = i; hasHeader = true; }
                }
            }
            if (!hasHeader || (usernameCol == -1 && realNameCol == -1 && roleCol == -1)) {
                usernameCol = 0; realNameCol = 1; roleCol = 2; deptCol = 3; statusCol = 4; startRow = 0;
            } else { startRow = 1; }

            int success = 0, fail = 0;
            StringBuilder errors = new StringBuilder();

            for (int rowIndex = startRow; rowIndex < sheet.getPhysicalNumberOfRows(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;
                try {
                    String username = getCellValue(row.getCell(usernameCol)).trim();
                    String realName = getCellValue(row.getCell(realNameCol)).trim();
                    String roleName = getCellValue(row.getCell(roleCol)).trim();
                    String deptStr = getCellValue(row.getCell(deptCol)).trim();
                    String statusStr = getCellValue(row.getCell(statusCol)).trim();

                    if (username.isEmpty() && realName.isEmpty() && roleName.isEmpty()) {
                        fail++; errors.append("第").append(rowIndex + 1).append("行：必填字段为空；"); continue;
                    }
                    if (!username.isEmpty() && userService.findByUsername(username) != null) {
                        fail++; errors.append("第").append(rowIndex + 1).append("行：用户名 ").append(username).append(" 已存在；"); continue;
                    }
                    if (username.isEmpty()) username = realName.isEmpty() ? "user_" + System.currentTimeMillis() : realName + "_" + System.currentTimeMillis();

                    Long roleId = null;
                    if (!roleName.isEmpty()) {
                        Role role = resolveRole(roleName);
                        if (role == null) { fail++; errors.append("第").append(rowIndex + 1).append("行：角色 ").append(roleName).append(" 不存在；"); continue; }
                        roleId = role.getId();
                    }

                    Long deptId = resolveDepartmentId(deptStr);
                    if (!deptStr.isEmpty() && deptId == null) {
                        fail++;
                        errors.append("第").append(rowIndex + 1).append("行：院系 ").append(deptStr).append(" 不存在；");
                        continue;
                    }
                    Integer status = parseStatus(statusStr);
                    boolean isTeacher = roleId != null && "TEACHER".equals(roleMapper.findById(roleId).getName());

                    if (isTeacher && username.isEmpty()) { fail++; errors.append("第").append(rowIndex + 1).append("行：教师角色必须提供用户名；"); continue; }
                    if (isTeacher && teacherMapper.findByTeacherNo(username) != null) { fail++; errors.append("第").append(rowIndex + 1).append("行：教师编号 ").append(username).append(" 已存在；"); continue; }

                    User user = new User();
                    user.setUsername(username);
                    user.setPassword(username);
                    user.setRealName(realName.isEmpty() ? null : realName);
                    user.setRoleId(roleId);
                    user.setDepartmentId(deptId);
                    user.setStatus(status);
                    userService.saveUser(user);

                    if (isTeacher) {
                        Teacher teacher = teacherMapper.findByUserId(user.getId());
                        if (teacher != null) {
                            teacher.setName(realName.isEmpty() ? username : realName);
                            teacher.setDepartmentId(deptId);
                            teacher.setStatus(status);
                            teacher.setPassword(user.getPassword());
                            teacherMapper.update(teacher);
                        }
                    }
                    success++;
                } catch (Exception e) {
                    fail++; errors.append("第").append(rowIndex + 1).append("行：").append(e.getMessage()).append("；");
                }
            }

            String result = "success:成功导入" + success + "条";
            if (fail > 0) {
                String msg = errors.length() > 500 ? errors.substring(0, 500) + "..." : errors.toString();
                result += "，失败" + fail + "条。错误详情：" + msg;
            }
            return result;
        } catch (Exception e) {
            return "error:导入失败：" + e.getMessage();
        }
    }

    @GetMapping("/users/template/download")
    public ResponseEntity<byte[]> downloadTemplate(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "CREATE_USER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("用户导入模板");
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont(); font.setBold(true); font.setFontHeightInPoints((short) 12);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"用户名", "真实姓名", "角色", "院系", "状态"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            String[][] examples = {
                    {"admin001", "张三", "SUPER_ADMIN", "CS", "1"},
                    {"T001", "李四", "TEACHER", "计算机科学与技术学院", "启用"},
                    {"dept_admin", "王五", "院系管理员", "CS", "1"},
                    {"20210001", "赵六", "STUDENT", "计算机科学与技术学院", "启用"}
            };
            for (int r = 0; r < examples.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < examples[r].length; c++) row.createCell(c).setCellValue(examples[r][c]);
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 3000) sheet.setColumnWidth(i, 3000);
            }
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            wb.write(out);
            byte[] bytes = out.toByteArray();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encode("用户导入模板.xlsx") + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user/{userId}/isDefenseLeader")
    @ResponseBody
    public boolean isDefenseLeader(@PathVariable Long userId, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) return false;
        User target = userService.findById(userId);
        if (target == null || target.getRole() == null || !"TEACHER".equals(target.getRole().getName())) return false;
        Teacher teacher = teacherMapper.findByUserId(userId);
        if (teacher == null) return false;
        return authService.isDefenseLeader(teacher.getId(), java.time.LocalDate.now().getYear());
    }

    private Long getDepartmentIdIfDeptAdmin(HttpSession session) {
        Object userObj = session.getAttribute("currentUser");
        if (userObj instanceof User) {
            User u = (User) userObj;
            if (u.getRole() != null && "DEPT_ADMIN".equals(u.getRole().getName())) {
                return u.getDepartmentId();
            }
        }
        return null;
    }

    private Role resolveRole(String name) {
        String upper = name.toUpperCase();
        if (upper.contains("SUPER_ADMIN") || upper.contains("超级管理员")) return roleMapper.findByName("SUPER_ADMIN");
        if (upper.contains("DEPT_ADMIN") || upper.contains("院系管理员")) return roleMapper.findByName("DEPT_ADMIN");
        if (upper.contains("TEACHER") || upper.contains("教师")) return roleMapper.findByName("TEACHER");
        if (upper.contains("STUDENT") || upper.contains("学生")) return roleMapper.findByName("STUDENT");
        if (upper.contains("DEFENSE_LEADER") || upper.contains("答辩组长")) return roleMapper.findByName("DEFENSE_LEADER");
        return roleMapper.findByName(name);
    }

    private Long resolveDepartmentId(String deptStr) {
        if (deptStr == null || deptStr.isEmpty()) return null;
        Department dept = departmentMapper.findByCode(deptStr);
        if (dept == null) {
            for (Department d : userService.getAllDepartments()) {
                if (d.getName() != null && d.getName().equals(deptStr)) { dept = d; break; }
            }
        }
        return dept != null ? dept.getId() : null;
    }

    private Integer parseStatus(String s) {
        if (s == null || s.isEmpty()) return 1;
        try {
            int v = Integer.parseInt(s.trim());
            return (v == 0 || v == 1) ? v : 1;
        } catch (NumberFormatException e) {
            if (s.contains("启用") || s.contains("激活")) return 1;
            if (s.contains("禁用") || s.contains("停用")) return 0;
            return 1;
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: if (DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue().toString();
                          double nv = cell.getNumericCellValue();
                          return nv == (long) nv ? String.valueOf((long) nv) : String.valueOf(nv);
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default: return "";
        }
    }

    private String encode(String name) {
        try { return java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"); }
        catch (Exception e) { return name; }
    }
}
