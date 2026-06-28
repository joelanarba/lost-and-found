package com.lfms.controller.admin;

import com.lfms.model.Claim;
import com.lfms.model.Item;
import com.lfms.model.User;
import com.lfms.service.ClaimService;
import com.lfms.service.ItemService;
import com.lfms.util.Dialogs;
import com.lfms.util.ImageUtil;
import com.lfms.util.SessionManager;
import com.lfms.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

/**
 * Admin screen for reviewing pending claims and approving or rejecting them.
 */
public class AdminClaimsController {

    @FXML private AdminSidebarController adminSidebarController;
    @FXML private TableView<Claim> claimsTable;
    @FXML private TableColumn<Claim, Integer> idCol;
    @FXML private TableColumn<Claim, String> itemCol;
    @FXML private TableColumn<Claim, String> claimantCol;
    @FXML private TableColumn<Claim, String> indexCol;
    @FXML private TableColumn<Claim, String> dateCol;
    @FXML private TableColumn<Claim, Void> actionsCol;

    @FXML private Label reviewPlaceholder;
    @FXML private VBox reviewContent;
    @FXML private ImageView reviewImage;
    @FXML private ImageView proofImageView;
    @FXML private Label reviewItemName;
    @FXML private Label reviewItemLocation;
    @FXML private Label reviewItemDesc;
    @FXML private Label reviewClaimant;
    @FXML private Label reviewFeatures;
    @FXML private Label reviewProof;
    @FXML private TextField reasonField;
    @FXML private Label reasonError;

    private final ClaimService claimService = new ClaimService();
    private final ItemService itemService = new ItemService();

    private Claim selectedClaim;

    @FXML
    public void initialize() {
        adminSidebarController.setActive("claims");

        idCol.setCellValueFactory(new PropertyValueFactory<>("claimId"));
        itemCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        claimantCol.setCellValueFactory(new PropertyValueFactory<>("claimantName"));
        indexCol.setCellValueFactory(new PropertyValueFactory<>("claimantIndexNo"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button review = new Button("Review");
            {
                review.getStyleClass().add("btn-secondary");
                review.setOnAction(e -> review(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void value, boolean empty) {
                super.updateItem(value, empty);
                setGraphic(empty ? null : review);
            }
        });

        claimsTable.setPlaceholder(new Label("There are no pending claims to review."));
        refresh();
    }

    private void refresh() {
        claimsTable.getItems().setAll(claimService.findPending());
        showPlaceholder();
    }

    private void review(Claim claim) {
        selectedClaim = claim;
        ValidationUtil.clearError(reasonError);
        reasonField.clear();

        Item item = itemService.findById(claim.getItemId());
        reviewImage.setImage(ImageUtil.loadThumbnail(item.getImagePath(), 150, 150));
        proofImageView.setImage(ImageUtil.loadThumbnail(claim.getProofImage(), 150, 150));
        reviewItemName.setText(item.getName());
        reviewItemLocation.setText(item.getLocation() == null ? "Not specified" : item.getLocation());
        reviewItemDesc.setText(item.getDescription());

        reviewClaimant.setText(claim.getClaimantName()
                + "  (" + nz(claim.getClaimantIndexNo()) + ")\n"
                + nz(claim.getClaimantEmail())
                + (claim.getClaimantPhone() != null ? "  •  " + claim.getClaimantPhone() : ""));
        reviewFeatures.setText(claim.getFeaturesDesc());
        reviewProof.setText(claim.getProofDesc());

        reviewPlaceholder.setVisible(false);
        reviewPlaceholder.setManaged(false);
        reviewContent.setVisible(true);
        reviewContent.setManaged(true);
    }

    @FXML
    private void approve() {
        if (selectedClaim == null) {
            return;
        }
        if (!Dialogs.confirm("Approve Claim",
                "Approve this claim and mark the item as awaiting collection?")) {
            return;
        }
        if (claimService.approveClaim(selectedClaim.getClaimId(), adminId())) {
            Dialogs.info("Claim Approved", "The claim has been approved.");
            refresh();
        } else {
            Dialogs.error("Error", "Could not approve the claim.");
        }
    }

    @FXML
    private void reject() {
        if (selectedClaim == null) {
            return;
        }
        ValidationUtil.clearError(reasonError);
        String reason = reasonField.getText();
        if (!ValidationUtil.isNotEmpty(reason)) {
            ValidationUtil.showError(reasonError, "A reason is required to reject a claim.");
            return;
        }
        if (claimService.rejectClaim(selectedClaim.getClaimId(), reason.trim(), adminId())) {
            Dialogs.info("Claim Rejected", "The claim has been rejected.");
            refresh();
        } else {
            Dialogs.error("Error", "Could not reject the claim.");
        }
    }

    private void showPlaceholder() {
        selectedClaim = null;
        reviewContent.setVisible(false);
        reviewContent.setManaged(false);
        reviewPlaceholder.setVisible(true);
        reviewPlaceholder.setManaged(true);
    }

    private int adminId() {
        User admin = SessionManager.getInstance().getCurrentUser();
        return admin != null ? admin.getUserId() : -1;
    }

    private String nz(String value) {
        return value == null ? "" : value;
    }
}
