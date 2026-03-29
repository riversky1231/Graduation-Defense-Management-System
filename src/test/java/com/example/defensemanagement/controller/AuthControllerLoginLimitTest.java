package com.example.defensemanagement.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class AuthControllerLoginLimitTest {

    private AuthController controller;
    private ConcurrentHashMap<String, long[]> loginFailures;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        controller = new AuthController();
        loginFailures = (ConcurrentHashMap<String, long[]>)
                ReflectionTestUtils.getField(controller, "loginFailures");
    }

    private boolean isLocked(String ip) {
        return (boolean) ReflectionTestUtils.invokeMethod(controller, "isLocked", ip);
    }

    private void recordFailure(String ip) {
        ReflectionTestUtils.invokeMethod(controller, "recordFailure", ip);
    }

    private void clearFailure(String ip) {
        ReflectionTestUtils.invokeMethod(controller, "clearFailure", ip);
    }

    @Test
    void notLocked_initially() {
        assertFalse(isLocked("1.1.1.1"), "初始状态不应被锁定");
    }

    @Test
    void lockedAfterFiveFailures() {
        for (int i = 0; i < 5; i++) {
            recordFailure("2.2.2.2");
        }
        assertTrue(isLocked("2.2.2.2"), "5次失败后应被锁定");
    }

    @Test
    void notLockedAfterFourFailures() {
        for (int i = 0; i < 4; i++) {
            recordFailure("3.3.3.3");
        }
        assertFalse(isLocked("3.3.3.3"), "4次失败不应被锁定");
    }

    @Test
    void clearFailure_removesLock() {
        for (int i = 0; i < 5; i++) {
            recordFailure("4.4.4.4");
        }
        assertTrue(isLocked("4.4.4.4"));
        clearFailure("4.4.4.4");
        assertFalse(isLocked("4.4.4.4"), "清除后不应被锁定");
    }

    @Test
    void expiredLock_isAutoCleared() {
        // 手动注入一条已过期的锁定记录（首次失败时间设为16分钟前）
        long expiredTime = System.currentTimeMillis() - (16 * 60 * 1000L);
        loginFailures.put("5.5.5.5", new long[]{5, expiredTime});
        assertFalse(isLocked("5.5.5.5"), "过期锁定应自动解除");
        assertNull(loginFailures.get("5.5.5.5"), "过期记录应被清除");
    }

    @Test
    void differentIps_areIndependent() {
        for (int i = 0; i < 5; i++) {
            recordFailure("6.6.6.6");
        }
        assertTrue(isLocked("6.6.6.6"));
        assertFalse(isLocked("7.7.7.7"), "不同 IP 应独立计数");
    }
}

