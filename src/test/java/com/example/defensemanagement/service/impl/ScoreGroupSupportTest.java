package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.Teacher;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoreGroupSupportTest {

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

    private ScoreGroupSupport support;

    @BeforeEach
    void setUp() {
        ScoreGroupQueries queries = new ScoreGroupQueries(teacherScoreRecordMapper, studentFinalScoreMapper, studentMapper);
        support = new ScoreGroupSupport(
                teacherScoreRecordMapper,
                studentFinalScoreMapper,
                studentMapper,
                largeGroupScoreMapper,
                defenseGroupMapper,
                defenseGroupTeacherMapper,
                queries);
    }

    @Test
    void getTeacherGroupStudentsIncludesTeacherScoresAndOwnScore() {
        DefenseGroupTeacher teacherGroup = new DefenseGroupTeacher();
        teacherGroup.setGroupId(9L);
        teacherGroup.setTeacherId(7L);
        teacherGroup.setIsLeader(1);
        when(defenseGroupTeacherMapper.findByTeacherId(7L)).thenReturn(teacherGroup);

        DefenseGroup group = new DefenseGroup();
        group.setId(9L);
        group.setName("A组");
        when(defenseGroupMapper.findById(9L)).thenReturn(group);

        Student student = new Student();
        student.setId(1L);
        student.setStudentNo("2026001");
        student.setName("学生甲");
        student.setDefenseType("PAPER");
        student.setDefenseYear(2026);
        when(studentMapper.findByDefenseGroupId(9L)).thenReturn(List.of(student));

        DefenseGroupTeacher member1 = new DefenseGroupTeacher();
        member1.setTeacherId(7L);
        DefenseGroupTeacher member2 = new DefenseGroupTeacher();
        member2.setTeacherId(8L);
        when(defenseGroupTeacherMapper.findByGroupId(9L)).thenReturn(List.of(member1, member2));

        TeacherScoreRecord score1 = teacherScore(1L, 7L, "李老师", "T007", 90);
        TeacherScoreRecord score2 = teacherScore(1L, 8L, "王老师", "T008", 84);
        when(teacherScoreRecordMapper.findByStudentIdsAndYear(anyList(), eq(2026)))
                .thenReturn(List.of(score1, score2));
        when(largeGroupScoreMapper.findByStudentIdAndYear(1L, 2026)).thenReturn(List.of());

        Map<String, Object> result = support.getTeacherGroupStudents(7L, 2026);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> students = (List<Map<String, Object>>) result.get("students");
        assertEquals(1, students.size());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> teacherScores = (List<Map<String, Object>>) students.get(0).get("teacherScores");
        assertEquals(2, teacherScores.size());
        assertEquals("李老师", teacherScores.get(0).get("teacherName"));
        TeacherScoreRecord myScore = (TeacherScoreRecord) students.get(0).get("myScore");
        assertEquals(90, myScore.getTotalScore());
        assertEquals(true, students.get(0).get("allScored"));
        assertNull(students.get(0).get("adjustmentFactor"));
    }

    @Test
    void getAllGroupStudentsForSuperAdminIncludesTeacherScoreEntries() {
        DefenseGroup group = new DefenseGroup();
        group.setId(9L);
        group.setName("A组");
        when(defenseGroupMapper.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(group));

        Student student = new Student();
        student.setId(1L);
        student.setStudentNo("2026001");
        student.setName("学生甲");
        student.setDefenseType("PAPER");
        student.setDefenseYear(2026);
        when(studentMapper.findByDefenseGroupId(9L)).thenReturn(List.of(student));

        DefenseGroupTeacher member = new DefenseGroupTeacher();
        member.setTeacherId(7L);
        when(defenseGroupTeacherMapper.findByGroupId(9L)).thenReturn(List.of(member));

        TeacherScoreRecord score = teacherScore(1L, 7L, "李老师", "T007", 90);
        when(teacherScoreRecordMapper.findByStudentIdsAndYear(anyList(), eq(2026)))
                .thenReturn(List.of(score));
        when(studentFinalScoreMapper.findByStudentIdsAndYear(anyList(), eq(2026))).thenReturn(List.of());

        Map<String, Object> result = support.getAllGroupStudentsForSuperAdmin(2026);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> groups = (List<Map<String, Object>>) result.get("groups");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> students = (List<Map<String, Object>>) groups.get(0).get("students");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> teacherScores = (List<Map<String, Object>>) students.get(0).get("teacherScores");
        assertEquals(1, teacherScores.size());
        assertEquals(90, teacherScores.get(0).get("totalScore"));
    }

    private TeacherScoreRecord teacherScore(Long studentId, Long teacherId, String teacherName, String teacherNo, int totalScore) {
        TeacherScoreRecord record = new TeacherScoreRecord();
        record.setStudentId(studentId);
        record.setTeacherId(teacherId);
        record.setTotalScore(totalScore);
        Teacher teacher = new Teacher();
        teacher.setId(teacherId);
        teacher.setName(teacherName);
        teacher.setTeacherNo(teacherNo);
        record.setTeacher(teacher);
        return record;
    }
}
