package com.snhu.weighttracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * WeightHistoryActivity displays all weight entries in a scrollable grid.
 * Supports CRUD operations: view (Read), navigate to add (Create),
 * delete entries (Delete), and navigates to AddWeight for updates (Update).
 * Also handles SMS permission requests on first load.
 */
public class WeightHistoryActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 100;
    private LinearLayout layoutWeightGrid;
    private DatabaseHelper dbHelper;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight_history);

        // Get logged-in username from Intent
        username = getIntent().getStringExtra("username");

        // Initialize database and UI references
        dbHelper = new DatabaseHelper(this);
        layoutWeightGrid = findViewById(R.id.layoutWeightGrid);
        Button buttonAddWeight = findViewById(R.id.buttonAddWeight);
        Button buttonGoalWeight = findViewById(R.id.buttonGoalWeight);

        // Request SMS permission for goal notifications
        checkSmsPermission();

        // Navigate to Add Weight screen
        buttonAddWeight.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddWeightActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        // Navigate to Goal Weight screen
        buttonGoalWeight.setOnClickListener(v -> {
            Intent intent = new Intent(this, GoalWeightActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the grid every time the screen is shown
        loadWeightGrid();
    }

    /**
     * Loads all weight entries from the database and displays them in the grid.
     * Each row shows date, weight, and a delete button.
     */
    private void loadWeightGrid() {
        layoutWeightGrid.removeAllViews();
        Cursor cursor = dbHelper.getAllWeights(username);

        if (cursor.getCount() == 0) {
            // Show message when no entries exist
            TextView emptyText = new TextView(this);
            emptyText.setText("No weight entries yet. Tap + Add Weight to begin.");
            emptyText.setPadding(32, 32, 32, 32);
            layoutWeightGrid.addView(emptyText);
        } else {
            // Populate grid with each weight entry
            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                String date = cursor.getString(2);
                double weight = cursor.getDouble(3);
                addWeightRow(id, date, weight);
            }
        }
        cursor.close();
    }

    /**
     * Inflates a row view for a single weight entry and adds it to the grid.
     * Includes a delete button that removes the entry from the database.
     */
    private void addWeightRow(int id, String date, double weight) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_weight_row, layoutWeightGrid, false);

        TextView textDate = row.findViewById(R.id.textDate);
        TextView textWeight = row.findViewById(R.id.textWeight);
        Button buttonDelete = row.findViewById(R.id.buttonDelete);

        textDate.setText(date);
        textWeight.setText(String.format("%.1f lbs", weight));

        // Delete button removes this entry and refreshes the grid
        buttonDelete.setOnClickListener(v -> {
            dbHelper.deleteWeight(id);
            Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show();
            loadWeightGrid();
        });

        layoutWeightGrid.addView(row);
    }

    /**
     * Checks if SMS permission has been granted. If not, requests it from the user.
     * The app continues to function regardless of the user's response.
     */
    private void checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS notifications enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS notifications disabled. App will continue without alerts.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
