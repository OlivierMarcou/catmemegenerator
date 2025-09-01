package com.example.catmeme;


import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

public class IsometricGame extends Application {

    // Constantes
    private static final int TILE_WIDTH = 64;
    private static final int TILE_HEIGHT = 32;
    private static final int WALL_HEIGHT = 128;
    private static final int MAP_SIZE = 50;
    private static final int CANVAS_WIDTH = 1200;
    private static final int CANVAS_HEIGHT = 800;

    // Éléments UI
    private Canvas canvas;
    private GraphicsContext gc;

    // Position de la caméra et du joueur
    private double cameraX = MAP_SIZE * TILE_WIDTH / 4;
    private double cameraY = MAP_SIZE * TILE_HEIGHT / 4;
    private Point2D playerPos = new Point2D(MAP_SIZE / 2, MAP_SIZE / 2);
    private Point2D targetPos = null;
    private List<Point2D> path = new ArrayList<>();
    private int currentPathIndex = 0;
    private boolean isMoving = false;
    private double moveProgress = 0.0;

    // Orientation du joueur
    private double mouseX, mouseY;
    private double playerAngle = 0;

    // Images
    private Map<String, Image> floorImages = new HashMap<>();
    private Map<String, Image> wallImages = new HashMap<>();
    private Map<String, Image> ceilingImages = new HashMap<>();

    // Cartes du jeu
    private int[][] floorMap = new int[MAP_SIZE][MAP_SIZE];
    private int[][] wallMap = new int[MAP_SIZE][MAP_SIZE];
    private int[][] ceilingMap = new int[MAP_SIZE][MAP_SIZE];
    private WallType[][] wallTypes = new WallType[MAP_SIZE][MAP_SIZE];
    private List<Item>[][] itemMap = new List[MAP_SIZE][MAP_SIZE];

    // Animation
    private Timeline moveTimeline;
    private boolean showExclamation = false;
    private Timeline exclamationTimeline;

    // Types de murs
    enum WallType {
        NONE, TRAVERSABLE, TRANSPARENT, DOOR, DESTRUCTIBLE, INDESTRUCTIBLE
    }

    // Classe pour les items
    static class Item {
        String type;
        int count;

