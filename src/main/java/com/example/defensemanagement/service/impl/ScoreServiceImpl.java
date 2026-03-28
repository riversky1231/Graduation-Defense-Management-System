package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.entity.EvaluationItem;
import com.example.defensemanagement.entity.LargeGroupScore;
import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.mapper.LargeGroupScoreMapper;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.HashMap;
import java.util.ArrayList;

@Service
public class ScoreServiceImpl implements ScoreService {

    @Autowired
    private TeacherScoreRecordMapper teacherScoreRecordMapper;

    @Autowired
    private StudentFinalScoreMapper studentFinalScoreMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private ConfigService configService;

    @Autowired
    private LargeGroupScoreMapper largeGroupScoreMapper;

    @Autowired
    private DefenseGroupMapper defenseGroupMapper;

    @Autowired
    private DefenseGroupTeacherMapper defenseGroupTeacherMapper;
    
    @Autowired
    private TeacherMapper teacherMapper;
    
    @Autowired
    private DepartmentMapper departmentMapper;

    @Override
    @Transactional
    public void setAdvisorScore(Long studentId, Integer year, Integer score) {
        upsertFinalScore(studentId, year, true, score);
    }

    @Override
    @Transactional
    public void setReviewerScore(Long studentId, Integer year, Integer score) {
        upsertFinalScore(studentId, year, false, score);
    }

    @Override
    @Transactional
    public void saveTeacherScore(TeacherScoreRecord record) {
        if (record.getStudentId() == null || record.getTeacherId() == null || record.getYear() == null) {
            throw new IllegalArgumentException("studentId / teacherId / year 不能为空");
        }
        record.setSubmitTime(LocalDateTime.now());
        TeacherScoreRecord existing = teacherScoreRecordMapper
                .findByStudentIdAndTeacherIdAndYear(record.getStudentId(), record.getTeacherId(), record.getYear());
        if (existing == null) {
            teacherScoreRecordMapper.insert(record);
        } else {
            record.setId(existing.getId());
            teacherScoreRecordMapper.update(record);
        }
    }

    @Override
    @Transactional
    public void autoSplitDesignScore(Long studentId, Long teacherId, Integer year, Integer totalScore, Long defenseGroupId) {
        if (studentId == null || teacherId == null || year == null || totalScore == null) {
            throw new IllegalArgumentException("studentId/teacherId/year/totalScore 不能为空");
        }
        Map<String, Integer> split = autoSplitScoreItems("DESIGN", totalScore);

        TeacherScoreRecord record = new TeacherScoreRecord();
        record.setStudentId(studentId);
        record.setTeacherId(teacherId);
        record.setYear(year);
        record.setDefenseGroupId(defenseGroupId);
        record.setItem1Score(split.getOrDefault("item1", 0));
        record.setItem2Score(split.getOrDefault("item2", 0));
        record.setItem3Score(split.getOrDefault("item3", 0));
        record.setItem4Score(split.getOrDefault("item4", 0));
        record.setItem5Score(split.getOrDefault("item5", 0));
        record.setItem6Score(split.getOrDefault("item6", 0));
        record.setTotalScore(totalScore);

        saveTeacherScore(record);
    }

