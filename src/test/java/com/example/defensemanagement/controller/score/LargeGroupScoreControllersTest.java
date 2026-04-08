package com.example.defensemanagement.controller.score;

import com.example.defensemanagement.common.ApiResponse;
import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.ScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LargeGroupScoreControllersTest {

    @Mock
    private ScoreService scoreService;
    @Mock
    private TeacherMapper teacherMapper;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private DefenseGroupMapper defenseGroupMapper;
    @Mock
    private ConfigService configService;

    private LargeGroupCandidateController candidateController;
    private LargeGroupTeacherScoreController teacherScoreController;
    private LargeGroupAdminScoreController adminScoreController;

    @BeforeEach
    void setUp() {
        candidateController = new LargeGroupCandidateController(
                scoreService,
                teacherMapper,
                studentMapper,
                defenseGroupMapper,
                configService);
        teacherScoreController = new LargeGroupTeacherScoreController(
                scoreService,
                teacherMapper,
                studentMapper,
                defenseGroupMapper,
                configService);
        adminScoreController = new LargeGroupAdminScoreController(
                scoreService,
                teacherMapper,
                studentMapper,
                defenseGroupMapper,
                configService);
    }

    @Test
    void getLargeGroupCandidatesReturnsLoginErrorWhenTeacherCannotBeResolved() {
        when(configService.getConfigValue("CURRENT_DEFENSE_YEAR")).thenReturn("2026");

        ApiResponse<Map<String, Object>> result = candidateController.getLargeGroupCandidates(new MockHttpSession());

        assertFalse(result.isSuccess());
        assertEquals("请先登录教师账号", result.getMessage());
    }

    @Test
    void getLargeGroupCandidatesFiltersDepartmentForDeptAdmin() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", user(1L, "DEPT_ADMIN", 8L, "dept"));
        when(configService.getConfigValue("CURRENT_DEFENSE_YEAR")).thenReturn("2026");
        when(scoreService.getLargeGroupCandidates(2026, null)).thenReturn(List.of(
                candidate(1L, 8L),
                candidate(2L, 9L)));

        ApiResponse<Map<String, Object>> result = candidateController.getLargeGroupCandidates(session);

        assertEquals("院系管理员", result.getData().get("teacherName"));
        assertEquals(true, result.getData().get("isDeptAdmin"));
        assertEquals(1, ((List<?>) result.getData().get("candidates")).size());
    }

    @Test
    void getLargeGroupCandidatesResolvesTeacherAndFiltersDepartment() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", user(2L, "TEACHER", 8L, "T008"));
        Teacher teacher = teacher(7L, "李老师", 8L, "T008");
        when(teacherMapper.findByTeacherNo("T008")).thenReturn(teacher);
        when(configService.getConfigValue("CURRENT_DEFENSE_YEAR")).thenReturn("2026");
        when(scoreService.getLargeGroupCandidates(2026, 7L)).thenReturn(List.of(
                candidate(1L, 8L),
                candidate(2L, 9L)));

        ApiResponse<Map<String, Object>> result = candidateController.getLargeGroupCandidates(session);

        assertEquals(7L, result.getData().get("teacherId"));
        assertEquals("李老师", result.getData().get("teacherName"));
        assertEquals(1, ((List<?>) result.getData().get("candidates")).size());
    }

    @Test
    void saveLargeGroupScoreRejectsArchivedScores() {
        MockHttpSession session = teacherSession(7L, "李老师", 8L);
        when(configService.getConfigValue("LARGE_GROUP_ARCHIVED")).thenReturn("1");

        ApiResponse<Void> result = teacherScoreController.saveLargeGroupScore(12L, 90, session);

        assertFalse(result.isSuccess());
        assertEquals("大组成绩已归档，无法再打分", result.getMessage());
    }

    @Test
    void saveLargeGroupScoreRejectsDepartmentMismatch() {
        MockHttpSession session = teacherSession(7L, "李老师", 8L);
        Student student = new Student();
        student.setDefenseGroupId(5L);
        DefenseGroup group = new DefenseGroup();
        group.setDepartmentId(9L);

        when(configService.getConfigValue("LARGE_GROUP_ARCHIVED")).thenReturn("0");
        when(configService.getConfigValue("LARGE_GROUP_DEADLINE")).thenReturn(null);
        when(studentMapper.findById(12L)).thenReturn(student);
        when(defenseGroupMapper.findById(5L)).thenReturn(group);

        ApiResponse<Void> result = teacherScoreController.saveLargeGroupScore(12L, 90, session);

        assertFalse(result.isSuccess());
        assertEquals("您只能给本院系的学生打分", result.getMessage());
    }

    @Test
    void saveLargeGroupScoreDelegatesWhenValid() {
        MockHttpSession session = teacherSession(7L, "李老师", 8L);
        Student student = new Student();
        student.setDefenseGroupId(5L);
        DefenseGroup group = new DefenseGroup();
        group.setDepartmentId(8L);

        when(configService.getConfigValue("LARGE_GROUP_ARCHIVED")).thenReturn("0");
        when(configService.getConfigValue("LARGE_GROUP_DEADLINE")).thenReturn(null);
        when(configService.getConfigValue("CURRENT_DEFENSE_YEAR")).thenReturn("2026");
        when(studentMapper.findById(12L)).thenReturn(student);
        when(defenseGroupMapper.findById(5L)).thenReturn(group);

        ApiResponse<Void> result = teacherScoreController.saveLargeGroupScore(12L, 90, session);

        assertEquals(true, result.isSuccess());
        verify(scoreService).saveLargeGroupScore(12L, 7L, 2026, 90);
    }

    @Test
    void saveLargeGroupScoreRejectsExpiredDeadline() {
        MockHttpSession session = teacherSession(7L, "李老师", 8L);
        String expired = LocalDateTime.now().minusMinutes(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        when(configService.getConfigValue("LARGE_GROUP_ARCHIVED")).thenReturn("0");
        when(configService.getConfigValue("LARGE_GROUP_DEADLINE")).thenReturn(expired);

        ApiResponse<Void> result = teacherScoreController.saveLargeGroupScore(12L, 90, session);

        assertFalse(result.isSuccess());
        assertEquals("大组打分已截止，无法再打分", result.getMessage());
    }

    @Test
    void getLargeGroupStudentScoresRejectsDepartmentMismatch() {
        MockHttpSession session = teacherSession(7L, "李老师", 8L);
        Student student = new Student();
        student.setDefenseGroupId(5L);
        DefenseGroup group = new DefenseGroup();
        group.setDepartmentId(9L);
        when(studentMapper.findById(12L)).thenReturn(student);
        when(defenseGroupMapper.findById(5L)).thenReturn(group);

        ApiResponse<Map<String, Object>> result = teacherScoreController.getLargeGroupStudentScores(12L, session);

        assertFalse(result.isSuccess());
        assertEquals("您只能查看本院系学生的大组评分", result.getMessage());
    }

    @Test
    void getLargeGroupStudentScoresForAdminRejectsNonSuperAdmin() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", user(1L, "DEPT_ADMIN", 8L, "dept"));

        ApiResponse<Map<String, Object>> result = adminScoreController.getLargeGroupStudentScoresForAdmin(12L, session);

        assertEquals("权限不足：只有超级管理员可以查看", result.getMessage());
    }

    @Test
    void getLargeGroupStudentScoresForAdminDelegatesForSuperAdmin() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", user(1L, "SUPER_ADMIN", null, "admin"));
        Map<String, Object> expected = new HashMap<>();
        expected.put("scores", List.of(95, 96));
        when(configService.getConfigValue("CURRENT_DEFENSE_YEAR")).thenReturn("2026");
        when(scoreService.getLargeGroupStudentScoresForAdmin(12L, 2026)).thenReturn(expected);

        ApiResponse<Map<String, Object>> result = adminScoreController.getLargeGroupStudentScoresForAdmin(12L, session);

        assertEquals(expected, result.getData());
    }

    @Test
    void updateLargeGroupScoreValidatesRange() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", user(1L, "SUPER_ADMIN", null, "admin"));
        Map<String, Object> request = new HashMap<>();
        request.put("studentId", 12L);
        request.put("teacherId", 7L);
        request.put("score", 101);

        ApiResponse<Void> result = adminScoreController.updateLargeGroupScore(request, session);

        assertEquals("分数必须在0-100之间", result.getMessage());
    }

    @Test
    void updateLargeGroupScoreDelegatesForSuperAdmin() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", user(1L, "SUPER_ADMIN", null, "admin"));
        Map<String, Object> request = new HashMap<>();
        request.put("scoreId", 3L);
        request.put("studentId", 12L);
        request.put("teacherId", 7L);
        request.put("score", 98);
        when(configService.getConfigValue("CURRENT_DEFENSE_YEAR")).thenReturn("2026");

        ApiResponse<Void> result = adminScoreController.updateLargeGroupScore(request, session);

        assertEquals(true, result.isSuccess());
        verify(scoreService).updateLargeGroupScore(3L, 12L, 7L, 2026, 98);
    }

    private Map<String, Object> candidate(Long studentId, Long departmentId) {
        Map<String, Object> candidate = new HashMap<>();
        candidate.put("studentId", studentId);
        candidate.put("departmentId", departmentId);
        return candidate;
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

    private Teacher teacher(Long id, String name, Long departmentId, String teacherNo) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setName(name);
        teacher.setDepartmentId(departmentId);
        teacher.setTeacherNo(teacherNo);
        return teacher;
    }

    private MockHttpSession teacherSession(Long teacherId, String teacherName, Long departmentId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentTeacher", teacher(teacherId, teacherName, departmentId, "T" + teacherId));
        return session;
    }
}
