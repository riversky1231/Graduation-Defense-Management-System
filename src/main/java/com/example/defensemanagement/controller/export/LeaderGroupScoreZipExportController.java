package com.example.defensemanagement.controller.export;

import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
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
@RequestMapping("/export/leader")
public class LeaderGroupScoreZipExportController extends AbstractLeaderExportController {

    public LeaderGroupScoreZipExportController(
            PaperExportController paperExportController,
            DesignExportController designExportController,
            TeacherMapper teacherMapper,
            StudentMapper studentMapper,
            StudentFinalScoreMapper studentFinalScoreMapper,
            TeacherScoreRecordMapper teacherScoreRecordMapper,
            DefenseGroupTeacherMapper defenseGroupTeacherMapper,
            ConfigService configService,
            DefenseGroupMapper defenseGroupMapper) {
        super(
                paperExportController,
                designExportController,
                teacherMapper,
                studentMapper,
                studentFinalScoreMapper,
                teacherScoreRecordMapper,
                defenseGroupTeacherMapper,
                configService,
                defenseGroupMapper);
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
                return errorBody(HttpStatus.BAD_REQUEST, noLeaderGroupMessage(session));
            }

            List<Student> students = filterStudentsByYear(
                    studentMapper.findByDefenseGroupId(groupId),
                    configService.getCurrentDefenseYear());
            if (students.isEmpty()) {
                return errorBody(HttpStatus.BAD_REQUEST, "小组无学生");
            }

            DefenseGroup group = defenseGroupMapper.findById(groupId);
            String groupName = group != null ? group.getName() : "小组" + groupId;
            return zipDocuments(
                    students,
                    this::buildScoreDocument,
                    this::defaultScoreFilename,
                    "答辩组长-" + currentTeacher.getName() + "-" + groupName + "-答辩成绩表.zip");
        } catch (Exception e) {
            return errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "打包导出失败: " + e.getMessage());
        }
    }
}
