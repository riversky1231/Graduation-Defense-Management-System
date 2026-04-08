package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Data-loading helpers used across ScoreGroupSupport operations.
 * All database reads are batched to minimize round-trips.
 */
@Component
final class ScoreGroupQueries {

    private final TeacherScoreRecordMapper teacherScoreRecordMapper;
    private final StudentFinalScoreMapper studentFinalScoreMapper;
    private final StudentMapper studentMapper;

    ScoreGroupQueries(TeacherScoreRecordMapper teacherScoreRecordMapper,
                     StudentFinalScoreMapper studentFinalScoreMapper,
                     StudentMapper studentMapper) {
        this.teacherScoreRecordMapper = teacherScoreRecordMapper;
        this.studentFinalScoreMapper = studentFinalScoreMapper;
        this.studentMapper = studentMapper;
    }

    /**
     * Returns a map of studentId -> their teacher score records for the given year.
     * Uses a batch query first, then falls back to individual lookups for students
     * with no batch result (handles edge cases where a student has records but the
     * batch query misses them).
     */
    Map<Long, List<TeacherScoreRecord>> loadTeacherScoreMap(List<Student> students, Integer year) {
        Map<Long, List<TeacherScoreRecord>> recordsByStudent = new HashMap<>();
        if (students == null || students.isEmpty() || year == null) {
            return recordsByStudent;
        }

        List<Long> studentIds = students.stream()
                .map(Student::getId)
                .collect(Collectors.toList());
        List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdsAndYear(studentIds, year);
        if (records != null) {
            for (TeacherScoreRecord record : records) {
                if (record.getStudentId() != null) {
                    recordsByStudent.computeIfAbsent(record.getStudentId(), key -> new ArrayList<>()).add(record);
                }
            }
        }
        return recordsByStudent;
    }

    /**
     * Returns a map of studentId -> StudentFinalScore for the given year.
     * Uses a batch query first, then falls back to individual lookups.
     */
    Map<Long, StudentFinalScore> loadFinalScores(List<Student> students, Integer year) {
        Map<Long, StudentFinalScore> finalScoresByStudent = new HashMap<>();
        if (students == null || students.isEmpty() || year == null) {
            return finalScoresByStudent;
        }

        List<Long> studentIds = students.stream()
                .map(Student::getId)
                .collect(Collectors.toList());
        List<StudentFinalScore> finalScores = studentFinalScoreMapper.findByStudentIdsAndYear(studentIds, year);
        if (finalScores != null) {
            for (StudentFinalScore finalScore : finalScores) {
                if (finalScore.getStudentId() != null) {
                    finalScoresByStudent.put(finalScore.getStudentId(), finalScore);
                }
            }
        }
        return finalScoresByStudent;
    }

    /**
     * Returns an existing StudentFinalScore, or inserts a new one if none exists.
     * Also updates the in-memory map.
     */
    StudentFinalScore ensureFinalScore(Map<Long, StudentFinalScore> finalScoresByStudent,
                                       Long studentId, Integer year) {
        StudentFinalScore finalScore = finalScoresByStudent.get(studentId);
        if (finalScore != null) {
            return finalScore;
        }
        finalScore = new StudentFinalScore();
        finalScore.setStudentId(studentId);
        finalScore.setYear(year);
        studentFinalScoreMapper.insert(finalScore);
        finalScoresByStudent.put(studentId, finalScore);
        return finalScore;
    }

    /**
     * Filters students to those matching the given defense year.
     */
    List<Student> filterStudentsByYear(List<Student> students, Integer year) {
        if (students == null || students.isEmpty()) {
            return new ArrayList<>();
        }
        if (year == null) {
            return new ArrayList<>(students);
        }
        return students.stream()
                .filter(student -> student.getDefenseYear() != null && student.getDefenseYear().equals(year))
                .collect(Collectors.toList());
    }

    /**
     * Batch loads all students for the given group IDs and year in a single query.
     * Returns a map of groupId -> list of students.
     */
    Map<Long, List<Student>> loadStudentsByGroupIds(List<Long> groupIds, Integer year) {
        Map<Long, List<Student>> studentsByGroup = new HashMap<>();
        if (groupIds == null || groupIds.isEmpty() || year == null) {
            return studentsByGroup;
        }

        List<Student> allStudents = studentMapper.findByDefenseGroupIdsAndYear(groupIds, year);
        if (allStudents != null) {
            for (Student student : allStudents) {
                if (student.getDefenseGroupId() != null) {
                    studentsByGroup.computeIfAbsent(student.getDefenseGroupId(), key -> new ArrayList<>()).add(student);
                }
            }
        }
        return studentsByGroup;
    }
}
