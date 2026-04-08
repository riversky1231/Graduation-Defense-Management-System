package com.example.defensemanagement.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisLoginFailureServiceTest {

    private RedisLoginFailureService service;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        service = new RedisLoginFailureService();
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);
    }

    @Test
    void notLocked_whenKeyDoesNotExist() {
        when(valueOps.get("login:failures:1.1.1.1")).thenReturn(null);
        assertFalse(service.isLocked("1.1.1.1"));
    }

    @Test
    void locked_afterFiveFailures() {
        when(valueOps.get("login:failures:2.2.2.2")).thenReturn("5");
        assertTrue(service.isLocked("2.2.2.2"));
    }

    @Test
    void notLocked_afterFourFailures() {
        when(valueOps.get("login:failures:3.3.3.3")).thenReturn("4");
        assertFalse(service.isLocked("3.3.3.3"));
    }

    @Test
    void isLocked_handlesInvalidValue() {
        when(valueOps.get("login:failures:bad")).thenReturn("not-a-number");
        assertFalse(service.isLocked("bad"));
    }

    @Test
    void recordFailure_incrementsCounter() {
        when(valueOps.increment("login:failures:4.4.4.4")).thenReturn(1L);
        service.recordFailure("4.4.4.4");
        verify(valueOps).increment("login:failures:4.4.4.4");
    }

    @Test
    void recordFailure_setsExpiry_onFirstFailure() {
        when(valueOps.increment("login:failures:5.5.5.5")).thenReturn(1L);
        service.recordFailure("5.5.5.5");
        verify(redisTemplate).expire("login:failures:5.5.5.5", Duration.ofMinutes(15));
    }

    @Test
    void recordFailure_doesNotSetExpiry_onSubsequentFailures() {
        when(valueOps.increment("login:failures:6.6.6.6")).thenReturn(3L);
        service.recordFailure("6.6.6.6");
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void clearFailure_deletesKey() {
        service.clearFailure("7.7.7.7");
        verify(redisTemplate).delete("login:failures:7.7.7.7");
    }

    @Test
    void remainingAttempts_returnsMax_whenKeyMissing() {
        when(valueOps.get("login:failures:8.8.8.8")).thenReturn(null);
        assertEquals(5, service.remainingAttempts("8.8.8.8"));
    }

    @Test
    void remainingAttempts_decrementsAfterFailures() {
        when(valueOps.get("login:failures:9.9.9.9")).thenReturn("3");
        assertEquals(2, service.remainingAttempts("9.9.9.9"));
    }

    @Test
    void remainingAttempts_zeroWhenLocked() {
        when(valueOps.get("login:failures:10.10.10.10")).thenReturn("7");
        assertEquals(0, service.remainingAttempts("10.10.10.10"));
    }

    @Test
    void fallsBackToInMemoryCounters_whenRedisUnavailable() {
        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("redis down"));
        when(redisTemplate.delete(anyString())).thenThrow(new RedisConnectionFailureException("redis down"));

        for (int i = 0; i < 3; i++) {
            service.recordFailure("11.11.11.11");
        }

        assertFalse(service.isLocked("11.11.11.11"));
        assertEquals(2, service.remainingAttempts("11.11.11.11"));

        service.recordFailure("11.11.11.11");
        service.recordFailure("11.11.11.11");

        assertTrue(service.isLocked("11.11.11.11"));
        assertEquals(0, service.remainingAttempts("11.11.11.11"));

        service.clearFailure("11.11.11.11");

        assertFalse(service.isLocked("11.11.11.11"));
        assertEquals(5, service.remainingAttempts("11.11.11.11"));
    }
}
