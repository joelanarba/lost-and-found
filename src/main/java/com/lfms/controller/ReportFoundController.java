package com.lfms.controller;

import com.lfms.model.Item;
import com.lfms.model.User;
import com.lfms.service.ItemService;
import com.lfms.util.Dialogs;
import com.lfms.util.ImageUtil;
import com.lfms.util.SceneNavigator;
import com.lfms.util.SessionManager;
import com.lfms.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;

/**
 * Controller for reporting a FOUND item. Location and image are required.
 */
public class ReportFoundController {

    @FXML private SidebarController sidebarController;
    @FXML private TextField nameField;
    @FXML private ComboBox<String> categoryBox;
    @FXML private TextArea descriptionArea;
    @FXML private TextField locationField;
    @FXML private DatePicker datePicker;
    @FXML private ImageView imageView;
    @FXML private Label nameError;
    @FXML private Label categoryError;
    @FXML private Label descriptionError;
    @FXML private Label locationError;
    @FXML private Label imageError;

    private final ItemService itemService = new ItemService();
    private File selectedImage;

    @FXML
    public void initialize() {
        sidebarController.setActive("reportfound");
        categoryBox.getItems().setAll(Item.CATEGORIES);
        imageView.setImage(ImageUtil.loadThumbnail(null, 200, 200));
    }

    @FXML
    private void chooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Item Image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        File file = chooser.showOpenDialog(SceneNavigator.getPrimaryStage());
        if (file != null) {
            selectedImage = file;
            imageView.setImage(new Image(file.toURI().toString(), 200, 200, true, true, true));
            ValidationUtil.clearError(imageError);
        }
    }

    @FXML
    private void handleSubmit() {
        ValidationUtil.clearAllErrors(nameError, categoryError, descriptionError, locationError, imageError);

        boolean valid = true;
        if (!ValidationUtil.isNotEmpty(nameField.getText())) {
            ValidationUtil.showError(nameError, "Item name is required.");
            valid = false;
        }
        if (categoryBox.getValue() == null) {
            ValidationUtil.showError(categoryError, "Please select a category.");
            valid = false;
        }
        if (!ValidationUtil.isNotEmpty(descriptionArea.getText())) {
            ValidationUtil.showError(descriptionError, "A description is required.");
            valid = false;
        }
        if (!ValidationUtil.isNotEmpty(locationField.getText())) {
            ValidationUtil.showError(locationError, "Location is required for found items.");
            valid = false;
        }
        if (selectedImage == null) {
            ValidationUtil.showError(imageError, "An image is required for found items.");
            valid = false;
        }
        if (!valid) {
            return;
        }

        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        Item item = new Item();
        item.setUserId(user.getUserId());
        item.setType(Item.TYPE_FOUND);
        item.setName(nameField.getText().trim());
        item.setCategory(categoryBox.getValue());
        item.setDescription(descriptionArea.getText().trim());
        item.setLocation(locationField.getText().trim());
        item.setStatus(Item.STATUS_OPEN);
        LocalDate date = datePicker.getValue();
        item.setDateReported(date != null ? date.toString() : null);

        try {
            int id = itemService.reportItem(item, selectedImage);
            item.setItemId(id);
            int matches = itemService.getMatchesForItem(item).size();
            Dialogs.info("Report Submitted",
                    "Found item reported! " + matches + " possible match" + (matches == 1 ? "" : "es") + " found.");
            SceneNavigator.navigateTo("/com/lfms/fxml/Dashboard.fxml");
        } catch (RuntimeException e) {
            Dialogs.error("Error", "Could not submit the report:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleClear() {
        nameField.clear();
        categoryBox.setValue(null);
        descriptionArea.clear();
        locationField.clear();
        datePicker.setValue(null);
        selectedImage = null;
        imageView.setImage(ImageUtil.loadThumbnail(null, 200, 200));
        ValidationUtil.clearAllErrors(nameError, categoryError, descriptionError, locationError, imageError);
    }
}
