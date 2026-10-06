package com.snhu.weighttracker;

/*
 * ==========================================================================================
 * File:    GoalWeightActivity.java
 * Author:  Jeremy White
 * Course:  CS-499 Computer Science Capstone (SNHU)
 * Date:    September 2026 (original artifact: CS-360, June 2026)
 * Context: CS-499 Enhancement 1 (Software Design & Engineering). REVISED to use
 *          WeightRepository and to validate goal-weight input via InputValidator before
 *          parsing, replacing the unguarded Double.parseDouble. (Course Outcomes 3, 5.)
 * ==========================================================================================
 */

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.snhu.weighttracker.data.WeightRepository;
import com.snhu.weighttracker.validation.InputValidator;

/**
 * GoalWeightActivity allows the user to set and view their target goal weight.
 *
 * ENHANCEMENT 1 (Software Design & Engineering) — REVISED.
 * Changes from the original:
 *   1. ARCHITECTURE: uses WeightRepository instead of DatabaseHelper directly.
 *   2. ROBUSTNESS: goal input is validated via InputValidator.validateWeight() before
 *      parsing, replacing the unguarded Double.parseDouble() that could crash the app.
 */
public class GoalWeightActivity extends AppCompatActivity {

    private EditText editGoalWeight;
    private TextView textCurrentGoal;
    // ENHANCEMENT 1: repository replaces the direct DatabaseHelper dependency.
    private WeightRepository repository;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_weight);

        // Get username from Intent
        username = getIntent().getStringExtra("username");

        // ENHANCEMENT 1: obtain data access through the repository layer.
        repository = new WeightRepository(this);
        editGoalWeight = findViewById(R.id.editGoalWeight);
        textCurrentGoal = findViewById(R.id.textCurrentGoal);
        Button buttonSaveGoal = findViewById(R.id.buttonSaveGoal);

        // Display current goal weight if one exists
        displayCurrentGoal();

        buttonSaveGoal.setOnClickListener(v -> saveGoalWeight());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    /**
     * Retrieves and displays the user's current goal weight.
     * ENHANCEMENT 1: reads through the repository.
     */
    private void displayCurrentGoal() {
        double goal = repository.getGoalWeight(username);
        if (goal > 0) {
            textCurrentGoal.setText(String.format("Current Goal: %.1f lbs", goal));
        } else {
            textCurrentGoal.setText("Current Goal: Not set");
        }
    }

    /**
     * Saves the entered goal weight after validating it.
     * ENHANCEMENT 1: validate first, then parse and persist via the repository.
     */
    private void saveGoalWeight() {
        String goalStr = editGoalWeight.getText().toString();

        // ENHANCEMENT 1: safe, centralized validation (replaces the raw parse).
        InputValidator.ValidationResult result = InputValidator.validateWeight(goalStr);
        if (!result.isValid()) {
            Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        double goalWeight = InputValidator.parseWeight(goalStr);

        repository.setGoalWeight(username, goalWeight);
        Toast.makeText(this, "Goal weight saved!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
