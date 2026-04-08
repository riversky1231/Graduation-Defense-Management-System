package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.SystemConfig;
import com.example.defensemanagement.mapper.SystemConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ConfigServiceImplTest {

    private ConfigServiceImpl service;
    private SystemConfigMapper systemConfigMapper;

    @BeforeEach
    void setUp() {
        service = new ConfigServiceImpl();
        systemConfigMapper = mock(SystemConfigMapper.class);
        ReflectionTestUtils.setField(service, "systemConfigMapper", systemConfigMapper);
    }

    @Test
    void setPromptTemplate_usesWhitelistedPaperKeyOnly() {
        when(systemConfigMapper.insertOrUpdate(any(SystemConfig.class))).thenReturn(1);
        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);

        service.setPromptTemplate("paper_prompt", "template");

        verify(systemConfigMapper).insertOrUpdate(captor.capture());
        assertEquals(ConfigServiceImpl.KEY_PAPER_PROMPT_TEMPLATE, captor.getValue().getConfigKey());
    }

    @Test
    void setPromptTemplate_rejectsUnexpectedTemplateKey() {
        assertThrows(IllegalArgumentException.class,
                () -> service.setPromptTemplate("PAPER_PROMPT_HACK", "template"));
        verifyNoInteractions(systemConfigMapper);
    }

    @Test
    void getPromptTemplate_rejectsUnexpectedTemplateKey() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getPromptTemplate("DESIGN_PROMPT_EXTRA"));
        verifyNoInteractions(systemConfigMapper);
    }
}
