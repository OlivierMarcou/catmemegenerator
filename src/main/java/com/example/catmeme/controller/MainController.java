package com.example.catmeme.controller;

import com.example.catmeme.service.CatService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    private Canvas imageCanvas;

    @FXML
    private TextField textField;

    @FXML
    private Button loadImageButton;

    @FXML
    private Button saveImageButton;

    private CatService catService;
    private Image currentImage;
    private GraphicsContext gc;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        catService = new CatService();
        gc = imageCanvas.getGraphicsContext2D();

        // Configuration initiale du canvas
        imageCanvas.setWidth(600);
        imageCanvas.setHeight(400);

        // Écouter les changements de texte pour redessiner en temps réel
        textField.textProperty().addListener((obs, oldText, newText) -> {
            if (currentImage != null) {
                drawImageWithText();
            }
        });

        // Charger une première image
        loadRandomCatImage();
    }

    @FXML
    private void loadRandomCatImage() {
        loadImageButton.setDisable(true);
        loadImageButton.setText("Chargement...");

        catService.getRandomCatImage().thenAccept(image -> {
            Platform.runLater(() -> {
                currentImage = image;
                drawImageWithText();
                loadImageButton.setDisable(false);
                loadImageButton.setText("Nouvelle image");
                saveImageButton.setDisable(false);
            });
        }).exceptionally(throwable -> {
            Platform.runLater(() -> {
                showError("Erreur lors du chargement de l'image: " + throwable.getMessage());
                loadImageButton.setDisable(false);
                loadImageButton.setText("Nouvelle image");
            });
            return null;
        });
    }

    private void drawImageWithText() {
        if (currentImage == null) return;

        // Effacer le canvas
        gc.clearRect(0, 0, imageCanvas.getWidth(), imageCanvas.getHeight());

        // Calculer les dimensions pour ajuster l'image au canvas
        double canvasWidth = imageCanvas.getWidth();
        double canvasHeight = imageCanvas.getHeight();
        double imageWidth = currentImage.getWidth();
        double imageHeight = currentImage.getHeight();

        // Calculer le ratio pour maintenir les proportions
        double ratio = Math.min(canvasWidth / imageWidth, canvasHeight / imageHeight);
        double scaledWidth = imageWidth * ratio;
        double scaledHeight = imageHeight * ratio;

        // Centrer l'image
        double x = (canvasWidth - scaledWidth) / 2;
        double y = (canvasHeight - scaledHeight) / 2;

        // Dessiner l'image
        gc.drawImage(currentImage, x, y, scaledWidth, scaledHeight);

        // Dessiner le texte s'il y en a un
        String text = textField.getText();
        if (text != null && !text.trim().isEmpty()) {
            drawText(text, canvasWidth, y);
        }
    }

    private void drawText(String text, double canvasWidth, double imageY) {
        // Configuration du texte
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        gc.setTextAlign(TextAlignment.CENTER);

        // Contour noir
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(3);
        gc.strokeText(text,  201, imageY -10);

        // Texte blanc par-dessus
        gc.setFill(Color.WHITE);
        gc.fillText(text,  200, 50);
    }

    @FXML
    private void saveImage() {
        if (currentImage == null) {
            showError("Aucune image à sauvegarder");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sauvegarder l'image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG files", "*.png")
        );
        fileChooser.setInitialFileName("cat-meme.png");

        Stage stage = (Stage) saveImageButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                // Créer un snapshot du canvas
                SnapshotParameters params = new SnapshotParameters();
                params.setFill(Color.TRANSPARENT);
                WritableImage writableImage = imageCanvas.snapshot(params, null);

                // Convertir en BufferedImage et sauvegarder
                BufferedImage bufferedImage = convertToBufferedImage(writableImage);
                ImageIO.write(bufferedImage, "png", file);

                showInfo("Image sauvegardée avec succès!");

            } catch (IOException e) {
                showError("Erreur lors de la sauvegarde: " + e.getMessage());
            }
        }
    }

    private BufferedImage convertToBufferedImage(WritableImage writableImage) {
        int width = (int) writableImage.getWidth();
        int height = (int) writableImage.getHeight();
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color color = writableImage.getPixelReader().getColor(x, y);
                int argb = ((int) (color.getOpacity() * 255) << 24) |
                        ((int) (color.getRed() * 255) << 16) |
                        ((int) (color.getGreen() * 255) << 8) |
                        (int) (color.getBlue() * 255);
                bufferedImage.setRGB(x, y, argb);
            }
        }

        return bufferedImage;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void cleanup() {
        if (catService != null) {
            catService.close();
        }
    }
}