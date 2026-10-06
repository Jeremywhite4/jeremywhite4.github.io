package com.snhu.weighttracker.data;

/*
 * ==========================================================================================
 * File:    WeightRepository.java
 * Author:  Jeremy White
 * Course:  CS-499 Computer Science Capstone (SNHU)
 * Date:    September 2026
 * Context: CS-499 Enhancement 1 (Software Design & Engineering). NEW repository that is the
 *          single point of access for weight/goal data, decoupling the Activities from the
 *          SQLite DAO (they previously each held their own DatabaseHelper). (Outcomes 3, 4.)
 * ==========================================================================================
 */

import android.content.Context;
import android.database.Cursor;

/**
 * ENHANCEMENT 1 (Software Design & Engineering) — NEW CLASS.
 *
 * WeightRepository is the single point of access for weight and goal data.
 *
 * WHY THIS WAS ADDED:
 * In the original artifact, EVERY Activity created its own DatabaseHelper and called it
 * directly (e.g. AddWeightActivity, GoalWeightActivity, WeightHistoryActivity all did
 * `dbHelper = new DatabaseHelper(this)` and issued data calls inline). That couples the
 * UI tightly to the persistence layer and duplicates data logic across screens.
 *
 * The Repository pattern introduces one abstraction the UI talks to for all
 * weight/goal operations. This decouples Activities from SQLite, centralizes data
 * access, and gives us a seam we could later back with a different data source
 * (network, Room, etc.) without touching the UI. (Outcomes 3 and 4.)
 */
public class WeightRepository {

    private final DatabaseHelper dbHelper;

    public WeightRepository(Context context) {
        // ENHANCEMENT 1: repository owns the DAO; Activities no longer construct it.
        this.dbHelper = new DatabaseHelper(context.getApplicationContext());
    }

    // ---- Weight operations ----

    public boolean addWeight(String username, String date, double weight) {
        return dbHelper.addWeight(username, date, weight);
    }

    public Cursor getAllWeights(String username) {
        return dbHelper.getAllWeights(username);
    }

    public void updateWeight(int id, double newWeight) {
        dbHelper.updateWeight(id, newWeight);
    }

    public void deleteWeight(int id) {
        dbHelper.deleteWeight(id);
    }

    // ---- Goal operations ----

    public void setGoalWeight(String username, double goalWeight) {
        dbHelper.setGoalWeight(username, goalWeight);
    }

    public double getGoalWeight(String username) {
        return dbHelper.getGoalWeight(username);
    }
}
