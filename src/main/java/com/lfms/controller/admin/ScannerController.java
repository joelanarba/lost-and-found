package com.lfms.controller.admin;

import com.github.sarxos.webcam.Webcam;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.lfms.util.SceneNavigator;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScannerController {

    @FXML private ImageView cameraView;
    @FXML private Label statusLabel;

    private Webcam webcam;
    private AtomicBoolean isScanning;

    @FXML
    public void initialize() {
        isScanning = new AtomicBoolean(true);
        statusLabel.setText("Initializing camera...");

        Thread cameraThread = new Thread(() -> {
            webcam = Webcam.getDefault();
            if (webcam != null) {
                webcam.open();
                Platform.runLater(() -> statusLabel.setText("Scanning for QR codes..."));
                startScanning();
            } else {
                Platform.runLater(() -> statusLabel.setText("No camera found."));
            }
        });
        cameraThread.setDaemon(true);
        cameraThread.start();
    }

    private void startScanning() {
        while (isScanning.get() && webcam.isOpen()) {
            BufferedImage image = webcam.getImage();
            if (image != null) {
                WritableImage fxImage = SwingFXUtils.toFXImage(image, null);
                Platform.runLater(() -> cameraView.setImage(fxImage));

                try {
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
                    Result result = new MultiFormatReader().decode(bitmap);

                    if (result != null) {
                        String text = result.getText();
                        if (text != null) {
                            if (text.startsWith("LFMS-ITEM-")) {
                                isScanning.set(false);
                                webcam.close();
                                Platform.runLater(() -> {
                                    statusLabel.setText("Item found: " + text);
                                    try {
                                        int itemId = Integer.parseInt(text.substring("LFMS-ITEM-".length()));
                                        SceneNavigator.navigateTo("/com/lfms/fxml/ItemDetail.fxml", itemId);
                                    } catch (NumberFormatException e) {
                                        statusLabel.setText("Invalid QR code format.");
                                    }
                                });
                                break;
                            } else if (text.startsWith("LFMS-CLAIM-")) {
                                isScanning.set(false);
                                webcam.close();
                                Platform.runLater(() -> {
                                    statusLabel.setText("Claim verified: " + text);
                                    try {
                                        int claimId = Integer.parseInt(text.substring("LFMS-CLAIM-".length()));
                                        com.lfms.service.ClaimService cs = new com.lfms.service.ClaimService();
                                        com.lfms.model.Claim claim = cs.findById(claimId);
                                        if (claim != null && com.lfms.model.Claim.STATUS_APPROVED.equals(claim.getStatus())) {
                                            boolean confirmed = com.lfms.util.Dialogs.confirm("Handover Item", 
                                                "Claim #" + claimId + " verified for handover. Mark the item as successfully returned to its owner?");
                                            if (confirmed) {
                                                com.lfms.service.ItemService is = new com.lfms.service.ItemService();
                                                com.lfms.model.User user = com.lfms.util.SessionManager.getInstance().getCurrentUser();
                                                int adminId = user != null ? user.getUserId() : -1;
                                                is.updateStatus(claim.getItemId(), com.lfms.model.Item.STATUS_RESOLVED, adminId);
                                                com.lfms.util.Dialogs.info("Handover Complete", "The item has been marked as returned.");
                                            }
                                        } else {
                                            com.lfms.util.Dialogs.error("Invalid Claim", "This claim was not found or is not approved.");
                                        }
                                        SceneNavigator.navigateTo("/com/lfms/fxml/admin/AdminDashboard.fxml");
                                    } catch (NumberFormatException e) {
                                        statusLabel.setText("Invalid QR code format.");
                                    }
                                });
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {
                    // Ignore exceptions from MultiFormatReader when no QR is found
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void stopCamera() {
        isScanning.set(false);
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
    }
}