    @Override
    public Map<String, Integer> autoSplitScoreItems(String defenseType, Integer totalScore) {
        if (defenseType == null || defenseType.trim().isEmpty() || totalScore == null) {
            throw new IllegalArgumentException("defenseType/totalScore 不能为空");
        }
        if (totalScore < 0 || totalScore > 100) {
            throw new IllegalArgumentException("总分必须在0-100之间");
        }

        final String type = defenseType.trim().toUpperCase();
        final int itemCount;
        final double[] defaultWeights;
        final int[] defaultMaxScores;

        if ("PAPER".equals(type)) {
            itemCount = 3;
            defaultWeights = new double[] { 0.5, 0.25, 0.25 };
            defaultMaxScores = new int[] { 50, 25, 25 };
        } else if ("DESIGN".equals(type)) {
            itemCount = 6;
            defaultWeights = new double[] { 0.15, 0.15, 0.15, 0.25, 0.15, 0.15 };
            defaultMaxScores = new int[] { 15, 15, 15, 25, 15, 15 };
        } else {
            throw new IllegalArgumentException("不支持的答辩类型: " + defenseType);
        }

        List<EvaluationItem> cfg = configService.getEvaluationItems(type);
        if (cfg == null) {
            cfg = java.util.Collections.emptyList();
        } else {
            cfg = new ArrayList<>(cfg);
            cfg.sort(Comparator.comparingInt(EvaluationItem::getDisplayOrder));
        }

        double[] weights = new double[itemCount];
        int[] maxScores = new int[itemCount];
        if (cfg != null && cfg.size() >= itemCount) {
            double sum = 0;
            for (int i = 0; i < itemCount; i++) {
                EvaluationItem it = cfg.get(i);
                double w = it.getWeight() == null ? 0 : it.getWeight();
                int max = it.getMaxScore() == null ? (int) Math.round(w * 100) : it.getMaxScore();
                weights[i] = w;
                maxScores[i] = Math.max(0, max);
                sum += w;
            }
            if (sum <= 0) {
                System.arraycopy(defaultWeights, 0, weights, 0, itemCount);
                System.arraycopy(defaultMaxScores, 0, maxScores, 0, itemCount);
            } else {
                for (int i = 0; i < itemCount; i++) {
                    weights[i] = weights[i] / sum;
                    if (maxScores[i] <= 0) {
                        maxScores[i] = (int) Math.round(weights[i] * 100);
                    }
                }
            }
        } else {
            System.arraycopy(defaultWeights, 0, weights, 0, itemCount);
            System.arraycopy(defaultMaxScores, 0, maxScores, 0, itemCount);
        }

        int maxTotal = 0;
        for (int m : maxScores) {
            maxTotal += m;
        }
        if (totalScore > maxTotal) {
            throw new IllegalArgumentException("总分超过分项上限之和: " + maxTotal);
        }

        int[] items = new int[itemCount];
        for (int i = 0; i < itemCount; i++) {
            int v = (int) Math.floor(totalScore * weights[i]);
            items[i] = Math.min(v, maxScores[i]);
        }

        int sum = 0;
        for (int v : items) {
            sum += v;
        }
        int diff = totalScore - sum;

        while (diff > 0) {
            int best = -1;
            int remain = -1;
            for (int i = 0; i < itemCount; i++) {
                int r = maxScores[i] - items[i];
                if (r > remain) {
                    remain = r;
                    best = i;
                }
            }
            if (best < 0 || remain <= 0) {
                break;
            }
            items[best]++;
            diff--;
        }

        while (diff < 0) {
            int best = -1;
            int current = -1;
            for (int i = 0; i < itemCount; i++) {
                if (items[i] > current) {
                    current = items[i];
                    best = i;
                }
            }
            if (best < 0 || current <= 0) {
                break;
            }
            items[best]--;
            diff++;
        }

        Map<String, Integer> result = new HashMap<>();
        for (int i = 0; i < itemCount; i++) {
            result.put("item" + (i + 1), items[i]);
        }
        return result;
    }

    @Override
    @Transactional
    public void finalizeGroupScores(Long defenseGroupId, Integer year, Integer largeGroupScore) {
        if (defenseGroupId == null || year == null) {
            throw new IllegalArgumentException("defenseGroupId / year 不能为空");
        }
        List<Student> students = studentMapper.findByDefenseGroupId(defenseGroupId);
        if (students == null || students.isEmpty()) {
            return;
        }

        // 计算每个学生在小组内的平均总分
        double championAvgScore = 0.0;
        for (Student stu : students) {
            List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(stu.getId(), year);
            if (records == null || records.isEmpty()) {
                continue;
            }
            DoubleSummaryStatistics stats = records.stream()
                    .filter(r -> r.getTotalScore() != null)
                    .mapToDouble(TeacherScoreRecord::getTotalScore)
                    .summaryStatistics();
            if (stats.getCount() == 0) {
                continue;
            }
            double groupAvg = round(stats.getAverage(), 2);

            // 更新/插入学生最终成绩
            StudentFinalScore finalScore = studentFinalScoreMapper.findByStudentIdAndYear(stu.getId(), year);
            if (finalScore == null) {
                finalScore = new StudentFinalScore();
                finalScore.setStudentId(stu.getId());
                finalScore.setYear(year);
                finalScore.setAdvisorScore(null);
                finalScore.setReviewerScore(null);
                studentFinalScoreMapper.insert(finalScore);
            }
            finalScore.setGroupAvgScore((int) Math.round(groupAvg));
            studentFinalScoreMapper.update(finalScore);

            if (groupAvg > championAvgScore) {
                championAvgScore = groupAvg;
            }
        }

        // 计算调节系数
        double adjustmentFactor = 1.0;
        if (largeGroupScore != null && championAvgScore > 0) {
            adjustmentFactor = round(largeGroupScore / championAvgScore, 3);
        }

        // 应用调节系数并计算最终答辩成绩、总评成绩
        for (Student stu : students) {
            StudentFinalScore finalScore = studentFinalScoreMapper.findByStudentIdAndYear(stu.getId(), year);
            if (finalScore == null || finalScore.getGroupAvgScore() == null) {
                continue;
            }
            double finalDefenseScore = round(finalScore.getGroupAvgScore() * adjustmentFactor, 2);
            finalScore.setAdjustmentFactor(adjustmentFactor);
            finalScore.setLargeGroupScore(largeGroupScore);
            finalScore.setFinalDefenseScore(finalDefenseScore);

            // 如果已有导师/评阅成绩，则计算总评成绩（30%、30%、40%，一位小数）
            if (finalScore.getAdvisorScore() != null && finalScore.getReviewerScore() != null) {
                double totalGrade = finalScore.getAdvisorScore() * 0.3
                        + finalScore.getReviewerScore() * 0.3
                        + finalDefenseScore * 0.4;
                finalScore.setTotalGrade(round(totalGrade, 1));
            }
            studentFinalScoreMapper.update(finalScore);
        }
    }

