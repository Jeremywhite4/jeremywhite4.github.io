package com.snhu.weighttracker.presenter;

/*
 * ==========================================================================================
 * File:    LoginPresenter.java
 * Author:  Jeremy White
 * Course:  CS-499 Computer Science Capstone (SNHU)
 * Date:    September 2026
 * Context: CS-499 Enhancement 1 (Software Design & Engineering). NEW MVP presenter that
 *          holds the login/registration coordination logic as plain, Android-free Java so
 *          it is easy to reason about and test. Talks to the Activity via a View interface.
 *          (Course Outcomes 3, 4.)
 * ==========================================================================================
 */

import com.snhu.weighttracker.auth.AuthService;

/**
 * ENHANCEMENT 1 (Software Design & Engineering) — NEW CLASS.
 *
 * LoginPresenter is the presenter in a Model-View-Presenter (MVP) style separation for
 * the login screen.
 *
 * WHY THIS WAS ADDED:
 * The original LoginActivity mixed UI code, input handling, and authentication together.
 * This presenter moves the coordination logic out of the Activity so that:
 *   - the Activity only renders UI and forwards user actions (thin view), and
 *   - the decision logic (validate -> authenticate -> tell the view what to show) is in a
 *     plain Java class with no Android dependencies, which is easy to reason about and to
 *     test. (Outcomes 3 and 4.)
 *
 * The presenter talks to the View through the {@link View} interface, so it never
 * references Android classes directly.
 */
public class LoginPresenter {

    /**
     * ENHANCEMENT 1: the view contract the Activity implements. Keeps the presenter free
     * of any Android/UI dependency.
     */
    public interface View {
        void showMessage(String message);
        void onLoginSuccess(String username);
    }

    private final AuthService authService;
    private final View view;

    public LoginPresenter(AuthService authService, View view) {
        this.authService = authService;
        this.view = view;
    }

    /**
     * ENHANCEMENT 1: handle a login attempt. Delegates to AuthService (which validates,
     * hashes, and verifies) and then instructs the view what to display.
     */
    public void onLoginClicked(String username, String password) {
        AuthService.AuthResult result = authService.login(username, password);
        if (result.isSuccess()) {
            view.onLoginSuccess(username == null ? "" : username.trim());
        } else {
            view.showMessage(result.getMessage());
        }
    }

    /**
     * ENHANCEMENT 1: handle account creation. Delegates to AuthService.registerUser and
     * surfaces the resulting message.
     */
    public void onCreateAccountClicked(String username, String password) {
        AuthService.AuthResult result = authService.registerUser(username, password);
        view.showMessage(result.getMessage());
    }
}
