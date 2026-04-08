package com.example.defensemanagement.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

/**
 * Resolves the real client IP address, respecting a configurable trusted proxy whitelist.
 *
 * Without trusted-proxy validation, reading X-Forwarded-For directly allows any attacker
 * to spoof their IP address and bypass IP-based rate limiting or login lockout. This resolver
 * only trusts the leftmost X-Forwarded-For address when the immediate upstream (first hop
 * proxy) is in the trusted list.
 */
@Component
public class ClientIpResolver {

    @Value("${app.security.trusted-proxies:}")
    private Set<String> trustedProxies;

    /**
     * Returns the real client IP, falling back to {@code request.getRemoteAddr()} when
     * no trusted proxy is detected or X-Forwarded-For is absent / malformed.
     */
    public String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        String xff = request.getHeader("X-Forwarded-For");
        if (xff == null || xff.isBlank()) {
            return remoteAddr;
        }

        String immediateProxy = xff.split(",")[0].trim();

        if (isTrustedProxy(remoteAddr)) {
            // immediateProxy is the real client IP (set by our trusted reverse proxy)
            return immediateProxy;
        }

        // Unknown / untrusted proxy — ignore X-Forwarded-For to prevent spoofing
        return remoteAddr;
    }

    private boolean isTrustedProxy(String ip) {
        return trustedProxies != null && trustedProxies.contains(ip);
    }
}