    @Override
    public Map<String, Object> getGroupAdjustmentFactor(Long groupId, Integer year) {
        Map<String, Object> result = new HashMap<>();
        List<Student> students = studentMapper.findByDefenseGroupId(groupId);
        if (students == null || students.isEmpty()) {
            result.put("adjustmentFactor", 1.0);
            result.put("message", "小组无学生");
            return result;
        }
        
        // 获取第一个学生的最终成绩（所有学生应该有相同的调节系数）
        Student firstStudent = students.get(0);
        StudentFinalScore fs = studentFinalScoreMapper.findByStudentIdAndYear(firstStudent.getId(), year);
        
        if (fs == null || fs.getAdjustmentFactor() == null) {
            result.put("adjustmentFactor", 1.0);
            result.put("message", "尚未计算调节系数，请先完成小组汇总");
            return result;
        }
        
        result.put("adjustmentFactor", fs.getAdjustmentFactor());
        result.put("groupAvgScore", fs.getGroupAvgScore() != null ? fs.getGroupAvgScore() : 0);
        result.put("largeGroupScore", fs.getLargeGroupScore() != null ? fs.getLargeGroupScore() : 0);
        result.put("finalDefenseScore", fs.getFinalDefenseScore() != null ? fs.getFinalDefenseScore() : 0.0);
        return result;
    }

