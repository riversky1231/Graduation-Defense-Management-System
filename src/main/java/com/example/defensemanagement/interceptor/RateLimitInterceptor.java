package com.example.defensemanagement.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对 AI 评语生成和文档导出接口进行简单限流。
 * 每个 IP 在滑动窗口内最多允许 N 次请求。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    /** AI 评语接口：每分钟最多 5 次 */
    private static final int AI_MAX_REQUESTS = 5;
    private static final long AI_WINDOW_MS = 60_000L;

    /** 导出接口：每分钟最多 10 次 */
    private static final int EXPORT_MAX_REQUESTS = 10;
    private static final long EXPORT_WINDOW_MS = 60_000L;

    /** key = "ip:path_prefix", value = [请求次数, 窗口开始时间] */
    private final ConcurrentHashMap<String, long[]> counters = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String path = request.getRequestURI();
        String ip = getClientIp(request);

        if (path.startsWith("/defense/comment/generate") || path.startsWith("/defense/comment/stream")) {
            return checkLimit(ip, "ai", AI_MAX_REQUESTS, AI_WINDOW_MS, response);
        }
        if (path.startsWith("/department/volunteer/match") || path.startsWith("/teacher/volunteer/match")) {
            return checkLimit(ip, "ai", AI_MAX_REQUESTS, AI_WINDOW_MS, response);
        }
        if (path.startsWith("/export/")) {
            return checkLimit(ip, "export", EXPORT_MAX_REQUESTS, EXPORT_WINDOW_MS, response);
        }
        return true;
    }

    private boolean checkLimit(String ip, String prefix, int maxRequests, long windowMs,
                                HttpServletResponse response) throws IOException {
        String key = ip + ":" + prefix;
        long now = Instant.now().toEpochMilli();

        long[] record = counters.compute(key, (k, v) -> {
            if (v == null || now - v[1] > windowMs) {
                return new long[]{1, now};
            }
            v[0]++;
            return v;
        });

        if (record[0] > maxRequests) {
            log.warn("Rate limit exceeded: ip={}, prefix={}, count={}", ip, prefix, record[0]);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"请求过于频繁，请稍后再试\"}");
            return false;
        }
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

