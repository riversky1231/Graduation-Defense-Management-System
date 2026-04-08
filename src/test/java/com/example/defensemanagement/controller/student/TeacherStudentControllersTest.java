package com.example.defensemanagement.controller.student;

import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.ScoreService;
import com.example.defensemanagement.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherStudentControllersTest {

    @Mock
    private StudentService studentService;

    @Mock
    private ConfigService configService;

    @Mock
    private TeacherMapper teacherMapper;

    @Mock
    private StudentFinalScoreMapper studentFinalScoreMapper;

    @Mock
    private ScoreService scoreService;

    @Mock
    private TeacherScoreRecordMapper teacherScoreRecordMapper;

    private TeacherStudentQueryController queryController;
    private TeacherStudentAdvisorController advisorController;
    private TeacherStudentScoreController scoreController;

    @BeforeEach
    void setUp() {
        queryController = new TeacherStudentQueryController(
                studentService, configService, teacherMapper, studentFinalScoreMapper, scoreService, teacherScoreRecordMapper);
        advisorController = new TeacherStudentAdvisorController(
                studentService, configService, teacherMapper, studentFinalScoreMapper, scoreService, teacherScoreRecordMapper);
        scoreController = new TeacherStudentScoreController(
                studentService, configService, teacherMapper, studentFinalScoreMapper, scoreService, teacherScoreRecordMapper);
    }

    @Test
    void advisedStudentsRequireTeacherWhenNotSuperAdmin() {
        MockHttpSession session = userSession("TEACHER", 10L, "T001", 2L);
        when(teacherMapper.findByUserId(10L)).thenReturn(null);
        when(teacherMapper.findByTeacherNo("T001")).thenReturn(null);

        Map<String, Object> result = queryController.getAdvisedStudentsWithScores(session);

        assertEquals("请先登录教师账号", result.get("error"));
    }

    @Test
    void advisedStudentsForTeacherBuildScoresFromRecordsAndFinalScores() {
        Teacher teacher = teacher(7L, "李老师", 2L);
        Student student = student(1L, 7L, 9L);
        StudentFinalScore finalScore = new StudentFinalScore();
        finalScore.setStudentId(1L);
        finalScore.setReviewerScore(88);
        finalScore.setFinalDefenseScore(85.5);
        finalScore.setTotalGrade(86.0);
        TeacherScoreRecord advisorRecord = new TeacherScoreRecord();
        advisorRecord.setTotalScore(90);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentTeacher", teacher);
        session.setAttribute("currentUser", user("TEACHER", 10L, "T001", 2L));

        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(studentService.getStudentsByAdvisor(7L, 2026)).thenReturn(List.of(student));
        when(studentFinalScoreMapper.findByStudentIdsAndYear(List.of(1L), 2026)).thenReturn(List.of(finalScore));
        when(teacherScoreRecordMapper.findByStudentIdAndTeacherIdAndYear(1L, 7L, 2026)).thenReturn(advisorRecord);
        when(teacherScoreRecordMapper.findByStudentIdAndTeacherIdAndYear(1L, 9L, 2026)).thenReturn(null);

        Map<String, Object> result = queryController.getAdvisedStudentsWithScores(session);

        assertEquals(7L, result.get("teacherId"));
        assertEquals("李老师", result.get("teacherName"));
        assertEquals(2026, result.get("year"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> students = (List<Map<String, Object>>) result.get("students");
        assertEquals(1, students.size());
        assertEquals(90, students.get(0).get("advisorScore"));
        assertEquals(88, students.get(0).get("reviewerScore"));
        assertEquals("评阅教师", students.get(0).get("reviewerName"));
        assertEquals(85.5, students.get(0).get("finalDefenseScore"));
    }

    @Test
    void reviewerCandidatesExcludeCurrentTeacher() {
        Teacher currentTeacher = teacher(7L, "李老师", 2L);
        Teacher reviewer = teacher(9L, "王老师", 2L);
        reviewer.setTeacherNo("T009");
        reviewer.setTitle("教授");

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentTeacher", currentTeacher);
        when(teacherMapper.findByDepartmentId(2L)).thenReturn(List.of(currentTeacher, reviewer));

        List<Map<String, Object>> result = queryController.getReviewerCandidates(session);

        assertEquals(1, result.size());
        assertEquals(9L, result.get(0).get("id"));
        assertEquals("王老师", result.get(0).get("name"));
    }

    @Test
    void updateStudentInfoRejectsOverlongTitle() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentTeacher", teacher(7L, "李老师", 2L));

        String result = advisorController.updateStudentInfoByTeacher(
                1L,
                "x".repeat(201),
                "summary",
                session);

        assertEquals("error:题目不能超过200个字符", result);
        verify(studentService, never()).saveStudent(any(Student.class));
    }

    @Test
    void setAdvisorScoreRejectsNonAdvisorTeacher() {
        Student student = student(1L, 7L, 9L);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentTeacher", teacher(8L, "张老师", 2L));
        session.setAttribute("currentUser", user("TEACHER", 10L, "T001", 2L));

        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(studentService.findById(1L)).thenReturn(student);

        String result = scoreController.setAdvisorScoreByTeacher(1L, 90, session);

        assertEquals("error:您不是该学生的指导教师", result);
    }

    @Test
    void getTeacherScoreRecordRejectsInvalidType() {
        Student student = student(1L, 7L, 9L);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentTeacher", teacher(7L, "李老师", 2L));
        session.setAttribute("currentUser", user("TEACHER", 10L, "T001", 2L));

        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(studentService.findById(1L)).thenReturn(student);

        Map<String, Object> result = scoreController.getTeacherScoreRecord(1L, "other", session);

        assertEquals("类型参数错误，应为advisor或reviewer", result.get("error"));
    }

    @Test
    void updateScoreRecordCreatesRecordAndUpdatesReviewerScore() {
        Student student = student(1L, 7L, 9L);
        Teacher reviewer = teacher(9L, "评阅教师", 2L);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentTeacher", reviewer);
        session.setAttribute("currentUser", user("TEACHER", 10L, "T009", 2L));

        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(studentService.findById(1L)).thenReturn(student);
        when(teacherScoreRecordMapper.findByStudentIdAndTeacherIdAndYear(1L, 9L, 2026)).thenReturn(null);

        String result = scoreController.updateTeacherScoreRecord(
                1L, "reviewer", 10, 20, 30, null, null, null, session);

        assertEquals("success", result);
        verify(teacherScoreRecordMapper).insert(any(TeacherScoreRecord.class));
        verify(scoreService).setReviewerScore(1L, 2026, 60);
    }

    private User user(String roleName, Long userId, String username, Long departmentId) {
        Role role = new Role();
        role.setName(roleName);
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setDepartmentId(departmentId);
        user.setRole(role);
        return user;
    }

    private MockHttpSession userSession(String roleName, Long userId, String username, Long departmentId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", user(roleName, userId, username, departmentId));
        return session;
    }

    private Teacher teacher(Long id, String name, Long departmentId) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setName(name);
        teacher.setDepartmentId(departmentId);
        teacher.setTeacherNo("T" + id);
        return teacher;
    }

    private Student student(Long id, Long advisorTeacherId, Long reviewerTeacherId) {
        Student student = new Student();
        student.setId(id);
        student.setName("学生");
        student.setStudentNo("2026001");
        student.setAdvisorTeacherId(advisorTeacherId);
        student.setReviewerTeacherId(reviewerTeacherId);
        student.setDefenseYear(2026);

        Department department = new Department();
        department.setName("计算机学院");
        student.setDepartment(department);

        Teacher reviewer = teacher(reviewerTeacherId, "评阅教师", 2L);
        student.setReviewer(reviewer);
        return student;
    }
}
