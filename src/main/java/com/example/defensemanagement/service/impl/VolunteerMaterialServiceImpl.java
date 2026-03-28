package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.common.StudentMaterialContext;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentPreference;
import com.example.defensemanagement.service.VolunteerMaterialService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class VolunteerMaterialServiceImpl implements VolunteerMaterialService {

    private static final Logger log = LoggerFactory.getLogger(VolunteerMaterialServiceImpl.class);
    private static final int MAX_PDF_TEXT_CHARS = 4000;

    @Override
    public StudentMaterialContext buildStudentContext(Student student, StudentPreference preference, Long teacherId) {
        String preferredPath = resolvePreferredPath(preference, teacherId);
        if (StringUtils.hasText(preferredPath)) {
            String text = extractPdfText(preferredPath);
            if (StringUtils.hasText(text)) {
                return new StudentMaterialContext(
                        composeContext(student, text),
                        StudentMaterialContext.SOURCE_PREFERRED_PDF,
                        "teacher_specific_pdf");
            }
        }

        String fallbackPath = resolveFirstAvailablePath(preference);
        if (StringUtils.hasText(fallbackPath) && !fallbackPath.equals(preferredPath)) {
            String text = extractPdfText(fallbackPath);
            if (StringUtils.hasText(text)) {
                return new StudentMaterialContext(
                        composeContext(student, text),
                        StudentMaterialContext.SOURCE_FIRST_AVAILABLE_PDF,
                        "first_available_pdf");
            }
        }

        String metadataContext = composeContext(student, null);
        if (StringUtils.hasText(metadataContext)) {
            return new StudentMaterialContext(
                    metadataContext,
                    StudentMaterialContext.SOURCE_METADATA_FALLBACK,
                    StringUtils.hasText(preferredPath) || StringUtils.hasText(fallbackPath)
                            ? "pdf_unavailable_or_unreadable"
                            : "pdf_not_uploaded");
        }

        return new StudentMaterialContext("", StudentMaterialContext.SOURCE_EMPTY, "student_context_blank");
    }

    private String resolvePreferredPath(StudentPreference preference, Long teacherId) {
        if (preference == null || teacherId == null) {
            return null;
        }
        if (teacherId.equals(preference.getChoice1TeacherId())) {
            return preference.getFile1Path();
        }
        if (teacherId.equals(preference.getChoice2TeacherId())) {
            return preference.getFile2Path();
        }
        if (teacherId.equals(preference.getChoice3TeacherId())) {
            return preference.getFile3Path();
        }
        return null;
    }

    private String resolveFirstAvailablePath(StudentPreference preference) {
        if (preference == null) {
            return null;
        }
        if (StringUtils.hasText(preference.getFile1Path())) {
            return preference.getFile1Path();
        }
        if (StringUtils.hasText(preference.getFile2Path())) {
            return preference.getFile2Path();
        }
        if (StringUtils.hasText(preference.getFile3Path())) {
            return preference.getFile3Path();
        }
        return null;
    }

    private String composeContext(Student student, String pdfText) {
        if (student == null) {
            return pdfText == null ? "" : pdfText;
        }
        StringBuilder builder = new StringBuilder();
        appendLine(builder, "题目", student.getTitle());
        appendLine(builder, "摘要", student.getSummary());
        appendLine(builder, "答辩类型", student.getDefenseType());
        appendLine(builder, "材料内容", pdfText);
        return builder.toString().trim();
    }

    private void appendLine(StringBuilder builder, String label, String value) {
        if (StringUtils.hasText(value)) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(label).append("：").append(value.trim());
        }
    }

    private String extractPdfText(String filePath) {
        Path resolvedPath = resolveVolunteerPath(filePath);
        if (resolvedPath == null || !Files.exists(resolvedPath) || !Files.isRegularFile(resolvedPath)) {
            return null;
        }

        try (PDDocument document = PDDocument.load(resolvedPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            if (!StringUtils.hasText(text)) {
                return null;
            }
            String normalized = text.replaceAll("\\s+", " ").trim();
            if (normalized.length() > MAX_PDF_TEXT_CHARS) {
                return normalized.substring(0, MAX_PDF_TEXT_CHARS);
            }
            return normalized;
        } catch (IOException e) {
            log.warn("Failed to extract volunteer pdf text from {}", resolvedPath, e);
            return null;
        }
    }

    private Path resolveVolunteerPath(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return null;
        }
        String normalized = filePath.replace("\\", "/");
        if (!normalized.startsWith("uploads/volunteer/")) {
            return null;
        }
        Path baseDir = Paths.get("uploads", "volunteer").toAbsolutePath().normalize();
        Path candidate = Paths.get(filePath).normalize();
        if (!candidate.isAbsolute()) {
            candidate = Paths.get("").toAbsolutePath().resolve(candidate).normalize();
        }
        if (!candidate.startsWith(baseDir)) {
            return null;
        }
        return candidate;
    }
}
