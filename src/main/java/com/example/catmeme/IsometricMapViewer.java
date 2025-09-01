package com.example.catmeme;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.*;

public class IsometricMapViewer extends Application {

    private static final int CANVAS_WIDTH = 1200;
    private static final int CANVAS_HEIGHT = 800;
    private static final int MAP_SIZE = 30;
    private static final int TILE_WIDTH = 60;
    private static final int TILE_HEIGHT = 35;
    private static final int TILE_DEPTH = 30;
    private static final int VIEW_DISTANCE = 15;

    // Couches de la carte
    private static final int LAYER_GROUND = 0;
    private static final int LAYER_WALL = 1;
    private static final int LAYER_CEILING = 2;

    // Position et orientation du joueur
    private double playerX = MAP_SIZE / 2.0;
    private double playerY = MAP_SIZE / 2.0;
    private int playerDirection = 0; // 0=Nord, 1=Est, 2=Sud, 3=Ouest

    // Déplacement
    private double cameraOffsetX = 0;
    private double cameraOffsetY = 0;

    // Images des cubes
    private List<Image> groundImages;
    private List<Image> wallImages;

    // Carte 3D [x][y][layer]
    private int[][][] map;

    // Gestion des touches
    private Set<KeyCode> pressedKeys = new HashSet<>();

    // Animation
    private AnimationTimer animationTimer;

    // Couleurs du joueur selon l'orientation
    private Color[] playerColors = {
            Color.BLUE,    // Nord
            Color.RED,     // Est
            Color.YELLOW,  // Sud
            Color.GREEN    // Ouest
    };

    @Override
    public void start(Stage primaryStage) {
        // Chargement des images
        loadImages();

        if (groundImages.isEmpty()) {
            System.err.println("Erreur: Aucune image trouvée dans resources/");
            Platform.exit();
            return;
        }

        // Génération de la carte
        generateMap();

        // Configuration de l'interface
        Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(true);

        StackPane root = new StackPane();
        root.getChildren().add(canvas);

        Scene scene = new Scene(root, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Gestion des événements clavier
        setupKeyHandling(scene);

        // Configuration de la fenêtre
        primaryStage.setTitle("Visualiseur isométrique 3D - ZQSD: déplacement, A/E: rotation");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Focus pour les événements clavier
        canvas.setFocusTraversable(true);
        canvas.requestFocus();

        // Démarrage de l'animation
        startGameLoop(gc);

        // Nettoyage
        primaryStage.setOnCloseRequest(e -> {
            if (animationTimer != null) {
                animationTimer.stop();
            }
            Platform.exit();
        });
    }

    private void loadImages() {
        groundImages = new ArrayList<>();
        wallImages = new ArrayList<>();

        System.out.println("Chargement des images isométriques...");


        System.out.println("Aucune image trouvée dans /resources/, tentative dans le classpath racine...");

        for (int i = 1; i <= 100; i++) {
            String resourcePath = "/" + i + ".jpg";

            try {
                var imageStream = getClass().getResourceAsStream(resourcePath);

                if (imageStream != null) {
                    Image image = new Image(imageStream);

                    if (!image.isError()) {
                        // Les 25 premières sont les sols, les suivantes les murs
                        if (i <= 25) {
                            groundImages.add(image);
                        } else if (i <= 50) {
                            wallImages.add(image);
                        }
                        System.out.println("Image chargée: " + i + " (" +
                                (i <= 25 ? "sol" : "mur") + ")");
//                            loadedImages.add(image);
//                            System.out.println("Image chargée: " + resourcePath);
                    }
                    imageStream.close();
                }
            } catch (Exception e) {
                System.err.println("Impossible de charger: " + resourcePath + " - " + e.getMessage());
            }
        }
        System.out.println("Images de sol: " + groundImages.size());
        System.out.println("Images de mur: " + wallImages.size());
    }

    private void generateMap() {
        map = new int[MAP_SIZE][MAP_SIZE][3];
        Random random = new Random();

        System.out.println("Génération de la carte 3D...");

        // Génération du sol (partout)
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                map[x][y][LAYER_GROUND] = random.nextInt(Math.max(1, groundImages.size()));
            }
        }

