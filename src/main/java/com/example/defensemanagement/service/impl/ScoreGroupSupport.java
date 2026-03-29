package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.LargeGroupScore;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.LargeGroupScoreMapper;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

final class ScoreGroupSupport {

    private static final Logger log = LoggerFactory.getLogger(ScoreGroupSupport.class);

    private final TeacherScoreRecordMapper teacherScoreRecordMapper;
    private final StudentFinalScoreMapper studentFinalScoreMapper;
    private final StudentMapper studentMapper;
    private final LargeGroupScoreMapper largeGroupScoreMapper;
    private final DefenseGroupMapper defenseGroupMapper;
    private final DefenseGroupTeacherMapper defenseGroupTeacherMapper;

    ScoreGroupSupport(TeacherScoreRecordMapper teacherScoreRecordMapper,
                      StudentFinalScoreMapper studentFinalScoreMapper,
                      StudentMapper studentMapper,
                      LargeGroupScoreMapper largeGroupScoreMapper,
                      DefenseGroupMapper defenseGroupMapper,
                      DefenseGroupTeacherMapper defenseGroupTeacherMapper) {
        this.teacherScoreRecordMapper = teacherScoreRecordMapper;
        this.studentFinalScoreMapper = studentFinalScoreMapper;
        this.studentMapper = studentMapper;
        this.largeGroupScoreMapper = largeGroupScoreMapper;
        this.defenseGroupMapper = defenseGroupMapper;
        this.defenseGroupTeacherMapper = defenseGroupTeacherMapper;
    }

    void finalizeGroupScores(Long defenseGroupId, Integer year, Integer largeGroupScore) {
        if (defenseGroupId == null || year == null) {
            throw new IllegalArgumentException("defenseGroupId / year 不能为空");
        }
        List<Student> students = studentMapper.findByDefenseGroupId(defenseGroupId);
        if (students == null || students.isEmpty()) {
            return;
        }

        Map<Long, List<TeacherScoreRecord>> recordsByStudent = loadTeacherScoreMap(students, year);
        Map<Long, StudentFinalScore> finalScoresByStudent = loadFinalScores(students, year);

        double championAvgScore = 0.0;
        for (Student student : students) {
            Double rawAverage = averageTotalScore(recordsByStudent.get(student.getId()));
            if (rawAverage == null) {
                continue;
            }

            double groupAvg = round(rawAverage, 2);
            StudentFinalScore finalScore = ensureFinalScore(finalScoresByStudent, student.getId(), year);
            finalScore.setGroupAvgScore((int) Math.round(groupAvg));

            if (groupAvg > championAvgScore) {
                championAvgScore = groupAvg;
            }
        }

        double adjustmentFactor = 1.0;
        if (largeGroupScore != null && championAvgScore > 0) {
            adjustmentFactor = round(largeGroupScore / championAvgScore, 3);
        }

        for (Student student : students) {
            StudentFinalScore finalScore = finalScoresByStudent.get(student.getId());
            if (finalScore == null || finalScore.getGroupAvgScore() == null) {
                continue;
            }

            double finalDefenseScore = round(finalScore.getGroupAvgScore() * adjustmentFactor, 2);
            finalScore.setAdjustmentFactor(adjustmentFactor);
            finalScore.setLargeGroupScore(largeGroupScore);
            finalScore.setFinalDefenseScore(finalDefenseScore);
            refreshTotalGrade(finalScore, finalDefenseScore);
            studentFinalScoreMapper.update(finalScore);
        }
    }

    Map<String, Object> getGroupAdjustmentFactor(Long groupId, Integer year) {
        Map<String, Object> result = new HashMap<>();
        List<Student> students = studentMapper.findByDefenseGroupId(groupId);
        if (students == null || students.isEmpty()) {
            result.put("adjustmentFactor", 1.0);
            result.put("message", "小组无学生");
            return result;
        }

        Student firstStudent = students.get(0);
        StudentFinalScore finalScore = studentFinalScoreMapper.findByStudentIdAndYear(firstStudent.getId(), year);
        if (finalScore == null || finalScore.getAdjustmentFactor() == null) {
            result.put("adjustmentFactor", 1.0);
            result.put("message", "尚未计算调节系数，请先完成小组汇总");
            return result;
        }

        result.put("adjustmentFactor", finalScore.getAdjustmentFactor());
        result.put("groupAvgScore", finalScore.getGroupAvgScore() != null ? finalScore.getGroupAvgScore() : 0);
        result.put("largeGroupScore", finalScore.getLargeGroupScore() != null ? finalScore.getLargeGroupScore() : 0);
        result.put("finalDefenseScore", finalScore.getFinalDefenseScore() != null ? finalScore.getFinalDefenseScore() : 0.0);
        return result;
    }

