package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/export/leader")
public class LeaderExportController {

    @Autowired
    private ExportController exportController;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private DefenseGroupTeacherMapper defenseGroupTeacherMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private StudentFinalScoreMapper studentFinalScoreMapper;

    @Autowired
    private ConfigService configService;

    @Autowired
    private DefenseGroupMapper defenseGroupMapper;

    @GetMapping("/students")
    public ResponseEntity<?> getLeaderStudents(HttpSession session) {
        try {
            Teacher currentTeacher = resolveLeaderTeacher(session);
            if (currentTeacher == null) {
                return errorBody(HttpStatus.UNAUTHORIZED, "未登录或不是教师");
            }

            Long groupId = findLeaderGroupId(currentTeacher.getId());
            if (groupId == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("students", new java.util.ArrayList<>());
                User currentUser = (User) session.getAttribute("currentUser");
                boolean isDefenseLeaderRole = currentUser != null
                        && currentUser.getRole() != null
                        && "DEFENSE_LEADER".equals(currentUser.getRole().getName());
                result.put("message", isDefenseLeaderRole
                        ? "您已被设置为答辩组长，但尚未被分配到任何答辩小组，请联系管理员"
                        : "您不是任何小组的组长");
                return ResponseEntity.ok(result);
            }

            List<Student> students = studentMapper.findByDefenseGroupId(groupId);
            Integer currentYear = configService.getCurrentDefenseYear();
            if (currentYear != null) {
                students = students.stream()
                        .filter(s -> currentYear.equals(s.getDefenseYear()))
                        .collect(java.util.stream.Collectors.toList());
            }

            List<Map<String, Object>> studentList = students.stream().map(s -> {
                Map<String, Object> info = new HashMap<>();
                info.put("id", s.getId());
                info.put("name", s.getName());
                info.put("studentNo", s.getStudentNo());
                info.put("title", s.getTitle());
                info.put("defenseType", s.getDefenseType());
                StudentFinalScore fs = studentFinalScoreMapper.findByStudentIdAndYear(s.getId(), s.getDefenseYear());
                info.put("hasFinalScore", fs != null);
                return info;
            }).collect(java.util.stream.Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("students", studentList);
            result.put("groupId", groupId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "获取学生列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/group/score/zip")
    public ResponseEntity<?> exportLeaderGroupScoreZip(HttpSession session) {
        try {
            Teacher currentTeacher = resolveLeaderTeacher(session);
            if (currentTeacher == null) {
                return errorBody(HttpStatus.UNAUTHORIZED, "未登录或不是教师");
            }

            Long groupId = findLeaderGroupId(currentTeacher.getId());
            if (groupId == null) {
                User currentUser = (User) session.getAttribute("currentUser");
                boolean isDefenseLeaderRole = currentUser != null
                        && currentUser.getRole() != null
                        && "DEFENSE_LEADER".equals(currentUser.getRole().getName());
                return errorBody(HttpStatus.BAD_REQUEST, isDefenseLeaderRole
                        ? "您已被设置为答辩组长，但尚未被分配到任何答辩小组，请联系管理员"
                        : "您不是任何小组的组长");
            }

            List<Student> students = studentMapper.findByDefenseGroupId(groupId);
            Integer currentYear = configService.getCurrentDefenseYear();
            if (currentYear != null) {
                final Integer year = currentYear;
                students = students.stream()
                        .filter(s -> year.equals(s.getDefenseYear()))
                        .collect(java.util.stream.Collectors.toList());
            }
            if (students == null || students.isEmpty()) {
                return errorBody(HttpStatus.BAD_REQUEST, "小组无学生");
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ZipOutputStream zos = new ZipOutputStream(baos)) {
                for (Student stu : students) {
                    boolean isPaper = "PAPER".equalsIgnoreCase(stu.getDefenseType());
                    ResponseEntity<byte[]> resp = exportController.buildScoreDoc(stu.getId(), isPaper);
                    String cleanName = extractFilename(resp,
                            (isPaper ? "本科毕业论文答辩成绩表-" : "本科毕业设计答辩成绩表-") + stu.getName() + ".docx");
                    zos.putNextEntry(new ZipEntry(cleanName));
                    zos.write(resp.getBody());
                    zos.closeEntry();
                }
                zos.finish();
                byte[] zipBytes = baos.toByteArray();
                com.example.defensemanagement.entity.DefenseGroup group = defenseGroupMapper.findById(groupId);
                String groupName = group != null ? group.getName() : "小组" + groupId;
                MediaType octet = MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + exportController.encode("答辩组长-" + currentTeacher.getName() + "-" + groupName + "-答辩成绩表.zip") + "\"")
                        .contentType(octet)
                        .body(zipBytes);
            }
        } catch (Exception e) {
            return errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "打包导出失败: " + e.getMessage());
        }
    }

    private Teacher resolveLeaderTeacher(HttpSession session) {
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
        if (currentTeacher != null) {
            return currentTeacher;
        }
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null && currentUser.getRole() != null
                && ("TEACHER".equals(currentUser.getRole().getName()) || "DEFENSE_LEADER".equals(currentUser.getRole().getName()))) {
            return teacherMapper.findByUserId(currentUser.getId());
        }
        return null;
    }

    private Long findLeaderGroupId(Long teacherId) {
        List<DefenseGroupTeacher> allGroups = defenseGroupTeacherMapper.findAll();
        for (DefenseGroupTeacher gt : allGroups) {
            if (gt.getTeacherId() != null && gt.getTeacherId().equals(teacherId)
                    && gt.getIsLeader() != null && gt.getIsLeader() == 1) {
                return gt.getGroupId();
            }
        }
        return null;
    }

    private ResponseEntity<Map<String, Object>> errorBody(HttpStatus status, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        return ResponseEntity.status(status).body(error);
    }

    private String extractFilename(ResponseEntity<byte[]> response, String fallback) {
        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        if (disposition != null && disposition.contains("filename=\"")) {
            int idx = disposition.indexOf("filename=\"") + 10;
            int end = disposition.indexOf("\"", idx);
            if (end > idx) {
                return disposition.substring(idx, end);
            }
        }
        return fallback;
    }
}
