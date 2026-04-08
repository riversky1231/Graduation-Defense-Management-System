package com.example.defensemanagement.controller.student;

import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.service.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 答辩组长 Controller。
 * 处理组长查看本组学生成绩等操作。
 */
@RestController
@RequestMapping("/department/student")
public class StudentLeaderController extends AbstractStudentController {

    private static final Logger log = LoggerFactory.getLogger(StudentLeaderController.class);

    @Autowired
    private DefenseGroupMapper defenseGroupMapper;

    @Autowired
    private DefenseGroupTeacherMapper defenseGroupTeacherMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeacherScoreRecordMapper teacherScoreRecordMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private ConfigService configService;

    /**
     * 获取当前用户（答辩组长）所在小组的学生成绩列表。
     */
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

        DefenseGroupTeacher groupTeacher = defenseGroupTeacherMapper.findByTeacherId(currentTeacher.getId());
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

        List<Map<String, Object>> studentScoreList = buildStudentScoreList(students, allScores);

        result.put("groupId", groupId);
        result.put("groupName", defenseGroupMapper.findById(groupId) != null
                ? defenseGroupMapper.findById(groupId).getName() : "");
        result.put("students", studentScoreList);
        result.put("year", currentYear);
        return result;
    }

    /**
     * 构建学生成绩列表。
     */
    private List<Map<String, Object>> buildStudentScoreList(
            List<Student> students, List<TeacherScoreRecord> allScores) {
        List<Map<String, Object>> studentScoreList = new ArrayList<>();
        for (Student student : students) {
            Map<String, Object> studentInfo = new HashMap<>();
            studentInfo.put("studentId", student.getId());
            studentInfo.put("studentNo", student.getStudentNo());
            studentInfo.put("studentName", student.getName());
            studentInfo.put("classInfo", student.getClassInfo());

            String departmentName = resolveDepartmentName(student);
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
        return studentScoreList;
    }

    /**
     * 解析学生所属院系名称。
     */
    private String resolveDepartmentName(Student student) {
        if (student.getDepartment() != null
                && student.getDepartment().getName() != null
                && !student.getDepartment().getName().isEmpty()) {
            return student.getDepartment().getName();
        }
        if (student.getDepartmentId() != null) {
            try {
                Department dept = departmentMapper.findById(student.getDepartmentId());
                if (dept != null && dept.getName() != null && !dept.getName().isEmpty()) {
                    return dept.getName();
                }
            } catch (Exception e) {
                log.warn("查询院系信息失败 studentId={}: {}", student.getId(), e.getMessage());
            }
        }
        return null;
    }
}
