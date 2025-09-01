package com.example.catmeme;

import com.example.catmeme.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CatMemeApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();

        MainController controller = loader.getController();

        Scene scene = new Scene(root, 640, 520);

        primaryStage.setTitle("Générateur de Mèmes de Chats");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);

        // Nettoyage lors de la fermeture
        primaryStage.setOnCloseRequest(event -> {
            controller.cleanup();
        });

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}