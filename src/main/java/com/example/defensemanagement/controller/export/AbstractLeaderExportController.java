package com.example.defensemanagement.controller.export;

import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.service.ConfigService;

import javax.servlet.http.HttpSession;
import java.util.List;

abstract class AbstractLeaderExportController extends AbstractTeacherExportController {

    protected final DefenseGroupMapper defenseGroupMapper;

    protected AbstractLeaderExportController(
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
                configService);
        this.defenseGroupMapper = defenseGroupMapper;
    }

    protected Teacher resolveLeaderTeacher(HttpSession session) {
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
        if (currentTeacher != null) {
            return currentTeacher;
        }
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null
                && currentUser.getRole() != null
                && ("TEACHER".equals(currentUser.getRole().getName())
                || "DEFENSE_LEADER".equals(currentUser.getRole().getName()))) {
            return teacherMapper.findByUserId(currentUser.getId());
        }
        return null;
    }

    protected Long findLeaderGroupId(Long teacherId) {
        List<DefenseGroupTeacher> groups = defenseGroupTeacherMapper.findAll();
        for (DefenseGroupTeacher group : groups) {
            if (group.getTeacherId() != null
                    && group.getTeacherId().equals(teacherId)
                    && group.getIsLeader() != null
                    && group.getIsLeader() == 1) {
                return group.getGroupId();
            }
        }
        return null;
    }

    protected String noLeaderGroupMessage(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        boolean isDefenseLeaderRole = currentUser != null
                && currentUser.getRole() != null
                && "DEFENSE_LEADER".equals(currentUser.getRole().getName());
        return isDefenseLeaderRole
                ? "您已被设置为答辩组长，但尚未被分配到任何答辩小组，请联系管理员"
                : "您不是任何小组的组长";
    }
}
