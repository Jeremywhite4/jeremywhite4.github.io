package com.snhu.weighttracker;

/*
 * ==========================================================================================
 * File:    LoginActivity.java
 * Author:  Jeremy White
 * Course:  CS-499 Computer Science Capstone (SNHU)
 * Date:    September 2026 (original artifact: CS-360, June 2026)
 * Context: CS-499 Enhancement 1 (Software Design & Engineering). REVISED to a thin MVP
 *          "view": authentication and validation were moved out of this Activity into
 *          LoginPresenter + AuthService. Removed the plaintext checkUser() call and the
 *          direct DatabaseHelper dependency. (Course Outcomes 3, 4, 5.)
 * ==========================================================================================
 */

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.snhu.weighttracker.auth.AuthService;
import com.snhu.weighttracker.data.DatabaseHelper;
import com.snhu.weighttracker.presenter.LoginPresenter;

/**
 * LoginActivity handles user authentication and account creation.
 *
 * ENHANCEMENT 1 (Software Design & Engineering) — REVISED.
 * Changes from the original:
 *   1. ARCHITECTURE: the Activity is now a thin "view". It no longer performs
 *      authentication or input validation itself. It forwards button clicks to a
 *      LoginPresenter and implements LoginPresenter.View to render results.
 *   2. SECURITY: it no longer calls DatabaseHelper.checkUser(username, plaintextPassword).
 *      Authentication now flows through AuthService (salted PBKDF2 + anti-enumeration).
 *   3. The Activity holds no DatabaseHelper reference and issues no SQL/data calls.
 */
public class LoginActivity extends AppCompatActivity implements LoginPresenter.View {

    private EditText editUsername, editPassword;

    // ENHANCEMENT 1: the Activity now depends on a presenter, not on the database.
    private LoginPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // ENHANCEMENT 1: wire up the layers. DatabaseHelper (DAO) -> AuthService ->
        // LoginPresenter -> this view. In a larger app this wiring would be handled by a
        // dependency-injection framework; here it is done explicitly for clarity.
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        AuthService authService = new AuthService(dbHelper);
        presenter = new LoginPresenter(authService, this);

        // Bind UI elements
        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);
        Button buttonLogin = findViewById(R.id.buttonLogin);
        Button buttonCreateAccount = findViewById(R.id.buttonCreateAccount);

        // ENHANCEMENT 1: forward user actions to the presenter (no logic in the view).
        buttonLogin.setOnClickListener(v ->
                presenter.onLoginClicked(getUsername(), getPassword()));
        buttonCreateAccount.setOnClickListener(v ->
                presenter.onCreateAccountClicked(getUsername(), getPassword()));
    }

    private String getUsername() {
        return editUsername.getText().toString();
    }

    private String getPassword() {
        return editPassword.getText().toString();
    }

    // ==================== LoginPresenter.View ====================

    /**
     * ENHANCEMENT 1: view callback — display any message (validation errors, generic
     * login failures, account-created confirmations) as a Toast.
     */
    @Override
    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * ENHANCEMENT 1: view callback — on successful login, navigate to the weight history
     * screen. Navigation stays in the view; the decision to navigate came from the
     * presenter.
     */
    @Override
    public void onLoginSuccess(String username) {
        Intent intent = new Intent(this, WeightHistoryActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
        finish();
    }
}
