package com.example.defensemanagement.controller;

import com.example.defensemanagement.common.ApiResponse;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * 教师/院系管理员/系主任/超级管理员签名上传。
 */
@RestController
@RequestMapping("/signature")
public class SignatureController {

    @Autowired
    private FileStorageService fileStorageService;

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
}

