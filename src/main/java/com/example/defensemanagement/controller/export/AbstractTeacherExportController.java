package com.example.defensemanagement.controller.export;

import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.service.ConfigService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

abstract class AbstractTeacherExportController {

    protected final PaperExportController paperExportController;
    protected final DesignExportController designExportController;
    protected final TeacherMapper teacherMapper;
    protected final StudentMapper studentMapper;
    protected final StudentFinalScoreMapper studentFinalScoreMapper;
    protected final TeacherScoreRecordMapper teacherScoreRecordMapper;
    protected final DefenseGroupTeacherMapper defenseGroupTeacherMapper;
    protected final ConfigService configService;

    protected AbstractTeacherExportController(
            PaperExportController paperExportController,
            DesignExportController designExportController,
            TeacherMapper teacherMapper,
            StudentMapper studentMapper,
            StudentFinalScoreMapper studentFinalScoreMapper,
            TeacherScoreRecordMapper teacherScoreRecordMapper,
            DefenseGroupTeacherMapper defenseGroupTeacherMapper,
            ConfigService configService) {
        this.paperExportController = paperExportController;
        this.designExportController = designExportController;
        this.teacherMapper = teacherMapper;
        this.studentMapper = studentMapper;
        this.studentFinalScoreMapper = studentFinalScoreMapper;
        this.teacherScoreRecordMapper = teacherScoreRecordMapper;
        this.defenseGroupTeacherMapper = defenseGroupTeacherMapper;
        this.configService = configService;
    }

    protected Teacher resolveTeacher(HttpSession session) {
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
        if (currentTeacher != null) {
            return currentTeacher;
        }
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null
                && currentUser.getRole() != null
                && "TEACHER".equals(currentUser.getRole().getName())) {
            return teacherMapper.findByUserId(currentUser.getId());
        }
        return null;
    }

    protected List<Student> filterStudentsByYear(List<Student> students, Integer year) {
        if (year == null) {
            return students;
        }
        return students.stream()
                .filter(student -> year.equals(student.getDefenseYear()))
                .collect(Collectors.toList());
    }

    protected List<Student> filterStudentsWithFinalScore(List<Student> students) {
        return students.stream()
                .filter(this::hasFinalScore)
                .collect(Collectors.toList());
    }

    protected List<Student> filterStudentsScoredByTeacher(List<Student> students, Long teacherId) {
        return students.stream()
                .filter(student -> hasTeacherScore(student, teacherId))
                .collect(Collectors.toList());
    }

    protected DefenseGroupTeacher findTeacherGroup(Long teacherId) {
        return defenseGroupTeacherMapper.findByTeacherId(teacherId);
    }

    protected ResponseEntity<byte[]> buildGradeDocument(Student student) {
        return isPaper(student)
                ? paperExportController.buildGradeDoc(student.getId())
                : designExportController.buildGradeDoc(student.getId());
    }

    protected ResponseEntity<byte[]> buildScoreDocument(Student student) {
        return isPaper(student)
                ? paperExportController.buildScoreDoc(student.getId())
                : designExportController.buildScoreDoc(student.getId());
    }

    protected ResponseEntity<byte[]> buildProcessDocument(Student student) {
        return isPaper(student)
                ? paperExportController.buildProcessDoc(student.getId())
                : designExportController.buildProcessDoc(student.getId());
    }

    protected String defaultGradeFilename(Student student) {
        return (isPaper(student) ? "本科毕业论文成绩评定表-" : "本科毕业设计成绩评定表-")
                + student.getName()
                + ".docx";
    }

    protected String defaultScoreFilename(Student student) {
        return (isPaper(student) ? "本科毕业论文答辩成绩表-" : "本科毕业设计答辩成绩表-")
                + student.getName()
                + ".docx";
    }

    protected String defaultProcessFilename(Student student) {
        return (isPaper(student) ? "毕业论文答辩成绩无评语过程表-" : "毕业设计答辩成绩无评语过程表-")
                + student.getName()
                + ".docx";
    }

    protected ResponseEntity<byte[]> zipDocuments(
            List<Student> students,
            Function<Student, ResponseEntity<byte[]>> exporter,
            Function<Student, String> fallbackFilename,
            String archiveFilename) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Student student : students) {
                ResponseEntity<byte[]> response = exporter.apply(student);
                zos.putNextEntry(new ZipEntry(extractFilename(response, fallbackFilename.apply(student))));
                zos.write(response.getBody());
                zos.closeEntry();
            }
            zos.finish();
            MediaType octet = MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + paperExportController.encode(archiveFilename) + "\"")
                    .contentType(octet)
                    .body(baos.toByteArray());
        }
    }

    protected ResponseEntity<Map<String, Object>> errorBody(HttpStatus status, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        return ResponseEntity.status(status).body(error);
    }

    protected String extractFilename(ResponseEntity<byte[]> response, String fallback) {
        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        if (disposition != null && disposition.contains("filename=\"")) {
            int index = disposition.indexOf("filename=\"") + 10;
            int end = disposition.indexOf("\"", index);
            if (end > index) {
                return disposition.substring(index, end);
            }
        }
        return fallback;
    }

    private boolean hasFinalScore(Student student) {
        return studentFinalScoreMapper.findByStudentIdAndYear(student.getId(), student.getDefenseYear()) != null;
    }

    private boolean hasTeacherScore(Student student, Long teacherId) {
        List<TeacherScoreRecord> records =
                teacherScoreRecordMapper.findByStudentIdAndYear(student.getId(), student.getDefenseYear());
        return records != null
                && records.stream()
                .anyMatch(record -> record.getTeacherId() != null && record.getTeacherId().equals(teacherId));
    }

    private boolean isPaper(Student student) {
        return "PAPER".equalsIgnoreCase(student.getDefenseType());
    }
}
