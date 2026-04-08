package com.example.defensemanagement.controller.score;

import com.example.defensemanagement.common.ApiResponse;
import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.ScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoreControllersTest {

    @Mock
    private ScoreService scoreService;
    @Mock
    private AuthService authService;
    @Mock
    private TeacherScoreRecordMapper teacherScoreRecordMapper;
    @Mock
    private DefenseGroupTeacherMapper defenseGroupTeacherMapper;
    @Mock
    private TeacherMapper teacherMapper;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private ConfigService configService;

    private ScoreSubmissionController submissionController;
    private ScoreGroupController groupController;
    private ScoreRecordController recordController;

    @BeforeEach
    void setUp() {
        submissionController = new ScoreSubmissionController(
                scoreService,
                authService,
                teacherScoreRecordMapper,
                defenseGroupTeacherMapper,
                teacherMapper,
                studentMapper,
                configService);
        groupController = new ScoreGroupController(
                scoreService,
                authService,
                teacherScoreRecordMapper,
                defenseGroupTeacherMapper,
                teacherMapper,
                studentMapper,
                configService);
        recordController = new ScoreRecordController(
                scoreService,
                authService,
                teacherScoreRecordMapper,
                defenseGroupTeacherMapper,
                teacherMapper,
                studentMapper,
                configService);
    }

    @Test
    void saveTeacherGroupScoreRejectsWhenTeacherMissing() {
        ApiResponse<Map<String, Object>> result = submissionController.saveTeacherGroupScore(
                1L, 90, 10, 20, 30, 40, 50, 60, new MockHttpSession());

        assertFalse(result.isSuccess());
        assertEquals("请先登录教师账号", result.getMessage());
    }

    @Test
    void saveTeacherGroupScoreRejectsWhenTeacherOutsideGroup() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentTeacher", teacher(7L, "T007", "李老师"));
        Student student = new Student();
        student.setId(12L);
        student.setDefenseGroupId(99L);
        when(studentMapper.findById(12L)).thenReturn(student);
        when(defenseGroupTeacherMapper.findByTeacherId(7L)).thenReturn(groupTeacher(88L, 7L));

        ApiResponse<Map<String, Object>> result = submissionController.saveTeacherGroupScore(
                12L, 88, 11, 12, 13, 14, 15, 16, session);

        assertFalse(result.isSuccess());
        assertEquals("您只能给本答辩小组的学生打分", result.getMessage());
        verifyNoInteractions(scoreService);
    }

    @Test
    void saveTeacherGroupScoreBuildsRecordWithResolvedTeacherYearAndGroup() {
        MockHttpSession session = new MockHttpSession();
        Teacher teacher = teacher(7L, "T007", "李老师");
        session.setAttribute("currentTeacher", teacher);
        Student student = new Student();
        student.setId(12L);
        student.setDefenseGroupId(99L);
        when(configService.getConfigValue("CURRENT_DEFENSE_YEAR")).thenReturn("2026");
        when(studentMapper.findById(12L)).thenReturn(student);
        when(defenseGroupTeacherMapper.findByTeacherId(7L)).thenReturn(groupTeacher(99L, 7L));

        ApiResponse<Map<String, Object>> result = submissionController.saveTeacherGroupScore(
                12L, 88, 11, 12, 13, 14, 15, 16, session);

        assertEquals(true, result.isSuccess());
        ArgumentCaptor<TeacherScoreRecord> captor = ArgumentCaptor.forClass(TeacherScoreRecord.class);
        verify(scoreService).saveTeacherScore(captor.capture());
        TeacherScoreRecord record = captor.getValue();
        assertEquals(12L, record.getStudentId());
        assertEquals(7L, record.getTeacherId());
        assertEquals(2026, record.getYear());
        assertEquals(99L, record.getDefenseGroupId());
        assertEquals(88, record.getTotalScore());
        assertEquals(16, record.getItem6Score());
    }

    @Test
    void saveTeacherScoreUsesTeacherFromSessionInsteadOfRequestBody() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentTeacher", teacher(7L, "T007", "李老师"));
        Student student = new Student();
        student.setId(12L);
        student.setDefenseGroupId(99L);
        when(configService.getConfigValue("CURRENT_DEFENSE_YEAR")).thenReturn("2026");
        when(studentMapper.findById(12L)).thenReturn(student);
        when(defenseGroupTeacherMapper.findByTeacherId(7L)).thenReturn(groupTeacher(99L, 7L));

        TeacherScoreRecord body = new TeacherScoreRecord();
        body.setStudentId(12L);
        body.setTeacherId(88L);
        body.setYear(2030);
        body.setTotalScore(91);

        ApiResponse<Map<String, Object>> result = submissionController.saveTeacherScore(body, session);

        assertEquals(true, result.isSuccess());
        ArgumentCaptor<TeacherScoreRecord> captor = ArgumentCaptor.forClass(TeacherScoreRecord.class);
        verify(scoreService).saveTeacherScore(captor.capture());
        assertEquals(7L, captor.getValue().getTeacherId());
        assertEquals(2026, captor.getValue().getYear());
        assertEquals(99L, captor.getValue().getDefenseGroupId());
    }

    @Test
    void autoSplitItemsRequiresLogin() {
        ApiResponse<Map<String, Integer>> result = submissionController.autoSplitItems("PAPER", 85, new MockHttpSession());

        assertFalse(result.isSuccess());
        assertEquals("未登录", result.getMessage());
    }

    @Test
    void getTeacherGroupStudentsFiltersDepartmentForDeptAdmin() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", user(1L, "DEPT_ADMIN", 8L, "deptAdmin"));
        when(configService.getConfigValue("CURRENT_DEFENSE_YEAR")).thenReturn("2026");
        Map<String, Object> allData = new HashMap<>();
        allData.put("groups", List.of(
                group(1L, 8L),
                group(2L, 9L)));
        when(scoreService.getAllGroupStudentsForSuperAdmin(2026)).thenReturn(allData);

        ApiResponse<Map<String, Object>> result = groupController.getTeacherGroupStudents(session);

        assertEquals(true, result.isSuccess());
        assertEquals(2026, result.getData().get("year"));
        assertEquals("院系管理员", result.getData().get("teacherName"));
        assertEquals(true, result.getData().get("isDeptAdmin"));
        assertEquals(1, ((List<?>) result.getData().get("groups")).size());
    }

    @Test
    void getCurrentTeacherResolvesFromCurrentUser() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", user(3L, "TEACHER", 1L, "T003"));
        Teacher teacher = teacher(3L, "T003", "王老师");
        when(teacherMapper.findByTeacherNo("T003")).thenReturn(teacher);

        ApiResponse<Map<String, Object>> result = submissionController.getCurrentTeacher(session);

        assertEquals(3L, result.getData().get("teacherId"));
        assertEquals("T003", result.getData().get("teacherNo"));
        assertEquals("王老师", result.getData().get("teacherName"));
    }

    @Test
    void finalizeGroupDelegatesToService() {
        ApiResponse<Void> result = groupController.finalizeGroup(9L, 2026, 91);

        assertEquals(true, result.isSuccess());
        verify(scoreService).finalizeGroupScores(9L, 2026, 91);
    }

    @Test
    void getAllScoreRecordsReturnsErrorWithoutPermission() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", user(1L, "DEPT_ADMIN", 8L, "deptAdmin"));
        when(authService.hasPermission(any(User.class), eq("SUPER_ADMIN_ACCESS"))).thenReturn(false);

        ApiResponse<List<TeacherScoreRecord>> result = recordController.getAllScoreRecords(2026, session);

        assertFalse(result.isSuccess());
        assertEquals("权限不足", result.getMessage());
    }

    @Test
    void updateScoreRecordReturnsSuccessForSuperAdmin() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", user(1L, "SUPER_ADMIN", null, "admin"));
        TeacherScoreRecord record = new TeacherScoreRecord();
        when(authService.hasPermission(any(User.class), eq("SUPER_ADMIN_ACCESS"))).thenReturn(true);
        when(teacherScoreRecordMapper.update(record)).thenReturn(1);

        ApiResponse<Void> result = recordController.updateScoreRecord(record, session);

        assertEquals(true, result.isSuccess());
    }

    @Test
    void getScoreRecordByIdReturnsMapperResultForSuperAdmin() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", user(1L, "SUPER_ADMIN", null, "admin"));
        TeacherScoreRecord record = new TeacherScoreRecord();
        record.setId(55L);
        when(authService.hasPermission(any(User.class), eq("SUPER_ADMIN_ACCESS"))).thenReturn(true);
        when(teacherScoreRecordMapper.findById(55L)).thenReturn(record);

        ApiResponse<TeacherScoreRecord> result = recordController.getScoreRecordById(55L, session);

        assertSame(record, result.getData());
    }

    private User user(Long id, String roleName, Long departmentId, String username) {
        User user = new User();
        user.setId(id);
        user.setDepartmentId(departmentId);
        user.setUsername(username);
        Role role = new Role();
        role.setName(roleName);
        user.setRole(role);
        return user;
    }

    private Teacher teacher(Long id, String teacherNo, String name) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setTeacherNo(teacherNo);
        teacher.setName(name);
        return teacher;
    }

    private Map<String, Object> group(Long id, Long departmentId) {
        Map<String, Object> result = new HashMap<>();
        result.put("groupId", id);
        result.put("departmentId", departmentId);
        return result;
    }

    private DefenseGroupTeacher groupTeacher(Long groupId, Long teacherId) {
        DefenseGroupTeacher groupTeacher = new DefenseGroupTeacher();
        groupTeacher.setGroupId(groupId);
        groupTeacher.setTeacherId(teacherId);
        return groupTeacher;
    }
}
