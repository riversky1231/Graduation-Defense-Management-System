package com.example.defensemanagement.controller.group;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.TeacherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 答辩小组容量配置 Controller。
 */
@RestController
@RequestMapping("/department/group")
public class GroupAssignmentConfigController extends AbstractGroupAssignmentController {

    public GroupAssignmentConfigController(
            AuthService authService,
            TeacherService teacherService,
            DefenseGroupMapper defenseGroupMapper,
            DefenseGroupTeacherMapper defenseGroupTeacherMapper,
            DepartmentMapper departmentMapper,
            ConfigService configService,
            StudentMapper studentMapper) {
        super(authService, teacherService, defenseGroupMapper, defenseGroupTeacherMapper, departmentMapper, configService, studentMapper);
    }

    @GetMapping("/config/max-students")
    public Map<String, Object> getGroupMaxStudents(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        result.put("maxStudents", getGroupMaxStudents());
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

        int maxStudents = getGroupMaxStudents();
        try {
            maxStudents = Integer.parseInt(String.valueOf(body.get("maxStudents")));
        } catch (Exception ignored) {
            // keep existing/default value
        }
        configService.saveConfig(GROUP_MAX_STUDENTS_KEY, String.valueOf(maxStudents), "每答辩小组最大学生人数");
        result.put("success", true);
        result.put("maxStudents", maxStudents);
        return result;
    }
}
