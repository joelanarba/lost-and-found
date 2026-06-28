package com.lfms;

import com.lfms.database.DatabaseManager;
import com.lfms.service.DemoDataService;
import com.lfms.util.SceneNavigator;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Alert;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * The JavaFX {@link Application}. Initialises the database (schema + seeded admin),
 * then shows the Login screen. Launched indirectly by {@link Main}.
 */
public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Initialise database before any UI is shown.
        try {
            DatabaseManager.initialize();
            // Populate a rich demo dataset on first run so the app looks alive immediately.
            new DemoDataService().seedIfEmpty();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "The application could not start because the database failed to initialise:\n\n"
                            + e.getMessage());
            alert.setHeaderText("Startup Error");
            alert.setTitle("Lost & Found Management System");
            alert.showAndWait();
            return;
        }

        SceneNavigator.setStage(primaryStage);
        primaryStage.setTitle("UCC Lost & Found Management System");

        // Size the window to fit comfortably within the visual screen area
        // (visual bounds exclude the OS taskbar, so the title bar is never pushed off-screen).
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double w = Math.min(1320, screen.getWidth()  * 0.95);
        double h = Math.min(840,  screen.getHeight() * 0.92);
        primaryStage.setMinWidth(Math.min(960, screen.getWidth()  * 0.75));
        primaryStage.setMinHeight(Math.min(660, screen.getHeight() * 0.75));
        primaryStage.setWidth(w);
        primaryStage.setHeight(h);
        primaryStage.setX(screen.getMinX() + (screen.getWidth()  - w) / 2);
        primaryStage.setY(screen.getMinY() + (screen.getHeight() - h) / 2);

        SceneNavigator.navigateTo("/com/lfms/fxml/Login.fxml");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
