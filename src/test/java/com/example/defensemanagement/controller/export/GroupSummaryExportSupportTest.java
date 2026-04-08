package com.example.defensemanagement.controller.export;

import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupSummaryExportSupportTest {

    @Mock
    private TeacherScoreRecordMapper teacherScoreRecordMapper;
    @Mock
    private StudentFinalScoreMapper studentFinalScoreMapper;
    @Mock
    private DefenseGroupTeacherMapper defenseGroupTeacherMapper;
    @Mock
    private ConfigService configService;
    @Mock
    private UserService userService;
    @Mock
    private SignatureHelper signatureHelper;

    @Test
    void prepareGroupSummaryBuildsPlaceholdersAndImages() {
        PaperExportController paperExportController = new PaperExportController();
        ReflectionTestUtils.setField(paperExportController, "signatureHelper", signatureHelper);
        GroupSummaryExportSupport support = new GroupSummaryExportSupport(
                teacherScoreRecordMapper,
                studentFinalScoreMapper,
                defenseGroupTeacherMapper,
                configService,
                userService,
                paperExportController);
        Student student = new Student();
        student.setId(11L);
        student.setName("张三");
        student.setDepartmentId(8L);

        TeacherScoreRecord judge1 = new TeacherScoreRecord();
        judge1.setTotalScore(80);
        TeacherScoreRecord judge2 = new TeacherScoreRecord();
        judge2.setTotalScore(90);
        StudentFinalScore finalScore = new StudentFinalScore();
        finalScore.setAdjustmentFactor(1.1);

        DefenseGroupTeacher leader = new DefenseGroupTeacher();
        leader.setTeacherId(7L);
        DefenseGroupTeacher judge = new DefenseGroupTeacher();
        judge.setTeacherId(8L);
        Department department = new Department();
        department.setId(8L);
        department.setName("计算机学院");

        when(configService.getDefenseDatePart("DEFENSE_DATE_YEAR")).thenReturn("2026");
        when(configService.getDefenseDatePart("DEFENSE_DATE_MONTH")).thenReturn("6");
        when(configService.getDefenseDatePart("DEFENSE_DATE_DAY")).thenReturn("18");
        when(teacherScoreRecordMapper.findByStudentIdAndYear(11L, 2026)).thenReturn(List.of(judge1, judge2));
        when(studentFinalScoreMapper.findByStudentIdAndYear(11L, 2026)).thenReturn(finalScore);
        when(defenseGroupTeacherMapper.findLeaderByGroupId(5L)).thenReturn(leader);
        when(defenseGroupTeacherMapper.findByGroupId(5L)).thenReturn(List.of(leader, judge));
        when(paperExportController.loadSignature("teacher_7")).thenReturn(new byte[]{1});
        when(paperExportController.loadSignature("teacher_8")).thenReturn(new byte[]{2});
        when(userService.getAllDepartments()).thenReturn(List.of(department));

        GroupSummaryExportSupport.PreparedGroupSummary result =
                support.prepareGroupSummary(5L, List.of(student), "第一组", 2026);

        assertEquals("2026", result.getPlaceholders().get("{{YEAR}}"));
        assertEquals("第一组", result.getPlaceholders().get("{{GROUP_NAME}}"));
        assertEquals("计算机学院", result.getPlaceholders().get("{{DEPT_NAME}}"));
        assertEquals("2", result.getPlaceholders().get("{{MAX_JUDGES}}"));
        assertEquals("80", result.getPlaceholders().get("{{JUDGE_1_SCORE_1}}"));
        assertEquals("90", result.getPlaceholders().get("{{JUDGE_2_SCORE_1}}"));
        assertEquals("85.0", result.getPlaceholders().get("{{DEFENSE_SCORE_1}}"));
        assertEquals("1.100", result.getPlaceholders().get("{{FACTOR_1}}"));
        assertEquals("93.5", result.getPlaceholders().get("{{FINAL_SCORE_1}}"));
        assertEquals("80、90", result.getPlaceholders().get("{{ROW_1_SCORES}}"));
        assertArrayEquals(new byte[]{1}, result.getImages().get("{{SIGN_LEADER}}"));
        assertArrayEquals(new byte[]{2}, result.getImages().get("{{SIGN_JUDGE_1}}"));
        assertTrue(result.getImages().containsKey("{{SIGN_GROUP_LEADER}}"));
    }
}
