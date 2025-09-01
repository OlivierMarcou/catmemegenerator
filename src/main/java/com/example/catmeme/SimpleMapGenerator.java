package com.example.catmeme;


import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Générateur de carte simple avec uniquement des sols et quelques murs
 */
public class SimpleMapGenerator {

    private static final int MAP_SIZE = 50;
    private static final Random random = new Random();

    public static void main(String[] args) {
        System.out.println("=== Générateur de Carte Simple ===");
        System.out.println("Génération d'une carte avec sols et quelques murs...");

        generateSimpleMap("simple_map.json");

        System.out.println("Carte simple générée avec succès !");
    }

    public static StringBuilder generateSimpleMap(String outputPath) {
        StringBuilder json = new StringBuilder();
        try {
            // Créer la structure JSON
            json.append("{\n");
            json.append("  \"mapSize\": ").append(MAP_SIZE).append(",\n");
            json.append("  \"generatedAt\": \"").append(new Date()).append("\",\n");

            // Générer et sauvegarder la carte des sols
            int[][] floorMap = generateFloorMap();
            json.append("  \"floorMap\": [\n");
            appendIntArray(json, floorMap);
            json.append("  ],\n");

            // Générer et sauvegarder la carte des murs
            int[][] wallMap = generateWallMap();
            json.append("  \"wallMap\": [\n");
            appendIntArray(json, wallMap);
            json.append("  ],\n");

            // Carte des plafonds (tous vides)
            json.append("  \"ceilingMap\": [\n");
            appendEmptyArray(json);
            json.append("  ],\n");

            // Types de murs
            String[][] wallTypes = generateWallTypes(wallMap);
            json.append("  \"wallTypes\": [\n");
            appendStringArray(json, wallTypes);
            json.append("  ],\n");

            // Items (tous vides pour simplicité)
            json.append("  \"itemMap\": [\n");
            appendEmptyItemArray(json);
            json.append("  ]\n");

            json.append("}\n");

            // Écriture du fichier
            try (FileWriter writer = new FileWriter(outputPath)) {
                writer.write(json.toString());
            }

            System.out.println("Carte sauvegardée : " + outputPath);
            printMapStats(floorMap, wallMap);
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde : " + e.getMessage());
        }
        return json;
    }

