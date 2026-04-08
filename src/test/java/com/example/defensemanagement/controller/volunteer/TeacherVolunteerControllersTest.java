package com.example.defensemanagement.controller.volunteer;

import com.example.defensemanagement.common.MatchScoreDetail;
import com.example.defensemanagement.common.RelevanceAnalysisResult;
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
import com.example.defensemanagement.service.impl.ConfigServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherVolunteerControllersTest {

    @Mock
    private TeacherMapper teacherMapper;
    @Mock
    private StudentPreferenceMapper studentPreferenceMapper;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private StudentService studentService;
    @Mock
    private ConfigService configService;
    @Mock
    private TeacherProfileMapper teacherProfileMapper;
    @Mock
    private VolunteerMatchService volunteerMatchService;

    @Test
    void getVolunteerConfigResolvesTeacherFromCurrentUser() {
        TeacherVolunteerQueryController controller = newQueryController();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", teacherUser(99L));
        when(teacherMapper.findByUserId(99L)).thenReturn(teacher(1L));
        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(configService.getConfigValue(ConfigServiceImpl.KEY_TEACHER_MAX_STUDENTS)).thenReturn("6");
        when(configService.getConfigValue(ConfigServiceImpl.KEY_VOLUNTEER_DEADLINE)).thenReturn("2099-12-31 23:59");
        when(studentMapper.countByAdvisorAndYear(1L, 2026)).thenReturn(2);

        Map<String, Object> result = controller.getVolunteerConfig(session);

        assertEquals(2026, result.get("year"));
        assertEquals(6, result.get("maxStudents"));
        assertEquals("2099-12-31 23:59", result.get("deadline"));
        assertEquals(2, result.get("assignedCount"));
        assertEquals(false, result.get("deadlinePassed"));
    }

    @Test
    void getVolunteerListRejectsInvalidRound() {
        TeacherVolunteerQueryController controller = newQueryController();

        Map<String, Object> result = controller.getVolunteerList(4, teacherSession(1L));

        assertEquals("round参数必须为1-3", result.get("error"));
    }

    @Test
    void getVolunteerListBuildsSortedItems() {
        TeacherVolunteerQueryController controller = newQueryController();
        MockHttpSession session = teacherSession(1L);
        Teacher teacher = (Teacher) session.getAttribute("currentTeacher");
        TeacherProfile profile = profile(1L);

        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(configService.getConfigValue(ConfigServiceImpl.KEY_TEACHER_MAX_STUDENTS)).thenReturn("3");
        when(configService.getConfigValue(ConfigServiceImpl.KEY_VOLUNTEER_DEADLINE)).thenReturn(null);
        when(studentMapper.countByAdvisorAndYear(1L, 2026)).thenReturn(1);
        when(studentPreferenceMapper.findByTeacherAndYearAndRound(1L, 2026, 1)).thenReturn(List.of(
                volunteerRow(10L, "S002", null, null, null, "uploads/volunteer/a.pdf", null, null),
                volunteerRow(11L, "S001", 1L, null, null, "uploads/volunteer/b.pdf", null, null)));
        when(teacherProfileMapper.findByTeacherId(1L)).thenReturn(profile);
        when(studentMapper.findById(10L)).thenReturn(student(10L, 2026, null));
        when(studentMapper.findById(11L)).thenReturn(student(11L, 2026, 1L));
        when(studentPreferenceMapper.findByStudentIdAndYear(10L, 2026)).thenReturn(preference(10L, 2026, 1L, null, null));
        when(studentPreferenceMapper.findByStudentIdAndYear(11L, 2026)).thenReturn(preference(11L, 2026, 1L, null, null));
        when(volunteerMatchService.calculateMatchDetail(any(), any(), eq(teacher), eq(profile), eq(2), eq(1), eq(3), anyMap()))
                .thenReturn(matchDetail(72.345, 61.2), matchDetail(88.888, 77.7));

        Map<String, Object> result = controller.getVolunteerList(1, session);

        assertEquals(2026, result.get("year"));
        assertEquals(1, result.get("round"));
        assertEquals(1, result.get("assignedCount"));
        assertEquals(3, result.get("maxStudents"));
        assertEquals(false, result.get("deadlinePassed"));

        List<Map<String, Object>> items = castItems(result.get("items"));
        assertEquals(2, items.size());
        assertEquals("S001", items.get(0).get("studentNo"));
        assertEquals(88.89, items.get(0).get("matchScore"));
        assertEquals(true, items.get(0).get("assignedToMe"));
        assertEquals(true, items.get(0).get("canCancel"));
        assertEquals("S002", items.get(1).get("studentNo"));
        assertEquals(72.35, items.get(1).get("matchScore"));
        assertEquals(true, items.get(1).get("canAccept"));
        assertEquals("uploads/volunteer/a.pdf", items.get(1).get("filePath"));
    }

    @Test
    void getAllVolunteersIncludesVolunteerRound() {
        TeacherVolunteerQueryController controller = newQueryController();
        MockHttpSession session = teacherSession(1L);
        Teacher teacher = (Teacher) session.getAttribute("currentTeacher");
        TeacherProfile profile = profile(1L);

        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(configService.getConfigValue(ConfigServiceImpl.KEY_TEACHER_MAX_STUDENTS)).thenReturn("4");
        when(configService.getConfigValue(ConfigServiceImpl.KEY_VOLUNTEER_DEADLINE)).thenReturn(null);
        when(studentMapper.countByAdvisorAndYear(1L, 2026)).thenReturn(0);
        when(studentPreferenceMapper.findAllByTeacherAndYear(1L, 2026)).thenReturn(List.of(
                volunteerRow(12L, "S003", null, 9L, 1L, null, "uploads/volunteer/second.pdf", null)));
        when(teacherProfileMapper.findByTeacherId(1L)).thenReturn(profile);
        when(studentMapper.findById(12L)).thenReturn(student(12L, 2026, null));
        when(studentPreferenceMapper.findByStudentIdAndYear(12L, 2026)).thenReturn(preference(12L, 2026, 9L, 1L, null));
        when(volunteerMatchService.calculateMatchDetail(any(), any(), eq(teacher), eq(profile), eq(4), eq(0), eq(4), anyMap()))
                .thenReturn(matchDetail(90.0, 80.0));

        Map<String, Object> result = controller.getAllVolunteers(session);

        List<Map<String, Object>> items = castItems(result.get("items"));
        assertEquals(1, items.size());
        assertEquals(2, items.get(0).get("volunteerRound"));
        assertEquals("uploads/volunteer/second.pdf", items.get(0).get("filePath"));
    }

    @Test
    void acceptVolunteerAssignsAdvisorWhenSelectionIsValid() {
        TeacherVolunteerDecisionController controller = newDecisionController();

        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(configService.getConfigValue(ConfigServiceImpl.KEY_VOLUNTEER_DEADLINE)).thenReturn(null);
        when(configService.getConfigValue(ConfigServiceImpl.KEY_TEACHER_MAX_STUDENTS)).thenReturn("3");
        when(studentMapper.findById(20L)).thenReturn(student(20L, 2026, null));
        when(studentPreferenceMapper.findByStudentIdAndYear(20L, 2026)).thenReturn(preference(20L, 2026, null, 1L, null));
        when(studentMapper.countByAdvisorAndYear(1L, 2026)).thenReturn(2);

        String result = controller.acceptVolunteer(20L, 2, teacherSession(1L));

        assertEquals("success", result);
        verify(studentService).assignAdvisor(20L, 1L);
    }

    @Test
    void acceptVolunteerRejectsWhenTeacherIsNotSelected() {
        TeacherVolunteerDecisionController controller = newDecisionController();

        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(configService.getConfigValue(ConfigServiceImpl.KEY_VOLUNTEER_DEADLINE)).thenReturn(null);
        when(studentMapper.findById(20L)).thenReturn(student(20L, 2026, null));
        when(studentPreferenceMapper.findByStudentIdAndYear(20L, 2026)).thenReturn(preference(20L, 2026, null, 2L, null));

        String result = controller.acceptVolunteer(20L, 2, teacherSession(1L));

        assertEquals("error:该学生未选择您作为本轮志愿导师", result);
        verify(studentService, never()).assignAdvisor(any(), any());
    }

    @Test
    void cancelVolunteerRejectsWhenStudentIsNotAssignedToTeacher() {
        TeacherVolunteerDecisionController controller = newDecisionController();

        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(configService.getConfigValue(ConfigServiceImpl.KEY_VOLUNTEER_DEADLINE)).thenReturn(null);
        when(studentMapper.findById(21L)).thenReturn(student(21L, 2026, 2L));

        String result = controller.cancelVolunteer(21L, 1, teacherSession(1L));

        assertEquals("error:该学生并非由您录取", result);
        verify(studentService, never()).unassignAdvisor(any());
    }

    @Test
    void downloadVolunteerFileBlocksUnexpectedPath() {
        TeacherVolunteerFileController controller = newFileController();
        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(studentPreferenceMapper.findByStudentIdAndYear(30L, 2026))
                .thenReturn(preferenceWithFiles(30L, 2026, 1L, null, null, "D:/tmp/outside.pdf", null, null));

        ResponseEntity<byte[]> response = controller.downloadVolunteerFile(30L, 1, teacherSession(1L));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void downloadVolunteerFileReturnsPdfBytes() throws IOException {
        TeacherVolunteerFileController controller = newFileController();
        Path uploadsDir = Paths.get("uploads", "volunteer");
        Files.createDirectories(uploadsDir);
        Path relativeFile = uploadsDir.resolve("teacher-volunteer-test-" + System.nanoTime() + ".pdf");
        byte[] expected = "pdf-bytes".getBytes();
        Files.write(relativeFile, expected);
        when(configService.getCurrentDefenseYear()).thenReturn(2026);
        when(studentPreferenceMapper.findByStudentIdAndYear(31L, 2026))
                .thenReturn(preferenceWithFiles(31L, 2026, 1L, null, null, relativeFile.toString(), null, null));

        try {
            ResponseEntity<byte[]> response = controller.downloadVolunteerFile(31L, 1, teacherSession(1L));

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertArrayEquals(expected, response.getBody());
            assertTrue(response.getHeaders().getFirst("Content-Disposition")
                    .contains(relativeFile.getFileName().toString()));
        } finally {
            Files.deleteIfExists(relativeFile);
        }
    }

    private TeacherVolunteerQueryController newQueryController() {
        return new TeacherVolunteerQueryController(
                teacherMapper,
                studentPreferenceMapper,
                studentMapper,
                studentService,
                configService,
                teacherProfileMapper,
                volunteerMatchService);
    }

    private TeacherVolunteerDecisionController newDecisionController() {
        return new TeacherVolunteerDecisionController(
                teacherMapper,
                studentPreferenceMapper,
                studentMapper,
                studentService,
                configService,
                teacherProfileMapper,
                volunteerMatchService);
    }

    private TeacherVolunteerFileController newFileController() {
        return new TeacherVolunteerFileController(
                teacherMapper,
                studentPreferenceMapper,
                studentMapper,
                studentService,
                configService,
                teacherProfileMapper,
                volunteerMatchService);
    }

    private MockHttpSession teacherSession(Long teacherId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentTeacher", teacher(teacherId));
        return session;
    }

    private Teacher teacher(Long id) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setName("Teacher-" + id);
        teacher.setDepartmentId(1L);
        return teacher;
    }

    private TeacherProfile profile(Long teacherId) {
        TeacherProfile profile = new TeacherProfile();
        profile.setTeacherId(teacherId);
        return profile;
    }

    private User teacherUser(Long userId) {
        User user = new User();
        user.setId(userId);
        Role role = new Role();
        role.setName("TEACHER");
        user.setRole(role);
        return user;
    }

    private Student student(Long id, Integer year, Long advisorTeacherId) {
        Student student = new Student();
        student.setId(id);
        student.setDefenseYear(year);
        student.setAdvisorTeacherId(advisorTeacherId);
        return student;
    }

    private StudentPreference preference(Long studentId, Integer year, Long choice1, Long choice2, Long choice3) {
        return preferenceWithFiles(studentId, year, choice1, choice2, choice3, null, null, null);
    }

    private StudentPreference preferenceWithFiles(
            Long studentId,
            Integer year,
            Long choice1,
            Long choice2,
            Long choice3,
            String file1,
            String file2,
            String file3) {
        StudentPreference preference = new StudentPreference();
        preference.setStudentId(studentId);
        preference.setYear(year);
        preference.setChoice1TeacherId(choice1);
        preference.setChoice2TeacherId(choice2);
        preference.setChoice3TeacherId(choice3);
        preference.setFile1Path(file1);
        preference.setFile2Path(file2);
        preference.setFile3Path(file3);
        return preference;
    }

    private MatchScoreDetail matchDetail(double totalScore, double relevanceScore) {
        return new MatchScoreDetail(
                totalScore,
                10.0,
                20.0,
                30.0,
                relevanceScore,
                RelevanceAnalysisResult.SOURCE_MODEL,
                "reason",
                "FILE",
                "matched");
    }

    private Map<String, Object> volunteerRow(
            Long studentId,
            String studentNo,
            Long advisorTeacherId,
            Long choice1TeacherId,
            Long choice2TeacherId,
            String file1Path,
            String file2Path,
            String file3Path) {
        Map<String, Object> row = new HashMap<>();
        row.put("student_id", studentId);
        row.put("student_no", studentNo);
        row.put("student_name", "Student-" + studentId);
        row.put("class_info", "Class");
        row.put("defense_type", "PAPER");
        row.put("title", "Title-" + studentId);
        row.put("summary", "Summary-" + studentId);
        row.put("advisor_teacher_id", advisorTeacherId);
        row.put("advisor_name", advisorTeacherId == null ? null : "Teacher-" + advisorTeacherId);
        row.put("student_avatar_path", "avatar-" + studentId + ".png");
        row.put("choice1_teacher_id", choice1TeacherId);
        row.put("choice2_teacher_id", choice2TeacherId);
        row.put("choice3_teacher_id", 7L);
        row.put("file1_path", file1Path);
        row.put("file2_path", file2Path);
        row.put("file3_path", file3Path);
        return row;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castItems(Object value) {
        return (List<Map<String, Object>>) value;
    }
}
