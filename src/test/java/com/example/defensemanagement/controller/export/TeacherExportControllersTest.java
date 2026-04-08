package com.example.defensemanagement.controller.export;

import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.entity.User;
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
class TeacherExportControllersTest {

    @Mock
    private TeacherMapper teacherMapper;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private StudentFinalScoreMapper studentFinalScoreMapper;
    @Mock
    private TeacherScoreRecordMapper teacherScoreRecordMapper;
    @Mock
    private DefenseGroupTeacherMapper defenseGroupTeacherMapper;
    @Mock
    private ConfigService configService;

    private TemplateHelper templateHelper;
    private SignatureHelper signatureHelper;
    private ScorePlaceholderHelper scorePlaceholderHelper;
    private PaperExportController paperExportController;
    private DesignExportController designExportController;

    private TeacherExportStudentController studentController;
    private TeacherGradeZipExportController gradeZipController;
    private TeacherGroupZipExportController groupZipController;

    @BeforeEach
    void setUp() {
        templateHelper = mock(TemplateHelper.class);
        signatureHelper = mock(SignatureHelper.class);
        scorePlaceholderHelper = mock(ScorePlaceholderHelper.class);

        paperExportController = spy(new PaperExportController());
        designExportController = spy(new DesignExportController());
        wireExportController(paperExportController);
        wireExportController(designExportController);

        studentController = new TeacherExportStudentController(
                paperExportController,
                designExportController,
                teacherMapper,
                studentMapper,
                studentFinalScoreMapper,
                teacherScoreRecordMapper,
                defenseGroupTeacherMapper,
                configService);
        gradeZipController = new TeacherGradeZipExportController(
                paperExportController,
                designExportController,
                teacherMapper,
                studentMapper,
                studentFinalScoreMapper,
                teacherScoreRecordMapper,
                defenseGroupTeacherMapper,
                configService);
        groupZipController = new TeacherGroupZipExportController(
                paperExportController,
                designExportController,
                teacherMapper,
                studentMapper,
                studentFinalScoreMapper,
                teacherScoreRecordMapper,
                defenseGroupTeacherMapper,
                configService);
    }

    @Test
    void getTeacherStudentsRejectsWhenTeacherMissing() {
        ResponseEntity<?> response = studentController.getTeacherStudents(new MockHttpSession());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("未登录或不是教师", body(response).get("error"));
    }

    @Test
    void getTeacherStudentsResolvesTeacherFromCurrentUser() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", user(8L, "TEACHER"));
        Teacher teacher = teacher(3L, "李老师");
        Student studentWithScore = student(1L, "张三", "2023001", "论文题目", "PAPER", 2026);
        Student studentWithoutScore = student(2L, "李四", "2023002", "设计题目", "DESIGN", 2026);

        when(teacherMapper.findByUserId(8L)).thenReturn(teacher);
        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(studentMapper.findByAdvisorIdAndYear(3L, 2026)).thenReturn(List.of(studentWithScore, studentWithoutScore));
        when(studentFinalScoreMapper.findByStudentIdAndYear(1L, 2026)).thenReturn(new StudentFinalScore());
        when(studentFinalScoreMapper.findByStudentIdAndYear(2L, 2026)).thenReturn(null);

        ResponseEntity<?> response = studentController.getTeacherStudents(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<?> students = (List<?>) body(response).get("students");
        assertEquals(2, students.size());
        Map<?, ?> first = (Map<?, ?>) students.get(0);
        Map<?, ?> second = (Map<?, ?>) students.get(1);
        assertEquals("张三", first.get("name"));
        assertEquals(true, first.get("hasFinalScore"));
        assertEquals("设计题目", second.get("title"));
        assertEquals(false, second.get("hasFinalScore"));
    }

