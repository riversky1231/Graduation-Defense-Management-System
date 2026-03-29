package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.RoleMapper;
import com.example.defensemanagement.mapper.UserMapper;
import com.example.defensemanagement.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.time.LocalDate;

@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private DefenseGroupMapper defenseGroupMapper;

    @Autowired
    private TeacherScoreRecordMapper teacherScoreRecordMapper;

    @Autowired
    private StudentFinalScoreMapper studentFinalScoreMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Student findById(Long studentId) {
        return studentMapper.findById(studentId);
    }

    @Override
    public List<Student> findAll() {
        return studentMapper.findAll();
    }

    @Override
    public List<Student> findByDepartmentAndYear(Long departmentId, Integer currentYear) {
        return studentMapper.findByDepartmentAndYear(departmentId, currentYear);
    }
    
    @Override
    public List<Student> findByYear(Integer year) {
        return studentMapper.findByYear(year);
    }

    @Override
    public List<Student> getStudentsByGroup(Long groupId) {
        return studentMapper.findByDefenseGroupId(groupId);
    }

    @Override
    public List<Student> getStudentsByAdvisor(Long teacherId, Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        return studentMapper.findByAdvisorIdAndYear(teacherId, year);
    }

    @Override
    public List<Student> getStudentsByReviewer(Long teacherId, Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        return studentMapper.findByReviewerIdAndYear(teacherId, year);
    }

    @Override
    @Transactional
    public boolean assignAdvisor(Long studentId, Long teacherId) {
        Student student = findById(studentId);
        if (student == null) {
            throw new IllegalArgumentException("学生不存在：" + studentId);
        }

        // 检查教师是否存在
        Teacher teacher = teacherMapper.findById(teacherId);
        if (teacher == null) {
            throw new IllegalArgumentException("指导教师不存在：" + teacherId);
        }

        // 如果修改了指导教师，需要级联删除原指导教师的打分记录和成绩
        Long oldAdvisorId = student.getAdvisorTeacherId();
        if (oldAdvisorId != null && !oldAdvisorId.equals(teacherId) && student.getDefenseYear() != null) {
            // 删除原指导教师的打分记录
            teacherScoreRecordMapper.deleteByStudentIdAndTeacherId(studentId, oldAdvisorId, student.getDefenseYear());
            // 清空最终成绩表中的指导教师成绩
            studentFinalScoreMapper.clearAdvisorScore(studentId, student.getDefenseYear());
        }

        student.setAdvisorTeacherId(teacherId);
        return studentMapper.update(student) > 0;
    }

    @Override
    @Transactional
    public boolean unassignAdvisor(Long studentId) {
        Student student = findById(studentId);
        if (student == null) {
            throw new IllegalArgumentException("学生不存在：" + studentId);
        }

        Long oldAdvisorId = student.getAdvisorTeacherId();
        if (oldAdvisorId == null) {
            return true;
        }

        if (student.getDefenseYear() != null) {
            teacherScoreRecordMapper.deleteByStudentIdAndTeacherId(studentId, oldAdvisorId, student.getDefenseYear());
            studentFinalScoreMapper.clearAdvisorScore(studentId, student.getDefenseYear());
        }

        return studentMapper.updateAdvisorTeacherId(studentId, null) > 0;
    }

    @Override
    @Transactional
    public boolean assignReviewer(Long studentId, Long teacherId) {
        Student student = findById(studentId);
        if (student == null) {
            throw new IllegalArgumentException("学生不存在：" + studentId);
        }

        // 检查教师是否存在
        Teacher teacher = teacherMapper.findById(teacherId);
        if (teacher == null) {
            throw new IllegalArgumentException("评阅教师不存在：" + teacherId);
        }

        // 如果修改了评阅教师，需要级联删除原评阅教师的打分记录和成绩
        Long oldReviewerId = student.getReviewerTeacherId();
        if (oldReviewerId != null && !oldReviewerId.equals(teacherId) && student.getDefenseYear() != null) {
            // 删除原评阅教师的打分记录
            teacherScoreRecordMapper.deleteByStudentIdAndTeacherId(studentId, oldReviewerId, student.getDefenseYear());
            // 清空最终成绩表中的评阅教师成绩
            studentFinalScoreMapper.clearReviewerScore(studentId, student.getDefenseYear());
        }

        student.setReviewerTeacherId(teacherId);
        return studentMapper.update(student) > 0;
    }

    @Override
    @Transactional
    public boolean assignDefenseGroup(Long studentId, Long groupId) {
        Student student = findById(studentId);
        if (student == null) {
            throw new IllegalArgumentException("学生不存在：" + studentId);
        }

        // 检查小组是否存在
        DefenseGroup group = defenseGroupMapper.findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("答辩小组不存在：" + groupId);
        }
        
        // 验证：每个学生只能属于一个小组
        // 如果学生已经在其他小组，需要先移除（实际上数据库字段是单个，所以直接更新即可）
        // 但这里我们添加一个提示，如果学生已经在其他小组，会覆盖之前的分配
        if (student.getDefenseGroupId() != null && !student.getDefenseGroupId().equals(groupId)) {
            // 学生已经在其他小组，这里直接更新到新小组
            // 如果需要更严格的验证，可以抛出异常
        }

        student.setDefenseGroupId(groupId);
        return studentMapper.update(student) > 0;
    }

    @Override
    @Transactional
    public boolean saveStudent(Student student) {
        if (student.getId() == null) {
            // 检查学号是否重复
            if (student.getStudentNo() != null && studentMapper.findByStudentNoAndYear(student.getStudentNo(), student.getDefenseYear()) != null) {
                throw new RuntimeException("学号在当前年份已存在");
            }
            boolean inserted = studentMapper.insert(student) > 0;
            if (inserted) {
                syncStudentUser(student, null);
            }
            return inserted;
        } else {
            // 更新逻辑
            Student existing = studentMapper.findById(student.getId());
            String oldStudentNo = existing != null ? existing.getStudentNo() : null;
            boolean updated = studentMapper.update(student) > 0;
            if (updated) {
                syncStudentUser(student, oldStudentNo);
            }
            return updated;
        }
    }

    @Override
    @Transactional
    public boolean deleteStudent(Long studentId) {
        if (studentId == null) return false;
        Student student = studentMapper.findById(studentId);
        boolean deleted = studentMapper.deleteById(studentId) > 0;
        if (deleted && student != null && student.getStudentNo() != null) {
            User user = userMapper.findByUsername(student.getStudentNo());
            if (user != null) {
                Role role = user.getRoleId() != null ? roleMapper.findById(user.getRoleId()) : null;
                if (role != null && "STUDENT".equals(role.getName())) {
                    userMapper.deleteById(user.getId());
                }
            }
        }
        return deleted;
    }

    private void syncStudentUser(Student student, String oldStudentNo) {
        if (student == null || student.getStudentNo() == null) {
            return;
        }
        Role studentRole = roleMapper.findByName("STUDENT");
        if (studentRole == null) {
            throw new RuntimeException("STUDENT role not found");
        }

        String newStudentNo = student.getStudentNo();
        User user = null;
        if (oldStudentNo != null && !oldStudentNo.equals(newStudentNo)) {
            user = userMapper.findByUsername(oldStudentNo);
        }
        if (user == null) {
            user = userMapper.findByUsername(newStudentNo);
        }

        if (user == null) {
            User newUser = new User();
            newUser.setUsername(newStudentNo);
            // 初始密码为学号本身，首次登录后应修改
            newUser.setPassword(passwordEncoder.encode(newStudentNo));
            newUser.setRealName(student.getName());
            newUser.setEmail(student.getEmail());
            newUser.setPhone(student.getPhone());
            newUser.setStatus(1);
            newUser.setRoleId(studentRole.getId());
            newUser.setDepartmentId(student.getDepartmentId());
            userMapper.insert(newUser);
            return;
        }

        if (user.getRoleId() != null) {
            Role role = roleMapper.findById(user.getRoleId());
            if (role != null && !"STUDENT".equals(role.getName())) {
                throw new RuntimeException("Username already used by non-student account: " + user.getUsername());
            }
        }

        user.setUsername(newStudentNo);
        if (student.getName() != null) user.setRealName(student.getName());
        if (student.getEmail() != null) user.setEmail(student.getEmail());
        if (student.getPhone() != null) user.setPhone(student.getPhone());
        if (student.getDepartmentId() != null) user.setDepartmentId(student.getDepartmentId());
        user.setRoleId(studentRole.getId());
        if (user.getStatus() == null) user.setStatus(1);
        userMapper.update(user);
    }

    @Override
    @Transactional
    public boolean unassignDefenseGroup(Long studentId) {
        if (studentId == null) return false;
        // explicitly set defense_group_id to NULL (XML update won't set null values)
        return studentMapper.updateDefenseGroupId(studentId, null) > 0;
    }
    
    @Override
    public List<Student> searchStudents(String keyword, Long departmentId, Integer year, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return studentMapper.searchStudents(keyword, departmentId, year, offset, pageSize);
    }
    
    @Override
    public int countStudents(String keyword, Long departmentId, Integer year) {
        return studentMapper.countStudents(keyword, departmentId, year);
    }
}

