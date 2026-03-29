package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.StudentPreference;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.TeacherProfile;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.StudentPreferenceMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherProfileMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.impl.ConfigServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Controller
@RequestMapping("/student")
public class StudentPortalController {

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private TeacherProfileMapper teacherProfileMapper;

    @Autowired
    private StudentPreferenceMapper studentPreferenceMapper;

    @Autowired
    private StudentFinalScoreMapper studentFinalScoreMapper;

    @Autowired
    private ConfigService configService;

    private Student getCurrentStudent(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || currentUser.getRole() == null ||
                !"STUDENT".equals(currentUser.getRole().getName())) {
            return null;
        }
        Integer year = configService.getCurrentDefenseYear();
        if (year == null) {
            year = java.time.Year.now().getValue();
        }
        Student student = studentMapper.findByStudentNoAndYear(currentUser.getUsername(), year);
        if (student == null) {
            student = studentMapper.findLatestByStudentNo(currentUser.getUsername());
        }
        return student;
    }

    @GetMapping("/me")
    @ResponseBody
    public Map<String, Object> getStudentProfile(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Student student = getCurrentStudent(session);
        if (student == null) {
            result.put("error", "学生信息不存在或未绑定");
            return result;
        }
        result.put("student", student);
        result.put("year", student.getDefenseYear());
        return result;
    }

    @GetMapping("/teachers")
    @ResponseBody
    public List<Map<String, Object>> getDepartmentTeachers(HttpSession session) {
        Student student = getCurrentStudent(session);
        if (student == null) {
            return Collections.emptyList();
        }
        Long departmentId = student.getDepartmentId();
        if (departmentId == null) {
            return Collections.emptyList();
        }
        List<Teacher> teachers = teacherMapper.findByDepartmentId(departmentId);
        List<Map<String, Object>> list = new ArrayList<>();
        if (teachers != null) {
            for (Teacher t : teachers) {
                TeacherProfile profile = teacherProfileMapper.findByTeacherId(t.getId());
                Map<String, Object> info = new HashMap<>();
                info.put("id", t.getId());
                info.put("teacherNo", t.getTeacherNo());
                info.put("name", t.getName());
                info.put("title", t.getTitle());
                info.put("email", t.getEmail());
                info.put("phone", t.getPhone());
                info.put("researchDirection", profile != null ? profile.getResearchDirection() : "");
                info.put("enrollmentRequirements", profile != null ? profile.getEnrollmentRequirements() : "");
                info.put("avatarPath", t.getAvatarPath());
                info.put("avatarUrl", t.getAvatarPath() != null ? "/avatar/view?path=" + t.getAvatarPath() : null);
                list.add(info);
            }
        }
        return list;
    }

    @GetMapping("/preference")
    @ResponseBody
    public Map<String, Object> getPreference(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Student student = getCurrentStudent(session);
        if (student == null) {
            result.put("error", "学生信息不存在或未绑定");
            return result;
        }
        Integer year = student.getDefenseYear();
        StudentPreference pref = studentPreferenceMapper.findByStudentIdAndYear(student.getId(), year);
        result.put("preference", pref);
        result.put("deadlinePassed", isVolunteerDeadlinePassed());
        return result;
    }

    @PostMapping("/preference")
    @ResponseBody
    public String savePreference(
            @RequestParam(required = false) Long choice1,
            @RequestParam(required = false) Long choice2,
            @RequestParam(required = false) Long choice3,
            @RequestParam(required = false) MultipartFile file,
            HttpSession session) {

        Student student = getCurrentStudent(session);
        if (student == null) {
            return "error:学生信息不存在或未绑定";
        }

        Integer year = student.getDefenseYear();
        if (year == null) {
            return "error:未设置答辩年份";
        }

        try {
            if (isVolunteerDeadlinePassed()) {
                return "error:志愿填报已截止";
            }
            String baseDir = "uploads/volunteer/" + year + "/" + student.getStudentNo();
            Path basePath = Paths.get(baseDir);
            Files.createDirectories(basePath);

            // 学生只需提交一份 PDF，三个志愿共用同一份材料
            String materialPath = savePdfIfPresent(file, basePath, "material.pdf", true);

            StudentPreference pref = studentPreferenceMapper.findByStudentIdAndYear(student.getId(), year);
            if (pref == null) {
                pref = new StudentPreference();
                pref.setStudentId(student.getId());
                pref.setYear(year);
                pref.setStatus(0);
                pref.setAdminAssignType(0);
                pref.setChoice1TeacherId(choice1);
                pref.setChoice2TeacherId(choice2);
                pref.setChoice3TeacherId(choice3);
                if (materialPath != null) {
                    pref.setFile1Path(materialPath);
                    pref.setFile2Path(materialPath);
                    pref.setFile3Path(materialPath);
                }
                studentPreferenceMapper.insert(pref);
            } else {
                pref.setChoice1TeacherId(choice1);
                pref.setChoice2TeacherId(choice2);
                pref.setChoice3TeacherId(choice3);
                if (materialPath != null) {
                    // 新上传的材料同步到三个志愿
                    pref.setFile1Path(materialPath);
                    pref.setFile2Path(materialPath);
                    pref.setFile3Path(materialPath);
                }
                pref.setStatus(0);
                studentPreferenceMapper.update(pref);
            }
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @GetMapping("/result")
    @ResponseBody
    public Map<String, Object> getResult(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Student student = getCurrentStudent(session);
        if (student == null) {
            result.put("error", "学生信息不存在或未绑定");
            return result;
        }
        Integer year = student.getDefenseYear();
        StudentFinalScore score = studentFinalScoreMapper.findByStudentIdAndYear(student.getId(), year);

        Teacher advisor = null;
        if (student.getAdvisorTeacherId() != null) {
            advisor = teacherMapper.findById(student.getAdvisorTeacherId());
        }

        result.put("student", student);
        result.put("advisor", advisor);
        result.put("finalScore", score);
        return result;
    }

    private String savePdfIfPresent(MultipartFile file, Path basePath, String filename, boolean required) throws Exception {
        if (file == null || file.isEmpty()) {
            if (required) {
                throw new RuntimeException("请上传对应志愿的PDF");
            }
            return null;
        }
        if (file.getOriginalFilename() != null && !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            throw new RuntimeException("只允许上传PDF文件");
        }
        Path target = basePath.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toString().replace("\\", "/");
    }

    private boolean isVolunteerDeadlinePassed() {
        String deadline = configService.getConfigValue(ConfigServiceImpl.KEY_VOLUNTEER_DEADLINE);
        if (deadline == null || deadline.trim().isEmpty()) {
            return false;
        }
        try {
            LocalDateTime end = LocalDateTime.parse(deadline.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return LocalDateTime.now().isAfter(end);
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}

