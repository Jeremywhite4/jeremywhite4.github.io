package com.snhu.weighttracker;

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

/**
 * AddWeightActivity allows the user to record a new daily weight entry.
 * Automatically captures today's date and saves to the database.
 * Checks if the user has reached their goal weight and sends an SMS notification.
 */
public class AddWeightActivity extends AppCompatActivity {

    private EditText editWeight;
    private DatabaseHelper dbHelper;
    private String username;
    private String todayDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_weight);

        // Get username from Intent
        username = getIntent().getStringExtra("username");

        // Initialize database and UI elements
        dbHelper = new DatabaseHelper(this);
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
     * Saves the weight entry to the database.
     * After saving, checks if the user has reached their goal weight
     * and sends an SMS notification if permission was granted.
     */
    private void saveWeight() {
        String weightStr = editWeight.getText().toString().trim();

        // Validate input
        if (weightStr.isEmpty()) {
            Toast.makeText(this, "Please enter your weight", Toast.LENGTH_SHORT).show();
            return;
        }

        double weight = Double.parseDouble(weightStr);

        // Save to database
        if (dbHelper.addWeight(username, todayDate, weight)) {
            Toast.makeText(this, "Weight saved!", Toast.LENGTH_SHORT).show();

            // Check if user has reached their goal weight
            checkGoalWeight(weight);

            // Return to weight history screen
            finish();
        } else {
            Toast.makeText(this, "Error saving weight", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Compares current weight against the user's goal.
     * If the goal is reached (weight <= goal), sends an SMS notification
     * only if the user has granted SMS permission.
     */
    private void checkGoalWeight(double currentWeight) {
        double goalWeight = dbHelper.getGoalWeight(username);

        // Only check if a goal has been set (goal > 0)
        if (goalWeight > 0 && currentWeight <= goalWeight) {
            // Goal reached! Send SMS if permission granted
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                    == PackageManager.PERMISSION_GRANTED) {
                sendGoalReachedSms(currentWeight, goalWeight);
            }
            Toast.makeText(this, "Congratulations! You reached your goal weight!",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Sends an SMS notification alerting the user that they have reached
     * their goal weight. Uses the device's default SMS manager.
     */
    private void sendGoalReachedSms(double currentWeight, double goalWeight) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            String message = "Weight Tracker: Congratulations! You reached your goal weight of "
                    + goalWeight + " lbs! Current weight: " + currentWeight + " lbs.";
            // Send to the device's own number as a self-notification
            smsManager.sendTextMessage("5555555555", null, message, null, null);
        } catch (Exception e) {
            // SMS failed silently - app continues without notification
            Toast.makeText(this, "SMS notification could not be sent", Toast.LENGTH_SHORT).show();
        }
    }
}
