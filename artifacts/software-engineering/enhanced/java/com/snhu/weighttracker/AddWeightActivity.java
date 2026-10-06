package com.snhu.weighttracker;

/*
 * ==========================================================================================
 * File:    AddWeightActivity.java
 * Author:  Jeremy White
 * Course:  CS-499 Computer Science Capstone (SNHU)
 * Date:    September 2026 (original artifact: CS-360, June 2026)
 * Context: CS-499 Enhancement 1 (Software Design & Engineering). REVISED to use
 *          WeightRepository instead of a direct DatabaseHelper, and to validate weight
 *          input via InputValidator before parsing (the original called Double.parseDouble
 *          directly and could crash on bad input). (Course Outcomes 3, 5.)
 * ==========================================================================================
 */

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.snhu.weighttracker.data.WeightRepository;
import com.snhu.weighttracker.validation.InputValidator;

/**
 * AddWeightActivity allows the user to record a new daily weight entry.
 *
 * ENHANCEMENT 1 (Software Design & Engineering) — REVISED.
 * Changes from the original:
 *   1. ARCHITECTURE: uses WeightRepository instead of talking to DatabaseHelper directly.
 *   2. ROBUSTNESS: weight input is now validated via InputValidator.validateWeight()
 *      BEFORE parsing. The original called Double.parseDouble() on the raw string, which
 *      throws NumberFormatException (and crashes) on non-numeric input. (Outcomes 3, 5)
 */
public class AddWeightActivity extends AppCompatActivity {

    private EditText editWeight;
    // ENHANCEMENT 1: repository replaces the direct DatabaseHelper dependency.
    private WeightRepository repository;
    private String username;
    private String todayDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_weight);

        // Get username from Intent
        username = getIntent().getStringExtra("username");

        // ENHANCEMENT 1: obtain data access through the repository layer.
        repository = new WeightRepository(this);
        editWeight = findViewById(R.id.editWeight);
        TextView textDate = findViewById(R.id.textDate);
        Button buttonSave = findViewById(R.id.buttonSave);

        // Display today's date automatically
        todayDate = new SimpleDateFormat("MM/dd/yyyy", Locale.US).format(new Date());
        textDate.setText(todayDate);

        // Save button adds the weight entry and checks goal
        buttonSave.setOnClickListener(v -> saveWeight());
    }

    /**
     * Saves the weight entry after validating it.
     *
     * ENHANCEMENT 1: validate first, then parse. No more unguarded Double.parseDouble().
     */
    private void saveWeight() {
        String weightStr = editWeight.getText().toString();

        // ENHANCEMENT 1: centralized, safe validation (replaces the raw parse).
        InputValidator.ValidationResult result = InputValidator.validateWeight(weightStr);
        if (!result.isValid()) {
            Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        double weight = InputValidator.parseWeight(weightStr);

        // ENHANCEMENT 1: save through the repository.
        if (repository.addWeight(username, todayDate, weight)) {
            Toast.makeText(this, "Weight saved!", Toast.LENGTH_SHORT).show();
            checkGoalWeight(weight);
            finish();
        } else {
            Toast.makeText(this, "Error saving weight", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Compares current weight against the user's goal and, if reached, notifies the user
     * (and optionally sends an SMS if permission was granted).
     *
     * ENHANCEMENT 1: goal lookup now goes through the repository.
     */
    private void checkGoalWeight(double currentWeight) {
        double goalWeight = repository.getGoalWeight(username);

        if (goalWeight > 0 && currentWeight <= goalWeight) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                    == PackageManager.PERMISSION_GRANTED) {
                sendGoalReachedSms(currentWeight, goalWeight);
            }
            Toast.makeText(this, "Congratulations! You reached your goal weight!",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Sends an SMS notification alerting the user that they reached their goal weight.
     */
    private void sendGoalReachedSms(double currentWeight, double goalWeight) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            String message = "Weight Tracker: Congratulations! You reached your goal weight of "
                    + goalWeight + " lbs! Current weight: " + currentWeight + " lbs.";
            smsManager.sendTextMessage("5555555555", null, message, null, null);
        } catch (Exception e) {
            Toast.makeText(this, "SMS notification could not be sent", Toast.LENGTH_SHORT).show();
        }
    }
}
