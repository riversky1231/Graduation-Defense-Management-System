package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.ScoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/defense/score")
public class LargeGroupScoreController {

    private static final Logger log = LoggerFactory.getLogger(LargeGroupScoreController.class);

    @Autowired
    private ScoreService scoreService;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private DefenseGroupMapper defenseGroupMapper;

    @Autowired
    private ConfigService configService;

    /**
     * 获取大组答辩候选人列表（每个小组最高分学生）
     * GET /defense/score/largegroup/candidates
     */
    @GetMapping("/largegroup/candidates")
    @ResponseBody
    public Map<String, Object> getLargeGroupCandidates(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        boolean isSuperAdmin = isRole(currentUser, "SUPER_ADMIN");
        boolean isDeptAdmin = isRole(currentUser, "DEPT_ADMIN");

        Teacher teacher = getTeacherFromSession(session);
        Integer year = getCurrentDefenseYear();

        Map<String, Object> result = new HashMap<>();
        if (isSuperAdmin) {
            result.put("candidates", scoreService.getLargeGroupCandidates(year, null));
            result.put("teacherId", null);
            result.put("teacherName", "超级管理员");
        } else if (isDeptAdmin) {
            List<Map<String, Object>> allCandidates = scoreService.getLargeGroupCandidates(year, null);
            Long deptId = currentUser.getDepartmentId();
            List<Map<String, Object>> deptCandidates = allCandidates.stream()
                    .filter(candidate -> sameDepartment(candidate.get("departmentId"), deptId))
                    .collect(Collectors.toList());
            result.put("candidates", deptCandidates);
            result.put("teacherId", null);
            result.put("teacherName", "院系管理员");
            result.put("isDeptAdmin", true);
        } else {
            if (teacher == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "请先登录教师账号");
                return error;
            }

            Long teacherDeptId = teacher.getDepartmentId();
            List<Map<String, Object>> allCandidates = scoreService.getLargeGroupCandidates(year, teacher.getId());
            List<Map<String, Object>> deptCandidates;
            if (teacherDeptId != null) {
                deptCandidates = allCandidates.stream()
                        .filter(candidate -> sameDepartment(candidate.get("departmentId"), teacherDeptId))
                        .collect(Collectors.toList());
            } else {
                deptCandidates = allCandidates;
            }

            result.put("candidates", deptCandidates);
            result.put("teacherId", teacher.getId());
            result.put("teacherName", teacher.getName());
        }
        result.put("year", year);
        return result;
    }

    /**
     * 保存大组答辩打分
     * POST /defense/score/largegroup/save
     */
    @PostMapping("/largegroup/save")
    @ResponseBody
    public String saveLargeGroupScore(@RequestParam Long studentId,
                                      @RequestParam Integer score,
                                      HttpSession session) {
        Teacher teacher = getTeacherFromSession(session);
        if (teacher == null) {
            return "error:请先登录教师账号";
        }

        String archived = configService.getConfigValue("LARGE_GROUP_ARCHIVED");
        if ("1".equals(archived)) {
            return "error:大组成绩已归档，无法再打分";
        }

        String deadlineStr = configService.getConfigValue("LARGE_GROUP_DEADLINE");
        if (deadlineStr != null && !deadlineStr.trim().isEmpty()) {
            try {
                LocalDateTime deadline = LocalDateTime.parse(
                        deadlineStr.trim(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                if (LocalDateTime.now().isAfter(deadline)) {
                    return "error:大组打分已截止，无法再打分";
                }
            } catch (Exception e) {
                log.warn("大组打分截止时间格式错误: {}", deadlineStr);
            }
        }

        Long teacherDeptId = teacher.getDepartmentId();
        if (teacherDeptId != null) {
            Student student = studentMapper.findById(studentId);
            if (student != null && student.getDefenseGroupId() != null) {
                DefenseGroup group = defenseGroupMapper.findById(student.getDefenseGroupId());
                if (group != null && group.getDepartmentId() != null && !teacherDeptId.equals(group.getDepartmentId())) {
                    return "error:您只能给本院系的学生打分";
                }
            }
        }

        Integer year = getCurrentDefenseYear();
        try {
            scoreService.saveLargeGroupScore(studentId, teacher.getId(), year, score);
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    /**
     * 获取大组答辩学生的所有打分及平均分
     * GET /defense/score/largegroup/student/{studentId}/scores
     */
    @GetMapping("/largegroup/student/{studentId}/scores")
    @ResponseBody
    public Map<String, Object> getLargeGroupStudentScores(@PathVariable Long studentId, HttpSession session) {
        Teacher teacher = getTeacherFromSession(session);
        if (teacher == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "请先登录教师账号");
            return error;
        }

        Integer year = getCurrentDefenseYear();
        return scoreService.getLargeGroupStudentScores(studentId, year);
    }

    /**
     * 获取大组答辩学生的所有打分详情（超级管理员使用）
     * GET /defense/score/largegroup/student/{studentId}/scores/admin
     */
    @GetMapping("/largegroup/student/{studentId}/scores/admin")
    @ResponseBody
    public Map<String, Object> getLargeGroupStudentScoresForAdmin(@PathVariable Long studentId, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (!isRole(currentUser, "SUPER_ADMIN")) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "权限不足：只有超级管理员可以查看");
            return error;
        }

        Integer year = getCurrentDefenseYear();
        return scoreService.getLargeGroupStudentScoresForAdmin(studentId, year);
    }

    /**
     * 更新大组答辩打分（超级管理员使用）
     * POST /defense/score/largegroup/update
     */
    @PostMapping("/largegroup/update")
    @ResponseBody
    public String updateLargeGroupScore(@RequestBody Map<String, Object> request, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (!isRole(currentUser, "SUPER_ADMIN")) {
            return "error:权限不足：只有超级管理员可以修改";
        }

        try {
            Long scoreId = request.get("scoreId") != null ? Long.valueOf(request.get("scoreId").toString()) : null;
            Long studentId = request.get("studentId") != null ? Long.valueOf(request.get("studentId").toString()) : null;
            Long teacherId = request.get("teacherId") != null ? Long.valueOf(request.get("teacherId").toString()) : null;
            Integer score = request.get("score") != null ? Integer.valueOf(request.get("score").toString()) : null;

            if (studentId == null || teacherId == null || score == null) {
                return "error:参数不完整";
            }
            if (score < 0 || score > 100) {
                return "error:分数必须在0-100之间";
            }

            Integer year = getCurrentDefenseYear();
            scoreService.updateLargeGroupScore(scoreId, studentId, teacherId, year, score);
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    private boolean isRole(User user, String roleName) {
        return user != null && user.getRole() != null && roleName.equals(user.getRole().getName());
    }

    private boolean sameDepartment(Object groupDeptId, Long expectedDeptId) {
        if (groupDeptId == null || expectedDeptId == null || !(groupDeptId instanceof Number)) {
            return false;
        }
        return expectedDeptId.equals(((Number) groupDeptId).longValue());
    }

    private Teacher getTeacherFromSession(HttpSession session) {
        Teacher teacher = (Teacher) session.getAttribute("currentTeacher");
        if (teacher != null) {
            log.debug("Resolved teacher from currentTeacher session attribute, teacherId={}", teacher.getId());
            return teacher;
        }

        User user = (User) session.getAttribute("currentUser");
        log.debug("Resolving teacher from currentUser session attribute, username={}", user != null ? user.getUsername() : null);
        if (user != null && user.getRole() != null) {
            String roleName = user.getRole().getName();
            log.debug("Current user role for teacher resolution={}", roleName);
            if ("TEACHER".equals(roleName) || "DEFENSE_LEADER".equals(roleName)) {
                teacher = teacherMapper.findByTeacherNo(user.getUsername());
                log.debug("Resolved teacher by username={}, teacherId={}",
                        user.getUsername(),
                        teacher != null ? teacher.getId() : null);
                return teacher;
            }
        }

        log.warn("Unable to resolve teacher from session");
        return null;
    }

    private Integer getCurrentDefenseYear() {
        String yearStr = configService.getConfigValue("CURRENT_DEFENSE_YEAR");
        if (yearStr != null && !yearStr.isEmpty()) {
            try {
                return Integer.parseInt(yearStr);
            } catch (NumberFormatException e) {
                log.warn("CURRENT_DEFENSE_YEAR 配置非法: {}", yearStr);
            }
        }
        return LocalDate.now().getYear();
    }
}
