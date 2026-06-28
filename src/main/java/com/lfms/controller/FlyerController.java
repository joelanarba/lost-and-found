package com.lfms.controller;

import com.lfms.model.Item;
import com.lfms.model.User;
import com.lfms.service.ItemService;
import com.lfms.service.UserService;
import com.lfms.util.Dialogs;
import com.lfms.util.ImageUtil;
import com.lfms.util.SceneNavigator;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * A printable "LOST ITEM" flyer rendered in its own 600&times;800 window. Receives an
 * {@link Item} (or its id) and lays it out as a campus-noticeboard poster in UCC colours; the
 * Print button sends the poster — and only the poster, not the action buttons — to a
 * {@link PrinterJob}, scaled to fit the page.
 */
public class FlyerController implements SceneNavigator.DataReceiver {

    @FXML private VBox posterNode;
    @FXML private ImageView flyerImage;
    @FXML private Label flyerName;
    @FXML private Label flyerCategory;
    @FXML private Label flyerDate;
    @FXML private Label flyerLocation;
    @FXML private Label flyerDescription;
    @FXML private Label flyerContact;

    private final ItemService itemService = new ItemService();
    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        // Wire ESC key to close so the window is always dismissible even without the button.
        Platform.runLater(() -> posterNode.getScene().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) close();
        }));
    }

    @Override
    public void receiveData(Object data) {
        Item item = null;
        if (data instanceof Item i) {
            item = i;
        } else if (data instanceof Integer id) {
            item = itemService.findById(id);
        }
        if (item != null) {
            render(item);
        }
    }

    private void render(Item item) {
        flyerImage.setImage(ImageUtil.loadImage(item.getImagePath()));
        flyerName.setText(item.getName());
        flyerCategory.setText(nz(item.getCategory(), "—"));
        flyerDate.setText(nz(item.getDateReported(), "Not specified"));
        flyerLocation.setText(nz(item.getLocation(), "Not specified"));
        flyerDescription.setText(nz(item.getDescription(), ""));

        User reporter = userService.findById(item.getUserId());
        flyerContact.setText(buildContact(reporter));
    }

    private String buildContact(User reporter) {
        if (reporter == null) {
            return "the University of Cape Coast Lost & Found office";
        }
        StringBuilder sb = new StringBuilder(reporter.getName());
        if (reporter.getEmail() != null && !reporter.getEmail().isBlank()) {
            sb.append("  —  ").append(reporter.getEmail());
        }
        if (reporter.getPhone() != null && !reporter.getPhone().isBlank()) {
            sb.append("  —  ").append(reporter.getPhone());
        }
        return sb.toString();
    }

    @FXML
    private void print() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            return;
        }
        if (!job.showPrintDialog(posterNode.getScene().getWindow())) {
            return;
        }

        // Scale the poster to fit the printable area so nothing is clipped.
        PageLayout layout = job.getJobSettings().getPageLayout();
        double scale = Math.min(
                layout.getPrintableWidth() / posterNode.getBoundsInParent().getWidth(),
                layout.getPrintableHeight() / posterNode.getBoundsInParent().getHeight());

        Scale applied = null;
        if (scale > 0 && scale < 1) {
            applied = new Scale(scale, scale);
            posterNode.getTransforms().add(applied);
        }

        boolean printed = job.printPage(posterNode);

        if (applied != null) {
            posterNode.getTransforms().remove(applied);
        }
        if (printed) {
            job.endJob();
        }
    }

    @FXML
    private void saveImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Flyer as PNG Image");
        String safeName = flyerName.getText().replaceAll("[^\\w-]", "_");
        chooser.setInitialFileName("flyer-" + safeName + ".png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        File dest = chooser.showSaveDialog(posterNode.getScene().getWindow());
        if (dest == null) return;

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.WHITE);
        WritableImage snapshot = posterNode.snapshot(params, null);
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", dest);
            Dialogs.info("Flyer Saved", "Image saved to:\n" + dest.getAbsolutePath());
        } catch (IOException e) {
            Dialogs.error("Save Failed", "Could not write the image:\n" + e.getMessage());
        }
    }

    @FXML
    private void close() {
        ((Stage) posterNode.getScene().getWindow()).close();
    }

    private String nz(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
