package com.lfms.controller;

import com.lfms.model.Claim;
import com.lfms.model.Item;
import com.lfms.model.User;
import com.lfms.service.ClaimService;
import com.lfms.service.ItemService;
import com.lfms.service.UserService;
import com.lfms.util.Badges;
import com.lfms.util.Dialogs;
import com.lfms.util.ImageUtil;
import com.lfms.util.SceneNavigator;
import com.lfms.util.SessionManager;
import com.lfms.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Optional;

/**
 * Item detail screen with conditional Claim / Edit / Delete actions and an approved-claim
 * contact panel. Receives the item id via {@link SceneNavigator.DataReceiver}.
 */
public class ItemDetailController implements SceneNavigator.DataReceiver {

    @FXML private SidebarController sidebarController;
    @FXML private ImageView imageView;
    @FXML private Label nameLabel;
    @FXML private Label typeBadge;
    @FXML private Label statusBadge;
    @FXML private Label descLabel;
    @FXML private Label categoryLabel;
    @FXML private Label locationLabel;
    @FXML private Label dateLabel;
    @FXML private Label reporterLabel;
    @FXML private Button claimButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private VBox approvedPanel;
    @FXML private Label finderNameLabel;
    @FXML private Label finderEmailLabel;
    @FXML private Label finderPhoneLabel;

    private final ItemService itemService = new ItemService();
    private final UserService userService = new UserService();
    private final ClaimService claimService = new ClaimService();

    private int itemId;
    private Item item;

    @FXML
    public void initialize() {
        if (sidebarController != null) {
            sidebarController.setActive("browse");
        }
    }

    @Override
    public void receiveData(Object data) {
        if (data instanceof Integer id) {
            this.itemId = id;
            render();
        }
    }

    private void render() {
        item = itemService.findById(itemId);
        if (item == null) {
            nameLabel.setText("Item not found");
            return;
        }

        imageView.setImage(ImageUtil.loadImage(item.getImagePath()));
        nameLabel.setText(item.getName());
        applyBadge(typeBadge, item.getType());
        applyBadge(statusBadge, item.getStatus());
        descLabel.setText(item.getDescription());
        categoryLabel.setText(item.getCategory());
        locationLabel.setText(nz(item.getLocation(), "Not specified"));
        dateLabel.setText(nz(item.getDateReported(), "Not specified"));

        User reporter = userService.findById(item.getUserId());
        reporterLabel.setText(reporter != null ? reporter.getName() : "Unknown");

        configureButtons();
        configureApprovedPanel(reporter);
    }

    private void configureButtons() {
        User current = SessionManager.getInstance().getCurrentUser();
        int uid = current != null ? current.getUserId() : -1;
        boolean owner = item.getUserId() == uid;

        setVisible(claimButton, item.isFound() && item.isOpen() && !owner);
        setVisible(editButton, owner && item.isOpen());
        setVisible(deleteButton, owner && item.isOpen());
    }

    private void configureApprovedPanel(User reporter) {
        User current = SessionManager.getInstance().getCurrentUser();
        int uid = current != null ? current.getUserId() : -1;
        boolean show = false;

        if (item.isFound() && Item.STATUS_APPROVED.equals(item.getStatus())) {
            for (Claim claim : claimService.findByItem(item.getItemId())) {
                if (Claim.STATUS_APPROVED.equals(claim.getStatus()) && claim.getClaimantId() == uid) {
                    finderNameLabel.setText("Name:  " + (reporter != null ? reporter.getName() : "Unknown"));
                    finderEmailLabel.setText("Email:  " + (reporter != null ? nz(reporter.getEmail(), "—") : "—"));
                    finderPhoneLabel.setText("Phone:  " + (reporter != null ? nz(reporter.getPhone(), "Not provided") : "—"));
                    show = true;
                    break;
                }
            }
        }
        setVisible(approvedPanel, show);
    }

    @FXML
    private void claimItem() {
        SceneNavigator.navigateTo("/com/lfms/fxml/ClaimForm.fxml", item.getItemId());
    }

    @FXML
    private void editItem() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Item");
        dialog.setHeaderText("Update the details of your report");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField = new TextField(item.getName());
        nameField.setPrefWidth(280);
        ComboBox<String> categoryField = new ComboBox<>();
        categoryField.getItems().setAll(Item.CATEGORIES);
        categoryField.setValue(item.getCategory());
        categoryField.setMaxWidth(Double.MAX_VALUE);
        TextArea descField = new TextArea(item.getDescription());
        descField.setPrefRowCount(4);
        descField.setWrapText(true);
        TextField locationField = new TextField(item.getLocation());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.addRow(0, new Label("Name"), nameField);
        grid.addRow(1, new Label("Category"), categoryField);
        grid.addRow(2, new Label("Description"), descField);
        grid.addRow(3, new Label("Location"), locationField);
        dialog.getDialogPane().setContent(grid);
        applyStylesheet(dialog);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        if (!ValidationUtil.isNotEmpty(nameField.getText()) || categoryField.getValue() == null
                || !ValidationUtil.isNotEmpty(descField.getText())) {
            Dialogs.error("Invalid Input", "Name, category and description are required.");
            return;
        }

        item.setName(nameField.getText().trim());
        item.setCategory(categoryField.getValue());
        item.setDescription(descField.getText().trim());
        item.setLocation(ValidationUtil.isNotEmpty(locationField.getText()) ? locationField.getText().trim() : null);

        if (itemService.updateItem(item)) {
            Dialogs.info("Saved", "Your report has been updated.");
            render();
        } else {
            Dialogs.error("Error", "Could not update the item.");
        }
    }

    @FXML
    private void deleteItem() {
        boolean confirmed = Dialogs.confirm("Delete Report",
                "Delete this report permanently? Any claims and matches linked to it will also be removed.");
        if (!confirmed) {
            return;
        }
        User current = SessionManager.getInstance().getCurrentUser();
        int uid = current != null ? current.getUserId() : -1;
        if (itemService.deleteItem(item.getItemId(), uid)) {
            Dialogs.info("Deleted", "Your report has been deleted.");
            SceneNavigator.navigateTo("/com/lfms/fxml/MyReports.fxml");
        } else {
            Dialogs.error("Error", "Could not delete the report.");
        }
    }

    @FXML
    private void goBack() {
        SceneNavigator.navigateTo("/com/lfms/fxml/Browse.fxml");
    }

    private void applyBadge(Label label, String value) {
        label.getStyleClass().removeIf(s -> s.startsWith("badge-"));
        label.setText(Badges.prettify(value));
        label.getStyleClass().add(Badges.styleFor(value));
    }

    private void applyStylesheet(Dialog<?> dialog) {
        URL css = getClass().getResource("/com/lfms/css/main.css");
        if (css != null) {
            dialog.getDialogPane().getStylesheets().add(css.toExternalForm());
        }
    }

    private void setVisible(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private String nz(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
