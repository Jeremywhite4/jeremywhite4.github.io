package com.snhu.weighttracker;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * GoalWeightActivity allows the user to set and view their target goal weight.
 * The goal is stored in the users table and used by AddWeightActivity
 * to trigger SMS notifications when the goal is reached.
 */
public class GoalWeightActivity extends AppCompatActivity {

    private EditText editGoalWeight;
    private TextView textCurrentGoal;
    private DatabaseHelper dbHelper;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_weight);

        // Get username from Intent
        username = getIntent().getStringExtra("username");

        // Initialize database and UI elements
        dbHelper = new DatabaseHelper(this);
        editGoalWeight = findViewById(R.id.editGoalWeight);
        textCurrentGoal = findViewById(R.id.textCurrentGoal);
        Button buttonSaveGoal = findViewById(R.id.buttonSaveGoal);

        // Display current goal weight if one exists
        displayCurrentGoal();

        // Save goal button updates the goal in the database and returns to history
        buttonSaveGoal.setOnClickListener(v -> saveGoalWeight());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    /**
     * Retrieves and displays the user's current goal weight from the database.
     */
    private void displayCurrentGoal() {
        double goal = dbHelper.getGoalWeight(username);
        if (goal > 0) {
            textCurrentGoal.setText(String.format("Current Goal: %.1f lbs", goal));
        } else {
            textCurrentGoal.setText("Current Goal: Not set");
        }
    }

    /**
     * Saves the entered goal weight to the database and updates the display.
     */
    private void saveGoalWeight() {
        String goalStr = editGoalWeight.getText().toString().trim();

        // Validate input
        if (goalStr.isEmpty()) {
            Toast.makeText(this, "Please enter a goal weight", Toast.LENGTH_SHORT).show();
            return;
        }

        double goalWeight = Double.parseDouble(goalStr);

        // Save goal to database
        dbHelper.setGoalWeight(username, goalWeight);
        Toast.makeText(this, "Goal weight saved!", Toast.LENGTH_SHORT).show();

        // Return to weight history screen
        finish();
    }
}
