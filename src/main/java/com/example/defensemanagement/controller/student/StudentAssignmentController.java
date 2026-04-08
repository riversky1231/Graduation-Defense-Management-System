package com.example.defensemanagement.controller.student;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * 学生分配 Controller。
 * 处理指导教师分配、评阅人分配、答辩小组分配等操作。
 */
@RestController
@RequestMapping("/department/student")
public class StudentAssignmentController extends AbstractStudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentMapper studentMapper;

    @PostMapping("/assign/advisor")
    @ResponseBody
    public String assignAdvisor(@RequestParam Long studentId, @RequestParam Long teacherId, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }
        try {
            studentService.assignAdvisor(studentId, teacherId);
            return "success";
        } catch (Exception e) {
            return "error:分配指导教师失败, " + e.getMessage();
        }
    }

    @PostMapping("/assign/reviewer")
    @ResponseBody
    public String assignReviewer(@RequestParam Long studentId, @RequestParam Long teacherId, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }
        try {
            studentService.assignReviewer(studentId, teacherId);
            return "success";
        } catch (Exception e) {
            return "error:分配评阅人失败, " + e.getMessage();
        }
    }

    @PostMapping("/assign/group")
    @ResponseBody
    public String assignDefenseGroup(@RequestParam Long studentId, @RequestParam Long groupId, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }
        try {
            studentService.assignDefenseGroup(studentId, groupId);
            return "success";
        } catch (Exception e) {
            return "error:分配答辩小组失败, " + e.getMessage();
        }
    }

    @PostMapping("/unassign/group")
    @ResponseBody
    public String unassignDefenseGroup(@RequestParam Long studentId, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }
        try {
            studentService.unassignDefenseGroup(studentId);
            return "success";
        } catch (Exception e) {
            return "error:移除失败, " + e.getMessage();
        }
    }

    @PostMapping("/assign-to-group")
    @ResponseBody
    public String assignStudentsToGroup(@RequestBody Map<String, Object> request, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }
        try {
            Long groupId = Long.valueOf(request.get("groupId").toString());
            @SuppressWarnings("unchecked")
            List<Integer> studentIds = (List<Integer>) request.get("studentIds");
            if (groupId == null || studentIds == null || studentIds.isEmpty()) {
                return "error:参数错误";
            }

            int successCount = 0;
            for (Integer studentId : studentIds) {
                if (studentService.assignDefenseGroup(studentId.longValue(), groupId)) {
                    successCount++;
                }
            }

            if (successCount == studentIds.size()) {
                return "success";
            }
            return "error:部分学生分配失败，成功: " + successCount + "/" + studentIds.size();
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @PostMapping("/remove-from-group")
    @ResponseBody
    public String removeStudentsFromGroup(@RequestBody Map<String, Object> request, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }
        try {
            @SuppressWarnings("unchecked")
            List<Integer> studentIds = (List<Integer>) request.get("studentIds");
            if (studentIds == null || studentIds.isEmpty()) {
                return "error:参数错误";
            }

            int successCount = 0;
            for (Integer studentId : studentIds) {
                if (studentMapper.updateDefenseGroupId(studentId.longValue(), null) > 0) {
                    successCount++;
                }
            }

            if (successCount == studentIds.size()) {
                return "success";
            }
            return "error:部分学生移除失败，成功: " + successCount + "/" + studentIds.size();
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }
}
