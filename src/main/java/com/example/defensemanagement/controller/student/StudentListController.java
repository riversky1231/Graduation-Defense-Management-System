package com.example.defensemanagement.controller.student;

import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生列表查询 Controller。
 * 处理列表、搜索、当前年份、答辩小组列表等只读操作。
 */
@RestController
@RequestMapping("/department/student")
public class StudentListController extends AbstractStudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private DefenseGroupMapper defenseGroupMapper;

    /**
     * 获取当前用户可见的学生列表。
     * 教师看到自己指导的学生，管理员看到本院系/全部学生。
     */
    @GetMapping("/list")
    @ResponseBody
    public List<Student> getStudentsByDept(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");

        if (currentTeacher != null) {
            Integer currentYear = configService.getCurrentDefenseYear();
            if (currentYear == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先设置当前答辩年份");
            }
            return studentService.getStudentsByAdvisor(currentTeacher.getId(), currentYear);
        }

        if (currentUser != null) {
            String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
            if ("SUPER_ADMIN".equals(roleName)) {
                Integer currentYear = configService.getCurrentDefenseYear();
                return currentYear == null ? studentService.findAll() : studentService.findByYear(currentYear);
            }

            if ("DEPT_ADMIN".equals(roleName)) {
                Long departmentId = currentUser.getDepartmentId();
                Integer currentYear = configService.getCurrentDefenseYear();
                if (departmentId == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "院系信息未配置，请联系管理员");
                }
                if (currentYear == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先设置当前答辩年份");
                }
                List<Student> students = studentService.findByDepartmentAndYear(departmentId, currentYear);
                return students != null ? students : new ArrayList<>();
            }

            if ("TEACHER".equals(roleName)) {
                Integer currentYear = configService.getCurrentDefenseYear();
                if (currentYear == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先设置当前答辩年份");
                }
                Long teacherId = findTeacherIdByUserId(currentUser.getId());
                if (teacherId == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未找到关联的教师信息");
                }
                return studentService.getStudentsByAdvisor(teacherId, currentYear);
            }
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "权限不足或未登录");
    }

    /**
     * 分页搜索学生。
     */
    @GetMapping("/search")
    @ResponseBody
    public Map<String, Object> searchStudents(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("currentUser");
        Long departmentId = null;
        Integer year = configService.getCurrentDefenseYear();

        if (currentUser != null) {
            String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
            if (!"SUPER_ADMIN".equals(roleName) && "DEPT_ADMIN".equals(roleName)) {
                departmentId = currentUser.getDepartmentId();
            }
        }

        List<Student> students = studentService.searchStudents(keyword, departmentId, year, page, pageSize);
        int total = studentService.countStudents(keyword, departmentId, year);
        int totalPages = (int) Math.ceil((double) total / pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("students", students);
        result.put("total", total);
        result.put("currentPage", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", totalPages);
        return result;
    }

    /**
     * 获取当前答辩年份。
     */
    @GetMapping("/currentYear")
    @ResponseBody
    public Integer getCurrentYear() {
        return configService.getCurrentDefenseYear();
    }

    /**
     * 获取当前用户可见的答辩小组列表。
     */
    @GetMapping("/groups")
    @ResponseBody
    public List<DefenseGroup> getDefenseGroups(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");

        if (currentUser != null) {
            String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
            if ("SUPER_ADMIN".equals(roleName)) {
                return defenseGroupMapper.findAllByOrderByDisplayOrderAsc();
            }
            if ("DEPT_ADMIN".equals(roleName)) {
                Long departmentId = currentUser.getDepartmentId();
                if (departmentId == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "院系信息未配置");
                }
                return defenseGroupMapper.findByDepartmentId(departmentId);
            }
        }

        if (currentTeacher != null) {
            Long departmentId = currentTeacher.getDepartmentId();
            if (departmentId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "教师院系信息未配置");
            }
            return defenseGroupMapper.findByDepartmentId(departmentId);
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "权限不足");
    }
}
