package com.example.defensemanagement.service;

import com.example.defensemanagement.common.RelevanceAnalysisResult;

import java.util.function.Consumer;

public interface AiCommentService {

    /**
     * 基于提示词与上下文生成评语。当前为占位实现，可根据实际 API 对接。
     * @param promptTemplateKey 配置中的模板 key（如 PAPER_PROMPT_TEMPLATE / DESIGN_PROMPT_TEMPLATE）
     * @param context 上下文（摘要、评分要点等）
     * @return 生成的评语
     */
    String generateComment(String promptTemplateKey, String context);

    /**
     * 流式生成评语，实时返回生成的内容片段
     * @param promptTemplateKey 配置中的模板 key
     * @param context 上下文
     * @param onChunk 接收到内容片段时的回调函数
     * @return 完整的生成评语
     */
    String generateCommentStream(String promptTemplateKey, String context, Consumer<String> onChunk);

    /**
     * 计算学生材料与教师研究方向/招生要求之间的相关度，返回 0-100 分。
     */
    default double calculateRelevanceScore(String studentContext, String teacherContext) {
        return analyzeRelevance(studentContext, teacherContext).getScore();
    }

    /**
     * 计算相关度并返回来源标记与回退原因。
     */
    RelevanceAnalysisResult analyzeRelevance(String studentContext, String teacherContext);
}

