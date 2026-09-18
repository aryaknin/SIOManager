package com.example.siomanager;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.stage.Stage;

public class SettingsController {
    @FXML
    private void closeWindow(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void saveAndClose(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}
