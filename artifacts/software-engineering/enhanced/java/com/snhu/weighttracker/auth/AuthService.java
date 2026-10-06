package com.snhu.weighttracker.auth;

/*
 * ==========================================================================================
 * File:    AuthService.java
 * Author:  Jeremy White
 * Course:  CS-499 Computer Science Capstone (SNHU)
 * Date:    September 2026
 * Context: CS-499 Enhancement 1 (Software Design & Engineering). NEW service that extracts
 *          authentication out of the UI. Validates input, stores salted PBKDF2 hashes, and
 *          returns an identical failure message for unknown-user vs. wrong-password to
 *          prevent user enumeration. Depends on the UserStore interface for testability.
 *          (Course Outcomes 3, 4, 5.)
 * ==========================================================================================
 */

import com.snhu.weighttracker.data.UserRecord;
import com.snhu.weighttracker.data.UserStore;
import com.snhu.weighttracker.security.PasswordHasher;
import com.snhu.weighttracker.validation.InputValidator;

/**
 * ENHANCEMENT 1 (Software Design & Engineering) — NEW CLASS.
 *
 * AuthService encapsulates all authentication logic: registration and login.
 *
 * WHY THIS WAS ADDED:
 * In the original artifact, LoginActivity performed authentication itself by calling
 * DatabaseHelper.checkUser(username, plaintextPassword) — a plaintext comparison — and
 * mixed that security-critical logic into an Android UI class. This enhancement extracts
 * authentication into a dedicated, testable service that:
 *   - validates input first (InputValidator),
 *   - stores salted PBKDF2 hashes instead of plaintext (PasswordHasher), and
 *   - returns an identical failure message for "no such user" and "wrong password" to
 *     avoid USER ENUMERATION (Outcome 5).
 *
 * It depends on the UserStore interface, not the concrete DatabaseHelper, so it can be
 * unit-tested off-device (Outcome 4).
 */
public class AuthService {

    // ENHANCEMENT 1: single, generic failure message prevents user enumeration —
    // an attacker cannot tell whether the username or the password was wrong.
    public static final String GENERIC_LOGIN_FAILURE = "Invalid username or password";

    private final UserStore userStore;

    public AuthService(UserStore userStore) {
        this.userStore = userStore;
    }

    /** ENHANCEMENT 1: immutable result carrying success plus a user-facing message. */
    public static final class AuthResult {
        private final boolean success;
        private final String message;

        private AuthResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static AuthResult success(String message) {
            return new AuthResult(true, message);
        }

        public static AuthResult failure(String message) {
            return new AuthResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * ENHANCEMENT 1: registers a new user securely.
     * Steps: validate username + password, reject duplicates, generate a random salt,
     * hash the password with PBKDF2, and persist only the hash + salt.
     */
    public AuthResult registerUser(String username, String password) {
        InputValidator.ValidationResult userCheck = InputValidator.validateUsername(username);
        if (!userCheck.isValid()) {
            return AuthResult.failure(userCheck.getMessage());
        }
        InputValidator.ValidationResult passCheck = InputValidator.validatePassword(password);
        if (!passCheck.isValid()) {
            return AuthResult.failure(passCheck.getMessage());
        }

        String uname = username.trim();
        if (userStore.userExists(uname)) {
            return AuthResult.failure("Username already exists");
        }

        // ENHANCEMENT 1: never store plaintext — generate salt, derive hash, store both.
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(password, salt);
        boolean inserted = userStore.addUser(uname, hash, salt);
        if (inserted) {
            return AuthResult.success("Account created! You can now log in.");
        }
        return AuthResult.failure("Could not create account. Please try again.");
    }

    /**
     * ENHANCEMENT 1: authenticates a user.
     * Fetches the stored hash + salt and verifies the candidate password with a
     * constant-time PBKDF2 comparison. Returns the SAME generic failure message whether
     * the username is unknown or the password is wrong (anti-enumeration — Outcome 5).
     */
    public AuthResult login(String username, String password) {
        // Basic presence check (avoids pointless hashing on empty input).
        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {
            return AuthResult.failure(GENERIC_LOGIN_FAILURE);
        }

        UserRecord user = userStore.getUser(username.trim());
        if (user == null) {
            return AuthResult.failure(GENERIC_LOGIN_FAILURE);
        }

        boolean ok = PasswordHasher.verify(password, user.getSalt(), user.getPasswordHash());
        if (ok) {
            return AuthResult.success("Login successful");
        }
        return AuthResult.failure(GENERIC_LOGIN_FAILURE);
    }
}
