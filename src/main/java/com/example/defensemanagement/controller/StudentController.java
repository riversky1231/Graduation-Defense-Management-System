package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/department/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private DefenseGroupMapper defenseGroupMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private TeacherScoreRecordMapper teacherScoreRecordMapper;

    @Autowired
    private DefenseGroupTeacherMapper defenseGroupTeacherMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    private String checkDeptAdmin(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "error:权限不足";
        }
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
        if ("SUPER_ADMIN".equals(roleName) || "DEPT_ADMIN".equals(roleName)) {
            return null;
        }
        return "error:权限不足";
    }

    private Long findTeacherIdByUserId(Long userId) {
        Teacher teacher = teacherMapper.findByUserId(userId);
        return teacher != null ? teacher.getId() : null;
    }

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

    @GetMapping("/currentYear")
    @ResponseBody
    public Integer getCurrentYear() {
        return configService.getCurrentDefenseYear();
    }

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

    @GetMapping("/leader/group/scores")
    @ResponseBody
    public Map<String, Object> getLeaderGroupScores(HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
        if (currentTeacher == null) {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser != null && currentUser.getRole() != null
                    && ("TEACHER".equals(currentUser.getRole().getName())
                    || "DEFENSE_LEADER".equals(currentUser.getRole().getName()))) {
                currentTeacher = teacherMapper.findByUserId(currentUser.getId());
            }
        }

        if (currentTeacher == null) {
            result.put("error", "未登录或不是教师");
            return result;
        }

        com.example.defensemanagement.entity.DefenseGroupTeacher groupTeacher =
                defenseGroupTeacherMapper.findByTeacherId(currentTeacher.getId());
        boolean isLeaderInGroup = groupTeacher != null
                && groupTeacher.getIsLeader() != null
                && groupTeacher.getIsLeader() == 1;

        if (!isLeaderInGroup) {
            User currentUser = (User) session.getAttribute("currentUser");
            boolean isDefenseLeaderRole = currentUser != null
                    && currentUser.getRole() != null
                    && "DEFENSE_LEADER".equals(currentUser.getRole().getName());

            if (isDefenseLeaderRole) {
                result.put("error", "您已被设置为答辩组长，但尚未被分配到任何答辩小组，请联系管理员");
            } else {
                result.put("error", "您不是任何小组的组长");
            }
            return result;
        }

        Long groupId = groupTeacher.getGroupId();
        Integer currentYear = configService.getCurrentDefenseYear();
        if (currentYear == null) {
            result.put("error", "请先设置当前答辩年份");
            return result;
        }

        List<Student> students = studentMapper.findByDefenseGroupId(groupId);
        List<TeacherScoreRecord> allScores = teacherScoreRecordMapper.findByGroupIdAndYear(groupId, currentYear);

        List<Map<String, Object>> studentScoreList = new ArrayList<>();
        for (Student student : students) {
            Map<String, Object> studentInfo = new HashMap<>();
            studentInfo.put("studentId", student.getId());
            studentInfo.put("studentNo", student.getStudentNo());
            studentInfo.put("studentName", student.getName());
            studentInfo.put("classInfo", student.getClassInfo());

            String departmentName = null;
            if (student.getDepartment() != null
                    && student.getDepartment().getName() != null
                    && !student.getDepartment().getName().isEmpty()) {
                departmentName = student.getDepartment().getName();
                System.out.println("学生ID: " + student.getId() + " - 从department对象获取院系名称: " + departmentName);
            }
            if (departmentName == null && student.getDepartmentId() != null) {
                try {
                    Department dept = departmentMapper.findById(student.getDepartmentId());
                    if (dept != null && dept.getName() != null && !dept.getName().isEmpty()) {
                        departmentName = dept.getName();
                        System.out.println("学生ID: " + student.getId() + " - 从departmentId(" + student.getDepartmentId()
                                + ")查询到院系名称: " + departmentName);
                    } else {
                        System.out.println("学生ID: " + student.getId() + " - 警告：departmentId(" + student.getDepartmentId()
                                + ")对应的院系不存在");
                    }
                } catch (Exception e) {
                    System.err.println("学生ID: " + student.getId() + " - 查询院系信息时出错: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            if (departmentName == null) {
                System.out.println("学生ID: " + student.getId() + " - 警告：无法获取院系名称，departmentId="
                        + student.getDepartmentId() + ", department对象=" + (student.getDepartment() != null ? "存在但无名称" : "null"));
            }

            studentInfo.put("departmentName", departmentName);
            studentInfo.put("defenseType", student.getDefenseType());
            studentInfo.put("title", student.getTitle());

            List<Map<String, Object>> teacherScores = new ArrayList<>();
            for (TeacherScoreRecord record : allScores) {
                if (record.getStudentId() != null && record.getStudentId().equals(student.getId())) {
                    Map<String, Object> scoreInfo = new HashMap<>();
                    scoreInfo.put("teacherId", record.getTeacherId());
                    if (record.getTeacher() != null) {
                        scoreInfo.put("teacherName", record.getTeacher().getName());
                        scoreInfo.put("teacherNo", record.getTeacher().getTeacherNo());
                    }
                    scoreInfo.put("item1Score", record.getItem1Score());
                    scoreInfo.put("item2Score", record.getItem2Score());
                    scoreInfo.put("item3Score", record.getItem3Score());
                    scoreInfo.put("item4Score", record.getItem4Score());
                    scoreInfo.put("item5Score", record.getItem5Score());
                    scoreInfo.put("item6Score", record.getItem6Score());
                    scoreInfo.put("totalScore", record.getTotalScore());
                    scoreInfo.put("submitTime", record.getSubmitTime());
                    teacherScores.add(scoreInfo);
                }
            }
            studentInfo.put("teacherScores", teacherScores);
            studentScoreList.add(studentInfo);
        }

        result.put("groupId", groupId);
        result.put("groupName", defenseGroupMapper.findById(groupId) != null
                ? defenseGroupMapper.findById(groupId).getName() : "");
        result.put("students", studentScoreList);
        result.put("year", currentYear);
        return result;
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
            Map<String, Object> info = new HashMap<>();
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
            Map<String, Object> info = new HashMap<>();
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
