package com.lfms.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Centralised navigation: loads FXML views, applies the shared stylesheet, swaps the root
 * of the primary stage, and opens modal dialogs. Optional data is delivered to a target
 * controller that implements {@link DataReceiver}.
 */
public class SceneNavigator {

    private static final String STYLESHEET = "/com/lfms/css/main.css";

    private static Stage primaryStage;

    private SceneNavigator() {
    }

    /**
     * Implemented by controllers that need a data object passed in during navigation.
     */
    public interface DataReceiver {
        void receiveData(Object data);
    }

    public static void setStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void navigateTo(String fxmlPath) {
        navigateTo(fxmlPath, null);
    }

    public static void navigateTo(String fxmlPath, Object data) {
        try {
            FXMLLoader loader = new FXMLLoader(resource(fxmlPath));
            Parent root = loader.load();
            passData(loader, data);

            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root);
                applyStyles(scene);
                primaryStage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load view: " + fxmlPath, e);
        }
    }

    public static void openModal(String fxmlPath, String title) {
        openModal(fxmlPath, title, null);
    }

    public static void openModal(String fxmlPath, String title, Object data) {
        try {
            FXMLLoader loader = new FXMLLoader(resource(fxmlPath));
            Parent root = loader.load();
            passData(loader, data);

            Stage modal = new Stage();
            modal.initOwner(primaryStage);
            modal.initModality(Modality.WINDOW_MODAL);
            modal.setTitle(title);
            Scene scene = new Scene(root);
            applyStyles(scene);
            modal.setScene(scene);
            modal.showAndWait();
        } catch (IOException e) {
            throw new RuntimeException("Failed to open modal: " + fxmlPath, e);
        }
    }

    private static void passData(FXMLLoader loader, Object data) {
        if (data == null) {
            return;
        }
        Object controller = loader.getController();
        if (controller instanceof DataReceiver) {
            ((DataReceiver) controller).receiveData(data);
        }
    }

    private static void applyStyles(Scene scene) {
        URL css = SceneNavigator.class.getResource(STYLESHEET);
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
    }

    private static URL resource(String path) {
        URL url = SceneNavigator.class.getResource(path);
        if (url == null) {
            throw new RuntimeException("FXML resource not found on classpath: " + path);
        }
        return url;
    }
}
