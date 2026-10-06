package com.snhu.weighttracker;

/*
 * ==========================================================================================
 * File:    WeightHistoryActivity.java
 * Author:  Jeremy White
 * Course:  CS-499 Computer Science Capstone (SNHU)
 * Date:    September 2026 (original artifact: CS-360, June 2026)
 * Context: CS-499 Enhancement 1 (Software Design & Engineering). REVISED to use
 *          WeightRepository for all read/delete operations instead of a direct
 *          DatabaseHelper dependency, decoupling the UI from SQLite. (Course Outcomes 3, 4.)
 * ==========================================================================================
 */

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

import com.snhu.weighttracker.data.WeightRepository;

/**
 * WeightHistoryActivity displays all weight entries in a scrollable grid and supports
 * navigation to add-weight / goal screens, plus per-row delete.
 *
 * ENHANCEMENT 1 (Software Design & Engineering) — REVISED.
 * Changes from the original:
 *   1. ARCHITECTURE: uses WeightRepository instead of DatabaseHelper directly for all
 *      read/delete operations, so the UI is decoupled from SQLite.
 */
public class WeightHistoryActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 100;
    private LinearLayout layoutWeightGrid;
    // ENHANCEMENT 1: repository replaces the direct DatabaseHelper dependency.
    private WeightRepository repository;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight_history);

        // Get logged-in username from Intent
        username = getIntent().getStringExtra("username");

        // ENHANCEMENT 1: obtain data access through the repository layer.
        repository = new WeightRepository(this);
        layoutWeightGrid = findViewById(R.id.layoutWeightGrid);
        Button buttonAddWeight = findViewById(R.id.buttonAddWeight);
        Button buttonGoalWeight = findViewById(R.id.buttonGoalWeight);

        // Request SMS permission for goal notifications
        checkSmsPermission();

        buttonAddWeight.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddWeightActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        buttonGoalWeight.setOnClickListener(v -> {
            Intent intent = new Intent(this, GoalWeightActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWeightGrid();
    }

    /**
     * Loads all weight entries and displays them in the grid.
     * ENHANCEMENT 1: reads through the repository.
     */
    private void loadWeightGrid() {
        layoutWeightGrid.removeAllViews();
        Cursor cursor = repository.getAllWeights(username);

        if (cursor.getCount() == 0) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No weight entries yet. Tap + Add Weight to begin.");
            emptyText.setPadding(32, 32, 32, 32);
            layoutWeightGrid.addView(emptyText);
        } else {
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
     * Inflates a row for a single weight entry and wires its delete button.
     * ENHANCEMENT 1: delete goes through the repository.
     */
    private void addWeightRow(int id, String date, double weight) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_weight_row, layoutWeightGrid, false);

        TextView textDate = row.findViewById(R.id.textDate);
        TextView textWeight = row.findViewById(R.id.textWeight);
        Button buttonDelete = row.findViewById(R.id.buttonDelete);

        textDate.setText(date);
        textWeight.setText(String.format("%.1f lbs", weight));

        buttonDelete.setOnClickListener(v -> {
            repository.deleteWeight(id);
            Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show();
            loadWeightGrid();
        });

        layoutWeightGrid.addView(row);
    }

    /**
     * Checks/requests SMS permission. The app functions regardless of the response.
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
