package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.service.DocTemplateService;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.AiCommentService;
import com.example.defensemanagement.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 简单导出接口：根据模板生成成绩相关 Word。
 * 模板需放在 classpath:templates/docx/ 下。
 */
@RestController
@RequestMapping("/export")
public class ExportController {

    private static final String PAPER_SCORE_TEMPLATE = "templates/docx/paper-score.docx";
    private static final String DESIGN_SCORE_TEMPLATE = "templates/docx/design-score.docx";
    private static final String PAPER_GRADE_TEMPLATE = "templates/docx/paper-grade.docx";
    private static final String DESIGN_GRADE_TEMPLATE = "templates/docx/design-grade.docx";
    private static final String PAPER_PROCESS_TEMPLATE = "templates/docx/paper-process.docx";
    private static final String DESIGN_PROCESS_TEMPLATE = "templates/docx/design-process.docx";
    @Autowired
    private DocTemplateService docTemplateService;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeacherScoreRecordMapper teacherScoreRecordMapper;

    @Autowired
    private StudentFinalScoreMapper studentFinalScoreMapper;

    @Autowired
    private ConfigService configService;

    @Autowired
    private AiCommentService aiCommentService;

    @Autowired
    private DefenseGroupTeacherMapper defenseGroupTeacherMapper;
    
    @Autowired
    private TeacherMapper teacherMapper;
    
    @Autowired
    private UserService userService;

    @Value("${app.upload.base-dir:uploads}")
    private String uploadBaseDir;
    
    /**
     * 获取上传目录的绝对路径
     */
    private java.nio.file.Path getUploadBasePath() {
        java.nio.file.Path basePath = Paths.get(uploadBaseDir);
        // 如果是相对路径，转换为相对于项目根目录的绝对路径
        if (!basePath.isAbsolute()) {
            String userDir = System.getProperty("user.dir");
            basePath = Paths.get(userDir, uploadBaseDir);
        }
        return basePath;
    }

