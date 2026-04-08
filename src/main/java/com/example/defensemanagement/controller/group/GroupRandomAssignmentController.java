package com.example.defensemanagement.controller.group;

import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.DefenseGroupTeacher;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 答辩小组随机分配 Controller。
 */
@RestController
@RequestMapping("/department/group")
public class GroupRandomAssignmentController extends AbstractGroupAssignmentController {

    public GroupRandomAssignmentController(
            AuthService authService,
            TeacherService teacherService,
            DefenseGroupMapper defenseGroupMapper,
            DefenseGroupTeacherMapper defenseGroupTeacherMapper,
            DepartmentMapper departmentMapper,
            ConfigService configService,
            StudentMapper studentMapper) {
        super(authService, teacherService, defenseGroupMapper, defenseGroupTeacherMapper, departmentMapper, configService, studentMapper);
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
        int maxStudents = getGroupMaxStudents();

        List<Student> allStudents = studentMapper.findByDepartmentAndYear(deptId, year);
        List<Student> unassignedStudents = new ArrayList<>();
        if (allStudents != null) {
            for (Student student : allStudents) {
                if (student.getDefenseGroupId() == null) {
                    unassignedStudents.add(student);
                }
            }
        }
        if (unassignedStudents.isEmpty()) {
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

        Collections.shuffle(unassignedStudents, new Random());
        int assigned = 0;
        for (Student student : unassignedStudents) {
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
        result.put("total", unassignedStudents.size());
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

        List<Long> assignedTeacherIds = new ArrayList<>();
        List<DefenseGroupTeacher> allAssigned = defenseGroupTeacherMapper.findAll();
        if (allAssigned != null) {
            for (DefenseGroupTeacher relation : allAssigned) {
                if (relation.getTeacherId() != null) {
                    assignedTeacherIds.add(relation.getTeacherId());
                }
            }
        }

        List<Teacher> unassignedTeachers = new ArrayList<>();
        for (Teacher teacher : allTeachers) {
            if (!assignedTeacherIds.contains(teacher.getId())) {
                unassignedTeachers.add(teacher);
            }
        }
        if (unassignedTeachers.isEmpty()) {
            result.put("assigned", 0);
            result.put("message", "没有未分配教师");
            return result;
        }

        List<DefenseGroup> groups = defenseGroupMapper.findByDepartmentId(deptId);
        if (groups == null || groups.isEmpty()) {
            result.put("error", "没有可用小组");
            return result;
        }

        Collections.shuffle(unassignedTeachers, new Random());
        int assigned = 0;
        int groupIndex = 0;
        for (Teacher teacher : unassignedTeachers) {
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
}
