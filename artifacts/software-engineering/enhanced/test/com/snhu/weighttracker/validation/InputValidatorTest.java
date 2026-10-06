package com.snhu.weighttracker.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * CS-499 Enhancement 1 (Software Design & Engineering) — NEW TEST CLASS.
 *
 * Author:  Jeremy White
 * Course:  CS-499 Computer Science Capstone (SNHU)
 * Date:    September 2026
 * Context: Unit tests for the InputValidator utility added in Enhancement 1. These are
 *          plain JUnit tests (no Android dependency) that verify the centralized
 *          validation rules for username, password, and weight input. They demonstrate
 *          test automation (Outcome 4) and evidence the robustness/security work
 *          (Outcomes 3 and 5) rather than merely asserting it.
 */
public class InputValidatorTest {

    // ---- Username ----

    @Test
    public void validUsernameAccepted() {
        assertTrue(InputValidator.validateUsername("jeremy_w").isValid());
        assertTrue(InputValidator.validateUsername("user.name-1").isValid());
    }

    @Test
    public void emptyUsernameRejected() {
        assertFalse(InputValidator.validateUsername("").isValid());
        assertFalse(InputValidator.validateUsername("   ").isValid());
        assertFalse(InputValidator.validateUsername(null).isValid());
    }

    @Test
    public void shortUsernameRejected() {
        assertFalse(InputValidator.validateUsername("ab").isValid());
    }

    @Test
    public void usernameWithIllegalCharsRejected() {
        assertFalse(InputValidator.validateUsername("bad name").isValid());
        assertFalse(InputValidator.validateUsername("drop;table").isValid());
    }

    // ---- Password ----

    @Test
    public void strongPasswordAccepted() {
        assertTrue(InputValidator.validatePassword("password1").isValid());
    }

    @Test
    public void shortPasswordRejected() {
        assertFalse(InputValidator.validatePassword("pw1").isValid());
    }

    @Test
    public void passwordWithoutDigitRejected() {
        assertFalse(InputValidator.validatePassword("passwordonly").isValid());
    }

    @Test
    public void passwordWithoutLetterRejected() {
        assertFalse(InputValidator.validatePassword("12345678").isValid());
    }

    @Test
    public void nullPasswordRejected() {
        assertFalse(InputValidator.validatePassword(null).isValid());
    }

    // ---- Weight ----

    @Test
    public void validWeightAccepted() {
        assertTrue(InputValidator.validateWeight("180.5").isValid());
        assertEquals(180.5, InputValidator.parseWeight("180.5"), 0.0001);
    }

    @Test
    public void nonNumericWeightRejectedWithoutCrashing() {
        // The original app called Double.parseDouble() directly and would crash here.
        assertFalse(InputValidator.validateWeight("abc").isValid());
    }

    @Test
    public void emptyWeightRejected() {
        assertFalse(InputValidator.validateWeight("").isValid());
        assertFalse(InputValidator.validateWeight(null).isValid());
    }

    @Test
    public void outOfRangeWeightRejected() {
        assertFalse(InputValidator.validateWeight("0").isValid());
        assertFalse(InputValidator.validateWeight("5000").isValid());
        assertFalse(InputValidator.validateWeight("-10").isValid());
    }
}
