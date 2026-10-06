package com.snhu.weighttracker.security;

/*
 * ==========================================================================================
 * File:    PasswordHasher.java
 * Author:  Jeremy White
 * Course:  CS-499 Computer Science Capstone (SNHU)
 * Date:    September 2026
 * Context: CS-499 Enhancement 1 (Software Design & Engineering). NEW file created to fix
 *          the original WeightTracker artifact's plaintext-password defect. Implements
 *          salted PBKDF2 hashing and constant-time verification (Course Outcome 5).
 * ==========================================================================================
 */

import android.util.Base64;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * ENHANCEMENT 1 (Software Design & Engineering) — NEW CLASS.
 *
 * PasswordHasher centralizes secure credential handling for the Weight Tracker app.
 *
 * WHY THIS WAS ADDED:
 * The original CS-360 artifact stored passwords in PLAINTEXT: DatabaseHelper.addUser()
 * wrote the raw password to the users table, and checkUser() compared it with a raw
 * "SELECT ... WHERE password=?" query. That is a serious security defect (Outcome 5).
 *
 * WHAT THIS PROVIDES:
 *   - Salted PBKDF2-with-HMAC-SHA256 hashing (a standard, well-founded technique).
 *   - A unique random salt per user so identical passwords do not produce identical hashes.
 *   - A constant-time comparison to avoid timing side-channel attacks during login.
 *
 * DESIGN TRADE-OFF (Outcome 3):
 * The PBKDF2 iteration count balances security against mobile CPU cost. 120,000 is a
 * reasonable 2020s-era baseline for a phone; it is exposed as a constant so it can be
 * tuned per device class without touching the hashing logic.
 */
public final class PasswordHasher {

    // ENHANCEMENT 1: tunable work factor. Higher = more resistant to brute force,
    // but slower on-device. Centralized here so the trade-off is explicit and testable.
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    private PasswordHasher() {
        // Utility class — no instances.
    }

    /**
     * ENHANCEMENT 1: generates a cryptographically secure random salt,
     * Base64-encoded for storage as TEXT in SQLite.
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        new SecureRandom().nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    /**
     * ENHANCEMENT 1: derives a PBKDF2 hash of {@code password} using {@code saltBase64}.
     * Returns the Base64-encoded hash. Never store or log the raw password.
     *
     * @param password   the plaintext password (handled in-memory only)
     * @param saltBase64 the Base64 salt produced by {@link #generateSalt()}
     * @return Base64-encoded derived key (the stored "hash")
     */
    public static String hash(String password, String saltBase64) {
        if (password == null || saltBase64 == null) {
            throw new IllegalArgumentException("password and salt must not be null");
        }
        byte[] salt = Base64.decode(saltBase64, Base64.NO_WRAP);
        // PBEKeySpec accepts a char[] so the secret can be cleared from memory afterward.
        char[] pwdChars = password.toCharArray();
        try {
            KeySpec spec = new PBEKeySpec(pwdChars, salt, ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // These indicate a platform/config problem, not user error.
            throw new IllegalStateException("Password hashing failed", e);
        } finally {
            // ENHANCEMENT 1: best-effort scrub of the plaintext from the char[].
            java.util.Arrays.fill(pwdChars, '\0');
        }
    }

    /**
     * ENHANCEMENT 1: verifies a candidate password against a stored hash/salt using a
     * constant-time comparison (mitigates timing attacks — Outcome 5).
     *
     * @param candidatePassword the password supplied at login
     * @param saltBase64        the stored salt for that user
     * @param expectedHashBase64 the stored hash for that user
     * @return true if the candidate matches
     */
    public static boolean verify(String candidatePassword, String saltBase64, String expectedHashBase64) {
        if (candidatePassword == null || saltBase64 == null || expectedHashBase64 == null) {
            return false;
        }
        String candidateHash = hash(candidatePassword, saltBase64);
        return constantTimeEquals(candidateHash, expectedHashBase64);
    }

    /**
     * ENHANCEMENT 1: length-constant string comparison. Compares every character
     * regardless of where the first mismatch occurs, so comparison time does not
     * leak how many leading characters were correct.
     */
    static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] ab = a.getBytes();
        byte[] bb = b.getBytes();
        int result = ab.length ^ bb.length;
        int max = Math.max(ab.length, bb.length);
        for (int i = 0; i < max; i++) {
            byte x = i < ab.length ? ab[i] : 0;
            byte y = i < bb.length ? bb[i] : 0;
            result |= (x ^ y);
        }
        return result == 0;
    }
}
