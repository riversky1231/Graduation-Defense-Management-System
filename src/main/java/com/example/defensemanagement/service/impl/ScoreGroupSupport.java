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
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.example.defensemanagement.service.impl.ScoreMathHelper.*;

/**
 * Group-level score operations: finalization, adjustment factor, candidate selection.
 *
 * Delegates pure-math operations to {@link ScoreMathHelper} and data-loading to
 * {@link ScoreGroupQueries}. Domain logic stays here.
 */
@Component
final class ScoreGroupSupport {

    private static final Logger log = LoggerFactory.getLogger(ScoreGroupSupport.class);

    private final TeacherScoreRecordMapper teacherScoreRecordMapper;
    private final StudentFinalScoreMapper studentFinalScoreMapper;
    private final StudentMapper studentMapper;
    private final LargeGroupScoreMapper largeGroupScoreMapper;
    private final DefenseGroupMapper defenseGroupMapper;
    private final DefenseGroupTeacherMapper defenseGroupTeacherMapper;
    private final ScoreGroupQueries queries;

    ScoreGroupSupport(TeacherScoreRecordMapper teacherScoreRecordMapper,
                      StudentFinalScoreMapper studentFinalScoreMapper,
                      StudentMapper studentMapper,
                      LargeGroupScoreMapper largeGroupScoreMapper,
                      DefenseGroupMapper defenseGroupMapper,
                      DefenseGroupTeacherMapper defenseGroupTeacherMapper,
                      ScoreGroupQueries queries) {
        this.teacherScoreRecordMapper = teacherScoreRecordMapper;
        this.studentFinalScoreMapper = studentFinalScoreMapper;
        this.studentMapper = studentMapper;
        this.largeGroupScoreMapper = largeGroupScoreMapper;
        this.defenseGroupMapper = defenseGroupMapper;
        this.defenseGroupTeacherMapper = defenseGroupTeacherMapper;
        this.queries = queries;
    }

