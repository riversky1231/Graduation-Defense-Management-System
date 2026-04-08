package com.example.defensemanagement.controller.volunteer;

import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentPreference;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.TeacherProfile;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.StudentPreferenceMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherProfileMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.StudentService;
import com.example.defensemanagement.service.VolunteerMatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentVolunteerControllersTest {

    @Mock
    private ConfigService configService;
    @Mock
    private StudentPreferenceMapper studentPreferenceMapper;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private TeacherMapper teacherMapper;
    @Mock
    private TeacherProfileMapper teacherProfileMapper;
    @Mock
    private StudentService studentService;
    @Mock
    private VolunteerMatchService volunteerMatchService;

    private DepartmentVolunteerQueryController queryController;
    private DepartmentVolunteerAssignmentController assignmentController;

    @BeforeEach
    void setUp() {
        queryController = new DepartmentVolunteerQueryController(
                configService,
                studentPreferenceMapper,
                studentMapper,
                teacherMapper,
                teacherProfileMapper,
                studentService,
                volunteerMatchService);
        assignmentController = new DepartmentVolunteerAssignmentController(
                configService,
                studentPreferenceMapper,
                studentMapper,
                teacherMapper,
                teacherProfileMapper,
                studentService,
                volunteerMatchService);
    }

    @Test
    void listDepartmentStudentsReturnsYearAndConfiguredCapacity() {
        MockHttpSession session = sessionWithDeptAdmin();
        Map<String, Object> student = new HashMap<>();
        student.put("studentId", 100L);
        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(configService.getConfigValue("TEACHER_MAX_STUDENTS")).thenReturn("6");
        when(studentPreferenceMapper.findByDepartmentAndYear(1L, 2026, true)).thenReturn(List.of(student));

        Map<String, Object> result = queryController.listDepartmentStudents(true, session);

        assertEquals(2026, result.get("year"));
        assertEquals(6, result.get("maxStudents"));
        assertEquals(1, ((List<?>) result.get("students")).size());
    }

    @Test
    void saveAdminAssignmentRejectsTeacherOutsideDepartment() {
        MockHttpSession session = sessionWithDeptAdmin();
        Map<String, Object> payload = new HashMap<>();
        payload.put("studentId", 11L);
        payload.put("adminAssignType", 1);
        payload.put("adminAssignedTeacherId", 99L);
        Student student = new Student();
        student.setId(11L);
        student.setDepartmentId(1L);
        student.setDefenseYear(2026);
        Teacher teacher = new Teacher();
        teacher.setId(99L);
        teacher.setDepartmentId(2L);
        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(studentMapper.findById(11L)).thenReturn(student);
        when(teacherMapper.findById(99L)).thenReturn(teacher);

        Map<String, Object> result = assignmentController.saveAdminAssignment(payload, session);

        assertEquals("指定导师不存在或不在本院系", result.get("error"));
    }

    @Test
    void allocateVolunteerAssignmentsHonorsCapacityAndReassignsUnmatchedStudent() {
        MockHttpSession session = sessionWithDeptAdmin();
        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(configService.getConfigValue("TEACHER_MAX_STUDENTS")).thenReturn("2");

        Teacher teacher1 = teacher(1L, 1L);
        Teacher teacher2 = teacher(2L, 1L);
        when(teacherMapper.findByDepartmentId(1L)).thenReturn(List.of(teacher1, teacher2));
        TeacherProfile profile1 = profile(1L);
        TeacherProfile profile2 = profile(2L);
        when(teacherProfileMapper.findByTeacherId(1L)).thenReturn(profile1);
        when(teacherProfileMapper.findByTeacherId(2L)).thenReturn(profile2);
        when(studentMapper.countByAdvisorAndYear(1L, 2026)).thenReturn(1);
        when(studentMapper.countByAdvisorAndYear(2L, 2026)).thenReturn(0);

        List<Map<String, Object>> candidates = new ArrayList<>();
        candidates.add(candidate(200L, 1, 1L));
        candidates.add(candidate(100L, null, null));
        when(studentPreferenceMapper.findByDepartmentAndYear(1L, 2026, true)).thenReturn(candidates);

        Student randomStudent = new Student();
        randomStudent.setId(100L);
        randomStudent.setDepartmentId(1L);
        randomStudent.setDefenseYear(2026);
        when(studentMapper.findById(100L)).thenReturn(randomStudent);

        StudentPreference preference = new StudentPreference();
        preference.setStudentId(100L);
        preference.setYear(2026);
        preference.setChoice1TeacherId(1L);
        preference.setChoice2TeacherId(2L);
        when(studentPreferenceMapper.findByStudentIdAndYear(100L, 2026)).thenReturn(preference);
        when(volunteerMatchService.calculateMatchScore(eq(preference), eq(randomStudent), eq(teacher2), eq(profile2),
                eq(2), eq(0), eq(2), anyMap())).thenReturn(82.0);

        Map<String, Object> result = assignmentController.allocateVolunteerAssignments(session);

        assertEquals(2, result.get("assignedCount"));
        assertEquals(1, result.get("specifiedAssigned"));
        assertEquals(1, result.get("randomAssigned"));
        assertTrue(((List<?>) result.get("failures")).isEmpty());
        verify(studentService).assignAdvisor(200L, 1L);
        verify(studentService).assignAdvisor(100L, 2L);
    }

    private MockHttpSession sessionWithDeptAdmin() {
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setDepartmentId(1L);
        Role role = new Role();
        role.setName("DEPT_ADMIN");
        user.setRole(role);
        session.setAttribute("currentUser", user);
        return session;
    }

    private Teacher teacher(Long id, Long departmentId) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setDepartmentId(departmentId);
        return teacher;
    }

    private TeacherProfile profile(Long teacherId) {
        TeacherProfile profile = new TeacherProfile();
        profile.setTeacherId(teacherId);
        return profile;
    }

    private Map<String, Object> candidate(Long studentId, Integer assignType, Long assignedTeacherId) {
        Map<String, Object> candidate = new HashMap<>();
        candidate.put("student_id", studentId);
        if (assignType != null) {
            candidate.put("admin_assign_type", assignType);
        }
        if (assignedTeacherId != null) {
            candidate.put("admin_assigned_teacher_id", assignedTeacherId);
        }
        return candidate;
    }
}
