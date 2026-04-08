package com.example.defensemanagement.controller.export;

import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.service.DocTemplateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupExportControllersTest {

    @Mock
    private StudentMapper studentMapper;
    @Mock
    private DocTemplateService docTemplateService;
    @Mock
    private DefenseGroupMapper defenseGroupMapper;
    @Mock
    private GroupSummaryExportSupport groupSummaryExportSupport;

    private TemplateHelper templateHelper;
    private SignatureHelper signatureHelper;
    private ScorePlaceholderHelper scorePlaceholderHelper;
    private PaperExportController paperExportController;
    private DesignExportController designExportController;

    @BeforeEach
    void setUp() {
        templateHelper = mock(TemplateHelper.class);
        signatureHelper = mock(SignatureHelper.class);
        scorePlaceholderHelper = mock(ScorePlaceholderHelper.class);

        paperExportController = spy(new PaperExportController());
        designExportController = spy(new DesignExportController());
        wireExportController(paperExportController);
        wireExportController(designExportController);
    }

    @Test
    void exportGroupSummaryRendersAttachment() {
        GroupSummaryExportController controller = new GroupSummaryExportController(
                paperExportController,
                designExportController,
                studentMapper,
                docTemplateService,
                defenseGroupMapper,
                groupSummaryExportSupport);
        Student student = student(1L, "张三", "PAPER", 2026, 8L);
        DefenseGroup group = new DefenseGroup();
        group.setName("第一组");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{{GROUP_NAME}}", "第一组");
        Map<String, byte[]> images = new HashMap<>();
        GroupSummaryExportSupport.PreparedGroupSummary summary =
                new GroupSummaryExportSupport.PreparedGroupSummary(placeholders, images);

        when(studentMapper.findByDefenseGroupId(4L)).thenReturn(List.of(student));
        when(defenseGroupMapper.findById(4L)).thenReturn(group);
        when(groupSummaryExportSupport.prepareGroupSummary(4L, List.of(student), "第一组", 2026)).thenReturn(summary);
        when(templateHelper.resolveTemplate("group-summary", "templates/docx/group-summary.docx", 8L))
                .thenReturn("resolved-template.docx");
        when(docTemplateService.renderDoc("resolved-template.docx", placeholders, images)).thenReturn(new byte[]{1, 2, 3});
        when(templateHelper.encode("毕业论文(设计)答辩小组统分表-第一组.docx")).thenReturn("summary.docx");

        ResponseEntity<byte[]> response = controller.exportGroupSummary(4L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(new byte[]{1, 2, 3}, response.getBody());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("summary.docx"));
    }

    @Test
    void exportGroupSummaryRejectsMissingStudents() {
        GroupSummaryExportController controller = new GroupSummaryExportController(
                paperExportController,
                designExportController,
                studentMapper,
                docTemplateService,
                defenseGroupMapper,
                groupSummaryExportSupport);
        when(studentMapper.findByDefenseGroupId(4L)).thenReturn(List.of());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> controller.exportGroupSummary(4L));

        assertEquals("小组无学生", exception.getMessage());
    }

    @Test
    void exportGroupZipPackagesPaperAndDesignDocs() throws Exception {
        GroupScoreZipExportController controller = new GroupScoreZipExportController(
                paperExportController,
                designExportController,
                studentMapper);
        Student paperStudent = student(1L, "张三", "PAPER", 2026, 8L);
        Student designStudent = student(2L, "李四", "DESIGN", 2026, 8L);

        when(studentMapper.findByDefenseGroupId(5L)).thenReturn(List.of(paperStudent, designStudent));
        doReturn(attachment("paper.docx", new byte[]{1, 2})).when(paperExportController).buildScoreDoc(1L);
        doReturn(attachment("design.docx", new byte[]{3, 4})).when(designExportController).buildScoreDoc(2L);
        when(templateHelper.encode("group-5-scores.zip")).thenReturn("group-5-scores.zip");

        ResponseEntity<byte[]> response = controller.exportGroupZip(5L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("group-5-scores.zip"));
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(response.getBody()))) {
            ZipEntry first = zipInputStream.getNextEntry();
            assertEquals("paper.docx", first.getName());
            assertArrayEquals(new byte[]{1, 2}, zipInputStream.readAllBytes());
            ZipEntry second = zipInputStream.getNextEntry();
            assertEquals("design.docx", second.getName());
            assertArrayEquals(new byte[]{3, 4}, zipInputStream.readAllBytes());
        }
    }

    private Student student(Long id, String name, String defenseType, Integer year, Long departmentId) {
        Student student = new Student();
        student.setId(id);
        student.setName(name);
        student.setDefenseType(defenseType);
        student.setDefenseYear(year);
        student.setDepartmentId(departmentId);
        return student;
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
