package com.lfms.controller;

import com.lfms.model.Item;
import com.lfms.service.ItemService;
import com.lfms.util.Badges;
import com.lfms.util.ImageUtil;
import com.lfms.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * Browse / search screen. Lost and found items are shown as a responsive grid of image
 * cards rather than a dense table, so the listing reads like a marketplace.
 */
public class BrowseController {

    @FXML private SidebarController sidebarController;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeBox;
    @FXML private HBox categoryTags;
    @FXML private TextField locationField;
    @FXML private FlowPane grid;
    @FXML private Label emptyLabel;
    @FXML private Label resultCountLabel;

    private final ItemService itemService = new ItemService();
    private String activeCategory = "All";

    @FXML
    public void initialize() {
        sidebarController.setActive("browse");

        typeBox.getItems().setAll("All", "Lost Items", "Found Items");
        typeBox.setValue("All");

        buildCategoryTags();
        doSearch();
    }

    private void buildCategoryTags() {
        categoryTags.getChildren().clear();
        List<String> categories = new ArrayList<>();
        categories.add("All");
        categories.addAll(Item.CATEGORIES);

        for (String cat : categories) {
            javafx.scene.control.ToggleButton btn = new javafx.scene.control.ToggleButton(cat);
            btn.getStyleClass().add("tag-pill");
            if (cat.equals(activeCategory)) {
                btn.setSelected(true);
            }
            btn.setOnAction(e -> {
                activeCategory = cat;
                buildCategoryTags(); // refresh styles
                doSearch();
            });
            categoryTags.getChildren().add(btn);
        }
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
        activeCategory = "All";
        buildCategoryTags();
        doSearch();
    }

    private void doSearch() {
        String type = mapType(typeBox.getValue());
        String category = "All".equals(activeCategory) ? null : activeCategory;
        List<Item> items = itemService.search(searchField.getText(), type, category,
                locationField.getText(), null, null);

        grid.getChildren().clear();
        for (Item item : items) {
            grid.getChildren().add(buildCard(item));
        }

        boolean empty = items.isEmpty();
        emptyLabel.setVisible(empty);
        emptyLabel.setManaged(empty);
        resultCountLabel.setText(items.size() + (items.size() == 1 ? " item" : " items"));
    }

    private Node buildCard(Item item) {
        VBox card = new VBox();
        card.getStyleClass().add("item-card");

        // --- Image header with a type badge overlaid ---
        ImageView image = new ImageView(ImageUtil.loadThumbnail(item.getImagePath(), 240, 150));
        image.setFitWidth(240);
        image.setFitHeight(150);
        image.setPreserveRatio(false);
        image.setSmooth(true);
        Rectangle clip = new Rectangle(240, 150);
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        image.setClip(clip);

        Label typeBadge = Badges.label(item.getType());
        StackPane.setAlignment(typeBadge, Pos.TOP_LEFT);
        StackPane.setMargin(typeBadge, new javafx.geometry.Insets(10));

        StackPane imageWrap = new StackPane(image, typeBadge);
        imageWrap.getStyleClass().add("item-card-image-wrap");

        // --- Body ---
        Label title = new Label(item.getName());
        title.getStyleClass().add("item-card-title");
        title.setWrapText(true);
        title.setMaxWidth(210);

        Label status = Badges.label(item.getStatus());

        Label location = new Label("📍  " + safe(item.getLocation()));
        location.getStyleClass().add("item-card-loc");

        Label date = new Label("Reported " + safe(item.getDateReported()));
        date.getStyleClass().add("item-card-meta");

        VBox body = new VBox(8.0, title, new HBox(status), location, date);
        body.getStyleClass().add("item-card-body");

        card.getChildren().addAll(imageWrap, body);
        card.setOnMouseClicked(e ->
                SceneNavigator.navigateTo("/com/lfms/fxml/ItemDetail.fxml", item.getItemId()));
        return card;
    }

    private String safe(String value) {
        return (value == null || value.isBlank()) ? "—" : value;
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
