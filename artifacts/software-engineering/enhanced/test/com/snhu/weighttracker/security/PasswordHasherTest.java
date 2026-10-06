package com.snhu.weighttracker.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * CS-499 Enhancement 1 (Software Design & Engineering) — NEW TEST CLASS.
 *
 * Author:  Jeremy White
 * Course:  CS-499 Computer Science Capstone (SNHU)
 * Date:    September 2026
 * Context: Tests for the PasswordHasher security utility added in Enhancement 1.
 *          PasswordHasher uses android.util.Base64, so these run under Robolectric
 *          (RobolectricTestRunner) which provides the Android framework classes on the
 *          JVM. They verify the core security guarantees: passwords are never stored in
 *          plaintext, each user gets a unique salt, correct passwords verify, wrong
 *          passwords are rejected, and the constant-time comparison behaves correctly
 *          (Outcome 5).
 *
 * NOTE: @Config(sdk = 34) pins Robolectric to an SDK it ships a runtime for, so the tests
 *       run even though the app targets a newer compile/target SDK.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PasswordHasherTest {

    @Test
    public void hashIsNotPlaintext() {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash("password1", salt);
        // The stored hash must not equal the raw password.
        assertNotEquals("password1", hash);
    }

    @Test
    public void saltsAreUnique() {
        String s1 = PasswordHasher.generateSalt();
        String s2 = PasswordHasher.generateSalt();
        assertNotEquals(s1, s2);
    }

    @Test
    public void samePasswordDifferentSaltsProduceDifferentHashes() {
        String s1 = PasswordHasher.generateSalt();
        String s2 = PasswordHasher.generateSalt();
        String h1 = PasswordHasher.hash("password1", s1);
        String h2 = PasswordHasher.hash("password1", s2);
        // Unique salts mean identical passwords do not collide to the same hash.
        assertNotEquals(h1, h2);
    }

    @Test
    public void correctPasswordVerifies() {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash("password1", salt);
        assertTrue(PasswordHasher.verify("password1", salt, hash));
    }

    @Test
    public void wrongPasswordDoesNotVerify() {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash("password1", salt);
        assertFalse(PasswordHasher.verify("wrongpass9", salt, hash));
    }

    @Test
    public void constantTimeEqualsMatchesEqualStrings() {
        assertTrue(PasswordHasher.constantTimeEquals("abc123", "abc123"));
    }

    @Test
    public void constantTimeEqualsRejectsDifferentStrings() {
        assertFalse(PasswordHasher.constantTimeEquals("abc123", "abc124"));
        assertFalse(PasswordHasher.constantTimeEquals("abc", "abcdef"));
        assertFalse(PasswordHasher.constantTimeEquals(null, "abc"));
    }
}
