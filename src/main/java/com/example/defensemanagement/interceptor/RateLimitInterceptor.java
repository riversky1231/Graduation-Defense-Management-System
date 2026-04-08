package com.example.defensemanagement.interceptor;

import com.example.defensemanagement.util.ClientIpResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;

/**
 * Simple per-IP rate limiting for AI comment generation and document export endpoints.
 *
 * Uses Redis (StringRedisTemplate) so the counter is shared across all application instances
 * in a production deployment.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private static final String AI_MAX_REQUESTS = "5";
    private static final Duration AI_WINDOW = Duration.ofMinutes(1);

    private static final String EXPORT_MAX_REQUESTS = "10";
    private static final Duration EXPORT_WINDOW = Duration.ofMinutes(1);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ClientIpResolver clientIpResolver;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String path = request.getRequestURI();
        String ip = clientIpResolver.resolveClientIp(request);

        String limitKey;
        int maxRequests;
        Duration window;
        if (path.startsWith("/defense/comment/") || path.startsWith("/department/volunteer/match")
                || path.startsWith("/teacher/volunteer/match")) {
            limitKey = buildKey(ip, "ai");
            maxRequests = Integer.parseInt(AI_MAX_REQUESTS);
            window = AI_WINDOW;
        } else if (path.startsWith("/export/")) {
            limitKey = buildKey(ip, "export");
            maxRequests = Integer.parseInt(EXPORT_MAX_REQUESTS);
            window = EXPORT_WINDOW;
        } else {
            return true;
        }

        return checkLimit(limitKey, maxRequests, window, response, ip);
    }

    private boolean checkLimit(String key, int maxRequests, Duration window,
                               HttpServletResponse response, String ip) throws IOException {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                log.error("Rate limit storage unavailable: ip={}, key={}", ip, key);
                return denyBecauseLimiterUnavailable(response);
            }

            if (count == 1L) {
                redisTemplate.expire(key, window);
            }

            if (count > maxRequests) {
                Long ttl = redisTemplate.getExpire(key);
                long retryAfter = ttl != null && ttl > 0 ? ttl : window.getSeconds();
                log.warn("Rate limit exceeded: ip={}, key={}, count={}", ip, key, count);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                response.setIntHeader("Retry-After", (int) retryAfter);
                response.getWriter().write("{\"success\":false,\"message\":\"请求过于频繁，请稍后再试\"}");
                return false;
            }
            return true;
        } catch (RuntimeException ex) {
            log.error("Rate limiter unavailable for ip={}, key={}", ip, key, ex);
            return denyBecauseLimiterUnavailable(response);
        }
    }

    private String buildKey(String ip, String prefix) {
        return "ratelimit:" + prefix + ":" + ip;
    }

    private boolean denyBecauseLimiterUnavailable(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"服务暂不可用，请稍后再试\"}");
        return false;
    }
}
