package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.FileStorageService;
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
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/avatar")
public class AvatarController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Value("${app.upload.base-dir:uploads}")
    private String baseDir;

    /** 教师或学生上传头像 */
    @PostMapping("/upload")
    public Map<String, Object> uploadAvatar(@RequestParam("file") MultipartFile file,
                                            HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User currentUser = (User) session.getAttribute("currentUser");
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");

        try {
            String filename;
            if (currentTeacher != null) {
                filename = "teacher_" + currentTeacher.getId();
                String path = fileStorageService.save(file, "avatars", filename);
                teacherMapper.updateAvatar(currentTeacher.getId(), path);
                // 同步更新 session 中的 teacher 对象
                currentTeacher.setAvatarPath(path);
                session.setAttribute("currentTeacher", currentTeacher);
                result.put("path", path);
                result.put("url", "/avatar/view?path=" + path);
            } else if (currentUser != null) {
                // 学生通过 user 关联 student
                filename = "user_" + currentUser.getId();
                String path = fileStorageService.save(file, "avatars", filename);
                // 找到对应的 student 记录
                Student student = findStudentByUser(currentUser, session);
                if (student != null) {
                    studentMapper.updateAvatarPath(student.getId(), path);
                }
                // 存到 session 方便侧边栏读取
                session.setAttribute("avatarPath", path);
                result.put("path", path);
                result.put("url", "/avatar/view?path=" + path);
            } else {
                result.put("error", "未登录");
                return result;
            }
            result.put("success", true);
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    /** 获取当前用户头像路径 */
    @GetMapping("/me")
    public Map<String, Object> getMyAvatar(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
        User currentUser = (User) session.getAttribute("currentUser");

        String avatarPath = null;
        if (currentTeacher != null) {
            Teacher t = teacherMapper.findById(currentTeacher.getId());
            avatarPath = t != null ? t.getAvatarPath() : null;
        } else if (currentUser != null) {
            Student student = findStudentByUser(currentUser, session);
            avatarPath = student != null ? student.getAvatarPath() : null;
        }
        result.put("avatarPath", avatarPath);
        result.put("avatarUrl", avatarPath != null ? "/avatar/view?path=" + avatarPath : null);
        return result;
    }

    /** 返回图片文件流，供 <img src="/avatar/view?path=..."> 使用 */
    @GetMapping("/view")
    public ResponseEntity<byte[]> viewAvatar(@RequestParam String path) throws IOException {
        // 安全校验：只允许访问 avatars/ 目录下的文件
        if (path == null || !path.startsWith("avatars/")) {
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
        byte[] data = Files.readAllBytes(filePath);
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) contentType = "image/jpeg";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }

    private Student findStudentByUser(User user, HttpSession session) {
        // 从 session 缓存中取
        Student cached = (Student) session.getAttribute("currentStudent");
        return cached;
    }
}

