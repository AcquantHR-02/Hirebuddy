package com.jobportal.kaamsathi.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Helpers for random one-time tokens (refresh, email verify, password reset).
 * No business logic: just "make a random token" and "hash it".
 */
public final class SecureTokens {

    private static final int TOKEN_BYTES = 32;                    // 256 bits: impossible to guess
    private static final SecureRandom RANDOM = new SecureRandom(); // crypto-strong, thread-safe

    private SecureTokens() {
        // only static methods; nobody makes an object of this class
    }

    /** New random token, URL/JSON-safe text (43 chars). Give it to the user ONCE. */
    public static String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 → 64 hex chars. Same input → same hash, so the DB row can be found by it. */
    public static String sha256(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);   // never happens: every JVM has it
        }
    }
}
