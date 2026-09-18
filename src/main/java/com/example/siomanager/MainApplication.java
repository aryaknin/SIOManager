package com.example.siomanager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(loader.load(), 1180, 720);
        MainController controller = loader.getController();

        stage.setTitle("SIOManager");
        stage.setMinWidth(820);
        stage.setMinHeight(520);
        stage.setScene(scene);
        controller.attachStage(stage);
        stage.setOnCloseRequest(event -> {
            if (!controller.confirmCloseAll()) {
                event.consume();
            } else {
                controller.dispose();
            }
        });
        stage.show();
    }
}
