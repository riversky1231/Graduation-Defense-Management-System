package com.example.defensemanagement.controller.group;

import com.example.defensemanagement.common.ApiResponse;
import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.Department;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 答辩小组教师分配 Controller。
 */
@RestController
@RequestMapping("/department/group")
public class GroupTeacherAssignmentController extends AbstractGroupAssignmentController {

    private static final Logger log = LoggerFactory.getLogger(GroupTeacherAssignmentController.class);

    public GroupTeacherAssignmentController(
            AuthService authService,
            TeacherService teacherService,
            DefenseGroupMapper defenseGroupMapper,
            DefenseGroupTeacherMapper defenseGroupTeacherMapper,
            DepartmentMapper departmentMapper,
            ConfigService configService,
            StudentMapper studentMapper) {
        super(authService, teacherService, defenseGroupMapper, defenseGroupTeacherMapper, departmentMapper, configService, studentMapper);
    }

    @GetMapping("/unassigned-teachers")
    public List<Map<String, Object>> getUnassignedTeachers(HttpSession session) {
        User currentUser = requireDeptAdmin(session);
        if (currentUser == null) {
            return new ArrayList<>();
        }

        List<Teacher> allTeachers = isSuperAdmin(currentUser)
                ? teacherService.findByDepartmentId(null)
                : teacherService.findByDepartmentId(currentUser.getDepartmentId());

        List<Long> assignedTeacherIds = loadAssignedTeacherIds();
        List<Map<String, Object>> result = new ArrayList<>();
        if (allTeachers == null) {
            return result;
        }

        for (Teacher teacher : allTeachers) {
            if (teacher == null || teacher.getId() == null || assignedTeacherIds.contains(teacher.getId())) {
                continue;
            }
            Map<String, Object> info = new HashMap<>();
            info.put("id", teacher.getId());
            info.put("teacherNo", teacher.getTeacherNo());
            info.put("name", teacher.getName());
            info.put("title", teacher.getTitle());
            info.put("departmentName", resolveDepartmentName(teacher.getDepartmentId()));
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
            Long groupId = Long.valueOf(String.valueOf(request.get("groupId")));
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
                    continue;
                }

                defenseGroupTeacherMapper.insert(groupId, teacherId.longValue(), 0);
                successCount++;
            }

            if (successCount == teacherIds.size()) {
                return ApiResponse.success("分配成功", buildAssignmentResult(groupId, successCount, skipCount));
            }
            if (successCount > 0) {
                return ApiResponse.success(
                        "部分教师分配成功，成功: " + successCount + "/" + teacherIds.size()
                                + (skipCount > 0 ? "，跳过: " + skipCount : ""),
                        buildAssignmentResult(groupId, successCount, skipCount));
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
            Long groupId = Long.valueOf(String.valueOf(request.get("groupId")));
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
            return ApiResponse.success("已成功从小组移除 " + successCount + " 个教师",
                    Map.of("groupId", groupId, "successCount", successCount));
        } catch (Exception e) {
            log.error("Failed to remove teachers from group", e);
            return ApiResponse.error("移除教师失败: " + e.getMessage());
        }
    }

    private List<Long> loadAssignedTeacherIds() {
        List<Long> assignedTeacherIds = new ArrayList<>();
        List<DefenseGroupTeacher> allAssigned = defenseGroupTeacherMapper.findAll();
        if (allAssigned == null) {
            return assignedTeacherIds;
        }
        for (DefenseGroupTeacher relation : allAssigned) {
            if (relation != null
                    && relation.getTeacherId() != null
                    && !assignedTeacherIds.contains(relation.getTeacherId())) {
                assignedTeacherIds.add(relation.getTeacherId());
            }
        }
        return assignedTeacherIds;
    }

    private String resolveDepartmentName(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        Department department = departmentMapper.findById(departmentId);
        return department != null ? department.getName() : null;
    }

    private Map<String, Object> buildAssignmentResult(Long groupId, int successCount, int skipCount) {
        Map<String, Object> result = new HashMap<>();
        result.put("groupId", groupId);
        result.put("successCount", successCount);
        result.put("skipCount", skipCount);
        return result;
    }
}
