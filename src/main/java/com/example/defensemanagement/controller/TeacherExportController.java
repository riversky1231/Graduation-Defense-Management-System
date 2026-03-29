package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
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
@RequestMapping("/export/teacher")
public class TeacherExportController {

    @Autowired
    private ExportController exportController;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private StudentFinalScoreMapper studentFinalScoreMapper;

    @Autowired
    private TeacherScoreRecordMapper teacherScoreRecordMapper;

    @Autowired
    private DefenseGroupTeacherMapper defenseGroupTeacherMapper;

    @Autowired
    private ConfigService configService;

    @GetMapping("/students")
    public ResponseEntity<?> getTeacherStudents(HttpSession session) {
        try {
            Teacher currentTeacher = resolveTeacher(session);
            if (currentTeacher == null) {
                return errorBody(HttpStatus.UNAUTHORIZED, "未登录或不是教师");
            }

            Integer currentYear = configService.getCurrentDefenseYear();
            List<Student> students = studentMapper.findByAdvisorIdAndYear(currentTeacher.getId(), currentYear);
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
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "获取学生列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/grade/zip")
    public ResponseEntity<?> exportTeacherGradeZip(HttpSession session) {
        try {
            Teacher currentTeacher = resolveTeacher(session);
            if (currentTeacher == null) {
                return errorBody(HttpStatus.UNAUTHORIZED, "未登录或不是教师");
            }

            Integer currentYear = configService.getCurrentDefenseYear();
            List<Student> students = studentMapper.findByAdvisorIdAndYear(currentTeacher.getId(), currentYear)
                    .stream()
                    .filter(s -> studentFinalScoreMapper.findByStudentIdAndYear(s.getId(), s.getDefenseYear()) != null)
                    .collect(java.util.stream.Collectors.toList());

            if (students.isEmpty()) {
                return errorBody(HttpStatus.BAD_REQUEST, "没有可导出的学生成绩评定表");
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ZipOutputStream zos = new ZipOutputStream(baos)) {
                for (Student stu : students) {
                    boolean isPaper = "PAPER".equalsIgnoreCase(stu.getDefenseType());
                    ResponseEntity<byte[]> resp = exportController.buildGradeDoc(stu.getId(), isPaper);
                    String fallback = (isPaper ? "本科毕业论文成绩评定表-" : "本科毕业设计成绩评定表-") + stu.getName() + ".docx";
                    zos.putNextEntry(new ZipEntry(extractFilename(resp, fallback)));
                    zos.write(resp.getBody());
                    zos.closeEntry();
                }
                zos.finish();
                byte[] zipBytes = baos.toByteArray();
                MediaType octet = MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + exportController.encode("教师-" + currentTeacher.getName() + "-成绩评定表.zip") + "\"")
                        .contentType(octet)
                        .body(zipBytes);
            }
        } catch (Exception e) {
            return errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "打包导出失败: " + e.getMessage());
        }
    }

    @GetMapping("/group/process/zip")
    public ResponseEntity<?> exportTeacherGroupProcessZip(HttpSession session) {
        try {
            Teacher currentTeacher = resolveTeacher(session);
            if (currentTeacher == null) {
                return errorBody(HttpStatus.UNAUTHORIZED, "未登录或不是教师");
            }

            DefenseGroupTeacher groupTeacher = defenseGroupTeacherMapper.findByTeacherId(currentTeacher.getId());
            if (groupTeacher != null && groupTeacher.getIsLeader() != null && groupTeacher.getIsLeader() == 1) {
                return errorBody(HttpStatus.FORBIDDEN, "答辩组长不能使用此功能");
            }
            if (groupTeacher == null || groupTeacher.getGroupId() == null) {
                return errorBody(HttpStatus.BAD_REQUEST, "您不在任何答辩小组中");
            }

            Integer currentYear = configService.getCurrentDefenseYear();
            if (currentYear == null) {
                return errorBody(HttpStatus.BAD_REQUEST, "请先设置当前答辩年份");
            }

            final Long teacherId = currentTeacher.getId();
            final Integer year = currentYear;
            List<Student> gradedStudents = studentMapper.findByDefenseGroupId(groupTeacher.getGroupId()).stream()
                    .filter(s -> year.equals(s.getDefenseYear()))
                    .filter(s -> {
                        List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(s.getId(), s.getDefenseYear());
                        return records != null && records.stream().anyMatch(r -> r.getTeacherId() != null && r.getTeacherId().equals(teacherId));
                    })
                    .collect(java.util.stream.Collectors.toList());

            if (gradedStudents.isEmpty()) {
                return errorBody(HttpStatus.BAD_REQUEST, "没有可导出的已评分学生无评语过程表");
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ZipOutputStream zos = new ZipOutputStream(baos)) {
                for (Student stu : gradedStudents) {
                    boolean isPaper = "PAPER".equalsIgnoreCase(stu.getDefenseType());
                    ResponseEntity<byte[]> resp = exportController.buildProcessDoc(stu.getId(), isPaper);
                    String fallback = (isPaper ? "毕业论文答辩成绩无评语过程表-" : "毕业设计答辩成绩无评语过程表-") + stu.getName() + ".docx";
                    zos.putNextEntry(new ZipEntry(extractFilename(resp, fallback)));
                    zos.write(resp.getBody());
                    zos.closeEntry();
                }
                zos.finish();
                byte[] zipBytes = baos.toByteArray();
                MediaType octet = MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + exportController.encode("教师-" + currentTeacher.getName() + "-本组已评分学生无评语过程表.zip") + "\"")
                        .contentType(octet)
                        .body(zipBytes);
            }
        } catch (Exception e) {
            return errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "打包导出失败: " + e.getMessage());
        }
    }

    @GetMapping("/group/graded/zip")
    public ResponseEntity<?> exportTeacherGradedGroupStudentsZip(HttpSession session) {
        try {
            Teacher currentTeacher = resolveTeacher(session);
            if (currentTeacher == null) {
                return errorBody(HttpStatus.UNAUTHORIZED, "未登录或不是教师");
            }

            final Long teacherId = currentTeacher.getId();
            DefenseGroupTeacher groupTeacher = defenseGroupTeacherMapper.findByTeacherId(teacherId);
            if (groupTeacher == null || groupTeacher.getGroupId() == null) {
                return errorBody(HttpStatus.BAD_REQUEST, "您不在任何答辩小组中");
            }

            Integer currentYear = configService.getCurrentDefenseYear();
            List<Student> groupStudents = studentMapper.findByDefenseGroupId(groupTeacher.getGroupId());
            if (currentYear != null) {
                final Integer year = currentYear;
                groupStudents = groupStudents.stream().filter(s -> year.equals(s.getDefenseYear()))
                        .collect(java.util.stream.Collectors.toList());
            }

            List<Student> gradedStudents = groupStudents.stream()
                    .filter(s -> {
                        List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(s.getId(), s.getDefenseYear());
                        return records != null && records.stream().anyMatch(r -> r.getTeacherId() != null && r.getTeacherId().equals(teacherId));
                    })
                    .filter(s -> studentFinalScoreMapper.findByStudentIdAndYear(s.getId(), s.getDefenseYear()) != null)
                    .collect(java.util.stream.Collectors.toList());

            if (gradedStudents.isEmpty()) {
                return errorBody(HttpStatus.BAD_REQUEST, "没有可导出的已评分学生成绩评定表");
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ZipOutputStream zos = new ZipOutputStream(baos)) {
                for (Student stu : gradedStudents) {
                    boolean isPaper = "PAPER".equalsIgnoreCase(stu.getDefenseType());
                    ResponseEntity<byte[]> resp = exportController.buildGradeDoc(stu.getId(), isPaper);
                    String fallback = (isPaper ? "本科毕业论文成绩评定表-" : "本科毕业设计成绩评定表-") + stu.getName() + ".docx";
                    zos.putNextEntry(new ZipEntry(extractFilename(resp, fallback)));
                    zos.write(resp.getBody());
                    zos.closeEntry();
                }
                zos.finish();
                byte[] zipBytes = baos.toByteArray();
                MediaType octet = MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + exportController.encode("教师-" + currentTeacher.getName() + "-本组已评分学生成绩评定表.zip") + "\"")
                        .contentType(octet)
                        .body(zipBytes);
            }
        } catch (Exception e) {
            return errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "打包导出失败: " + e.getMessage());
        }
    }

    private Teacher resolveTeacher(HttpSession session) {
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
        if (currentTeacher != null) {
            return currentTeacher;
        }
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null && currentUser.getRole() != null && "TEACHER".equals(currentUser.getRole().getName())) {
            return teacherMapper.findByUserId(currentUser.getId());
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
