package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.common.RelevanceAnalysisResult;
import com.example.defensemanagement.service.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiCommentServiceImplTest {

    @Mock
    private ConfigService configService;

    private AiCommentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiCommentServiceImpl();
        ReflectionTestUtils.setField(service, "configService", configService);
    }

    @Test
    void calculateRelevanceScoreFallsBackToKeywordMatchingWhenApiKeyMissing() {
        when(configService.getConfigValue("QWEN_API_KEY")).thenReturn(null);

        double score = service.calculateRelevanceScore("机器学习 图像分割", "机器学习,数据挖掘");

        assertEquals(50.0, score);
    }

    @Test
    void analyzeRelevanceMarksFallbackSourceWhenApiKeyMissing() {
        when(configService.getConfigValue("QWEN_API_KEY")).thenReturn(null);

        RelevanceAnalysisResult result = service.analyzeRelevance("机器学习 图像分割", "机器学习,数据挖掘");

        assertEquals(50.0, result.getScore());
        assertEquals(RelevanceAnalysisResult.SOURCE_FALLBACK, result.getSource());
        assertEquals("api_key_missing", result.getReason());
    }

    @Test
    void analyzeRelevanceMarksEmptyInputSource() {
        RelevanceAnalysisResult result = service.analyzeRelevance("", "机器学习");

        assertEquals(0.0, result.getScore());
        assertEquals(RelevanceAnalysisResult.SOURCE_EMPTY_INPUT, result.getSource());
        assertEquals("input_missing", result.getReason());
    }

    @Test
    void generateCommentReturnsHelpfulMessageWhenTemplateMissing() {
        when(configService.getPromptTemplate("PAPER_PROMPT_TEMPLATE")).thenReturn(null);

        String result = service.generateComment("PAPER_PROMPT_TEMPLATE", "测试上下文");

        assertEquals("【提示】尚未配置评语提示词模板，无法调用大模型。", result);
    }

    @Test
    void generateCommentReturnsHelpfulMessageWhenApiKeyMissing() {
        when(configService.getPromptTemplate("PAPER_PROMPT_TEMPLATE")).thenReturn("请生成评语");
        when(configService.getConfigValue("QWEN_API_KEY")).thenReturn("");

        String result = service.generateComment("PAPER_PROMPT_TEMPLATE", "测试上下文");

        assertEquals("【提示】尚未配置大模型 API Key，无法调用大模型。", result);
    }
}
