package com.example.defensemanagement.controller.export;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.service.AiCommentService;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.DocTemplateService;
import com.example.defensemanagement.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportControllersTest {

    @TempDir
    Path tempDir;

    @Mock
    private DocTemplateService docTemplateService;

    @Mock
    private StudentMapper studentMapper;

    @Mock
    private TeacherScoreRecordMapper teacherScoreRecordMapper;

    @Mock
    private StudentFinalScoreMapper studentFinalScoreMapper;

    @Mock
    private ConfigService configService;

    @Mock
    private AiCommentService aiCommentService;

    @Mock
    private DefenseGroupTeacherMapper defenseGroupTeacherMapper;

    @Mock
    private TeacherMapper teacherMapper;

    @Mock
    private UserService userService;

    private TemplateHelper templateHelper;
    private SignatureHelper signatureHelper;
    private ScorePlaceholderHelper scorePlaceholderHelper;

    private PaperExportController paperExportController;
    private DesignExportController designExportController;

    @BeforeEach
    void setUp() {
        templateHelper = new TemplateHelper(configService);
        ReflectionTestUtils.setField(templateHelper, "uploadBaseDir", tempDir.toString());
        signatureHelper = new SignatureHelper(defenseGroupTeacherMapper, teacherMapper, userService, templateHelper);
        scorePlaceholderHelper = new ScorePlaceholderHelper(configService, aiCommentService);

        paperExportController = new PaperExportController();
        designExportController = new DesignExportController();
        wireExportController(paperExportController);
        wireExportController(designExportController);

        lenient().when(configService.getDefenseDatePart(anyString())).thenReturn(null);
        lenient().when(aiCommentService.generateComment(anyString(), anyString())).thenReturn("评语");
        lenient().when(userService.getUsersByRole("DEPT_ADMIN")).thenReturn(Collections.emptyList());
    }

    @Test
    void paperBuildScoreDocRendersAttachment() {
        Student student = paperStudent();
        TeacherScoreRecord record = paperRecord();
        StudentFinalScore finalScore = new StudentFinalScore();
        finalScore.setAdjustmentFactor(1.0);

        when(studentMapper.findById(1L)).thenReturn(student);
        when(teacherScoreRecordMapper.findByStudentIdAndYear(1L, 2025)).thenReturn(List.of(record));
        when(studentFinalScoreMapper.findByStudentIdAndYear(1L, 2025)).thenReturn(finalScore);
        when(configService.getEvaluationItems("PAPER")).thenReturn(null);
        when(docTemplateService.renderDoc(anyString(), anyMap(), anyMap())).thenAnswer(invocation -> {
            String templatePath = invocation.getArgument(0, String.class);
            Map<String, String> placeholders = invocation.getArgument(1);
            assertEquals("templates/docx/paper-score.docx", templatePath);
            assertEquals("张三", placeholders.get("{{NAME}}"));
            assertEquals("40", placeholders.get("{{ITEM1}}"));
            assertEquals("78.0", placeholders.get("{{TOTAL}}"));
            assertEquals("评语", placeholders.get("{{COMMENT}}"));
            return new byte[]{1, 2, 3};
        });

        ResponseEntity<byte[]> response = paperExportController.buildScoreDoc(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains(".docx"));
        assertArrayEquals(new byte[]{1, 2, 3}, response.getBody());
    }

    @Test
    void paperExportPaperScoreReturnsBadRequestWhenTemplateMissing() {
        Student student = paperStudent();
        when(studentMapper.findById(1L)).thenReturn(student);
        when(teacherScoreRecordMapper.findByStudentIdAndYear(1L, 2025)).thenReturn(List.of(paperRecord()));
        when(studentFinalScoreMapper.findByStudentIdAndYear(1L, 2025)).thenReturn(new StudentFinalScore());
        when(configService.getEvaluationItems("PAPER")).thenReturn(null);
        when(docTemplateService.renderDoc(anyString(), anyMap(), anyMap()))
                .thenThrow(new IllegalArgumentException("模板不存在"));

        ResponseEntity<?> response = paperExportController.exportPaperScore(1L);

        assertEquals(400, response.getStatusCodeValue());
        Map<?, ?> body = assertInstanceOf(Map.class, response.getBody());
        assertEquals(Boolean.TRUE, body.get("error"));
        assertEquals("template_not_found", body.get("type"));
    }

    @Test
    void paperBuildGradeDocRejectsMissingFinalScore() {
        Student student = paperStudent();
        when(studentMapper.findById(1L)).thenReturn(student);
        when(studentFinalScoreMapper.findByStudentIdAndYear(1L, 2025)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> paperExportController.buildGradeDoc(1L));

        assertTrue(exception.getMessage().contains("未找到最终成绩"));
    }

    @Test
    void designBuildGradeDocRendersAttachment() {
        Student student = designStudent();
        StudentFinalScore finalScore = new StudentFinalScore();
        finalScore.setAdvisorScore(90);
        finalScore.setReviewerScore(88);
        finalScore.setFinalDefenseScore(84.5);
        finalScore.setTotalGrade(87.0);
        finalScore.setAdjustmentFactor(1.0);

        when(studentMapper.findById(2L)).thenReturn(student);
        when(studentFinalScoreMapper.findByStudentIdAndYear(2L, 2025)).thenReturn(finalScore);
        when(teacherScoreRecordMapper.findByStudentIdAndYear(2L, 2025)).thenReturn(List.of(designRecord()));
        when(docTemplateService.renderDoc(anyString(), anyMap(), anyMap())).thenAnswer(invocation -> {
            Map<String, String> placeholders = invocation.getArgument(1);
            assertEquals("90", placeholders.get("{{ADVISOR_SCORE}}"));
            assertEquals("88", placeholders.get("{{REVIEWER_SCORE}}"));
            assertEquals("84.5", placeholders.get("{{DEFENSE_SCORE}}"));
            assertEquals("87.0", placeholders.get("{{TOTAL_GRADE}}"));
            return new byte[]{9, 8, 7};
        });

        ResponseEntity<byte[]> response = designExportController.buildGradeDoc(2L);

        assertEquals(200, response.getStatusCodeValue());
        assertArrayEquals(new byte[]{9, 8, 7}, response.getBody());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains(".docx"));
    }

    @Test
    void designBuildProcessDocRendersAttachment() {
        Student student = designStudent();
        StudentFinalScore finalScore = new StudentFinalScore();
        finalScore.setAdjustmentFactor(1.0);

        when(studentMapper.findById(2L)).thenReturn(student);
        when(studentFinalScoreMapper.findByStudentIdAndYear(2L, 2025)).thenReturn(finalScore);
        when(teacherScoreRecordMapper.findByStudentIdAndYear(2L, 2025)).thenReturn(List.of(designRecord()));
        when(configService.getEvaluationItems("DESIGN")).thenReturn(null);
        when(docTemplateService.renderDoc(anyString(), anyMap(), anyMap())).thenAnswer(invocation -> {
            String templatePath = invocation.getArgument(0, String.class);
            Map<String, String> placeholders = invocation.getArgument(1);
            assertEquals("templates/docx/design-process.docx", templatePath);
            assertEquals("15", placeholders.get("{{ITEM1}}"));
            assertEquals("90.0", placeholders.get("{{TOTAL}}"));
            assertEquals("评语", placeholders.get("{{COMMENT}}"));
            return new byte[]{4, 5, 6};
        });

        ResponseEntity<byte[]> response = designExportController.buildProcessDoc(2L);

        assertEquals(200, response.getStatusCodeValue());
        assertArrayEquals(new byte[]{4, 5, 6}, response.getBody());
    }

    @Test
    void exportHelperPrefersDepartmentTemplateAndFallsBackToUserSignature() throws Exception {
        Path deptTemplate = Files.createDirectories(tempDir.resolve("templates").resolve("dept_2"))
                .resolve("paper-score.docx");
        Files.write(deptTemplate, new byte[]{1});

        Path signatureFile = Files.createDirectories(tempDir.resolve("signatures")).resolve("user_88.png");
        byte[] signatureBytes = new byte[]{7, 7, 7};
        Files.write(signatureFile, signatureBytes);

        Teacher teacher = new Teacher();
        teacher.setId(5L);
        teacher.setUserId(88L);
        when(teacherMapper.findById(5L)).thenReturn(teacher);

        String resolvedTemplate = paperExportController.resolveTemplate("paper-score", "default.docx", 2L);
        byte[] loadedSignature = paperExportController.loadSignature("teacher_5");
        String encoded = paperExportController.encode("a b.docx");

        assertEquals(deptTemplate.toAbsolutePath().toString(), resolvedTemplate);
        assertArrayEquals(signatureBytes, loadedSignature);
        assertTrue(encoded.contains("%20"));
    }

    private void wireExportController(Object controller) {
        ReflectionTestUtils.setField(controller, "docTemplateService", docTemplateService);
        ReflectionTestUtils.setField(controller, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(controller, "teacherScoreRecordMapper", teacherScoreRecordMapper);
        ReflectionTestUtils.setField(controller, "studentFinalScoreMapper", studentFinalScoreMapper);
        ReflectionTestUtils.setField(controller, "templateHelper", templateHelper);
        ReflectionTestUtils.setField(controller, "signatureHelper", signatureHelper);
        ReflectionTestUtils.setField(controller, "scorePlaceholderHelper", scorePlaceholderHelper);
    }

    private Student paperStudent() {
        Student student = new Student();
        student.setId(1L);
        student.setName("张三");
        student.setStudentNo("2025001");
        student.setTitle("论文题目");
        student.setSummary("论文摘要");
        student.setDefenseYear(2025);
        student.setDepartmentId(2L);
        return student;
    }

    private Student designStudent() {
        Student student = new Student();
        student.setId(2L);
        student.setName("李四");
        student.setStudentNo("2025002");
        student.setTitle("设计题目");
        student.setSummary("设计摘要");
        student.setDefenseYear(2025);
        student.setDepartmentId(2L);
        return student;
    }

    private TeacherScoreRecord paperRecord() {
        TeacherScoreRecord record = new TeacherScoreRecord();
        record.setItem1Score(40);
        record.setItem2Score(20);
        record.setItem3Score(18);
        record.setTotalScore(78);
        return record;
    }

    private TeacherScoreRecord designRecord() {
        TeacherScoreRecord record = new TeacherScoreRecord();
        record.setItem1Score(15);
        record.setItem2Score(15);
        record.setItem3Score(10);
        record.setItem4Score(20);
        record.setItem5Score(15);
        record.setItem6Score(15);
        record.setTotalScore(90);
        return record;
    }
}
