package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.service.ScoreService;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.DefenseGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/defense/score")
public class ScoreController {

    private static final Logger log = LoggerFactory.getLogger(ScoreController.class);

    @Autowired
    private ScoreService scoreService;

    @Autowired
    private AuthService authService;

    @Autowired
    private TeacherScoreRecordMapper teacherScoreRecordMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private DefenseGroupMapper defenseGroupMapper;

    @Autowired
    private ConfigService configService;

    /**
     * 教师提交/更新打分。
     */
    @PostMapping("/teacher/save")
    public String saveTeacherScore(@RequestBody TeacherScoreRecord record) {
        scoreService.saveTeacherScore(record);
        return "success";
    }

    /**
     * 教师小组打分（表单提交）
     * POST /defense/score/teacher/group/score
     */
    @PostMapping("/teacher/group/score")
    @ResponseBody
    public String saveTeacherGroupScore(
            @RequestParam Long studentId,
            @RequestParam Integer totalScore,
            @RequestParam(required = false) Integer item1,
            @RequestParam(required = false) Integer item2,
            @RequestParam(required = false) Integer item3,
            @RequestParam(required = false) Integer item4,
            @RequestParam(required = false) Integer item5,
            @RequestParam(required = false) Integer item6,
            HttpSession session) {
        
        Teacher teacher = getTeacherFromSession(session);
        if (teacher == null) {
            return "error:请先登录教师账号";
        }
        
        Integer year = getCurrentDefenseYear();
        
        try {
            TeacherScoreRecord record = new TeacherScoreRecord();
            record.setStudentId(studentId);
            record.setTeacherId(teacher.getId());
            record.setYear(year);
            record.setItem1Score(item1);
            record.setItem2Score(item2);
            record.setItem3Score(item3);
            record.setItem4Score(item4);
            record.setItem5Score(item5);
            record.setItem6Score(item6);
            record.setTotalScore(totalScore);
            
            // 查找学生所在小组
            com.example.defensemanagement.entity.Student student = studentMapper.findById(studentId);
            if (student != null && student.getDefenseGroupId() != null) {
                record.setDefenseGroupId(student.getDefenseGroupId());
            }
            
            scoreService.saveTeacherScore(record);
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    /**
     * 小组得分汇总：计算小组均分、调节系数、最终答辩成绩、总评成绩。
     */
    @PostMapping("/group/finalize")
    public String finalizeGroup(@RequestParam Long groupId,
            @RequestParam Integer year,
            @RequestParam(required = false) Integer largeGroupScore) {
        scoreService.finalizeGroupScores(groupId, year, largeGroupScore);
        return "success";
    }

    /**
     * 获取小组的调节系数（答辩组长可以查看）
     * GET /defense/score/group/{groupId}/adjustmentFactor?year=2024
     */
    @GetMapping("/group/{groupId}/adjustmentFactor")
    public Map<String, Object> getAdjustmentFactor(@PathVariable Long groupId,
                                                    @RequestParam Integer year) {
        return scoreService.getGroupAdjustmentFactor(groupId, year);
    }

    /**
     * 设计类：输入总分，自动按权值拆分六个小项并保存。
     */
    @PostMapping("/design/autoSplit")
    public String autoSplitDesign(@RequestParam Long studentId,
            @RequestParam Long teacherId,
            @RequestParam Integer year,
            @RequestParam Integer totalScore,
            @RequestParam(required = false) Long defenseGroupId) {
        scoreService.autoSplitDesignScore(studentId, teacherId, year, totalScore, defenseGroupId);
        return "success";
    }

    /**
     * 通用分项自动生成：输入总分返回分项（不落库）。
     * POST /defense/score/teacher/auto-split?defenseType=PAPER|DESIGN&totalScore=88
     */
    @PostMapping("/teacher/auto-split")
    @ResponseBody
    public Map<String, Object> autoSplitItems(@RequestParam String defenseType,
                                              @RequestParam Integer totalScore,
                                              HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Teacher teacher = getTeacherFromSession(session);
        User currentUser = (User) session.getAttribute("currentUser");
        if (teacher == null && currentUser == null) {
            result.put("success", false);
            result.put("error", "未登录");
            return result;
        }
        try {
            Map<String, Integer> items = scoreService.autoSplitScoreItems(defenseType, totalScore);
            result.put("success", true);
            result.put("items", items);
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * 设置指导教师成绩
     */
    @PostMapping("/advisor/set")
    public String setAdvisorScore(@RequestParam Long studentId,
            @RequestParam Integer year,
            @RequestParam Integer score) {
        scoreService.setAdvisorScore(studentId, year, score);
        return "success";
    }

    /**
     * 设置评阅人成绩
     */
    @PostMapping("/reviewer/set")
    public String setReviewerScore(@RequestParam Long studentId,
            @RequestParam Integer year,
            @RequestParam Integer score) {
        scoreService.setReviewerScore(studentId, year, score);
        return "success";
    }

    /**
     * 获取所有打分记录（超级管理员用）
     * GET /defense/score/records/list?year=2024
     */
    @GetMapping("/records/list")
    @ResponseBody
    public List<TeacherScoreRecord> getAllScoreRecords(@RequestParam(required = false) Integer year,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "SUPER_ADMIN_ACCESS")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "权限不足");
        }

        if (year != null) {
            return teacherScoreRecordMapper.findByYear(year);
        } else {
            return teacherScoreRecordMapper.findAll();
        }
    }

    /**
     * 根据ID获取打分记录
     * GET /defense/score/records/{id}
     */
    @GetMapping("/records/{id}")
    @ResponseBody
    public TeacherScoreRecord getScoreRecordById(@PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "SUPER_ADMIN_ACCESS")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "权限不足");
        }
        return teacherScoreRecordMapper.findById(id);
    }

    /**
     * 更新打分记录
     * PUT /defense/score/records/update
     */
    @PutMapping("/records/update")
    @ResponseBody
    public String updateScoreRecord(@RequestBody TeacherScoreRecord record, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "SUPER_ADMIN_ACCESS")) {
            return "error:权限不足";
        }

        try {
            if (teacherScoreRecordMapper.update(record) > 0) {
                return "success";
            } else {
                return "error:更新失败";
            }
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    /**
     * 删除打分记录
     * DELETE /defense/score/records/{id}
     */
    @DeleteMapping("/records/{id}")
    @ResponseBody
    public String deleteScoreRecord(@PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "SUPER_ADMIN_ACCESS")) {
            return "error:权限不足";
        }

        try {
            if (teacherScoreRecordMapper.deleteById(id) > 0) {
                return "success";
            } else {
                return "error:删除失败";
            }
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    // ======================= 教师小组打分相关 API =======================

    /**
     * 获取当前教师所在小组的学生列表（含打分状态）
     * GET /defense/score/teacher/group/students
     */
    @GetMapping("/teacher/group/students")
    @ResponseBody
    public Map<String, Object> getTeacherGroupStudents(HttpSession session) {
        // 检查是否是超级管理员或院系管理员
        User currentUser = (User) session.getAttribute("currentUser");
        boolean isSuperAdmin = currentUser != null && currentUser.getRole() != null && 
                               "SUPER_ADMIN".equals(currentUser.getRole().getName());
        boolean isDeptAdmin = currentUser != null && currentUser.getRole() != null && 
                              "DEPT_ADMIN".equals(currentUser.getRole().getName());
        
        Teacher teacher = getTeacherFromSession(session);
        Integer year = getCurrentDefenseYear();
        
        Map<String, Object> result;
        if (isSuperAdmin) {
            // 超级管理员：返回所有小组的所有学生
            result = scoreService.getAllGroupStudentsForSuperAdmin(year);
            result.put("teacherId", null);
            result.put("teacherName", "超级管理员");
        } else if (isDeptAdmin) {
            // 院系管理员：返回本院系的所有小组学生
            Map<String, Object> allData = scoreService.getAllGroupStudentsForSuperAdmin(year);
            Long deptId = currentUser.getDepartmentId();
            // 过滤本院系的小组
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> allGroups = (List<Map<String, Object>>) allData.get("groups");
            if (allGroups != null) {
                List<Map<String, Object>> deptGroups = allGroups.stream()
                        .filter(g -> {
                            Object groupDeptId = g.get("departmentId");
                            return groupDeptId != null && groupDeptId.equals(deptId);
                        })
                        .collect(java.util.stream.Collectors.toList());
                allData.put("groups", deptGroups);
            }
            result = allData;
            result.put("teacherId", null);
            result.put("teacherName", "院系管理员");
            result.put("isDeptAdmin", true);
        } else {
            // 普通教师：返回自己所在小组的学生
            if (teacher == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "请先登录教师账号");
                return error;
            }
            result = scoreService.getTeacherGroupStudents(teacher.getId(), year);
            result.put("teacherId", teacher.getId());
            result.put("teacherName", teacher.getName());
        }
        result.put("year", year);
        return result;
    }

    /**
     * 获取当前教师ID
     * GET /defense/score/teacher/current
     */
    @GetMapping("/teacher/current")
    @ResponseBody
    public Map<String, Object> getCurrentTeacher(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Teacher teacher = getTeacherFromSession(session);
        if (teacher != null) {
            result.put("teacherId", teacher.getId());
            result.put("teacherNo", teacher.getTeacherNo());
            result.put("teacherName", teacher.getName());
        } else {
            result.put("teacherId", null);
        }
        return result;
    }

    // ======================= 大组答辩相关 API =======================

    /**
     * 获取大组答辩候选人列表（每个小组最高分学生）
     * GET /defense/score/largegroup/candidates
     */
    @GetMapping("/largegroup/candidates")
    @ResponseBody
    public Map<String, Object> getLargeGroupCandidates(HttpSession session) {
        // 检查是否是超级管理员或院系管理员
        User currentUser = (User) session.getAttribute("currentUser");
        boolean isSuperAdmin = currentUser != null && currentUser.getRole() != null && 
                               "SUPER_ADMIN".equals(currentUser.getRole().getName());
        boolean isDeptAdmin = currentUser != null && currentUser.getRole() != null && 
                              "DEPT_ADMIN".equals(currentUser.getRole().getName());
        
        Teacher teacher = getTeacherFromSession(session);
        Integer year = getCurrentDefenseYear();
        
        Map<String, Object> result = new HashMap<>();
        if (isSuperAdmin) {
            // 超级管理员：返回所有候选人（teacherId为null表示查看所有教师的打分）
            result.put("candidates", scoreService.getLargeGroupCandidates(year, null));
            result.put("teacherId", null);
            result.put("teacherName", "超级管理员");
        } else if (isDeptAdmin) {
            // 院系管理员：返回本院系的候选人（不需要打分权限）
            List<Map<String, Object>> allCandidates = scoreService.getLargeGroupCandidates(year, null);
            Long deptId = currentUser.getDepartmentId();
            // 按院系过滤候选人
            List<Map<String, Object>> deptCandidates = allCandidates.stream()
                    .filter(c -> {
                        Object groupDeptId = c.get("departmentId");
                        if (groupDeptId == null || deptId == null) return false;
                        // 统一转换为Long进行比较
                        Long gDeptId = groupDeptId instanceof Number ? ((Number) groupDeptId).longValue() : null;
                        return deptId.equals(gDeptId);
                    })
                    .collect(java.util.stream.Collectors.toList());
            result.put("candidates", deptCandidates);
            result.put("teacherId", null);
            result.put("teacherName", "院系管理员");
            result.put("isDeptAdmin", true);
        } else {
            // 普通教师：返回本院系的候选人（院系隔离）
            if (teacher == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "请先登录教师账号");
                return error;
            }
            
            // 获取教师的院系ID
            Long teacherDeptId = teacher.getDepartmentId();
            
            // 获取所有候选人
            List<Map<String, Object>> allCandidates = scoreService.getLargeGroupCandidates(year, teacher.getId());
            
            // 按院系过滤（教师只能看到本院系的候选人）
            List<Map<String, Object>> deptCandidates;
            if (teacherDeptId != null) {
                deptCandidates = allCandidates.stream()
                        .filter(c -> {
                            Object groupDeptId = c.get("departmentId");
                            if (groupDeptId == null) return false;
                            Long gDeptId = groupDeptId instanceof Number ? ((Number) groupDeptId).longValue() : null;
                            return teacherDeptId.equals(gDeptId);
                        })
                        .collect(java.util.stream.Collectors.toList());
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

        // 检查是否已归档
        String archived = configService.getConfigValue("LARGE_GROUP_ARCHIVED");
        if ("1".equals(archived)) {
            return "error:大组成绩已归档，无法再打分";
        }

        // 检查截止时间
        String deadlineStr = configService.getConfigValue("LARGE_GROUP_DEADLINE");
        if (deadlineStr != null && !deadlineStr.trim().isEmpty()) {
            try {
                java.time.LocalDateTime deadline = java.time.LocalDateTime.parse(
                        deadlineStr.trim(),
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                if (java.time.LocalDateTime.now().isAfter(deadline)) {
                    return "error:大组打分已截止，无法再打分";
                }
            } catch (Exception e) {
                // 解析失败则忽略截止时间检查
                System.err.println("大组打分截止时间格式错误: " + deadlineStr);
            }
        }
        
        // 院系隔离校验：检查学生所属小组的院系是否与教师院系一致
        Long teacherDeptId = teacher.getDepartmentId();
        if (teacherDeptId != null) {
            Student student = studentMapper.findById(studentId);
            if (student != null && student.getDefenseGroupId() != null) {
                DefenseGroup group = defenseGroupMapper.findById(student.getDefenseGroupId());
                if (group != null && group.getDepartmentId() != null) {
                    if (!teacherDeptId.equals(group.getDepartmentId())) {
                        return "error:您只能给本院系的学生打分";
                    }
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
        // 检查是否是超级管理员
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || currentUser.getRole() == null || 
            !"SUPER_ADMIN".equals(currentUser.getRole().getName())) {
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
        // 检查是否是超级管理员
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || currentUser.getRole() == null || 
            !"SUPER_ADMIN".equals(currentUser.getRole().getName())) {
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

    // ======================= 辅助方法 =======================

    /**
     * 从 Session 中获取当前教师
     */
    private Teacher getTeacherFromSession(HttpSession session) {
        // 先尝试从 session 获取教师
        Teacher teacher = (Teacher) session.getAttribute("currentTeacher");
        if (teacher != null) {
            log.debug("Resolved teacher from currentTeacher session attribute, teacherId={}", teacher.getId());
            return teacher;
        }
        
        // 如果是 User 登录，检查是否是教师角色
        User user = (User) session.getAttribute("currentUser");
        log.debug("Resolving teacher from currentUser session attribute, username={}", user != null ? user.getUsername() : null);
        if (user != null) {
            if (user.getRole() != null) {
                String roleName = user.getRole().getName();
                log.debug("Current user role for teacher resolution={}", roleName);
                if ("TEACHER".equals(roleName) || "DEFENSE_LEADER".equals(roleName)) {
                    // 根据 username 查找对应的教师
                    teacher = teacherMapper.findByTeacherNo(user.getUsername());
                    log.debug("Resolved teacher by username={}, teacherId={}",
                            user.getUsername(),
                            teacher != null ? teacher.getId() : null);
                    return teacher;
                }
            }
        }
        
        log.warn("Unable to resolve teacher from session");
        return null;
    }

    /**
     * 获取当前答辩年份
     */
    private Integer getCurrentDefenseYear() {
        String yearStr = configService.getConfigValue("CURRENT_DEFENSE_YEAR");
        if (yearStr != null && !yearStr.isEmpty()) {
            try {
                return Integer.parseInt(yearStr);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return java.time.LocalDate.now().getYear();
    }
}
