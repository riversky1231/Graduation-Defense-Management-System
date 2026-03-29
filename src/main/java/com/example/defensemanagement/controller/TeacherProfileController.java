package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.TeacherProfile;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherProfileMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/teacher/profile")
public class TeacherProfileController {

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private TeacherProfileMapper teacherProfileMapper;

    private Teacher getCurrentTeacher(HttpSession session) {
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
        if (currentTeacher != null) {
            return currentTeacher;
        }
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null && currentUser.getRole() != null) {
            String role = currentUser.getRole().getName();
            if ("TEACHER".equals(role) || "DEFENSE_LEADER".equals(role)) {
                return teacherMapper.findByUserId(currentUser.getId());
            }
        }
        return null;
    }

    @GetMapping
    @ResponseBody
    public Map<String, Object> getProfile(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Teacher teacher = getCurrentTeacher(session);
        if (teacher == null) {
            result.put("error", "未登录或非教师身份");
            return result;
        }
        TeacherProfile profile = teacherProfileMapper.findByTeacherId(teacher.getId());
        result.put("teacherId", teacher.getId());
        result.put("teacherNo", teacher.getTeacherNo());
        result.put("teacherName", teacher.getName());
        result.put("title", teacher.getTitle());
        result.put("email", teacher.getEmail());
        result.put("phone", teacher.getPhone());
        result.put("researchDirection", profile != null ? profile.getResearchDirection() : "");
        result.put("enrollmentRequirements", profile != null ? profile.getEnrollmentRequirements() : "");
        result.put("avatarPath", teacher.getAvatarPath());
        result.put("avatarUrl", teacher.getAvatarPath() != null ? "/avatar/view?path=" + teacher.getAvatarPath() : null);
        return result;
    }

    @PostMapping
    @ResponseBody
    public String saveProfile(@RequestBody Map<String, String> payload, HttpSession session) {
        Teacher teacher = getCurrentTeacher(session);
        if (teacher == null) {
            return "error:未登录或非教师身份";
        }
        String researchDirection = payload.getOrDefault("researchDirection", "").trim();
        String enrollmentRequirements = payload.getOrDefault("enrollmentRequirements", "").trim();

        TeacherProfile profile = teacherProfileMapper.findByTeacherId(teacher.getId());
        if (profile == null) {
            profile = new TeacherProfile();
            profile.setTeacherId(teacher.getId());
            profile.setResearchDirection(researchDirection);
            profile.setEnrollmentRequirements(enrollmentRequirements);
            teacherProfileMapper.insert(profile);
        } else {
            profile.setResearchDirection(researchDirection);
            profile.setEnrollmentRequirements(enrollmentRequirements);
            teacherProfileMapper.update(profile);
        }
        return "success";
    }
}