    @Test
    void exportTeacherGradeZipRejectsWhenNoStudentHasFinalScore() {
        MockHttpSession session = teacherSession(3L, "李老师");

        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(studentMapper.findByAdvisorIdAndYear(3L, 2026)).thenReturn(List.of(student(1L, "张三", "2023001", "论文", "PAPER", 2026)));
        when(studentFinalScoreMapper.findByStudentIdAndYear(1L, 2026)).thenReturn(null);

        ResponseEntity<?> response = gradeZipController.exportTeacherGradeZip(session);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("没有可导出的学生成绩评定表", body(response).get("error"));
    }

    @Test
    void exportTeacherGradeZipPackagesOnlyStudentsWithFinalScore() throws Exception {
        MockHttpSession session = teacherSession(3L, "李老师");
        Student paperStudent = student(1L, "张三", "2023001", "论文", "PAPER", 2026);
        Student designStudent = student(2L, "李四", "2023002", "设计", "DESIGN", 2026);
        Student skippedStudent = student(3L, "王五", "2023003", "未评分", "PAPER", 2026);

        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(studentMapper.findByAdvisorIdAndYear(3L, 2026)).thenReturn(List.of(paperStudent, designStudent, skippedStudent));
        when(studentFinalScoreMapper.findByStudentIdAndYear(1L, 2026)).thenReturn(new StudentFinalScore());
        when(studentFinalScoreMapper.findByStudentIdAndYear(2L, 2026)).thenReturn(new StudentFinalScore());
        when(studentFinalScoreMapper.findByStudentIdAndYear(3L, 2026)).thenReturn(null);
        doReturn(attachment("paper-grade.docx", new byte[]{1, 2})).when(paperExportController).buildGradeDoc(1L);
        doReturn(attachment("design-grade.docx", new byte[]{3, 4})).when(designExportController).buildGradeDoc(2L);
        when(templateHelper.encode("教师-李老师-成绩评定表.zip")).thenReturn("teacher-grade.zip");

        ResponseEntity<?> response = gradeZipController.exportTeacherGradeZip(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("teacher-grade.zip"));
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream((byte[]) response.getBody()))) {
            ZipEntry first = zipInputStream.getNextEntry();
            assertEquals("paper-grade.docx", first.getName());
            assertArrayEquals(new byte[]{1, 2}, zipInputStream.readAllBytes());
            ZipEntry second = zipInputStream.getNextEntry();
            assertEquals("design-grade.docx", second.getName());
            assertArrayEquals(new byte[]{3, 4}, zipInputStream.readAllBytes());
            assertEquals(null, zipInputStream.getNextEntry());
        }
    }

    @Test
    void exportTeacherGroupProcessZipRejectsLeader() {
        MockHttpSession session = teacherSession(7L, "组长老师");
        DefenseGroupTeacher groupTeacher = new DefenseGroupTeacher();
        groupTeacher.setGroupId(11L);
        groupTeacher.setIsLeader(1);
        when(defenseGroupTeacherMapper.findByTeacherId(7L)).thenReturn(groupTeacher);

        ResponseEntity<?> response = groupZipController.exportTeacherGroupProcessZip(session);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("答辩组长不能使用此功能", body(response).get("error"));
    }

    @Test
    void exportTeacherGroupProcessZipPackagesOnlyCurrentYearStudentsScoredByTeacher() throws Exception {
        MockHttpSession session = teacherSession(7L, "李老师");
        DefenseGroupTeacher groupTeacher = new DefenseGroupTeacher();
        groupTeacher.setGroupId(11L);
        groupTeacher.setIsLeader(0);
        Student paperStudent = student(1L, "张三", "2023001", "论文", "PAPER", 2026);
        Student otherTeacherStudent = student(2L, "李四", "2023002", "论文", "PAPER", 2026);
        Student oldYearStudent = student(3L, "王五", "2023003", "设计", "DESIGN", 2025);
        Student designStudent = student(4L, "赵六", "2023004", "设计", "DESIGN", 2026);

        when(defenseGroupTeacherMapper.findByTeacherId(7L)).thenReturn(groupTeacher);
        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(studentMapper.findByDefenseGroupId(11L)).thenReturn(List.of(
                paperStudent,
                otherTeacherStudent,
                oldYearStudent,
                designStudent));
        when(teacherScoreRecordMapper.findByStudentIdAndYear(1L, 2026)).thenReturn(List.of(scoreRecord(7L)));
        when(teacherScoreRecordMapper.findByStudentIdAndYear(2L, 2026)).thenReturn(List.of(scoreRecord(9L)));
        when(teacherScoreRecordMapper.findByStudentIdAndYear(4L, 2026)).thenReturn(List.of(scoreRecord(7L)));
        doReturn(attachment("paper-process.docx", new byte[]{5, 6})).when(paperExportController).buildProcessDoc(1L);
        doReturn(attachment("design-process.docx", new byte[]{7, 8})).when(designExportController).buildProcessDoc(4L);
        when(templateHelper.encode("教师-李老师-本组已评分学生无评语过程表.zip")).thenReturn("teacher-process.zip");

        ResponseEntity<?> response = groupZipController.exportTeacherGroupProcessZip(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("teacher-process.zip"));
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream((byte[]) response.getBody()))) {
            ZipEntry first = zipInputStream.getNextEntry();
            assertEquals("paper-process.docx", first.getName());
            assertArrayEquals(new byte[]{5, 6}, zipInputStream.readAllBytes());
            ZipEntry second = zipInputStream.getNextEntry();
            assertEquals("design-process.docx", second.getName());
            assertArrayEquals(new byte[]{7, 8}, zipInputStream.readAllBytes());
            assertEquals(null, zipInputStream.getNextEntry());
        }
    }

    @Test
    void exportTeacherGradedGroupStudentsZipPackagesOnlyScoredStudentsWithFinalScore() throws Exception {
        MockHttpSession session = teacherSession(7L, "李老师");
        DefenseGroupTeacher groupTeacher = new DefenseGroupTeacher();
        groupTeacher.setGroupId(11L);
        Student scoredWithFinalScore = student(1L, "张三", "2023001", "论文", "PAPER", 2026);
        Student scoredWithoutFinalScore = student(2L, "李四", "2023002", "设计", "DESIGN", 2026);
        Student finalScoreFromOtherTeacher = student(3L, "王五", "2023003", "论文", "PAPER", 2026);

        when(defenseGroupTeacherMapper.findByTeacherId(7L)).thenReturn(groupTeacher);
        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(studentMapper.findByDefenseGroupId(11L)).thenReturn(List.of(
                scoredWithFinalScore,
                scoredWithoutFinalScore,
                finalScoreFromOtherTeacher));
        when(teacherScoreRecordMapper.findByStudentIdAndYear(1L, 2026)).thenReturn(List.of(scoreRecord(7L)));
        when(teacherScoreRecordMapper.findByStudentIdAndYear(2L, 2026)).thenReturn(List.of(scoreRecord(7L)));
        when(teacherScoreRecordMapper.findByStudentIdAndYear(3L, 2026)).thenReturn(List.of(scoreRecord(9L)));
        when(studentFinalScoreMapper.findByStudentIdAndYear(1L, 2026)).thenReturn(new StudentFinalScore());
        when(studentFinalScoreMapper.findByStudentIdAndYear(2L, 2026)).thenReturn(null);
        doReturn(attachment("paper-grade.docx", new byte[]{9, 10})).when(paperExportController).buildGradeDoc(1L);
        when(templateHelper.encode("教师-李老师-本组已评分学生成绩评定表.zip")).thenReturn("teacher-graded.zip");

        ResponseEntity<?> response = groupZipController.exportTeacherGradedGroupStudentsZip(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("teacher-graded.zip"));
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream((byte[]) response.getBody()))) {
            ZipEntry first = zipInputStream.getNextEntry();
            assertEquals("paper-grade.docx", first.getName());
            assertArrayEquals(new byte[]{9, 10}, zipInputStream.readAllBytes());
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

    private TeacherScoreRecord scoreRecord(Long teacherId) {
        TeacherScoreRecord record = new TeacherScoreRecord();
        record.setTeacherId(teacherId);
        return record;
    }

    private MockHttpSession teacherSession(Long teacherId, String teacherName) {
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
