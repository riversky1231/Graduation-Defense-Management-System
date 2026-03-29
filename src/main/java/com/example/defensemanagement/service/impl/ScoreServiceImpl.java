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
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(ScoreServiceImpl.class);

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
        groupSupport().finalizeGroupScores(defenseGroupId, year, largeGroupScore);
    }

    @Override
    public Map<String, Object> getGroupAdjustmentFactor(Long groupId, Integer year) {
        return groupSupport().getGroupAdjustmentFactor(groupId, year);
    }

    @Override
    public Map<String, Object> getTeacherGroupStudents(Long teacherId, Integer year) {
        return groupSupport().getTeacherGroupStudents(teacherId, year);
    }

    @Override
    public Map<String, Object> getAllGroupStudentsForSuperAdmin(Integer year) {
        return groupSupport().getAllGroupStudentsForSuperAdmin(year);
    }

    @Override
    public List<Map<String, Object>> getLargeGroupCandidates(Integer year, Long currentTeacherId) {
        return groupSupport().getLargeGroupCandidates(year, currentTeacherId);
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
        groupSupport().updateGroupAdjustmentFactor(studentId, year);
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
                if (score.getTeacher() != null) {
                    scoreInfo.put("teacherName", score.getTeacher().getName());
                    scoreInfo.put("teacherNo", score.getTeacher().getTeacherNo());
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
        groupSupport().updateGroupAdjustmentFactor(studentId, year);
    }

    @Override
    public Double calculateGroupAvgScore(Long studentId, Integer year) {
        return groupSupport().calculateGroupAvgScore(studentId, year);
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

    private ScoreGroupSupport groupSupport() {
        return new ScoreGroupSupport(
                teacherScoreRecordMapper,
                studentFinalScoreMapper,
                studentMapper,
                largeGroupScoreMapper,
                defenseGroupMapper,
                defenseGroupTeacherMapper
        );
    }
}

