package com.lfms.controller.admin;

import com.lfms.model.AuditLog;
import com.lfms.model.Claim;
import com.lfms.model.Item;
import com.lfms.service.AuditService;
import com.lfms.service.ClaimService;
import com.lfms.service.ItemService;
import com.lfms.service.UserService;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

/**
 * Admin dashboard: key counts, a Lost-vs-Found pie chart, a claims-by-status bar chart
 * (all from live data) and a recent-activity table.
 */
public class AdminDashboardController {

    @FXML private AdminSidebarController adminSidebarController;
    @FXML private Label totalUsersLabel;
    @FXML private Label openLostLabel;
    @FXML private Label openFoundLabel;
    @FXML private Label pendingClaimsLabel;
    @FXML private Label resolvedLabel;
    @FXML private VBox pendingCard;
    @FXML private PieChart typePie;
    @FXML private BarChart<String, Number> claimsBar;
    @FXML private TableView<AuditLog> recentTable;
    @FXML private TableColumn<AuditLog, String> recentTimeCol;
    @FXML private TableColumn<AuditLog, String> recentActionCol;
    @FXML private TableColumn<AuditLog, String> recentNoteCol;

    private final UserService userService = new UserService();
    private final ItemService itemService = new ItemService();
    private final ClaimService claimService = new ClaimService();
    private final AuditService auditService = new AuditService();

    @FXML
    public void initialize() {
        adminSidebarController.setActive("dashboard");
        loadStats();
        loadCharts();
        loadRecentActivity();
    }

    private void loadStats() {
        totalUsersLabel.setText(String.valueOf(userService.countAll()));
        openLostLabel.setText(String.valueOf(itemService.countOpenByType(Item.TYPE_LOST)));
        openFoundLabel.setText(String.valueOf(itemService.countOpenByType(Item.TYPE_FOUND)));

        int pending = claimService.countByStatus(Claim.STATUS_PENDING);
        pendingClaimsLabel.setText(String.valueOf(pending));
        if (pending > 0 && !pendingCard.getStyleClass().contains("stat-card-warning")) {
            pendingCard.getStyleClass().add("stat-card-warning");
        }

        int resolved = itemService.countByStatus(Item.STATUS_RESOLVED)
                + itemService.countByStatus(Item.STATUS_APPROVED);
        resolvedLabel.setText(String.valueOf(resolved));
    }

    @SuppressWarnings("unchecked")
    private void loadCharts() {
        int lost = itemService.countByType(Item.TYPE_LOST);
        int found = itemService.countByType(Item.TYPE_FOUND);
        typePie.setTitle("Lost vs Found Items");
        typePie.getData().setAll(
                new PieChart.Data("Lost (" + lost + ")", lost),
                new PieChart.Data("Found (" + found + ")", found));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Claims");
        series.getData().add(new XYChart.Data<>("Pending", claimService.countByStatus(Claim.STATUS_PENDING)));
        series.getData().add(new XYChart.Data<>("Approved", claimService.countByStatus(Claim.STATUS_APPROVED)));
        series.getData().add(new XYChart.Data<>("Rejected", claimService.countByStatus(Claim.STATUS_REJECTED)));
        claimsBar.setTitle("Claims by Status");
        claimsBar.getData().setAll(series);
    }

    private void loadRecentActivity() {
        recentTimeCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        recentActionCol.setCellValueFactory(new PropertyValueFactory<>("action"));
        recentNoteCol.setCellValueFactory(new PropertyValueFactory<>("note"));
        recentTable.setPlaceholder(new Label("No recent activity yet."));
        recentTable.getItems().setAll(auditService.findRecent(10));
    }
}
