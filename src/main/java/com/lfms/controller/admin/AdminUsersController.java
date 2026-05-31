package com.lfms.controller.admin;

import com.lfms.model.User;
import com.lfms.service.ExportService;
import com.lfms.service.UserService;
import com.lfms.util.Dialogs;
import com.lfms.util.SceneNavigator;
import com.lfms.util.SessionManager;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin user-management screen: real-time search, activate/deactivate, delete and CSV export.
 */
public class AdminUsersController {

    @FXML private AdminSidebarController adminSidebarController;
    @FXML private TextField searchField;
    @FXML private TableView<User> table;
    @FXML private TableColumn<User, String> nameCol;
    @FXML private TableColumn<User, String> indexCol;
    @FXML private TableColumn<User, String> emailCol;
    @FXML private TableColumn<User, String> roleCol;
    @FXML private TableColumn<User, String> statusCol;
    @FXML private TableColumn<User, String> dateCol;
    @FXML private TableColumn<User, Void> actionsCol;

    private final UserService userService = new UserService();
    private final ExportService exportService = new ExportService();

    private final List<User> masterUsers = new ArrayList<>();

    @FXML
    public void initialize() {
        adminSidebarController.setActive("users");
        configureColumns();
        table.setPlaceholder(new Label("No users match your search."));
        searchField.textProperty().addListener((obs, oldText, newText) -> applyFilter(newText));
        reload();
    }

    private void configureColumns() {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        indexCol.setCellValueFactory(new PropertyValueFactory<>("indexNo"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        statusCol.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().isActive() ? "Active" : "Inactive"));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(null);
                if (empty || value == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(value);
                    badge.getStyleClass().add("Active".equals(value) ? "badge-approved" : "badge-rejected");
                    setGraphic(badge);
                }
            }
        });

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button toggle = new Button();
            private final Button delete = new Button("Delete");
            private final HBox box = new HBox(6, toggle, delete);
            {
                delete.getStyleClass().add("btn-danger");
                toggle.setOnAction(e -> toggleActive(current()));
                delete.setOnAction(e -> deleteUser(current()));
            }
            private User current() {
                return getTableView().getItems().get(getIndex());
            }
            @Override
            protected void updateItem(Void value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                User user = current();
                toggle.setText(user.isActive() ? "Deactivate" : "Activate");
                toggle.getStyleClass().setAll("button", user.isActive() ? "btn-secondary" : "btn-success");
                setGraphic(box);
            }
        });
    }

    private void reload() {
        masterUsers.clear();
        masterUsers.addAll(userService.findAll());
        applyFilter(searchField.getText());
    }

    private void applyFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        if (q.isEmpty()) {
            table.getItems().setAll(masterUsers);
            return;
        }
        List<User> filtered = new ArrayList<>();
        for (User user : masterUsers) {
            if (contains(user.getName(), q) || contains(user.getEmail(), q) || contains(user.getIndexNo(), q)) {
                filtered.add(user);
            }
        }
        table.getItems().setAll(filtered);
    }

    private void toggleActive(User user) {
        boolean newState = !user.isActive();
        if (userService.setActive(user.getUserId(), newState, adminId())) {
            reload();
        } else {
            Dialogs.error("Error", "Could not update the user's status.");
        }
    }

    private void deleteUser(User user) {
        if (user.getUserId() == adminId()) {
            Dialogs.error("Not Allowed", "You cannot delete your own account while logged in.");
            return;
        }
        if (!Dialogs.confirm("Delete User", "Permanently delete " + user.getName() + "?")) {
            return;
        }
        if (userService.delete(user.getUserId(), adminId())) {
            reload();
        } else {
            Dialogs.error("Could Not Delete",
                    "This user has existing reports or claims and cannot be deleted. Deactivate the account instead.");
        }
    }

    @FXML
    private void exportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Users to CSV");
        chooser.setInitialFileName("users.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showSaveDialog(SceneNavigator.getPrimaryStage());
        if (file == null) {
            return;
        }
        try {
            exportService.exportUsers(masterUsers, file);
            Dialogs.info("Export Complete",
                    "Exported " + masterUsers.size() + " user(s) to:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            Dialogs.error("Export Failed", e.getMessage());
        }
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private int adminId() {
        User admin = SessionManager.getInstance().getCurrentUser();
        return admin != null ? admin.getUserId() : -1;
    }
}