        Item(String type, int count) {
            this.type = type;
            this.count = count;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        System.out.println("=== Démarrage du Jeu Isométrique ===");

        initializeImages();
        initializeMaps();

        canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        gc = canvas.getGraphicsContext2D();

        // Optimisation avec cache
        gc.setImageSmoothing(false);

        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Événements souris
        canvas.setOnMouseMoved(this::onMouseMoved);
        canvas.setOnMouseClicked(this::onMouseClicked);

        primaryStage.setTitle("Jeu Isométrique - Carte depuis Resources");
        primaryStage.setScene(scene);
        primaryStage.show();

        System.out.println("=== Jeu démarré avec succès ! ===");
        System.out.println("Instructions :");
        System.out.println("- Bougez la souris pour orienter le personnage");
        System.out.println("- Cliquez sur une case pour vous déplacer");
        System.out.println("- Le personnage contourne automatiquement les obstacles");
        System.out.println("- Carte chargée depuis src/main/resources/village_map.json");

        // Boucle de rendu
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                render();
            }
        }.start();
    }

    private void initializeImages() {
        // Chargement des images générées depuis les resources
        try {
            System.out.println("Chargement des images générées...");

            // Images de sol (50 types générés)
            for (int i = 0; i < 50; i++) {
                String path = "/sol/floor_" + i + ".jpg";
                Image image = loadImageOrDefault(path, createDefaultFloorImage());
                floorImages.put("floor_" + i, image);
            }
            System.out.println("Images de sol chargées : " + floorImages.size());

            // Images de murs (50 types générés)
            for (int i = 0; i < 50; i++) {
                String path = "/murs/wall_" + i + ".jpg";
                Image image = loadImageOrDefault(path, createDefaultWallImage());
                wallImages.put("wall_" + i, image);
            }
            System.out.println("Images de murs chargées : " + wallImages.size());

            // Images de plafonds (30 types générés)
            for (int i = 0; i < 30; i++) {
                String path = "/plafonds/ceiling_" + i + ".jpg";
                Image image = loadImageOrDefault(path, createDefaultCeilingImage());
                ceilingImages.put("ceiling_" + i, image);
            }
            System.out.println("Images de plafonds chargées : " + ceilingImages.size());

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des images: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Image loadImageOrDefault(String path, Image defaultImage) {
        try {
            return new Image(getClass().getResourceAsStream(path));
        } catch (Exception e) {
            return defaultImage;
        }
    }

    private Image createDefaultFloorImage() {
        javafx.scene.canvas.Canvas tempCanvas = new javafx.scene.canvas.Canvas(TILE_WIDTH, TILE_HEIGHT);
        GraphicsContext tempGc = tempCanvas.getGraphicsContext2D();
        tempGc.setFill(Color.LIGHTGRAY);
        tempGc.fillRect(0, 0, TILE_WIDTH, TILE_HEIGHT);
        tempGc.setStroke(Color.DARKGRAY);
        tempGc.strokeRect(0, 0, TILE_WIDTH, TILE_HEIGHT);
        return tempCanvas.snapshot(null, null);
    }

    private Image createDefaultWallImage() {
        javafx.scene.canvas.Canvas tempCanvas = new javafx.scene.canvas.Canvas(TILE_WIDTH, WALL_HEIGHT);
        GraphicsContext tempGc = tempCanvas.getGraphicsContext2D();
        tempGc.setFill(Color.GRAY);
        tempGc.fillRect(0, 0, TILE_WIDTH, WALL_HEIGHT);
        tempGc.setStroke(Color.BLACK);
        tempGc.strokeRect(0, 0, TILE_WIDTH, WALL_HEIGHT);
        return tempCanvas.snapshot(null, null);
    }

    private Image createDefaultCeilingImage() {
        javafx.scene.canvas.Canvas tempCanvas = new javafx.scene.canvas.Canvas(TILE_WIDTH, TILE_HEIGHT);
        GraphicsContext tempGc = tempCanvas.getGraphicsContext2D();
        tempGc.setFill(Color.DARKGRAY);
        tempGc.fillRect(0, 0, TILE_WIDTH, TILE_HEIGHT);
        tempGc.setStroke(Color.BLACK);
        tempGc.strokeRect(0, 0, TILE_WIDTH, TILE_HEIGHT);
        return tempCanvas.snapshot(null, null);
    }

    private void initializeMaps() {
        System.out.println("Initialisation des cartes...");

        // Charger la carte depuis les resources
        if (!loadMapFromResources("village_map.json")) {
            System.out.println("Impossible de charger village_map.json des resources, génération de données par défaut...");
//            generateDefaultMap();
            throw new RuntimeException("pas de carte");
        }

        System.out.println("Cartes initialisées avec succès !");
    }

    private boolean loadMapFromResources(String filename) {
        try {
            System.out.println("Chargement de la carte depuis resources : " + filename);

            // Charger le fichier JSON depuis les resources
            java.io.InputStream inputStream = getClass().getResourceAsStream("/" + filename);

            if (inputStream == null) {
                System.out.println("Fichier " + filename + " non trouvé dans les resources");
                return false;
            }

            // Lire le contenu du fichier
            StringBuilder jsonContent = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonContent.append(line).append("\n");
                }
            }

            // Parser le JSON
            String json = jsonContent.toString();

            if (parseMapFromJson(json)) {
                System.out.println("Carte chargée avec succès depuis resources/" + filename);
                return true;
            }

        } catch (IOException e) {
            System.out.println("Erreur lors du chargement de " + filename + " : " + e.getMessage());
        }

        return false;
    }

    private boolean parseMapFromJson(String json) {
        try {
            // Extraire la taille de la carte
            String mapSizePattern = "\"mapSize\":\\s*(\\d+)";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(mapSizePattern);
            java.util.regex.Matcher matcher = pattern.matcher(json);

            int loadedMapSize = MAP_SIZE; // Par défaut
            if (matcher.find()) {
                loadedMapSize = Integer.parseInt(matcher.group(1));
            }

            // Ajuster la taille si nécessaire
            if (loadedMapSize != MAP_SIZE) {
                System.out.println("Attention : taille de carte différente (" + loadedMapSize + " vs " + MAP_SIZE + ")");
            }

            // Parser les cartes (version simplifiée - extraction des tableaux)
            if (parseArrayFromJson(json, "floorMap", floorMap) &&
                    parseArrayFromJson(json, "wallMap", wallMap) &&
                    parseArrayFromJson(json, "ceilingMap", ceilingMap) &&
                    parseWallTypesFromJson(json) &&
                    parseItemsFromJson(json)) {

                return true;
            }

        } catch (Exception e) {
            System.err.println("Erreur lors du parsing JSON : " + e.getMessage());
        }

        return false;
    }

    private boolean parseArrayFromJson(String json, String arrayName, int[][] targetArray) {
        try {
            String pattern = "\"" + arrayName + "\":\\s*\\[(.*?)\\](?=\\s*[,}])";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher m = p.matcher(json);

            if (m.find()) {
                String arrayContent = m.group(1);
                String[] rows = arrayContent.split("\\],\\s*\\[");

                for (int x = 0; x < Math.min(rows.length, MAP_SIZE); x++) {
                    String row = rows[x].replaceAll("[\\[\\]\\s]", "");
                    String[] values = row.split(",");

                    for (int y = 0; y < Math.min(values.length, MAP_SIZE); y++) {
                        try {
                            targetArray[x][y] = Integer.parseInt(values[y].trim());
                        } catch (NumberFormatException e) {
                            targetArray[x][y] = -1; // Valeur par défaut pour les erreurs
                        }
                    }
                }

                System.out.println("Tableau " + arrayName + " chargé avec succès");
                return true;
            }

        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de " + arrayName + ": " + e.getMessage());
        }

        return false;
    }

    private boolean parseWallTypesFromJson(String json) {
        try {
            String pattern = "\"wallTypes\":\\s*\\[(.*?)\\](?=\\s*[,}])";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher m = p.matcher(json);

            if (m.find()) {
                String arrayContent = m.group(1);
                String[] rows = arrayContent.split("\\],\\s*\\[");

                for (int x = 0; x < Math.min(rows.length, MAP_SIZE); x++) {
                    String row = rows[x].replaceAll("[\\[\\]\\s\"]", "");
                    String[] values = row.split(",\\s*");

                    for (int y = 0; y < Math.min(values.length, MAP_SIZE); y++) {
                        try {
                            String wallTypeName = values[y].trim().replace("\"", "");
                            wallTypes[x][y] = WallType.valueOf(wallTypeName);
                        } catch (Exception e) {
                            wallTypes[x][y] = WallType.NONE;
                        }
                    }
                }

                System.out.println("Types de murs chargés avec succès");
                return true;
            }

        } catch (Exception e) {
            System.err.println("Erreur lors du parsing des wallTypes: " + e.getMessage());
        }

        return false;
    }

    private boolean parseItemsFromJson(String json) {
        try {
            // Initialiser toutes les listes d'items
            for (int x = 0; x < MAP_SIZE; x++) {
                for (int y = 0; y < MAP_SIZE; y++) {
                    itemMap[x][y] = new ArrayList<>();
                }
            }

            // Parser la structure des items (version simplifiée)
            String pattern = "\"itemMap\":\\s*\\[(.*?)\\](?=\\s*[}])";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher m = p.matcher(json);

            if (m.find()) {
                // Pour cette version, on va juste générer quelques items aléatoires
                // car le parsing complet des items JSON est complexe
                Random rand = new Random();
                for (int x = 0; x < MAP_SIZE; x++) {
                    for (int y = 0; y < MAP_SIZE; y++) {
                        if (rand.nextDouble() < 0.03) { // 3% de chance
                            String[] itemTypes = {"coin", "potion", "key", "herb", "gem"};
                            String itemType = itemTypes[rand.nextInt(itemTypes.length)];
                            itemMap[x][y].add(new Item(itemType, 1 + rand.nextInt(3)));
                        }
                    }
                }

                System.out.println("Items générés (parsing simplifié)");
                return true;
            }

        } catch (Exception e) {
            System.err.println("Erreur lors du parsing des items: " + e.getMessage());
        }

        return false;
    }

    private void onMouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();

        // Calculer l'angle vers la souris
        double centerX = CANVAS_WIDTH / 2;
        double centerY = CANVAS_HEIGHT / 2;
        playerAngle = Math.atan2(mouseY - centerY, mouseX - centerX);
    }

    private void onMouseClicked(MouseEvent e) {
        Point2D clickedTile = screenToTile(e.getX(), e.getY());
        if (clickedTile != null && isValidTile((int) clickedTile.getX(), (int) clickedTile.getY())) {
            moveToPosition(clickedTile);
        }
    }

    private Point2D screenToTile(double screenX, double screenY) {
        // Conversion des coordonnées écran vers coordonnées isométriques
        double relX = screenX - CANVAS_WIDTH / 2 + cameraX;
        double relY = screenY - CANVAS_HEIGHT / 2 + cameraY;

        double tileX = (relX / TILE_WIDTH + relY / TILE_HEIGHT) / 2;
        double tileY = (relY / TILE_HEIGHT - relX / TILE_WIDTH) / 2;

        return new Point2D(Math.floor(tileX), Math.floor(tileY));
    }

    private Point2D tileToScreen(double tileX, double tileY) {
        double screenX = (tileX - tileY) * TILE_WIDTH / 2 - cameraX + CANVAS_WIDTH / 2;
        double screenY = (tileX + tileY) * TILE_HEIGHT / 2 - cameraY + CANVAS_HEIGHT / 2;
        return new Point2D(screenX, screenY);
    }

    private boolean isValidTile(int x, int y) {
        return x >= 0 && x < MAP_SIZE && y >= 0 && y < MAP_SIZE;
    }

    private boolean canWalkThrough(int x, int y) {
        if (!isValidTile(x, y)) return false;
        WallType wall = wallTypes[x][y];
        return wall == WallType.NONE || wall == WallType.TRAVERSABLE || wall == WallType.TRANSPARENT;
    }

    private void moveToPosition(Point2D target) {
        if (isMoving) return;

        targetPos = target;
        path = findPath(playerPos, targetPos);

        if (path.isEmpty()) {
            showExclamationMark();
            return;
        }

        currentPathIndex = 0;
        isMoving = true;
        moveProgress = 0.0;

        if (moveTimeline != null) {
            moveTimeline.stop();
        }

        moveTimeline = new Timeline(new KeyFrame(Duration.millis(16), e -> updateMovement()));
        moveTimeline.setCycleCount(Timeline.INDEFINITE);
        moveTimeline.play();
    }

    private void updateMovement() {
        if (!isMoving || path.isEmpty()) return;

        moveProgress += 0.05; // Vitesse de déplacement

        if (moveProgress >= 1.0) {
            moveProgress = 0.0;
            currentPathIndex++;

            if (currentPathIndex >= path.size()) {
                isMoving = false;
                playerPos = targetPos;
                moveTimeline.stop();
                return;
            }

            playerPos = path.get(currentPathIndex - 1);
        }

        // Mise à jour de la caméra pour suivre le joueur
        Point2D currentTarget = path.get(Math.min(currentPathIndex, path.size() - 1));
        Point2D screenPos = tileToScreen(currentTarget.getX(), currentTarget.getY());
        cameraX += (screenPos.getX() - CANVAS_WIDTH / 2) * 0.1;
        cameraY += (screenPos.getY() - CANVAS_HEIGHT / 2) * 0.1;
    }

    private List<Point2D> findPath(Point2D start, Point2D end) {
        // Algorithme A* simplifié
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<String> closedSet = new HashSet<>();
        Map<String, Node> allNodes = new HashMap<>();

        Node startNode = new Node((int) start.getX(), (int) start.getY(), null, 0, heuristic(start, end));
        openSet.add(startNode);
        allNodes.put(startNode.getKey(), startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            String currentKey = current.getKey();

            if (closedSet.contains(currentKey)) continue;
            closedSet.add(currentKey);

            if (current.x == (int) end.getX() && current.y == (int) end.getY()) {
                return reconstructPath(current);
            }

            for (int[] dir : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                int newX = current.x + dir[0];
                int newY = current.y + dir[1];
                String newKey = newX + "," + newY;

                if (!canWalkThrough(newX, newY) || closedSet.contains(newKey)) continue;

                double newG = current.g + 1;
                double newH = heuristic(new Point2D(newX, newY), end);
                Node neighbor = new Node(newX, newY, current, newG, newH);

                Node existing = allNodes.get(newKey);
                if (existing == null || newG < existing.g) {
                    allNodes.put(newKey, neighbor);
                    openSet.add(neighbor);
                }
            }
        }

        return new ArrayList<>(); // Pas de chemin trouvé
    }

    private double heuristic(Point2D a, Point2D b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
    }

    private List<Point2D> reconstructPath(Node endNode) {
        List<Point2D> path = new ArrayList<>();
        Node current = endNode;

        while (current != null) {
            path.add(0, new Point2D(current.x, current.y));
            current = current.parent;
        }

        return path;
    }

    private void showExclamationMark() {
        showExclamation = true;
        if (exclamationTimeline != null) {
            exclamationTimeline.stop();
        }
        exclamationTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> showExclamation = false));
        exclamationTimeline.play();
    }

    private void render() {
        gc.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Rendu en ordre isométrique (de l'arrière vers l'avant)
        for (int sum = 0; sum < MAP_SIZE * 2; sum++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                int y = sum - x;
                if (y < 0 || y >= MAP_SIZE) continue;

                renderTile(x, y);
            }
        }

        renderPlayer();
    }

    private void renderTile(int x, int y) {
        Point2D screenPos = tileToScreen(x, y);
        double screenX = screenPos.getX();
        double screenY = screenPos.getY();

        // Vérification si la tuile est visible
        if (screenX < -TILE_WIDTH || screenX > CANVAS_WIDTH + TILE_WIDTH ||
                screenY < -WALL_HEIGHT || screenY > CANVAS_HEIGHT + TILE_HEIGHT) {
            return;
        }

        // Rendu du sol
        if (floorMap[x][y] >= 0) {
            Image floorImg = floorImages.get("floor_" + floorMap[x][y]);
            if (floorImg != null) {
                gc.drawImage(floorImg, screenX - TILE_WIDTH / 2, screenY - TILE_HEIGHT / 2);
            } else {
                // Image de sol par défaut si pas trouvée
                Image defaultFloor = createDefaultFloorImage();
                gc.drawImage(defaultFloor, screenX - TILE_WIDTH / 2, screenY - TILE_HEIGHT / 2);
            }
        }

        // Rendu des items avec meilleure visibilité
        if (!itemMap[x][y].isEmpty()) {
            // Fond circulaire pour les items
            gc.setFill(Color.YELLOW.deriveColor(0, 1, 1, 0.8));
            gc.fillOval(screenX - 6, screenY - 6, 12, 12);

            // Croix rouge pour les items
            gc.setStroke(Color.DARKRED);
            gc.setLineWidth(2);
            gc.strokeLine(screenX - 5, screenY, screenX + 5, screenY);
            gc.strokeLine(screenX, screenY - 5, screenX, screenY + 5);

            // Afficher le nombre d'items si plus de 1
            int totalItems = itemMap[x][y].stream().mapToInt(item -> item.count).sum();
            if (totalItems > 1) {
                gc.setFill(Color.WHITE);
                gc.fillText(String.valueOf(totalItems), screenX + 6, screenY - 6);
            }
        }

        // Calcul de la transparence pour les murs et plafonds
        double alpha = 1.0;
        double distanceToPlayer = Math.abs(x - playerPos.getX()) + Math.abs(y - playerPos.getY());

        // Vérification si l'élément est devant le joueur
        if (isInFrontOfPlayer(x, y)) {
            if (distanceToPlayer <= 1) {
                alpha = 0.2; // 80% transparent
            } else if (distanceToPlayer <= 2) {
                alpha = 0.5; // 50% transparent
            }
        }

        // Rendu des murs
        if (wallMap[x][y] >= 0) {
            Image wallImg = wallImages.get("wall_" + wallMap[x][y]);
            if (wallImg != null) {
                gc.setGlobalAlpha(alpha);
                gc.drawImage(wallImg, screenX - TILE_WIDTH / 2, screenY - WALL_HEIGHT + TILE_HEIGHT / 2);
                gc.setGlobalAlpha(1.0);
            } else {
                // Image de mur par défaut si pas trouvée
                gc.setGlobalAlpha(alpha);
                Image defaultWall = createDefaultWallImage();
                gc.drawImage(defaultWall, screenX - TILE_WIDTH / 2, screenY - WALL_HEIGHT + TILE_HEIGHT / 2);
                gc.setGlobalAlpha(1.0);
            }
        }

        // Rendu des plafonds
        if (ceilingMap[x][y] >= 0) {
            Image ceilingImg = ceilingImages.get("ceiling_" + ceilingMap[x][y]);
            if (ceilingImg != null) {
                gc.setGlobalAlpha(alpha);
                gc.drawImage(ceilingImg, screenX - TILE_WIDTH / 2, screenY - TILE_HEIGHT / 2 - WALL_HEIGHT);
                gc.setGlobalAlpha(1.0);
            } else {
                // Image de plafond par défaut si pas trouvée
                gc.setGlobalAlpha(alpha);
                Image defaultCeiling = createDefaultCeilingImage();
                gc.drawImage(defaultCeiling, screenX - TILE_WIDTH / 2, screenY - TILE_HEIGHT / 2 - WALL_HEIGHT);
                gc.setGlobalAlpha(1.0);
            }
        }
    }

    private boolean isInFrontOfPlayer(int x, int y) {
        double dx = x - playerPos.getX();
        double dy = y - playerPos.getY();
        double angle = Math.atan2(dy, dx);
        double angleDiff = Math.abs(angle - playerAngle);

        // Normaliser l'angle
        while (angleDiff > Math.PI) {
            angleDiff -= 2 * Math.PI;
        }
        while (angleDiff < -Math.PI) {
            angleDiff += 2 * Math.PI;
        }

        return Math.abs(angleDiff) < Math.PI / 2; // 90 degrés de chaque côté
    }

    private void renderPlayer() {
        Point2D screenPos = tileToScreen(playerPos.getX(), playerPos.getY());
        double screenX = screenPos.getX();
        double screenY = screenPos.getY();

        // Si en mouvement, interpoler la position
        if (isMoving && currentPathIndex < path.size()) {
            Point2D nextPos = path.get(currentPathIndex);
            Point2D nextScreenPos = tileToScreen(nextPos.getX(), nextPos.getY());

            screenX += (nextScreenPos.getX() - screenX) * moveProgress;
            screenY += (nextScreenPos.getY() - screenY) * moveProgress;
        }

        // Couleur du joueur basée sur l'orientation
        double hue = (playerAngle + Math.PI) / (2 * Math.PI) * 360;
        gc.setFill(Color.hsb(hue, 1.0, 1.0));
        gc.fillOval(screenX - 8, screenY - 8, 16, 16);

        // Direction du joueur
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        double dirX = screenX + Math.cos(playerAngle) * 12;
        double dirY = screenY + Math.sin(playerAngle) * 12;
        gc.strokeLine(screenX, screenY, dirX, dirY);

        // Point d'exclamation si nécessaire
        if (showExclamation) {
            gc.setFill(Color.YELLOW);
            gc.fillOval(screenX - 4, screenY - 24, 8, 8);
            gc.setFill(Color.RED);
            gc.fillText("!", screenX - 3, screenY - 18);
        }
    }

    // Classe pour l'algorithme A*
    private static class Node implements Comparable<Node> {
        int x, y;
        Node parent;
        double g, h;

        Node(int x, int y, Node parent, double g, double h) {
            this.x = x;
            this.y = y;
            this.parent = parent;
            this.g = g;
            this.h = h;
        }

        double getF() {
            return g + h;
        }

        String getKey() {
            return x + "," + y;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.getF(), other.getF());
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Jeu Isométrique avec Carte des Resources ===");
        System.out.println("Recherche des images générées...");
        System.out.println("Recherche de la carte : src/main/resources/village_map.json");
        System.out.println();

        launch(args);
    }
}