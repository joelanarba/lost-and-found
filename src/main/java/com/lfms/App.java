package com.lfms;

import com.lfms.database.DatabaseManager;
import com.lfms.util.SceneNavigator;
import javafx.application.Application;
import javafx.scene.control.Alert;
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
        primaryStage.setTitle("Lost & Found Management System");
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(700);
        primaryStage.setWidth(1200);
        primaryStage.setHeight(760);
        SceneNavigator.navigateTo("/com/lfms/fxml/Login.fxml");
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
