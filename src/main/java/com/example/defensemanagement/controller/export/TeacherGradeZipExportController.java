package com.example.defensemanagement.controller.export;

import com.example.defensemanagement.entity.Student;
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
import java.util.List;

@RestController
@RequestMapping("/export/teacher")
public class TeacherGradeZipExportController extends AbstractTeacherExportController {

    public TeacherGradeZipExportController(
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

    @GetMapping("/grade/zip")
    public ResponseEntity<?> exportTeacherGradeZip(HttpSession session) {
        try {
            Teacher currentTeacher = resolveTeacher(session);
            if (currentTeacher == null) {
                return errorBody(HttpStatus.UNAUTHORIZED, "未登录或不是教师");
            }

            Integer currentYear = configService.getCurrentDefenseYear();
            List<Student> students = filterStudentsWithFinalScore(
                    studentMapper.findByAdvisorIdAndYear(currentTeacher.getId(), currentYear));
            if (students.isEmpty()) {
                return errorBody(HttpStatus.BAD_REQUEST, "没有可导出的学生成绩评定表");
            }

            return zipDocuments(
                    students,
                    this::buildGradeDocument,
                    this::defaultGradeFilename,
                    "教师-" + currentTeacher.getName() + "-成绩评定表.zip");
        } catch (Exception e) {
            return errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "打包导出失败: " + e.getMessage());
        }
    }
}
