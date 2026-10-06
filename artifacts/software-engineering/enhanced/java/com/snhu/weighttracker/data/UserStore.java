package com.snhu.weighttracker.data;

/*
 * ==========================================================================================
 * File:    UserStore.java
 * Author:  Jeremy White
 * Course:  CS-499 Computer Science Capstone (SNHU)
 * Date:    September 2026
 * Context: CS-499 Enhancement 1 (Software Design & Engineering). NEW interface abstracting
 *          user persistence so AuthService depends on an abstraction (Dependency Inversion)
 *          and can be unit-tested with an in-memory fake. (Course Outcomes 3, 4.)
 * ==========================================================================================
 */

/**
 * ENHANCEMENT 1 (Software Design & Engineering) — NEW INTERFACE.
 *
 * UserStore abstracts the persistence operations the authentication layer needs.
 *
 * WHY THIS WAS ADDED:
 * In the original artifact, LoginActivity depended directly on the concrete
 * DatabaseHelper (a hard, untestable dependency on Android's SQLite). By defining an
 * interface, AuthService depends on an abstraction (Dependency Inversion), which:
 *   - decouples the auth logic from Android/SQLite, and
 *   - lets unit tests substitute an in-memory fake so the security logic can be tested
 *     off-device with plain JUnit (Outcome 4 — well-founded tools/techniques).
 *
 * DatabaseHelper implements this interface for production; tests provide a fake.
 */
public interface UserStore {

    /** Insert a new user with a pre-computed password hash + salt. */
    boolean addUser(String username, String passwordHash, String salt);

    /** Return the stored credential material for a username, or null if absent. */
    UserRecord getUser(String username);

    /** Return true if a user with this username already exists. */
    boolean userExists(String username);
}
