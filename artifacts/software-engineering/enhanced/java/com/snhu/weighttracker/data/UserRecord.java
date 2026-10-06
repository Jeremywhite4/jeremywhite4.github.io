package com.snhu.weighttracker.data;

/*
 * ==========================================================================================
 * File:    UserRecord.java
 * Author:  Jeremy White
 * Course:  CS-499 Computer Science Capstone (SNHU)
 * Date:    September 2026
 * Context: CS-499 Enhancement 1 (Software Design & Engineering). NEW immutable model for a
 *          stored user's credential material (username, password hash, salt), so the data
 *          layer can return credentials for hash verification instead of doing a plaintext
 *          match. (Course Outcomes 3, 5.)
 * ==========================================================================================
 */

/**
 * ENHANCEMENT 1 (Software Design & Engineering) — NEW CLASS.
 *
 * UserRecord is a small immutable data holder representing a stored user's
 * credential material (username, password hash, and salt).
 *
 * WHY THIS WAS ADDED:
 * The original artifact never modeled a user; it just ran a boolean
 * "does this username+plaintext-password row exist?" query. By returning a
 * UserRecord from the data layer, the authentication logic (AuthService) can
 * perform a proper salted-hash verification instead of a plaintext match, and
 * the data layer no longer needs to know anything about passwords.
 */
public final class UserRecord {

    private final String username;
    private final String passwordHash;
    private final String salt;

    public UserRecord(String username, String passwordHash, String salt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getSalt() {
        return salt;
    }
}
