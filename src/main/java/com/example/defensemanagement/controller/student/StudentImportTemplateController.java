package com.example.defensemanagement.controller.student;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

/**
 * 学生导入模板下载 Controller。
 */
@RestController
@RequestMapping("/department/student")
public class StudentImportTemplateController extends AbstractStudentController {

    private static final Logger log = LoggerFactory.getLogger(StudentImportTemplateController.class);

    private final StudentImportTemplateSupport studentImportTemplateSupport;

    public StudentImportTemplateController(StudentImportTemplateSupport studentImportTemplateSupport) {
        this.studentImportTemplateSupport = studentImportTemplateSupport;
    }

    @GetMapping("/template/download")
    public ResponseEntity<byte[]> downloadStudentTemplate(HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            byte[] bytes = studentImportTemplateSupport.buildTemplateBytes();
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            responseHeaders.setContentDispositionFormData("attachment", "学生导入模板.xlsx");
            responseHeaders.setContentLength(bytes.length);
            return ResponseEntity.ok().headers(responseHeaders).body(bytes);
        } catch (Exception e) {
            log.warn("Failed to build student import template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
