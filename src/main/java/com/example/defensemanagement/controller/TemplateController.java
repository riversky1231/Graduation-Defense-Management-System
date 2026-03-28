package com.example.defensemanagement.controller;

import com.example.defensemanagement.common.ApiResponse;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * 超级管理员或院系管理员上传 Word 模板。
 * - 超级管理员：上传到全局目录 templates/
 * - 院系管理员：上传到院系目录 templates/dept_{deptId}/，仅覆盖本院系的模板
 */
@RestController
@RequestMapping("/admin/template")
public class TemplateController {

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> uploadTemplate(@RequestParam String templateKey,
                                                           @RequestParam("file") MultipartFile file,
                                                           HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return ApiResponse.error("权限不足");
        }
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
        if (!"SUPER_ADMIN".equals(roleName) && !"DEPT_ADMIN".equals(roleName)) {
            return ApiResponse.error("权限不足");
        }

        if (templateKey == null || templateKey.trim().isEmpty()) {
            return ApiResponse.error("模板标识不能为空");
        }

        String subDir;
        if ("DEPT_ADMIN".equals(roleName)) {
            Long deptId = currentUser.getDepartmentId();
            if (deptId == null) {
                return ApiResponse.error("当前账号未绑定院系，无法上传模板");
            }
            subDir = "templates/dept_" + deptId;
        } else {
            subDir = "templates";
        }

        String path = fileStorageService.save(file, subDir, templateKey);
        return ApiResponse.success("上传成功", Map.of("path", path));
    }
}
