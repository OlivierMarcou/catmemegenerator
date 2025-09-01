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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class IsometricGame2 extends Application {

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
    private double cameraX;
    private double cameraY;
    private Point2D playerPos = new Point2D(MAP_SIZE / 2, MAP_SIZE / 2);
    private Point2D targetPos = null;
    private Point2D clickedPos = null; // Position où on a cliqué
    private Point2D mouseHoverPos = null; // Position du curseur souris
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
    private WallProperties[][] wallProperties = new WallProperties[MAP_SIZE][MAP_SIZE];
    private List<Item>[][] itemMap = new List[MAP_SIZE][MAP_SIZE];
    private Set<String> playerKeys = new HashSet<>(); // Clés possédées par le joueur

    // Animation
    private Timeline moveTimeline;
    private boolean showExclamation = false;
    private Timeline exclamationTimeline;
    private String messageAbovePlayer = null; // Message à afficher au-dessus du personnage
    private Timeline messageTimeline;

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

    // Classe pour les propriétés avancées des murs
    static class WallProperties {
        boolean isOpen = false; // Pour les portes
        boolean isLocked = false; // Pour les portes verrouillées
        String keyId = null; // ID unique de la clé nécessaire
        int health = 255; // Vie des murs destructibles (0-255)

        WallProperties() {}

        WallProperties(boolean isOpen, boolean isLocked, String keyId, int health) {
            this.isOpen = isOpen;
            this.isLocked = isLocked;
            this.keyId = keyId;
            this.health = Math.max(0, Math.min(255, health));
        }
    }

    @Override
    public void start(Stage primaryStage) {
        System.out.println("=== Démarrage du Jeu Isométrique ===");

        initializeImages();
        initializeMaps();

        // Centrer la caméra sur le personnage après initialisation des cartes
        centerCameraOnPlayer();

        canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        gc = canvas.getGraphicsContext2D();

        // Optimisation
        gc.setImageSmoothing(false);

        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Événements souris
        canvas.setOnMouseMoved(this::onMouseMoved);
        canvas.setOnMouseClicked(this::onMouseClicked);
        canvas.setOnMousePressed(this::onMousePressed); // Pour gérer les clics sur les portes

        primaryStage.setTitle("Jeu Isométrique - Village");
        primaryStage.setScene(scene);
        primaryStage.show();

        System.out.println("=== Jeu démarré ! ===");
        System.out.println("- Bougez la souris pour orienter le personnage");
        System.out.println("- Cliquez pour vous déplacer");

        // Boucle de rendu
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                render();
            }
        }.start();
    }

    private void initializeImages() {
        System.out.println("Chargement des images PNG transparentes...");

        // Charger les 50 images de sol
        for (int i = 0; i < 50; i++) {
            Image image = loadImage("/sol/floor_" + i + ".png");
            if (image != null) {
                floorImages.put("floor_" + i, image);
            }
        }

        // Charger les 50 images de murs
        for (int i = 0; i < 50; i++) {
            Image image = loadImage("/murs/wall_" + i + ".png");
            if (image != null) {
                wallImages.put("wall_" + i, image);
            }
        }

        // Charger les 30 images de plafonds
        for (int i = 0; i < 30; i++) {
            Image image = loadImage("/plafonds/ceiling_" + i + ".png");
            if (image != null) {
                ceilingImages.put("ceiling_" + i, image);
            }
        }

        System.out.println("Images PNG chargées: " + floorImages.size() + " sols, " +
                wallImages.size() + " murs, " + ceilingImages.size() + " plafonds");
    }

    private Image loadImage(String path) {
        try {
            InputStream stream = getClass().getResourceAsStream(path);
            if (stream != null) {
                return new Image(stream);
            }
        } catch (Exception e) {
            // Image par défaut si non trouvée
        }
        return createDefaultImage();
    }

    private Image createDefaultImage() {
        Canvas tempCanvas = new Canvas(TILE_WIDTH, TILE_HEIGHT);
        GraphicsContext tempGc = tempCanvas.getGraphicsContext2D();

        // Créer une image par défaut avec fond transparent
        tempGc.clearRect(0, 0, TILE_WIDTH, TILE_HEIGHT);

        // Forme losange isométrique transparent par défaut
        double[] xPoints = {TILE_WIDTH/2.0, TILE_WIDTH, TILE_WIDTH/2.0, 0};
        double[] yPoints = {0, TILE_HEIGHT/2.0, TILE_HEIGHT, TILE_HEIGHT/2.0};

        tempGc.setFill(Color.LIGHTGRAY.deriveColor(0, 1, 1, 0.8)); // Semi-transparent
        tempGc.fillPolygon(xPoints, yPoints, 4);

        tempGc.setStroke(Color.BLACK.deriveColor(0, 1, 1, 0.8));
        tempGc.setLineWidth(1);
        tempGc.strokePolygon(xPoints, yPoints, 4);

        return tempCanvas.snapshot(null, null);
    }

    private void initializeMaps() {
        System.out.println("Chargement de la carte...");

        if (!loadMapFromJson()) {
            System.out.println("village_map.json non trouvé, génération d'une carte par défaut");
            generateDefaultMap();
        }

        System.out.println("Carte initialisée!");
    }

    private boolean loadMapFromJson() {
        try {
            System.out.println("Tentative de chargement de village_map.json...");
            InputStream stream = getClass().getResourceAsStream("/village_map.json");
            if (stream == null) {
                System.out.println("❌ Fichier /village_map.json non trouvé dans les resources");
                return false;
            }

            StringBuilder json = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line).append("\n");
            }
            reader.close();

            String jsonContent = json.toString();
            System.out.println("✅ Fichier JSON lu, taille: " + jsonContent.length() + " caractères");

            // Vérifier la structure de base
            if (!jsonContent.contains("\"floorMap\"") || !jsonContent.contains("\"wallMap\"")) {
                System.out.println("❌ Structure JSON invalide - floorMap ou wallMap manquant");
                return false;
            }

            return parseJson(jsonContent);

        } catch (Exception e) {
            System.err.println("❌ Erreur chargement JSON: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean parseJson(String json) {
        try {
            System.out.println("🔍 Début du parsing JSON...");

            // Parser chaque section avec debug
            boolean floorSuccess = parseFloorMap(json);
            boolean wallSuccess = parseWallMap(json);
            boolean wallTypeSuccess = parseWallTypes(json);

            // Initialiser les items (vides pour simplicité)
            initializeEmptyItems();

            System.out.println("📊 Résultats du parsing:");
            System.out.println("  - FloorMap: " + (floorSuccess ? "✅" : "❌"));
            System.out.println("  - WallMap: " + (wallSuccess ? "✅" : "❌"));
            System.out.println("  - WallTypes: " + (wallTypeSuccess ? "✅" : "❌"));

            if (floorSuccess && wallSuccess) {
                System.out.println("✅ Carte JSON chargée avec succès!");
                printLoadedMapStats();
                return true;
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur parsing JSON: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    private boolean parseFloorMap(String json) {
        try {
            String floorData = extractJsonArray(json, "floorMap");
            if (floorData == null) {
                System.out.println("❌ FloorMap non trouvé dans le JSON");
                return false;
            }

            System.out.println("🔍 Parsing FloorMap, taille données: " + floorData.length());
            parseIntArrayData(floorData, floorMap);

            // Vérification
            int nonZeroCount = 0;
            for (int x = 0; x < MAP_SIZE; x++) {
                for (int y = 0; y < MAP_SIZE; y++) {
                    if (floorMap[x][y] > 0) nonZeroCount++;
                }
            }
            System.out.println("✅ FloorMap chargé - " + nonZeroCount + " cases non-nulles");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur parsing FloorMap: " + e.getMessage());
            return false;
        }
    }

    private boolean parseWallMap(String json) {
        try {
            String wallData = extractJsonArray(json, "wallMap");
            if (wallData == null) {
                System.out.println("❌ WallMap non trouvé dans le JSON");
                return false;
            }

            System.out.println("🔍 Parsing WallMap, taille données: " + wallData.length());
            parseIntArrayData(wallData, wallMap);

            // Vérification
            int wallCount = 0;
            for (int x = 0; x < MAP_SIZE; x++) {
                for (int y = 0; y < MAP_SIZE; y++) {
                    if (wallMap[x][y] != -1) wallCount++;
                }
            }
            System.out.println("✅ WallMap chargé - " + wallCount + " murs trouvés");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur parsing WallMap: " + e.getMessage());
            return false;
        }
    }

    private boolean parseWallTypes(String json) {
        try {
            String wallTypeData = extractJsonArray(json, "wallTypes");
            if (wallTypeData == null) {
                System.out.println("❌ WallTypes non trouvé dans le JSON");
                return false;
            }

            System.out.println("🔍 Parsing WallTypes...");
            parseWallTypeArrayData(wallTypeData);
            System.out.println("✅ WallTypes chargé");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur parsing WallTypes: " + e.getMessage());
            return false;
        }
    }

    private String extractJsonArray(String json, String arrayName) {
        String startMarker = "\"" + arrayName + "\":[";
        int startIndex = json.indexOf(startMarker);

        if (startIndex == -1) {
            System.out.println("❌ Marqueur de début non trouvé pour: " + arrayName);
            return null;
        }

        startIndex += startMarker.length() - 1; // Position sur '['

        int brackets = 0;
        int endIndex = startIndex;

        for (int i = startIndex; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') {
                brackets++;
            } else if (c == ']') {
                brackets--;
                if (brackets == 0) {
                    endIndex = i + 1;
                    break;
                }
            }
        }

        if (brackets != 0) {
            System.out.println("❌ Crochets non équilibrés pour: " + arrayName);
            return null;
        }

        String result = json.substring(startIndex, endIndex);
        System.out.println("🔍 Array " + arrayName + " extrait, longueur: " + result.length());
        return result;
    }

    private void parseIntArrayData(String arrayData, int[][] target) {
        // Nettoyer les données
        arrayData = arrayData.trim();
        if (arrayData.startsWith("[")) arrayData = arrayData.substring(1);
        if (arrayData.endsWith("]")) arrayData = arrayData.substring(0, arrayData.length() - 1);

        // Diviser en lignes
        String[] lines = arrayData.split("\\],\\s*\\[");
        System.out.println("🔍 Nombre de lignes trouvées: " + lines.length);

        for (int x = 0; x < Math.min(lines.length, MAP_SIZE); x++) {
            String line = lines[x].replaceAll("[\\[\\]\\s]", "");
            String[] values = line.split(",");

            for (int y = 0; y < Math.min(values.length, MAP_SIZE); y++) {
                try {
                    target[x][y] = Integer.parseInt(values[y].trim());
                } catch (NumberFormatException e) {
                    target[x][y] = -1;
                    System.out.println("⚠️ Erreur parsing valeur à [" + x + "][" + y + "]: " + values[y]);
                }
            }
        }
    }

    private void parseWallTypeArrayData(String arrayData) {
        arrayData = arrayData.trim();
        if (arrayData.startsWith("[")) arrayData = arrayData.substring(1);
        if (arrayData.endsWith("]")) arrayData = arrayData.substring(0, arrayData.length() - 1);

        String[] lines = arrayData.split("\\],\\s*\\[");

        for (int x = 0; x < Math.min(lines.length, MAP_SIZE); x++) {
            String line = lines[x].replaceAll("[\\[\\]\\s]", "");
            String[] values = line.split(",");

            for (int y = 0; y < Math.min(values.length, MAP_SIZE); y++) {
                try {
                    String wallTypeName = values[y].trim().replace("\"", "");
                    wallTypes[x][y] = WallType.valueOf(wallTypeName);
                } catch (Exception e) {
                    wallTypes[x][y] = WallType.NONE;
                }
            }
        }
    }

    private void initializeEmptyItems() {
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                itemMap[x][y] = new ArrayList<>();

                // Initialiser les propriétés des murs
                wallProperties[x][y] = new WallProperties();

                // Configuration par défaut selon le type de mur
                if (wallTypes[x][y] == WallType.DOOR) {
                    // Portes fermées par défaut
                    wallProperties[x][y].isOpen = false;

                    // 20% de chance d'être verrouillées
                    if (Math.random() < 0.2) {
                        wallProperties[x][y].isLocked = true;
                        wallProperties[x][y].keyId = "key_" + (x * MAP_SIZE + y); // ID unique
                    }
                } else if (wallTypes[x][y] == WallType.DESTRUCTIBLE) {
                    // Murs destructibles avec vie aléatoire
                    wallProperties[x][y].health = 100 + (int)(Math.random() * 156); // 100-255
                }
            }
        }

        // Ajouter quelques clés aléatoires au joueur pour test
        playerKeys.add("key_1250");
        playerKeys.add("key_750");
        System.out.println("🗝️ Clés du joueur: " + playerKeys);
    }

    private void printLoadedMapStats() {
        int wallCount = 0;
        Map<Integer, Integer> floorStats = new HashMap<>();

        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                // Compter les murs
                if (wallMap[x][y] != -1) wallCount++;

                // Compter les types de sol
                int floorType = floorMap[x][y];
                floorStats.put(floorType, floorStats.getOrDefault(floorType, 0) + 1);
            }
        }

        System.out.println("📊 Statistiques de la carte chargée:");
        System.out.println("  - Nombre de murs: " + wallCount);
        System.out.println("  - Types de sol les plus fréquents:");
        floorStats.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .forEach(entry -> System.out.println("    Type " + entry.getKey() + ": " + entry.getValue() + " cases"));
    }

    private void generateDefaultMap() {
        Random rand = new Random();

        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                // Sol aléatoire
                floorMap[x][y] = rand.nextInt(50);

                // Murs occasionnels
                if (rand.nextDouble() < 0.1) {
                    wallMap[x][y] = rand.nextInt(50);
                    wallTypes[x][y] = WallType.values()[1 + rand.nextInt(WallType.values().length - 1)];
                } else {
                    wallMap[x][y] = -1;
                    wallTypes[x][y] = WallType.NONE;
                }

                // Plafonds rares
                if (rand.nextDouble() < 0.05) {
                    ceilingMap[x][y] = rand.nextInt(30);
                } else {
                    ceilingMap[x][y] = -1;
                }

                // Items
                itemMap[x][y] = new ArrayList<>();
                if (rand.nextDouble() < 0.03) {
                    itemMap[x][y].add(new Item("treasure", 1 + rand.nextInt(3)));
                }

                // Initialiser les propriétés des murs
                wallProperties[x][y] = new WallProperties();
                if (wallTypes[x][y] == WallType.DOOR) {
                    // Quelques portes verrouillées pour test
                    if (rand.nextDouble() < 0.3) {
                        wallProperties[x][y].isLocked = true;
                        wallProperties[x][y].keyId = "key_" + (x * MAP_SIZE + y);
                    }
                } else if (wallTypes[x][y] == WallType.DESTRUCTIBLE) {
                    wallProperties[x][y].health = 50 + rand.nextInt(206); // 50-255
                }
            }
        }
    }

    private void onMouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();

        // Debug des coordonnées (activer au besoin)
        // System.out.println("Mouse: (" + String.format("%.1f", mouseX) + ", " + String.format("%.1f", mouseY) + ")");

        double centerX = CANVAS_WIDTH / 2;
        double centerY = CANVAS_HEIGHT / 2;
        playerAngle = Math.atan2(mouseY - centerY, mouseX - centerX);

        // Calculer la position de la tuile sous le curseur avec vérification
        Point2D calculatedTile = screenToTile(mouseX, mouseY);

        // Vérifier que la conversion est cohérente
        if (calculatedTile != null && isValidTile((int)calculatedTile.getX(), (int)calculatedTile.getY())) {
            mouseHoverPos = calculatedTile;

            // Debug optionnel de la conversion
            /*
            Point2D backToScreen = tileToScreen(calculatedTile.getX(), calculatedTile.getY());
            double deltaX = Math.abs(backToScreen.getX() - mouseX);
            double deltaY = Math.abs(backToScreen.getY() - mouseY);
            if (deltaX > TILE_WIDTH || deltaY > TILE_HEIGHT) {
                System.out.println("⚠️ Décalage détecté: ΔX=" + String.format("%.1f", deltaX) + ", ΔY=" + String.format("%.1f", deltaY));
            }
            */
        } else {
            mouseHoverPos = null;
        }
    }

    private void onMouseClicked(MouseEvent e) {
        Point2D clickedTile = screenToTile(e.getX(), e.getY());
        if (clickedTile != null && isValidTile((int)clickedTile.getX(), (int)clickedTile.getY())) {
            clickedPos = clickedTile; // Sauvegarder la position cliquée
            moveToPosition(clickedTile);
        }
    }

    private void onMousePressed(MouseEvent e) {
        Point2D clickedTile = screenToTile(e.getX(), e.getY());
        if (clickedTile != null && isValidTile((int)clickedTile.getX(), (int)clickedTile.getY())) {
            handleDoorInteraction((int)clickedTile.getX(), (int)clickedTile.getY());
        }
    }

    private void handleDoorInteraction(int x, int y) {
        // Vérifier si c'est une porte
        if (wallTypes[x][y] != WallType.DOOR || wallMap[x][y] == -1) {
            return;
        }

        // Vérifier si le joueur est adjacent à la porte
        double distance = Math.abs(x - playerPos.getX()) + Math.abs(y - playerPos.getY());
        if (distance > 1.5) { // Un peu de marge pour la diagonale
            return;
        }

        WallProperties props = wallProperties[x][y];
        if (props == null) {
            props = new WallProperties();
            wallProperties[x][y] = props;
        }

        // Si la porte est verrouillée
        if (props.isLocked && props.keyId != null) {
            if (!playerKeys.contains(props.keyId)) {
                showMessageAbovePlayer("Need Key", Color.RED);
                System.out.println("🔒 Porte verrouillée - Clé requise: " + props.keyId);
                return;
            } else {
                // Déverrouiller la porte avec la clé
                props.isLocked = false;
                showMessageAbovePlayer("Unlocked!", Color.GREEN);
                System.out.println("🔓 Porte déverrouillée avec la clé: " + props.keyId);
            }
        }

        // Ouvrir/fermer la porte
        props.isOpen = !props.isOpen;
        String status = props.isOpen ? "ouvert" : "fermé";
        showMessageAbovePlayer(props.isOpen ? "Opened" : "Closed", Color.YELLOW);
        System.out.println("🚪 Porte " + status + " à la position (" + x + ", " + y + ")");
    }

    private Point2D screenToTile(double screenX, double screenY) {
        // Obtenir les coordonnées relatives au canvas (pas à l'écran)
        double canvasX = screenX;
        double canvasY = screenY;

        // Convertir en coordonnées monde (avec la caméra)
        double worldX = canvasX - CANVAS_WIDTH / 2 + cameraX;
        double worldY = canvasY - CANVAS_HEIGHT / 2 + cameraY;

        // Conversion isométrique vers coordonnées de grille
        double tileX = (worldX / (TILE_WIDTH / 2) + worldY / (TILE_HEIGHT / 2)) / 2;
        double tileY = (worldY / (TILE_HEIGHT / 2) - worldX / (TILE_WIDTH / 2)) / 2;

        return new Point2D(Math.floor(tileX), Math.floor(tileY));
    }

    private Point2D tileToScreen(double tileX, double tileY) {
        // Conversion grille vers coordonnées monde
        double worldX = (tileX - tileY) * (TILE_WIDTH / 2);
        double worldY = (tileX + tileY) * (TILE_HEIGHT / 2);

        // Conversion monde vers coordonnées écran (avec la caméra)
        double screenX = worldX - cameraX + CANVAS_WIDTH / 2;
        double screenY = worldY - cameraY + CANVAS_HEIGHT / 2;

        return new Point2D(screenX, screenY);
    }

    private boolean isValidTile(int x, int y) {
        return x >= 0 && x < MAP_SIZE && y >= 0 && y < MAP_SIZE;
    }

    private boolean canWalkThrough(int x, int y) {
        if (!isValidTile(x, y)) return false;

        WallType wall = wallTypes[x][y];

        // Cas spécial pour les portes
        if (wall == WallType.DOOR) {
            WallProperties props = wallProperties[x][y];
            if (props != null && props.isOpen) {
                return true; // Porte ouverte = traversable
            } else {
                return false; // Porte fermée = non traversable
            }
        }

        return wall == WallType.NONE || wall == WallType.TRAVERSABLE || wall == WallType.TRANSPARENT;
    }

    private void moveToPosition(Point2D target) {
        if (isMoving) return;

        targetPos = target;
        path = findPath(playerPos, targetPos);

        if (path.isEmpty()) {
            showExclamationMark();
            // Garder la position cliquée même si inaccessible
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

    private void showExclamationMark() {
        showExclamation = true;
        if (exclamationTimeline != null) {
            exclamationTimeline.stop();
        }
        exclamationTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> showExclamation = false));
        exclamationTimeline.play();
    }

    private void updateMovement() {
        if (!isMoving || path.isEmpty()) return;

        moveProgress += 0.05;

        if (moveProgress >= 1.0) {
            moveProgress = 0.0;
            currentPathIndex++;

            if (currentPathIndex >= path.size()) {
                isMoving = false;
                playerPos = targetPos;
                clickedPos = null; // Effacer l'indicateur bleu une fois arrivé
                moveTimeline.stop();
                return;
            }

            playerPos = path.get(currentPathIndex - 1);
        }

        // Mise à jour caméra pour suivre le mouvement - CORRECTION ICI
        // Calculer la position interpolée du personnage
        Point2D currentPos = playerPos;
        if (currentPathIndex < path.size()) {
            Point2D nextPos = path.get(currentPathIndex);
            // Interpolation entre la position actuelle et la suivante
            double interpX = currentPos.getX() + (nextPos.getX() - currentPos.getX()) * moveProgress;
            double interpY = currentPos.getY() + (nextPos.getY() - currentPos.getY()) * moveProgress;
            currentPos = new Point2D(interpX, interpY);
        }

        // Centrer la caméra sur la position interpolée du personnage
        Point2D playerScreenPos = tileToScreen(currentPos.getX(), currentPos.getY());
        double targetCameraX = playerScreenPos.getX() - CANVAS_WIDTH / 2;
        double targetCameraY = playerScreenPos.getY() - CANVAS_HEIGHT / 2;

        // Mouvement fluide de caméra (plus lent pour éviter les sauts)
        cameraX += (targetCameraX - cameraX) * 0.05; // Réduit de 0.1 à 0.05
        cameraY += (targetCameraY - cameraY) * 0.05;
    }

    private List<Point2D> findPath(Point2D start, Point2D end) {
        // A* simplifié
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<String> closedSet = new HashSet<>();
        Map<String, Node> allNodes = new HashMap<>();

        Node startNode = new Node((int)start.getX(), (int)start.getY(), null, 0, heuristic(start, end));
        openSet.add(startNode);
        allNodes.put(startNode.getKey(), startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            String currentKey = current.getKey();

            if (closedSet.contains(currentKey)) continue;
            closedSet.add(currentKey);

            if (current.x == (int)end.getX() && current.y == (int)end.getY()) {
                return reconstructPath(current);
            }

            for (int[] dir : new int[][]{{-1,0},{1,0},{0,-1},{0,1}}) {
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

        return new ArrayList<>();
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

    private void showMessageAbovePlayer(String message, Color color) {
        messageAbovePlayer = message;

        if (messageTimeline != null) {
            messageTimeline.stop();
        }

        messageTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> messageAbovePlayer = null));
        messageTimeline.play();
    }

    private void render() {
        // Fond noir pour contraster avec les PNG transparents
        gc.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        gc.setFill(Color.rgb(20, 20, 30)); // Fond sombre pour mieux voir les transparences
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Rendu en ordre isométrique
        for (int sum = 0; sum < MAP_SIZE * 2; sum++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                int y = sum - x;
                if (y < 0 || y >= MAP_SIZE) continue;

                renderTile(x, y);
            }
        }

        renderPlayer();
        renderMouseIndicators();
    }

    private void renderTile(int x, int y) {
        Point2D screenPos = tileToScreen(x, y);
        double screenX = screenPos.getX();
        double screenY = screenPos.getY();

        // Culling
        if (screenX < -TILE_WIDTH || screenX > CANVAS_WIDTH + TILE_WIDTH ||
                screenY < -WALL_HEIGHT || screenY > CANVAS_HEIGHT + TILE_HEIGHT) {
            return;
        }

        // Sol
        if (floorMap[x][y] >= 0) {
            Image floorImg = floorImages.get("floor_" + floorMap[x][y]);
            if (floorImg != null) {
                gc.drawImage(floorImg, screenX - TILE_WIDTH/2, screenY - TILE_HEIGHT/2);
            }
        }

        // Items
        if (!itemMap[x][y].isEmpty()) {
            gc.setFill(Color.YELLOW.deriveColor(0, 1, 1, 0.8));
            gc.fillOval(screenX - 6, screenY - 6, 12, 12);

            gc.setStroke(Color.DARKRED);
            gc.setLineWidth(2);
            gc.strokeLine(screenX - 5, screenY, screenX + 5, screenY);
            gc.strokeLine(screenX, screenY - 5, screenX, screenY + 5);

            int totalItems = itemMap[x][y].stream().mapToInt(item -> item.count).sum();
            if (totalItems > 1) {
                gc.setFill(Color.WHITE);
                gc.fillText(String.valueOf(totalItems), screenX + 6, screenY - 6);
            }
        }

        // Transparence selon position
        double alpha = 1.0;
        double distanceToPlayer = Math.abs(x - playerPos.getX()) + Math.abs(y - playerPos.getY());

        if (isInFrontOfPlayer(x, y)) {
            if (distanceToPlayer <= 1) {
                alpha = 0.2; // 80% transparent
            } else if (distanceToPlayer <= 2) {
                alpha = 0.5; // 50% transparent
            }
        }

        // Murs
        if (wallMap[x][y] >= 0) {
            // Déterminer l'image à utiliser (normale ou porte ouverte)
            String wallImageKey = "wall_" + wallMap[x][y];

            // Cas spécial pour les portes ouvertes
            if (wallTypes[x][y] == WallType.DOOR) {
                WallProperties props = wallProperties[x][y];
                if (props != null && props.isOpen) {
                    wallImageKey = "wall_" + wallMap[x][y] + "_o"; // Image porte ouverte
                }
            }

            Image wallImg = wallImages.get(wallImageKey);
            if (wallImg == null) {
                // Fallback vers l'image normale si l'image ouverte n'existe pas
                wallImg = wallImages.get("wall_" + wallMap[x][y]);
            }

            if (wallImg != null) {
                gc.setGlobalAlpha(alpha);
                gc.drawImage(wallImg, screenX - TILE_WIDTH/2, screenY - WALL_HEIGHT + TILE_HEIGHT/2);
                gc.setGlobalAlpha(1.0);

                // Afficher la santé des murs destructibles
                if (wallTypes[x][y] == WallType.DESTRUCTIBLE) {
                    WallProperties props = wallProperties[x][y];
                    if (props != null && props.health < 255) {
                        renderHealthBar(screenX, screenY - WALL_HEIGHT + TILE_HEIGHT/2, props.health);
                    }
                }
            }
        }

        // Plafonds
        if (ceilingMap[x][y] >= 0) {
            Image ceilingImg = ceilingImages.get("ceiling_" + ceilingMap[x][y]);
            if (ceilingImg != null) {
                gc.setGlobalAlpha(alpha);
                gc.drawImage(ceilingImg, screenX - TILE_WIDTH/2, screenY - TILE_HEIGHT/2 - WALL_HEIGHT);
                gc.setGlobalAlpha(1.0);
            }
        }
    }

    private boolean isInFrontOfPlayer(int x, int y) {
        double dx = x - playerPos.getX();
        double dy = y - playerPos.getY();
        double angle = Math.atan2(dy, dx);
        double angleDiff = Math.abs(angle - playerAngle);

        while (angleDiff > Math.PI) {
            angleDiff -= 2 * Math.PI;
        }
        while (angleDiff < -Math.PI) {
            angleDiff += 2 * Math.PI;
        }

        return Math.abs(angleDiff) < Math.PI / 2;
    }

    private void renderPlayer() {
        Point2D screenPos = tileToScreen(playerPos.getX(), playerPos.getY());
        double screenX = screenPos.getX();
        double screenY = screenPos.getY();

        // Interpolation si en mouvement
        if (isMoving && currentPathIndex < path.size()) {
            Point2D nextPos = path.get(currentPathIndex);
            Point2D nextScreenPos = tileToScreen(nextPos.getX(), nextPos.getY());

            screenX += (nextScreenPos.getX() - screenX) * moveProgress;
            screenY += (nextScreenPos.getY() - screenY) * moveProgress;
        }

        // Couleur selon orientation
        double hue = (playerAngle + Math.PI) / (2 * Math.PI) * 360;
        gc.setFill(Color.hsb(hue, 1.0, 1.0));
        gc.fillOval(screenX - 8, screenY - 8, 16, 16);

        // Direction
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        double dirX = screenX + Math.cos(playerAngle) * 12;
        double dirY = screenY + Math.sin(playerAngle) * 12;
        gc.strokeLine(screenX, screenY, dirX, dirY);

        // Point d'exclamation
        if (showExclamation) {
            gc.setFill(Color.YELLOW);
            gc.fillOval(screenX - 4, screenY - 24, 8, 8);
            gc.setFill(Color.RED);
            gc.fillText("!", screenX - 3, screenY - 18);
        }

        // Message au-dessus du personnage
        if (messageAbovePlayer != null) {
            gc.setFill(Color.WHITE);
            gc.fillText(messageAbovePlayer, screenX - messageAbovePlayer.length() * 3, screenY - 30);
        }
    }

    private void renderHealthBar(double x, double y, int health) {
        // Barre de santé pour les murs destructibles
        double barWidth = 30;
        double barHeight = 4;
        double healthPercent = health / 255.0;

        // Fond de la barre
        gc.setFill(Color.BLACK.deriveColor(0, 1, 1, 0.7));
        gc.fillRect(x - barWidth/2, y - 10, barWidth, barHeight);

        // Barre de santé colorée
        Color healthColor;
        if (healthPercent > 0.6) {
            healthColor = Color.GREEN;
        } else if (healthPercent > 0.3) {
            healthColor = Color.ORANGE;
        } else {
            healthColor = Color.RED;
        }

        gc.setFill(healthColor);
        gc.fillRect(x - barWidth/2, y - 10, barWidth * healthPercent, barHeight);

        // Contour
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokeRect(x - barWidth/2, y - 10, barWidth, barHeight);
    }

    private void renderMouseIndicators() {
        // Losange bleu sur la position cliquée
        if (clickedPos != null && isValidTile((int)clickedPos.getX(), (int)clickedPos.getY())) {
            renderTileIndicator(clickedPos, Color.BLUE, 0.6);
        }

        // Losange vert/rouge sur la position du curseur
        if (mouseHoverPos != null && isValidTile((int)mouseHoverPos.getX(), (int)mouseHoverPos.getY())) {
            boolean canWalk = canWalkThrough((int)mouseHoverPos.getX(), (int)mouseHoverPos.getY());
            Color indicatorColor = canWalk ? Color.LIME : Color.RED;
            renderTileIndicator(mouseHoverPos, indicatorColor, 0.4);
        }
    }

    private void renderTileIndicator(Point2D tilePos, Color color, double opacity) {
        Point2D screenPos = tileToScreen(tilePos.getX(), tilePos.getY());
        double screenX = screenPos.getX();
        double screenY = screenPos.getY();

        // Forme losange isométrique
        double[] xPoints = {
                screenX, // Haut
                screenX + TILE_WIDTH/2, // Droite
                screenX, // Bas
                screenX - TILE_WIDTH/2 // Gauche
        };
        double[] yPoints = {
                screenY - TILE_HEIGHT/2, // Haut
                screenY, // Droite
                screenY + TILE_HEIGHT/2, // Bas
                screenY // Gauche
        };

        // Dessiner le losange avec transparence
        gc.setFill(color.deriveColor(0, 1, 1, opacity));
        gc.fillPolygon(xPoints, yPoints, 4);

        // Contour plus visible
        gc.setStroke(color.deriveColor(0, 1, 0.7, 0.8));
        gc.setLineWidth(2);
        gc.strokePolygon(xPoints, yPoints, 4);
    }

    private void centerCameraOnPlayer() {
        // Calculer la position écran du personnage
        Point2D playerScreenPos = tileToScreen(playerPos.getX(), playerPos.getY());

        // Ajuster la caméra pour centrer le personnage à l'écran
        cameraX = playerScreenPos.getX() - CANVAS_WIDTH / 2;
        cameraY = playerScreenPos.getY() - CANVAS_HEIGHT / 2;

        System.out.println("📷 Caméra centrée sur le personnage à la position (" +
                (int)playerPos.getX() + ", " + (int)playerPos.getY() + ")");
        System.out.println("   Position caméra: (" + String.format("%.1f", cameraX) + ", " + String.format("%.1f", cameraY) + ")");
    }

    private void smoothCenterCameraOnPlayer() {
        // Version douce pour recentrer pendant le jeu
        Point2D playerScreenPos = tileToScreen(playerPos.getX(), playerPos.getY());
        double targetCameraX = playerScreenPos.getX() - CANVAS_WIDTH / 2;
        double targetCameraY = playerScreenPos.getY() - CANVAS_HEIGHT / 2;

        // Animation douce vers la position centrée
        Timeline cameraTimeline = new Timeline();
        final double startCameraX = cameraX;
        final double startCameraY = cameraY;

        KeyFrame keyFrame = new KeyFrame(Duration.millis(16), e -> {
            double progress = cameraTimeline.getCurrentTime().toMillis() / 500.0; // 500ms pour recentrer
            progress = Math.min(1.0, progress);

            // Interpolation douce (easing)
            double eased = 1 - Math.pow(1 - progress, 3); // Cubic ease-out

            cameraX = startCameraX + (targetCameraX - startCameraX) * eased;
            cameraY = startCameraY + (targetCameraY - startCameraY) * eased;
        });

        cameraTimeline.getKeyFrames().add(keyFrame);
        cameraTimeline.setCycleCount((int)(500 / 16)); // 500ms à 60fps
        cameraTimeline.play();
    }

    // Classe Node pour A*
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

        double getF() { return g + h; }
        String getKey() { return x + "," + y; }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.getF(), other.getF());
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Jeu Isométrique ===");
        System.out.println("Chargement des ressources...");
        launch(args);
    }
}