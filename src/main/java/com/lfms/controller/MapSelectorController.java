package com.lfms.controller;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.net.URL;
import java.util.function.Consumer;

public class MapSelectorController implements com.lfms.util.SceneNavigator.DataReceiver {

    @FXML private WebView webView;

    private Consumer<String> onLocationSelected;
    private String selectedCoordinates;

    @FXML
    public void initialize() {
        WebEngine engine = webView.getEngine();
        URL url = getClass().getResource("/com/lfms/html/map.html");
        if (url != null) {
            engine.load(url.toExternalForm());
        }

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaConnector", new JavaConnector());
            }
        });
    }

    public void setOnLocationSelected(Consumer<String> callback) {
        this.onLocationSelected = callback;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void receiveData(Object data) {
        if (data instanceof Consumer) {
            this.onLocationSelected = (Consumer<String>) data;
        }
    }

    @FXML
    private void confirmSelection() {
        if (onLocationSelected != null && selectedCoordinates != null) {
            onLocationSelected.accept(selectedCoordinates);
        }
        closeWindow();
    }

    @FXML
    private void cancelSelection() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) webView.getScene().getWindow();
        stage.close();
    }

    public class JavaConnector {
        public void setCoordinates(double lat, double lng) {
            selectedCoordinates = String.format("%.5f, %.5f", lat, lng);
        }
    }
}