    Map<String, Object> getTeacherGroupStudents(Long teacherId, Integer year) {
        Map<String, Object> result = new HashMap<>();

        DefenseGroupTeacher groupTeacher = defenseGroupTeacherMapper.findByTeacherId(teacherId);
        if (groupTeacher == null) {
            result.put("groupId", null);
            result.put("groupName", "");
            result.put("students", Collections.emptyList());
            result.put("message", "教师未分配到任何小组");
            return result;
        }

        Long groupId = groupTeacher.getGroupId();
        DefenseGroup group = defenseGroupMapper.findById(groupId);
        result.put("groupId", groupId);
        result.put("groupName", group != null ? group.getName() : "");
        result.put("isLeader", groupTeacher.getIsLeader() != null && groupTeacher.getIsLeader() == 1);

        List<Student> students = filterStudentsByYear(studentMapper.findByDefenseGroupId(groupId), year);
        List<DefenseGroupTeacher> groupTeachers = defenseGroupTeacherMapper.findByGroupId(groupId);
        int totalTeachers = groupTeachers != null ? groupTeachers.size() : 0;
        Map<Long, List<TeacherScoreRecord>> recordsByStudent = loadTeacherScoreMap(students, year);

        Student topStudent = null;
        double topAvgScore = -1;
        for (Student student : students) {
            List<TeacherScoreRecord> records = recordsByStudent.getOrDefault(student.getId(), Collections.emptyList());
            if (records.size() < totalTeachers || totalTeachers <= 0) {
                continue;
            }
            Double avgScore = averageTotalScore(records);
            if (avgScore != null && avgScore > topAvgScore) {
                topAvgScore = avgScore;
                topStudent = student;
            }
        }

        Double groupAdjustmentFactor = null;
        if (topStudent != null && topAvgScore > 0) {
            List<LargeGroupScore> largeScores = largeGroupScoreMapper.findByStudentIdAndYear(topStudent.getId(), year);
            if (largeScores != null && !largeScores.isEmpty()) {
                double largeGroupAvgScore = largeScores.stream()
                        .filter(score -> score.getScore() != null)
                        .mapToInt(LargeGroupScore::getScore)
                        .average()
                        .orElse(0.0);
                groupAdjustmentFactor = round(largeGroupAvgScore / topAvgScore, 3);
            }
        }
        result.put("groupAdjustmentFactor", groupAdjustmentFactor);

        List<Map<String, Object>> studentList = new ArrayList<>();
        for (Student student : students) {
            List<TeacherScoreRecord> records = recordsByStudent.getOrDefault(student.getId(), Collections.emptyList());

            Map<String, Object> studentInfo = new HashMap<>();
            studentInfo.put("id", student.getId());
            studentInfo.put("studentNo", student.getStudentNo());
            studentInfo.put("name", student.getName());
            studentInfo.put("classInfo", student.getClassInfo());
            studentInfo.put("departmentName", resolveDepartmentName(student));
            studentInfo.put("defenseType", student.getDefenseType());
            studentInfo.put("title", student.getTitle());
            studentInfo.put("defenseYear", student.getDefenseYear());
            studentInfo.put("scoredTeachersCount", records.size());
            studentInfo.put("totalTeachersCount", totalTeachers);

            TeacherScoreRecord myScore = null;
            for (TeacherScoreRecord record : records) {
                if (record.getTeacherId() != null && record.getTeacherId().equals(teacherId)) {
                    myScore = record;
                    break;
                }
            }
            studentInfo.put("hasScored", myScore != null);
            studentInfo.put("myScore", myScore);

            if (records.size() >= totalTeachers && totalTeachers > 0) {
                Double studentAvgScore = averageTotalScore(records);
                studentAvgScore = studentAvgScore == null ? null : round(studentAvgScore, 1);
                studentInfo.put("avgScore", studentAvgScore);
                studentInfo.put("allScored", true);
                studentInfo.put("adjustmentFactor", groupAdjustmentFactor);
                studentInfo.put("finalDefenseScore", groupAdjustmentFactor != null && studentAvgScore != null
                        ? round(studentAvgScore * groupAdjustmentFactor, 1) : null);
            } else {
                studentInfo.put("avgScore", null);
                studentInfo.put("allScored", false);
                studentInfo.put("adjustmentFactor", groupAdjustmentFactor);
                studentInfo.put("finalDefenseScore", null);
            }

            studentList.add(studentInfo);
        }

        result.put("students", studentList);
        return result;
    }

