package com.example.defensemanagement.controller.group;

import com.example.defensemanagement.common.ApiResponse;
import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentPreference;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.TeacherProfile;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.StudentPreferenceMapper;
import com.example.defensemanagement.mapper.TeacherProfileMapper;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.StudentService;
import com.example.defensemanagement.service.TeacherService;
import com.example.defensemanagement.service.VolunteerMatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupAssignmentControllersTest {

    @Mock
    private AuthService authService;
    @Mock
    private TeacherService teacherService;
    @Mock
    private DefenseGroupMapper defenseGroupMapper;
    @Mock
    private DefenseGroupTeacherMapper defenseGroupTeacherMapper;
    @Mock
    private DepartmentMapper departmentMapper;
    @Mock
    private ConfigService configService;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private StudentPreferenceMapper studentPreferenceMapper;
    @Mock
    private TeacherProfileMapper teacherProfileMapper;
    @Mock
    private StudentService studentService;
    @Mock
    private VolunteerMatchService volunteerMatchService;

    @Test
    void getUnassignedTeachersFiltersAlreadyAssignedTeachers() {
        GroupTeacherAssignmentController controller = newTeacherController();
        MockHttpSession session = deptAdminSession();
        User currentUser = (User) session.getAttribute("currentUser");
        Teacher teacher1 = teacher(1L, "T001");
        Teacher teacher2 = teacher(2L, "T002");
        Department department = new Department();
        department.setId(1L);
        department.setName("计算机学院");

        when(authService.hasPermission(currentUser, "MANAGE_TEACHERS")).thenReturn(true);
        when(teacherService.findByDepartmentId(1L)).thenReturn(List.of(teacher1, teacher2));
        when(defenseGroupTeacherMapper.findAll()).thenReturn(List.of(groupTeacher(9L, 2L)));
        when(departmentMapper.findById(1L)).thenReturn(department);

        List<Map<String, Object>> result = controller.getUnassignedTeachers(session);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).get("id"));
        assertEquals("计算机学院", result.get(0).get("departmentName"));
    }

    @Test
    void assignTeachersReturnsPartialSuccessWhenSomeTeachersAreSkipped() {
        GroupTeacherAssignmentController controller = newTeacherController();
        MockHttpSession session = deptAdminSession();
        User currentUser = (User) session.getAttribute("currentUser");
        Map<String, Object> request = new HashMap<>();
        request.put("groupId", 9L);
        request.put("teacherIds", List.of(1, 2));
        DefenseGroup group = new DefenseGroup();
        group.setId(9L);
        Teacher teacher1 = teacher(1L, "T001");
        Teacher teacher2 = teacher(2L, "T002");

        when(authService.hasPermission(currentUser, "MANAGE_TEACHERS")).thenReturn(true);
        when(defenseGroupMapper.findById(9L)).thenReturn(group);
        when(teacherService.findById(1L)).thenReturn(teacher1);
        when(teacherService.findById(2L)).thenReturn(teacher2);
        when(defenseGroupTeacherMapper.findByTeacherId(1L)).thenReturn(null);
        when(defenseGroupTeacherMapper.findByTeacherId(2L)).thenReturn(groupTeacher(8L, 2L));

        ApiResponse<Map<String, Object>> response = controller.assignTeachers(request, session);

        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("部分教师分配成功"));
        assertEquals(1, response.getData().get("successCount"));
        assertEquals(1, response.getData().get("skipCount"));
        verify(defenseGroupTeacherMapper).insert(9L, 1L, 0);
    }

    @Test
    void saveGroupMaxStudentsPersistsConfiguredValue() {
        GroupAssignmentConfigController controller = newConfigController();
        MockHttpSession session = deptAdminSession();
        User currentUser = (User) session.getAttribute("currentUser");
        Map<String, Object> body = new HashMap<>();
        body.put("maxStudents", 12);

        when(authService.hasPermission(currentUser, "MANAGE_TEACHERS")).thenReturn(true);

        Map<String, Object> result = controller.saveGroupMaxStudents(body, session);

        assertEquals(true, result.get("success"));
        assertEquals(12, result.get("maxStudents"));
        verify(configService).saveConfig("GROUP_MAX_STUDENTS", "12", "每答辩小组最大学生人数");
    }

    @Test
    void randomAssignStudentsAssignsOnlyUnassignedStudents() {
        GroupRandomAssignmentController controller = newRandomController();
        MockHttpSession session = deptAdminSession();
        User currentUser = (User) session.getAttribute("currentUser");
        Student student1 = student(101L, null);
        Student student2 = student(102L, 7L);
        DefenseGroup group = new DefenseGroup();
        group.setId(9L);

        when(authService.hasPermission(currentUser, "MANAGE_TEACHERS")).thenReturn(true);
        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(configService.getConfigValue("GROUP_MAX_STUDENTS")).thenReturn("2");
        when(studentMapper.findByDepartmentAndYear(1L, 2026)).thenReturn(List.of(student1, student2));
        when(defenseGroupMapper.findByDepartmentId(1L)).thenReturn(List.of(group));
        when(studentMapper.findByDefenseGroupId(9L)).thenReturn(List.of());

        Map<String, Object> result = controller.randomAssignStudents(session);

        assertEquals(true, result.get("success"));
        assertEquals(1, result.get("assigned"));
        assertEquals(1, result.get("total"));
        verify(studentMapper).updateDefenseGroupId(101L, 9L);
    }

    @Test
    void allocateVolunteerAssignmentsHandlesSpecifiedAndRandomStudents() {
        GroupVolunteerAllocationController controller = newAllocationController();
        MockHttpSession session = deptAdminSession();
        User currentUser = (User) session.getAttribute("currentUser");
        Teacher teacher1 = teacher(1L, "T001");
        Teacher teacher2 = teacher(2L, "T002");
        TeacherProfile profile1 = new TeacherProfile();
        profile1.setTeacherId(1L);
        TeacherProfile profile2 = new TeacherProfile();
        profile2.setTeacherId(2L);
        Student randomStudent = student(201L, null);
        StudentPreference preference = new StudentPreference();
        preference.setStudentId(201L);
        preference.setYear(2026);

        Map<String, Object> specified = new HashMap<>();
        specified.put("student_id", 200L);
        specified.put("admin_assign_type", 1);
        specified.put("admin_assigned_teacher_id", 1L);
        Map<String, Object> random = new HashMap<>();
        random.put("student_id", 201L);

        when(authService.hasPermission(currentUser, "MANAGE_TEACHERS")).thenReturn(true);
        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(configService.getConfigValue("GROUP_MAX_STUDENTS")).thenReturn("2");
        when(teacherService.findByDepartmentId(1L)).thenReturn(List.of(teacher1, teacher2));
        when(studentMapper.countByAdvisorAndYear(1L, 2026)).thenReturn(0);
        when(studentMapper.countByAdvisorAndYear(2L, 2026)).thenReturn(0);
        when(teacherProfileMapper.findByTeacherId(1L)).thenReturn(profile1);
        when(teacherProfileMapper.findByTeacherId(2L)).thenReturn(profile2);
        when(studentPreferenceMapper.findByDepartmentAndYear(1L, 2026, true)).thenReturn(List.of(specified, random));
        when(studentMapper.findById(201L)).thenReturn(randomStudent);
        when(studentPreferenceMapper.findByStudentIdAndYear(201L, 2026)).thenReturn(preference);
        when(volunteerMatchService.calculateMatchScore(eq(preference), eq(randomStudent), eq(teacher1), eq(profile1),
                eq(1), eq(1), eq(2), anyMap())).thenReturn(70.0);
        when(volunteerMatchService.calculateMatchScore(eq(preference), eq(randomStudent), eq(teacher2), eq(profile2),
                eq(2), eq(0), eq(2), anyMap())).thenReturn(88.0);

        Map<String, Object> result = controller.allocateVolunteerAssignments(session);

        assertEquals(2, result.get("assignedCount"));
        assertEquals(1, result.get("specifiedAssigned"));
        assertEquals(1, result.get("randomAssigned"));
        assertTrue(((List<?>) result.get("failures")).isEmpty());
        verify(studentService).assignAdvisor(200L, 1L);
        verify(studentService).assignAdvisor(201L, 2L);
    }

    private GroupTeacherAssignmentController newTeacherController() {
        return new GroupTeacherAssignmentController(
                authService,
                teacherService,
                defenseGroupMapper,
                defenseGroupTeacherMapper,
                departmentMapper,
                configService,
                studentMapper);
    }

    private GroupAssignmentConfigController newConfigController() {
        return new GroupAssignmentConfigController(
                authService,
                teacherService,
                defenseGroupMapper,
                defenseGroupTeacherMapper,
                departmentMapper,
                configService,
                studentMapper);
    }

    private GroupRandomAssignmentController newRandomController() {
        return new GroupRandomAssignmentController(
                authService,
                teacherService,
                defenseGroupMapper,
                defenseGroupTeacherMapper,
                departmentMapper,
                configService,
                studentMapper);
    }

    private GroupVolunteerAllocationController newAllocationController() {
        return new GroupVolunteerAllocationController(
                authService,
                teacherService,
                defenseGroupMapper,
                defenseGroupTeacherMapper,
                departmentMapper,
                configService,
                studentMapper,
                studentPreferenceMapper,
                teacherProfileMapper,
                studentService,
                volunteerMatchService);
    }

    private MockHttpSession deptAdminSession() {
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setDepartmentId(1L);
        com.example.defensemanagement.entity.Role role = new com.example.defensemanagement.entity.Role();
        role.setName("DEPT_ADMIN");
        user.setRole(role);
        session.setAttribute("currentUser", user);
        return session;
    }

    private Teacher teacher(Long id, String teacherNo) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setTeacherNo(teacherNo);
        teacher.setName("Teacher-" + id);
        teacher.setDepartmentId(1L);
        return teacher;
    }

    private DefenseGroupTeacher groupTeacher(Long groupId, Long teacherId) {
        DefenseGroupTeacher relation = new DefenseGroupTeacher();
        relation.setGroupId(groupId);
        relation.setTeacherId(teacherId);
        return relation;
    }

    private Student student(Long id, Long groupId) {
        Student student = new Student();
        student.setId(id);
        student.setDefenseGroupId(groupId);
        student.setDepartmentId(1L);
        return student;
    }
}
