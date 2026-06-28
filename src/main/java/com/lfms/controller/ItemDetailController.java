package com.lfms.controller;

import com.lfms.model.Claim;
import com.lfms.model.Item;
import com.lfms.model.TimelineEvent;
import com.lfms.model.User;
import com.lfms.service.ClaimService;
import com.lfms.service.ItemService;
import com.lfms.service.TimelineService;
import com.lfms.service.UserService;
import com.lfms.util.Badges;
import com.lfms.util.Dialogs;
import com.lfms.util.ImageUtil;
import com.lfms.util.QrCodeUtil;
import com.lfms.util.SceneNavigator;
import com.lfms.util.SessionManager;
import com.lfms.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
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
    @FXML private Button printFlyerButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private VBox approvedPanel;
    @FXML private Label finderNameLabel;
    @FXML private Label finderEmailLabel;
    @FXML private Label finderPhoneLabel;
    @FXML private VBox qrPanel;
    @FXML private ImageView qrImageView;
    @FXML private Button saveQrButton;
    @FXML private VBox timelineBox;
    @FXML private HBox timelineRows;

    private final ItemService itemService = new ItemService();
    private final UserService userService = new UserService();
    private final ClaimService claimService = new ClaimService();
    private final TimelineService timelineService = new TimelineService();

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
        configureQr();
        loadTimeline();
    }

    private void configureButtons() {
        User current = SessionManager.getInstance().getCurrentUser();
        int uid = current != null ? current.getUserId() : -1;
        boolean owner = item.getUserId() == uid;

        setVisible(claimButton, item.isFound() && item.isOpen() && !owner);
        setVisible(printFlyerButton, item.isLost());
        setVisible(editButton, owner && item.isOpen());
        setVisible(deleteButton, owner && item.isOpen());
    }

    /** Found items show a QR code (encoding LFMS-ITEM-{id}); lost items hide the panel. */
    private void configureQr() {
        User current = SessionManager.getInstance().getCurrentUser();
        int uid = current != null ? current.getUserId() : -1;
        
        // If this is an approved claim for the current user, show the Claim QR
        if (item.isFound() && Item.STATUS_APPROVED.equals(item.getStatus())) {
            for (Claim claim : claimService.findByItem(item.getItemId())) {
                if (Claim.STATUS_APPROVED.equals(claim.getStatus()) && claim.getClaimantId() == uid) {
                    Path qr = QrCodeUtil.ensureForClaim(claim.getClaimId());
                    if (qr != null && Files.exists(qr)) {
                        qrImageView.setImage(new Image(qr.toUri().toString(), 170, 170, true, true, true));
                        
                        // Add Download Certificate Button if it doesn't exist
                        boolean hasCertBtn = qrPanel.getChildren().stream().anyMatch(n -> n instanceof javafx.scene.control.Button && ((javafx.scene.control.Button)n).getText().equals("Download Certificate"));
                        if (!hasCertBtn) {
                            javafx.scene.control.Button certBtn = new javafx.scene.control.Button("Download Certificate");
                            certBtn.getStyleClass().add("btn-primary");
                            certBtn.setOnAction(e -> {
                                Path cert = com.lfms.service.PdfReportService.generateCertificate(claim, item);
                                if (cert != null) {
                                    com.lfms.util.Dialogs.info("Certificate Downloaded", "Saved to Desktop: " + cert.getFileName());
                                }
                            });
                            qrPanel.getChildren().add(certBtn);
                        }
                        
                        setVisible(qrPanel, true);
                        return;
                    }
                }
            }
        }

        // Otherwise, if it's a found item, show the Item QR
        if (item.isFound()) {
            Path qr = QrCodeUtil.ensureForItem(item.getItemId());
            if (qr != null && Files.exists(qr)) {
                qrImageView.setImage(new Image(qr.toUri().toString(), 170, 170, true, true, true));
                setVisible(qrPanel, true);
                return;
            }
        }
        setVisible(qrPanel, false);
    }

    @FXML
    private void saveQrCode() {
        if (item == null || !item.isFound()) {
            return;
        }
        Path qr = QrCodeUtil.ensureForItem(item.getItemId());
        if (qr == null || !Files.exists(qr)) {
            Dialogs.error("QR Code", "The QR code could not be generated.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save QR Code");
        chooser.setInitialFileName("lfms-item-" + item.getItemId() + "-qr.png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        File dest = chooser.showSaveDialog(SceneNavigator.getPrimaryStage());
        if (dest == null) {
            return;
        }
        try {
            Files.copy(qr, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Dialogs.info("Saved", "QR code saved to:\n" + dest.getAbsolutePath());
        } catch (IOException e) {
            Dialogs.error("Save Failed", e.getMessage());
        }
    }

    @FXML
    private void printFlyer() {
        if (item == null || !item.isLost()) {
            return;
        }
        SceneNavigator.openModal("/com/lfms/fxml/Flyer.fxml",
                "Lost Item Flyer — " + item.getName(), item, 600, 800);
    }

    private void loadTimeline() {
        timelineRows.getChildren().clear();
        List<TimelineEvent> events = timelineService.build(item.getItemId());
        for (int i = 0; i < events.size(); i++) {
            timelineRows.getChildren().add(buildTimelineNode(events.get(i)));
            if (i < events.size() - 1) {
                Label arrow = new Label("→");
                arrow.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 16px; -fx-padding: 0 10 0 10;");
                timelineRows.getChildren().add(arrow);
            }
        }
        setVisible(timelineBox, !events.isEmpty());
    }

    private VBox buildTimelineNode(TimelineEvent event) {
        Label dot = new Label(event.icon());
        dot.setStyle("-fx-font-size: 24px;");
        Label title = new Label(event.title());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Label date = new Label(event.date());
        date.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px;");

        VBox node = new VBox(4.0, dot, title, date);
        node.setAlignment(Pos.CENTER);
        return node;
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
