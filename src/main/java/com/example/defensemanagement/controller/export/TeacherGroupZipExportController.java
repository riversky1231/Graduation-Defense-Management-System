package com.example.defensemanagement.controller.export;

import com.example.defensemanagement.entity.DefenseGroupTeacher;
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
public class TeacherGroupZipExportController extends AbstractTeacherExportController {

    public TeacherGroupZipExportController(
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

    @GetMapping("/group/process/zip")
    public ResponseEntity<?> exportTeacherGroupProcessZip(HttpSession session) {
        try {
            Teacher currentTeacher = resolveTeacher(session);
            if (currentTeacher == null) {
                return errorBody(HttpStatus.UNAUTHORIZED, "未登录或不是教师");
            }

            DefenseGroupTeacher groupTeacher = findTeacherGroup(currentTeacher.getId());
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

            List<Student> gradedStudents = filterStudentsScoredByTeacher(
                    filterStudentsByYear(studentMapper.findByDefenseGroupId(groupTeacher.getGroupId()), currentYear),
                    currentTeacher.getId());
            if (gradedStudents.isEmpty()) {
                return errorBody(HttpStatus.BAD_REQUEST, "没有可导出的已评分学生无评语过程表");
            }

            return zipDocuments(
                    gradedStudents,
                    this::buildProcessDocument,
                    this::defaultProcessFilename,
                    "教师-" + currentTeacher.getName() + "-本组已评分学生无评语过程表.zip");
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

            DefenseGroupTeacher groupTeacher = findTeacherGroup(currentTeacher.getId());
            if (groupTeacher == null || groupTeacher.getGroupId() == null) {
                return errorBody(HttpStatus.BAD_REQUEST, "您不在任何答辩小组中");
            }

            Integer currentYear = configService.getCurrentDefenseYear();
            List<Student> gradedStudents = filterStudentsWithFinalScore(
                    filterStudentsScoredByTeacher(
                            filterStudentsByYear(studentMapper.findByDefenseGroupId(groupTeacher.getGroupId()), currentYear),
                            currentTeacher.getId()));
            if (gradedStudents.isEmpty()) {
                return errorBody(HttpStatus.BAD_REQUEST, "没有可导出的已评分学生成绩评定表");
            }

            return zipDocuments(
                    gradedStudents,
                    this::buildGradeDocument,
                    this::defaultGradeFilename,
                    "教师-" + currentTeacher.getName() + "-本组已评分学生成绩评定表.zip");
        } catch (Exception e) {
            return errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "打包导出失败: " + e.getMessage());
        }
    }
}