    @Override
    public Map<String, Object> getTeacherGroupStudents(Long teacherId, Integer year) {
        Map<String, Object> result = new HashMap<>();
        
        // 查找教师所在的小组
        DefenseGroupTeacher dgt = defenseGroupTeacherMapper.findByTeacherId(teacherId);
        if (dgt == null) {
            result.put("groupId", null);
            result.put("groupName", "");
            result.put("students", java.util.Collections.emptyList());
            result.put("message", "教师未分配到任何小组");
            return result;
        }
        
        Long groupId = dgt.getGroupId();
        DefenseGroup group = defenseGroupMapper.findById(groupId);
        
        result.put("groupId", groupId);
        result.put("groupName", group != null ? group.getName() : "");
        result.put("isLeader", dgt.getIsLeader() != null && dgt.getIsLeader() == 1);
        
        // 获取小组内的所有学生
        List<Student> students = studentMapper.findByDefenseGroupId(groupId);
        if (students == null) {
            students = new java.util.ArrayList<>();
        }
        
        // 过滤年份
        if (year != null) {
            students = students.stream()
                    .filter(s -> s.getDefenseYear() != null && s.getDefenseYear().equals(year))
                    .collect(java.util.stream.Collectors.toList());
        }
        
        // 获取小组内的教师列表
        List<DefenseGroupTeacher> groupTeachers = defenseGroupTeacherMapper.findByGroupId(groupId);
        int totalTeachers = groupTeachers != null ? groupTeachers.size() : 0;
        
        // 找到小组第一名（平均分最高的学生），并计算统一的调节系数
        Student topStudent = null;
        double topAvgScore = -1;
        
        for (Student s : students) {
            List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(s.getId(), year);
            if (records != null && records.size() >= totalTeachers && totalTeachers > 0) {
                double avgScore = records.stream()
                        .filter(r -> r.getTotalScore() != null)
                        .mapToInt(TeacherScoreRecord::getTotalScore)
                        .average()
                        .orElse(0.0);
                if (avgScore > topAvgScore) {
                    topAvgScore = avgScore;
                    topStudent = s;
                }
            }
        }
        
        // 计算小组统一的调节系数
        Double groupAdjustmentFactor = null;
        if (topStudent != null && topAvgScore > 0) {
            // 获取小组第一名的大组答辩成绩
            List<LargeGroupScore> largeScores = largeGroupScoreMapper.findByStudentIdAndYear(topStudent.getId(), year);
            if (largeScores != null && !largeScores.isEmpty()) {
                double largeGroupAvgScore = largeScores.stream()
                        .filter(ls -> ls.getScore() != null)
                        .mapToInt(LargeGroupScore::getScore)
                        .average()
                        .orElse(0.0);
                // 调节系数 = 大组答辩平均分 / 小组第一名的小组平均分
                groupAdjustmentFactor = round(largeGroupAvgScore / topAvgScore, 3);
            }
        }
        
        result.put("groupAdjustmentFactor", groupAdjustmentFactor);
        
        // 为每个学生获取打分状态
        List<Map<String, Object>> studentList = new java.util.ArrayList<>();
        for (Student s : students) {
            Map<String, Object> studentInfo = new HashMap<>();
            studentInfo.put("id", s.getId());
            studentInfo.put("studentNo", s.getStudentNo());
            studentInfo.put("name", s.getName());
            studentInfo.put("classInfo", s.getClassInfo());
            // 设置院系信息
            String departmentName = null;
            if (s.getDepartment() != null && s.getDepartment().getName() != null && !s.getDepartment().getName().isEmpty()) {
                departmentName = s.getDepartment().getName();
            } else if (s.getDepartmentId() != null) {
                try {
                    com.example.defensemanagement.entity.Department dept = departmentMapper.findById(s.getDepartmentId());
                    if (dept != null && dept.getName() != null && !dept.getName().isEmpty()) {
                        departmentName = dept.getName();
                    }
                } catch (Exception e) {
                    System.err.println("查询院系信息时出错: " + e.getMessage());
                }
            }
            studentInfo.put("departmentName", departmentName);
            studentInfo.put("defenseType", s.getDefenseType());
            studentInfo.put("title", s.getTitle());
            studentInfo.put("defenseYear", s.getDefenseYear());
            
            // 获取该学生的所有打分记录
            List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(s.getId(), year);
            studentInfo.put("scoredTeachersCount", records != null ? records.size() : 0);
            studentInfo.put("totalTeachersCount", totalTeachers);
            
            // 检查当前教师是否已打分
            boolean hasScored = false;
            TeacherScoreRecord myScore = null;
            if (records != null) {
                for (TeacherScoreRecord r : records) {
                    if (r.getTeacherId() != null && r.getTeacherId().equals(teacherId)) {
                        hasScored = true;
                        myScore = r;
                        break;
                    }
                }
            }
            studentInfo.put("hasScored", hasScored);
            studentInfo.put("myScore", myScore);
            
            // 计算该学生的小组平均分
            Double studentAvgScore = null;
            if (records != null && records.size() >= totalTeachers && totalTeachers > 0) {
                studentAvgScore = round(records.stream()
                        .filter(r -> r.getTotalScore() != null)
                        .mapToInt(TeacherScoreRecord::getTotalScore)
                        .average()
                        .orElse(0.0), 1);
                studentInfo.put("avgScore", studentAvgScore);
                studentInfo.put("allScored", true);
            } else {
                studentInfo.put("avgScore", null);
                studentInfo.put("allScored", false);
            }
            
            // 使用小组统一的调节系数计算最终答辩成绩
            studentInfo.put("adjustmentFactor", groupAdjustmentFactor);
            if (groupAdjustmentFactor != null && studentAvgScore != null) {
                double finalDefenseScore = round(studentAvgScore * groupAdjustmentFactor, 1);
                studentInfo.put("finalDefenseScore", finalDefenseScore);
            } else {
                studentInfo.put("finalDefenseScore", null);
            }
            
            studentList.add(studentInfo);
        }
        
        result.put("students", studentList);
        return result;
    }

    @Override
    public Map<String, Object> getAllGroupStudentsForSuperAdmin(Integer year) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取所有小组
        List<DefenseGroup> allGroups = defenseGroupMapper.findAllByOrderByDisplayOrderAsc();
        if (allGroups == null || allGroups.isEmpty()) {
            result.put("groups", java.util.Collections.emptyList());
            return result;
        }
        
