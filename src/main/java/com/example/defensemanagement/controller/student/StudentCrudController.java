package com.example.defensemanagement.controller.student;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学生增删改 Controller。
 * 处理保存、删除、批量删除、未分配学生查询等操作。
 */
@RestController
@RequestMapping("/department/student")
public class StudentCrudController extends AbstractStudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private DefenseGroupMapper defenseGroupMapper;

    @PostMapping("/save")
    @ResponseBody
    public String saveStudent(@RequestBody Student student, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }
        try {
            studentService.saveStudent(student);
            return "success";
        } catch (Exception e) {
            return "error:保存学生信息失败, " + e.getMessage();
        }
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public String deleteStudent(@PathVariable Long id, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "权限不足");
        }
        try {
            return studentService.deleteStudent(id) ? "success" : "error:删除失败";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @PostMapping("/batch-delete")
    @ResponseBody
    public String batchDeleteStudents(@RequestBody List<Long> ids, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            return "error:权限不足";
        }
        if (ids == null || ids.isEmpty()) {
            return "error:请选择要删除的学生";
        }

        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMessages = new StringBuilder();
        for (Long id : ids) {
            try {
                if (studentService.deleteStudent(id)) {
                    successCount++;
                } else {
                    failCount++;
                    errorMessages.append("学生[ID:").append(id).append("]删除失败；");
                }
            } catch (Exception e) {
                failCount++;
                errorMessages.append("学生[ID:").append(id).append("]删除失败：").append(e.getMessage()).append("；");
            }
        }

        if (failCount == 0) {
            return "success";
        }
        String errorMsg = errorMessages.toString();
        if (errorMsg.length() > 500) {
            errorMsg = errorMsg.substring(0, 500) + "...";
        }
        return "error:成功删除" + successCount + "个，失败" + failCount + "个。" + errorMsg;
    }

    @GetMapping("/unassigned")
    @ResponseBody
    public List<Map<String, Object>> getUnassignedStudents(@RequestParam Long groupId, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, permissionError);
        }

        Integer currentYear = configService.getCurrentDefenseYear();
        if (currentYear == null) {
            currentYear = java.time.Year.now().getValue();
        }

        List<Student> allStudents = studentService.findByYear(currentYear);
        List<Student> unassignedStudents = allStudents.stream()
                .filter(s -> s.getDefenseGroupId() == null)
                .collect(Collectors.toList());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Student s : unassignedStudents) {
            Map<String, Object> info = new java.util.HashMap<>();
            info.put("id", s.getId());
            info.put("studentNo", s.getStudentNo());
            info.put("name", s.getName());
            info.put("departmentName", s.getDepartment() != null ? s.getDepartment().getName() : null);
            info.put("defenseType", s.getDefenseType());
            info.put("title", s.getTitle());
            result.add(info);
        }
        return result;
    }

    @GetMapping("/group/{groupId}/students")
    @ResponseBody
    public List<Map<String, Object>> getGroupStudents(@PathVariable Long groupId, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, permissionError);
        }

        List<Student> students = studentMapper.findByDefenseGroupId(groupId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Student s : students) {
            Map<String, Object> info = new java.util.HashMap<>();
            info.put("id", s.getId());
            info.put("studentNo", s.getStudentNo());
            info.put("name", s.getName());
            info.put("departmentName", s.getDepartment() != null ? s.getDepartment().getName() : null);
            info.put("defenseType", s.getDefenseType());
            info.put("title", s.getTitle());
            result.add(info);
        }
        return result;
    }
}
