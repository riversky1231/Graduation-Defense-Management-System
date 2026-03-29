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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 教师侧学生与评分相关端点，从 StudentController 中拆分。
 */
@Controller
@RequestMapping("/department/student")
public class TeacherStudentController {

    private static final Logger log = LoggerFactory.getLogger(TeacherStudentController.class);
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_SUMMARY_LENGTH = 2000;

    @Autowired
    private StudentService studentService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private StudentFinalScoreMapper studentFinalScoreMapper;

    @Autowired
    private ScoreService scoreService;

    @Autowired
    private TeacherScoreRecordMapper teacherScoreRecordMapper;

    private Teacher getTeacherFromSession(HttpSession session) {
        Teacher teacher = (Teacher) session.getAttribute("currentTeacher");
        if (teacher != null) {
            return teacher;
        }

        User user = (User) session.getAttribute("currentUser");
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

    @GetMapping("/teacher/advised")
    @ResponseBody
    public Map<String, Object> getAdvisedStudentsWithScores(HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        User currentUser = (User) session.getAttribute("currentUser");
        boolean isSuperAdmin = currentUser != null && currentUser.getRole() != null
                && "SUPER_ADMIN".equals(currentUser.getRole().getName());

        Teacher teacher = getTeacherFromSession(session);
        Integer currentYear = configService.getCurrentDefenseYear();

        List<Student> students;
        if (isSuperAdmin) {
            students = currentYear == null ? studentService.findAll() : studentService.findByYear(currentYear);
            result.put("teacherId", null);
            result.put("teacherName", "超级管理员");
        } else {
            if (teacher == null) {
                result.put("error", "请先登录教师账号");
                return result;
            }
            if (currentYear == null) {
                result.put("error", "请先设置当前答辩年份");
                return result;
            }
            students = studentService.getStudentsByAdvisor(teacher.getId(), currentYear);
            result.put("teacherId", teacher.getId());
            result.put("teacherName", teacher.getName());
        }

        List<Map<String, Object>> studentList = new ArrayList<>();
        if (students != null) {
            List<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toList());
            List<StudentFinalScore> scores = studentIds.isEmpty() ? new ArrayList<>()
                    : studentFinalScoreMapper.findByStudentIdsAndYear(studentIds, currentYear);
            Map<Long, StudentFinalScore> scoreMap = scores.stream()
                    .collect(Collectors.toMap(StudentFinalScore::getStudentId, s -> s));

            for (Student s : students) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", s.getId());
                info.put("studentNo", s.getStudentNo());
                info.put("name", s.getName());
                info.put("classInfo", s.getClassInfo());
                if (s.getDepartment() != null) {
                    Map<String, Object> deptMap = new HashMap<>();
                    deptMap.put("name", s.getDepartment().getName());
                    info.put("department", deptMap);
                    info.put("departmentName", s.getDepartment().getName());
                } else {
                    info.put("department", null);
                    info.put("departmentName", null);
                }
                info.put("defenseType", s.getDefenseType());
                info.put("title", s.getTitle());
                info.put("defenseYear", s.getDefenseYear());
                info.put("advisorTeacherId", s.getAdvisorTeacherId());
                info.put("reviewerTeacherId", s.getReviewerTeacherId());

                if (s.getReviewer() != null) {
                    info.put("reviewerName", s.getReviewer().getName());
                    info.put("reviewerTeacherNo", s.getReviewer().getTeacherNo());
                } else {
                    info.put("reviewerName", null);
                    info.put("reviewerTeacherNo", null);
                }

                info.put("defenseGroupId", s.getDefenseGroupId());
                if (s.getDefenseGroup() != null) {
                    info.put("defenseGroupName", s.getDefenseGroup().getName());
                } else {
                    info.put("defenseGroupName", null);
                }

                Integer advisorScore = null;
                Integer reviewerScore = null;
                if (s.getAdvisorTeacherId() != null) {
                    TeacherScoreRecord advisorRecord = teacherScoreRecordMapper.findByStudentIdAndTeacherIdAndYear(
                            s.getId(), s.getAdvisorTeacherId(), currentYear);
                    if (advisorRecord != null && advisorRecord.getTotalScore() != null) {
                        advisorScore = advisorRecord.getTotalScore();
                    }
                }
                if (advisorScore == null) {
                    StudentFinalScore score = scoreMap.get(s.getId());
                    if (score != null) {
                        advisorScore = score.getAdvisorScore();
                    }
                }
                if (s.getReviewerTeacherId() != null) {
                    TeacherScoreRecord reviewerRecord = teacherScoreRecordMapper.findByStudentIdAndTeacherIdAndYear(
                            s.getId(), s.getReviewerTeacherId(), currentYear);
                    if (reviewerRecord != null && reviewerRecord.getTotalScore() != null) {
                        reviewerScore = reviewerRecord.getTotalScore();
                    }
                }
                if (reviewerScore == null) {
                    StudentFinalScore score = scoreMap.get(s.getId());
                    if (score != null) {
                        reviewerScore = score.getReviewerScore();
                    }
                }

                StudentFinalScore score = scoreMap.get(s.getId());
                info.put("advisorScore", advisorScore);
                info.put("reviewerScore", reviewerScore);
                if (score != null) {
                    info.put("finalDefenseScore", score.getFinalDefenseScore());
                    info.put("totalGrade", score.getTotalGrade());
                } else {
                    info.put("finalDefenseScore", null);
                    info.put("totalGrade", null);
                }
                studentList.add(info);
            }
        }

