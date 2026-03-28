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
import com.example.defensemanagement.service.VolunteerMatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class VolunteerMatchServiceImpl implements VolunteerMatchService {

    private static final double WEIGHT_PREFERENCE = 0.45;
    private static final double WEIGHT_CAPACITY = 0.20;
    private static final double WEIGHT_BALANCE = 0.20;
    private static final double WEIGHT_RELEVANCE = 0.15;

    @Autowired
    private AiCommentService aiCommentService;

    @Autowired
    private VolunteerMaterialService volunteerMaterialService;

    @Override
    public MatchScoreDetail calculateMatchDetail(StudentPreference preference,
                                                 Student student,
                                                 Teacher teacher,
                                                 TeacherProfile teacherProfile,
                                                 int remainingCapacity,
                                                 int assignedCount,
                                                 int maxStudents,
                                                 Map<String, RelevanceAnalysisResult> relevanceCache) {
        if (teacher == null) {
            return new MatchScoreDetail(Double.NEGATIVE_INFINITY, 0.0, 0.0, 0.0, 0.0,
                    RelevanceAnalysisResult.SOURCE_EMPTY_INPUT, "teacher_missing",
                    StudentMaterialContext.SOURCE_EMPTY, "teacher_missing");
        }

        double preferenceScore = getPreferencePriorityScore(preference, teacher.getId());
        double capacityScore = maxStudents > 0 ? (remainingCapacity * 100.0 / maxStudents) : 0.0;
        double balanceScore = maxStudents > 0
                ? Math.max(0.0, (1.0 - assignedCount * 1.0 / maxStudents) * 100.0)
                : 0.0;
        StudentMaterialContext material = volunteerMaterialService.buildStudentContext(student, preference, teacher.getId());
        RelevanceAnalysisResult relevance = getRelevanceAnalysis(material, teacherProfile, relevanceCache, teacher);
        double relevanceScore = relevance.getScore();
        double totalScore = WEIGHT_PREFERENCE * preferenceScore
                + WEIGHT_CAPACITY * capacityScore
                + WEIGHT_BALANCE * balanceScore
                + WEIGHT_RELEVANCE * relevanceScore;

        return new MatchScoreDetail(totalScore,
                preferenceScore,
                capacityScore,
                balanceScore,
                relevanceScore,
                relevance.getSource(),
                relevance.getReason(),
                material.getSource(),
                material.getReason());
    }

    private double getPreferencePriorityScore(StudentPreference preference, Long teacherId) {
        if (preference == null || teacherId == null) {
            return 10.0;
        }
        if (teacherId.equals(preference.getChoice1TeacherId())) {
            return 100.0;
        }
        if (teacherId.equals(preference.getChoice2TeacherId())) {
            return 70.0;
        }
        if (teacherId.equals(preference.getChoice3TeacherId())) {
            return 40.0;
        }
        return 10.0;
    }

    private RelevanceAnalysisResult getRelevanceAnalysis(StudentMaterialContext studentMaterial,
                                                         TeacherProfile teacherProfile,
                                                         Map<String, RelevanceAnalysisResult> relevanceCache,
                                                         Teacher teacher) {
        if (studentMaterial == null || teacherProfile == null || teacher == null) {
            return new RelevanceAnalysisResult(0.0, RelevanceAnalysisResult.SOURCE_EMPTY_INPUT, "context_missing");
        }

        String teacherContext = joinTeacherContext(teacherProfile);
        if (teacherContext.isBlank()) {
            return new RelevanceAnalysisResult(0.0, RelevanceAnalysisResult.SOURCE_EMPTY_INPUT, "teacher_context_blank");
        }

        String studentContext = studentMaterial.getContent() == null ? "" : studentMaterial.getContent().trim();
        if (studentContext.isBlank()) {
            return new RelevanceAnalysisResult(0.0, RelevanceAnalysisResult.SOURCE_EMPTY_INPUT, "student_context_blank");
        }

        String cacheKey = studentContext.hashCode() + ":" + teacher.getId();
        if (relevanceCache != null && relevanceCache.containsKey(cacheKey)) {
            return relevanceCache.get(cacheKey);
        }

        RelevanceAnalysisResult score = aiCommentService.analyzeRelevance(studentContext, teacherContext);
        if (relevanceCache != null) {
            relevanceCache.put(cacheKey, score);
        }
        return score;
    }

    private String joinTeacherContext(TeacherProfile teacherProfile) {
        String researchDirection = teacherProfile.getResearchDirection() == null ? "" : teacherProfile.getResearchDirection();
        String enrollmentRequirements = teacherProfile.getEnrollmentRequirements() == null ? "" : teacherProfile.getEnrollmentRequirements();
        return (researchDirection + "\n" + enrollmentRequirements).trim();
    }
}
