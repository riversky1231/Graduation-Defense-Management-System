package com.example.defensemanagement.controller.export;

import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.service.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderExportControllersTest {

    @Mock
    private TeacherMapper teacherMapper;
    @Mock
    private DefenseGroupTeacherMapper defenseGroupTeacherMapper;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private StudentFinalScoreMapper studentFinalScoreMapper;
    @Mock
    private TeacherScoreRecordMapper teacherScoreRecordMapper;
    @Mock
    private ConfigService configService;
    @Mock
    private DefenseGroupMapper defenseGroupMapper;

    private TemplateHelper templateHelper;
    private SignatureHelper signatureHelper;
    private ScorePlaceholderHelper scorePlaceholderHelper;
    private PaperExportController paperExportController;
    private DesignExportController designExportController;

    private LeaderExportStudentController studentController;
    private LeaderGroupScoreZipExportController zipController;

    @BeforeEach
    void setUp() {
        templateHelper = mock(TemplateHelper.class);
        signatureHelper = mock(SignatureHelper.class);
        scorePlaceholderHelper = mock(ScorePlaceholderHelper.class);

        paperExportController = spy(new PaperExportController());
        designExportController = spy(new DesignExportController());
        wireExportController(paperExportController);
        wireExportController(designExportController);

        studentController = new LeaderExportStudentController(
                paperExportController,
                designExportController,
                teacherMapper,
                studentMapper,
                studentFinalScoreMapper,
                teacherScoreRecordMapper,
                defenseGroupTeacherMapper,
                configService,
                defenseGroupMapper);
        zipController = new LeaderGroupScoreZipExportController(
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

    @Test
    void getLeaderStudentsReturnsMessageWhenDefenseLeaderHasNoGroup() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", user(9L, "DEFENSE_LEADER"));
        when(teacherMapper.findByUserId(9L)).thenReturn(teacher(7L, "组长老师"));
        when(defenseGroupTeacherMapper.findAll()).thenReturn(List.of());

        ResponseEntity<?> response = studentController.getLeaderStudents(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("您已被设置为答辩组长，但尚未被分配到任何答辩小组，请联系管理员", body(response).get("message"));
        assertEquals(0, ((List<?>) body(response).get("students")).size());
    }

    @Test
    void getLeaderStudentsFiltersCurrentYearAndFlagsFinalScore() {
        MockHttpSession session = leaderSession(7L, "组长老师");
        Student currentYearStudent = student(1L, "张三", "2023001", "论文", "PAPER", 2026);
        Student oldYearStudent = student(2L, "李四", "2023002", "设计", "DESIGN", 2025);

        when(defenseGroupTeacherMapper.findAll()).thenReturn(List.of(groupTeacher(11L, 7L, 1)));
        when(studentMapper.findByDefenseGroupId(11L)).thenReturn(List.of(currentYearStudent, oldYearStudent));
        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(studentFinalScoreMapper.findByStudentIdAndYear(1L, 2026)).thenReturn(new StudentFinalScore());

        ResponseEntity<?> response = studentController.getLeaderStudents(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(11L, body(response).get("groupId"));
        List<?> students = (List<?>) body(response).get("students");
        assertEquals(1, students.size());
        Map<?, ?> first = (Map<?, ?>) students.get(0);
        assertEquals("张三", first.get("name"));
        assertEquals(true, first.get("hasFinalScore"));
    }

    @Test
    void exportLeaderGroupScoreZipRejectsWhenNoLeaderGroup() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", user(9L, "DEFENSE_LEADER"));
        when(teacherMapper.findByUserId(9L)).thenReturn(teacher(7L, "组长老师"));
        when(defenseGroupTeacherMapper.findAll()).thenReturn(List.of());

        ResponseEntity<?> response = zipController.exportLeaderGroupScoreZip(session);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("您已被设置为答辩组长，但尚未被分配到任何答辩小组，请联系管理员", body(response).get("error"));
    }

    @Test
    void exportLeaderGroupScoreZipPackagesPaperAndDesignDocs() throws Exception {
        MockHttpSession session = leaderSession(7L, "组长老师");
        Student paperStudent = student(1L, "张三", "2023001", "论文", "PAPER", 2026);
        Student designStudent = student(2L, "李四", "2023002", "设计", "DESIGN", 2026);
        DefenseGroup group = new DefenseGroup();
        group.setName("第一组");

        when(defenseGroupTeacherMapper.findAll()).thenReturn(List.of(groupTeacher(11L, 7L, 1)));
        when(studentMapper.findByDefenseGroupId(11L)).thenReturn(List.of(paperStudent, designStudent));
        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(defenseGroupMapper.findById(11L)).thenReturn(group);
        doReturn(attachment("paper-score.docx", new byte[]{1, 2})).when(paperExportController).buildScoreDoc(1L);
        doReturn(attachment("design-score.docx", new byte[]{3, 4})).when(designExportController).buildScoreDoc(2L);
        when(templateHelper.encode("答辩组长-组长老师-第一组-答辩成绩表.zip")).thenReturn("leader-group.zip");

        ResponseEntity<?> response = zipController.exportLeaderGroupScoreZip(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("leader-group.zip"));
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream((byte[]) response.getBody()))) {
            ZipEntry first = zipInputStream.getNextEntry();
            assertEquals("paper-score.docx", first.getName());
            assertArrayEquals(new byte[]{1, 2}, zipInputStream.readAllBytes());
            ZipEntry second = zipInputStream.getNextEntry();
            assertEquals("design-score.docx", second.getName());
            assertArrayEquals(new byte[]{3, 4}, zipInputStream.readAllBytes());
            assertEquals(null, zipInputStream.getNextEntry());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> body(ResponseEntity<?> response) {
        return (Map<String, Object>) response.getBody();
    }

    private User user(Long id, String roleName) {
        User user = new User();
        user.setId(id);
        Role role = new Role();
        role.setName(roleName);
        user.setRole(role);
        return user;
    }

    private Teacher teacher(Long id, String name) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setName(name);
        return teacher;
    }

    private DefenseGroupTeacher groupTeacher(Long groupId, Long teacherId, Integer isLeader) {
        DefenseGroupTeacher groupTeacher = new DefenseGroupTeacher();
        groupTeacher.setGroupId(groupId);
        groupTeacher.setTeacherId(teacherId);
        groupTeacher.setIsLeader(isLeader);
        return groupTeacher;
    }

    private Student student(Long id, String name, String studentNo, String title, String defenseType, Integer year) {
        Student student = new Student();
        student.setId(id);
        student.setName(name);
        student.setStudentNo(studentNo);
        student.setTitle(title);
        student.setDefenseType(defenseType);
        student.setDefenseYear(year);
        return student;
    }

    private MockHttpSession leaderSession(Long teacherId, String teacherName) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentTeacher", teacher(teacherId, teacherName));
        return session;
    }

    private ResponseEntity<byte[]> attachment(String filename, byte[] body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    private void wireExportController(Object controller) {
        ReflectionTestUtils.setField(controller, "templateHelper", templateHelper);
        ReflectionTestUtils.setField(controller, "signatureHelper", signatureHelper);
        ReflectionTestUtils.setField(controller, "scorePlaceholderHelper", scorePlaceholderHelper);
    }
}
