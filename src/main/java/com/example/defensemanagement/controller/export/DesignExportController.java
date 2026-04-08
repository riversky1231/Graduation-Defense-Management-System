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
 * 设计导出 Controller。
 * 处理设计（课题）相关的成绩表、成绩评定表、过程表导出。
 */
@RestController
@RequestMapping("/export")
public class DesignExportController extends ExportHelper {

    private static final Logger log = LoggerFactory.getLogger(DesignExportController.class);

    private static final String DESIGN_SCORE_TEMPLATE = "templates/docx/design-score.docx";
    private static final String DESIGN_GRADE_TEMPLATE = "templates/docx/design-grade.docx";
    private static final String DESIGN_PROCESS_TEMPLATE = "templates/docx/design-process.docx";

    @Autowired
    private DocTemplateService docTemplateService;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeacherScoreRecordMapper teacherScoreRecordMapper;

    @Autowired
    private StudentFinalScoreMapper studentFinalScoreMapper;

    @GetMapping("/score/design/{studentId}")
    public ResponseEntity<byte[]> exportDesignScore(@PathVariable Long studentId) {
        return buildScoreDoc(studentId);
    }

    @GetMapping("/grade/design/{studentId}")
    public ResponseEntity<byte[]> exportDesignGrade(@PathVariable Long studentId) {
        return buildGradeDoc(studentId);
    }

    @GetMapping("/process/design/{studentId}")
    public ResponseEntity<byte[]> exportDesignProcess(@PathVariable Long studentId) {
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

        Map<String, String> ph = buildCommonPlaceholders(stu, getDateParts("DEFENSE_DATE"), false);
        Map<String, byte[]> img = new HashMap<>();

        try {
            fillSignatures(img, stu, false, false);
        } catch (Exception e) {
            log.warn("加载签名失败: {}", e.getMessage());
        }

        fillDesignScorePlaceholders(ph, records, factor, stu);

        String templatePath = resolveTemplate("design-score", DESIGN_SCORE_TEMPLATE, stu.getDepartmentId());
        String filename = encode("本科毕业设计答辩成绩表-" + stu.getName() + ".docx");
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
        Map<String, String> ph = buildCommonPlaceholders(stu, getDateParts("GRADE_DATE"), false);
        Map<String, byte[]> img = new HashMap<>();

        fillGradePlaceholders(ph, fs, false, records, stu);

        try {
            fillSignatures(img, stu, true, false);
        } catch (Exception e) {
            log.warn("加载签名失败: {}", e.getMessage());
        }

        String filename = encode("本科毕业设计成绩评定表-" + stu.getName() + ".docx");
        String template = resolveTemplate("design-grade", DESIGN_GRADE_TEMPLATE, stu.getDepartmentId());
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

        Map<String, String> ph = buildCommonPlaceholders(stu, getDateParts("DEFENSE_DATE"), false);
        Map<String, byte[]> img = new HashMap<>();

        fillDesignScorePlaceholders(ph, records, factor, stu);

        try {
            fillSignatures(img, stu, false, true);
        } catch (Exception e) {
            log.warn("加载签名失败: {}", e.getMessage());
        }

        String filename = encode("毕业设计答辩成绩无评语过程表-" + stu.getName() + ".docx");
        String template = resolveTemplate("design-process", DESIGN_PROCESS_TEMPLATE, stu.getDepartmentId());
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
