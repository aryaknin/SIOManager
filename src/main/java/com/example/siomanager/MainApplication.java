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
        Scene scene = new Scene(loader.load(), 1280, 800);
        MainController controller = loader.getController();

        stage.setTitle("SIOManager");
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            if (!controller.confirmCloseAll()) {
                event.consume();
            }
        });
        stage.show();
    }
}
