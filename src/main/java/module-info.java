module catmeme {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires java.desktop;

    opens com.example.catmeme to javafx.fxml;
    opens com.example.catmeme.controller to javafx.fxml;

    exports com.example.catmeme;
    exports com.isometric.game;
    opens com.isometric.game to javafx.fxml;
    exports com.isometric.game.model;
    opens com.isometric.game.model to javafx.fxml;
}