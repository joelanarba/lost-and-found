package com.lfms.controller;

import com.lfms.service.AuthService;
import com.lfms.util.SceneNavigator;
import com.lfms.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

/**
 * Controller for the Register screen. Performs inline per-field validation, then delegates
 * uniqueness checks and account creation to {@link AuthService}.
 */
public class RegisterController {

    @FXML private TextField nameField;
    @FXML private TextField indexField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;

    @FXML private Label nameError;
    @FXML private Label indexError;
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label confirmError;
    @FXML private Label generalError;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleRegister() {
        ValidationUtil.clearAllErrors(nameError, indexError, emailError, passwordError, confirmError, generalError);

        boolean valid = true;
        if (!ValidationUtil.isNotEmpty(nameField.getText())) {
            ValidationUtil.showError(nameError, "Full name is required.");
            valid = false;
        }
        if (!ValidationUtil.isNotEmpty(indexField.getText())) {
            ValidationUtil.showError(indexError, "Index / ID number is required.");
            valid = false;
        }
        if (!ValidationUtil.isValidEmail(emailField.getText())) {
            ValidationUtil.showError(emailError, "Enter a valid email address.");
            valid = false;
        }
        if (!ValidationUtil.isMinLength(passwordField.getText(), 8)) {
            ValidationUtil.showError(passwordError, "Password must be at least 8 characters.");
            valid = false;
        }
        if (!passwordField.getText().equals(confirmField.getText())) {
            ValidationUtil.showError(confirmError, "Passwords do not match.");
            valid = false;
        }
        if (!valid) {
            return;
        }

        try {
            AuthService.Result result = authService.register(
                    nameField.getText(), indexField.getText(), emailField.getText(),
                    passwordField.getText(), phoneField.getText());

            if (result.isSuccess()) {
                SceneNavigator.navigateTo("/com/lfms/fxml/Login.fxml",
                        "Account created successfully. Please sign in.");
            } else {
                routeServerError(result.getErrorMessage());
            }
        } catch (RuntimeException e) {
            ValidationUtil.showError(generalError, "Registration failed: " + e.getMessage());
        }
    }

    private void routeServerError(String message) {
        String lower = message == null ? "" : message.toLowerCase();
        if (lower.contains("email")) {
            ValidationUtil.showError(emailError, message);
        } else if (lower.contains("index")) {
            ValidationUtil.showError(indexError, message);
        } else {
            ValidationUtil.showError(generalError, message);
        }
    }

    @FXML
    private void goToLogin(MouseEvent event) {
        SceneNavigator.navigateTo("/com/lfms/fxml/Login.fxml");
    }
}
