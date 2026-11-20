package com.jobportal.kaamsathi.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

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

    /** New 6-digit OTP, e.g. "048213" (leading zeros kept). 1 in a million to guess, so callers must limit tries. */
    public static String newOtp() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));   // 000000 … 999999
    }

    /**
     * Hash of an OTP, mixed with its row id. Two users can get the same 6 digits;
     * mixing in the unique row id gives different hashes, so the UNIQUE token_hash column never clashes.
     */
    public static String otpHash(UUID rowId, String otp) {
        return sha256(rowId + ":" + otp);
    }

    /** SHA-256 → 64 hex chars. Same input → same hash, so the DB row can be found by it. */
    public static String sha256(String token) {
        return sha256Hex(token.getBytes(StandardCharsets.UTF_8));
    }

    /** SHA-256 of raw bytes (e.g. an uploaded file) → 64 hex chars. */
    public static String sha256Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);   // never happens: every JVM has it
        }
    }
}
