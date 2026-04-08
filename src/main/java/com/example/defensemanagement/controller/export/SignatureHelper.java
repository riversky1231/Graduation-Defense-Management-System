package com.example.defensemanagement.controller.export;

import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Component
public class SignatureHelper {

    private static final Logger log = LoggerFactory.getLogger(SignatureHelper.class);

    private final DefenseGroupTeacherMapper defenseGroupTeacherMapper;
    private final TeacherMapper teacherMapper;
    private final UserService userService;
    private final TemplateHelper templateHelper;

    public SignatureHelper(
            DefenseGroupTeacherMapper defenseGroupTeacherMapper,
            TeacherMapper teacherMapper,
            UserService userService,
            TemplateHelper templateHelper) {
        this.defenseGroupTeacherMapper = defenseGroupTeacherMapper;
        this.teacherMapper = teacherMapper;
        this.userService = userService;
        this.templateHelper = templateHelper;
    }

    void fillSignatures(Map<String, byte[]> img, Student stu, boolean isGradeForm, boolean isProcessForm) {
        if (stu.getAdvisorTeacherId() != null) {
            byte[] bytes = loadSignature("teacher_" + stu.getAdvisorTeacherId());
            if (bytes != null) {
                img.put("{{SIGN_ADVISOR}}", bytes);
                img.put("{{SIGN_ADVISOR_TEACHER}}", bytes);
            }
        }

        if (stu.getReviewerTeacherId() != null) {
            byte[] bytes = loadSignature("teacher_" + stu.getReviewerTeacherId());
            if (bytes != null) {
                img.put("{{SIGN_REVIEWER}}", bytes);
                img.put("{{SIGN_REVIEWER_TEACHER}}", bytes);
            }
        }

        if (stu.getDefenseGroupId() != null) {
            try {
                DefenseGroupTeacher leader = defenseGroupTeacherMapper.findLeaderByGroupId(stu.getDefenseGroupId());
                if (leader != null && leader.getTeacherId() != null) {
                    byte[] bytes = loadSignature("teacher_" + leader.getTeacherId());
                    if (bytes != null) {
                        img.put("{{SIGN_LEADER}}", bytes);
                        img.put("{{SIGN_GROUP_LEADER}}", bytes);
                    }
                }
            } catch (Exception e) {
                log.warn("加载答辩组长签名失败: {}", e.getMessage());
            }
        }

        if (isGradeForm && stu.getDepartmentId() != null) {
            try {
                List<User> deptAdmins = userService.getUsersByRole("DEPT_ADMIN");
                User fallbackAdmin = null;
                boolean found = false;

                if (deptAdmins != null) {
                    for (User admin : deptAdmins) {
                        if (fallbackAdmin == null && admin.getDepartmentId() != null) {
                            fallbackAdmin = admin;
                        }
                        if (admin.getDepartmentId() != null
                                && admin.getDepartmentId().equals(stu.getDepartmentId())) {
                            byte[] bytes = loadSignature("user_" + admin.getId());
                            if (bytes != null) {
                                img.put("{{SIGN_DEPT_HEAD}}", bytes);
                                img.put("{{SIGN_DEAN}}", bytes);
                                found = true;
                                break;
                            }
                        }
                    }
                }

                if (!found && fallbackAdmin != null) {
                    byte[] bytes = loadSignature("user_" + fallbackAdmin.getId());
                    if (bytes != null) {
                        img.put("{{SIGN_DEPT_HEAD}}", bytes);
                        img.put("{{SIGN_DEAN}}", bytes);
                    }
                }
            } catch (Exception e) {
                log.warn("加载系主任签名失败: {}", e.getMessage());
            }
        }

        if (isProcessForm && stu.getDefenseGroupId() != null) {
            try {
                List<DefenseGroupTeacher> groupTeachers =
                        defenseGroupTeacherMapper.findByGroupId(stu.getDefenseGroupId());
                int judgeIndex = 1;
                for (DefenseGroupTeacher groupTeacher : groupTeachers) {
                    if (groupTeacher.getTeacherId() != null && judgeIndex <= 3) {
                        byte[] bytes = loadSignature("teacher_" + groupTeacher.getTeacherId());
                        if (bytes != null) {
                            img.put("{{SIGN_JUDGE_" + judgeIndex + "}}", bytes);
                            if (judgeIndex == 1) {
                                img.put("{{SIGN_JUDGE}}", bytes);
                            }
                            judgeIndex++;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("加载评委签名失败: {}", e.getMessage());
            }
        }
    }

    byte[] loadSignature(String namePrefix) {
        String[] exts = {"png", "jpg", "jpeg"};
        Path basePath = templateHelper.getUploadBasePath();

        for (String ext : exts) {
            Path path = basePath.resolve("signatures").resolve(namePrefix + "." + ext);
            if (Files.exists(path)) {
                try {
                    return Files.readAllBytes(path);
                } catch (Exception e) {
                    log.warn("读取签名文件失败 {}: {}", path, e.getMessage());
                }
            }
        }

        if (!namePrefix.startsWith("teacher_")) {
            return null;
        }

        try {
            Long teacherId = Long.parseLong(namePrefix.substring("teacher_".length()));
            Teacher teacher = teacherMapper.findById(teacherId);
            if (teacher == null || teacher.getUserId() == null) {
                return null;
            }

            String userPrefix = "user_" + teacher.getUserId();
            for (String ext : exts) {
                Path path = basePath.resolve("signatures").resolve(userPrefix + "." + ext);
                if (Files.exists(path)) {
                    try {
                        return Files.readAllBytes(path);
                    } catch (Exception e) {
                        log.warn("读取user_签名失败 {}: {}", path, e.getMessage());
                    }
                }
            }
        } catch (NumberFormatException ignored) {
        }

        return null;
    }
}
