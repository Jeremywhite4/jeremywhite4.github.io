package com.snhu.weighttracker.validation;

/*
 * ==========================================================================================
 * File:    InputValidator.java
 * Author:  Jeremy White
 * Course:  CS-499 Computer Science Capstone (SNHU)
 * Date:    September 2026
 * Context: CS-499 Enhancement 1 (Software Design & Engineering). NEW file that centralizes
 *          all user-input validation (username, password, weight), replacing the original
 *          artifact's empty-checks and unguarded Double.parseDouble() calls. Supports
 *          Course Outcomes 3 (robust design) and 5 (treat input as untrusted).
 * ==========================================================================================
 */

/**
 * ENHANCEMENT 1 (Software Design & Engineering) — NEW CLASS.
 *
 * InputValidator centralizes all user-input validation for the Weight Tracker app.
 *
 * WHY THIS WAS ADDED:
 * The original CS-360 artifact only checked that fields were non-empty and then called
 * Double.parseDouble(...) directly on weight/goal input, which throws
 * NumberFormatException on bad input and could crash the app. Treating input as
 * untrusted and validating it centrally supports Outcome 5 (security mindset) and
 * Outcome 3 (robust, well-designed solution).
 *
 * DESIGN:
 * Each method returns a ValidationResult carrying a boolean plus a user-facing message,
 * so the UI/presenter layer can show consistent errors without embedding rules.
 */
public final class InputValidator {

    // ENHANCEMENT 1: explicit, centralized rules (previously there were none).
    public static final int USERNAME_MIN = 3;
    public static final int USERNAME_MAX = 30;
    public static final int PASSWORD_MIN = 8;
    public static final double WEIGHT_MIN = 1.0;      // lbs, sane lower bound
    public static final double WEIGHT_MAX = 1500.0;   // lbs, sane upper bound

    private InputValidator() {
        // Utility class — no instances.
    }

    /** ENHANCEMENT 1: simple immutable result object for validation outcomes. */
    public static final class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, "");
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * ENHANCEMENT 1: validates a username. Must be non-empty, within length bounds,
     * and restricted to a safe character set (letters, digits, underscore, dot, hyphen).
     */
    public static ValidationResult validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return ValidationResult.error("Username is required");
        }
        String u = username.trim();
        if (u.length() < USERNAME_MIN || u.length() > USERNAME_MAX) {
            return ValidationResult.error(
                    "Username must be " + USERNAME_MIN + "-" + USERNAME_MAX + " characters");
        }
        if (!u.matches("[A-Za-z0-9_.-]+")) {
            return ValidationResult.error(
                    "Username may only contain letters, numbers, and _ . -");
        }
        return ValidationResult.ok();
    }

    /**
     * ENHANCEMENT 1: validates password strength. Requires a minimum length and a mix
     * of letters and digits. Kept intentionally reasonable for a mobile app rather than
     * overly strict, balancing security against usability (Outcome 3 trade-off).
     */
    public static ValidationResult validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return ValidationResult.error("Password is required");
        }
        if (password.length() < PASSWORD_MIN) {
            return ValidationResult.error(
                    "Password must be at least " + PASSWORD_MIN + " characters");
        }
        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        if (!hasLetter || !hasDigit) {
            return ValidationResult.error("Password must contain at least one letter and one number");
        }
        return ValidationResult.ok();
    }

    /**
     * ENHANCEMENT 1: safely parses and validates a weight string. Replaces the raw
     * Double.parseDouble(...) calls in the original activities, which could throw and
     * crash the app on non-numeric input.
     *
     * @return a ValidationResult; if valid, callers can safely call parseWeight(...)
     */
    public static ValidationResult validateWeight(String weightStr) {
        if (weightStr == null || weightStr.trim().isEmpty()) {
            return ValidationResult.error("Please enter a weight");
        }
        double value;
        try {
            value = Double.parseDouble(weightStr.trim());
        } catch (NumberFormatException e) {
            return ValidationResult.error("Weight must be a number");
        }
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return ValidationResult.error("Weight must be a valid number");
        }
        if (value < WEIGHT_MIN || value > WEIGHT_MAX) {
            return ValidationResult.error(
                    "Weight must be between " + WEIGHT_MIN + " and " + WEIGHT_MAX + " lbs");
        }
        return ValidationResult.ok();
    }

    /**
     * ENHANCEMENT 1: convenience parser used only after validateWeight() returns ok().
     * Returns -1 if the string is not parseable (should not happen post-validation).
     */
    public static double parseWeight(String weightStr) {
        try {
            return Double.parseDouble(weightStr.trim());
        } catch (Exception e) {
            return -1;
        }
    }
}
