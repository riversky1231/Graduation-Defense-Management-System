package com.example.defensemanagement.controller.export;

import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.service.DocTemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小组统分表导出 Controller。
 */
@RestController
@RequestMapping("/export")
public class GroupSummaryExportController extends AbstractGroupExportController {

    private static final String GROUP_SUMMARY_TEMPLATE = "templates/docx/group-summary.docx";

    private final DocTemplateService docTemplateService;
    private final DefenseGroupMapper defenseGroupMapper;
    private final GroupSummaryExportSupport groupSummaryExportSupport;

    public GroupSummaryExportController(
            PaperExportController paperExportController,
            DesignExportController designExportController,
            StudentMapper studentMapper,
            DocTemplateService docTemplateService,
            DefenseGroupMapper defenseGroupMapper,
            GroupSummaryExportSupport groupSummaryExportSupport) {
        super(paperExportController, designExportController, studentMapper);
        this.docTemplateService = docTemplateService;
        this.defenseGroupMapper = defenseGroupMapper;
        this.groupSummaryExportSupport = groupSummaryExportSupport;
    }

    @GetMapping("/group/{groupId}/summary")
    public ResponseEntity<byte[]> exportGroupSummary(@PathVariable Long groupId) {
        List<Student> students = getGroupStudents(groupId);
        Integer year = students.get(0).getDefenseYear();
        if (year == null) {
            throw new RuntimeException("学生未设置答辩年份");
        }

        DefenseGroup group = defenseGroupMapper.findById(groupId);
        String groupName = group != null ? group.getName() : "小组" + groupId;
        GroupSummaryExportSupport.PreparedGroupSummary summary = groupSummaryExportSupport.prepareGroupSummary(
                groupId,
                students,
                groupName,
                year);

        Long departmentId = students.get(0).getDepartmentId();
        byte[] document = docTemplateService.renderDoc(
                paperExportController.resolveTemplate("group-summary", GROUP_SUMMARY_TEMPLATE, departmentId),
                summary.getPlaceholders(),
                summary.getImages());
        String filename = paperExportController.encode("毕业论文(设计)答辩小组统分表-" + groupName + ".docx");
        return attachment(document, filename);
    }
}
