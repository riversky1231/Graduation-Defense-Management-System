package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.LargeGroupScoreMapper;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoreServiceImplAdjustmentTest {

    @Mock
    private TeacherScoreRecordMapper teacherScoreRecordMapper;
    @Mock
    private StudentFinalScoreMapper studentFinalScoreMapper;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private LargeGroupScoreMapper largeGroupScoreMapper;
    @Mock
    private DefenseGroupMapper defenseGroupMapper;
    @Mock
    private DefenseGroupTeacherMapper defenseGroupTeacherMapper;

    private ScoreServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ScoreServiceImpl();
        ScoreGroupQueries queries = new ScoreGroupQueries(teacherScoreRecordMapper, studentFinalScoreMapper, studentMapper);
        ScoreGroupSupport support = new ScoreGroupSupport(
                teacherScoreRecordMapper,
                studentFinalScoreMapper,
                studentMapper,
                largeGroupScoreMapper,
                defenseGroupMapper,
                defenseGroupTeacherMapper,
                queries);
        ReflectionTestUtils.setField(service, "teacherScoreRecordMapper", teacherScoreRecordMapper);
        ReflectionTestUtils.setField(service, "studentFinalScoreMapper", studentFinalScoreMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "scoreGroupSupport", support);
    }

    @Test
    void finalizeGroupScoresAppliesAdjustmentFactorAndRefreshesTotalGrades() {
        Student student1 = student(1L);
        Student student2 = student(2L);
        when(studentMapper.findByDefenseGroupId(9L)).thenReturn(List.of(student1, student2));

        // Batch query for teacher scores (new code path)
        when(teacherScoreRecordMapper.findByStudentIdsAndYear(anyList(), eq(2026)))
                .thenReturn(List.of(score(1L, 80), score(1L, 100), score(2L, 70), score(2L, 70)));

        // Batch query for final scores — returns existing records with advisor/reviewer
        Map<Long, StudentFinalScore> existingFinalScores = new HashMap<>();
        existingFinalScores.put(1L, finalScore(1L, 80, 85));
        existingFinalScores.put(2L, finalScore(2L, 75, 80));
        when(studentFinalScoreMapper.findByStudentIdsAndYear(anyList(), eq(2026)))
                .thenReturn(List.of(existingFinalScores.get(1L), existingFinalScores.get(2L)));

        service.finalizeGroupScores(9L, 2026, 95);

        assertEquals(90, existingFinalScores.get(1L).getGroupAvgScore());
        assertEquals(70, existingFinalScores.get(2L).getGroupAvgScore());
        assertEquals(1.056, existingFinalScores.get(1L).getAdjustmentFactor());
        assertEquals(1.056, existingFinalScores.get(2L).getAdjustmentFactor());
        assertEquals(95.0, existingFinalScores.get(1L).getFinalDefenseScore());
        assertEquals(73.9, existingFinalScores.get(2L).getFinalDefenseScore());
        assertEquals(87.5, existingFinalScores.get(1L).getTotalGrade());
        assertEquals(76.1, existingFinalScores.get(2L).getTotalGrade());
        assertEquals(95, existingFinalScores.get(1L).getLargeGroupScore());
        assertEquals(95, existingFinalScores.get(2L).getLargeGroupScore());
    }

    private Student student(Long id) {
        Student student = new Student();
        student.setId(id);
        return student;
    }

    private TeacherScoreRecord score(Long studentId, int total) {
        TeacherScoreRecord record = new TeacherScoreRecord();
        record.setStudentId(studentId);
        record.setTotalScore(total);
        return record;
    }

    private StudentFinalScore finalScore(Long studentId, int advisorScore, int reviewerScore) {
        StudentFinalScore score = new StudentFinalScore();
        score.setStudentId(studentId);
        score.setYear(2026);
        score.setAdvisorScore(advisorScore);
        score.setReviewerScore(reviewerScore);
        return score;
    }
}