    Map<String, Object> getAllGroupStudentsForSuperAdmin(Integer year) {
        Map<String, Object> result = new HashMap<>();
        List<DefenseGroup> allGroups = defenseGroupMapper.findAllByOrderByDisplayOrderAsc();
        if (allGroups == null || allGroups.isEmpty()) {
            result.put("groups", Collections.emptyList());
            return result;
        }

        List<Map<String, Object>> groupList = new ArrayList<>();
        for (DefenseGroup group : allGroups) {
            List<Student> students = filterStudentsByYear(studentMapper.findByDefenseGroupId(group.getId()), year);
            if (students.isEmpty()) {
                continue;
            }

            List<DefenseGroupTeacher> groupTeachers = defenseGroupTeacherMapper.findByGroupId(group.getId());
            int totalTeachers = groupTeachers != null ? groupTeachers.size() : 0;
            Map<Long, List<TeacherScoreRecord>> recordsByStudent = loadTeacherScoreMap(students, year);
            Map<Long, StudentFinalScore> finalScoresByStudent = loadFinalScores(students, year);

            List<Map<String, Object>> studentList = new ArrayList<>();
            for (Student student : students) {
                List<TeacherScoreRecord> records = recordsByStudent.getOrDefault(student.getId(), Collections.emptyList());
                StudentFinalScore finalScore = finalScoresByStudent.get(student.getId());

                Map<String, Object> studentInfo = new HashMap<>();
                studentInfo.put("id", student.getId());
                studentInfo.put("studentNo", student.getStudentNo());
                studentInfo.put("name", student.getName());
                studentInfo.put("departmentName", resolveDepartmentName(student));
                studentInfo.put("defenseType", student.getDefenseType());
                studentInfo.put("title", student.getTitle());
                studentInfo.put("scoredTeachersCount", records.size());
                studentInfo.put("totalTeachersCount", totalTeachers);
                studentInfo.put("hasScored", !records.isEmpty());

                if (records.size() >= totalTeachers && totalTeachers > 0) {
                    Double avgScore = averageTotalScore(records);
                    studentInfo.put("avgScore", avgScore == null ? null : round(avgScore, 1));
                    studentInfo.put("allScored", true);
                } else {
                    studentInfo.put("avgScore", null);
                    studentInfo.put("allScored", false);
                }

                if (finalScore != null) {
                    studentInfo.put("adjustmentFactor", finalScore.getAdjustmentFactor());
                    studentInfo.put("finalDefenseScore", finalScore.getFinalDefenseScore());
                } else {
                    studentInfo.put("adjustmentFactor", null);
                    studentInfo.put("finalDefenseScore", null);
                }
                studentList.add(studentInfo);
            }

            Map<String, Object> groupInfo = new HashMap<>();
            groupInfo.put("groupId", group.getId());
            groupInfo.put("groupName", group.getName());
            groupInfo.put("departmentId", group.getDepartmentId());
            groupInfo.put("students", studentList);
            groupList.add(groupInfo);
        }

        result.put("groups", groupList);
        result.put("isSuperAdmin", true);
        return result;
    }

