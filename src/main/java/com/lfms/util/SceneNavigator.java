package com.lfms.util;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Screen;
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
        openModal(fxmlPath, title, data, -1, -1);
    }

    /**
     * Opens a modal window for the given view. When {@code width}/{@code height} are positive the
     * scene is created at that size; otherwise it sizes to its content.
     */
    public static void openModal(String fxmlPath, String title, Object data, double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(resource(fxmlPath));
            Parent root = loader.load();
            passData(loader, data);

            // Clamp requested size to the visual screen area so the window never opens off-screen.
            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
            double w = width  > 0 ? Math.min(width,  screen.getWidth()  * 0.90) : -1;
            double h = height > 0 ? Math.min(height, screen.getHeight() * 0.88) : -1;

            Stage modal = new Stage();
            modal.initOwner(primaryStage);
            modal.initModality(Modality.WINDOW_MODAL);
            modal.setTitle(title);
            modal.setResizable(true);
            Scene scene = w > 0 && h > 0 ? new Scene(root, w, h) : new Scene(root);
            applyStyles(scene);
            modal.setScene(scene);

            // Center within the visual screen area (respects taskbar position).
            if (w > 0 && h > 0) {
                modal.setX(screen.getMinX() + (screen.getWidth()  - w) / 2);
                modal.setY(screen.getMinY() + (screen.getHeight() - h) / 2);
            }
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

    private static boolean darkModeEnabled = false;

    public static void toggleTheme() {
        darkModeEnabled = !darkModeEnabled;
        Scene scene = primaryStage.getScene();
        if (scene != null) {
            scene.getStylesheets().clear();
            applyStyles(scene);
        }
    }

    private static void applyStyles(Scene scene) {
        URL css = SceneNavigator.class.getResource(STYLESHEET);
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        if (darkModeEnabled) {
            URL darkCss = SceneNavigator.class.getResource("/com/lfms/css/dark-theme.css");
            if (darkCss != null) {
                scene.getStylesheets().add(darkCss.toExternalForm());
            }
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
