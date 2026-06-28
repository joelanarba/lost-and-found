package com.lfms.controller;

import com.lfms.model.User;
import com.lfms.service.AuthService;
import com.lfms.service.DemoDataService;
import com.lfms.util.SceneNavigator;
import com.lfms.util.SessionManager;
import com.lfms.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

/**
 * Controller for the Login screen.
 */
public class LoginController implements SceneNavigator.DataReceiver {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private final AuthService authService = new AuthService();
    private final DemoDataService demoDataService = new DemoDataService();

    /** Receives an optional success message (e.g. after registration). */
    @Override
    public void receiveData(Object data) {
        if (data instanceof String message && !message.isBlank()) {
            showSuccess(message);
        }
    }

    @FXML
    private void handleLogin() {
        ValidationUtil.clearError(errorLabel);
        String email = emailField.getText();
        String password = passwordField.getText();

        if (!ValidationUtil.isNotEmpty(email) || !ValidationUtil.isNotEmpty(password)) {
            ValidationUtil.showError(errorLabel, "Please enter your email and password.");
            return;
        }

        try {
            User user = authService.login(email, password);
            if (user == null) {
                ValidationUtil.showError(errorLabel, "Invalid email/index or password.");
                return;
            }
            SessionManager.getInstance().setCurrentUser(user);
            if (user.isAdmin()) {
                SceneNavigator.navigateTo("/com/lfms/fxml/admin/AdminDashboard.fxml");
            } else {
                SceneNavigator.navigateTo("/com/lfms/fxml/Dashboard.fxml");
            }
        } catch (RuntimeException e) {
            ValidationUtil.showError(errorLabel, "Login failed: " + e.getMessage());
        }
    }

    @FXML
    private void goToRegister(MouseEvent event) {
        SceneNavigator.navigateTo("/com/lfms/fxml/Register.fxml");
    }

    @FXML
    private void loadDemoData(MouseEvent event) {
        ValidationUtil.clearError(errorLabel);
        try {
            boolean seeded = demoDataService.seedIfEmpty();
            if (seeded) {
                showSuccess("Demo data loaded. Sign in as  " + DemoDataService.DEMO_EMAIL
                        + " / " + DemoDataService.DEMO_PASSWORD
                        + "  (admin: admin@gmail.com / 12345678).");
            } else {
                showSuccess("Demo data is already loaded. Sign in as  "
                        + DemoDataService.DEMO_EMAIL + " / " + DemoDataService.DEMO_PASSWORD + ".");
            }
        } catch (RuntimeException e) {
            ValidationUtil.showError(errorLabel, "Could not load demo data: " + e.getMessage());
        }
    }

    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
    }
}