    List<Map<String, Object>> getLargeGroupCandidates(Integer year, Long currentTeacherId) {
        List<Map<String, Object>> candidates = new ArrayList<>();
        List<DefenseGroup> groups = defenseGroupMapper.findAllByOrderByDisplayOrderAsc();
        if (groups == null || groups.isEmpty()) {
            return candidates;
        }

        List<DefenseGroupTeacher> allGroupTeachers = defenseGroupTeacherMapper.findAll();
        Map<Long, Integer> deptTeacherCountMap = new HashMap<>();
        if (allGroupTeachers != null) {
            Map<Long, Long> groupDeptMap = new HashMap<>();
            for (DefenseGroup group : groups) {
                if (group.getDepartmentId() != null) {
                    groupDeptMap.put(group.getId(), group.getDepartmentId());
                }
            }

            Map<Long, Set<Long>> deptTeachersSet = new HashMap<>();
            for (DefenseGroupTeacher teacher : allGroupTeachers) {
                Long departmentId = groupDeptMap.get(teacher.getGroupId());
                if (departmentId != null && teacher.getTeacherId() != null) {
                    deptTeachersSet.computeIfAbsent(departmentId, key -> new HashSet<>()).add(teacher.getTeacherId());
                }
            }

            for (Map.Entry<Long, Set<Long>> entry : deptTeachersSet.entrySet()) {
                deptTeacherCountMap.put(entry.getKey(), entry.getValue().size());
            }
        }

        for (DefenseGroup group : groups) {
            List<Student> students = filterStudentsByYear(studentMapper.findByDefenseGroupId(group.getId()), year);
            if (students.isEmpty()) {
                continue;
            }

            List<DefenseGroupTeacher> groupTeachers = defenseGroupTeacherMapper.findByGroupId(group.getId());
            int groupTeacherCount = groupTeachers != null ? groupTeachers.size() : 0;
            Map<Long, List<TeacherScoreRecord>> recordsByStudent = loadTeacherScoreMap(students, year);

            Student topStudent = null;
            double topAvgScore = -1;
            for (Student student : students) {
                List<TeacherScoreRecord> records = recordsByStudent.getOrDefault(student.getId(), Collections.emptyList());
                if (records.isEmpty() || records.size() < groupTeacherCount) {
                    continue;
                }
                Double avgScore = averageTotalScore(records);
                if (avgScore != null && avgScore > topAvgScore) {
                    topAvgScore = avgScore;
                    topStudent = student;
                }
            }

            if (topStudent == null) {
                continue;
            }

            Map<String, Object> candidate = new HashMap<>();
            candidate.put("groupId", group.getId());
            candidate.put("groupName", group.getName());
            candidate.put("departmentId", group.getDepartmentId());
            candidate.put("studentId", topStudent.getId());
            candidate.put("studentNo", topStudent.getStudentNo());
            candidate.put("studentName", topStudent.getName());
            candidate.put("defenseType", topStudent.getDefenseType());
            candidate.put("title", topStudent.getTitle());
            candidate.put("groupAvgScore", round(topAvgScore, 1));

            List<LargeGroupScore> largeScores = largeGroupScoreMapper.findByStudentIdAndYear(topStudent.getId(), year);
            candidate.put("largeGroupScoredCount", largeScores != null ? largeScores.size() : 0);
            candidate.put("totalTeachersCount", deptTeacherCountMap.getOrDefault(group.getDepartmentId(), 0));

            Double largeGroupAvgScore = null;
            if (largeScores != null && !largeScores.isEmpty()) {
                double largeAvg = largeScores.stream()
                        .filter(score -> score.getScore() != null)
                        .mapToInt(LargeGroupScore::getScore)
                        .average()
                        .orElse(0.0);
                largeGroupAvgScore = round(largeAvg, 1);
            }
            candidate.put("largeGroupAvgScore", largeGroupAvgScore);
            candidate.put("adjustmentFactor",
                    largeGroupAvgScore != null && topAvgScore > 0 ? round(largeGroupAvgScore / topAvgScore, 3) : null);

            Integer myLargeGroupScore = null;
            if (currentTeacherId != null && largeScores != null) {
                for (LargeGroupScore largeScore : largeScores) {
                    if (largeScore.getTeacherId() != null && largeScore.getTeacherId().equals(currentTeacherId)) {
                        myLargeGroupScore = largeScore.getScore();
                        break;
                    }
                }
            }
            candidate.put("myLargeGroupScore", myLargeGroupScore);
            candidates.add(candidate);
        }

        return candidates;
    }

    void updateGroupAdjustmentFactor(Long topStudentId, Integer year) {
        Student topStudent = studentMapper.findById(topStudentId);
        if (topStudent == null || topStudent.getDefenseGroupId() == null) {
            return;
        }

        List<LargeGroupScore> largeScores = largeGroupScoreMapper.findByStudentIdAndYear(topStudentId, year);
        if (largeScores == null || largeScores.isEmpty()) {
            return;
        }

        double largeGroupAvgScore = largeScores.stream()
                .filter(score -> score.getScore() != null)
                .mapToInt(LargeGroupScore::getScore)
                .average()
                .orElse(0.0);

        List<Student> students = filterStudentsByYear(studentMapper.findByDefenseGroupId(topStudent.getDefenseGroupId()), year);
        if (students.isEmpty()) {
            return;
        }

        Map<Long, List<TeacherScoreRecord>> recordsByStudent = loadTeacherScoreMap(students, year);
        Double topStudentGroupAvgScore = averageTotalScore(recordsByStudent.get(topStudentId));
        if (topStudentGroupAvgScore == null || topStudentGroupAvgScore <= 0) {
            return;
        }

        double adjustmentFactor = round(largeGroupAvgScore / topStudentGroupAvgScore, 3);
        Map<Long, StudentFinalScore> finalScoresByStudent = loadFinalScores(students, year);

        for (Student student : students) {
            Double studentGroupAvgScore = averageTotalScore(recordsByStudent.get(student.getId()));
            if (studentGroupAvgScore == null) {
                continue;
            }

            StudentFinalScore finalScore = ensureFinalScore(finalScoresByStudent, student.getId(), year);
            finalScore.setGroupAvgScore((int) Math.round(studentGroupAvgScore));
            finalScore.setAdjustmentFactor(adjustmentFactor);

            double finalDefenseScore = round(studentGroupAvgScore * adjustmentFactor, 1);
            finalScore.setFinalDefenseScore(finalDefenseScore);

            if (student.getId().equals(topStudentId)) {
                finalScore.setLargeGroupScore((int) Math.round(largeGroupAvgScore));
            }

            refreshTotalGrade(finalScore, finalDefenseScore);
            studentFinalScoreMapper.update(finalScore);
        }
    }

