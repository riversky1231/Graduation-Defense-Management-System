package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.TeacherScoreRecord;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoreServiceImplAdjustmentTest {

    @Mock
    private TeacherScoreRecordMapper teacherScoreRecordMapper;
    @Mock
    private StudentFinalScoreMapper studentFinalScoreMapper;
    @Mock
    private StudentMapper studentMapper;

    private ScoreServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ScoreServiceImpl();
        ReflectionTestUtils.setField(service, "teacherScoreRecordMapper", teacherScoreRecordMapper);
        ReflectionTestUtils.setField(service, "studentFinalScoreMapper", studentFinalScoreMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
    }

    @Test
    void finalizeGroupScoresAppliesAdjustmentFactorAndRefreshesTotalGrades() {
        Student student1 = student(1L);
        Student student2 = student(2L);
        when(studentMapper.findByDefenseGroupId(9L)).thenReturn(List.of(student1, student2));
        when(teacherScoreRecordMapper.findByStudentIdAndYear(1L, 2026)).thenReturn(List.of(score(80), score(100)));
        when(teacherScoreRecordMapper.findByStudentIdAndYear(2L, 2026)).thenReturn(List.of(score(70), score(70)));

        Map<Long, StudentFinalScore> finalScores = new HashMap<>();
        finalScores.put(1L, finalScore(1L, 80, 85));
        finalScores.put(2L, finalScore(2L, 75, 80));
        when(studentFinalScoreMapper.findByStudentIdAndYear(anyLong(), eq(2026)))
                .thenAnswer(invocation -> finalScores.get(invocation.getArgument(0)));

        service.finalizeGroupScores(9L, 2026, 95);

        assertEquals(90, finalScores.get(1L).getGroupAvgScore());
        assertEquals(70, finalScores.get(2L).getGroupAvgScore());
        assertEquals(1.056, finalScores.get(1L).getAdjustmentFactor());
        assertEquals(1.056, finalScores.get(2L).getAdjustmentFactor());
        assertEquals(95.04, finalScores.get(1L).getFinalDefenseScore());
        assertEquals(73.92, finalScores.get(2L).getFinalDefenseScore());
        assertEquals(87.5, finalScores.get(1L).getTotalGrade());
        assertEquals(76.1, finalScores.get(2L).getTotalGrade());
        assertEquals(95, finalScores.get(1L).getLargeGroupScore());
        assertEquals(95, finalScores.get(2L).getLargeGroupScore());
    }

    private Student student(Long id) {
        Student student = new Student();
        student.setId(id);
        return student;
    }

    private TeacherScoreRecord score(int total) {
        TeacherScoreRecord record = new TeacherScoreRecord();
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