    @GetMapping("/score/paper/{studentId}")
    public ResponseEntity<?> exportPaperScore(@PathVariable Long studentId) {
        try {
            return buildScoreDoc(studentId, true);
        } catch (IllegalArgumentException e) {
            // 模板文件不存在等参数错误，返回JSON错误信息
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", e.getMessage());
            error.put("type", "template_not_found");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            // 其他错误，返回JSON错误信息
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", "导出论文成绩表失败: " + e.getMessage());
            error.put("type", "export_error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/score/design/{studentId}")
    public ResponseEntity<byte[]> exportDesignScore(@PathVariable Long studentId) {
        try {
            return buildScoreDoc(studentId, false);
        } catch (Exception e) {
            throw new RuntimeException("导出设计成绩表失败: " + e.getMessage(), e);
        }
    }

    @GetMapping("/grade/paper/{studentId}")
    public ResponseEntity<byte[]> exportPaperGrade(@PathVariable Long studentId) {
        return buildGradeDoc(studentId, true);
    }

    @GetMapping("/grade/design/{studentId}")
    public ResponseEntity<byte[]> exportDesignGrade(@PathVariable Long studentId) {
        return buildGradeDoc(studentId, false);
    }

    @GetMapping("/process/paper/{studentId}")
    public ResponseEntity<byte[]> exportPaperProcess(@PathVariable Long studentId) {
        return buildProcessDoc(studentId, true);
    }

    @GetMapping("/process/design/{studentId}")
    public ResponseEntity<byte[]> exportDesignProcess(@PathVariable Long studentId) {
        return buildProcessDoc(studentId, false);
    }

    ResponseEntity<byte[]> buildScoreDoc(Long studentId, boolean isPaper) {
        try {
            Student stu = studentMapper.findById(studentId);
            if (stu == null) throw new RuntimeException("学生不存在，ID: " + studentId);
            
            Integer year = stu.getDefenseYear();
            if (year == null) throw new RuntimeException("学生未设置答辩年份，ID: " + studentId);
            
            List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(studentId, year);
            StudentFinalScore fs = studentFinalScoreMapper.findByStudentIdAndYear(studentId, year);
            double factor = fs != null && fs.getAdjustmentFactor() != null ? fs.getAdjustmentFactor() : 1.0;
            
            Map<String, String> ph = buildCommonPlaceholders(stu, getDateParts("DEFENSE_DATE"), isPaper);
            Map<String, byte[]> img = new HashMap<>();

            // 答辩成绩表：需要答辩组长签名（可选，没有签名也能导出）
            try {
                fillSignatures(img, stu, false, false);
            } catch (Exception e) {
                // 签名加载失败不影响导出，只记录日志
                System.err.println("警告：加载签名失败: " + e.getMessage());
            }

            String templatePath = resolveTemplate(isPaper ? "paper-score" : "design-score",
                    isPaper ? PAPER_SCORE_TEMPLATE : DESIGN_SCORE_TEMPLATE, stu.getDepartmentId());
            
            if (isPaper) {
                fillPaperScores(ph, records, factor, fs, stu);
                String filename = encode("本科毕业论文答辩成绩表-" + stu.getName() + ".docx");
                byte[] doc = docTemplateService.renderDoc(templatePath, ph, img);
                return attachment(doc, filename);
            } else {
                fillDesignScores(ph, records, factor, fs, stu);
                String filename = encode("本科毕业设计答辩成绩表-" + stu.getName() + ".docx");
                byte[] doc = docTemplateService.renderDoc(templatePath, ph, img);
                return attachment(doc, filename);
            }
        } catch (IllegalArgumentException e) {
            // 模板文件不存在的错误，提供更友好的提示
            throw new RuntimeException("导出失败: " + e.getMessage() + 
                "。请以超级管理员身份登录，在\"系统设置\"->\"模板管理\"中上传对应的Word模板文件。", e);
        } catch (Exception e) {
            throw new RuntimeException("导出失败: " + e.getMessage() + 
                " (学生ID: " + studentId + ", 类型: " + (isPaper ? "论文" : "设计") + ")", e);
        }
    }

    ResponseEntity<byte[]> buildGradeDoc(Long studentId, boolean isPaper) {
        Student stu = studentMapper.findById(studentId);
        if (stu == null) throw new RuntimeException("学生不存在");
        Integer year = stu.getDefenseYear();
        StudentFinalScore fs = studentFinalScoreMapper.findByStudentIdAndYear(studentId, year);
        if (fs == null) throw new RuntimeException("未找到最终成绩");
        Map<String, String> ph = buildCommonPlaceholders(stu, getDateParts("GRADE_DATE"), isPaper);
        Map<String, byte[]> img = new HashMap<>();

        int advisorScore = fs.getAdvisorScore() == null ? 0 : fs.getAdvisorScore();
        int reviewerScore = fs.getReviewerScore() == null ? 0 : fs.getReviewerScore();
        double defenseScore = fs.getFinalDefenseScore() == null ? 0 : fs.getFinalDefenseScore();
        ph.put("{{ADVISOR_SCORE}}", String.valueOf(advisorScore));
        ph.put("{{REVIEWER_SCORE}}", String.valueOf(reviewerScore));
        ph.put("{{DEFENSE_SCORE}}", format1(defenseScore));
        ph.put("{{ADVISOR_30}}", format1(advisorScore * 0.3));
        ph.put("{{REVIEWER_30}}", format1(reviewerScore * 0.3));
        ph.put("{{DEFENSE_40}}", format1(defenseScore * 0.4));
        ph.put("{{TOTAL_GRADE}}", format1(fs.getTotalGrade() == null ? 0 : fs.getTotalGrade()));

        // 成绩评定表：需要评语（论文和设计类型都需要评语）
        List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(studentId, year);
        double factor = fs.getAdjustmentFactor() != null ? fs.getAdjustmentFactor() : 1.0;
        if (isPaper) {
            // 论文类型：需要评语
            AvgScores avg = avgPaper(records);
            double item1Scaled = avg.item1Scaled(factor);
            double item2Scaled = avg.item2Scaled(factor);
            double item3Scaled = avg.item3Scaled(factor);
            double totalFromItems = item1Scaled + item2Scaled + item3Scaled;
            ph.put("{{COMMENT}}", generateComment("PAPER_PROMPT", stu, totalFromItems, factor));
        } else {
            // 设计类型：需要评语
            double totalScore = fs.getFinalDefenseScore() != null ? fs.getFinalDefenseScore() : 0.0;
            ph.put("{{COMMENT}}", generateComment("DESIGN_PROMPT", stu, totalScore, factor));
        }

        // 成绩评定表：需要指导教师、评阅教师、答辩组长和系主任签名
        try {
            fillSignatures(img, stu, true, false);
        } catch (Exception e) {
            // 签名加载失败不影响导出，只记录日志
            System.err.println("警告：加载签名失败: " + e.getMessage());
        }
        
        String filename = encode((isPaper ? "本科毕业论文成绩评定表-" : "本科毕业设计成绩评定表-") + stu.getName() + ".docx");
        String template = isPaper ? resolveTemplate("paper-grade", PAPER_GRADE_TEMPLATE, stu.getDepartmentId())
                : resolveTemplate("design-grade", DESIGN_GRADE_TEMPLATE, stu.getDepartmentId());
        byte[] doc = docTemplateService.renderDoc(template, ph, img);
        return attachment(doc, filename);
    }

    ResponseEntity<byte[]> buildProcessDoc(Long studentId, boolean isPaper) {
        Student stu = studentMapper.findById(studentId);
        if (stu == null) throw new RuntimeException("学生不存在");
        Integer year = stu.getDefenseYear();
        List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(studentId, year);
        StudentFinalScore fs = studentFinalScoreMapper.findByStudentIdAndYear(studentId, year);
        double factor = fs != null && fs.getAdjustmentFactor() != null ? fs.getAdjustmentFactor() : 1.0;

        Map<String, String> ph = buildCommonPlaceholders(stu, getDateParts("DEFENSE_DATE"), isPaper);
        Map<String, byte[]> img = new HashMap<>();
        // 过程表：需要评委签名
        fillSignatures(img, stu, false, true);
        if (isPaper) {
            fillPaperScores(ph, records, factor, fs, stu);
        } else {
            fillDesignScores(ph, records, factor, fs, stu);
        }
        String filename = encode((isPaper ? "毕业论文答辩成绩无评语过程表-" : "毕业设计答辩成绩无评语过程表-") + stu.getName() + ".docx");
        String template = isPaper ? resolveTemplate("paper-process", PAPER_PROCESS_TEMPLATE, stu.getDepartmentId())
                : resolveTemplate("design-process", DESIGN_PROCESS_TEMPLATE, stu.getDepartmentId());
        byte[] doc = docTemplateService.renderDoc(template, ph, img);
        return attachment(doc, filename);
    }

    private Map<String, String> buildCommonPlaceholders(Student stu, DateParts dp, boolean isPaper) {
        Map<String, String> ph = new HashMap<>();
        ph.put("{{NAME}}", nvl(stu.getName()));
        ph.put("{{STUDENT_NO}}", nvl(stu.getStudentNo()));
        ph.put("{{TITLE}}", nvl(stu.getTitle()));
        ph.put("{{YEAR}}", dp.year);
        ph.put("{{MONTH}}", dp.month);
        ph.put("{{DAY}}", dp.day);
        if (!isPaper) {
            // 设计表有摘要/类型可按需加入
            ph.put("{{SUMMARY}}", nvl(stu.getSummary()));
        }
        return ph;
    }

    private void fillPaperScores(Map<String, String> ph, List<TeacherScoreRecord> records, double factor, StudentFinalScore fs, Student stu) {
        AvgScores avg = avgPaper(records);
        // 计算各个分项（应用调节系数）
        double item1Scaled = avg.item1Scaled(factor);
        double item2Scaled = avg.item2Scaled(factor);
        double item3Scaled = avg.item3Scaled(factor);
        
        // 总分应该等于各个分项的和（确保一致性）
        double totalFromItems = item1Scaled + item2Scaled + item3Scaled;
        
        // 获取评价指标配置
        List<com.example.defensemanagement.entity.EvaluationItem> items = configService.getEvaluationItems("PAPER");
        
        // 填充分数占位符
        ph.put("{{ITEM1}}", formatInt(item1Scaled));
        ph.put("{{ITEM2}}", formatInt(item2Scaled));
        ph.put("{{ITEM3}}", formatInt(item3Scaled));
        
        // 填充评价指标名称和描述占位符
        if (items != null && items.size() >= 3) {
            ph.put("{{ITEM1_NAME}}", items.get(0).getItemName() != null ? items.get(0).getItemName() : "论文质量");
            ph.put("{{ITEM1_DESC}}", items.get(0).getDescription() != null ? items.get(0).getDescription() : "");
            ph.put("{{ITEM1_MAX}}", items.get(0).getMaxScore() != null ? String.valueOf(items.get(0).getMaxScore()) : "50");
            
            ph.put("{{ITEM2_NAME}}", items.get(1).getItemName() != null ? items.get(1).getItemName() : "答辩的自述报告");
            ph.put("{{ITEM2_DESC}}", items.get(1).getDescription() != null ? items.get(1).getDescription() : "");
            ph.put("{{ITEM2_MAX}}", items.get(1).getMaxScore() != null ? String.valueOf(items.get(1).getMaxScore()) : "25");
            
            ph.put("{{ITEM3_NAME}}", items.get(2).getItemName() != null ? items.get(2).getItemName() : "回答问题的情况");
            ph.put("{{ITEM3_DESC}}", items.get(2).getDescription() != null ? items.get(2).getDescription() : "");
            ph.put("{{ITEM3_MAX}}", items.get(2).getMaxScore() != null ? String.valueOf(items.get(2).getMaxScore()) : "25");
        } else {
            // 默认值
            ph.put("{{ITEM1_NAME}}", "论文质量");
            ph.put("{{ITEM1_DESC}}", "");
            ph.put("{{ITEM1_MAX}}", "50");
            ph.put("{{ITEM2_NAME}}", "答辩的自述报告");
            ph.put("{{ITEM2_DESC}}", "");
            ph.put("{{ITEM2_MAX}}", "25");
            ph.put("{{ITEM3_NAME}}", "回答问题的情况");
            ph.put("{{ITEM3_DESC}}", "");
            ph.put("{{ITEM3_MAX}}", "25");
        }
        
        // 总分使用各个分项的和，确保一致性
        ph.put("{{TOTAL}}", format1(totalFromItems));
        ph.put("{{COMMENT}}", generateComment("PAPER_PROMPT", stu, totalFromItems, factor));
    }

    private void fillDesignScores(Map<String, String> ph, List<TeacherScoreRecord> records, double factor, StudentFinalScore fs, Student stu) {
        AvgScores avg = avgDesign(records);
        // 计算各个分项（应用调节系数）
        double item1Scaled = avg.item1Scaled(factor);
        double item2Scaled = avg.item2Scaled(factor);
        double item3Scaled = avg.item3Scaled(factor);
        double item4Scaled = avg.item4Scaled(factor);
        double item5Scaled = avg.item5Scaled(factor);
        double item6Scaled = avg.item6Scaled(factor);
        
        // 总分应该等于各个分项的和（确保一致性）
        double totalFromItems = item1Scaled + item2Scaled + item3Scaled + item4Scaled + item5Scaled + item6Scaled;
        
        // 获取评价指标配置
        List<com.example.defensemanagement.entity.EvaluationItem> items = configService.getEvaluationItems("DESIGN");
        
        // 填充分数占位符
        ph.put("{{ITEM1}}", formatInt(item1Scaled));
        ph.put("{{ITEM2}}", formatInt(item2Scaled));
        ph.put("{{ITEM3}}", formatInt(item3Scaled));
        ph.put("{{ITEM4}}", formatInt(item4Scaled));
        ph.put("{{ITEM5}}", formatInt(item5Scaled));
        ph.put("{{ITEM6}}", formatInt(item6Scaled));
        
        // 填充评价指标名称和描述占位符
        if (items != null && items.size() >= 6) {
            for (int i = 0; i < 6; i++) {
                com.example.defensemanagement.entity.EvaluationItem item = items.get(i);
                int itemNum = i + 1;
                ph.put("{{ITEM" + itemNum + "_NAME}}", item.getItemName() != null ? item.getItemName() : "指标" + itemNum);
                ph.put("{{ITEM" + itemNum + "_DESC}}", item.getDescription() != null ? item.getDescription() : "");
                ph.put("{{ITEM" + itemNum + "_MAX}}", item.getMaxScore() != null ? String.valueOf(item.getMaxScore()) : "15");
            }
        } else {
            // 默认值
            String[] defaultNames = {"设计质量1", "设计质量2", "设计质量3", "答辩的自述报告成绩", "回答问题的情况1", "回答问题的情况2"};
            int[] defaultMaxScores = {15, 15, 15, 25, 15, 15};
            for (int i = 0; i < 6; i++) {
                int itemNum = i + 1;
                ph.put("{{ITEM" + itemNum + "_NAME}}", defaultNames[i]);
                ph.put("{{ITEM" + itemNum + "_DESC}}", "");
                ph.put("{{ITEM" + itemNum + "_MAX}}", String.valueOf(defaultMaxScores[i]));
            }
        }
        
        // 总分使用各个分项的和，确保一致性
        ph.put("{{TOTAL}}", format1(totalFromItems));
        ph.put("{{COMMENT}}", generateComment("DESIGN_PROMPT", stu, totalFromItems, factor));
    }

    private AvgScores avgPaper(List<TeacherScoreRecord> records) {
        AvgScores s = new AvgScores();
        if (records == null || records.isEmpty()) return s;
        int n = 0;
        for (TeacherScoreRecord r : records) {
            if (r.getTotalScore() == null) continue;
            n++;
            s.item1 += nz(r.getItem1Score());
            s.item2 += nz(r.getItem2Score());
            s.item3 += nz(r.getItem3Score());
            s.total += nz(r.getTotalScore());
        }
        if (n > 0) s.divide(n);
        return s;
    }

    private AvgScores avgDesign(List<TeacherScoreRecord> records) {
        AvgScores s = new AvgScores();
        if (records == null || records.isEmpty()) return s;
        int n = 0;
        for (TeacherScoreRecord r : records) {
            if (r.getTotalScore() == null) continue;
            n++;
            s.item1 += nz(r.getItem1Score());
            s.item2 += nz(r.getItem2Score());
            s.item3 += nz(r.getItem3Score());
            s.item4 += nz(r.getItem4Score());
            s.item5 += nz(r.getItem5Score());
            s.item6 += nz(r.getItem6Score());
            s.total += nz(r.getTotalScore());
        }
        if (n > 0) s.divide(n);
        return s;
    }

    private DateParts getDateParts(String prefix) {
        String y = configService.getDefenseDatePart(prefix + "_YEAR");
        String m = configService.getDefenseDatePart(prefix + "_MONTH");
        String d = configService.getDefenseDatePart(prefix + "_DAY");
        LocalDate now = LocalDate.now();
        return new DateParts(nvl(y, String.valueOf(now.getYear())),
                nvl(m, String.valueOf(now.getMonthValue())),
                nvl(d, String.valueOf(now.getDayOfMonth())));
    }

    private ResponseEntity<byte[]> attachment(byte[] bytes, String filename) {
        MediaType octet = MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(octet)
                .body(bytes);
    }

    String encode(String name) {
        try {
            return URLEncoder.encode(name, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
        } catch (Exception e) {
            return name;
        }
    }

    private String nvl(String v) { return v == null ? "" : v; }

    private String nvl(String v, String def) { return StringUtils.hasText(v) ? v : def; }

    private int nz(Integer v) { return v == null ? 0 : v; }

    private String format1(double v) { return String.format(java.util.Locale.ROOT, "%.1f", v); }

    private String formatInt(double v) { return String.valueOf(Math.round(v)); }

    private String generateComment(String promptKey, Student stu, double scaledTotal, double factor) {
        String context = "学生:" + nvl(stu.getName()) +
                "，题目:" + nvl(stu.getTitle()) +
                "，摘要:" + nvl(stu.getSummary()) +
                "，调节系数:" + format1(factor) +
                "，小组加权总分:" + format1(scaledTotal);
        String resp = aiCommentService.generateComment(promptKey, context);
        return resp == null ? "" : resp;
    }

    /**
     * 填充签名图片
     * @param img 图片映射
     * @param stu 学生信息
     * @param isGradeForm 是否为成绩评定表（需要系主任签名）
     * @param isProcessForm 是否为过程表（需要评委签名）
     */
    private void fillSignatures(Map<String, byte[]> img, Student stu, boolean isGradeForm, boolean isProcessForm) {
        // 指导教师签名 - 对应占位符：{{SIGN_ADVISOR}} 或 {{SIGN_ADVISOR_TEACHER}}
        if (stu.getAdvisorTeacherId() != null) {
            byte[] bytes = loadSignature("teacher_" + stu.getAdvisorTeacherId());
            if (bytes != null) {
                img.put("{{SIGN_ADVISOR}}", bytes);
                img.put("{{SIGN_ADVISOR_TEACHER}}", bytes); // 兼容性占位符
                System.out.println("已加载指导教师签名: teacher_" + stu.getAdvisorTeacherId() + ", 大小: " + bytes.length);
            } else {
                System.out.println("警告：未找到指导教师签名: teacher_" + stu.getAdvisorTeacherId());
            }
        }
        
        // 评阅教师签名 - 对应占位符：{{SIGN_REVIEWER}} 或 {{SIGN_REVIEWER_TEACHER}}
        if (stu.getReviewerTeacherId() != null) {
            byte[] bytes = loadSignature("teacher_" + stu.getReviewerTeacherId());
            if (bytes != null) {
                img.put("{{SIGN_REVIEWER}}", bytes);
                img.put("{{SIGN_REVIEWER_TEACHER}}", bytes); // 兼容性占位符
                System.out.println("已加载评阅教师签名: teacher_" + stu.getReviewerTeacherId() + ", 大小: " + bytes.length);
            } else {
                System.out.println("警告：未找到评阅教师签名: teacher_" + stu.getReviewerTeacherId());
            }
        }
        
        // 答辩组长签名（用于答辩成绩表和成绩评定表）- 对应占位符：{{SIGN_LEADER}} 或 {{SIGN_GROUP_LEADER}}
        if (stu.getDefenseGroupId() != null) {
            try {
                DefenseGroupTeacher leader = defenseGroupTeacherMapper.findLeaderByGroupId(stu.getDefenseGroupId());
                if (leader != null && leader.getTeacherId() != null) {
                    byte[] bytes = loadSignature("teacher_" + leader.getTeacherId());
                    if (bytes != null) {
                        img.put("{{SIGN_LEADER}}", bytes);
                        img.put("{{SIGN_GROUP_LEADER}}", bytes); // 兼容性占位符
                        System.out.println("已加载答辩组长签名: teacher_" + leader.getTeacherId() + ", 大小: " + bytes.length);
                    } else {
                        System.out.println("警告：未找到答辩组长签名: teacher_" + leader.getTeacherId());
                    }
                } else {
                    System.out.println("警告：未找到答辩组长，小组ID: " + stu.getDefenseGroupId());
                }
            } catch (Exception e) {
                System.out.println("警告：加载答辩组长签名时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // 系主任签名（用于成绩评定表）- 对应占位符：{{SIGN_DEPT_HEAD}} 或 {{SIGN_DEAN}}
        if (isGradeForm && stu.getDepartmentId() != null) {
            try {
                System.out.println("开始查找系主任签名，学生院系ID: " + stu.getDepartmentId());
                // 查找该院系的院系管理员（DEPT_ADMIN角色）作为系主任
                List<com.example.defensemanagement.entity.User> deptAdmins = 
                    userService.getUsersByRole("DEPT_ADMIN");
                System.out.println("找到 " + (deptAdmins != null ? deptAdmins.size() : 0) + " 个院系管理员");
                
                boolean found = false;
                com.example.defensemanagement.entity.User fallbackAdmin = null; // 备选院系管理员
                
                if (deptAdmins != null) {
                    // 首先尝试查找匹配的院系管理员
                    for (com.example.defensemanagement.entity.User admin : deptAdmins) {
                        System.out.println("检查院系管理员: ID=" + admin.getId() + ", 用户名=" + admin.getUsername() + ", 院系ID=" + admin.getDepartmentId());
                        if (admin.getDepartmentId() != null && 
                            admin.getDepartmentId().equals(stu.getDepartmentId())) {
                            System.out.println("找到匹配的院系管理员: user_" + admin.getId() + ", 开始加载签名");
                            byte[] bytes = loadSignature("user_" + admin.getId());
                            if (bytes != null) {
                                img.put("{{SIGN_DEPT_HEAD}}", bytes);
                                img.put("{{SIGN_DEAN}}", bytes); // 兼容性占位符
                                System.out.println("已加载系主任签名: user_" + admin.getId() + ", 大小: " + bytes.length);
                                found = true;
                                break; // 只取第一个找到的
                            } else {
                                System.out.println("警告：未找到系主任签名文件: user_" + admin.getId());
                            }
                        }
                        // 记录第一个可用的院系管理员作为备选
                        if (fallbackAdmin == null && admin.getDepartmentId() != null) {
                            fallbackAdmin = admin;
                        }
                    }
                    
                    // 如果找不到匹配的院系管理员，使用第一个可用的作为备选
                    if (!found && fallbackAdmin != null) {
                        System.out.println("未找到匹配的院系管理员（学生院系ID: " + stu.getDepartmentId() + "），使用备选院系管理员: user_" + fallbackAdmin.getId());
                        byte[] bytes = loadSignature("user_" + fallbackAdmin.getId());
                        if (bytes != null) {
                            img.put("{{SIGN_DEPT_HEAD}}", bytes);
                            img.put("{{SIGN_DEAN}}", bytes); // 兼容性占位符
                            System.out.println("已加载备选系主任签名: user_" + fallbackAdmin.getId() + ", 大小: " + bytes.length);
                            found = true;
                        } else {
                            System.out.println("警告：未找到备选系主任签名文件: user_" + fallbackAdmin.getId());
                        }
                    }
                }
                
                if (!found) {
                    System.out.println("警告：未找到可用的系主任签名（学生院系ID: " + stu.getDepartmentId() + "）");
                }
            } catch (Exception e) {
                System.err.println("加载系主任签名时出错: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            if (!isGradeForm) {
                System.out.println("不是成绩评定表，跳过系主任签名");
            } else if (stu.getDepartmentId() == null) {
                System.out.println("学生院系ID为空，跳过系主任签名");
            }
        }
        
        // 评委签名（用于过程表，可能需要多个评委的签名）
        if (isProcessForm && stu.getDefenseGroupId() != null) {
            try {
                // 获取该小组的所有教师（评委）
                List<DefenseGroupTeacher> groupTeachers = 
                    defenseGroupTeacherMapper.findByGroupId(stu.getDefenseGroupId());
                int judgeIndex = 1;
                for (DefenseGroupTeacher groupTeacher : groupTeachers) {
                    if (groupTeacher.getTeacherId() != null) {
                        byte[] bytes = loadSignature("teacher_" + groupTeacher.getTeacherId());
                        if (bytes != null) {
                            // 支持多个评委签名：{{SIGN_JUDGE_1}}, {{SIGN_JUDGE_2}}, {{SIGN_JUDGE_3}}
                            img.put("{{SIGN_JUDGE_" + judgeIndex + "}}", bytes);
                            // 第一个评委也可以使用通用占位符
                            if (judgeIndex == 1) {
                                img.put("{{SIGN_JUDGE}}", bytes);
                            }
                            judgeIndex++;
                            // 最多支持3个评委签名
                            if (judgeIndex > 3) break;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
    }
    
    /**
     * 兼容旧版本的签名填充方法（默认不是成绩评定表，不是过程表）
     */
    private void fillSignatures(Map<String, byte[]> img, Student stu) {
        fillSignatures(img, stu, false, false);
    }

    byte[] loadSignature(String namePrefix) {
        String[] exts = {"png", "jpg", "jpeg"};
        java.nio.file.Path basePath = getUploadBasePath();
        
        // 首先尝试直接使用传入的前缀（如 teacher_1）
        for (String ext : exts) {
            java.nio.file.Path p = basePath.resolve("signatures").resolve(namePrefix + "." + ext);
            if (Files.exists(p)) {
                try {
                    byte[] bytes = Files.readAllBytes(p);
                    System.out.println("找到签名文件: " + p.toString() + ", 大小: " + bytes.length);
                    return bytes;
                } catch (Exception e) {
                    System.out.println("读取签名文件失败: " + p.toString() + ", 错误: " + e.getMessage());
                }
            } else {
                System.out.println("签名文件不存在: " + p.toString());
            }
        }
        
        // 如果是teacher_前缀但没找到签名，尝试查找对应的user_签名
        // 因为教师登录时可能以User身份登录，签名保存为user_{userId}格式
        if (namePrefix.startsWith("teacher_")) {
            try {
                Long teacherId = Long.parseLong(namePrefix.substring("teacher_".length()));
                Teacher teacher = teacherMapper.findById(teacherId);
                if (teacher != null && teacher.getUserId() != null) {
                    String userPrefix = "user_" + teacher.getUserId();
                    System.out.println("尝试查找user_格式签名: " + userPrefix);
                    for (String ext : exts) {
                        java.nio.file.Path p = basePath.resolve("signatures").resolve(userPrefix + "." + ext);
                        if (Files.exists(p)) {
                            try {
                                byte[] bytes = Files.readAllBytes(p);
                                System.out.println("找到user_格式签名文件: " + p.toString() + ", 大小: " + bytes.length);
                                return bytes;
                            } catch (Exception e) {
                                System.out.println("读取user_格式签名文件失败: " + p.toString() + ", 错误: " + e.getMessage());
                            }
                        } else {
                            System.out.println("user_格式签名文件不存在: " + p.toString());
                        }
                    }
                } else {
                    System.out.println("教师不存在或没有关联的userId: teacherId=" + teacherId);
                }
            } catch (NumberFormatException e) {
                System.out.println("解析teacherId失败: " + namePrefix);
            }
        }
        
        System.out.println("未找到签名: " + namePrefix);
        return null;
    }

    private String resolveTemplate(String key, String defaultClasspath) {
        return resolveTemplate(key, defaultClasspath, null);
    }

    /**
     * 模板解析优先级：
     * 1. 院系专属模板 uploads/templates/dept_{deptId}/{key}.docx（院系管理员上传）
     * 2. 全局模板     uploads/templates/{key}.docx（超级管理员上传）
     * 3. Classpath 默认模板（兜底）
     */
    String resolveTemplate(String key, String defaultClasspath, Long departmentId) {
        java.nio.file.Path basePath = getUploadBasePath();
        // 1. 优先查找院系专属模板
        if (departmentId != null) {
            java.nio.file.Path deptPath = basePath.resolve("templates")
                    .resolve("dept_" + departmentId).resolve(key + ".docx");
            if (Files.exists(deptPath)) {
                return deptPath.toAbsolutePath().toString();
            }
        }
        // 2. 查找全局模板
        java.nio.file.Path globalPath = basePath.resolve("templates").resolve(key + ".docx");
        if (Files.exists(globalPath)) {
            return globalPath.toAbsolutePath().toString();
        }
        // 3. 兜底 classpath 默认模板
        return defaultClasspath;
    }

    private static class DateParts {
        String year; String month; String day;
        DateParts(String y, String m, String d) { this.year = y; this.month = m; this.day = d; }
    }

    private static class AvgScores {
        double item1, item2, item3, item4, item5, item6, total;
        void divide(int n) {
            item1 /= n; item2 /= n; item3 /= n; item4 /= n; item5 /= n; item6 /= n; total /= n;
        }
        double item1Scaled(double f) { return item1 * f; }
        double item2Scaled(double f) { return item2 * f; }
        double item3Scaled(double f) { return item3 * f; }
        double item4Scaled(double f) { return item4 * f; }
        double item5Scaled(double f) { return item5 * f; }
        double item6Scaled(double f) { return item6 * f; }
        double totalScaled(double f) { return total * f; }
    }
}



