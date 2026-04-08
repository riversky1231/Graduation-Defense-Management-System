package com.example.defensemanagement.interceptor.auth;

public final class AccessDecision {

    private static final AccessDecision ABSTAIN = new AccessDecision(false, false, 200, "");
    private static final AccessDecision ALLOW = new AccessDecision(true, true, 200, "");

    private final boolean matched;
    private final boolean allowed;
    private final int statusCode;
    private final String message;

    private AccessDecision(boolean matched, boolean allowed, int statusCode, String message) {
        this.matched = matched;
        this.allowed = allowed;
        this.statusCode = statusCode;
        this.message = message;
    }

    public static AccessDecision abstain() {
        return ABSTAIN;
    }

    public static AccessDecision allow() {
        return ALLOW;
    }

    public static AccessDecision deny(int statusCode, String message) {
        return new AccessDecision(true, false, statusCode, message);
    }

    public boolean isMatched() {
        return matched;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }
}
