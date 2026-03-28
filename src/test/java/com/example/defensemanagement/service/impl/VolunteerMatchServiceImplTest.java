package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.common.MatchScoreDetail;
import com.example.defensemanagement.common.RelevanceAnalysisResult;
import com.example.defensemanagement.common.StudentMaterialContext;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentPreference;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.TeacherProfile;
import com.example.defensemanagement.service.AiCommentService;
import com.example.defensemanagement.service.VolunteerMaterialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VolunteerMatchServiceImplTest {

    @Mock
    private AiCommentService aiCommentService;
    @Mock
    private VolunteerMaterialService volunteerMaterialService;

    private VolunteerMatchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VolunteerMatchServiceImpl();
        ReflectionTestUtils.setField(service, "aiCommentService", aiCommentService);
        ReflectionTestUtils.setField(service, "volunteerMaterialService", volunteerMaterialService);
    }

    @Test
    void firstChoiceGetsHigherUtilityThanThirdChoiceWithSameCapacityAndRelevance() {
        StudentPreference preference = new StudentPreference();
        preference.setChoice1TeacherId(1L);
        preference.setChoice3TeacherId(3L);

        Student student = student("基于机器学习的图像识别", "机器学习 图像");
        TeacherProfile profile = teacherProfile(99L, "机器学习", "图像");
        when(volunteerMaterialService.buildStudentContext(student, preference, 1L))
                .thenReturn(new StudentMaterialContext("题目：基于机器学习的图像识别\n材料内容：机器学习 图像", StudentMaterialContext.SOURCE_PREFERRED_PDF, "teacher_specific_pdf"));
        when(volunteerMaterialService.buildStudentContext(student, preference, 3L))
                .thenReturn(new StudentMaterialContext("题目：基于机器学习的图像识别\n材料内容：机器学习 图像", StudentMaterialContext.SOURCE_PREFERRED_PDF, "teacher_specific_pdf"));
        when(aiCommentService.analyzeRelevance("题目：基于机器学习的图像识别\n材料内容：机器学习 图像", "机器学习\n图像"))
                .thenReturn(new RelevanceAnalysisResult(90.0, RelevanceAnalysisResult.SOURCE_MODEL, "model_success"));

        double firstChoiceScore = service.calculateMatchScore(preference, student, teacher(1L), profile, 3, 1, 4, new HashMap<>());
        double thirdChoiceScore = service.calculateMatchScore(preference, student, teacher(3L), profile, 3, 1, 4, new HashMap<>());

        assertTrue(firstChoiceScore > thirdChoiceScore);
    }

    @Test
    void relevanceScoreIsCachedForSameStudentAndTeacherPair() {
        Student student = student("大数据分析平台", "大数据 可视化");
        Teacher teacher = teacher(8L);
        TeacherProfile profile = teacherProfile(8L, "大数据", "可视化");
        Map<String, RelevanceAnalysisResult> relevanceCache = new HashMap<>();
        when(volunteerMaterialService.buildStudentContext(student, null, 8L))
                .thenReturn(new StudentMaterialContext("题目：大数据分析平台\n摘要：大数据 可视化", StudentMaterialContext.SOURCE_METADATA_FALLBACK, "pdf_not_uploaded"));
        when(aiCommentService.analyzeRelevance("题目：大数据分析平台\n摘要：大数据 可视化", "大数据\n可视化"))
                .thenReturn(new RelevanceAnalysisResult(88.0, RelevanceAnalysisResult.SOURCE_MODEL, "model_success"));

        double first = service.calculateMatchScore(null, student, teacher, profile, 2, 0, 4, relevanceCache);
        double second = service.calculateMatchScore(null, student, teacher, profile, 2, 0, 4, relevanceCache);

        assertEquals(first, second);
        verify(aiCommentService).analyzeRelevance("题目：大数据分析平台\n摘要：大数据 可视化", "大数据\n可视化");
    }

    @Test
    void matchDetailCarriesRelevanceSourceAndReason() {
        Student student = student("智能问答系统", "自然语言处理");
        Teacher teacher = teacher(9L);
        TeacherProfile profile = teacherProfile(9L, "自然语言处理", "有机器学习基础");
        when(volunteerMaterialService.buildStudentContext(student, null, 9L))
                .thenReturn(new StudentMaterialContext("题目：智能问答系统\n摘要：自然语言处理", StudentMaterialContext.SOURCE_METADATA_FALLBACK, "pdf_not_uploaded"));
        when(aiCommentService.analyzeRelevance("题目：智能问答系统\n摘要：自然语言处理", "自然语言处理\n有机器学习基础"))
                .thenReturn(new RelevanceAnalysisResult(66.0, RelevanceAnalysisResult.SOURCE_FALLBACK, "api_key_missing"));

        MatchScoreDetail detail = service.calculateMatchDetail(null, student, teacher, profile, 2, 0, 4, new HashMap<>());

        assertEquals(66.0, detail.getRelevanceScore());
        assertEquals(RelevanceAnalysisResult.SOURCE_FALLBACK, detail.getRelevanceSource());
        assertEquals("api_key_missing", detail.getRelevanceReason());
        assertEquals(StudentMaterialContext.SOURCE_METADATA_FALLBACK, detail.getMaterialSource());
        assertEquals("pdf_not_uploaded", detail.getMaterialReason());
    }

    private Student student(String title, String summary) {
        Student student = new Student();
        student.setId(1L);
        student.setTitle(title);
        student.setSummary(summary);
        student.setDefenseType("DESIGN");
        return student;
    }

    private Teacher teacher(Long id) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        return teacher;
    }

    private TeacherProfile teacherProfile(Long teacherId, String direction, String requirements) {
        TeacherProfile profile = new TeacherProfile();
        profile.setTeacherId(teacherId);
        profile.setResearchDirection(direction);
        profile.setEnrollmentRequirements(requirements);
        return profile;
    }
}
