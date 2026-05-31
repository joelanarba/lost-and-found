package com.lfms.controller.admin;

import com.lfms.model.User;
import com.lfms.util.SceneNavigator;
import com.lfms.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for the shared admin sidebar.
 */
public class AdminSidebarController {

    @FXML private Button btnDashboard;
    @FXML private Button btnClaims;
    @FXML private Button btnReports;
    @FXML private Button btnUsers;
    @FXML private Button btnAudit;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;

    private Map<String, Button> navButtons;

    @FXML
    public void initialize() {
        navButtons = new LinkedHashMap<>();
        navButtons.put("dashboard", btnDashboard);
        navButtons.put("claims", btnClaims);
        navButtons.put("reports", btnReports);
        navButtons.put("users", btnUsers);
        navButtons.put("audit", btnAudit);

        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            userNameLabel.setText(user.getName());
            userRoleLabel.setText(user.getRole());
        }
    }

    public void setActive(String key) {
        if (navButtons == null) {
            return;
        }
        for (Button button : navButtons.values()) {
            button.getStyleClass().remove("active");
        }
        Button active = navButtons.get(key);
        if (active != null && !active.getStyleClass().contains("active")) {
            active.getStyleClass().add("active");
        }
    }

    @FXML
    private void goDashboard() {
        SceneNavigator.navigateTo("/com/lfms/fxml/admin/AdminDashboard.fxml");
    }

    @FXML
    private void goClaims() {
        SceneNavigator.navigateTo("/com/lfms/fxml/admin/AdminClaims.fxml");
    }

    @FXML
    private void goReports() {
        SceneNavigator.navigateTo("/com/lfms/fxml/admin/AdminReports.fxml");
    }

    @FXML
    private void goUsers() {
        SceneNavigator.navigateTo("/com/lfms/fxml/admin/AdminUsers.fxml");
    }

    @FXML
    private void goAudit() {
        SceneNavigator.navigateTo("/com/lfms/fxml/admin/AdminAuditLog.fxml");
    }

    @FXML
    private void logout() {
        SessionManager.getInstance().clearSession();
        SceneNavigator.navigateTo("/com/lfms/fxml/Login.fxml");
    }
}
