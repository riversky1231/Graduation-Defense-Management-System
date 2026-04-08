package com.example.defensemanagement.controller.export;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.mapper.StudentMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * 小组导出共享基类。
 */
abstract class AbstractGroupExportController {

    protected final PaperExportController paperExportController;
    protected final DesignExportController designExportController;
    protected final StudentMapper studentMapper;

    protected AbstractGroupExportController(
            PaperExportController paperExportController,
            DesignExportController designExportController,
            StudentMapper studentMapper) {
        this.paperExportController = paperExportController;
        this.designExportController = designExportController;
        this.studentMapper = studentMapper;
    }

    protected List<Student> getGroupStudents(Long groupId) {
        List<Student> students = studentMapper.findByDefenseGroupId(groupId);
        if (students == null || students.isEmpty()) {
            throw new RuntimeException("小组无学生");
        }
        return students;
    }

    protected ResponseEntity<byte[]> attachment(byte[] bytes, String filename) {
        MediaType octet = MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(octet)
                .body(bytes);
    }

    protected String extractFilename(ResponseEntity<byte[]> response, String fallback) {
        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        if (disposition != null && disposition.contains("filename=\"")) {
            int start = disposition.indexOf("filename=\"") + 10;
            int end = disposition.indexOf("\"", start);
            if (end > start) {
                return disposition.substring(start, end);
            }
        }
        return fallback;
    }
}
