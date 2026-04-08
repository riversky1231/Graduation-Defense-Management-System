package com.example.defensemanagement.controller.score;

import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.ScoreService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpSession;

/**
 * 分数模块共享基类。
 */
public abstract class AbstractScoreController extends AbstractScoreControllerSupport {

    protected final ScoreService scoreService;
    protected final AuthService authService;
    protected final TeacherScoreRecordMapper teacherScoreRecordMapper;
    protected final DefenseGroupTeacherMapper defenseGroupTeacherMapper;

    protected AbstractScoreController(
            ScoreService scoreService,
            AuthService authService,
            TeacherScoreRecordMapper teacherScoreRecordMapper,
            DefenseGroupTeacherMapper defenseGroupTeacherMapper,
            TeacherMapper teacherMapper,
            StudentMapper studentMapper,
            ConfigService configService) {
        super(teacherMapper, studentMapper, configService);
        this.scoreService = scoreService;
        this.authService = authService;
        this.teacherScoreRecordMapper = teacherScoreRecordMapper;
        this.defenseGroupTeacherMapper = defenseGroupTeacherMapper;
    }

    protected User requireSuperAdmin(HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null || !authService.hasPermission(currentUser, "SUPER_ADMIN_ACCESS")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "权限不足");
        }
        return currentUser;
    }

    protected boolean hasSuperAdminAccess(HttpSession session) {
        User currentUser = getCurrentUser(session);
        return currentUser != null && authService.hasPermission(currentUser, "SUPER_ADMIN_ACCESS");
    }

    protected boolean isTeacherScoringOwnGroup(Long teacherId, Long studentId) {
        if (teacherId == null || studentId == null) {
            return false;
        }
        Student student = studentMapper.findById(studentId);
        if (student == null || student.getDefenseGroupId() == null) {
            return false;
        }
        com.example.defensemanagement.entity.DefenseGroupTeacher groupTeacher =
                defenseGroupTeacherMapper.findByTeacherId(teacherId);
        return groupTeacher != null && student.getDefenseGroupId().equals(groupTeacher.getGroupId());
    }
}
