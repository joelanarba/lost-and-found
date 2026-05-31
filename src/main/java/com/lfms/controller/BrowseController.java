package com.lfms.controller;

import com.lfms.model.Item;
import com.lfms.service.ItemService;
import com.lfms.util.Badges;
import com.lfms.util.ImageUtil;
import com.lfms.util.SceneNavigator;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Browse / search screen listing lost and found items with filters.
 */
public class BrowseController {

    @FXML private SidebarController sidebarController;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeBox;
    @FXML private ComboBox<String> categoryBox;
    @FXML private TextField locationField;
    @FXML private TableView<Item> table;
    @FXML private TableColumn<Item, Item> imageCol;
    @FXML private TableColumn<Item, String> nameCol;
    @FXML private TableColumn<Item, String> categoryCol;
    @FXML private TableColumn<Item, String> typeCol;
    @FXML private TableColumn<Item, String> statusCol;
    @FXML private TableColumn<Item, String> locationCol;
    @FXML private TableColumn<Item, String> dateCol;
    @FXML private TableColumn<Item, Void> actionsCol;

    private final ItemService itemService = new ItemService();

    @FXML
    public void initialize() {
        sidebarController.setActive("browse");

        typeBox.getItems().setAll("All", "Lost Items", "Found Items");
        typeBox.setValue("All");

        List<String> categories = new ArrayList<>();
        categories.add("All");
        categories.addAll(Item.CATEGORIES);
        categoryBox.getItems().setAll(categories);
        categoryBox.setValue("All");

        configureColumns();
        Label placeholder = new Label("No items found matching your search.");
        placeholder.getStyleClass().add("table-placeholder-text");
        table.setPlaceholder(placeholder);

        doSearch();
    }

    private void configureColumns() {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("dateReported"));

        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setCellFactory(Badges.badgeCellFactory());
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(Badges.badgeCellFactory());

        imageCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        imageCol.setCellFactory(col -> new TableCell<>() {
            private final ImageView view = new ImageView();
            {
                view.setFitWidth(48);
                view.setFitHeight(48);
                view.setPreserveRatio(true);
            }
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    view.setImage(ImageUtil.loadThumbnail(item.getImagePath(), 48, 48));
                    setGraphic(view);
                }
            }
        });

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("View");
            {
                viewButton.getStyleClass().add("btn-secondary");
                viewButton.setOnAction(e -> {
                    Item item = getTableView().getItems().get(getIndex());
                    SceneNavigator.navigateTo("/com/lfms/fxml/ItemDetail.fxml", item.getItemId());
                });
            }
            @Override
            protected void updateItem(Void value, boolean empty) {
                super.updateItem(value, empty);
                setGraphic(empty ? null : viewButton);
            }
        });
    }

    @FXML
    private void handleSearch() {
        doSearch();
    }

    @FXML
    private void handleClear() {
        searchField.clear();
        locationField.clear();
        typeBox.setValue("All");
        categoryBox.setValue("All");
        doSearch();
    }

    private void doSearch() {
        String type = mapType(typeBox.getValue());
        String category = "All".equals(categoryBox.getValue()) ? null : categoryBox.getValue();
        List<Item> items = itemService.search(searchField.getText(), type, category,
                locationField.getText(), null, null);
        table.getItems().setAll(items);
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
}
