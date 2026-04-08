package com.example.defensemanagement.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks login failure counts and lockout state per IP using Redis.
 *
 * Replaces the previous in-memory ConcurrentHashMap so that rate limiting works correctly
 * across multiple application instances behind a load balancer.
 *
 * Key format: {@code login:failures:<ip>} — stores the failure count as a string.
 * TTL is set to LOCK_DURATION so expired entries auto-cleanup.
 */
@Service
public class RedisLoginFailureService {

    private static final Logger log = LoggerFactory.getLogger(RedisLoginFailureService.class);
    private static final String KEY_PREFIX = "login:failures:";
    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    @Autowired
    private StringRedisTemplate redisTemplate;
    private final Map<String, LocalFailureRecord> localFailures = new ConcurrentHashMap<>();
    private final AtomicBoolean redisFallbackLogged = new AtomicBoolean(false);

    /**
     * Returns true if the given IP is currently locked out due to too many failed attempts.
     */
    public boolean isLocked(String ip) {
        String key = KEY_PREFIX + ip;
        LocalFailureRecord localRecord = getActiveLocalRecord(key);
        if (localRecord != null) {
            return localRecord.failures >= MAX_FAILURES;
        }

        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return false;
            }
            try {
                int failures = Integer.parseInt(value);
                return failures >= MAX_FAILURES;
            } catch (NumberFormatException e) {
                return false;
            }
        } catch (RuntimeException ex) {
            logRedisFallback(ex);
            return false;
        }
    }

    /**
     * Increments the failure counter for the given IP and sets/restores TTL to LOCK_DURATION.
     */
    public void recordFailure(String ip) {
        String key = KEY_PREFIX + ip;
        if (getActiveLocalRecord(key) != null) {
            recordFailureLocally(key);
            return;
        }

        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, LOCK_DURATION);
            }
        } catch (RuntimeException ex) {
            logRedisFallback(ex);
            recordFailureLocally(key);
        }
    }

    /**
     * Removes the failure record for the given IP on a successful login.
     */
    public void clearFailure(String ip) {
        String key = KEY_PREFIX + ip;
        localFailures.remove(key);
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException ex) {
            logRedisFallback(ex);
        }
    }

    /**
     * Returns how many login attempts remain before lockout, or 0 if already locked.
     */
    public int remainingAttempts(String ip) {
        String key = KEY_PREFIX + ip;
        LocalFailureRecord localRecord = getActiveLocalRecord(key);
        if (localRecord != null) {
            return Math.max(0, MAX_FAILURES - localRecord.failures);
        }

        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return MAX_FAILURES;
            }
            try {
                int failures = Integer.parseInt(value);
                return Math.max(0, MAX_FAILURES - failures);
            } catch (NumberFormatException e) {
                return MAX_FAILURES;
            }
        } catch (RuntimeException ex) {
            logRedisFallback(ex);
            return MAX_FAILURES;
        }
    }

    private void recordFailureLocally(String key) {
        Instant now = Instant.now();
        localFailures.compute(key, (ignored, existing) -> {
            if (existing == null || existing.isExpired(now)) {
                return new LocalFailureRecord(1, now.plus(LOCK_DURATION));
            }
            return new LocalFailureRecord(existing.failures + 1, existing.expiresAt);
        });
    }

    private LocalFailureRecord getActiveLocalRecord(String key) {
        LocalFailureRecord record = localFailures.get(key);
        if (record == null) {
            return null;
        }

        if (record.isExpired(Instant.now())) {
            localFailures.remove(key, record);
            return null;
        }
        return record;
    }

    private void logRedisFallback(RuntimeException ex) {
        if (redisFallbackLogged.compareAndSet(false, true)) {
            log.warn("Redis unavailable for login failure tracking, falling back to in-memory counters", ex);
        } else {
            log.debug("Redis unavailable for login failure tracking", ex);
        }
    }

    private static final class LocalFailureRecord {
        private final int failures;
        private final Instant expiresAt;

        private LocalFailureRecord(int failures, Instant expiresAt) {
            this.failures = failures;
            this.expiresAt = expiresAt;
        }

        private boolean isExpired(Instant now) {
            return !expiresAt.isAfter(now);
        }
    }
}
