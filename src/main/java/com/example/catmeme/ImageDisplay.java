package com.example.catmeme;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.animation.AnimationTimer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ImageDisplay extends Application {

    private static final int CANVAS_WIDTH = 1200;
    private static final int CANVAS_HEIGHT = 800;
    private static final int MAX_IMAGES = 1000;
    private static final double DEFAULT_IMAGE_SIZE = 80;

    private List<ImageSprite> imageSprites;
    private List<Image> loadedImages;
    private Random random;
    private AnimationTimer animationTimer;

    // Classe pour représenter une image animée
    private static class ImageSprite {
        double x, y;
        double velocityX, velocityY;
        Image image;
        double size;
        double rotation;
        double rotationSpeed;
        double scale;

        public ImageSprite(double x, double y, Image image, double size) {
            this.x = x;
            this.y = y;
            this.image = image;
            this.size = size;
            this.rotation = 0;
            this.scale = 1.0;

            // Vitesse aléatoire pour l'animation
            Random rand = new Random();
            this.velocityX = (rand.nextDouble() - 0.5) * 3;
            this.velocityY = (rand.nextDouble() - 0.5) * 3;
            this.rotationSpeed = (rand.nextDouble() - 0.5) * 2;
        }

        public void update() {
            // Mise à jour de la position
            x += velocityX;
            y += velocityY;
            rotation += rotationSpeed;

            // Animation de l'échelle (effet de pulsation)
            scale = 0.8 + 0.3 * Math.sin(System.currentTimeMillis() * 0.002 + x * 0.01);

            // Rebond sur les bords
            if (x <= 0 || x >= CANVAS_WIDTH - size) {
                velocityX = -velocityX;
                x = Math.max(0, Math.min(CANVAS_WIDTH - size, x));
            }
            if (y <= 0 || y >= CANVAS_HEIGHT - size) {
                velocityY = -velocityY;
                y = Math.max(0, Math.min(CANVAS_HEIGHT - size, y));
            }
        }
    }

    @Override
    public void start(Stage primaryStage) {
        // Initialisation
        random = new Random();
        imageSprites = new ArrayList<>();
        loadedImages = new ArrayList<>();

        // Chargement des images
        loadImages();

        if (loadedImages.isEmpty()) {
            showErrorAndExit("Aucune image trouvée ! Placez vos images JPG nommées 1.jpg, 2.jpg, etc. dans le répertoire du programme.");
            return;
        }

        // Création du Canvas pour l'accélération matérielle
        Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Activation de l'anti-aliasing pour un rendu plus fluide
        gc.setImageSmoothing(true);

        // Génération des sprites d'images
        generateImageSprites();

        // Configuration de la scène
        StackPane root = new StackPane();
        root.getChildren().add(canvas);
        Scene scene = new Scene(root, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Configuration de la fenêtre
        primaryStage.setTitle("Affichage d'images JPG avec accélération graphique - " + loadedImages.size() + " images chargées");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Démarrage de l'animation
        startAnimation(gc);

        // Nettoyage lors de la fermeture
        primaryStage.setOnCloseRequest(e -> {
            if (animationTimer != null) {
                animationTimer.stop();
            }
            Platform.exit();
        });

        // Gestion des événements clavier
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case R:
                    generateImageSprites(); // Régénérer les positions
                    break;
                case ESCAPE:
                    Platform.exit();
                    break;
            }
        });

        // Focus pour les événements clavier
        canvas.setFocusTraversable(true);
        canvas.requestFocus();
    }

    private void loadImages() {
        System.out.println("Chargement des images depuis le dossier resources...");

        // Tentative de chargement des images de 1.jpg à n.jpg depuis resources
        for (int i = 1; i <= MAX_IMAGES; i++) {
            String resourcePath = "/resources/images/" + i + ".jpg";

            try {
                // Utilisation du classloader pour accéder aux ressources
                var imageStream = getClass().getResourceAsStream(resourcePath);

                if (imageStream != null) {
                    Image image = new Image(imageStream);

                    if (!image.isError()) {
                        loadedImages.add(image);
                        System.out.println("Image chargée: " + resourcePath +
                                " (" + (int)image.getWidth() + "x" + (int)image.getHeight() + ")");
                    } else {
                        System.err.println("Erreur lors du chargement de: " + resourcePath);
                    }
                    imageStream.close();
                } else {
                    // Essayer sans le préfixe "/" au cas où
                    resourcePath = "resources/images/" + i + ".jpg";
                    imageStream = getClass().getResourceAsStream(resourcePath);

                    if (imageStream != null) {
                        Image image = new Image(imageStream);

                        if (!image.isError()) {
                            loadedImages.add(image);
                            System.out.println("Image chargée: " + resourcePath +
                                    " (" + (int)image.getWidth() + "x" + (int)image.getHeight() + ")");
                        }
                        imageStream.close();
                    }
                }
            } catch (Exception e) {
                System.err.println("Impossible de charger: " + resourcePath + " - " + e.getMessage());
            }
        }

        // Si aucune image n'est trouvée dans resources, essayer dans le classpath racine
        if (loadedImages.isEmpty()) {
            System.out.println("Aucune image trouvée dans /resources/, tentative dans le classpath racine...");

            for (int i = 1; i <= MAX_IMAGES; i++) {
                String resourcePath = "/images/" + i + ".jpg";

                try {
                    var imageStream = getClass().getResourceAsStream(resourcePath);

                    if (imageStream != null) {
                        Image image = new Image(imageStream);

                        if (!image.isError()) {
                            loadedImages.add(image);
                            System.out.println("Image chargée: " + resourcePath);
                        }
                        imageStream.close();
                    }
                } catch (Exception e) {
                    System.err.println("Impossible de charger: " + resourcePath + " - " + e.getMessage());
                }
            }
        }

        System.out.println("Total d'images chargées: " + loadedImages.size());
    }
    private void showErrorAndExit(String message) {
        System.err.println(message);
        System.err.println("Instructions:");
        System.err.println("1. Placez vos images JPG dans le même répertoire que le programme");
        System.err.println("2. Nommez-les: 1.jpg, 2.jpg, 3.jpg, etc.");
        System.err.println("3. Ou créez un dossier 'images' et placez-les dedans");
        Platform.exit();
    }

    private void generateImageSprites() {
        imageSprites.clear();

        // Nombre de sprites basé sur le nombre d'images disponibles
        int numSprites = Math.min(MAX_IMAGES, loadedImages.size() * 2);

        for (int i = 0; i < numSprites; i++) {
            // Position aléatoire
            double size = DEFAULT_IMAGE_SIZE + random.nextDouble() * 40 - 20; // Taille variable
            double x = random.nextDouble() * (CANVAS_WIDTH - size);
            double y = random.nextDouble() * (CANVAS_HEIGHT - size);

            // Sélection aléatoire d'une image
            Image selectedImage = loadedImages.get(random.nextInt(loadedImages.size()));

            imageSprites.add(new ImageSprite(x, y, selectedImage, size));
        }
    }

    private void startAnimation(GraphicsContext gc) {
        animationTimer = new AnimationTimer() {
            private long lastUpdate = 0;
            private final long FRAME_TIME = 16_000_000; // ~60 FPS
            private int frameCount = 0;
            private long fpsTimer = System.currentTimeMillis();

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= FRAME_TIME) {
                    updateAndDraw(gc);
                    lastUpdate = now;
                    frameCount++;

                    // Calcul des FPS toutes les secondes
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - fpsTimer >= 1000) {
                        fpsTimer = currentTime;
                        frameCount = 0;
                    }
                }
            }
        };
        animationTimer.start();
    }

    private void updateAndDraw(GraphicsContext gc) {
        // Effacement du canvas avec un fond dégradé
        gc.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Fond dégradé pour un effet visuel
        gc.setFill(Color.color(0.05, 0.05, 0.15));
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Mise à jour et dessin de chaque sprite d'image
        for (ImageSprite sprite : imageSprites) {
            sprite.update();
            drawImageSprite(gc, sprite);
        }

        // Affichage des informations
        gc.setFill(Color.WHITE);
        gc.fillText("Images affichées: " + imageSprites.size(), 10, 20);
        gc.fillText("Images chargées: " + loadedImages.size(), 10, 40);
        gc.fillText("Utilise l'accélération matérielle JavaFX", 10, 60);
        gc.fillText("Appuyez sur R pour régénérer les positions", 10, 80);
        gc.fillText("Appuyez sur ECHAP pour quitter", 10, 100);
    }

    private void drawImageSprite(GraphicsContext gc, ImageSprite sprite) {
        // Sauvegarde de l'état graphique
        gc.save();

        // Calcul du centre de l'image pour la rotation
        double centerX = sprite.x + sprite.size / 2;
        double centerY = sprite.y + sprite.size / 2;

        // Application des transformations
        gc.translate(centerX, centerY);
        gc.rotate(sprite.rotation);
        gc.scale(sprite.scale, sprite.scale);

        // Ajout d'un effet d'ombre
        gc.setEffect(new javafx.scene.effect.DropShadow(5, Color.color(0, 0, 0, 0.5)));

        // Dessin de l'image centrée
        gc.drawImage(sprite.image,
                -sprite.size / 2, -sprite.size / 2,
                sprite.size, sprite.size);

        // Restauration de l'état graphique
        gc.restore();
    }

    public static void main(String[] args) {
        // Configuration des paramètres JavaFX pour l'accélération
        System.setProperty("prism.order", "es2,d3d,sw"); // Priorité à OpenGL ES 2.0 et Direct3D
        System.setProperty("javafx.animation.fullspeed", "true");
        System.setProperty("prism.vsync", "true"); // VSync pour éviter le tearing
        System.setProperty("prism.allowhidpi", "true"); // Support des écrans haute résolution

        launch(args);
    }
}