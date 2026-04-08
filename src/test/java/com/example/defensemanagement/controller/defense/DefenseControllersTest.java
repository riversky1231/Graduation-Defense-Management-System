package com.example.defensemanagement.controller.defense;

import com.example.defensemanagement.entity.ArchiveDetail;
import com.example.defensemanagement.entity.ArchiveSession;
import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.DefenseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefenseControllersTest {

    @Mock
    private DefenseService defenseService;
    @Mock
    private DefenseGroupTeacherMapper defenseGroupTeacherMapper;
    @Mock
    private DefenseGroupMapper defenseGroupMapper;
    @Mock
    private TeacherMapper teacherMapper;
    @Mock
    private DepartmentMapper departmentMapper;

    @Test
    void indexRedirectsToLoginWhenSessionIsAnonymous() {
        DefenseHomeController controller = newHomeController();

        String view = controller.index(new ExtendedModelMap(), new MockHttpSession());

        assertEquals("redirect:/login", view);
    }

    @Test
    void indexResolvesTeacherUserAndMarksDefenseLeader() {
        DefenseHomeController controller = newHomeController();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", teacherUser(8L, "TEACHER", 2L));
        Teacher teacher = teacher(3L, 2L);
        DefenseGroupTeacher relation = new DefenseGroupTeacher();
        relation.setTeacherId(3L);
        relation.setIsLeader(1);
        DefenseGroup group = new DefenseGroup();
        group.setId(11L);
        ExtendedModelMap model = new ExtendedModelMap();

        when(teacherMapper.findByUserId(8L)).thenReturn(teacher);
        when(defenseGroupTeacherMapper.findAll()).thenReturn(List.of(relation));
        when(defenseGroupMapper.findByDepartmentId(2L)).thenReturn(List.of(group));

        String view = controller.index(model, session);

        assertEquals("index", view);
        assertSame(teacher, model.get("currentTeacher"));
        assertEquals(true, model.get("isDefenseLeader"));
        assertEquals(List.of(group), model.get("groups"));
        assertSame(teacher, session.getAttribute("currentTeacher"));
    }

    @Test
    void addGroupAutoGeneratesNameForDeptAdmin() {
        DefenseGroupAdminController controller = newAdminController();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", teacherUser(1L, "DEPT_ADMIN", 5L));
        DefenseGroupAdminController.AddGroupRequest request = new DefenseGroupAdminController.AddGroupRequest();
        request.setScore(88);
        Department department = new Department();
        department.setId(5L);
        department.setDescription("计算机");

        when(defenseGroupMapper.findByDepartmentId(5L)).thenReturn(List.of(new DefenseGroup(), new DefenseGroup()));
        when(departmentMapper.findById(5L)).thenReturn(department);
        when(defenseService.getAllGroups()).thenReturn(List.of(new DefenseGroup(), new DefenseGroup(), new DefenseGroup()));

        String result = controller.addGroup(request, session);

        assertEquals("success", result);
        ArgumentCaptor<DefenseGroup> captor = ArgumentCaptor.forClass(DefenseGroup.class);
        verify(defenseService).addGroup(captor.capture());
        DefenseGroup saved = captor.getValue();
        assertEquals("计算机第三组", saved.getName());
        assertEquals(5L, saved.getDepartmentId());
        assertEquals(88, saved.getScore());
        assertEquals(3, saved.getDisplayOrder());
    }

    @Test
    void addGroupsBatchRejectsInvalidCount() {
        DefenseGroupAdminController controller = newAdminController();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", teacherUser(1L, "SUPER_ADMIN", null));
        DefenseGroupAdminController.BatchAddGroupRequest request = new DefenseGroupAdminController.BatchAddGroupRequest();
        request.setDepartmentId(9L);
        request.setCount(0);

        String result = controller.addGroupsBatch(request, session);

        assertEquals("error:小组个数必须在1-50之间", result);
    }

    @Test
    void deleteGroupRejectsCrossDepartmentDeletionForDeptAdmin() {
        DefenseGroupAdminController controller = newAdminController();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", teacherUser(1L, "DEPT_ADMIN", 5L));
        DefenseGroup group = new DefenseGroup();
        group.setId(20L);
        group.setDepartmentId(6L);

        when(defenseService.getGroupById(20L)).thenReturn(group);

        String result = controller.deleteGroup(20L, session);

        assertEquals("error:只能删除本院系的小组", result);
    }

    @Test
    void archiveEndpointsDelegateToDefenseService() {
        DefenseArchiveController controller = newArchiveController();
        ArchiveSession archiveSession = new ArchiveSession();
        archiveSession.setId(1L);
        ArchiveDetail archiveDetail = new ArchiveDetail();

        when(defenseService.getArchiveList()).thenReturn(List.of(archiveSession));
        when(defenseService.getArchiveDetail(7L)).thenReturn(archiveDetail);

        controller.archiveCurrentSession();
        List<ArchiveSession> list = controller.getArchiveList();
        ArchiveDetail detail = controller.getArchiveDetail(7L);
        controller.deleteArchive(7L);

        verify(defenseService).archiveCurrentSession();
        verify(defenseService).deleteArchive(7L);
        assertEquals(1, list.size());
        assertSame(archiveDetail, detail);
    }

    @Test
    void groupOperationEndpointsDelegateAndPreserveLegacyUnsupportedMemberCalls() {
        DefenseGroupOperationController controller = newOperationController();
        when(defenseService.getGroupStudentInfo(9L)).thenReturn(List.of("张三-题目A"));

        List<String> members = controller.getMembers(9L);
        controller.updateOrder(List.of(1L, 2L));
        controller.updateScore(9L, 95);

        assertEquals(List.of("张三-题目A"), members);
        verify(defenseService).updateOrder(List.of(1L, 2L));
        verify(defenseService).updateScore(9L, 95);
    }

    private DefenseHomeController newHomeController() {
        return new DefenseHomeController(
                defenseService,
                defenseGroupTeacherMapper,
                defenseGroupMapper,
                teacherMapper,
                departmentMapper);
    }

    private DefenseGroupAdminController newAdminController() {
        return new DefenseGroupAdminController(
                defenseService,
                defenseGroupTeacherMapper,
                defenseGroupMapper,
                teacherMapper,
                departmentMapper);
    }

    private DefenseArchiveController newArchiveController() {
        return new DefenseArchiveController(
                defenseService,
                defenseGroupTeacherMapper,
                defenseGroupMapper,
                teacherMapper,
                departmentMapper);
    }

    private DefenseGroupOperationController newOperationController() {
        return new DefenseGroupOperationController(
                defenseService,
                defenseGroupTeacherMapper,
                defenseGroupMapper,
                teacherMapper,
                departmentMapper);
    }

    private User teacherUser(Long id, String roleName, Long departmentId) {
        User user = new User();
        user.setId(id);
        user.setDepartmentId(departmentId);
        Role role = new Role();
        role.setName(roleName);
        user.setRole(role);
        return user;
    }

    private Teacher teacher(Long id, Long departmentId) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setDepartmentId(departmentId);
        return teacher;
    }
}
