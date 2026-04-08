package com.example.defensemanagement.controller.volunteer;

import com.example.defensemanagement.entity.StudentPreference;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.StudentPreferenceMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherProfileMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.StudentService;
import com.example.defensemanagement.service.VolunteerMatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 教师志愿材料下载 Controller。
 * 处理志愿材料访问控制和文件下载。
 */
@RestController
@RequestMapping("/teacher/volunteer")
public class TeacherVolunteerFileController extends AbstractTeacherVolunteerController {

    private static final Logger log = LoggerFactory.getLogger(TeacherVolunteerFileController.class);

    public TeacherVolunteerFileController(
            TeacherMapper teacherMapper,
            StudentPreferenceMapper studentPreferenceMapper,
            StudentMapper studentMapper,
            StudentService studentService,
            ConfigService configService,
            TeacherProfileMapper teacherProfileMapper,
            VolunteerMatchService volunteerMatchService) {
        super(
                teacherMapper,
                studentPreferenceMapper,
                studentMapper,
                studentService,
                configService,
                teacherProfileMapper,
                volunteerMatchService);
    }

    @GetMapping("/file")
    public ResponseEntity<byte[]> downloadVolunteerFile(@RequestParam Long studentId,
                                                        @RequestParam Integer round,
                                                        HttpSession session) {
        Teacher teacher = getCurrentTeacher(session);
        if (teacher == null) {
            return ResponseEntity.status(403).build();
        }

        String roundError = validateRound(round);
        if (roundError != null) {
            return ResponseEntity.badRequest().build();
        }

        Integer year = getCurrentYear();
        StudentPreference preference = studentPreferenceMapper.findByStudentIdAndYear(studentId, year);
        if (preference == null) {
            return ResponseEntity.notFound().build();
        }

        VolunteerRoundSelection selection = resolveSelection(preference, round);
        if (selection.getTeacherId() == null || !selection.getTeacherId().equals(teacher.getId())) {
            return ResponseEntity.status(403).build();
        }
        if (selection.getFilePath() == null || selection.getFilePath().trim().isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!isVolunteerUploadPath(selection.getFilePath())) {
            return ResponseEntity.status(403).build();
        }

        try {
            Path path = Paths.get(selection.getFilePath());
            if (!Files.exists(path)) {
                return ResponseEntity.notFound().build();
            }

            byte[] bytes = Files.readAllBytes(path);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", path.getFileName().toString());
            headers.setContentLength(bytes.length);
            return ResponseEntity.ok().headers(headers).body(bytes);
        } catch (Exception e) {
            log.warn("Failed to download volunteer file, studentId={}, round={}", studentId, round, e);
            return ResponseEntity.status(500).build();
        }
    }
}
