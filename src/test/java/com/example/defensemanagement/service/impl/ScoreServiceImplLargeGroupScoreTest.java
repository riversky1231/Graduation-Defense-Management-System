package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.LargeGroupScore;
import com.example.defensemanagement.entity.Student;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoreServiceImplLargeGroupScoreTest {

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
        ReflectionTestUtils.setField(service, "largeGroupScoreMapper", largeGroupScoreMapper);
        ReflectionTestUtils.setField(service, "defenseGroupMapper", defenseGroupMapper);
        ReflectionTestUtils.setField(service, "defenseGroupTeacherMapper", defenseGroupTeacherMapper);
        ReflectionTestUtils.setField(service, "scoreGroupSupport", support);
    }

    @Test
    void updateLargeGroupScoreUsesScoreIdLookupBeforeTupleLookup() {
        LargeGroupScore existing = new LargeGroupScore();
        existing.setId(5L);
        existing.setStudentId(12L);
        existing.setTeacherId(7L);
        existing.setYear(2026);
        existing.setScore(90);
        when(largeGroupScoreMapper.findById(5L)).thenReturn(existing);
        Student student = new Student();
        student.setId(12L);
        when(studentMapper.findById(12L)).thenReturn(student);

        service.updateLargeGroupScore(5L, 12L, 7L, 2026, 96);

        assertEquals(96, existing.getScore());
        verify(largeGroupScoreMapper).findById(5L);
        verify(largeGroupScoreMapper, never()).findByStudentIdAndTeacherIdAndYear(any(), any(), any());
        verify(largeGroupScoreMapper).update(existing);
    }

    @Test
    void updateLargeGroupScoreRejectsMismatchedScoreIdAndTuple() {
        LargeGroupScore existing = new LargeGroupScore();
        existing.setId(5L);
        existing.setStudentId(12L);
        existing.setTeacherId(8L);
        existing.setYear(2026);
        when(largeGroupScoreMapper.findById(5L)).thenReturn(existing);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateLargeGroupScore(5L, 12L, 7L, 2026, 96));

        assertEquals("打分记录与请求参数不匹配", exception.getMessage());
    }

    @Test
    void saveLargeGroupScoreRejectsOutOfRangeValues() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.saveLargeGroupScore(12L, 7L, 2026, 101));

        assertEquals("分数必须在0-100之间", exception.getMessage());
        verify(largeGroupScoreMapper, never()).findByStudentIdAndTeacherIdAndYear(any(), any(), any());
    }
}
