package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.EvaluationItem;
import com.example.defensemanagement.service.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoreServiceImplAutoSplitTest {

    @Mock
    private ConfigService configService;

    private ScoreServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ScoreServiceImpl();
        // 用反射注入 configService（实际只用到这一个依赖）
        try {
            java.lang.reflect.Field field = ScoreServiceImpl.class.getDeclaredField("configService");
            field.setAccessible(true);
            field.set(service, configService);
        } catch (Exception e) {
            fail("Failed to inject configService mock: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    //  边界与异常
    // ─────────────────────────────────────────────

    @Test
    void nullDefenseType_throws() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.autoSplitScoreItems(null, 80)
        );
        assertTrue(ex.getMessage().contains("不能为空"));
    }

    @Test
    void emptyDefenseType_throws() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.autoSplitScoreItems("  ", 80)
        );
        assertTrue(ex.getMessage().contains("不能为空"));
    }

    @Test
    void nullTotalScore_throws() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.autoSplitScoreItems("PAPER", null)
        );
        assertTrue(ex.getMessage().contains("不能为空"));
    }

    @Test
    void negativeTotalScore_throws() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.autoSplitScoreItems("PAPER", -1)
        );
        assertTrue(ex.getMessage().contains("0-100之间"));
    }

    @Test
    void totalScoreOver100_throws() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.autoSplitScoreItems("PAPER", 101)
        );
        assertTrue(ex.getMessage().contains("0-100之间"));
    }

    @Test
    void unsupportedDefenseType_throws() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.autoSplitScoreItems("OTHER", 80)
        );
        assertTrue(ex.getMessage().contains("不支持的答辩类型"));
    }

    // ─────────────────────────────────────────────
    //  PAPER — 默认权重
    // ─────────────────────────────────────────────

    @Test
    void paper_defaultWeights_sumsToTotal() {
        when(configService.getEvaluationItems("PAPER")).thenReturn(null);

        Map<String, Integer> items = service.autoSplitScoreItems("PAPER", 80);

        int sum = items.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(80, sum, "PAPER 拆分后各项之和应等于总分");
    }

    @Test
    void paper_defaultWeights_respectsMaxBounds() {
        when(configService.getEvaluationItems("PAPER")).thenReturn(null);

        Map<String, Integer> items = service.autoSplitScoreItems("PAPER", 100);

        assertTrue(items.get("item1") <= 50, "item1 不应超过 maxScore 上限 50");
        assertTrue(items.get("item2") <= 25, "item2 不应超过 maxScore 上限 25");
        assertTrue(items.get("item3") <= 25, "item3 不应超过 maxScore 上限 25");
    }

    @Test
    void paper_defaultWeights_zeroTotal() {
        when(configService.getEvaluationItems("PAPER")).thenReturn(null);

        Map<String, Integer> items = service.autoSplitScoreItems("PAPER", 0);

        assertEquals(0, items.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(3, items.size());
    }

    @Test
    void paper_defaultWeights_allItemsPresent() {
        when(configService.getEvaluationItems("PAPER")).thenReturn(null);

        Map<String, Integer> items = service.autoSplitScoreItems("PAPER", 50);

        assertTrue(items.containsKey("item1"));
        assertTrue(items.containsKey("item2"));
        assertTrue(items.containsKey("item3"));
    }

    // ─────────────────────────────────────────────
    //  PAPER — 配置权重
    // ─────────────────────────────────────────────

    @Test
    void paper_configWeights_sumsToTotal() {
        // 配置：权重 0.6 / 0.3 / 0.1，对应上限 60 / 30 / 10
        List<EvaluationItem> cfg = Arrays.asList(
                item(0.6, 60, 1),
                item(0.3, 30, 2),
                item(0.1, 10, 3)
        );
        when(configService.getEvaluationItems("PAPER")).thenReturn(cfg);

        Map<String, Integer> items = service.autoSplitScoreItems("PAPER", 100);

        int sum = items.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(100, sum);
        assertTrue(items.get("item1") <= 60);
        assertTrue(items.get("item2") <= 30);
        assertTrue(items.get("item3") <= 10);
    }

    @Test
    void paper_partialConfig_fallsBackToDefaults() {
        // 只配了 2 项，不足 3 项，应使用默认权重
        List<EvaluationItem> cfg = Arrays.asList(
                item(0.6, 60, 1),
                item(0.4, 40, 2)
        );
        when(configService.getEvaluationItems("PAPER")).thenReturn(cfg);

        Map<String, Integer> items = service.autoSplitScoreItems("PAPER", 80);

        int sum = items.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(80, sum);
    }

    @Test
    void paper_zeroWeightConfig_fallsBackToDefaults() {
        // 所有权重为 0，应回退到默认
        List<EvaluationItem> cfg = Arrays.asList(
                item(0.0, 0, 1),
                item(0.0, 0, 2),
                item(0.0, 0, 3)
        );
        when(configService.getEvaluationItems("PAPER")).thenReturn(cfg);

        Map<String, Integer> items = service.autoSplitScoreItems("PAPER", 80);

        int sum = items.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(80, sum);
    }

    // ─────────────────────────────────────────────
    //  DESIGN — 默认权重
    // ─────────────────────────────────────────────

    @Test
    void design_defaultWeights_sumsToTotal() {
        when(configService.getEvaluationItems("DESIGN")).thenReturn(null);

        Map<String, Integer> items = service.autoSplitScoreItems("DESIGN", 85);

        int sum = items.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(85, sum, "DESIGN 拆分后各项之和应等于总分");
    }

    @Test
    void design_defaultWeights_respectsMaxBounds() {
        when(configService.getEvaluationItems("DESIGN")).thenReturn(null);

        Map<String, Integer> items = service.autoSplitScoreItems("DESIGN", 100);

        assertTrue(items.get("item1") <= 15);
        assertTrue(items.get("item2") <= 15);
        assertTrue(items.get("item3") <= 15);
        assertTrue(items.get("item4") <= 25);
        assertTrue(items.get("item5") <= 15);
        assertTrue(items.get("item6") <= 15);
    }

    @Test
    void design_defaultWeights_allItemsPresent() {
        when(configService.getEvaluationItems("DESIGN")).thenReturn(null);

        Map<String, Integer> items = service.autoSplitScoreItems("DESIGN", 85);

        assertEquals(6, items.size());
        assertTrue(items.containsKey("item1"));
        assertTrue(items.containsKey("item4")); // item4 上限最高
    }

    // ─────────────────────────────────────────────
    //  DESIGN — 配置权重
    // ─────────────────────────────────────────────

    @Test
    void design_configWeights_sumsToTotal() {
        List<EvaluationItem> cfg = Arrays.asList(
                item(0.1, 10, 1),
                item(0.1, 10, 2),
                item(0.1, 10, 3),
                item(0.3, 30, 4),
                item(0.2, 20, 5),
                item(0.2, 20, 6)
        );
        when(configService.getEvaluationItems("DESIGN")).thenReturn(cfg);

        Map<String, Integer> items = service.autoSplitScoreItems("DESIGN", 100);

        int sum = items.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(100, sum);
    }

    @Test
    void design_partialConfig_fallsBackToDefaults() {
        // 只有 4 项，不足 6 项，应回退默认
        List<EvaluationItem> cfg = Arrays.asList(
                item(0.2, 20, 1),
                item(0.2, 20, 2),
                item(0.2, 20, 3),
                item(0.4, 40, 4)
        );
        when(configService.getEvaluationItems("DESIGN")).thenReturn(cfg);

        Map<String, Integer> items = service.autoSplitScoreItems("DESIGN", 85);

        int sum = items.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(85, sum);
    }

    // ─────────────────────────────────────────────
    //  总分超出配置上限
    // ─────────────────────────────────────────────

    @Test
    void paper_totalExceedsConfigMax_throws() {
        // 配置上限之和为 60+30+10=100，与默认值相同，这里用较小的配置
        List<EvaluationItem> cfg = Arrays.asList(
                item(0.5, 30, 1),
                item(0.25, 15, 2),
                item(0.25, 15, 3)
        );
        when(configService.getEvaluationItems("PAPER")).thenReturn(cfg);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.autoSplitScoreItems("PAPER", 61)
        );
        assertTrue(ex.getMessage().contains("总分超过分项上限之和"));
    }

    // ─────────────────────────────────────────────
    //  差值调整（diff > 0 / diff < 0）
    // ─────────────────────────────────────────────

    @Test
    void paper_roundingGap_filledByDiffPositive() {
        // 总分 88，加权乘积往往有小数，diff 补偿应生效
        when(configService.getEvaluationItems("PAPER")).thenReturn(null);
        Map<String, Integer> items = service.autoSplitScoreItems("PAPER", 88);
        assertEquals(88, items.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void paper_roundingGap_filledByDiffNegative() {
        // 低分边界，确保 diff 负向补偿也生效
        when(configService.getEvaluationItems("PAPER")).thenReturn(null);
        Map<String, Integer> items = service.autoSplitScoreItems("PAPER", 3);
        assertEquals(3, items.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void design_roundingGap_fullyAdjusted() {
        when(configService.getEvaluationItems("DESIGN")).thenReturn(null);
        Map<String, Integer> items = service.autoSplitScoreItems("DESIGN", 77);
        assertEquals(77, items.values().stream().mapToInt(Integer::intValue).sum());
    }

    // ─────────────────────────────────────────────
    //  大小写不敏感
    // ─────────────────────────────────────────────

    @Test
    void defenseType_caseInsensitive() {
        when(configService.getEvaluationItems("PAPER")).thenReturn(null);

        Map<String, Integer> upper = service.autoSplitScoreItems("PAPER", 80);
        Map<String, Integer> lower = service.autoSplitScoreItems("paper", 80);

        int sumUpper = upper.values().stream().mapToInt(Integer::intValue).sum();
        int sumLower = lower.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(sumUpper, sumLower);
        verify(configService, times(2)).getEvaluationItems("PAPER");
    }

    // ─────────────────────────────────────────────
    //  Helper
    // ─────────────────────────────────────────────

    private EvaluationItem item(double weight, int maxScore, int order) {
        EvaluationItem it = new EvaluationItem();
        it.setWeight(weight);
        it.setMaxScore(maxScore);
        it.setDisplayOrder(order);
        return it;
    }
}
