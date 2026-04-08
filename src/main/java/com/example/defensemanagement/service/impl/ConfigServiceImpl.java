package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.EvaluationItem;
import com.example.defensemanagement.entity.SystemConfig;
import com.example.defensemanagement.mapper.EvaluationItemMapper;
import com.example.defensemanagement.mapper.SystemConfigMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConfigServiceImpl implements ConfigService {

    // 常量定义，用于 SystemConfig 的 Key
    public static final String KEY_CURRENT_YEAR = "CURRENT_DEFENSE_YEAR";
    public static final String KEY_QWEN_API_KEY = "QWEN_API_KEY";
    public static final String KEY_PAPER_PROMPT_TEMPLATE = "PAPER_PROMPT_TEMPLATE";
    public static final String KEY_DESIGN_PROMPT_TEMPLATE = "DESIGN_PROMPT_TEMPLATE";
    public static final String KEY_DEFENSE_DATE_YEAR = "DEFENSE_DATE_YEAR";
    public static final String KEY_DEFENSE_DATE_MONTH = "DEFENSE_DATE_MONTH";
    public static final String KEY_DEFENSE_DATE_DAY = "DEFENSE_DATE_DAY";
    public static final String KEY_GRADE_DATE_YEAR = "GRADE_DATE_YEAR";
    public static final String KEY_GRADE_DATE_MONTH = "GRADE_DATE_MONTH";
    public static final String KEY_GRADE_DATE_DAY = "GRADE_DATE_DAY";
    public static final String KEY_TEACHER_MAX_STUDENTS = "TEACHER_MAX_STUDENTS";
    public static final String KEY_VOLUNTEER_DEADLINE = "VOLUNTEER_DEADLINE";
    public static final String KEY_VOLUNTEER_CURRENT_ROUND = "VOLUNTEER_CURRENT_ROUND";

    @Autowired
    private EvaluationItemMapper evaluationItemMapper;

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Override
    @Transactional
    public void saveEvaluationItems(String defenseType, List<EvaluationItem> items) {
        // 1. 清除旧的配置
        evaluationItemMapper.deleteAllByType(defenseType);

        // 2. 插入新的配置
        for (int i = 0; i < items.size(); i++) {
            EvaluationItem item = items.get(i);
            item.setDefenseType(defenseType);
            item.setDisplayOrder(i + 1);
            // 计算最大分数 (权值 * 100)
            if (item.getWeight() != null) {
                item.setMaxScore((int) (item.getWeight() * 100));
            }
            evaluationItemMapper.insert(item);
        }
    }

    @Override
    public List<EvaluationItem> getEvaluationItems(String defenseType) {
        return evaluationItemMapper.findByType(defenseType);
    }

    @Override
    public Map<String, Double> getEvaluationWeights(String defenseType) {
        return getEvaluationItems(defenseType).stream()
                .collect(Collectors.toMap(EvaluationItem::getItemName, EvaluationItem::getWeight));
    }

    @Override
    public String getConfigValue(String key) {
        SystemConfig config = systemConfigMapper.findByKey(key);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    @Transactional
    public void saveConfig(String key, String value, String description) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setDescription(description);

        // 使用 Mapper 提供的 UPSERT 逻辑
        if (systemConfigMapper.insertOrUpdate(config) == 0) {
            throw new RuntimeException("配置保存失败：" + key);
        }
    }

    @Override
    public Integer getCurrentDefenseYear() {
        String yearStr = getConfigValue(KEY_CURRENT_YEAR);
        if (yearStr == null) {
            java.util.List<Integer> years = studentMapper.findAllYears();
            if (years != null && !years.isEmpty()) {
                return years.get(0);
            }
            return java.time.LocalDate.now().getYear();
        }
        try {
            return Integer.parseInt(yearStr);
        } catch (NumberFormatException e) {
            java.util.List<Integer> years = studentMapper.findAllYears();
            if (years != null && !years.isEmpty()) {
                return years.get(0);
            }
            return java.time.LocalDate.now().getYear();
        }
    }

    @Override
    @Transactional
    public void setCurrentDefenseYear(Integer year) {
        saveConfig(KEY_CURRENT_YEAR, String.valueOf(year), "当前答辩年份");
    }

    @Override
    @Transactional
    public void setDefenseDate(String dateKeyPrefix, Integer year, Integer month, Integer day) {
        // 设置日期：年
        saveConfig(dateKeyPrefix + "_YEAR", String.valueOf(year), "答辩/评定日期-年");
        // 设置日期：月
        saveConfig(dateKeyPrefix + "_MONTH", String.valueOf(month), "答辩/评定日期-月");
        // 设置日期：日
        saveConfig(dateKeyPrefix + "_DAY", String.valueOf(day), "答辩/评定日期-日");
    }

    @Override
    public String getDefenseDatePart(String dateKey) {
        return getConfigValue(dateKey);
    }

    @Override
    @Transactional
    public void setQwenApiKey(String apiKey) {
        saveConfig(KEY_QWEN_API_KEY, apiKey, "QWEN 大模型 API Key");
    }

    @Override
    @Transactional
    public void setPromptTemplate(String templateKey, String templateContent) {
        String normalizedTemplateKey = normalizeTemplateKey(templateKey);
        String configKey = resolvePromptConfigKey(normalizedTemplateKey);
        String description = "PAPER_PROMPT".equals(normalizedTemplateKey)
                ? "本科毕业论文答辩小组评语提示词模板"
                : "本科毕业设计答辩小组评语提示词模板";
        saveConfig(configKey, templateContent, description);
    }

    @Override
    public String getPromptTemplate(String templateKey) {
        String configKey = resolvePromptConfigKey(normalizeTemplateKey(templateKey));
        return getConfigValue(configKey);
    }

    @Override
    public List<Integer> getAllYears() {
        return studentMapper.findAllYears();
    }

    private String normalizeTemplateKey(String templateKey) {
        if (templateKey == null || templateKey.trim().isEmpty()) {
            throw new IllegalArgumentException("templateKey 不能为空");
        }
        return templateKey.trim().toUpperCase();
    }

    private String resolvePromptConfigKey(String templateKey) {
        switch (templateKey) {
            case "PAPER_PROMPT":
                return KEY_PAPER_PROMPT_TEMPLATE;
            case "DESIGN_PROMPT":
                return KEY_DESIGN_PROMPT_TEMPLATE;
            default:
                throw new IllegalArgumentException("非法模板类型: " + templateKey);
        }
    }
}