    private static int[][] generateFloorMap() {
        int[][] floorMap = new int[MAP_SIZE][MAP_SIZE];

        System.out.println("Génération des sols...");

        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                // Types de sol variés mais simples
                // 0-14 : herbe (75% de la carte)
                // 15-19 : chemins de terre (15% de la carte)
                // 20-24 : pierre (10% de la carte)

                double rand = random.nextDouble();
                if (rand < 0.75) {
                    // Herbe (indices 0-14)
                    floorMap[x][y] = random.nextInt(15);
                } else if (rand < 0.90) {
                    // Chemins (indices 15-19)
                    floorMap[x][y] = 15 + random.nextInt(5);
                } else {
                    // Pierre (indices 20-24)
                    floorMap[x][y] = 20 + random.nextInt(5);
                }
            }
        }

        // Ajouter quelques chemins connectés
        addConnectedPaths(floorMap);

        return floorMap;
    }

    private static void addConnectedPaths(int[][] floorMap) {
        // Chemin horizontal au milieu
        int centerY = MAP_SIZE / 2;
        for (int x = 5; x < MAP_SIZE - 5; x++) {
            floorMap[x][centerY] = 15 + random.nextInt(5); // Chemin
            if (random.nextDouble() < 0.3) {
                // Élargir le chemin parfois
                if (centerY > 0) floorMap[x][centerY - 1] = 15 + random.nextInt(5);
                if (centerY < MAP_SIZE - 1) floorMap[x][centerY + 1] = 15 + random.nextInt(5);
            }
        }

        // Chemin vertical au milieu
        int centerX = MAP_SIZE / 2;
        for (int y = 5; y < MAP_SIZE - 5; y++) {
            floorMap[centerX][y] = 15 + random.nextInt(5); // Chemin
            if (random.nextDouble() < 0.3) {
                // Élargir le chemin parfois
                if (centerX > 0) floorMap[centerX - 1][y] = 15 + random.nextInt(5);
                if (centerX < MAP_SIZE - 1) floorMap[centerX + 1][y] = 15 + random.nextInt(5);
            }
        }
    }

    private static int[][] generateWallMap() {
        int[][] wallMap = new int[MAP_SIZE][MAP_SIZE];

        System.out.println("Génération des murs...");

        // Initialiser tous à -1 (pas de mur)
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                wallMap[x][y] = -1;
            }
        }

        // Ajouter quelques murs dispersés (5% de la carte)
        int wallCount = 0;
        int maxWalls = (MAP_SIZE * MAP_SIZE) / 20; // 5% de la carte

        while (wallCount < maxWalls) {
            int x = random.nextInt(MAP_SIZE);
            int y = random.nextInt(MAP_SIZE);

            // Ne pas placer de murs sur les chemins principaux
            if (isOnMainPath(x, y)) {
                continue;
            }

            // Types de murs simples
            // 0-9 : murs en bois (60%)
            // 10-14 : murs en pierre (25%)
            // 30-34 : buissons traversables (15%)

            double rand = random.nextDouble();
            if (rand < 0.60) {
                wallMap[x][y] = random.nextInt(10); // Bois (0-9)
            } else if (rand < 0.85) {
                wallMap[x][y] = 10 + random.nextInt(5); // Pierre (10-14)
            } else {
                wallMap[x][y] = 30 + random.nextInt(5); // Buissons (30-34)
            }

            wallCount++;
        }

        // Ajouter quelques petites structures
        addSmallStructures(wallMap);

        return wallMap;
    }

    private static boolean isOnMainPath(int x, int y) {
        int centerX = MAP_SIZE / 2;
        int centerY = MAP_SIZE / 2;

        // Vérifier si sur le chemin horizontal (avec marge)
        if (Math.abs(y - centerY) <= 2 && x >= 5 && x < MAP_SIZE - 5) {
            return true;
        }

        // Vérifier si sur le chemin vertical (avec marge)
        if (Math.abs(x - centerX) <= 2 && y >= 5 && y < MAP_SIZE - 5) {
            return true;
        }

        return false;
    }

    private static void addSmallStructures(int[][] wallMap) {
        // Ajouter 3-5 petites structures (maisonnettes, enclos, etc.)
        int numStructures = 3 + random.nextInt(3);

        for (int i = 0; i < numStructures; i++) {
            // Position aléatoire
            int startX = 5 + random.nextInt(MAP_SIZE - 15);
            int startY = 5 + random.nextInt(MAP_SIZE - 15);

            // Taille de la structure (3x3 à 5x5)
            int size = 3 + random.nextInt(3);

            // Éviter les chemins principaux
            if (isOnMainPath(startX + size/2, startY + size/2)) {
                continue;
            }

            // Créer un petit enclos ou bâtiment
            for (int x = startX; x < startX + size; x++) {
                for (int y = startY; y < startY + size; y++) {
                    if (x >= 0 && x < MAP_SIZE && y >= 0 && y < MAP_SIZE) {
                        // Murs sur les bords seulement
                        if (x == startX || x == startX + size - 1 ||
                                y == startY || y == startY + size - 1) {

                            // Laisser parfois une ouverture (porte)
                            if (random.nextDouble() > 0.2) {
                                wallMap[x][y] = random.nextInt(15); // Mur en bois ou pierre
                            }
                        }
                    }
                }
            }
        }
    }

    private static String[][] generateWallTypes(int[][] wallMap) {
        String[][] wallTypes = new String[MAP_SIZE][MAP_SIZE];

        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                if (wallMap[x][y] == -1) {
                    wallTypes[x][y] = "NONE";
                } else if (wallMap[x][y] >= 30 && wallMap[x][y] <= 34) {
                    wallTypes[x][y] = "TRAVERSABLE"; // Buissons
                } else {
                    wallTypes[x][y] = "INDESTRUCTIBLE"; // Murs normaux
                }
            }
        }

        return wallTypes;
    }

    private static void appendIntArray(StringBuilder json, int[][] array) {
        for (int x = 0; x < MAP_SIZE; x++) {
            json.append("    [");
            for (int y = 0; y < MAP_SIZE; y++) {
                json.append(array[x][y]);
                if (y < MAP_SIZE - 1) json.append(", ");
            }
            json.append("]");
            if (x < MAP_SIZE - 1) json.append(",");
            json.append("\n");
        }
    }

    private static void appendEmptyArray(StringBuilder json) {
        for (int x = 0; x < MAP_SIZE; x++) {
            json.append("    [");
            for (int y = 0; y < MAP_SIZE; y++) {
                json.append("-1");
                if (y < MAP_SIZE - 1) json.append(", ");
            }
            json.append("]");
            if (x < MAP_SIZE - 1) json.append(",");
            json.append("\n");
        }
    }

    private static void appendStringArray(StringBuilder json, String[][] array) {
        for (int x = 0; x < MAP_SIZE; x++) {
            json.append("    [");
            for (int y = 0; y < MAP_SIZE; y++) {
                json.append("\"").append(array[x][y]).append("\"");
                if (y < MAP_SIZE - 1) json.append(", ");
            }
            json.append("]");
            if (x < MAP_SIZE - 1) json.append(",");
            json.append("\n");
        }
    }

    private static void appendEmptyItemArray(StringBuilder json) {
        for (int x = 0; x < MAP_SIZE; x++) {
            json.append("    [\n");
            for (int y = 0; y < MAP_SIZE; y++) {
                json.append("      []");
                if (y < MAP_SIZE - 1) json.append(",");
                json.append("\n");
            }
            json.append("    ]");
            if (x < MAP_SIZE - 1) json.append(",");
            json.append("\n");
        }
    }

    private static void printMapStats(int[][] floorMap, int[][] wallMap) {
        int wallCount = 0;
        int grassCount = 0, pathCount = 0, stoneCount = 0;

        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                // Compter les murs
                if (wallMap[x][y] != -1) {
                    wallCount++;
                }

                // Compter les types de sol
                if (floorMap[x][y] < 15) {
                    grassCount++;
                } else if (floorMap[x][y] < 20) {
                    pathCount++;
                } else {
                    stoneCount++;
                }
            }
        }

        System.out.println("\n=== Statistiques de la carte ===");
        System.out.println("Taille : " + MAP_SIZE + "x" + MAP_SIZE);
        System.out.println("Nombre de murs : " + wallCount + " (" + String.format("%.1f", wallCount * 100.0 / (MAP_SIZE * MAP_SIZE)) + "%)");
        System.out.println("Sol - Herbe : " + grassCount + " (" + String.format("%.1f", grassCount * 100.0 / (MAP_SIZE * MAP_SIZE)) + "%)");
        System.out.println("Sol - Chemins : " + pathCount + " (" + String.format("%.1f", pathCount * 100.0 / (MAP_SIZE * MAP_SIZE)) + "%)");
        System.out.println("Sol - Pierre : " + stoneCount + " (" + String.format("%.1f", stoneCount * 100.0 / (MAP_SIZE * MAP_SIZE)) + "%)");

        System.out.println("\n📁 Pour utiliser cette carte :");
        System.out.println("1. Copiez simple_map.json vers src/main/resources/village_map.json");
        System.out.println("2. Lancez IsometricGame");
    }
}