        // 为每个小组获取学生列表
        List<Map<String, Object>> groupList = new java.util.ArrayList<>();
        for (DefenseGroup group : allGroups) {
            List<Student> students = studentMapper.findByDefenseGroupId(group.getId());
            if (students == null) {
                students = new java.util.ArrayList<>();
            }
            
            // 过滤年份
            if (year != null) {
                students = students.stream()
                        .filter(s -> s.getDefenseYear() != null && s.getDefenseYear().equals(year))
                        .collect(java.util.stream.Collectors.toList());
            }
            
            if (students.isEmpty()) {
                continue; // 跳过没有学生的小组
            }
            
            // 获取小组内的教师列表
            List<DefenseGroupTeacher> groupTeachers = defenseGroupTeacherMapper.findByGroupId(group.getId());
            int totalTeachers = groupTeachers != null ? groupTeachers.size() : 0;
            
            // 为每个学生获取打分状态
            List<Map<String, Object>> studentList = new java.util.ArrayList<>();
            for (Student s : students) {
                Map<String, Object> studentInfo = new HashMap<>();
                studentInfo.put("id", s.getId());
                studentInfo.put("studentNo", s.getStudentNo());
                studentInfo.put("name", s.getName());
                // 设置院系信息
                String departmentName = null;
                if (s.getDepartment() != null && s.getDepartment().getName() != null && !s.getDepartment().getName().isEmpty()) {
                    departmentName = s.getDepartment().getName();
                } else if (s.getDepartmentId() != null) {
                    try {
                        com.example.defensemanagement.entity.Department dept = departmentMapper.findById(s.getDepartmentId());
                        if (dept != null && dept.getName() != null && !dept.getName().isEmpty()) {
                            departmentName = dept.getName();
                        }
                    } catch (Exception e) {
                        System.err.println("查询院系信息时出错: " + e.getMessage());
                    }
                }
                studentInfo.put("departmentName", departmentName);
                studentInfo.put("defenseType", s.getDefenseType());
                studentInfo.put("title", s.getTitle());
                
                // 获取该学生的所有打分记录
                List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(s.getId(), year);
                studentInfo.put("scoredTeachersCount", records != null ? records.size() : 0);
                studentInfo.put("totalTeachersCount", totalTeachers);
                studentInfo.put("hasScored", records != null && !records.isEmpty());
                
                // 计算该学生的小组平均分
                Double studentAvgScore = null;
                if (records != null && records.size() >= totalTeachers && totalTeachers > 0) {
                    studentAvgScore = round(records.stream()
                            .filter(r -> r.getTotalScore() != null)
                            .mapToInt(TeacherScoreRecord::getTotalScore)
                            .average()
                            .orElse(0.0), 1);
                    studentInfo.put("avgScore", studentAvgScore);
                    studentInfo.put("allScored", true);
                } else {
                    studentInfo.put("avgScore", null);
                    studentInfo.put("allScored", false);
                }
                
                // 获取学生的最终成绩信息（调节系数和最终答辩成绩）
                StudentFinalScore fs = studentFinalScoreMapper.findByStudentIdAndYear(s.getId(), year);
                if (fs != null) {
                    studentInfo.put("adjustmentFactor", fs.getAdjustmentFactor());
                    studentInfo.put("finalDefenseScore", fs.getFinalDefenseScore());
                } else {
                    studentInfo.put("adjustmentFactor", null);
                    studentInfo.put("finalDefenseScore", null);
                }
                
                studentList.add(studentInfo);
            }
            
            Map<String, Object> groupInfo = new HashMap<>();
            groupInfo.put("groupId", group.getId());
            groupInfo.put("groupName", group.getName());
            groupInfo.put("departmentId", group.getDepartmentId()); // 添加院系ID用于过滤
            groupInfo.put("students", studentList);
            groupList.add(groupInfo);
        }
        
