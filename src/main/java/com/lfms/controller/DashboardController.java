package com.lfms.controller;

import com.lfms.model.Item;
import com.lfms.model.Match;
import com.lfms.model.User;
import com.lfms.service.ClaimService;
import com.lfms.service.ItemService;
import com.lfms.util.MatchBreakdownView;
import com.lfms.util.SceneNavigator;
import com.lfms.util.SessionManager;
import com.lfms.util.StatFormat;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Student dashboard: open-report counts, pending-claim count and suggested matches.
 */
public class DashboardController {

    @FXML private SidebarController sidebarController;
    @FXML private Label welcomeLabel;
    @FXML private Label openLostLabel;
    @FXML private Label openFoundLabel;
    @FXML private Label pendingClaimsLabel;
    @FXML private Label recoveryRateLabel;
    @FXML private Label topCategoryLabel;
    @FXML private FlowPane matchesPane;
    @FXML private Label emptyMatchesLabel;

    private final ItemService itemService = new ItemService();
    private final ClaimService claimService = new ClaimService();

    @FXML
    public void initialize() {
        sidebarController.setActive("dashboard");
        User user = SessionManager.getInstance().getCurrentUser();
        welcomeLabel.setText("Welcome back, " + (user != null ? user.getName() : "") + ".");
        loadStats(user);
        loadMatches(user);
    }

    private void loadStats(User user) {
        openLostLabel.setText(String.valueOf(itemService.countOpenByType(Item.TYPE_LOST)));
        openFoundLabel.setText(String.valueOf(itemService.countOpenByType(Item.TYPE_FOUND)));
        int pending = user != null ? claimService.countPendingByClaimant(user.getUserId()) : 0;
        pendingClaimsLabel.setText(String.valueOf(pending));

        recoveryRateLabel.setText(StatFormat.recoveryRate(itemService.recoveryRate()));
        topCategoryLabel.setText(StatFormat.mostLostCategory(
                itemService.topLostCategory(), itemService.countByType(Item.TYPE_LOST)));
    }

    private void loadMatches(User user) {
        matchesPane.getChildren().clear();
        List<Match> matches = user != null ? itemService.getMatchesForUser(user.getUserId()) : List.of();
        boolean empty = matches.isEmpty();
        emptyMatchesLabel.setVisible(empty);
        emptyMatchesLabel.setManaged(empty);
        if (empty) {
            return;
        }
        int currentUserId = user.getUserId();
        for (Match match : matches) {
            matchesPane.getChildren().add(buildMatchCard(match, currentUserId));
        }
    }

    private Node buildMatchCard(Match match, int currentUserId) {
        VBox card = new VBox(8.0);
        card.getStyleClass().add("match-card");

        Label badge = new Label("Match Confidence: " + match.getConfidence() + " (" + match.getScore() + "/10)");
        badge.getStyleClass().add(confidenceClass(match.getConfidence()));

        Label lost = new Label("LOST:  " + match.getLostItemName());
        lost.getStyleClass().add("detail-value");
        lost.setWrapText(true);

        Label arrow = new Label("↕  possible match");
        arrow.getStyleClass().add("match-arrow");

        Label found = new Label("FOUND:  " + match.getFoundItemName());
        found.getStyleClass().add("detail-value");
        found.setWrapText(true);

        // Visual breakdown panel explaining the score instead of a bare number.
        VBox breakdown = MatchBreakdownView.build(match.getBreakdown());

        Button view = new Button("View Details");
        view.getStyleClass().add("btn-secondary");
        view.setOnAction(e -> openDetail(match, currentUserId));

        card.getChildren().addAll(badge, lost, arrow, found, breakdown, view);
        return card;
    }

    private void openDetail(Match match, int currentUserId) {
        Item found = itemService.findById(match.getFoundItemId());
        int targetId = (found != null && found.getUserId() == currentUserId)
                ? match.getLostItemId()
                : match.getFoundItemId();
        SceneNavigator.navigateTo("/com/lfms/fxml/ItemDetail.fxml", targetId);
    }

    private String confidenceClass(String confidence) {
        return switch (confidence) {
            case "HIGH" -> "badge-high";
            case "MEDIUM" -> "badge-medium";
            default -> "badge-low";
        };
    }

    @FXML
    private void goToBrowseLost() {
        SceneNavigator.navigateTo("/com/lfms/fxml/Browse.fxml");
    }

    @FXML
    private void goToBrowseFound() {
        SceneNavigator.navigateTo("/com/lfms/fxml/Browse.fxml");
    }

    @FXML
    private void goToMyClaims() {
        SceneNavigator.navigateTo("/com/lfms/fxml/MyReports.fxml");
    }
}
