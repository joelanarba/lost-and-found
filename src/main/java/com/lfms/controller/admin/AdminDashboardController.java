package com.lfms.controller.admin;

import com.lfms.model.AuditLog;
import com.lfms.model.Claim;
import com.lfms.model.Item;
import com.lfms.model.LabelCount;
import com.lfms.service.AuditService;
import com.lfms.service.ClaimService;
import com.lfms.service.ItemService;
import com.lfms.service.UserService;
import com.lfms.util.StatFormat;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.util.List;

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
    @FXML private Label recoveryRateLabel;
    @FXML private Label topCategoryLabel;
    @FXML private Label averageClaimTimeLabel;
    @FXML private VBox pendingCard;
    @FXML private PieChart typePie;
    @FXML private BarChart<String, Number> claimsBar;
    @FXML private javafx.scene.web.WebView heatmapWeb;
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

        recoveryRateLabel.setText(StatFormat.recoveryRate(itemService.recoveryRate()));
        topCategoryLabel.setText(StatFormat.mostLostCategory(
                itemService.topLostCategory(), itemService.countByType(Item.TYPE_LOST)));
        averageClaimTimeLabel.setText(StatFormat.averageClaimTime(claimService.averageClaimTimeHours()));
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

        loadTopLocations();
    }

    /** Loads heatmap map and injects top loss locations. */
    private void loadTopLocations() {
        List<LabelCount> locations = itemService.topLostLocations(10);
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (LabelCount lc : locations) {
            com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
            obj.addProperty("location", lc.label());
            obj.addProperty("count", lc.count());
            arr.add(obj);
        }
        String json = arr.toString();

        java.net.URL url = getClass().getResource("/com/lfms/html/heatmap.html");
        if (url != null) {
            heatmapWeb.getEngine().load(url.toExternalForm());
            heatmapWeb.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    heatmapWeb.getEngine().executeScript("window.setHeatData('" + json + "');");
                }
            });
        }
    }

    private void loadRecentActivity() {
        recentTimeCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        recentActionCol.setCellValueFactory(new PropertyValueFactory<>("action"));
        recentNoteCol.setCellValueFactory(new PropertyValueFactory<>("note"));
        recentTable.setPlaceholder(new Label("No recent activity yet."));
        recentTable.getItems().setAll(auditService.findRecent(10));
    }

    @FXML
    private void archiveStaleItems() {
        int archived = new com.lfms.dao.ItemDAO().archiveStaleItems(90);
        com.lfms.util.Dialogs.info("Archive Successful", archived + " items older than 90 days were archived.");
        loadStats();
    }

    @FXML
    private void exportPdfReport() {
        java.nio.file.Path pdf = com.lfms.service.PdfReportService.generateMonthlyReport(
            userService.countAll(),
            itemService.countOpenByType(Item.TYPE_LOST),
            itemService.countOpenByType(Item.TYPE_FOUND),
            claimService.countByStatus(Claim.STATUS_PENDING),
            itemService.countByStatus(Item.STATUS_RESOLVED) + itemService.countByStatus(Item.STATUS_APPROVED)
        );
        if (pdf != null) {
            com.lfms.util.Dialogs.info("PDF Exported", "Report saved to Desktop: " + pdf.getFileName());
        } else {
            com.lfms.util.Dialogs.error("Export Failed", "Could not generate PDF.");
        }
    }

    @FXML
    private void generateDemoData() {
        boolean confirm = com.lfms.util.Dialogs.confirm("Generate Demo Data", 
            "This will WIPE all existing data (except the admin account) and insert a realistic demo dataset. Are you sure you want to proceed?");
        if (confirm) {
            com.lfms.database.DataSeeder.seedDemoData();
            com.lfms.util.Dialogs.info("Demo Data Generated", "The database has been seeded with demo users, items, and claims.");
            loadStats();
            loadCharts();
            loadRecentActivity();
        }
    }
}
