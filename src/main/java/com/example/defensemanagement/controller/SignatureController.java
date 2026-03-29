package com.example.defensemanagement.controller;

import com.example.defensemanagement.common.ApiResponse;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * 教师/院系管理员/系主任/超级管理员签名上传。
 */
@Tag(name = "签名管理", description = "教师/管理员签名上传与预览")
@RestController
@RequestMapping("/signature")
public class SignatureController {

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${app.upload.base-dir:uploads}")
    private String baseDir;

    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> uploadSignature(@RequestParam("file") MultipartFile file,
                                                            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
        if (currentUser == null && currentTeacher == null) {
            return ApiResponse.error("未登录");
        }
        String filename = currentUser != null ? "user_" + currentUser.getId() : "teacher_" + currentTeacher.getId();
        String path = fileStorageService.save(file, "signatures", filename);
        return ApiResponse.success("上传成功", Map.of("path", path));
    }

    /** 查看签名图片，供前端 <img src="/signature/view?path=..."> 使用 */
    @GetMapping("/view")
    public ResponseEntity<byte[]> viewSignature(@RequestParam String path, HttpSession session) throws IOException {
        User currentUser = (User) session.getAttribute("currentUser");
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
        if (currentUser == null && currentTeacher == null) {
            return ResponseEntity.status(401).build();
        }
        // 安全校验：只允许访问 signatures/ 目录下的文件
        if (path == null || !path.startsWith("signatures/")) {
            return ResponseEntity.badRequest().build();
        }
        Path basePath = Paths.get(baseDir);
        if (!basePath.isAbsolute()) {
            basePath = Paths.get(System.getProperty("user.dir"), baseDir);
        }
        Path filePath = basePath.resolve(path).normalize();
        if (!filePath.startsWith(basePath.normalize())) {
            return ResponseEntity.badRequest().build();
        }
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = Files.readAllBytes(filePath);
        String lower = path.toLowerCase();
        MediaType mediaType = lower.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(mediaType).body(bytes);
    }
}

