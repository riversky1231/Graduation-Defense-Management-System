package com.example.defensemanagement.controller;

import com.example.defensemanagement.common.ApiResponse;
import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.TeacherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Random;

/**
 * 院系管理员：小组-教师分配与组长设置
 */
@RestController
@RequestMapping("/department/group")
public class GroupTeacherController {

    private static final Logger log = LoggerFactory.getLogger(GroupTeacherController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private DefenseGroupMapper defenseGroupMapper;

    @Autowired
    private DefenseGroupTeacherMapper defenseGroupTeacherMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private ConfigService configService;

    @Autowired
    private StudentMapper studentMapper;

    private static final String GROUP_MAX_STUDENTS_KEY = "GROUP_MAX_STUDENTS";

    private User requireDeptAdmin(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "MANAGE_TEACHERS")) {
            return null;
        }
        return currentUser;
    }

    @GetMapping("/teachers")
    public List<Teacher> listDepartmentTeachers(HttpSession session) {
        // 允许所有已登录用户获取教师列表
        User currentUser = (User) session.getAttribute("currentUser");
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");

        if (currentUser == null && currentTeacher == null) {
            return java.util.Collections.emptyList();
        }

        // 超级管理员可以查看所有教师
        if (currentUser != null && currentUser.getRole() != null
                && "SUPER_ADMIN".equals(currentUser.getRole().getName())) {
            return teacherService.findByDepartmentId(null);
        }

        // 院系管理员查看本院系教师
        if (currentUser != null && currentUser.getRole() != null
                && "DEPT_ADMIN".equals(currentUser.getRole().getName())) {
            return teacherService.findByDepartmentId(currentUser.getDepartmentId());
        }

        // 教师查看本院系教师
        if (currentTeacher != null) {
            return teacherService.findByDepartmentId(currentTeacher.getDepartmentId());
        }

        return java.util.Collections.emptyList();
    }

    @GetMapping("/{groupId}/teachers")
    public List<DefenseGroupTeacher> listGroupTeachers(@PathVariable Long groupId, HttpSession session) {
        User u = requireDeptAdmin(session);
        if (u == null)
            return java.util.Collections.emptyList();
        DefenseGroup g = defenseGroupMapper.findById(groupId);
        if (g == null)
            return java.util.Collections.emptyList();
        return defenseGroupTeacherMapper.findByGroupId(groupId);
    }

    @PostMapping("/{groupId}/teacher/assign")
    public ApiResponse<Map<String, Object>> assignTeacher(@PathVariable Long groupId, @RequestParam Long teacherId, HttpSession session) {
        User u = requireDeptAdmin(session);
        if (u == null)
            return ApiResponse.error("权限不足");
        DefenseGroup g = defenseGroupMapper.findById(groupId);
        if (g == null)
            return ApiResponse.error("小组不存在");
        Teacher t = teacherService.findById(teacherId);
        if (t == null)
            return ApiResponse.error("教师不存在");
        if (u.getRole() != null && "DEPT_ADMIN".equals(u.getRole().getName())
                && u.getDepartmentId() != null && !u.getDepartmentId().equals(t.getDepartmentId())) {
            return ApiResponse.error("只能分配本院系教师");
        }

        // 检查该教师是否已经属于其他小组
        DefenseGroupTeacher existing = defenseGroupTeacherMapper.findByTeacherId(teacherId);
        if (existing != null && !existing.getGroupId().equals(groupId)) {
            // 教师已经属于其他小组，返回错误信息
            DefenseGroup existingGroup = defenseGroupMapper.findById(existing.getGroupId());
            String groupName = existingGroup != null ? existingGroup.getName() : "其他小组";
            return ApiResponse.error("该教师已经属于" + groupName + "，一个教师不能加入两个小组");
        }

        // 如果教师已经在当前小组，直接返回成功（避免重复插入）
        if (existing != null && existing.getGroupId().equals(groupId)) {
            return ApiResponse.success("教师已在当前小组", Map.of("groupId", groupId, "teacherId", teacherId));
        }

        defenseGroupTeacherMapper.insert(groupId, teacherId, 0);
        return ApiResponse.success("分配成功", Map.of("groupId", groupId, "teacherId", teacherId));
    }

    @DeleteMapping("/{groupId}/teacher/{teacherId}")
    public ApiResponse<Map<String, Object>> removeTeacher(@PathVariable Long groupId, @PathVariable Long teacherId, HttpSession session) {
        User u = requireDeptAdmin(session);
        if (u == null)
            return ApiResponse.error("权限不足");
        defenseGroupTeacherMapper.delete(groupId, teacherId);
        return ApiResponse.success("移除成功", Map.of("groupId", groupId, "teacherId", teacherId));
    }

    @PostMapping("/{groupId}/leader/set")
    public ApiResponse<Map<String, Object>> setLeader(@PathVariable Long groupId, @RequestParam Long teacherId, HttpSession session) {
        User u = requireDeptAdmin(session);
        if (u == null)
            return ApiResponse.error("权限不足");
        DefenseGroup g = defenseGroupMapper.findById(groupId);
        if (g == null)
            return ApiResponse.error("小组不存在");
        Teacher t = teacherService.findById(teacherId);
        if (t == null)
            return ApiResponse.error("教师不存在");
        if (u.getRole() != null && "DEPT_ADMIN".equals(u.getRole().getName())
                && u.getDepartmentId() != null && !u.getDepartmentId().equals(t.getDepartmentId())) {
            return ApiResponse.error("只能设置本院系教师为组长");
        }
        // ensure teacher is assigned to this group, then mark leader
        defenseGroupTeacherMapper.insert(groupId, teacherId, 0);
        defenseGroupTeacherMapper.clearLeader(groupId);
        defenseGroupTeacherMapper.setLeader(groupId, teacherId);
        return ApiResponse.success("组长设置成功", Map.of("groupId", groupId, "teacherId", teacherId));
    }

    /**
     * 获取未分配到任何小组的教师列表
     * GET /department/group/unassigned-teachers
     */
    @GetMapping("/unassigned-teachers")
    public List<Map<String, Object>> getUnassignedTeachers(HttpSession session) {
        User u = requireDeptAdmin(session);
        if (u == null) {
            return new ArrayList<>();
        }

        // 获取所有教师（根据权限：超级管理员看全部，院系管理员看本院系）
        List<Teacher> allTeachers;
        if (u.getRole() != null && "SUPER_ADMIN".equals(u.getRole().getName())) {
            allTeachers = teacherService.findByDepartmentId(null);
        } else {
            allTeachers = teacherService.findByDepartmentId(u.getDepartmentId());
        }

        log.debug("Loading unassigned teachers, role={}, departmentId={}, teacherCount={}",
                u.getRole() != null ? u.getRole().getName() : null,
                u.getDepartmentId(),
                allTeachers != null ? allTeachers.size() : 0);
        if (allTeachers != null) {
            for (Teacher t : allTeachers) {
                log.trace("Teacher candidate id={}, name={}, departmentId={}, status={}",
                        t.getId(), t.getName(), t.getDepartmentId(), t.getStatus());
            }
        }

        // 如果查询结果为空，直接返回
        if (allTeachers == null || allTeachers.isEmpty()) {
            log.debug("No teachers available for unassigned lookup");
            return new ArrayList<>();
        }

        // 获取所有已分配教师的ID列表（从 defense_group_teacher 表）
        List<DefenseGroupTeacher> allAssigned = defenseGroupTeacherMapper.findAll();
        log.debug("Assigned teacher relation count={}", allAssigned != null ? allAssigned.size() : 0);

        final List<Long> assignedTeacherIds = new ArrayList<>();
        if (allAssigned != null && !allAssigned.isEmpty()) {
            for (DefenseGroupTeacher dgt : allAssigned) {
                if (dgt != null && dgt.getTeacherId() != null && !assignedTeacherIds.contains(dgt.getTeacherId())) {
                    assignedTeacherIds.add(dgt.getTeacherId());
                }
            }
        }
        log.trace("Assigned teacher IDs={}", assignedTeacherIds);

        // 过滤出未分配的教师（不在任何小组的教师）
        // 注意：不过滤状态，显示所有未分配的教师（包括禁用的），让管理员决定
        final List<Teacher> unassignedTeachers = new ArrayList<>();
        for (Teacher t : allTeachers) {
            if (t != null && t.getId() != null && !assignedTeacherIds.contains(t.getId())) {
                unassignedTeachers.add(t);
                log.trace("Unassigned teacher id={}, name={}, departmentId={}, status={}",
                        t.getId(), t.getName(), t.getDepartmentId(), t.getStatus());
            } else if (t != null && t.getId() != null) {
                log.trace("Skipping assigned teacher id={}, name={}, inAssignedList={}",
                        t.getId(), t.getName(), assignedTeacherIds.contains(t.getId()));
            }
        }
        log.debug("Unassigned teacher count={}", unassignedTeachers.size());

        // 转换为Map格式返回（包含院系信息）
        List<Map<String, Object>> result = new ArrayList<>();
        for (Teacher t : unassignedTeachers) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", t.getId());
            info.put("teacherNo", t.getTeacherNo());
            info.put("name", t.getName());
            info.put("title", t.getTitle());
            // 获取院系名称
            if (t.getDepartmentId() != null) {
                Department dept = departmentMapper.findById(t.getDepartmentId());
                if (dept != null) {
                    info.put("departmentName", dept.getName());
                } else {
                    info.put("departmentName", null);
                }
            } else {
                info.put("departmentName", null);
            }
            result.add(info);
        }

        return result;
    }

    /**
     * 批量分配教师到小组
     * POST /department/group/assign-teachers
     */
    @PostMapping("/assign-teachers")
    public ApiResponse<Map<String, Object>> assignTeachers(@RequestBody Map<String, Object> request, HttpSession session) {
        User u = requireDeptAdmin(session);
        if (u == null) {
            return ApiResponse.error("权限不足");
        }

        try {
            Long groupId = Long.valueOf(request.get("groupId").toString());
            @SuppressWarnings("unchecked")
            List<Integer> teacherIds = (List<Integer>) request.get("teacherIds");

            if (groupId == null || teacherIds == null || teacherIds.isEmpty()) {
                return ApiResponse.error("参数错误");
            }

            DefenseGroup g = defenseGroupMapper.findById(groupId);
            if (g == null) {
                return ApiResponse.error("小组不存在");
            }

            // 批量分配教师到小组
            int successCount = 0;
            int skipCount = 0;
            for (Integer teacherId : teacherIds) {
                Teacher t = teacherService.findById(teacherId.longValue());
                if (t == null) {
                    continue;
                }

                // 检查权限：院系管理员只能分配本院系教师
                if (u.getRole() != null && "DEPT_ADMIN".equals(u.getRole().getName())
                        && u.getDepartmentId() != null && !u.getDepartmentId().equals(t.getDepartmentId())) {
                    skipCount++;
                    continue;
                }

                // 检查该教师是否已经属于其他小组
                DefenseGroupTeacher existing = defenseGroupTeacherMapper.findByTeacherId(teacherId.longValue());
                if (existing != null && !existing.getGroupId().equals(groupId)) {
                    // 教师已经属于其他小组，跳过
                    skipCount++;
                    continue;
                }

                // 如果教师已经在当前小组，跳过（避免重复插入）
                if (existing != null && existing.getGroupId().equals(groupId)) {
                    skipCount++;
                    continue;
                }

                // 分配教师到小组
                defenseGroupTeacherMapper.insert(groupId, teacherId.longValue(), 0);
                successCount++;
            }

            if (successCount == teacherIds.size()) {
                return ApiResponse.success("分配成功", Map.of(
                        "groupId", groupId,
                        "successCount", successCount,
                        "skipCount", skipCount));
            } else if (successCount > 0) {
                return ApiResponse.success("部分教师分配成功，成功: " + successCount + "/" + teacherIds.size() +
                        (skipCount > 0 ? "，跳过: " + skipCount : ""), Map.of(
                        "groupId", groupId,
                        "successCount", successCount,
                        "skipCount", skipCount));
            } else {
                return ApiResponse.error("所有教师分配失败，可能已属于其他小组或权限不足");
            }
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 批量从小组移除教师
     * POST /department/group/remove-teachers
     */
    @PostMapping("/remove-teachers")
    @ResponseBody
    public ApiResponse<Map<String, Object>> removeTeachersFromGroup(@RequestBody Map<String, Object> request, HttpSession session) {
        User u = requireDeptAdmin(session);
        if (u == null) {
            return ApiResponse.error("权限不足");
        }

        try {
            Long groupId = Long.valueOf(request.get("groupId").toString());
            @SuppressWarnings("unchecked")
            List<Integer> teacherIds = (List<Integer>) request.get("teacherIds");

            if (groupId == null || teacherIds == null || teacherIds.isEmpty()) {
                return ApiResponse.error("参数错误");
            }

            DefenseGroup g = defenseGroupMapper.findById(groupId);
            if (g == null) {
                return ApiResponse.error("小组不存在");
            }

            // 批量从小组移除教师
            int successCount = 0;
            for (Integer teacherId : teacherIds) {
                try {
                    // 从 defense_group_teacher 表中删除记录
                    defenseGroupTeacherMapper.delete(groupId, teacherId.longValue());
                    successCount++;
                } catch (Exception e) {
                    log.warn("Failed to remove teacher from group, groupId={}, teacherId={}", groupId, teacherId, e);
                }
            }

            if (successCount == 0) {
                return ApiResponse.error("没有成功移除任何教师");
            }

            return ApiResponse.success("已成功从小组移除 " + successCount + " 个教师", Map.of(
                    "groupId", groupId,
                    "successCount", successCount));
        } catch (Exception e) {
            log.error("Failed to remove teachers from group", e);
            return ApiResponse.error("移除教师失败: " + e.getMessage());
        }
    }

    /**
     * 获取每组最大学生人数配置
     * GET /department/group/config/max-students
     */
    @GetMapping("/config/max-students")
    public Map<String, Object> getGroupMaxStudents(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        String val = configService.getConfigValue(GROUP_MAX_STUDENTS_KEY);
        int maxStudents = 10;
        if (val != null) {
            try {
                maxStudents = Integer.parseInt(val.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        result.put("maxStudents", maxStudents);
        return result;
    }

    /**
     * 保存每组最大学生人数配置
     * POST /department/group/config/max-students
     */
    @PostMapping("/config/max-students")
    public Map<String, Object> saveGroupMaxStudents(@RequestBody Map<String, Object> body, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User u = requireDeptAdmin(session);
        if (u == null) {
            result.put("error", "权限不足");
            return result;
        }
        int max = 10;
        try {
            max = Integer.parseInt(body.get("maxStudents").toString());
        } catch (Exception ignored) {
        }
        configService.saveConfig(GROUP_MAX_STUDENTS_KEY, String.valueOf(max), "每答辩小组最大学生人数");
        result.put("success", true);
        result.put("maxStudents", max);
        return result;
    }

    /**
     * 随机将未分组学生分配到人数未满的答辩组
     * POST /department/group/random-assign/students
     */
    @PostMapping("/random-assign/students")
    public Map<String, Object> randomAssignStudents(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User u = requireDeptAdmin(session);
        if (u == null) {
            result.put("error", "权限不足");
            return result;
        }

        Long deptId = u.getDepartmentId();
        Integer year = configService.getCurrentDefenseYear();

        int maxStudents = 10;
        String val = configService.getConfigValue(GROUP_MAX_STUDENTS_KEY);
        if (val != null) {
            try {
                maxStudents = Integer.parseInt(val.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        // 本院所有学生
        List<Student> allStudents = studentMapper.findByDepartmentAndYear(deptId, year);
        List<Student> unassigned = new ArrayList<>();
        for (Student s : allStudents) {
            if (s.getDefenseGroupId() == null)
                unassigned.add(s);
        }
        if (unassigned.isEmpty()) {
            result.put("assigned", 0);
            result.put("message", "没有未分组学生");
            return result;
        }

        // 本院所有小组
        List<DefenseGroup> groups = defenseGroupMapper.findByDepartmentId(deptId);
        if (groups == null || groups.isEmpty()) {
            result.put("error", "没有可用小组");
            return result;
        }

        // 计算每组当前学生数
        Map<Long, Integer> groupCount = new HashMap<>();
        for (DefenseGroup g : groups) {
            List<Student> members = studentMapper.findByDefenseGroupId(g.getId());
            groupCount.put(g.getId(), members == null ? 0 : members.size());
        }

        Collections.shuffle(unassigned, new Random());
        int assigned = 0;
        for (Student s : unassigned) {
            for (DefenseGroup g : groups) {
                int cnt = groupCount.getOrDefault(g.getId(), 0);
                if (cnt < maxStudents) {
                    studentMapper.updateDefenseGroupId(s.getId(), g.getId());
                    groupCount.put(g.getId(), cnt + 1);
                    assigned++;
                    break;
                }
            }
        }
        result.put("assigned", assigned);
        result.put("total", unassigned.size());
        result.put("success", true);
        return result;
    }

    /**
     * 随机将本院未分组教师分配到各答辩组
     * POST /department/group/random-assign/teachers
     */
    @PostMapping("/random-assign/teachers")
    public Map<String, Object> randomAssignTeachers(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User u = requireDeptAdmin(session);
        if (u == null) {
            result.put("error", "权限不足");
            return result;
        }

        Long deptId = u.getDepartmentId();
        List<Teacher> allTeachers = teacherService.findByDepartmentId(deptId);
        if (allTeachers == null)
            allTeachers = new ArrayList<>();

        List<DefenseGroupTeacher> allAssigned = defenseGroupTeacherMapper.findAll();
        List<Long> assignedIds = new ArrayList<>();
        if (allAssigned != null) {
            for (DefenseGroupTeacher dgt : allAssigned) {
                if (dgt.getTeacherId() != null)
                    assignedIds.add(dgt.getTeacherId());
            }
        }

        List<Teacher> unassigned = new ArrayList<>();
        for (Teacher t : allTeachers) {
            if (!assignedIds.contains(t.getId()))
                unassigned.add(t);
        }
        if (unassigned.isEmpty()) {
            result.put("assigned", 0);
            result.put("message", "没有未分配教师");
            return result;
        }

        List<DefenseGroup> groups = defenseGroupMapper.findByDepartmentId(deptId);
        if (groups == null || groups.isEmpty()) {
            result.put("error", "没有可用小组");
            return result;
        }

        Collections.shuffle(unassigned, new Random());
        int assigned = 0;
        int gi = 0;
        for (Teacher t : unassigned) {
            DefenseGroup g = groups.get(gi % groups.size());
            defenseGroupTeacherMapper.insert(g.getId(), t.getId(), 0);
            assigned++;
            gi++;
        }
        result.put("assigned", assigned);
        result.put("success", true);
        return result;
    }

    /**
     * 随机设定本院各小组组长（从已分配教师中随机选一人）
     * POST /department/group/random-assign/leader
     */
    @PostMapping("/random-assign/leader")
    public Map<String, Object> randomAssignLeader(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User u = requireDeptAdmin(session);
        if (u == null) {
            result.put("error", "权限不足");
            return result;
        }

        Long deptId = u.getDepartmentId();
        List<DefenseGroup> groups = defenseGroupMapper.findByDepartmentId(deptId);
        if (groups == null || groups.isEmpty()) {
            result.put("error", "没有小组");
            return result;
        }

        Random rnd = new Random();
        int successCount = 0;
        for (DefenseGroup g : groups) {
            List<DefenseGroupTeacher> groupTeachers = defenseGroupTeacherMapper.findByGroupId(g.getId());
            if (groupTeachers == null || groupTeachers.isEmpty())
                continue;
            DefenseGroupTeacher chosen = groupTeachers.get(rnd.nextInt(groupTeachers.size()));
            defenseGroupTeacherMapper.clearLeader(g.getId());
            defenseGroupTeacherMapper.setLeader(g.getId(), chosen.getTeacherId());
            successCount++;
        }
        result.put("success", true);
        result.put("groupsUpdated", successCount);
        return result;
    }
}