        result.put("groups", groupList);
        result.put("isSuperAdmin", true);
        return result;
    }

    @Override
    public List<Map<String, Object>> getLargeGroupCandidates(Integer year, Long currentTeacherId) {
        List<Map<String, Object>> candidates = new java.util.ArrayList<>();
        
        // 获取所有小组
        List<DefenseGroup> groups = defenseGroupMapper.findAllByOrderByDisplayOrderAsc();
        if (groups == null || groups.isEmpty()) {
            return candidates;
        }
        
        // 获取所有教师（用于按院系统计）
        List<DefenseGroupTeacher> allGroupTeachers = defenseGroupTeacherMapper.findAll();
        
        // 按院系统计教师数量（根据小组所属院系）
        Map<Long, Integer> deptTeacherCountMap = new HashMap<>();
        if (allGroupTeachers != null) {
            // 先获取每个小组的院系ID
            Map<Long, Long> groupDeptMap = new HashMap<>();
            for (DefenseGroup g : groups) {
                if (g.getDepartmentId() != null) {
                    groupDeptMap.put(g.getId(), g.getDepartmentId());
                }
            }
            
            // 统计每个院系的教师数量（通过小组所属院系）
            Map<Long, java.util.Set<Long>> deptTeachersSet = new HashMap<>();
            for (DefenseGroupTeacher gt : allGroupTeachers) {
                Long groupId = gt.getGroupId();
                Long deptId = groupDeptMap.get(groupId);
                if (deptId != null && gt.getTeacherId() != null) {
                    deptTeachersSet.computeIfAbsent(deptId, k -> new java.util.HashSet<>()).add(gt.getTeacherId());
                }
            }
            
            // 转换为数量
            for (Map.Entry<Long, java.util.Set<Long>> entry : deptTeachersSet.entrySet()) {
                deptTeacherCountMap.put(entry.getKey(), entry.getValue().size());
            }
        }
        
        for (DefenseGroup group : groups) {
            // 获取小组内的学生
            List<Student> students = studentMapper.findByDefenseGroupId(group.getId());
            if (students == null || students.isEmpty()) {
                continue;
            }
            
            // 过滤年份
            if (year != null) {
                students = students.stream()
                        .filter(s -> s.getDefenseYear() != null && s.getDefenseYear().equals(year))
                        .collect(java.util.stream.Collectors.toList());
            }
            
            if (students.isEmpty()) {
                continue;
            }
            
            // 获取小组内教师数量
            List<DefenseGroupTeacher> groupTeachers = defenseGroupTeacherMapper.findByGroupId(group.getId());
            int groupTeacherCount = groupTeachers != null ? groupTeachers.size() : 0;
            
            // 找到小组内平均分最高的学生
            Student topStudent = null;
            double topAvgScore = -1;
            
            for (Student s : students) {
                List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(s.getId(), year);
                if (records == null || records.isEmpty()) {
                    continue;
                }
                
                // 检查是否所有小组内教师都已打分
                if (records.size() < groupTeacherCount) {
                    continue;
                }
                
                double avgScore = records.stream()
                        .filter(r -> r.getTotalScore() != null)
                        .mapToInt(TeacherScoreRecord::getTotalScore)
                        .average()
                        .orElse(0.0);
                
                if (avgScore > topAvgScore) {
                    topAvgScore = avgScore;
                    topStudent = s;
                }
            }
            
            if (topStudent != null) {
                Map<String, Object> candidate = new HashMap<>();
                candidate.put("groupId", group.getId());
                candidate.put("groupName", group.getName());
                candidate.put("departmentId", group.getDepartmentId()); // 添加院系ID用于过滤
                candidate.put("studentId", topStudent.getId());
                candidate.put("studentNo", topStudent.getStudentNo());
                candidate.put("studentName", topStudent.getName());
                candidate.put("defenseType", topStudent.getDefenseType());
                candidate.put("title", topStudent.getTitle());
                candidate.put("groupAvgScore", round(topAvgScore, 1));
                
                // 获取大组答辩打分情况
                List<LargeGroupScore> largeScores = largeGroupScoreMapper.findByStudentIdAndYear(topStudent.getId(), year);
                candidate.put("largeGroupScoredCount", largeScores != null ? largeScores.size() : 0);
                
                // 获取该小组所属院系的教师数量（用于显示打分进度）
                int deptTeacherCount = deptTeacherCountMap.getOrDefault(group.getDepartmentId(), 0);
                candidate.put("totalTeachersCount", deptTeacherCount);
                
                // 计算大组答辩平均分
                Double largeGroupAvgScore = null;
                if (largeScores != null && !largeScores.isEmpty()) {
                    double largeAvg = largeScores.stream()
                            .filter(ls -> ls.getScore() != null)
                            .mapToInt(LargeGroupScore::getScore)
                            .average()
                            .orElse(0.0);
                    largeGroupAvgScore = round(largeAvg, 1);
                    candidate.put("largeGroupAvgScore", largeGroupAvgScore);
                } else {
                    candidate.put("largeGroupAvgScore", null);
                }
                
                // 计算调节系数（大组答辩平均分 / 小组平均分，保留3位小数）
                Double adjustmentFactor = null;
                if (largeGroupAvgScore != null && topAvgScore > 0) {
                    adjustmentFactor = round(largeGroupAvgScore / topAvgScore, 3);
                }
                candidate.put("adjustmentFactor", adjustmentFactor);
                
                // 获取当前教师对该候选人的打分
                Integer myLargeGroupScore = null;
                if (currentTeacherId != null && largeScores != null) {
                    for (LargeGroupScore ls : largeScores) {
                        if (ls.getTeacherId() != null && ls.getTeacherId().equals(currentTeacherId)) {
                            myLargeGroupScore = ls.getScore();
                            break;
                        }
                    }
                }
                candidate.put("myLargeGroupScore", myLargeGroupScore);
                
                candidates.add(candidate);
            }
        }
        
        return candidates;
    }

    @Override
    @Transactional
    public void saveLargeGroupScore(Long studentId, Long teacherId, Integer year, Integer score) {
        if (studentId == null || teacherId == null || year == null || score == null) {
            throw new IllegalArgumentException("学生ID/教师ID/年份/分数不能为空");
        }
        
        LargeGroupScore existing = largeGroupScoreMapper.findByStudentIdAndTeacherIdAndYear(studentId, teacherId, year);
        if (existing == null) {
            LargeGroupScore record = new LargeGroupScore();
            record.setStudentId(studentId);
            record.setTeacherId(teacherId);
            record.setYear(year);
            record.setScore(score);
            largeGroupScoreMapper.insert(record);
        } else {
            existing.setScore(score);
            largeGroupScoreMapper.update(existing);
        }
        
        // 自动更新该小组的调节系数和所有学生的最终答辩成绩
        updateGroupAdjustmentFactor(studentId, year);
    }
    
    /**
     * 更新小组的调节系数和所有学生的最终答辩成绩
     * @param topStudentId 小组第一名学生的ID（参与大组答辩的学生）
     * @param year 答辩年份
     */
    private void updateGroupAdjustmentFactor(Long topStudentId, Integer year) {
        // 获取学生信息
        Student topStudent = studentMapper.findById(topStudentId);
        if (topStudent == null || topStudent.getDefenseGroupId() == null) {
            return;
        }
        
        Long groupId = topStudent.getDefenseGroupId();
        
        // 计算大组答辩平均分
        List<LargeGroupScore> largeScores = largeGroupScoreMapper.findByStudentIdAndYear(topStudentId, year);
        if (largeScores == null || largeScores.isEmpty()) {
            return;
        }
        
        double largeGroupAvgScore = largeScores.stream()
                .filter(ls -> ls.getScore() != null)
                .mapToInt(LargeGroupScore::getScore)
                .average()
                .orElse(0.0);
        
        // 计算该学生在小组中的平均分
        List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(topStudentId, year);
        if (records == null || records.isEmpty()) {
            return;
        }
        
        double topStudentGroupAvgScore = records.stream()
                .filter(r -> r.getTotalScore() != null)
                .mapToInt(TeacherScoreRecord::getTotalScore)
                .average()
                .orElse(0.0);
        
        if (topStudentGroupAvgScore <= 0) {
            return;
        }
        
        // 计算调节系数（保留3位小数）
        double adjustmentFactor = round(largeGroupAvgScore / topStudentGroupAvgScore, 3);
        
        // 获取小组内所有学生
        List<Student> students = studentMapper.findByDefenseGroupId(groupId);
        if (students == null || students.isEmpty()) {
            return;
        }
        
        // 过滤年份
        students = students.stream()
                .filter(s -> s.getDefenseYear() != null && s.getDefenseYear().equals(year))
                .collect(java.util.stream.Collectors.toList());
        
        // 更新每个学生的调节系数和最终答辩成绩
        for (Student stu : students) {
            // 计算该学生在小组中的平均分
            List<TeacherScoreRecord> stuRecords = teacherScoreRecordMapper.findByStudentIdAndYear(stu.getId(), year);
            if (stuRecords == null || stuRecords.isEmpty()) {
                continue;
            }
            
            double stuGroupAvgScore = stuRecords.stream()
                    .filter(r -> r.getTotalScore() != null)
                    .mapToInt(TeacherScoreRecord::getTotalScore)
                    .average()
                    .orElse(0.0);
            
            // 更新/插入学生最终成绩
            StudentFinalScore finalScore = studentFinalScoreMapper.findByStudentIdAndYear(stu.getId(), year);
            if (finalScore == null) {
                finalScore = new StudentFinalScore();
                finalScore.setStudentId(stu.getId());
                finalScore.setYear(year);
                studentFinalScoreMapper.insert(finalScore);
            }
            
            finalScore.setGroupAvgScore((int) Math.round(stuGroupAvgScore));
            finalScore.setAdjustmentFactor(adjustmentFactor);
            
            // 计算最终答辩成绩 = 小组平均分 × 调节系数
            double finalDefenseScore = round(stuGroupAvgScore * adjustmentFactor, 1);
            finalScore.setFinalDefenseScore(finalDefenseScore);
            
            // 仅小组第一名记录大组答辩成绩
            if (stu.getId().equals(topStudentId)) {
                finalScore.setLargeGroupScore((int) Math.round(largeGroupAvgScore));
            }
            
            // 如果已有导师/评阅成绩，则计算总评成绩（30%、3 0%、40%）
            if (finalScore.getAdvisorScore() != null && finalScore.getReviewerScore() != null) {
                double totalGrade = finalScore.getAdvisorScore() * 0.3
                        + finalScore.getReviewerScore() * 0.3
                        + finalDefenseScore * 0.4;
                finalScore.setTotalGrade(round(totalGrade, 1));
            }
            
            studentFinalScoreMapper.update(finalScore);
        }
    }

    @Override
    public Map<String, Object> getLargeGroupStudentScores(Long studentId, Integer year) {
        Map<String, Object> result = new HashMap<>();
        
        List<LargeGroupScore> scores = largeGroupScoreMapper.findByStudentIdAndYear(studentId, year);
        result.put("scores", scores != null ? scores : java.util.Collections.emptyList());
        
        if (scores != null && !scores.isEmpty()) {
            double avgScore = scores.stream()
                    .filter(s -> s.getScore() != null)
                    .mapToInt(LargeGroupScore::getScore)
                    .average()
                    .orElse(0.0);
            result.put("avgScore", round(avgScore, 1));
        } else {
            result.put("avgScore", null);
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getLargeGroupStudentScoresForAdmin(Long studentId, Integer year) {
        Map<String, Object> result = new HashMap<>();
        
        List<LargeGroupScore> scores = largeGroupScoreMapper.findByStudentIdAndYear(studentId, year);
        
        // 获取所有教师的打分记录，包含教师信息
        List<Map<String, Object>> scoreList = new java.util.ArrayList<>();
        if (scores != null) {
            for (LargeGroupScore score : scores) {
                Map<String, Object> scoreInfo = new HashMap<>();
                scoreInfo.put("id", score.getId());
                scoreInfo.put("studentId", score.getStudentId());
                scoreInfo.put("teacherId", score.getTeacherId());
                scoreInfo.put("score", score.getScore());
                scoreInfo.put("year", score.getYear());
                
                // 获取教师信息
                if (score.getTeacherId() != null) {
                    Teacher teacher = teacherMapper.findById(score.getTeacherId());
                    if (teacher != null) {
                        scoreInfo.put("teacherName", teacher.getName());
                        scoreInfo.put("teacherNo", teacher.getTeacherNo());
                    } else {
                        scoreInfo.put("teacherName", "未知");
                        scoreInfo.put("teacherNo", "");
                    }
                } else {
                    scoreInfo.put("teacherName", "未知");
                    scoreInfo.put("teacherNo", "");
                }
                
                scoreList.add(scoreInfo);
            }
        }
        
        result.put("scores", scoreList);
        
        // 计算平均分
        if (scores != null && !scores.isEmpty()) {
            double avgScore = scores.stream()
                    .filter(s -> s.getScore() != null)
                    .mapToInt(LargeGroupScore::getScore)
                    .average()
                    .orElse(0.0);
            result.put("avgScore", round(avgScore, 1));
        } else {
            result.put("avgScore", null);
        }
        
        return result;
    }

    @Override
    @Transactional
    public void updateLargeGroupScore(Long scoreId, Long studentId, Long teacherId, Integer year, Integer score) {
        if (studentId == null || teacherId == null || year == null || score == null) {
            throw new IllegalArgumentException("学生ID/教师ID/年份/分数不能为空");
        }
        
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("分数必须在0-100之间");
        }
        
        LargeGroupScore largeGroupScore;
        if (scoreId != null) {
            // 如果提供了scoreId，尝试查找现有记录
            largeGroupScore = largeGroupScoreMapper.findByStudentIdAndTeacherIdAndYear(studentId, teacherId, year);
            if (largeGroupScore == null || !largeGroupScore.getId().equals(scoreId)) {
                throw new IllegalArgumentException("打分记录不存在或不匹配");
            }
        } else {
            // 如果没有提供scoreId，查找现有记录
            largeGroupScore = largeGroupScoreMapper.findByStudentIdAndTeacherIdAndYear(studentId, teacherId, year);
        }
        
        if (largeGroupScore == null) {
            // 创建新记录
            largeGroupScore = new LargeGroupScore();
            largeGroupScore.setStudentId(studentId);
            largeGroupScore.setTeacherId(teacherId);
            largeGroupScore.setYear(year);
            largeGroupScore.setScore(score);
            largeGroupScoreMapper.insert(largeGroupScore);
        } else {
            // 更新现有记录
            largeGroupScore.setScore(score);
            largeGroupScoreMapper.update(largeGroupScore);
        }
        
        // 自动更新该小组的调节系数和所有学生的最终答辩成绩
        updateGroupAdjustmentFactor(studentId, year);
    }

    @Override
    public Double calculateGroupAvgScore(Long studentId, Integer year) {
        List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(studentId, year);
        if (records == null || records.isEmpty()) {
            return null;
        }
        
        return round(records.stream()
                .filter(r -> r.getTotalScore() != null)
                .mapToInt(TeacherScoreRecord::getTotalScore)
                .average()
                .orElse(0.0), 1);
    }

    private double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 插入/更新最终成绩的导师或评阅分，并在已有答辩成绩时同步刷新总评。
     */
    private void upsertFinalScore(Long studentId, Integer year, boolean isAdvisor, Integer score) {
        if (studentId == null || year == null || score == null) {
            throw new IllegalArgumentException("studentId/year/score 不能为空");
        }
        StudentFinalScore finalScore = studentFinalScoreMapper.findByStudentIdAndYear(studentId, year);
        if (finalScore == null) {
            finalScore = new StudentFinalScore();
            finalScore.setStudentId(studentId);
            finalScore.setYear(year);
            studentFinalScoreMapper.insert(finalScore);
        }
        if (isAdvisor) {
            finalScore.setAdvisorScore(score);
        } else {
            finalScore.setReviewerScore(score);
        }

        // 如果已有答辩成绩，刷新总评
        if (finalScore.getFinalDefenseScore() != null
                && finalScore.getAdvisorScore() != null
                && finalScore.getReviewerScore() != null) {
            double totalGrade = finalScore.getAdvisorScore() * 0.3
                    + finalScore.getReviewerScore() * 0.3
                    + finalScore.getFinalDefenseScore() * 0.4;
            finalScore.setTotalGrade(round(totalGrade, 1));
        }
        studentFinalScoreMapper.update(finalScore);
    }
}

