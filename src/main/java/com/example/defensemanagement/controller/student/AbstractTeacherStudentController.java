package com.example.defensemanagement.controller.student;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.ScoreService;
import com.example.defensemanagement.service.StudentService;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 教师侧学生管理共享基类。
 * 提供教师会话解析、学生列表组装和评分记录辅助方法。
 */
public abstract class AbstractTeacherStudentController {

    protected final StudentService studentService;
    protected final ConfigService configService;
    protected final TeacherMapper teacherMapper;
    protected final StudentFinalScoreMapper studentFinalScoreMapper;
    protected final ScoreService scoreService;
    protected final TeacherScoreRecordMapper teacherScoreRecordMapper;

    protected AbstractTeacherStudentController(
            StudentService studentService,
            ConfigService configService,
            TeacherMapper teacherMapper,
            StudentFinalScoreMapper studentFinalScoreMapper,
            ScoreService scoreService,
            TeacherScoreRecordMapper teacherScoreRecordMapper) {
        this.studentService = studentService;
        this.configService = configService;
        this.teacherMapper = teacherMapper;
        this.studentFinalScoreMapper = studentFinalScoreMapper;
        this.scoreService = scoreService;
        this.teacherScoreRecordMapper = teacherScoreRecordMapper;
    }

    protected Teacher getTeacherFromSession(HttpSession session) {
        Teacher teacher = (Teacher) session.getAttribute("currentTeacher");
        if (teacher != null) {
            return teacher;
        }

        User user = getCurrentUser(session);
        if (user != null && user.getRole() != null) {
            String roleName = user.getRole().getName();
            if ("TEACHER".equals(roleName) || "DEFENSE_LEADER".equals(roleName)) {
                teacher = teacherMapper.findByUserId(user.getId());
                if (teacher != null) {
                    return teacher;
                }
                return teacherMapper.findByTeacherNo(user.getUsername());
            }
        }
        return null;
    }