        // Génération des murs (aléatoire, ~30% de chance)
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                if (Math.abs(x - playerX) > 2 || Math.abs(y - playerY) > 2) {
                    if (random.nextDouble() < 0.3) {
                        map[x][y][LAYER_WALL] = random.nextInt(Math.max(1, wallImages.size())) + 1;
                    }
                }
            }
        }

        // Génération des plafonds (au-dessus de certains murs)
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                if (map[x][y][LAYER_WALL] > 0 && random.nextDouble() < 0.5) {
                    map[x][y][LAYER_CEILING] = random.nextInt(Math.max(1, wallImages.size())) + 1;
                }
            }
        }

        System.out.println("Carte générée: " + MAP_SIZE + "x" + MAP_SIZE + " avec 3 couches");
    }

    private void setupKeyHandling(Scene scene) {
        scene.setOnKeyPressed(e -> pressedKeys.add(e.getCode()));
        scene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));
    }

    private void startGameLoop(GraphicsContext gc) {
        animationTimer = new AnimationTimer() {
            private long lastUpdate = 0;
            private final long FRAME_TIME = 16_000_000; // 60 FPS

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= FRAME_TIME) {
                    processInput();
                    updateCamera();
                    render(gc);
                    lastUpdate = now;
                }
            }
        };
        animationTimer.start();
    }

    private void processInput() {
        double moveSpeed = 0.1;
        boolean moved = false;

        // Rotation avec A et E
        if (pressedKeys.contains(KeyCode.A)) {
            playerDirection = (playerDirection + 3) % 4; // Rotation gauche
            pressedKeys.remove(KeyCode.A); // Une seule rotation par pression
        }
        if (pressedKeys.contains(KeyCode.E)) {
            playerDirection = (playerDirection + 1) % 4; // Rotation droite
            pressedKeys.remove(KeyCode.E);
        }

        // Mouvement avec ZQSD selon l'orientation
        double newX = playerX;
        double newY = playerY;

        if (pressedKeys.contains(KeyCode.Z)) { // Avancer
            switch (playerDirection) {
                case 0:
                    newY -= moveSpeed;
                    break; // Nord
                case 1:
                    newX += moveSpeed;
                    break; // Est
                case 2:
                    newY += moveSpeed;
                    break; // Sud
                case 3:
                    newX -= moveSpeed;
                    break; // Ouest
            }
            moved = true;
        }
        if (pressedKeys.contains(KeyCode.S)) { // Reculer
            switch (playerDirection) {
                case 0:
                    newY += moveSpeed;
                    break; // Nord
                case 1:
                    newX -= moveSpeed;
                    break; // Est
                case 2:
                    newY -= moveSpeed;
                    break; // Sud
                case 3:
                    newX += moveSpeed;
                    break; // Ouest
            }
            moved = true;
        }
        if (pressedKeys.contains(KeyCode.Q)) { // Gauche
            switch (playerDirection) {
                case 0:
                    newX -= moveSpeed;
                    break; // Nord
                case 1:
                    newY -= moveSpeed;
                    break; // Est
                case 2:
                    newX += moveSpeed;
                    break; // Sud
                case 3:
                    newY += moveSpeed;
                    break; // Ouest
            }
            moved = true;
        }
        if (pressedKeys.contains(KeyCode.D)) { // Droite
            switch (playerDirection) {
                case 0:
                    newX += moveSpeed;
                    break; // Nord
                case 1:
                    newY += moveSpeed;
                    break; // Est
                case 2:
                    newX -= moveSpeed;
                    break; // Sud
                case 3:
                    newY -= moveSpeed;
                    break; // Ouest
            }
            moved = true;
        }

        // Vérification des collisions et mise à jour de la position
        if (moved && isValidPosition(newX, newY)) {
            playerX = newX;
            playerY = newY;
        }
    }

    private boolean isValidPosition(double x, double y) {
        int mapX = (int) Math.round(x);
        int mapY = (int) Math.round(y);

        // Vérification des limites
        if (mapX < 0 || mapX >= MAP_SIZE || mapY < 0 || mapY >= MAP_SIZE) {
            return false;
        }

        // Vérification des collisions avec les murs
        return map[mapX][mapY][LAYER_WALL] == 0;
    }

    private void updateCamera() {
        // Centrage de la caméra sur le joueur
        cameraOffsetX = CANVAS_WIDTH / 2 - playerX * TILE_WIDTH;
        cameraOffsetY = CANVAS_HEIGHT / 2 - playerY * TILE_HEIGHT;
    }

    private void render(GraphicsContext gc) {
        // Effacement
        gc.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Fond
        gc.setFill(Color.color(0.2, 0.3, 0.4));
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Calcul de la zone visible
        int startX = Math.max(0, (int) playerX - VIEW_DISTANCE);
        int endX = Math.min(MAP_SIZE - 1, (int) playerX + VIEW_DISTANCE);
        int startY = Math.max(0, (int) playerY - VIEW_DISTANCE);
        int endY = Math.min(MAP_SIZE - 1, (int) playerY + VIEW_DISTANCE);

        // Liste des tuiles à rendre (pour le tri en profondeur)
        List<RenderTile> tilesToRender = new ArrayList<>();

        // Collecte des tuiles visibles
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                // Conversion en coordonnées isométriques
                double isoX = (x - y) * (TILE_WIDTH / 2.0) + cameraOffsetX;
                double isoY = (x + y) * (TILE_HEIGHT / 2.0) + cameraOffsetY;

                // Sol
                if (map[x][y][LAYER_GROUND] > 0) {
                    tilesToRender.add(new RenderTile(x, y, LAYER_GROUND, isoX, isoY, 1.0f));
                }

                // Mur avec gestion de transparence
                if (map[x][y][LAYER_WALL] > 0) {
                    float alpha = calculateWallTransparency(x, y);
                    tilesToRender.add(new RenderTile(x, y, LAYER_WALL, isoX, isoY - TILE_DEPTH, alpha));
                }

                // Plafond avec gestion de transparence
                if (map[x][y][LAYER_CEILING] > 0) {
                    float alpha = calculateCeilingTransparency(x, y);
                    tilesToRender.add(new RenderTile(x, y, LAYER_CEILING, isoX, isoY - TILE_DEPTH * 2, alpha));
                }
            }
        }

        // Tri par profondeur (rendu du fond vers l'avant)
        tilesToRender.sort((a, b) -> {
            double depthA = a.mapX + a.mapY + a.layer * 0.001;
            double depthB = b.mapX + b.mapY + b.layer * 0.001;
            return Double.compare(depthA, depthB);
        });

        // Rendu des tuiles
        for (RenderTile tile : tilesToRender) {
            renderTile(gc, tile);
        }

        // Rendu du joueur
        renderPlayer(gc);

        // Interface utilisateur
        renderUI(gc);
    }

    private float calculateWallTransparency(int wallX, int wallY) {
        double dx = wallX - playerX;
        double dy = wallY - playerY;

        // Direction du joueur
        double[] dirVec = getDirectionVector();

        // Produit scalaire pour savoir si le mur est devant
        double dot = dx * dirVec[0] + dy * dirVec[1];
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (dot > 0 && distance < 3) { // Mur devant et proche
            if (distance < 1.5) {
                return 0.2f; // 80% transparent
            } else {
                return 0.5f; // 50% transparent
            }
        }

        return 1.0f; // Opaque
    }

    private float calculateCeilingTransparency(int ceilingX, int ceilingY) {
        double distance = Math.sqrt(Math.pow(ceilingX - playerX, 2) + Math.pow(ceilingY - playerY, 2));

        if (distance < 2.5) {
            return 0.3f; // Plafond proche plus transparent
        } else if (distance < 4) {
            return 0.6f;
        }

        return 1.0f;
    }

    private double[] getDirectionVector() {
        switch (playerDirection) {
            case 0:
                return new double[]{0, -1}; // Nord
            case 1:
                return new double[]{1, 0};  // Est
            case 2:
                return new double[]{0, 1};  // Sud
            case 3:
                return new double[]{-1, 0}; // Ouest
            default:
                return new double[]{0, -1};
        }
    }

    private void renderTile(GraphicsContext gc, RenderTile tile) {
        Image image = null;

        // Sélection de l'image
        if (tile.layer == LAYER_GROUND && !groundImages.isEmpty()) {
            int imageIndex = Math.max(0, map[tile.mapX][tile.mapY][tile.layer] - 1);
            imageIndex = Math.min(imageIndex, groundImages.size() - 1);
            image = groundImages.get(imageIndex);
        } else if ((tile.layer == LAYER_WALL || tile.layer == LAYER_CEILING) && !wallImages.isEmpty()) {
            int imageIndex = Math.max(0, map[tile.mapX][tile.mapY][tile.layer] - 1);
            imageIndex = Math.min(imageIndex, wallImages.size() - 1);
            image = wallImages.get(imageIndex);
        }

        if (image != null) {
            gc.save();
            gc.setGlobalAlpha(tile.alpha);
            gc.drawImage(image, tile.screenX - TILE_WIDTH / 2, tile.screenY - TILE_HEIGHT,
                    TILE_WIDTH, TILE_WIDTH * image.getHeight() / image.getWidth());
            gc.restore();
        }
    }

    private void renderPlayer(GraphicsContext gc) {
        double playerScreenX = cameraOffsetX;
        double playerScreenY = cameraOffsetY;

        // Cercle du joueur avec couleur selon l'orientation
        gc.setFill(playerColors[playerDirection]);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);

        double playerRadius = 8;
        gc.fillOval(playerScreenX - playerRadius, playerScreenY - playerRadius,
                playerRadius * 2, playerRadius * 2);
        gc.strokeOval(playerScreenX - playerRadius, playerScreenY - playerRadius,
                playerRadius * 2, playerRadius * 2);

        // Indicateur de direction
        double[] dirVec = getDirectionVector();
        double arrowLength = 15;
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(3);
        gc.strokeLine(playerScreenX, playerScreenY,
                playerScreenX + dirVec[0] * arrowLength,
                playerScreenY + dirVec[1] * arrowLength);
    }

    private void renderUI(GraphicsContext gc) {
        gc.setFill(Color.WHITE);
        gc.fillText("Position: (" + String.format("%.1f", playerX) + ", " +
                String.format("%.1f", playerY) + ")", 10, 20);

        String[] directions = {"Nord", "Est", "Sud", "Ouest"};
        gc.fillText("Direction: " + directions[playerDirection], 10, 40);

        gc.fillText("Contrôles:", 10, 70);
        gc.fillText("ZQSD: Déplacement", 10, 90);
        gc.fillText("A/E: Rotation", 10, 110);

        gc.fillText("Images chargées: " + (groundImages.size() + wallImages.size()), 10, 140);
    }

    // Classe pour stocker les informations de rendu d'une tuile
    private static class RenderTile {
        int mapX, mapY, layer;
        double screenX, screenY;
        float alpha;

        RenderTile(int mapX, int mapY, int layer, double screenX, double screenY, float alpha) {
            this.mapX = mapX;
            this.mapY = mapY;
            this.layer = layer;
            this.screenX = screenX;
            this.screenY = screenY;
            this.alpha = alpha;
        }
    }

    public static void main(String[] args) {
        // Configuration JavaFX pour l'accélération graphique
        System.setProperty("prism.order", "es2,d3d,sw");
        System.setProperty("javafx.animation.fullspeed", "true");
        System.setProperty("prism.vsync", "true");
        System.setProperty("prism.allowhidpi", "true");

        launch(args);
    }
}