package com.lfms.controller;

import com.lfms.model.Claim;
import com.lfms.model.Item;
import com.lfms.model.User;
import com.lfms.service.ClaimService;
import com.lfms.service.ItemService;
import com.lfms.util.Dialogs;
import com.lfms.util.ImageUtil;
import com.lfms.util.SceneNavigator;
import com.lfms.util.SessionManager;
import com.lfms.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

/**
 * Full-screen claim submission form. Guards against duplicate pending claims and
 * unavailable items, and enforces minimum-length descriptions.
 */
public class ClaimFormController implements SceneNavigator.DataReceiver {

    private static final int MIN_LENGTH = 30;

    @FXML private SidebarController sidebarController;
    @FXML private ImageView thumbnail;
    @FXML private Label itemNameLabel;
    @FXML private VBox guardPanel;
    @FXML private Label guardTitle;
    @FXML private Label guardMessage;
    @FXML private VBox formBox;
    @FXML private TextArea featuresArea;
    @FXML private TextArea proofArea;
    @FXML private TextField contactField;
    @FXML private Label featuresError;
    @FXML private Label proofError;
    @FXML private Label imageNameLabel;
    @FXML private javafx.scene.layout.StackPane imagePreviewFrame;
    @FXML private ImageView proofImageView;

    private final ItemService itemService = new ItemService();
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
            load();
        }
    }

    private void load() {
        item = itemService.findById(itemId);
        User user = SessionManager.getInstance().getCurrentUser();

        if (item == null) {
            thumbnail.setImage(ImageUtil.loadThumbnail(null, 80, 80));
            itemNameLabel.setText("Unknown item");
            showGuard("Item unavailable", "This item could not be found.");
            return;
        }

        thumbnail.setImage(ImageUtil.loadThumbnail(item.getImagePath(), 80, 80));
        itemNameLabel.setText(item.getName());

        if (!(item.isFound() && item.isOpen())) {
            showGuard("Claim not available",
                    "This item is no longer open for claims.");
            return;
        }
        if (user != null && claimService.hasPendingClaim(itemId, user.getUserId())) {
            showGuard("Claim already submitted",
                    "You already submitted a claim for this item. Check My Reports for status.");
            return;
        }
        showForm();
    }

    private String selectedImagePath = null;

    @FXML
    private void handleChooseImage() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Select Proof Image");
        chooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        java.io.File file = chooser.showOpenDialog(SceneNavigator.getPrimaryStage());
        if (file != null) {
            selectedImagePath = file.getAbsolutePath();
            imageNameLabel.setText(file.getName());
            proofImageView.setImage(new javafx.scene.image.Image(file.toURI().toString()));
            imagePreviewFrame.setVisible(true);
            imagePreviewFrame.setManaged(true);
        }
    }

    @FXML
    private void handleSubmit() {
        ValidationUtil.clearAllErrors(featuresError, proofError);

        boolean valid = true;
        if (!ValidationUtil.isMinLength(featuresArea.getText(), MIN_LENGTH)) {
            ValidationUtil.showError(featuresError,
                    "Please provide at least " + MIN_LENGTH + " characters describing the item's features.");
            valid = false;
        }
        if (!ValidationUtil.isMinLength(proofArea.getText(), MIN_LENGTH)) {
            ValidationUtil.showError(proofError,
                    "Please provide at least " + MIN_LENGTH + " characters of proof.");
            valid = false;
        }
        if (!valid) {
            return;
        }

        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        String proof = proofArea.getText().trim();
        if (ValidationUtil.isNotEmpty(contactField.getText())) {
            proof = proof + "\n\nPreferred contact: " + contactField.getText().trim();
        }

        String savedImagePath = null;
        if (selectedImagePath != null) {
            try {
                savedImagePath = ImageUtil.copyImageToStorage(new java.io.File(selectedImagePath));
            } catch (RuntimeException e) {
                Dialogs.error("Image Error", "Could not save the image: " + e.getMessage());
                return;
            }
        }

        Claim claim = new Claim();
        claim.setItemId(itemId);
        claim.setClaimantId(user.getUserId());
        claim.setFeaturesDesc(featuresArea.getText().trim());
        claim.setProofDesc(proof);
        claim.setProofImage(savedImagePath);
        claim.setStatus(Claim.STATUS_PENDING);

        try {
            if (claimService.submitClaim(claim)) {
                Dialogs.info("Claim Submitted",
                        "Your claim has been submitted and is now pending review by an administrator.");
                SceneNavigator.navigateTo("/com/lfms/fxml/Browse.fxml");
            } else {
                Dialogs.error("Error", "Your claim could not be submitted. Please try again.");
            }
        } catch (RuntimeException e) {
            Dialogs.error("Error", "Your claim could not be submitted:\n" + e.getMessage());
        }
    }

    @FXML
    private void cancel() {
        SceneNavigator.navigateTo("/com/lfms/fxml/Browse.fxml");
    }

    private void showGuard(String title, String message) {
        guardTitle.setText(title);
        guardMessage.setText(message);
        setVisible(guardPanel, true);
        setVisible(formBox, false);
    }

    private void showForm() {
        setVisible(guardPanel, false);
        setVisible(formBox, true);
    }

    private void setVisible(VBox node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