    void finalizeGroupScores(Long defenseGroupId, Integer year, Integer largeGroupScore) {
        if (defenseGroupId == null || year == null) {
            throw new IllegalArgumentException("defenseGroupId / year 不能为空");
        }
        List<Student> students = studentMapper.findByDefenseGroupId(defenseGroupId);
        if (students == null || students.isEmpty()) {
            return;
        }

        Map<Long, List<TeacherScoreRecord>> recordsByStudent = queries.loadTeacherScoreMap(students, year);
        Map<Long, StudentFinalScore> finalScoresByStudent = queries.loadFinalScores(students, year);
        Map<Long, Double> groupAverageByStudent = new HashMap<>();

        double championAvgScore = 0.0;
        for (Student student : students) {
            Double rawAverage = averageTotalScore(recordsByStudent.get(student.getId()));
            if (rawAverage == null) {
                continue;
            }
            groupAverageByStudent.put(student.getId(), rawAverage);
            StudentFinalScore finalScore = queries.ensureFinalScore(finalScoresByStudent, student.getId(), year);
            finalScore.setGroupAvgScore(roundToIntegerScore(rawAverage));
            if (rawAverage > championAvgScore) {
                championAvgScore = rawAverage;
            }
        }

        Double computedAdjustmentFactor = ScoreMathHelper.computeAdjustmentFactor(largeGroupScore, championAvgScore);
        double adjustmentFactor = computedAdjustmentFactor != null ? computedAdjustmentFactor : 1.0;

        for (Student student : students) {
            StudentFinalScore finalScore = finalScoresByStudent.get(student.getId());
            Double rawGroupAverage = groupAverageByStudent.get(student.getId());
            if (finalScore == null || rawGroupAverage == null) {
                continue;
            }
            double finalDefenseScore = computeFinalDefenseScore(rawGroupAverage, adjustmentFactor);
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

        List<Student> students = queries.filterStudentsByYear(studentMapper.findByDefenseGroupId(groupId), year);
        List<DefenseGroupTeacher> groupTeachers = defenseGroupTeacherMapper.findByGroupId(groupId);
        int totalTeachers = groupTeachers != null ? groupTeachers.size() : 0;
        Map<Long, List<TeacherScoreRecord>> recordsByStudent = queries.loadTeacherScoreMap(students, year);
        Student topStudent = findTopStudent(students, recordsByStudent, totalTeachers);
        double topAvgScore = topStudent != null
                ? averageTotalScore(recordsByStudent.getOrDefault(topStudent.getId(), Collections.emptyList()))
                : -1;

        Double groupAdjustmentFactor = computeAdjustmentFactor(topStudent, topAvgScore, year);
        result.put("groupAdjustmentFactor", groupAdjustmentFactor);

        List<Map<String, Object>> studentList = new ArrayList<>();
        for (Student student : students) {
            List<TeacherScoreRecord> records = recordsByStudent.getOrDefault(student.getId(), Collections.emptyList());
            Map<String, Object> studentInfo = buildStudentInfo(student, records, totalTeachers,
                    groupAdjustmentFactor, teacherId);
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
            List<Student> students = queries.filterStudentsByYear(studentMapper.findByDefenseGroupId(group.getId()), year);
            if (students.isEmpty()) {
                continue;
            }

            List<DefenseGroupTeacher> groupTeachers = defenseGroupTeacherMapper.findByGroupId(group.getId());
            int totalTeachers = groupTeachers != null ? groupTeachers.size() : 0;
            Map<Long, List<TeacherScoreRecord>> recordsByStudent = queries.loadTeacherScoreMap(students, year);
            Map<Long, StudentFinalScore> finalScoresByStudent = queries.loadFinalScores(students, year);

            List<Map<String, Object>> studentList = new ArrayList<>();
            for (Student student : students) {
                List<TeacherScoreRecord> records = recordsByStudent.getOrDefault(student.getId(), Collections.emptyList());
                StudentFinalScore finalScore = finalScoresByStudent.get(student.getId());
                Map<String, Object> studentInfo = buildSuperAdminStudentInfo(
                        student, records, totalTeachers, finalScore);
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

        // 批量加载所有小组的学生（消除 N+1 查询）
        List<Long> groupIds = groups.stream()
                .map(DefenseGroup::getId)
                .collect(Collectors.toList());
        Map<Long, List<Student>> studentsByGroup = queries.loadStudentsByGroupIds(groupIds, year);

        // 批量加载所有大组答辩得分
        Set<Long> allStudentIds = studentsByGroup.values().stream()
                .flatMap(List::stream)
                .map(Student::getId)
                .collect(Collectors.toSet());
        Map<Long, List<LargeGroupScore>> largeScoresByStudent = loadLargeGroupScoresBatch(allStudentIds, year);
        List<Student> allStudents = studentsByGroup.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        Map<Long, List<TeacherScoreRecord>> recordsByStudent = queries.loadTeacherScoreMap(allStudents, year);
        Map<Long, List<DefenseGroupTeacher>> groupTeachersByGroup = loadGroupTeachersByGroup(groups);
        Map<Long, Integer> deptTeacherCountMap = buildDepartmentTeacherCountMap(groups, groupTeachersByGroup);

        for (DefenseGroup group : groups) {
            List<Student> students = studentsByGroup.getOrDefault(group.getId(), Collections.emptyList());
            if (students.isEmpty()) {
                continue;
            }

            List<DefenseGroupTeacher> groupTeachers = groupTeachersByGroup.getOrDefault(
                    group.getId(), Collections.emptyList());
            int groupTeacherCount = groupTeachers != null ? groupTeachers.size() : 0;

            Student topStudent = findTopStudent(students, recordsByStudent, groupTeacherCount);
            if (topStudent == null) {
                continue;
            }

            Double topAvgScore = averageTotalScore(recordsByStudent.get(topStudent.getId()));
            if (topAvgScore == null) {
                continue;
            }

            List<LargeGroupScore> topStudentLargeScores = largeScoresByStudent.getOrDefault(topStudent.getId(), Collections.emptyList());
            Map<String, Object> candidate = buildCandidateInfo(group, topStudent, topAvgScore, year,
                    currentTeacherId, deptTeacherCountMap, topStudentLargeScores);
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

        List<Student> students = queries.filterStudentsByYear(
                studentMapper.findByDefenseGroupId(topStudent.getDefenseGroupId()), year);
        if (students.isEmpty()) {
            return;
        }

        Map<Long, List<TeacherScoreRecord>> recordsByStudent = queries.loadTeacherScoreMap(students, year);
        Double topStudentGroupAvgScore = averageTotalScore(recordsByStudent.get(topStudentId));
        if (topStudentGroupAvgScore == null || topStudentGroupAvgScore <= 0) {
            return;
        }

        Double adjustmentFactor = ScoreMathHelper.computeAdjustmentFactor(largeGroupAvgScore, topStudentGroupAvgScore);
        if (adjustmentFactor == null) {
            return;
        }
        Map<Long, StudentFinalScore> finalScoresByStudent = queries.loadFinalScores(students, year);

        for (Student student : students) {
            Double studentGroupAvgScore = averageTotalScore(recordsByStudent.get(student.getId()));
            if (studentGroupAvgScore == null) {
                continue;
            }

            StudentFinalScore finalScore = queries.ensureFinalScore(finalScoresByStudent, student.getId(), year);
            finalScore.setGroupAvgScore(roundToIntegerScore(studentGroupAvgScore));
            finalScore.setAdjustmentFactor(adjustmentFactor);

            double finalDefenseScore = computeFinalDefenseScore(studentGroupAvgScore, adjustmentFactor);
            finalScore.setFinalDefenseScore(finalDefenseScore);

            if (student.getId().equals(topStudentId)) {
                finalScore.setLargeGroupScore(roundToIntegerScore(largeGroupAvgScore));
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
        return avgScore == null ? null : roundDefault(avgScore);
    }

    // ─── Private helpers ───────────────────────────────────────────────────────

    private Double computeAdjustmentFactor(Student topStudent, double topAvgScore, Integer year) {
        if (topStudent == null || topAvgScore <= 0) {
            return null;
        }
        List<LargeGroupScore> largeScores = largeGroupScoreMapper.findByStudentIdAndYear(topStudent.getId(), year);
        if (largeScores == null || largeScores.isEmpty()) {
            return null;
        }
        double largeGroupAvgScore = largeScores.stream()
                .filter(score -> score.getScore() != null)
                .mapToInt(LargeGroupScore::getScore)
                .average()
                .orElse(0.0);
        return ScoreMathHelper.computeAdjustmentFactor(largeGroupAvgScore, topAvgScore);
    }

    private Map<String, Object> buildStudentInfo(Student student, List<TeacherScoreRecord> records,
                                                   int totalTeachers, Double groupAdjustmentFactor,
                                                   Long teacherId) {
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
        studentInfo.put("teacherScores", buildTeacherScoreEntries(records));

        TeacherScoreRecord myScore = findTeacherScore(records, teacherId);
        studentInfo.put("hasScored", myScore != null);
        studentInfo.put("myScore", myScore);
        applyProgressFields(studentInfo, records, totalTeachers, groupAdjustmentFactor);

        return studentInfo;
    }

    private Map<String, Object> buildSuperAdminStudentInfo(Student student, List<TeacherScoreRecord> records,
                                                            int totalTeachers, StudentFinalScore finalScore) {
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
        studentInfo.put("teacherScores", buildTeacherScoreEntries(records));
        applyProgressFields(studentInfo, records, totalTeachers, null);

        if (finalScore != null) {
            studentInfo.put("adjustmentFactor", finalScore.getAdjustmentFactor());
            studentInfo.put("finalDefenseScore", finalScore.getFinalDefenseScore());
        } else {
            studentInfo.put("adjustmentFactor", null);
            studentInfo.put("finalDefenseScore", null);
        }

        return studentInfo;
    }

    private void applyProgressFields(Map<String, Object> studentInfo,
                                     List<TeacherScoreRecord> records,
                                     int totalTeachers,
                                     Double groupAdjustmentFactor) {
        Double avgScore = hasAllTeacherScores(records, totalTeachers)
                ? averageTotalScore(records)
                : null;
        studentInfo.put("avgScore", avgScore == null ? null : roundDefault(avgScore));
        studentInfo.put("allScored", avgScore != null);
        if (groupAdjustmentFactor != null) {
            studentInfo.put("adjustmentFactor", groupAdjustmentFactor);
            studentInfo.put("finalDefenseScore", avgScore != null
                    ? computeFinalDefenseScore(avgScore, groupAdjustmentFactor)
                    : null);
        }
    }

    private boolean hasAllTeacherScores(List<TeacherScoreRecord> records, int totalTeachers) {
        return totalTeachers > 0 && records.size() >= totalTeachers;
    }

    private TeacherScoreRecord findTeacherScore(List<TeacherScoreRecord> records, Long teacherId) {
        if (teacherId == null || records == null || records.isEmpty()) {
            return null;
        }
        for (TeacherScoreRecord record : records) {
            if (record.getTeacherId() != null && record.getTeacherId().equals(teacherId)) {
                return record;
            }
        }
        return null;
    }

    private List<Map<String, Object>> buildTeacherScoreEntries(List<TeacherScoreRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> teacherScores = new ArrayList<>();
        for (TeacherScoreRecord record : records) {
            Map<String, Object> scoreInfo = new HashMap<>();
            scoreInfo.put("teacherId", record.getTeacherId());
            scoreInfo.put("teacherName",
                    record.getTeacher() != null ? record.getTeacher().getName() : null);
            scoreInfo.put("teacherNo",
                    record.getTeacher() != null ? record.getTeacher().getTeacherNo() : null);
            scoreInfo.put("item1Score", record.getItem1Score());
            scoreInfo.put("item2Score", record.getItem2Score());
            scoreInfo.put("item3Score", record.getItem3Score());
            scoreInfo.put("item4Score", record.getItem4Score());
            scoreInfo.put("item5Score", record.getItem5Score());
            scoreInfo.put("item6Score", record.getItem6Score());
            scoreInfo.put("totalScore", record.getTotalScore());
            scoreInfo.put("submitTime", record.getSubmitTime());
            teacherScores.add(scoreInfo);
        }
        return teacherScores;
    }

    private Map<String, Object> buildCandidateInfo(DefenseGroup group, Student topStudent, double topAvgScore,
                                                   Integer year, Long currentTeacherId,
                                                   Map<Long, Integer> deptTeacherCountMap,
                                                   List<LargeGroupScore> topStudentLargeScores) {
        Map<String, Object> candidate = new HashMap<>();
        candidate.put("groupId", group.getId());
        candidate.put("groupName", group.getName());
        candidate.put("departmentId", group.getDepartmentId());
        candidate.put("studentId", topStudent.getId());
        candidate.put("studentNo", topStudent.getStudentNo());
        candidate.put("studentName", topStudent.getName());
        candidate.put("defenseType", topStudent.getDefenseType());
        candidate.put("title", topStudent.getTitle());
        candidate.put("groupAvgScore", roundDefault(topAvgScore));

        candidate.put("largeGroupScoredCount", topStudentLargeScores != null ? topStudentLargeScores.size() : 0);
        candidate.put("totalTeachersCount", deptTeacherCountMap.getOrDefault(group.getDepartmentId(), 0));

        Double largeGroupAvgScore = null;
        Double rawLargeGroupAvgScore = null;
        if (topStudentLargeScores != null && !topStudentLargeScores.isEmpty()) {
            double largeAvg = topStudentLargeScores.stream()
                    .filter(score -> score.getScore() != null)
                    .mapToInt(LargeGroupScore::getScore)
                    .average()
                    .orElse(0.0);
            rawLargeGroupAvgScore = largeAvg;
            largeGroupAvgScore = roundDefault(largeAvg);
        }
        candidate.put("largeGroupAvgScore", largeGroupAvgScore);
        candidate.put("adjustmentFactor", ScoreMathHelper.computeAdjustmentFactor(rawLargeGroupAvgScore, topAvgScore));

        Integer myLargeGroupScore = null;
        if (currentTeacherId != null && topStudentLargeScores != null) {
            for (LargeGroupScore largeScore : topStudentLargeScores) {
                if (largeScore.getTeacherId() != null && largeScore.getTeacherId().equals(currentTeacherId)) {
                    myLargeGroupScore = largeScore.getScore();
                    break;
                }
            }
        }
        candidate.put("myLargeGroupScore", myLargeGroupScore);

        return candidate;
    }

    private Map<Long, List<DefenseGroupTeacher>> loadGroupTeachersByGroup(List<DefenseGroup> groups) {
        Map<Long, List<DefenseGroupTeacher>> teachersByGroup = new HashMap<>();
        List<Long> groupIds = groups.stream()
                .map(DefenseGroup::getId)
                .collect(Collectors.toList());
        List<DefenseGroupTeacher> relevantGroupTeachers = defenseGroupTeacherMapper.findByGroupIds(groupIds);
        if (relevantGroupTeachers == null) {
            return teachersByGroup;
        }
        for (DefenseGroupTeacher teacher : relevantGroupTeachers) {
            if (teacher.getGroupId() != null) {
                teachersByGroup.computeIfAbsent(teacher.getGroupId(), key -> new ArrayList<>()).add(teacher);
            }
        }
        return teachersByGroup;
    }

    private Map<Long, Integer> buildDepartmentTeacherCountMap(
            List<DefenseGroup> groups,
            Map<Long, List<DefenseGroupTeacher>> groupTeachersByGroup) {
        Map<Long, Integer> deptTeacherCountMap = new HashMap<>();
        if (groupTeachersByGroup == null || groupTeachersByGroup.isEmpty()) {
            return deptTeacherCountMap;
        }

        Map<Long, Long> groupDeptMap = new HashMap<>();
        for (DefenseGroup group : groups) {
            if (group.getDepartmentId() != null) {
                groupDeptMap.put(group.getId(), group.getDepartmentId());
            }
        }

        Map<Long, Set<Long>> deptTeachersSet = new HashMap<>();
        for (Map.Entry<Long, List<DefenseGroupTeacher>> entry : groupTeachersByGroup.entrySet()) {
            Long departmentId = groupDeptMap.get(entry.getKey());
            if (departmentId == null) {
                continue;
            }
            for (DefenseGroupTeacher teacher : entry.getValue()) {
                if (teacher.getTeacherId() != null) {
                    deptTeachersSet.computeIfAbsent(departmentId, key -> new HashSet<>()).add(teacher.getTeacherId());
                }
            }
        }

        for (Map.Entry<Long, Set<Long>> entry : deptTeachersSet.entrySet()) {
            deptTeacherCountMap.put(entry.getKey(), entry.getValue().size());
        }
        return deptTeacherCountMap;
    }

    private Student findTopStudent(List<Student> students, Map<Long, List<TeacherScoreRecord>> recordsByStudent,
                                   int groupTeacherCount) {
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
        return topStudent;
    }

    /**
     * 批量加载所有候选学生的的大组答辩成绩。
     */
    private Map<Long, List<LargeGroupScore>> loadLargeGroupScoresBatch(Set<Long> studentIds, Integer year) {
        Map<Long, List<LargeGroupScore>> scoresByStudent = new HashMap<>();
        if (studentIds == null || studentIds.isEmpty() || year == null) {
            return scoresByStudent;
        }
        List<Long> ids = new ArrayList<>(studentIds);
        List<LargeGroupScore> allScores = largeGroupScoreMapper.findByStudentIdsAndYear(ids, year);
        if (allScores != null) {
            for (LargeGroupScore score : allScores) {
                if (score.getStudentId() != null) {
                    scoresByStudent.computeIfAbsent(score.getStudentId(), key -> new ArrayList<>()).add(score);
                }
            }
        }
        return scoresByStudent;
    }
}
