package com.example.defensemanagement.controller.export;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.service.DocTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 论文导出 Controller。
 * 处理论文相关的成绩表、成绩评定表、过程表导出。
 */
@RestController
@RequestMapping("/export")
public class PaperExportController extends ExportHelper {

    private static final Logger log = LoggerFactory.getLogger(PaperExportController.class);

    private static final String PAPER_SCORE_TEMPLATE = "templates/docx/paper-score.docx";
    private static final String PAPER_GRADE_TEMPLATE = "templates/docx/paper-grade.docx";
    private static final String PAPER_PROCESS_TEMPLATE = "templates/docx/paper-process.docx";

    @Autowired
    private DocTemplateService docTemplateService;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeacherScoreRecordMapper teacherScoreRecordMapper;

    @Autowired
    private StudentFinalScoreMapper studentFinalScoreMapper;

    @GetMapping("/score/paper/{studentId}")
    public ResponseEntity<?> exportPaperScore(@PathVariable Long studentId) {
        try {
            return buildScoreDoc(studentId);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", e.getMessage());
            error.put("type", "template_not_found");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", "导出论文成绩表失败: " + e.getMessage());
            error.put("type", "export_error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/grade/paper/{studentId}")
    public ResponseEntity<byte[]> exportPaperGrade(@PathVariable Long studentId) {
        return buildGradeDoc(studentId);
    }

    @GetMapping("/process/paper/{studentId}")
    public ResponseEntity<byte[]> exportPaperProcess(@PathVariable Long studentId) {
        return buildProcessDoc(studentId);
    }

    public ResponseEntity<byte[]> buildScoreDoc(Long studentId) {
        Student stu = studentMapper.findById(studentId);
        if (stu == null) throw new RuntimeException("学生不存在，ID: " + studentId);

        Integer year = stu.getDefenseYear();
        if (year == null) throw new RuntimeException("学生未设置答辩年份，ID: " + studentId);

        List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(studentId, year);
        StudentFinalScore fs = studentFinalScoreMapper.findByStudentIdAndYear(studentId, year);
        double factor = fs != null && fs.getAdjustmentFactor() != null ? fs.getAdjustmentFactor() : 1.0;

        Map<String, String> ph = buildCommonPlaceholders(stu, getDateParts("DEFENSE_DATE"), true);
        Map<String, byte[]> img = new HashMap<>();

        try {
            fillSignatures(img, stu, false, false);
        } catch (Exception e) {
            log.warn("加载签名失败: {}", e.getMessage());
        }

        fillPaperScorePlaceholders(ph, records, factor, stu);

        String templatePath = resolveTemplate("paper-score", PAPER_SCORE_TEMPLATE, stu.getDepartmentId());
        String filename = encode("本科毕业论文答辩成绩表-" + stu.getName() + ".docx");
        byte[] doc = docTemplateService.renderDoc(templatePath, ph, img);
        return attachment(doc, filename);
    }

    public ResponseEntity<byte[]> buildGradeDoc(Long studentId) {
        Student stu = studentMapper.findById(studentId);
        if (stu == null) throw new RuntimeException("学生不存在");
        Integer year = stu.getDefenseYear();
        StudentFinalScore fs = studentFinalScoreMapper.findByStudentIdAndYear(studentId, year);
        if (fs == null) throw new RuntimeException("未找到最终成绩");

        List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(studentId, year);
        Map<String, String> ph = buildCommonPlaceholders(stu, getDateParts("GRADE_DATE"), true);
        Map<String, byte[]> img = new HashMap<>();

        fillGradePlaceholders(ph, fs, true, records, stu);

        try {
            fillSignatures(img, stu, true, false);
        } catch (Exception e) {
            log.warn("加载签名失败: {}", e.getMessage());
        }

        String filename = encode("本科毕业论文成绩评定表-" + stu.getName() + ".docx");
        String template = resolveTemplate("paper-grade", PAPER_GRADE_TEMPLATE, stu.getDepartmentId());
        byte[] doc = docTemplateService.renderDoc(template, ph, img);
        return attachment(doc, filename);
    }

    public ResponseEntity<byte[]> buildProcessDoc(Long studentId) {
        Student stu = studentMapper.findById(studentId);
        if (stu == null) throw new RuntimeException("学生不存在");
        Integer year = stu.getDefenseYear();
        List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(studentId, year);
        StudentFinalScore fs = studentFinalScoreMapper.findByStudentIdAndYear(studentId, year);
        double factor = fs != null && fs.getAdjustmentFactor() != null ? fs.getAdjustmentFactor() : 1.0;

        Map<String, String> ph = buildCommonPlaceholders(stu, getDateParts("DEFENSE_DATE"), true);
        Map<String, byte[]> img = new HashMap<>();

        fillPaperScorePlaceholders(ph, records, factor, stu);

        try {
            fillSignatures(img, stu, false, true);
        } catch (Exception e) {
            log.warn("加载签名失败: {}", e.getMessage());
        }

        String filename = encode("毕业论文答辩成绩无评语过程表-" + stu.getName() + ".docx");
        String template = resolveTemplate("paper-process", PAPER_PROCESS_TEMPLATE, stu.getDepartmentId());
        byte[] doc = docTemplateService.renderDoc(template, ph, img);
        return attachment(doc, filename);
    }

    private ResponseEntity<byte[]> attachment(byte[] bytes, String filename) {
        MediaType octet = MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(octet)
                .body(bytes);
    }
}
