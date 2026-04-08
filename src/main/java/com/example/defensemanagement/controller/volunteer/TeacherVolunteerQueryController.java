package com.example.defensemanagement.controller.volunteer;

import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.StudentPreferenceMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherProfileMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.StudentService;
import com.example.defensemanagement.service.VolunteerMatchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 教师志愿查询 Controller。
 * 处理配置、分轮列表和全量志愿列表接口。
 */
@RestController
@RequestMapping("/teacher/volunteer")
public class TeacherVolunteerQueryController extends AbstractTeacherVolunteerController {

    public TeacherVolunteerQueryController(
            TeacherMapper teacherMapper,
            StudentPreferenceMapper studentPreferenceMapper,
            StudentMapper studentMapper,
            StudentService studentService,
            ConfigService configService,
            TeacherProfileMapper teacherProfileMapper,
            VolunteerMatchService volunteerMatchService) {
        super(
                teacherMapper,
                studentPreferenceMapper,
                studentMapper,
                studentService,
                configService,
                teacherProfileMapper,
                volunteerMatchService);
    }

    @GetMapping("/config")
    public Map<String, Object> getVolunteerConfig(HttpSession session) {
        Teacher teacher = getCurrentTeacher(session);
        if (teacher == null) {
            return errorResult("未登录或非教师身份");
        }

        Integer year = getCurrentYear();
        Map<String, Object> result = new HashMap<>();
        result.put("year", year);
        result.put("maxStudents", getMaxStudents());
        result.put("deadline", getVolunteerDeadline());
        result.put("assignedCount", studentMapper.countByAdvisorAndYear(teacher.getId(), year));
        result.put("deadlinePassed", isDeadlinePassed());
        return result;
    }

    @GetMapping("/list")
    public Map<String, Object> getVolunteerList(@RequestParam Integer round, HttpSession session) {
        Teacher teacher = getCurrentTeacher(session);
        if (teacher == null) {
            return errorResult("未登录或非教师身份");
        }

        String roundError = validateRound(round);
        if (roundError != null) {
            return errorResult(roundError);
        }

        Integer year = getCurrentYear();
        int maxStudents = getMaxStudents();
        int assigned = studentMapper.countByAdvisorAndYear(teacher.getId(), year);
        boolean deadlinePassed = isDeadlinePassed();
        List<Map<String, Object>> rows = studentPreferenceMapper.findByTeacherAndYearAndRound(teacher.getId(), year, round);

        Map<String, Object> result = new HashMap<>();
        result.put("year", year);
        result.put("round", round);
        result.put("assignedCount", assigned);
        result.put("maxStudents", maxStudents);
        result.put("deadlinePassed", deadlinePassed);
        result.put("items", buildVolunteerItems(rows, teacher, year, round, assigned, maxStudents, deadlinePassed));
        return result;
    }

    @GetMapping("/all")
    public Map<String, Object> getAllVolunteers(HttpSession session) {
        Teacher teacher = getCurrentTeacher(session);
        if (teacher == null) {
            return errorResult("未登录或非教师身份");
        }

        Integer year = getCurrentYear();
        int maxStudents = getMaxStudents();
        int assigned = studentMapper.countByAdvisorAndYear(teacher.getId(), year);
        boolean deadlinePassed = isDeadlinePassed();
        List<Map<String, Object>> rows = studentPreferenceMapper.findAllByTeacherAndYear(teacher.getId(), year);

        Map<String, Object> result = new HashMap<>();
        result.put("year", year);
        result.put("assignedCount", assigned);
        result.put("maxStudents", maxStudents);
        result.put("deadlinePassed", deadlinePassed);
        result.put("items", buildVolunteerItems(rows, teacher, year, null, assigned, maxStudents, deadlinePassed));
        return result;
    }
}
