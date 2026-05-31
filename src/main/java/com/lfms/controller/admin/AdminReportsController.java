package com.lfms.controller.admin;

import com.lfms.model.Item;
import com.lfms.model.User;
import com.lfms.service.ExportService;
import com.lfms.service.ItemService;
import com.lfms.service.UserService;
import com.lfms.util.Badges;
import com.lfms.util.Dialogs;
import com.lfms.util.ImageUtil;
import com.lfms.util.SceneNavigator;
import com.lfms.util.SessionManager;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Admin report-management screen: search/filter all items, view, change status, delete
 * (with reason) and export to CSV.
 */
public class AdminReportsController {

    private static final List<String> STATUS_CODES = List.of(
            Item.STATUS_OPEN, Item.STATUS_CLAIM_PENDING, Item.STATUS_APPROVED,
            Item.STATUS_RESOLVED, Item.STATUS_CLOSED);

    @FXML private AdminSidebarController adminSidebarController;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeBox;
    @FXML private ComboBox<String> statusBox;
    @FXML private TableView<Item> table;
    @FXML private TableColumn<Item, Integer> idCol;
    @FXML private TableColumn<Item, String> typeCol;
    @FXML private TableColumn<Item, String> nameCol;
    @FXML private TableColumn<Item, String> categoryCol;
    @FXML private TableColumn<Item, String> reporterCol;
    @FXML private TableColumn<Item, String> statusCol;
    @FXML private TableColumn<Item, String> dateCol;
    @FXML private TableColumn<Item, Void> actionsCol;

    private final ItemService itemService = new ItemService();
    private final UserService userService = new UserService();
    private final ExportService exportService = new ExportService();

    private Map<Integer, String> nameMap = Map.of();

    @FXML
    public void initialize() {
        adminSidebarController.setActive("reports");
        nameMap = userService.nameMap();

        typeBox.getItems().setAll("All", "Lost Items", "Found Items");
        typeBox.setValue("All");
        statusBox.getItems().setAll("All", "Open", "Claim Pending", "Approved", "Resolved", "Closed");
        statusBox.setValue("All");

        configureColumns();
        table.setPlaceholder(new Label("No reports match your filters."));
        refresh();
    }

    private void configureColumns() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("itemId"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("dateReported"));

        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setCellFactory(Badges.badgeCellFactory());
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(Badges.badgeCellFactory());

        reporterCol.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nameMap.getOrDefault(data.getValue().getUserId(), "Unknown")));

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button view = new Button("View");
            private final Button delete = new Button("Delete");
            private final ComboBox<String> statusPicker = new ComboBox<>();
            private final HBox box = new HBox(6, view, delete, statusPicker);
            {
                view.getStyleClass().add("btn-secondary");
                delete.getStyleClass().add("btn-danger");
                statusPicker.getItems().setAll(STATUS_CODES);
                statusPicker.setPrefWidth(130);
                view.setOnAction(e -> viewItem(current()));
                delete.setOnAction(e -> deleteItem(current()));
                statusPicker.setOnAction(e -> {
                    Item item = current();
                    String chosen = statusPicker.getValue();
                    if (item != null && chosen != null && !chosen.equals(item.getStatus())) {
                        changeStatus(item, chosen);
                    }
                });
            }
            private Item current() {
                return getTableView().getItems().get(getIndex());
            }
            @Override
            protected void updateItem(Void value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                statusPicker.setValue(current().getStatus());
                setGraphic(box);
            }
        });
    }

    @FXML
    private void handleSearch() {
        refresh();
    }

    private void refresh() {
        nameMap = userService.nameMap();
        String type = mapType(typeBox.getValue());
        String statusFilter = mapStatus(statusBox.getValue());
        List<Item> results = itemService.search(searchField.getText(), type, null, null, null, null);
        if (statusFilter != null) {
            results.removeIf(item -> !statusFilter.equals(item.getStatus()));
        }
        table.getItems().setAll(results);
    }

    private void changeStatus(Item item, String newStatus) {
        if (itemService.updateStatus(item.getItemId(), newStatus, adminId())) {
            refresh();
        } else {
            Dialogs.error("Error", "Could not change the item status.");
        }
    }

    private void deleteItem(Item item) {
        Optional<String> reason = Dialogs.prompt("Delete Report",
                "Delete \"" + item.getName() + "\"? This also removes its claims and matches.",
                "Reason for deletion");
        if (reason.isEmpty()) {
            return;
        }
        if (itemService.deleteItem(item.getItemId(), adminId(), reason.get())) {
            refresh();
        } else {
            Dialogs.error("Error", "Could not delete the report.");
        }
    }

    @FXML
    private void exportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Reports to CSV");
        chooser.setInitialFileName("reports.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showSaveDialog(SceneNavigator.getPrimaryStage());
        if (file == null) {
            return;
        }
        try {
            exportService.exportItems(new ArrayList<>(table.getItems()), file);
            Dialogs.info("Export Complete",
                    "Exported " + table.getItems().size() + " report(s) to:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            Dialogs.error("Export Failed", e.getMessage());
        }
    }

    private void viewItem(Item item) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Report #" + item.getItemId());
        dialog.setHeaderText(item.getName());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ImageView image = new ImageView(ImageUtil.loadImage(item.getImagePath()));
        image.setFitWidth(220);
        image.setFitHeight(220);
        image.setPreserveRatio(true);
        StackPane frame = new StackPane(image);
        frame.getStyleClass().add("image-frame");

        VBox content = new VBox(6,
                frame,
                keyValue("Type", item.getType()),
                keyValue("Status", item.getStatus()),
                keyValue("Category", item.getCategory()),
                keyValue("Location", nz(item.getLocation())),
                keyValue("Date Reported", nz(item.getDateReported())),
                keyValue("Reported By", nameMap.getOrDefault(item.getUserId(), "Unknown")),
                new Label("Description:"),
                wrapped(item.getDescription()));
        dialog.getDialogPane().setContent(content);
        applyStylesheet(dialog);
        dialog.showAndWait();
    }

    private Label keyValue(String key, String value) {
        Label label = new Label(key + ":  " + (value == null ? "" : value));
        label.getStyleClass().add("detail-value");
        return label;
    }

    private Label wrapped(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(360);
        label.getStyleClass().add("description-text");
        return label;
    }

    private void applyStylesheet(Dialog<?> dialog) {
        URL css = getClass().getResource("/com/lfms/css/main.css");
        if (css != null) {
            dialog.getDialogPane().getStylesheets().add(css.toExternalForm());
        }
    }

    private String mapType(String label) {
        if ("Lost Items".equals(label)) {
            return Item.TYPE_LOST;
        }
        if ("Found Items".equals(label)) {
            return Item.TYPE_FOUND;
        }
        return null;
    }

    private String mapStatus(String label) {
        if (label == null || "All".equals(label)) {
            return null;
        }
        return label.toUpperCase().replace(' ', '_');
    }

    private int adminId() {
        User admin = SessionManager.getInstance().getCurrentUser();
        return admin != null ? admin.getUserId() : -1;
    }

    private String nz(String value) {
        return (value == null || value.isBlank()) ? "—" : value;
    }
}
