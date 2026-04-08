package com.example.defensemanagement.controller.config;

import com.example.defensemanagement.entity.EvaluationItem;
import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.impl.ConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigControllersTest {

    @Mock
    private ConfigService configService;

    private ConfigEvaluationController evaluationController;
    private ConfigTimelineController timelineController;
    private ConfigVolunteerController volunteerController;
    private ConfigLargeGroupController largeGroupController;
    private ConfigAiController aiController;

    @BeforeEach
    void setUp() {
        evaluationController = new ConfigEvaluationController(configService);
        timelineController = new ConfigTimelineController(configService);
        volunteerController = new ConfigVolunteerController(configService);
        largeGroupController = new ConfigLargeGroupController(configService);
        aiController = new ConfigAiController(configService);
    }

    @Test
    void evaluationListRejectsNonAdmin() {
        MockHttpSession session = new MockHttpSession();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> evaluationController.getEvaluationItems("paper", session));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void saveEvaluationItemsUppercasesTypeForAdmin() {
        MockHttpSession session = adminSession("SUPER_ADMIN");
        List<EvaluationItem> items = List.of(new EvaluationItem());

        String result = evaluationController.saveEvaluationItems("paper", items, session);

        assertEquals("success", result);
        verify(configService).saveEvaluationItems("PAPER", items);
    }

    @Test
    void setCurrentDefenseYearRejectsOutOfRangeValue() {
        MockHttpSession session = adminSession("DEPT_ADMIN");

        String result = timelineController.setCurrentDefenseYear(1999, session);

        assertEquals("error:年份必须在 2000-2100 之间", result);
    }

    @Test
    void setDefenseDateRejectsIllegalPrefix() {
        MockHttpSession session = adminSession("SUPER_ADMIN");

        String result = timelineController.setDefenseDate(Map.of(
                "dateKeyPrefix", "INVALID_DATE",
                "year", 2026,
                "month", 6,
                "day", 10), session);

        assertEquals("error:非法日期配置项", result);
    }

    @Test
    void volunteerConfigReturnsConfiguredValuesForAdmin() {
        MockHttpSession session = adminSession("DEPT_ADMIN");
        when(configService.getConfigValue(ConfigServiceImpl.KEY_TEACHER_MAX_STUDENTS)).thenReturn("6");
        when(configService.getConfigValue(ConfigServiceImpl.KEY_VOLUNTEER_DEADLINE)).thenReturn("2026-06-10 18:00");
        when(configService.getConfigValue(ConfigServiceImpl.KEY_VOLUNTEER_CURRENT_ROUND)).thenReturn("2");

        Map<String, Object> result = volunteerController.getVolunteerConfig(session);

        assertEquals("6", result.get("maxStudents"));
        assertEquals("2026-06-10 18:00", result.get("deadline"));
        assertEquals("2", result.get("currentRound"));
    }

    @Test
    void archiveLargeGroupStoresArchiveFlag() {
        MockHttpSession session = adminSession("SUPER_ADMIN");

        String result = largeGroupController.archiveLargeGroup(session);

        assertEquals("success", result);
        verify(configService).saveConfig("LARGE_GROUP_ARCHIVED", "1", "大组成绩是否已归档(0未归档/1已归档)");
    }

    @Test
    void getQwenApiKeyMasksSecret() {
        MockHttpSession session = adminSession("SUPER_ADMIN");
        when(configService.getConfigValue(ConfigServiceImpl.KEY_QWEN_API_KEY)).thenReturn("sk-test-1234");

        String result = aiController.getQwenApiKey(session);

        assertEquals("****1234", result);
    }

    @Test
    void setPromptTemplateRejectsIllegalTemplateKey() {
        MockHttpSession session = adminSession("SUPER_ADMIN");

        String result = aiController.setPromptTemplate(Map.of(
                "templateKey", "OTHER_PROMPT",
                "templateContent", "content"), session);

        assertEquals("error:非法模板类型", result);
    }

    private MockHttpSession adminSession(String roleName) {
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        Role role = new Role();
        role.setName(roleName);
        user.setRole(role);
        session.setAttribute("currentUser", user);
        return session;
    }
}
