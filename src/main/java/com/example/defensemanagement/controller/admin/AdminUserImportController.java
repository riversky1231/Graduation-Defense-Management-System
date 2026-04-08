package com.example.defensemanagement.controller.admin;

import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.UserService;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 用户导入与模板下载端点。
 */
@Controller
@RequestMapping("/admin")
public class AdminUserImportController extends AbstractAdminUserController {

    private final UserExcelImportSupport userExcelImportSupport;
    private final UserImportTemplateSupport userImportTemplateSupport;

    public AdminUserImportController(
            UserService userService,
            AuthService authService,
            TeacherMapper teacherMapper,
            UserExcelImportSupport userExcelImportSupport,
            UserImportTemplateSupport userImportTemplateSupport) {
        super(userService, authService, teacherMapper);
        this.userExcelImportSupport = userExcelImportSupport;
        this.userImportTemplateSupport = userImportTemplateSupport;
    }

    @PostMapping("/users/import/excel")
    @ResponseBody
    public String importExcel(@RequestParam("file") MultipartFile file, HttpSession session) {
        if (!hasCreateUserPermission(session)) {
            return "error:权限不足";
        }
        return userExcelImportSupport.importUsers(file);
    }

    @GetMapping("/users/template/download")
    public ResponseEntity<byte[]> downloadTemplate(HttpSession session) {
        if (!hasCreateUserPermission(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            byte[] bytes = userImportTemplateSupport.buildTemplateBytes();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + encode("用户导入模板.xlsx") + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String encode(String name) {
        try {
            return URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception e) {
            return name;
        }
    }
}
