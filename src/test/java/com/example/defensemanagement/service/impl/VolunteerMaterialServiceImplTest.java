package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.common.StudentMaterialContext;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentPreference;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VolunteerMaterialServiceImplTest {

    private Path createdRoot;

    @AfterEach
    void tearDown() throws IOException {
        if (createdRoot != null && Files.exists(createdRoot)) {
            try (var stream = Files.walk(createdRoot)) {
                stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
            }
        }
    }

    @Test
    void usesTeacherSpecificPdfWhenSelectedTeacherMatchesChoice() throws Exception {
        VolunteerMaterialServiceImpl service = new VolunteerMaterialServiceImpl();
        Student student = student();
        StudentPreference preference = new StudentPreference();
        preference.setChoice2TeacherId(22L);
        preference.setFile2Path(createVolunteerPdf("choice2.pdf", "computer vision segmentation"));

        StudentMaterialContext context = service.buildStudentContext(student, preference, 22L);

        assertEquals(StudentMaterialContext.SOURCE_PREFERRED_PDF, context.getSource());
        assertEquals("teacher_specific_pdf", context.getReason());
        assertTrue(context.getContent().contains("computer vision segmentation"));
        assertTrue(context.getContent().contains("题目：智能分割系统"));
    }

    @Test
    void returnsEmptyWhenPdfMissing() {
        VolunteerMaterialServiceImpl service = new VolunteerMaterialServiceImpl();
        Student student = student();
        StudentPreference preference = new StudentPreference();
        preference.setChoice1TeacherId(11L);
        preference.setFile1Path("uploads/volunteer/2026/S001/missing.pdf");

        StudentMaterialContext context = service.buildStudentContext(student, preference, 11L);

        // 当 PDF 不存在时，服务不回退到 metadata（避免排名误导），而是返回空
        assertEquals(StudentMaterialContext.SOURCE_EMPTY, context.getSource());
        assertEquals("pdf_unavailable_or_unreadable", context.getReason());
        assertEquals("", context.getContent());
    }

    private Student student() {
        Student student = new Student();
        student.setTitle("智能分割系统");
        student.setSummary("用于医学图像分析");
        student.setDefenseType("DESIGN");
        return student;
    }

    private String createVolunteerPdf(String fileName, String text) throws IOException {
        createdRoot = Path.of("uploads", "volunteer", "test-" + UUID.randomUUID()).toAbsolutePath().normalize();
        Path uploadsDir = createdRoot.resolve("2026").resolve("S001");
        Files.createDirectories(uploadsDir);
        Path pdfPath = uploadsDir.resolve(fileName);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(50, 700);
                stream.showText(text);
                stream.endText();
            }
            document.save(pdfPath.toFile());
        }

        return Path.of("uploads", "volunteer", createdRoot.getFileName().toString(), "2026", "S001", fileName)
                .toString()
                .replace("\\", "/");
    }
}
