package com.example.defensemanagement.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitInterceptorTest {

    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor();
    }

    private MockHttpServletRequest buildRequest(String path, String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", path);
        req.setRemoteAddr(ip);
        return req;
    }

    @Test
    void aiPath_allowsUpToLimit() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();
        for (int i = 0; i < 5; i++) {
            assertTrue(interceptor.preHandle(buildRequest("/defense/comment/generate", "1.2.3.4"), resp, null),
                    "第 " + (i + 1) + " 次应通过");
        }
    }

    @Test
    void aiPath_blocksAfterLimit() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();
        for (int i = 0; i < 5; i++) {
            interceptor.preHandle(buildRequest("/defense/comment/generate", "1.2.3.5"), resp, null);
        }
        MockHttpServletResponse resp6 = new MockHttpServletResponse();
        boolean result = interceptor.preHandle(buildRequest("/defense/comment/generate", "1.2.3.5"), resp6, null);
        assertFalse(result, "第6次应被限流");
        assertEquals(429, resp6.getStatus());
    }

    @Test
    void exportPath_blocksAfterLimit() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();
        for (int i = 0; i < 10; i++) {
            interceptor.preHandle(buildRequest("/export/word", "2.2.2.2"), resp, null);
        }
        MockHttpServletResponse resp11 = new MockHttpServletResponse();
        boolean result = interceptor.preHandle(buildRequest("/export/word", "2.2.2.2"), resp11, null);
        assertFalse(result, "第11次应被限流");
        assertEquals(429, resp11.getStatus());
    }

    @Test
    void volunteerMatchPath_usesAiLimit() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();
        for (int i = 0; i < 5; i++) {
            interceptor.preHandle(buildRequest("/department/volunteer/match", "3.3.3.3"), resp, null);
        }
        MockHttpServletResponse resp6 = new MockHttpServletResponse();
        boolean result = interceptor.preHandle(buildRequest("/department/volunteer/match", "3.3.3.3"), resp6, null);
        assertFalse(result);
    }

    @Test
    void differentIps_areIsolated() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();
        for (int i = 0; i < 5; i++) {
            interceptor.preHandle(buildRequest("/defense/comment/generate", "10.0.0.1"), resp, null);
        }
        // 不同 IP 不受影响
        MockHttpServletResponse resp2 = new MockHttpServletResponse();
        boolean result = interceptor.preHandle(buildRequest("/defense/comment/generate", "10.0.0.2"), resp2, null);
        assertTrue(result, "不同 IP 应独立计数");
    }

    @Test
    void nonLimitedPath_alwaysPasses() throws Exception {
        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse resp = new MockHttpServletResponse();
            assertTrue(interceptor.preHandle(buildRequest("/api/student/list", "5.5.5.5"), resp, null));
        }
    }

    @Test
    void xForwardedFor_usedAsIp() throws Exception {
        MockHttpServletRequest req = buildRequest("/defense/comment/generate", "127.0.0.1");
        req.addHeader("X-Forwarded-For", "9.9.9.9, 10.0.0.1");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(req, resp, null));
    }
}

