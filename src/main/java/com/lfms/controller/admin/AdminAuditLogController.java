package com.lfms.controller.admin;

import com.lfms.model.AuditLog;
import com.lfms.service.AuditService;
import com.lfms.service.ExportService;
import com.lfms.util.Dialogs;
import com.lfms.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin audit-log viewer with keyword and date-range filtering plus CSV export.
 */
public class AdminAuditLogController {

    @FXML private AdminSidebarController adminSidebarController;
    @FXML private TextField searchField;
    @FXML private DatePicker dateFrom;
    @FXML private DatePicker dateTo;
    @FXML private TableView<AuditLog> table;
    @FXML private TableColumn<AuditLog, String> timestampCol;
    @FXML private TableColumn<AuditLog, String> actorCol;
    @FXML private TableColumn<AuditLog, String> actionCol;
    @FXML private TableColumn<AuditLog, String> targetTypeCol;
    @FXML private TableColumn<AuditLog, Integer> targetIdCol;
    @FXML private TableColumn<AuditLog, String> noteCol;

    private final AuditService auditService = new AuditService();
    private final ExportService exportService = new ExportService();

    private final List<AuditLog> master = new ArrayList<>();

    @FXML
    public void initialize() {
        adminSidebarController.setActive("audit");

        timestampCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        actorCol.setCellValueFactory(new PropertyValueFactory<>("actorName"));
        actionCol.setCellValueFactory(new PropertyValueFactory<>("action"));
        targetTypeCol.setCellValueFactory(new PropertyValueFactory<>("targetType"));
        targetIdCol.setCellValueFactory(new PropertyValueFactory<>("targetId"));
        noteCol.setCellValueFactory(new PropertyValueFactory<>("note"));

        table.setPlaceholder(new Label("No audit entries match your filter."));

        master.addAll(auditService.findAll());
        applyFilter();
    }

    @FXML
    private void handleFilter() {
        applyFilter();
    }

    @FXML
    private void handleClear() {
        searchField.clear();
        dateFrom.setValue(null);
        dateTo.setValue(null);
        applyFilter();
    }

    private void applyFilter() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        LocalDate from = dateFrom.getValue();
        LocalDate to = dateTo.getValue();

        List<AuditLog> filtered = new ArrayList<>();
        for (AuditLog log : master) {
            boolean matchesText = q.isEmpty()
                    || contains(log.getAction(), q)
                    || contains(log.getNote(), q)
                    || contains(log.getActorName(), q)
                    || contains(log.getTargetType(), q);

            boolean matchesDate = true;
            String day = (log.getTimestamp() != null && log.getTimestamp().length() >= 10)
                    ? log.getTimestamp().substring(0, 10) : null;
            if (from != null && (day == null || day.compareTo(from.toString()) < 0)) {
                matchesDate = false;
            }
            if (to != null && (day == null || day.compareTo(to.toString()) > 0)) {
                matchesDate = false;
            }

            if (matchesText && matchesDate) {
                filtered.add(log);
            }
        }
        table.getItems().setAll(filtered);
    }

    @FXML
    private void exportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Audit Log to CSV");
        chooser.setInitialFileName("audit-log.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showSaveDialog(SceneNavigator.getPrimaryStage());
        if (file == null) {
            return;
        }
        try {
            exportService.exportAuditLog(new ArrayList<>(table.getItems()), file);
            Dialogs.info("Export Complete",
                    "Exported " + table.getItems().size() + " entries to:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            Dialogs.error("Export Failed", e.getMessage());
        }
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }
}
