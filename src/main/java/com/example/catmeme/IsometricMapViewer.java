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
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;

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

    // Navigation automatique
    private List<Point> currentPath = new ArrayList<>();
    private int currentPathIndex = 0;
    private boolean isAutoMoving = false;
    private boolean pathBlocked = false;
    private long pathBlockedTime = 0;

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

        // Gestion des événements clavier et souris
        setupInputHandling(scene, canvas);

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

        // Chargement de toutes les images disponibles
        for (int i = 1; i <= 100; i++) {
            String resourcePath = "/resources/" + i + ".jpg";

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
                    }
                    imageStream.close();
                }
            } catch (Exception e) {
                // Pas d'erreur si l'image n'existe pas
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

    private void setupInputHandling(Scene scene, Canvas canvas) {
        // Gestion du clavier
        scene.setOnKeyPressed(e -> pressedKeys.add(e.getCode()));
        scene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));

        // Gestion de la souris
        canvas.setOnMouseClicked(this::handleMouseClick);
    }

    private void handleMouseClick(MouseEvent event) {
        double mouseX = event.getX();
        double mouseY = event.getY();

        // Conversion des coordonnées écran vers coordonnées de carte
        Point mapCoord = screenToMapCoordinates(mouseX, mouseY);

        if (mapCoord != null && isValidMapPosition(mapCoord.x, mapCoord.y)) {
            System.out.println("Clic sur: (" + mapCoord.x + ", " + mapCoord.y + ")");

            // Vérification que c'est un sol libre
            if (map[mapCoord.x][mapCoord.y][LAYER_WALL] == 0) {
                navigateToPosition(mapCoord.x, mapCoord.y);
            } else {
                System.out.println("Position bloquée par un mur");
            }
        }
    }

    private Point screenToMapCoordinates(double screenX, double screenY) {
        // Conversion inverse de la projection isométrique
        double relativeX = screenX - cameraOffsetX;
        double relativeY = screenY - cameraOffsetY;

        // Formules de conversion inverse
        double mapX = (relativeX / (TILE_WIDTH / 2.0) + relativeY / (TILE_HEIGHT / 2.0)) / 2.0;
        double mapY = (relativeY / (TILE_HEIGHT / 2.0) - relativeX / (TILE_WIDTH / 2.0)) / 2.0;

        return new Point((int) Math.round(mapX), (int) Math.round(mapY));
    }

    private boolean isValidMapPosition(int x, int y) {
        return x >= 0 && x < MAP_SIZE && y >= 0 && y < MAP_SIZE;
    }

    private void navigateToPosition(int targetX, int targetY) {
        Point start = new Point((int) Math.round(playerX), (int) Math.round(playerY));
        Point target = new Point(targetX, targetY);

        // Calcul du chemin avec A*
        List<Point> path = findPath(start, target);

        if (path != null && path.size() > 1) {
            currentPath = path;
            currentPathIndex = 1; // Ignore le point de départ
            isAutoMoving = true;
            pathBlocked = false;

            System.out.println("Chemin trouvé: " + path.size() + " points");

            // Orientation vers la première destination
            Point nextPoint = currentPath.get(currentPathIndex);
            turnTowardsTarget(nextPoint.x, nextPoint.y);
        } else {
            // Pas de chemin trouvé
            pathBlocked = true;
            pathBlockedTime = System.currentTimeMillis();
            isAutoMoving = false;
            System.out.println("Aucun chemin trouvé vers la destination");
        }
    }

    private List<Point> findPath(Point start, Point goal) {
        // Algorithme A* pour le pathfinding
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));
        Set<String> closedSet = new HashSet<>();
        Map<String, AStarNode> allNodes = new HashMap<>();

        AStarNode startNode = new AStarNode(start.x, start.y, null);
        startNode.gCost = 0;
        startNode.hCost = manhattanDistance(start, goal);
        startNode.fCost = startNode.gCost + startNode.hCost;

        openSet.add(startNode);
        allNodes.put(start.x + "," + start.y, startNode);

        // Directions de mouvement (8 directions mais privilégier les orthogonales)
        int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}, {1,1}, {1,-1}, {-1,1}, {-1,-1}};

        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();
            String currentKey = current.x + "," + current.y;

            if (closedSet.contains(currentKey)) {
                continue;
            }

            closedSet.add(currentKey);

            // Destination atteinte
            if (current.x == goal.x && current.y == goal.y) {
                List<Point> path = reconstructPath(current);
                System.out.println("Chemin A* calculé: " + path.size() + " étapes");
                return path;
            }

            // Exploration des voisins
            for (int[] dir : directions) {
                int newX = current.x + dir[0];
                int newY = current.y + dir[1];
                String neighborKey = newX + "," + newY;

                // Vérifications de validité
                if (!isValidMapPosition(newX, newY) ||
                        closedSet.contains(neighborKey) ||
                        map[newX][newY][LAYER_WALL] != 0) {
                    continue;
                }

                // Coût plus élevé pour les diagonales
                double moveCost = (Math.abs(dir[0]) + Math.abs(dir[1]) == 2) ? 1.414 : 1.0;
                double tentativeGCost = current.gCost + moveCost;

                AStarNode neighbor = allNodes.get(neighborKey);
                if (neighbor == null) {
                    neighbor = new AStarNode(newX, newY, current);
                    neighbor.gCost = tentativeGCost;
                    neighbor.hCost = manhattanDistance(new Point(newX, newY), goal);
                    neighbor.fCost = neighbor.gCost + neighbor.hCost;

                    allNodes.put(neighborKey, neighbor);
                    openSet.add(neighbor);
                } else if (tentativeGCost < neighbor.gCost) {
                    neighbor.parent = current;
                    neighbor.gCost = tentativeGCost;
                    neighbor.fCost = neighbor.gCost + neighbor.hCost;

                    // Mise à jour dans la queue
                    openSet.remove(neighbor);
                    openSet.add(neighbor);
                }
            }
        }

        System.out.println("Aucun chemin A* trouvé vers la destination");
        return null; // Aucun chemin trouvé
    }

    private double manhattanDistance(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    private List<Point> reconstructPath(AStarNode endNode) {
        List<Point> path = new ArrayList<>();
        AStarNode current = endNode;

        while (current != null) {
            path.add(0, new Point(current.x, current.y));
            current = current.parent;
        }

        return path;
    }

    private void turnTowardsTarget(int targetX, int targetY) {
        double dx = targetX - playerX;
        double dy = targetY - playerY;

        // Calcul de l'angle vers la cible
        double angle = Math.atan2(dy, dx);

        // Conversion en direction (0=Nord, 1=Est, 2=Sud, 3=Ouest)
        int targetDirection = 0;
        if (angle >= -Math.PI/4 && angle < Math.PI/4) {
            targetDirection = 1; // Est
        } else if (angle >= Math.PI/4 && angle < 3*Math.PI/4) {
            targetDirection = 2; // Sud
        } else if (angle >= 3*Math.PI/4 || angle < -3*Math.PI/4) {
            targetDirection = 3; // Ouest
        } else {
            targetDirection = 0; // Nord
        }

        playerDirection = targetDirection;
    }

    private void startGameLoop(GraphicsContext gc) {
        animationTimer = new AnimationTimer() {
            private long lastUpdate = 0;
            private final long FRAME_TIME = 16_000_000; // 60 FPS

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= FRAME_TIME) {
                    processInput();
                    updateCamera(); // Mise à jour de la caméra à chaque frame
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
                case 0: newY -= moveSpeed; break; // Nord
                case 1: newX += moveSpeed; break; // Est
                case 2: newY += moveSpeed; break; // Sud
                case 3: newX -= moveSpeed; break; // Ouest
            }
            moved = true;
        }
        if (pressedKeys.contains(KeyCode.S)) { // Reculer
            switch (playerDirection) {
                case 0: newY += moveSpeed; break; // Nord
                case 1: newX -= moveSpeed; break; // Est
                case 2: newY -= moveSpeed; break; // Sud
                case 3: newX += moveSpeed; break; // Ouest
            }
            moved = true;
        }
        if (pressedKeys.contains(KeyCode.Q)) { // Gauche
            switch (playerDirection) {
                case 0: newX -= moveSpeed; break; // Nord
                case 1: newY -= moveSpeed; break; // Est
                case 2: newX += moveSpeed; break; // Sud
                case 3: newY += moveSpeed; break; // Ouest
            }
            moved = true;
        }
        if (pressedKeys.contains(KeyCode.D)) { // Droite
            switch (playerDirection) {
                case 0: newX += moveSpeed; break; // Nord
                case 1: newY += moveSpeed; break; // Est
                case 2: newX -= moveSpeed; break; // Sud
                case 3: newY -= moveSpeed; break; // Ouest
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
        // La caméra suit le joueur - le joueur doit apparaître au centre de l'écran
        // On calcule l'offset nécessaire pour centrer le joueur
        double playerIsoX = (playerX - playerY) * (TILE_WIDTH / 2.0);
        double playerIsoY = (playerX + playerY) * (TILE_HEIGHT / 2.0);

        // L'offset repositionne la carte pour que le joueur soit au centre
        cameraOffsetX = CANVAS_WIDTH / 2.0 - playerIsoX;
        cameraOffsetY = CANVAS_HEIGHT / 2.0 - playerIsoY;
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
            case 0: return new double[]{0, -1}; // Nord
            case 1: return new double[]{1, 0};  // Est
            case 2: return new double[]{0, 1};  // Sud
            case 3: return new double[]{-1, 0}; // Ouest
            default: return new double[]{0, -1};
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
            gc.drawImage(image, tile.screenX - TILE_WIDTH/2, tile.screenY - TILE_HEIGHT,
                    TILE_WIDTH, TILE_WIDTH * image.getHeight() / image.getWidth());
            gc.restore();
        }
    }

    private void renderPlayer(GraphicsContext gc) {
        double playerScreenX = cameraOffsetX;
        double playerScreenY = cameraOffsetY+400;

        // Point d'exclamation si le chemin est bloqué
        if (pathBlocked && System.currentTimeMillis() - pathBlockedTime < 3000) {
            gc.setFill(Color.RED);
            gc.setFont(javafx.scene.text.Font.font(20));
            gc.fillText("!", playerScreenX - 4, playerScreenY - 20);
        }

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

        // Affichage du chemin si navigation active
        if (isAutoMoving && !currentPath.isEmpty()) {
            gc.setStroke(Color.LIME);
            gc.setLineWidth(2);

            for (int i = currentPathIndex; i < currentPath.size(); i++) {
                Point p = currentPath.get(i);
                double pathX = (p.x - playerX) * TILE_WIDTH + playerScreenX;
                double pathY = (p.y - playerY) * TILE_HEIGHT + playerScreenY;

                gc.strokeOval(pathX - 3, pathY - 3, 6, 6);

                if (i > currentPathIndex) {
                    Point prev = currentPath.get(i - 1);
                    double prevX = (prev.x - playerX) * TILE_WIDTH + playerScreenX;
                    double prevY = (prev.y - playerY) * TILE_HEIGHT + playerScreenY;
                    gc.strokeLine(prevX, prevY, pathX, pathY);
                }
            }
        }
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
        gc.fillText("Clic souris: Navigation auto", 10, 130);

        if (isAutoMoving) {
            gc.setFill(Color.LIME);
            gc.fillText("Navigation automatique active", 10, 160);
        } else if (pathBlocked && System.currentTimeMillis() - pathBlockedTime < 3000) {
            gc.setFill(Color.RED);
            gc.fillText("Destination inaccessible !", 10, 160);
        }

        gc.setFill(Color.WHITE);
        gc.fillText("Images chargées: " + (groundImages.size() + wallImages.size()), 10, 180);
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

    // Classes pour le pathfinding A*
    private static class Point {
        int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }
    }

    private static class AStarNode {
        int x, y;
        AStarNode parent;
        double gCost, hCost, fCost;

        AStarNode(int x, int y, AStarNode parent) {
            this.x = x;
            this.y = y;
            this.parent = parent;
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