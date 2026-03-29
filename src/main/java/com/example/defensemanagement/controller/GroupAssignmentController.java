package com.example.defensemanagement.controller;

import com.example.defensemanagement.common.ApiResponse;
import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.TeacherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 院系管理员：教师批量分配与随机分组配置
 */
@RestController
@RequestMapping("/department/group")
public class GroupAssignmentController {

    private static final Logger log = LoggerFactory.getLogger(GroupAssignmentController.class);
    private static final String GROUP_MAX_STUDENTS_KEY = "GROUP_MAX_STUDENTS";

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

    @GetMapping("/unassigned-teachers")
    public List<Map<String, Object>> getUnassignedTeachers(HttpSession session) {
        User currentUser = requireDeptAdmin(session);
        if (currentUser == null) {
            return new ArrayList<>();
        }

        List<Teacher> allTeachers;
        if (isSuperAdmin(currentUser)) {
            allTeachers = teacherService.findByDepartmentId(null);
        } else {
            allTeachers = teacherService.findByDepartmentId(currentUser.getDepartmentId());
        }

        log.debug("Loading unassigned teachers, role={}, departmentId={}, teacherCount={}",
                currentUser.getRole() != null ? currentUser.getRole().getName() : null,
                currentUser.getDepartmentId(),
                allTeachers != null ? allTeachers.size() : 0);

        if (allTeachers == null || allTeachers.isEmpty()) {
            return new ArrayList<>();
        }

        List<DefenseGroupTeacher> allAssigned = defenseGroupTeacherMapper.findAll();
        List<Long> assignedTeacherIds = new ArrayList<>();
        if (allAssigned != null) {
            for (DefenseGroupTeacher relation : allAssigned) {
                if (relation != null
                        && relation.getTeacherId() != null
                        && !assignedTeacherIds.contains(relation.getTeacherId())) {
                    assignedTeacherIds.add(relation.getTeacherId());
                }
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Teacher teacher : allTeachers) {
            if (teacher == null || teacher.getId() == null || assignedTeacherIds.contains(teacher.getId())) {
                continue;
            }

            Map<String, Object> info = new HashMap<>();
            info.put("id", teacher.getId());
            info.put("teacherNo", teacher.getTeacherNo());
            info.put("name", teacher.getName());
            info.put("title", teacher.getTitle());
            if (teacher.getDepartmentId() != null) {
                Department department = departmentMapper.findById(teacher.getDepartmentId());
                info.put("departmentName", department != null ? department.getName() : null);
            } else {
                info.put("departmentName", null);
            }
            result.add(info);
        }

        return result;
    }

    @PostMapping("/assign-teachers")
    public ApiResponse<Map<String, Object>> assignTeachers(@RequestBody Map<String, Object> request, HttpSession session) {
        User currentUser = requireDeptAdmin(session);
        if (currentUser == null) {
            return ApiResponse.error("权限不足");
        }

        try {
            Long groupId = Long.valueOf(request.get("groupId").toString());
            @SuppressWarnings("unchecked")
            List<Integer> teacherIds = (List<Integer>) request.get("teacherIds");

            if (groupId == null || teacherIds == null || teacherIds.isEmpty()) {
                return ApiResponse.error("参数错误");
            }

            DefenseGroup group = defenseGroupMapper.findById(groupId);
            if (group == null) {
                return ApiResponse.error("小组不存在");
            }

            int successCount = 0;
            int skipCount = 0;
            for (Integer teacherId : teacherIds) {
                Teacher teacher = teacherService.findById(teacherId.longValue());
                if (teacher == null) {
                    continue;
                }

                if (isDeptAdmin(currentUser)
                        && currentUser.getDepartmentId() != null
                        && !currentUser.getDepartmentId().equals(teacher.getDepartmentId())) {
                    skipCount++;
                    continue;
                }

                DefenseGroupTeacher existing = defenseGroupTeacherMapper.findByTeacherId(teacherId.longValue());
                if (existing != null) {
                    skipCount++;
                    if (existing.getGroupId().equals(groupId)) {
                        continue;
                    }
                    continue;
                }

                defenseGroupTeacherMapper.insert(groupId, teacherId.longValue(), 0);
                successCount++;
            }

            if (successCount == teacherIds.size()) {
                return ApiResponse.success("分配成功", Map.of(
                        "groupId", groupId,
                        "successCount", successCount,
                        "skipCount", skipCount));
            }
            if (successCount > 0) {
                return ApiResponse.success("部分教师分配成功，成功: " + successCount + "/" + teacherIds.size()
                                + (skipCount > 0 ? "，跳过: " + skipCount : ""),
                        Map.of(
                                "groupId", groupId,
                                "successCount", successCount,
                                "skipCount", skipCount));
            }
            return ApiResponse.error("所有教师分配失败，可能已属于其他小组或权限不足");
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/remove-teachers")
    @ResponseBody
    public ApiResponse<Map<String, Object>> removeTeachersFromGroup(@RequestBody Map<String, Object> request,
                                                                    HttpSession session) {
        User currentUser = requireDeptAdmin(session);
        if (currentUser == null) {
            return ApiResponse.error("权限不足");
        }

        try {
            Long groupId = Long.valueOf(request.get("groupId").toString());
            @SuppressWarnings("unchecked")
            List<Integer> teacherIds = (List<Integer>) request.get("teacherIds");

            if (groupId == null || teacherIds == null || teacherIds.isEmpty()) {
                return ApiResponse.error("参数错误");
            }

            DefenseGroup group = defenseGroupMapper.findById(groupId);
            if (group == null) {
                return ApiResponse.error("小组不存在");
            }

            int successCount = 0;
            for (Integer teacherId : teacherIds) {
                try {
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

    @PostMapping("/config/max-students")
    public Map<String, Object> saveGroupMaxStudents(@RequestBody Map<String, Object> body, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User currentUser = requireDeptAdmin(session);
        if (currentUser == null) {
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

    @PostMapping("/random-assign/students")
    public Map<String, Object> randomAssignStudents(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User currentUser = requireDeptAdmin(session);
        if (currentUser == null) {
            result.put("error", "权限不足");
            return result;
        }

        Long deptId = currentUser.getDepartmentId();
        Integer year = configService.getCurrentDefenseYear();

        int maxStudents = 10;
        String val = configService.getConfigValue(GROUP_MAX_STUDENTS_KEY);
        if (val != null) {
            try {
                maxStudents = Integer.parseInt(val.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        List<Student> allStudents = studentMapper.findByDepartmentAndYear(deptId, year);
        List<Student> unassigned = new ArrayList<>();
        for (Student student : allStudents) {
            if (student.getDefenseGroupId() == null) {
                unassigned.add(student);
            }
        }
        if (unassigned.isEmpty()) {
            result.put("assigned", 0);
            result.put("message", "没有未分组学生");
            return result;
        }

        List<DefenseGroup> groups = defenseGroupMapper.findByDepartmentId(deptId);
        if (groups == null || groups.isEmpty()) {
            result.put("error", "没有可用小组");
            return result;
        }

        Map<Long, Integer> groupCount = new HashMap<>();
        for (DefenseGroup group : groups) {
            List<Student> members = studentMapper.findByDefenseGroupId(group.getId());
            groupCount.put(group.getId(), members == null ? 0 : members.size());
        }

        Collections.shuffle(unassigned, new Random());
        int assigned = 0;
        for (Student student : unassigned) {
            for (DefenseGroup group : groups) {
                int count = groupCount.getOrDefault(group.getId(), 0);
                if (count < maxStudents) {
                    studentMapper.updateDefenseGroupId(student.getId(), group.getId());
                    groupCount.put(group.getId(), count + 1);
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

    @PostMapping("/random-assign/teachers")
    public Map<String, Object> randomAssignTeachers(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User currentUser = requireDeptAdmin(session);
        if (currentUser == null) {
            result.put("error", "权限不足");
            return result;
        }

        Long deptId = currentUser.getDepartmentId();
        List<Teacher> allTeachers = teacherService.findByDepartmentId(deptId);
        if (allTeachers == null) {
            allTeachers = new ArrayList<>();
        }

        List<DefenseGroupTeacher> allAssigned = defenseGroupTeacherMapper.findAll();
        List<Long> assignedIds = new ArrayList<>();
        if (allAssigned != null) {
            for (DefenseGroupTeacher relation : allAssigned) {
                if (relation.getTeacherId() != null) {
                    assignedIds.add(relation.getTeacherId());
                }
            }
        }

        List<Teacher> unassigned = new ArrayList<>();
        for (Teacher teacher : allTeachers) {
            if (!assignedIds.contains(teacher.getId())) {
                unassigned.add(teacher);
            }
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
        int groupIndex = 0;
        for (Teacher teacher : unassigned) {
            DefenseGroup group = groups.get(groupIndex % groups.size());
            defenseGroupTeacherMapper.insert(group.getId(), teacher.getId(), 0);
            assigned++;
            groupIndex++;
        }

        result.put("assigned", assigned);
        result.put("success", true);
        return result;
    }

    @PostMapping("/random-assign/leader")
    public Map<String, Object> randomAssignLeader(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User currentUser = requireDeptAdmin(session);
        if (currentUser == null) {
            result.put("error", "权限不足");
            return result;
        }

        Long deptId = currentUser.getDepartmentId();
        List<DefenseGroup> groups = defenseGroupMapper.findByDepartmentId(deptId);
        if (groups == null || groups.isEmpty()) {
            result.put("error", "没有小组");
            return result;
        }

        Random random = new Random();
        int successCount = 0;
        for (DefenseGroup group : groups) {
            List<DefenseGroupTeacher> groupTeachers = defenseGroupTeacherMapper.findByGroupId(group.getId());
            if (groupTeachers == null || groupTeachers.isEmpty()) {
                continue;
            }
            DefenseGroupTeacher chosen = groupTeachers.get(random.nextInt(groupTeachers.size()));
            defenseGroupTeacherMapper.clearLeader(group.getId());
            defenseGroupTeacherMapper.setLeader(group.getId(), chosen.getTeacherId());
            successCount++;
        }

        result.put("success", true);
        result.put("groupsUpdated", successCount);
        return result;
    }

    private User requireDeptAdmin(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "MANAGE_TEACHERS")) {
            return null;
        }
        return currentUser;
    }

    private boolean isSuperAdmin(User user) {
        return user.getRole() != null && "SUPER_ADMIN".equals(user.getRole().getName());
    }

    private boolean isDeptAdmin(User user) {
        return user.getRole() != null && "DEPT_ADMIN".equals(user.getRole().getName());
    }
}
