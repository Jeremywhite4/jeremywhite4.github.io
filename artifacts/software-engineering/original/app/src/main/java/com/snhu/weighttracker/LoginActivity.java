package com.snhu.weighttracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * LoginActivity handles user authentication and account creation.
 * Users can log in with existing credentials or create a new account.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText editUsername, editPassword;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize database helper
        dbHelper = new DatabaseHelper(this);

        // Bind UI elements
        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);
        Button buttonLogin = findViewById(R.id.buttonLogin);
        Button buttonCreateAccount = findViewById(R.id.buttonCreateAccount);

        // Login button validates credentials against the database
        buttonLogin.setOnClickListener(v -> attemptLogin());

        // Create Account button adds a new user to the database
        buttonCreateAccount.setOnClickListener(v -> createAccount());
    }

    /**
     * Checks username and password against the database.
     * Navigates to WeightHistoryActivity on success.
     */
    private void attemptLogin() {
        String username = editUsername.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        // Validate input fields are not empty
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check credentials against database
        if (dbHelper.checkUser(username, password)) {
            // Successful login - navigate to weight history screen
            Intent intent = new Intent(this, WeightHistoryActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Creates a new user account and saves it to the database.
     */
    private void createAccount() {
        String username = editUsername.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        // Validate input fields are not empty
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Attempt to add user to database
        if (dbHelper.addUser(username, password)) {
            Toast.makeText(this, "Account created! You can now log in.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show();
        }
    }
}