    Double calculateGroupAvgScore(Long studentId, Integer year) {
        if (studentId == null || year == null) {
            return null;
        }
        List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(studentId, year);
        Double avgScore = averageTotalScore(records);
        return avgScore == null ? null : round(avgScore, 1);
    }

    private Map<Long, List<TeacherScoreRecord>> loadTeacherScoreMap(List<Student> students, Integer year) {
        Map<Long, List<TeacherScoreRecord>> recordsByStudent = new HashMap<>();
        if (students == null || students.isEmpty() || year == null) {
            return recordsByStudent;
        }

        List<Long> studentIds = students.stream()
                .map(Student::getId)
                .collect(Collectors.toList());
        List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdsAndYear(studentIds, year);
        if (records == null) {
            return recordsByStudent;
        }

        for (TeacherScoreRecord record : records) {
            if (record.getStudentId() != null) {
                recordsByStudent.computeIfAbsent(record.getStudentId(), key -> new ArrayList<>()).add(record);
            }
        }

        for (Long studentId : studentIds) {
            if (!recordsByStudent.containsKey(studentId)) {
                List<TeacherScoreRecord> studentRecords = teacherScoreRecordMapper.findByStudentIdAndYear(studentId, year);
                if (studentRecords != null && !studentRecords.isEmpty()) {
                    recordsByStudent.put(studentId, studentRecords);
                }
            }
        }
        return recordsByStudent;
    }

    private Map<Long, StudentFinalScore> loadFinalScores(List<Student> students, Integer year) {
        Map<Long, StudentFinalScore> finalScoresByStudent = new HashMap<>();
        if (students == null || students.isEmpty() || year == null) {
            return finalScoresByStudent;
        }

        List<Long> studentIds = students.stream()
                .map(Student::getId)
                .collect(Collectors.toList());
        List<StudentFinalScore> finalScores = studentFinalScoreMapper.findByStudentIdsAndYear(studentIds, year);
        if (finalScores == null) {
            return finalScoresByStudent;
        }

        for (StudentFinalScore finalScore : finalScores) {
            if (finalScore.getStudentId() != null) {
                finalScoresByStudent.put(finalScore.getStudentId(), finalScore);
            }
        }

        for (Long studentId : studentIds) {
            if (!finalScoresByStudent.containsKey(studentId)) {
                StudentFinalScore finalScore = studentFinalScoreMapper.findByStudentIdAndYear(studentId, year);
                if (finalScore != null) {
                    finalScoresByStudent.put(studentId, finalScore);
                }
            }
        }
        return finalScoresByStudent;
    }

    private StudentFinalScore ensureFinalScore(Map<Long, StudentFinalScore> finalScoresByStudent,
                                               Long studentId,
                                               Integer year) {
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

    private List<Student> filterStudentsByYear(List<Student> students, Integer year) {
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

    private String resolveDepartmentName(Student student) {
        if (student.getDepartment() != null && student.getDepartment().getName() != null
                && !student.getDepartment().getName().isEmpty()) {
            return student.getDepartment().getName();
        }
        return null;
    }

    private Double averageTotalScore(List<TeacherScoreRecord> records) {
        if (records == null || records.isEmpty()) {
            return null;
        }
        DoubleSummaryStatistics stats = records.stream()
                .filter(record -> record.getTotalScore() != null)
                .mapToDouble(TeacherScoreRecord::getTotalScore)
                .summaryStatistics();
        return stats.getCount() == 0 ? null : stats.getAverage();
    }

    private void refreshTotalGrade(StudentFinalScore finalScore, double finalDefenseScore) {
        if (finalScore.getAdvisorScore() != null && finalScore.getReviewerScore() != null) {
            double totalGrade = finalScore.getAdvisorScore() * 0.3
                    + finalScore.getReviewerScore() * 0.3
                    + finalDefenseScore * 0.4;
            finalScore.setTotalGrade(round(totalGrade, 1));
        }
    }

    private double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
