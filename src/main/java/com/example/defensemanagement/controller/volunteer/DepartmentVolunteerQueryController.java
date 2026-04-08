package com.example.defensemanagement.controller.volunteer;

import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.StudentPreferenceMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherProfileMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.StudentService;
import com.example.defensemanagement.service.VolunteerMatchService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 院系志愿查询端点。
 */
@Controller
@RequestMapping("/department/volunteer")
public class DepartmentVolunteerQueryController extends AbstractDepartmentVolunteerController {

    public DepartmentVolunteerQueryController(
            ConfigService configService,
            StudentPreferenceMapper studentPreferenceMapper,
            StudentMapper studentMapper,
            TeacherMapper teacherMapper,
            TeacherProfileMapper teacherProfileMapper,
            StudentService studentService,
            VolunteerMatchService volunteerMatchService) {
        super(
                configService,
                studentPreferenceMapper,
                studentMapper,
                teacherMapper,
                teacherProfileMapper,
                studentService,
                volunteerMatchService);
    }

    @GetMapping("/teachers")
    @ResponseBody
    public Object listDepartmentTeachers(HttpSession session) {
        User admin = getCurrentDeptAdmin(session);
        if (admin == null) {
            return Collections.singletonMap("error", "无权限");
        }

        Long departmentId = admin.getDepartmentId();
        if (departmentId == null) {
            return Collections.singletonMap("error", "未绑定院系");
        }

        List<Teacher> teachers = teacherMapper.findByDepartmentId(departmentId);
        List<Map<String, Object>> result = new ArrayList<>();
        if (teachers != null) {
            for (Teacher teacher : teachers) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", teacher.getId());
                item.put("teacherNo", teacher.getTeacherNo());
                item.put("name", teacher.getName());
                item.put("title", teacher.getTitle());
                result.add(item);
            }
        }
        return result;
    }

    @GetMapping("/students")
    @ResponseBody
    public Map<String, Object> listDepartmentStudents(
            @RequestParam(required = false) Boolean unassignedOnly,
            HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User admin = getCurrentDeptAdmin(session);
        if (admin == null) {
            result.put("error", "无权限");
            return result;
        }

        Long departmentId = admin.getDepartmentId();
        if (departmentId == null) {
            result.put("error", "未绑定院系");
            return result;
        }

        Integer year = getCurrentYear();
        result.put("students", studentPreferenceMapper.findByDepartmentAndYear(departmentId, year, unassignedOnly));
        result.put("year", year);
        result.put("maxStudents", getTeacherMaxStudents());
        return result;
    }
}