        result.put("students", studentList);
        result.put("year", currentYear);
        return result;
    }

    @PostMapping("/teacher/setAdvisorScore")
    @ResponseBody
    public String setAdvisorScoreByTeacher(@RequestParam Long studentId, @RequestParam Integer score,
                                           HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        boolean isSuperAdmin = currentUser != null && currentUser.getRole() != null
                && "SUPER_ADMIN".equals(currentUser.getRole().getName());

        Teacher teacher = getTeacherFromSession(session);
        if (teacher == null && !isSuperAdmin) {
            return "error:请先登录教师账号";
        }

        Integer currentYear = configService.getCurrentDefenseYear();
        if (currentYear == null) {
            return "error:请先设置当前答辩年份";
        }

        Student student = studentService.findById(studentId);
        if (student == null) {
            return "error:学生不存在";
        }

        if (!isSuperAdmin && teacher != null
                && (student.getAdvisorTeacherId() == null || !student.getAdvisorTeacherId().equals(teacher.getId()))) {
            return "error:您不是该学生的指导教师";
        }

        try {
            scoreService.setAdvisorScore(studentId, currentYear, score);
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @PostMapping("/teacher/assignReviewer")
    @ResponseBody
    public String assignReviewerByTeacher(@RequestParam Long studentId, @RequestParam Long reviewerId,
                                          HttpSession session) {
        Teacher teacher = getTeacherFromSession(session);
        if (teacher == null) {
            return "error:请先登录教师账号";
        }

        Student student = studentService.findById(studentId);
        if (student == null) {
            return "error:学生不存在";
        }
        if (student.getAdvisorTeacherId() == null || !student.getAdvisorTeacherId().equals(teacher.getId())) {
            return "error:您不是该学生的指导教师";
        }

        try {
            studentService.assignReviewer(studentId, reviewerId);
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @PostMapping("/teacher/updateStudentInfo")
    @ResponseBody
    public String updateStudentInfoByTeacher(@RequestParam Long studentId,
                                             @RequestParam(required = false) String title,
                                             @RequestParam(required = false) String summary,
                                             HttpSession session) {
        Teacher teacher = getTeacherFromSession(session);
        if (teacher == null) {
            return "error:请先登录教师账号";
        }

        if (title != null && title.length() > MAX_TITLE_LENGTH) {
            return "error:题目不能超过" + MAX_TITLE_LENGTH + "个字符";
        }
        if (summary != null && summary.length() > MAX_SUMMARY_LENGTH) {
            return "error:摘要不能超过" + MAX_SUMMARY_LENGTH + "个字符";
        }

        try {
            Student student = studentService.findById(studentId);
            if (student == null) {
                return "error:学生不存在";
            }
            if (student.getAdvisorTeacherId() == null || !student.getAdvisorTeacherId().equals(teacher.getId())) {
                return "error:您不是该学生的指导教师";
            }

            Student update = new Student();
            update.setId(studentId);
            update.setTitle(title);
            update.setSummary(summary);
            studentService.saveStudent(update);
            return "success";
        } catch (DataAccessException e) {
            log.error("更新学生题目摘要时发生数据库错误，studentId={}", studentId, e);
            return "error:数据库操作失败，请稍后重试";
        } catch (Exception e) {
            log.error("更新学生题目摘要时发生未知错误，studentId={}", studentId, e);
            return "error:系统错误，请稍后重试";
        }
    }

    @GetMapping("/teacher/reviewed")
    @ResponseBody
    public Map<String, Object> getReviewedStudentsWithScores(HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        User currentUser = (User) session.getAttribute("currentUser");
        boolean isSuperAdmin = currentUser != null && currentUser.getRole() != null
                && "SUPER_ADMIN".equals(currentUser.getRole().getName());
        boolean isDeptAdmin = currentUser != null && currentUser.getRole() != null
                && "DEPT_ADMIN".equals(currentUser.getRole().getName());

        Teacher teacher = getTeacherFromSession(session);
        Integer currentYear = configService.getCurrentDefenseYear();

        List<Student> students;
        if (isSuperAdmin) {
            students = currentYear == null ? studentService.findAll() : studentService.findByYear(currentYear);
            result.put("teacherId", null);
            result.put("teacherName", "超级管理员");
        } else if (isDeptAdmin) {
            students = studentService.findByDepartmentAndYear(currentUser.getDepartmentId(), currentYear);
            result.put("teacherId", null);
            result.put("teacherName", "院系管理员");
            result.put("isDeptAdmin", true);
        } else {
            if (teacher == null) {
                result.put("error", "请先登录教师账号");
                return result;
            }
            if (currentYear == null) {
                result.put("error", "请先设置当前答辩年份");
                return result;
            }
            students = studentService.getStudentsByReviewer(teacher.getId(), currentYear);
            result.put("teacherId", teacher.getId());
            result.put("teacherName", teacher.getName());
        }

        List<Map<String, Object>> studentList = new ArrayList<>();
        if (students != null) {
            List<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toList());
            List<StudentFinalScore> scores = studentIds.isEmpty() ? new ArrayList<>()
                    : studentFinalScoreMapper.findByStudentIdsAndYear(studentIds, currentYear);
            Map<Long, StudentFinalScore> scoreMap = scores.stream()
                    .collect(Collectors.toMap(StudentFinalScore::getStudentId, s -> s));

            for (Student s : students) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", s.getId());
                info.put("studentNo", s.getStudentNo());
                info.put("name", s.getName());
                info.put("classInfo", s.getClassInfo());
                if (s.getDepartment() != null) {
                    Map<String, Object> deptMap = new HashMap<>();
                    deptMap.put("name", s.getDepartment().getName());
                    info.put("department", deptMap);
                    info.put("departmentName", s.getDepartment().getName());
                } else {
                    info.put("department", null);
                    info.put("departmentName", null);
                }
                info.put("defenseType", s.getDefenseType());
                info.put("title", s.getTitle());
                info.put("defenseYear", s.getDefenseYear());
                info.put("advisorTeacherId", s.getAdvisorTeacherId());
                info.put("reviewerTeacherId", s.getReviewerTeacherId());

                if (s.getAdvisor() != null) {
                    info.put("advisorName", s.getAdvisor().getName());
                    info.put("advisorTeacherNo", s.getAdvisor().getTeacherNo());
                } else {
                    info.put("advisorName", null);
                    info.put("advisorTeacherNo", null);
                }
                if (s.getReviewer() != null) {
                    info.put("reviewerName", s.getReviewer().getName());
                    info.put("reviewerTeacherNo", s.getReviewer().getTeacherNo());
                } else {
                    info.put("reviewerName", null);
                    info.put("reviewerTeacherNo", null);
                }
                info.put("defenseGroupId", s.getDefenseGroupId());
                if (s.getDefenseGroup() != null) {
                    info.put("defenseGroupName", s.getDefenseGroup().getName());
                } else {
                    info.put("defenseGroupName", null);
                }

                Integer advisorScore = null;
                Integer reviewerScore = null;
                if (s.getAdvisorTeacherId() != null) {
                    TeacherScoreRecord advisorRecord = teacherScoreRecordMapper.findByStudentIdAndTeacherIdAndYear(
                            s.getId(), s.getAdvisorTeacherId(), currentYear);
                    if (advisorRecord != null && advisorRecord.getTotalScore() != null) {
                        advisorScore = advisorRecord.getTotalScore();
                    }
                }
                if (advisorScore == null) {
                    StudentFinalScore score = scoreMap.get(s.getId());
                    if (score != null) {
                        advisorScore = score.getAdvisorScore();
                    }
                }
                if (s.getReviewerTeacherId() != null) {
                    TeacherScoreRecord reviewerRecord = teacherScoreRecordMapper.findByStudentIdAndTeacherIdAndYear(
                            s.getId(), s.getReviewerTeacherId(), currentYear);
                    if (reviewerRecord != null && reviewerRecord.getTotalScore() != null) {
                        reviewerScore = reviewerRecord.getTotalScore();
                    }
                }
                if (reviewerScore == null) {
                    StudentFinalScore score = scoreMap.get(s.getId());
                    if (score != null) {
                        reviewerScore = score.getReviewerScore();
                    }
                }

                StudentFinalScore score = scoreMap.get(s.getId());
                info.put("advisorScore", advisorScore);
                info.put("reviewerScore", reviewerScore);
                if (score != null) {
                    info.put("finalDefenseScore", score.getFinalDefenseScore());
                    info.put("totalGrade", score.getTotalGrade());
                } else {
                    info.put("finalDefenseScore", null);
                    info.put("totalGrade", null);
                }
                studentList.add(info);
            }
        }

        result.put("students", studentList);
        result.put("year", currentYear);
        return result;
    }

    @PostMapping("/teacher/setReviewerScore")
    @ResponseBody
    public String setReviewerScoreByTeacher(@RequestParam Long studentId, @RequestParam Integer score,
                                            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        boolean isSuperAdmin = currentUser != null && currentUser.getRole() != null
                && "SUPER_ADMIN".equals(currentUser.getRole().getName());

        Teacher teacher = getTeacherFromSession(session);
        if (teacher == null && !isSuperAdmin) {
            return "error:请先登录教师账号";
        }

        Integer currentYear = configService.getCurrentDefenseYear();
        if (currentYear == null) {
            return "error:请先设置当前答辩年份";
        }

        Student student = studentService.findById(studentId);
        if (student == null) {
            return "error:学生不存在";
        }

        if (!isSuperAdmin && teacher != null
                && (student.getReviewerTeacherId() == null || !student.getReviewerTeacherId().equals(teacher.getId()))) {
            return "error:您不是该学生的评阅人";
        }

        try {
            scoreService.setReviewerScore(studentId, currentYear, score);
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @GetMapping("/teacher/scoreRecord")
    @ResponseBody
    public Map<String, Object> getTeacherScoreRecord(@RequestParam Long studentId, @RequestParam String type,
                                                     HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        User currentUser = (User) session.getAttribute("currentUser");
        boolean isSuperAdmin = currentUser != null && currentUser.getRole() != null
                && "SUPER_ADMIN".equals(currentUser.getRole().getName());

        Teacher currentTeacher = getTeacherFromSession(session);
        Integer currentYear = configService.getCurrentDefenseYear();
        if (currentYear == null) {
            result.put("error", "请先设置当前答辩年份");
            return result;
        }

        Student student = studentService.findById(studentId);
        if (student == null) {
            result.put("error", "学生不存在");
            return result;
        }

        Long teacherId;
        if ("advisor".equals(type)) {
            teacherId = student.getAdvisorTeacherId();
        } else if ("reviewer".equals(type)) {
            teacherId = student.getReviewerTeacherId();
        } else {
            result.put("error", "类型参数错误，应为advisor或reviewer");
            return result;
        }

        if (teacherId == null) {
            result.put("error", "该学生未分配" + ("advisor".equals(type) ? "指导教师" : "评阅人"));
            return result;
        }

        if (!isSuperAdmin) {
            if (currentTeacher == null) {
                result.put("error", "权限不足：请先登录教师账号");
                return result;
            }
            if (!currentTeacher.getId().equals(teacherId)) {
                result.put("error", "权限不足：您不是该学生的" + ("advisor".equals(type) ? "指导教师" : "评阅人"));
                return result;
            }
        }

        TeacherScoreRecord record = teacherScoreRecordMapper.findByStudentIdAndTeacherIdAndYear(studentId, teacherId,
                currentYear);
        if (record == null) {
            record = new TeacherScoreRecord();
            record.setStudentId(studentId);
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
        }

        result.put("record", record);
        result.put("student", student);
        Teacher teacher = teacherMapper.findById(teacherId);
        if (teacher != null) {
            result.put("teacherName", teacher.getName());
        }
        return result;
    }

    @PostMapping("/teacher/updateScoreRecord")
    @ResponseBody
    public String updateTeacherScoreRecord(
            @RequestParam Long studentId,
            @RequestParam String type,
            @RequestParam(required = false) Integer item1,
            @RequestParam(required = false) Integer item2,
            @RequestParam(required = false) Integer item3,
            @RequestParam(required = false) Integer item4,
            @RequestParam(required = false) Integer item5,
            @RequestParam(required = false) Integer item6,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("currentUser");
        Teacher currentTeacher = getTeacherFromSession(session);
        boolean isAdmin = false;
        if (currentUser != null && currentUser.getRole() != null) {
            String roleName = currentUser.getRole().getName();
            isAdmin = "SUPER_ADMIN".equals(roleName) || "DEPT_ADMIN".equals(roleName);
        }

        Integer currentYear = configService.getCurrentDefenseYear();
        if (currentYear == null) {
            return "error:请先设置当前答辩年份";
        }

        Student student = studentService.findById(studentId);
        if (student == null) {
            return "error:学生不存在";
        }

        Long teacherId;
        if ("advisor".equals(type)) {
            teacherId = student.getAdvisorTeacherId();
        } else if ("reviewer".equals(type)) {
            teacherId = student.getReviewerTeacherId();
        } else {
            return "error:类型参数错误";
        }

        if (teacherId == null) {
            return "error:该学生未分配" + ("advisor".equals(type) ? "指导教师" : "评阅人");
        }

        if (!isAdmin) {
            if (currentTeacher == null) {
                return "error:权限不足：请先登录教师账号";
            }
            if (!currentTeacher.getId().equals(teacherId)) {
                return "error:权限不足：您不是该学生的" + ("advisor".equals(type) ? "指导教师" : "评阅人");
            }
        }

        TeacherScoreRecord record = teacherScoreRecordMapper.findByStudentIdAndTeacherIdAndYear(studentId, teacherId,
                currentYear);
        boolean isNewRecord = false;
        if (record == null) {
            record = new TeacherScoreRecord();
            record.setStudentId(studentId);
            record.setTeacherId(teacherId);
            record.setYear(currentYear);
            record.setDefenseGroupId(student.getDefenseGroupId());
            record.setSubmitTime(java.time.LocalDateTime.now());
            isNewRecord = true;
        }

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

        int total = (record.getItem1Score() != null ? record.getItem1Score() : 0)
                + (record.getItem2Score() != null ? record.getItem2Score() : 0)
                + (record.getItem3Score() != null ? record.getItem3Score() : 0)
                + (record.getItem4Score() != null ? record.getItem4Score() : 0)
                + (record.getItem5Score() != null ? record.getItem5Score() : 0)
                + (record.getItem6Score() != null ? record.getItem6Score() : 0);
        record.setTotalScore(total);

        try {
            if (isNewRecord) {
                teacherScoreRecordMapper.insert(record);
            } else {
                teacherScoreRecordMapper.update(record);
            }
            if ("advisor".equals(type)) {
                scoreService.setAdvisorScore(studentId, currentYear, total);
            } else {
                scoreService.setReviewerScore(studentId, currentYear, total);
            }
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @GetMapping("/teacher/reviewerCandidates")
    @ResponseBody
    public List<Map<String, Object>> getReviewerCandidates(HttpSession session) {
        Teacher currentTeacher = getTeacherFromSession(session);
        List<Map<String, Object>> candidates = new ArrayList<>();

        User currentUser = (User) session.getAttribute("currentUser");
        Long departmentId = null;
        if (currentTeacher != null) {
            departmentId = currentTeacher.getDepartmentId();
        } else if (currentUser != null) {
            departmentId = currentUser.getDepartmentId();
        }

        List<Teacher> teachers = teacherMapper.findByDepartmentId(departmentId);
        if (teachers != null) {
            for (Teacher t : teachers) {
                if (currentTeacher != null && t.getId().equals(currentTeacher.getId())) {
                    continue;
                }
                Map<String, Object> info = new HashMap<>();
                info.put("id", t.getId());
                info.put("teacherNo", t.getTeacherNo());
                info.put("name", t.getName());
                info.put("title", t.getTitle());
                candidates.add(info);
            }
        }

        return candidates;
    }
}
