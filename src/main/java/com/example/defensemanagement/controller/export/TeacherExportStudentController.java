package com.example.defensemanagement.controller.export;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.service.ConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/export/teacher")
public class TeacherExportStudentController extends AbstractTeacherExportController {

    public TeacherExportStudentController(
            PaperExportController paperExportController,
            DesignExportController designExportController,
            TeacherMapper teacherMapper,
            StudentMapper studentMapper,
            StudentFinalScoreMapper studentFinalScoreMapper,
            TeacherScoreRecordMapper teacherScoreRecordMapper,
            DefenseGroupTeacherMapper defenseGroupTeacherMapper,
            ConfigService configService) {
        super(
                paperExportController,
                designExportController,
                teacherMapper,
                studentMapper,
                studentFinalScoreMapper,
                teacherScoreRecordMapper,
                defenseGroupTeacherMapper,
                configService);
    }

    @GetMapping("/students")
    public ResponseEntity<?> getTeacherStudents(HttpSession session) {
        try {
            Teacher currentTeacher = resolveTeacher(session);
            if (currentTeacher == null) {
                return errorBody(HttpStatus.UNAUTHORIZED, "未登录或不是教师");
            }

            Integer currentYear = configService.getCurrentDefenseYear();
            List<Map<String, Object>> studentList = studentMapper.findByAdvisorIdAndYear(currentTeacher.getId(), currentYear)
                    .stream()
                    .map(this::studentInfo)
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("students", studentList);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "获取学生列表失败: " + e.getMessage());
        }
    }

    private Map<String, Object> studentInfo(Student student) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", student.getId());
        info.put("name", student.getName());
        info.put("studentNo", student.getStudentNo());
        info.put("title", student.getTitle());
        info.put("defenseType", student.getDefenseType());
        StudentFinalScore finalScore =
                studentFinalScoreMapper.findByStudentIdAndYear(student.getId(), student.getDefenseYear());
        info.put("hasFinalScore", finalScore != null);
        return info;
    }
}
