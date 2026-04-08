package com.example.defensemanagement.interceptor;

import com.example.defensemanagement.util.ClientIpResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RateLimitInterceptorTest {

    private RateLimitInterceptor interceptor;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private ClientIpResolver clientIpResolver;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        clientIpResolver = mock(ClientIpResolver.class);

        interceptor = new RateLimitInterceptor();
        ReflectionTestUtils.setField(interceptor, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(interceptor, "clientIpResolver", clientIpResolver);
    }

    private MockHttpServletRequest buildRequest(String path, String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", path);
        req.setRemoteAddr(ip);
        return req;
    }

    @Test
    void aiPath_allowsUpToLimit() throws Exception {
        when(clientIpResolver.resolveClientIp(any())).thenReturn("1.2.3.4");
        when(valueOps.increment(anyString())).thenReturn(1L).thenReturn(2L).thenReturn(3L)
                .thenReturn(4L).thenReturn(5L);

        MockHttpServletResponse resp = new MockHttpServletResponse();
        for (int i = 0; i < 5; i++) {
            assertTrue(interceptor.preHandle(buildRequest("/defense/comment/generate", "1.2.3.4"), resp, null),
                    "第 " + (i + 1) + " 次应通过");
        }
    }

    @Test
    void aiPath_blocksAfterLimit() throws Exception {
        when(clientIpResolver.resolveClientIp(any())).thenReturn("1.2.3.5");
        when(valueOps.increment(anyString())).thenReturn(6L);
        when(redisTemplate.getExpire(anyString())).thenReturn(55L);

        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean result = interceptor.preHandle(buildRequest("/defense/comment/generate", "1.2.3.5"), resp, null);
        assertFalse(result, "第6次应被限流");
        assertEquals(429, resp.getStatus());
        assertTrue(resp.getContentAsString().contains("请求过于频繁"));
    }

    @Test
    void exportPath_blocksAfterLimit() throws Exception {
        when(clientIpResolver.resolveClientIp(any())).thenReturn("2.2.2.2");
        when(valueOps.increment(anyString())).thenReturn(11L);
        when(redisTemplate.getExpire(anyString())).thenReturn(58L);

        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean result = interceptor.preHandle(buildRequest("/export/word", "2.2.2.2"), resp, null);
        assertFalse(result, "第11次应被限流");
        assertEquals(429, resp.getStatus());
    }

    @Test
    void volunteerMatchPath_usesAiLimit() throws Exception {
        when(clientIpResolver.resolveClientIp(any())).thenReturn("3.3.3.3");
        when(valueOps.increment(anyString())).thenReturn(6L);
        when(redisTemplate.getExpire(anyString())).thenReturn(59L);

        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean result = interceptor.preHandle(buildRequest("/department/volunteer/match", "3.3.3.3"), resp, null);
        assertFalse(result);
    }

    @Test
    void differentIps_areIsolated() throws Exception {
        when(clientIpResolver.resolveClientIp(any())).thenReturn("10.0.0.1");
        when(valueOps.increment(anyString())).thenReturn(6L);

        MockHttpServletResponse resp = new MockHttpServletResponse();
        interceptor.preHandle(buildRequest("/defense/comment/generate", "10.0.0.1"), resp, null);

        // 不同 IP 不受影响
        when(clientIpResolver.resolveClientIp(any())).thenReturn("10.0.0.2");
        when(valueOps.increment(anyString())).thenReturn(1L);
        MockHttpServletResponse resp2 = new MockHttpServletResponse();
        boolean result = interceptor.preHandle(buildRequest("/defense/comment/generate", "10.0.0.2"), resp2, null);
        assertTrue(result, "不同 IP 应独立计数");
    }

    @Test
    void nonLimitedPath_alwaysPasses() throws Exception {
        when(clientIpResolver.resolveClientIp(any())).thenReturn("5.5.5.5");
        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse resp = new MockHttpServletResponse();
            assertTrue(interceptor.preHandle(buildRequest("/api/student/list", "5.5.5.5"), resp, null));
        }
    }

    @Test
    void usesResolvedClientIp() throws Exception {
        // ClientIpResolver 被调用，结果用于限流 key
        when(clientIpResolver.resolveClientIp(any())).thenReturn("9.9.9.9");
        when(valueOps.increment("ratelimit:ai:9.9.9.9")).thenReturn(1L);

        MockHttpServletRequest req = buildRequest("/defense/comment/generate", "127.0.0.1");
        req.addHeader("X-Forwarded-For", "spoofed, 10.0.0.1");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        interceptor.preHandle(req, resp, null);

        verify(clientIpResolver).resolveClientIp(req);
        verify(valueOps).increment("ratelimit:ai:9.9.9.9");
    }

    @Test
    void failsClosedWhenRedisReturnsNull() throws Exception {
        when(clientIpResolver.resolveClientIp(any())).thenReturn("1.2.3.4");
        when(valueOps.increment(anyString())).thenReturn(null);

        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(buildRequest("/defense/comment/generate", "1.2.3.4"), resp, null));
        assertEquals(503, resp.getStatus());
        assertTrue(resp.getContentAsString().contains("服务暂不可用"));
    }

    @Test
    void failsClosedWhenRedisThrowsException() throws Exception {
        when(clientIpResolver.resolveClientIp(any())).thenReturn("1.2.3.4");
        when(valueOps.increment(anyString())).thenThrow(new RuntimeException("redis down"));

        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(buildRequest("/defense/comment/generate", "1.2.3.4"), resp, null));
        assertEquals(503, resp.getStatus());
    }
}
