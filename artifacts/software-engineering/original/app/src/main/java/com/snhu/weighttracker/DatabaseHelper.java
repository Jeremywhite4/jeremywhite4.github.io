package com.snhu.weighttracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * DatabaseHelper manages the SQLite database for the Weight Tracker app.
 * Contains two tables: users (login credentials) and weights (daily entries).
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "weighttracker.db";
    private static final int DATABASE_VERSION = 1;

    // Users table
    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "id";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";
    private static final String COL_GOAL_WEIGHT = "goal_weight";

    // Weights table
    private static final String TABLE_WEIGHTS = "weights";
    private static final String COL_WEIGHT_ID = "id";
    private static final String COL_WEIGHT_USER = "username";
    private static final String COL_WEIGHT_DATE = "date";
    private static final String COL_WEIGHT_VALUE = "weight";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table to store login credentials and goal weight
        String createUsers = "CREATE TABLE " + TABLE_USERS + " ("
                + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_USERNAME + " TEXT UNIQUE NOT NULL, "
                + COL_PASSWORD + " TEXT NOT NULL, "
                + COL_GOAL_WEIGHT + " REAL DEFAULT 0)";
        db.execSQL(createUsers);

        // Create weights table to store daily weight entries
        String createWeights = "CREATE TABLE " + TABLE_WEIGHTS + " ("
                + COL_WEIGHT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_WEIGHT_USER + " TEXT NOT NULL, "
                + COL_WEIGHT_DATE + " TEXT NOT NULL, "
                + COL_WEIGHT_VALUE + " REAL NOT NULL)";
        db.execSQL(createWeights);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEIGHTS);
        onCreate(db);
    }

    // ==================== USER OPERATIONS ====================

    /**
     * Adds a new user to the database.
     * Returns true if successful, false if username already exists.
     */
    public boolean addUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username);
        values.put(COL_PASSWORD, password);
        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    /**
     * Validates login credentials against the database.
     * Returns true if username/password combination exists.
     */
    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_USERS + " WHERE " + COL_USERNAME + "=? AND " + COL_PASSWORD + "=?",
                new String[]{username, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /**
     * Sets the goal weight for a specific user.
     */
    public void setGoalWeight(String username, double goalWeight) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_GOAL_WEIGHT, goalWeight);
        db.update(TABLE_USERS, values, COL_USERNAME + "=?", new String[]{username});
    }

    /**
     * Retrieves the goal weight for a specific user.
     * Returns 0 if no goal has been set.
     */
    public double getGoalWeight(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COL_GOAL_WEIGHT + " FROM " + TABLE_USERS + " WHERE " + COL_USERNAME + "=?",
                new String[]{username});
        double goal = 0;
        if (cursor.moveToFirst()) {
            goal = cursor.getDouble(0);
        }
        cursor.close();
        return goal;
    }

    // ==================== WEIGHT OPERATIONS ====================

    /**
     * Adds a new weight entry for a user.
     * Returns true if successful.
     */
    public boolean addWeight(String username, String date, double weight) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_WEIGHT_USER, username);
        values.put(COL_WEIGHT_DATE, date);
        values.put(COL_WEIGHT_VALUE, weight);
        long result = db.insert(TABLE_WEIGHTS, null, values);
        return result != -1;
    }

    /**
     * Retrieves all weight entries for a specific user, ordered by date descending.
     */
    public Cursor getAllWeights(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM " + TABLE_WEIGHTS + " WHERE " + COL_WEIGHT_USER + "=? ORDER BY " + COL_WEIGHT_DATE + " DESC",
                new String[]{username});
    }

    /**
     * Updates an existing weight entry by its ID.
     */
    public void updateWeight(int id, double newWeight) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_WEIGHT_VALUE, newWeight);
        db.update(TABLE_WEIGHTS, values, COL_WEIGHT_ID + "=?", new String[]{String.valueOf(id)});
    }

    /**
     * Deletes a weight entry by its ID.
     */
    public void deleteWeight(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WEIGHTS, COL_WEIGHT_ID + "=?", new String[]{String.valueOf(id)});
    }
}
