package com.lfms.controller;

import com.lfms.model.User;
import com.lfms.util.NotificationBell;
import com.lfms.util.SceneNavigator;
import com.lfms.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for the shared student sidebar. Handles navigation between the main student
 * screens, shows the logged-in user, and highlights the active section.
 */
public class SidebarController {

    @FXML private Button btnDashboard;
    @FXML private Button btnReportLost;
    @FXML private Button btnReportFound;
    @FXML private Button btnBrowse;
    @FXML private Button btnMyReports;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Button bellButton;
    @FXML private Label notifBadge;
    @FXML private javafx.scene.layout.HBox badgeBox;

    private Map<String, Button> navButtons;
    private final com.lfms.service.ClaimService claimService = new com.lfms.service.ClaimService();

    @FXML
    public void initialize() {
        navButtons = new LinkedHashMap<>();
        navButtons.put("dashboard", btnDashboard);
        navButtons.put("reportlost", btnReportLost);
        navButtons.put("reportfound", btnReportFound);
        navButtons.put("browse", btnBrowse);
        navButtons.put("myreports", btnMyReports);

        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            userNameLabel.setText(user.getName());
            userRoleLabel.setText(user.getRole());

            int successfulReturns = claimService.countSuccessfulReturns(user.getUserId());
            if (badgeBox != null) {
                badgeBox.getChildren().clear();
                if (successfulReturns >= 1) {
                    Label bronze = new Label("🥉");
                    bronze.setStyle("-fx-font-size: 18px;");
                    javafx.scene.control.Tooltip.install(bronze, new javafx.scene.control.Tooltip("Good Samaritan - 1 return"));
                    badgeBox.getChildren().add(bronze);
                }
                if (successfulReturns >= 5) {
                    Label silver = new Label("🥈");
                    silver.setStyle("-fx-font-size: 18px;");
                    javafx.scene.control.Tooltip.install(silver, new javafx.scene.control.Tooltip("Campus Hero - 5 returns"));
                    badgeBox.getChildren().add(silver);
                }
                if (successfulReturns >= 10) {
                    Label gold = new Label("🥇");
                    gold.setStyle("-fx-font-size: 18px;");
                    javafx.scene.control.Tooltip.install(gold, new javafx.scene.control.Tooltip("Legend - 10 returns"));
                    badgeBox.getChildren().add(gold);
                }
            }
        }
        NotificationBell.install(bellButton, notifBadge);
    }

    /** Highlights the active nav button (key matches the map above). */
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
        SceneNavigator.navigateTo("/com/lfms/fxml/Dashboard.fxml");
    }

    @FXML
    private void goReportLost() {
        SceneNavigator.navigateTo("/com/lfms/fxml/ReportLost.fxml");
    }

    @FXML
    private void goReportFound() {
        SceneNavigator.navigateTo("/com/lfms/fxml/ReportFound.fxml");
    }

    @FXML
    private void goBrowse() {
        SceneNavigator.navigateTo("/com/lfms/fxml/Browse.fxml");
    }

    @FXML
    private void goMyReports() {
        SceneNavigator.navigateTo("/com/lfms/fxml/MyReports.fxml");
    }

    @FXML
    private void openNotifications() {
        if (bellButton != null) {
            bellButton.fire();
        }
    }

    @FXML
    private void logout() {
        SessionManager.getInstance().clearSession();
        SceneNavigator.navigateTo("/com/lfms/fxml/Login.fxml");
    }

    @FXML
    private void toggleTheme() {
        SceneNavigator.toggleTheme();
    }
}
