package com.lfms.controller;

import com.lfms.model.Claim;
import com.lfms.model.Item;
import com.lfms.model.User;
import com.lfms.service.ClaimService;
import com.lfms.service.ItemService;
import com.lfms.util.Badges;
import com.lfms.util.Dialogs;
import com.lfms.util.ImageUtil;
import com.lfms.util.SceneNavigator;
import com.lfms.util.SessionManager;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;

/**
 * "My Reports" screen — the current user's lost items, found items and claims.
 */
public class MyReportsController {

    @FXML private SidebarController sidebarController;

    @FXML private TableView<Item> lostTable;
    @FXML private TableColumn<Item, Item> lostImageCol;
    @FXML private TableColumn<Item, String> lostNameCol;
    @FXML private TableColumn<Item, String> lostCategoryCol;
    @FXML private TableColumn<Item, String> lostStatusCol;
    @FXML private TableColumn<Item, String> lostDateCol;
    @FXML private TableColumn<Item, Void> lostActionsCol;

    @FXML private TableView<Item> foundTable;
    @FXML private TableColumn<Item, Item> foundImageCol;
    @FXML private TableColumn<Item, String> foundNameCol;
    @FXML private TableColumn<Item, String> foundCategoryCol;
    @FXML private TableColumn<Item, String> foundStatusCol;
    @FXML private TableColumn<Item, String> foundDateCol;
    @FXML private TableColumn<Item, Void> foundActionsCol;

    @FXML private TableView<Claim> claimsTable;
    @FXML private TableColumn<Claim, String> claimItemCol;
    @FXML private TableColumn<Claim, String> claimDateCol;
    @FXML private TableColumn<Claim, String> claimStatusCol;
    @FXML private TableColumn<Claim, String> claimNoteCol;
    @FXML private TableColumn<Claim, Void> claimActionsCol;

    private final ItemService itemService = new ItemService();
    private final ClaimService claimService = new ClaimService();

    @FXML
    public void initialize() {
        sidebarController.setActive("myreports");

        configureItemColumns(lostImageCol, lostNameCol, lostCategoryCol, lostStatusCol, lostDateCol,
                lostActionsCol, false);
        configureItemColumns(foundImageCol, foundNameCol, foundCategoryCol, foundStatusCol, foundDateCol,
                foundActionsCol, true);
        configureClaimColumns();

        lostTable.setPlaceholder(placeholder("You haven't reported any lost items yet."));
        foundTable.setPlaceholder(placeholder("You haven't reported any found items yet."));
        claimsTable.setPlaceholder(placeholder("You haven't submitted any claims yet."));

        refresh();
    }

    private void refresh() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        List<Item> lost = new ArrayList<>();
        List<Item> found = new ArrayList<>();
        for (Item item : itemService.findByUser(user.getUserId())) {
            if (item.isLost()) {
                lost.add(item);
            } else {
                found.add(item);
            }
        }
        lostTable.getItems().setAll(lost);
        foundTable.getItems().setAll(found);
        claimsTable.getItems().setAll(claimService.findByClaimant(user.getUserId()));
    }

    private void configureItemColumns(TableColumn<Item, Item> imageCol, TableColumn<Item, String> nameCol,
                                      TableColumn<Item, String> categoryCol, TableColumn<Item, String> statusCol,
                                      TableColumn<Item, String> dateCol, TableColumn<Item, Void> actionsCol,
                                      boolean foundTab) {
        imageCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        imageCol.setCellFactory(col -> new TableCell<>() {
            private final ImageView view = new ImageView();
            {
                view.setFitWidth(44);
                view.setFitHeight(44);
                view.setPreserveRatio(true);
            }
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    view.setImage(ImageUtil.loadThumbnail(item.getImagePath(), 44, 44));
                    setGraphic(view);
                }
            }
        });
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(Badges.badgeCellFactory());
        dateCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        actionsCol.setCellFactory(itemActionsFactory(foundTab));
    }

    private Callback<TableColumn<Item, Void>, TableCell<Item, Void>> itemActionsFactory(boolean foundTab) {
        return col -> new TableCell<>() {
            private final Button view = new Button("View");
            private final Button delete = new Button("Delete");
            private final Button markReturned = new Button("Mark Returned");
            private final HBox box = new HBox(6);
            {
                view.getStyleClass().add("btn-secondary");
                delete.getStyleClass().add("btn-danger");
                markReturned.getStyleClass().add("btn-success");
                view.setOnAction(e -> openDetail(current().getItemId()));
                delete.setOnAction(e -> deleteItem(current()));
                markReturned.setOnAction(e -> markReturned(current()));
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
                Item item = current();
                box.getChildren().setAll(view);
                if (Item.STATUS_OPEN.equals(item.getStatus())) {
                    box.getChildren().add(delete);
                }
                if (foundTab && Item.STATUS_APPROVED.equals(item.getStatus())) {
                    box.getChildren().add(markReturned);
                }
                setGraphic(box);
            }
        };
    }

    private void configureClaimColumns() {
        claimItemCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        claimDateCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        claimStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        claimStatusCol.setCellFactory(Badges.badgeCellFactory());
        claimNoteCol.setCellValueFactory(new PropertyValueFactory<>("adminNote"));
        claimActionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button view = new Button("View Item");
            {
                view.getStyleClass().add("btn-secondary");
                view.setOnAction(e -> {
                    Claim claim = getTableView().getItems().get(getIndex());
                    openDetail(claim.getItemId());
                });
            }
            @Override
            protected void updateItem(Void value, boolean empty) {
                super.updateItem(value, empty);
                setGraphic(empty ? null : view);
            }
        });
    }

    private void openDetail(int itemId) {
        SceneNavigator.navigateTo("/com/lfms/fxml/ItemDetail.fxml", itemId);
    }

    private void deleteItem(Item item) {
        if (!Dialogs.confirm("Delete Report", "Delete \"" + item.getName() + "\" permanently?")) {
            return;
        }
        User user = SessionManager.getInstance().getCurrentUser();
        int uid = user != null ? user.getUserId() : -1;
        if (itemService.deleteItem(item.getItemId(), uid)) {
            refresh();
        } else {
            Dialogs.error("Error", "Could not delete the report.");
        }
    }

    private void markReturned(Item item) {
        if (!Dialogs.confirm("Mark as Returned", "Mark \"" + item.getName() + "\" as returned to its owner?")) {
            return;
        }
        User user = SessionManager.getInstance().getCurrentUser();
        int uid = user != null ? user.getUserId() : -1;
        if (itemService.updateStatus(item.getItemId(), Item.STATUS_RESOLVED, uid)) {
            refresh();
        } else {
            Dialogs.error("Error", "Could not update the item status.");
        }
    }

    private Label placeholder(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("table-placeholder-text");
        return label;
    }
}
