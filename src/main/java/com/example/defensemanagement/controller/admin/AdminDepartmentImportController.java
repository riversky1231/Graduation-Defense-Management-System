package com.example.defensemanagement.controller.admin;

import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;

/**
 * 院系模板下载与 Excel 导入 Controller。
 */
@RestController
@RequestMapping("/admin")
public class AdminDepartmentImportController extends AbstractAdminDepartmentController {

    private final DepartmentExcelSupport departmentExcelSupport;

    public AdminDepartmentImportController(
            UserService userService,
            AuthService authService,
            DepartmentMapper departmentMapper,
            DepartmentExcelSupport departmentExcelSupport) {
        super(userService, authService, departmentMapper);
        this.departmentExcelSupport = departmentExcelSupport;
    }

    @GetMapping("/departments/template/download")
    public ResponseEntity<byte[]> downloadDepartmentTemplate(HttpSession session) {
        if (!canManageDepartments(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            byte[] bytes = departmentExcelSupport.buildTemplateBytes();
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            responseHeaders.setContentDispositionFormData("attachment", "院系导入模板.xlsx");
            responseHeaders.setContentLength(bytes.length);
            return ResponseEntity.ok().headers(responseHeaders).body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/departments/import/excel")
    @ResponseBody
    public String importDepartmentsFromExcel(@RequestParam("file") MultipartFile file, HttpSession session) {
        String permissionError = checkDepartmentPermission(session);
        if (permissionError != null) {
            return permissionError;
        }
        return departmentExcelSupport.importDepartments(file);
    }
}
