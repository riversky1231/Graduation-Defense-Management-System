package com.example.defensemanagement.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Shared password policy and default-password detection helpers.
 */
public final class PasswordSecurityUtils {

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 128;
    public static final String FORCE_PASSWORD_CHANGE_SESSION_KEY = "forcePasswordChange";

    // Shared seed hash used by bootstrap data.sql accounts for the weak default password.
    private static final String DEFAULT_SEED_PASSWORD_HASH =
            "$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba";

    private PasswordSecurityUtils() {
    }

    public static boolean isPasswordLengthAllowed(String password) {
        return password != null && !password.isBlank() && password.length() <= MAX_PASSWORD_LENGTH;
    }

    public static boolean isNewPasswordLengthValid(String password) {
        return isPasswordLengthAllowed(password) && password.length() >= MIN_PASSWORD_LENGTH;
    }

    public static boolean isDefaultSeedPasswordHash(String encodedPassword) {
        return constantTimeEquals(DEFAULT_SEED_PASSWORD_HASH, encodedPassword);
    }

    public static boolean isIdentifierDefaultPassword(String rawPassword, String identifier) {
        return constantTimeEquals(rawPassword, identifier);
    }

    public static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }
}
