package com.example.defensemanagement.controller.export;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.mapper.StudentMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 小组成绩表打包导出 Controller。
 */
@RestController
@RequestMapping("/export")
public class GroupScoreZipExportController extends AbstractGroupExportController {

    public GroupScoreZipExportController(
            PaperExportController paperExportController,
            DesignExportController designExportController,
            StudentMapper studentMapper) {
        super(paperExportController, designExportController, studentMapper);
    }

    @GetMapping("/group/{groupId}/zip")
    public ResponseEntity<byte[]> exportGroupZip(@PathVariable Long groupId) {
        List<Student> students = getGroupStudents(groupId);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            for (Student student : students) {
                boolean isPaper = "PAPER".equalsIgnoreCase(student.getDefenseType());
                ResponseEntity<byte[]> response = isPaper
                        ? paperExportController.buildScoreDoc(student.getId())
                        : designExportController.buildScoreDoc(student.getId());
                String fallback = (isPaper ? "本科毕业论文答辩成绩表-" : "本科毕业设计答辩成绩表-")
                        + student.getName() + ".docx";
                zipOutputStream.putNextEntry(new ZipEntry(extractFilename(response, fallback)));
                zipOutputStream.write(response.getBody());
                zipOutputStream.closeEntry();
            }
            zipOutputStream.finish();
            return attachment(outputStream.toByteArray(), paperExportController.encode("group-" + groupId + "-scores.zip"));
        } catch (Exception e) {
            throw new RuntimeException("打包导出失败: " + e.getMessage(), e);
        }
    }
}