    protected User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("currentUser");
    }

    protected boolean isSuperAdmin(User user) {
        return hasRole(user, "SUPER_ADMIN");
    }

    protected boolean isDeptAdmin(User user) {
        return hasRole(user, "DEPT_ADMIN");
    }

    protected boolean isAdmin(User user) {
        return isSuperAdmin(user) || isDeptAdmin(user);
    }

    protected boolean hasRole(User user, String roleName) {
        return user != null && user.getRole() != null && roleName.equals(user.getRole().getName());
    }

    protected Map<String, Object> buildStudentListResponse(
            List<Student> students,
            Integer currentYear,
            boolean includeAdvisorInfo,
            boolean includeReviewerInfo) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> studentList = new ArrayList<>();

        if (students != null) {
            List<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toList());
            List<StudentFinalScore> scores = studentIds.isEmpty()
                    ? new ArrayList<>()
                    : studentFinalScoreMapper.findByStudentIdsAndYear(studentIds, currentYear);
            Map<Long, StudentFinalScore> scoreMap = scores.stream()
                    .collect(Collectors.toMap(StudentFinalScore::getStudentId, score -> score));

            for (Student student : students) {
                studentList.add(buildStudentInfo(student, currentYear, scoreMap, includeAdvisorInfo, includeReviewerInfo));
            }
        }

        result.put("students", studentList);
        result.put("year", currentYear);
        return result;
    }

    protected Map<String, Object> buildStudentInfo(
            Student student,
            Integer currentYear,
            Map<Long, StudentFinalScore> scoreMap,
            boolean includeAdvisorInfo,
            boolean includeReviewerInfo) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", student.getId());
        info.put("studentNo", student.getStudentNo());
        info.put("name", student.getName());
        info.put("classInfo", student.getClassInfo());

        if (student.getDepartment() != null) {
            Map<String, Object> deptMap = new HashMap<>();
            deptMap.put("name", student.getDepartment().getName());
            info.put("department", deptMap);
            info.put("departmentName", student.getDepartment().getName());
        } else {
            info.put("department", null);
            info.put("departmentName", null);
        }

        info.put("defenseType", student.getDefenseType());
        info.put("title", student.getTitle());
        info.put("defenseYear", student.getDefenseYear());
        info.put("advisorTeacherId", student.getAdvisorTeacherId());
        info.put("reviewerTeacherId", student.getReviewerTeacherId());

        if (includeAdvisorInfo) {
            if (student.getAdvisor() != null) {
                info.put("advisorName", student.getAdvisor().getName());
                info.put("advisorTeacherNo", student.getAdvisor().getTeacherNo());
            } else {
                info.put("advisorName", null);
                info.put("advisorTeacherNo", null);
            }
        }

        if (includeReviewerInfo) {
            if (student.getReviewer() != null) {
                info.put("reviewerName", student.getReviewer().getName());
                info.put("reviewerTeacherNo", student.getReviewer().getTeacherNo());
            } else {
                info.put("reviewerName", null);
                info.put("reviewerTeacherNo", null);
            }
        }

        info.put("defenseGroupId", student.getDefenseGroupId());
        info.put("defenseGroupName",
                student.getDefenseGroup() != null ? student.getDefenseGroup().getName() : null);

        Integer advisorScore = resolveTeacherScore(student.getId(), student.getAdvisorTeacherId(), currentYear);
        Integer reviewerScore = resolveTeacherScore(student.getId(), student.getReviewerTeacherId(), currentYear);
        StudentFinalScore finalScore = scoreMap.get(student.getId());

        if (advisorScore == null && finalScore != null) {
            advisorScore = finalScore.getAdvisorScore();
        }
        if (reviewerScore == null && finalScore != null) {
            reviewerScore = finalScore.getReviewerScore();
        }

        info.put("advisorScore", advisorScore);
        info.put("reviewerScore", reviewerScore);
        info.put("finalDefenseScore", finalScore != null ? finalScore.getFinalDefenseScore() : null);
        info.put("totalGrade", finalScore != null ? finalScore.getTotalGrade() : null);
        return info;
    }

    protected Integer resolveTeacherScore(Long studentId, Long teacherId, Integer currentYear) {
        if (teacherId == null) {
            return null;
        }
        TeacherScoreRecord record = teacherScoreRecordMapper.findByStudentIdAndTeacherIdAndYear(
                studentId, teacherId, currentYear);
        return record != null ? record.getTotalScore() : null;
    }

    protected Long resolveTeacherIdByType(Student student, String type) {
        if ("advisor".equals(type)) {
            return student.getAdvisorTeacherId();
        }
        if ("reviewer".equals(type)) {
            return student.getReviewerTeacherId();
        }
        return null;
    }

    protected String resolveTeacherTypeLabel(String type) {
        return "advisor".equals(type) ? "指导教师" : "评阅人";
    }

    protected TeacherScoreRecord createEmptyScoreRecord(Student student, Long teacherId, Integer currentYear) {
        TeacherScoreRecord record = new TeacherScoreRecord();
        record.setStudentId(student.getId());
        record.setTeacherId(teacherId);
        record.setYear(currentYear);
        record.setDefenseGroupId(student.getDefenseGroupId());
        record.setItem1Score(null);
        record.setItem2Score(null);
        record.setItem3Score(null);
        record.setItem4Score(null);
        record.setItem5Score(null);
        record.setItem6Score(null);
        record.setTotalScore(null);
        return record;
    }

    protected int calculateTotalScore(TeacherScoreRecord record) {
        return valueOrZero(record.getItem1Score())
                + valueOrZero(record.getItem2Score())
                + valueOrZero(record.getItem3Score())
                + valueOrZero(record.getItem4Score())
                + valueOrZero(record.getItem5Score())
                + valueOrZero(record.getItem6Score());
    }

    protected void applyScoreItems(
            TeacherScoreRecord record,
            Integer item1,
            Integer item2,
            Integer item3,
            Integer item4,
            Integer item5,
            Integer item6) {
        if (item1 != null) {
            record.setItem1Score(item1);
        }
        if (item2 != null) {
            record.setItem2Score(item2);
        }
        if (item3 != null) {
            record.setItem3Score(item3);
        }
        if (item4 != null) {
            record.setItem4Score(item4);
        }
        if (item5 != null) {
            record.setItem5Score(item5);
        }
        if (item6 != null) {
            record.setItem6Score(item6);
        }
    }

    private int valueOrZero(Integer value) {
        return value != null ? value : 0;
    }
}
