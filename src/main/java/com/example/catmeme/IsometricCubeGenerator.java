package com.example.catmeme;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;


public class IsometricCubeGenerator {

    private static final int IMAGE_SIZE = 256;
    private static final int NUM_GROUND_CUBES = 25; // Cubes de sol
    private static final int NUM_WALL_CUBES = 25;   // Cubes de mur
    private static final String OUTPUT_DIR = "resources";

    private static Random random = new Random();

    // Dimensions du cube isométrique
    private static final int CUBE_WIDTH = 120;
    private static final int CUBE_HEIGHT = 70;
    private static final int CUBE_DEPTH = 60;

    public static void main(String[] args) {
        System.out.println("Génération de cubes 3D isométriques...");

        // Création du dossier de sortie
        File outputDir = new File(OUTPUT_DIR);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
            System.out.println("Dossier créé: " + OUTPUT_DIR);
        }

        int imageCounter = 1;

        // Génération des cubes de sol
        System.out.println("Génération des cubes de sol...");
        for (int i = 0; i < NUM_GROUND_CUBES; i++) {
            try {
                BufferedImage image = generateGroundCube(i);
                String filename = OUTPUT_DIR + File.separator + imageCounter + ".jpg";

                ImageIO.write(image, "jpg", new File(filename));
                System.out.println("Cube de sol généré: " + filename + " (type: " + getGroundType(i % 6) + ")");
                imageCounter++;

            } catch (IOException e) {
                System.err.println("Erreur lors de la génération du cube de sol " + i + ": " + e.getMessage());
            }
        }

        // Génération des cubes de mur
        System.out.println("Génération des cubes de mur...");
        for (int i = 0; i < NUM_WALL_CUBES; i++) {
            try {
                BufferedImage image = generateWallCube(i);
                String filename = OUTPUT_DIR + File.separator + imageCounter + ".jpg";

                ImageIO.write(image, "jpg", new File(filename));
                System.out.println("Cube de mur généré: " + filename + " (type: " + getWallType(i % 6) + ")");
                imageCounter++;

            } catch (IOException e) {
                System.err.println("Erreur lors de la génération du cube de mur " + i + ": " + e.getMessage());
            }
        }

        System.out.println("Génération terminée ! " + (NUM_GROUND_CUBES + NUM_WALL_CUBES) +
                " cubes isométriques créés dans le dossier '" + OUTPUT_DIR + "'");
    }

    private static BufferedImage generateGroundCube(int index) {
        BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        setupGraphics(g2d);

        // Fond transparent/neutre
        g2d.setColor(new Color(240, 248, 255));
        g2d.fillRect(0, 0, IMAGE_SIZE, IMAGE_SIZE);

        // Position du cube au centre
        int centerX = IMAGE_SIZE / 2;
        int centerY = IMAGE_SIZE / 2;

        // Type de sol basé sur l'index
        int groundType = index % 6;
        drawIsometricGroundCube(g2d, centerX, centerY, groundType);

        g2d.dispose();
        return image;
    }

    private static BufferedImage generateWallCube(int index) {
        BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        setupGraphics(g2d);

        // Fond transparent/neutre
        g2d.setColor(new Color(240, 248, 255));
        g2d.fillRect(0, 0, IMAGE_SIZE, IMAGE_SIZE);

        // Position du cube au centre
        int centerX = IMAGE_SIZE / 2;
        int centerY = IMAGE_SIZE / 2;

        // Type de mur basé sur l'index
        int wallType = index % 6;
        drawIsometricWallCube(g2d, centerX, centerY, wallType);

        g2d.dispose();
        return image;
    }

    private static void setupGraphics(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private static void drawIsometricGroundCube(Graphics2D g2d, int centerX, int centerY, int groundType) {
        // Calcul des points du cube isométrique
        int[] topX = new int[4];
        int[] topY = new int[4];
        int[] leftX = new int[4];
        int[] leftY = new int[4];
        int[] rightX = new int[4];
        int[] rightY = new int[4];

        // Face du dessus (visible)
        topX[0] = centerX;                          // Haut
        topY[0] = centerY - CUBE_HEIGHT / 2;
        topX[1] = centerX + CUBE_WIDTH / 2;         // Droite
        topY[1] = centerY - CUBE_HEIGHT / 2 + CUBE_DEPTH / 2;
        topX[2] = centerX;                          // Bas
        topY[2] = centerY + CUBE_DEPTH / 2;
        topX[3] = centerX - CUBE_WIDTH / 2;         // Gauche
        topY[3] = centerY - CUBE_HEIGHT / 2 + CUBE_DEPTH / 2;

        // Face gauche
        leftX[0] = centerX - CUBE_WIDTH / 2;        // Haut gauche
        leftY[0] = centerY - CUBE_HEIGHT / 2 + CUBE_DEPTH / 2;
        leftX[1] = centerX;                         // Haut milieu
        leftY[1] = centerY + CUBE_DEPTH / 2;
        leftX[2] = centerX;                         // Bas milieu
        leftY[2] = centerY + CUBE_DEPTH / 2 + CUBE_HEIGHT;
        leftX[3] = centerX - CUBE_WIDTH / 2;        // Bas gauche
        leftY[3] = centerY - CUBE_HEIGHT / 2 + CUBE_DEPTH / 2 + CUBE_HEIGHT;

        // Face droite
        rightX[0] = centerX;                        // Haut milieu
        rightY[0] = centerY + CUBE_DEPTH / 2;
        rightX[1] = centerX + CUBE_WIDTH / 2;       // Haut droite
        rightY[1] = centerY - CUBE_HEIGHT / 2 + CUBE_DEPTH / 2;
        rightX[2] = centerX + CUBE_WIDTH / 2;       // Bas droite
        rightY[2] = centerY - CUBE_HEIGHT / 2 + CUBE_DEPTH / 2 + CUBE_HEIGHT;
        rightX[3] = centerX;                        // Bas milieu
        rightY[3] = centerY + CUBE_DEPTH / 2 + CUBE_HEIGHT;

        // Couleurs et textures selon le type de sol
        Color topColor, leftColor, rightColor;

        switch (groundType) {
            case 0: // Herbe
                topColor = new Color(76, 153, 61);
                leftColor = new Color(61, 122, 49);
                rightColor = new Color(54, 110, 44);
                break;
            case 1: // Terre
                topColor = new Color(139, 90, 43);
                leftColor = new Color(111, 72, 34);
                rightColor = new Color(100, 65, 31);
                break;
            case 2: // Sable
                topColor = new Color(238, 203, 173);
                leftColor = new Color(205, 175, 149);
                rightColor = new Color(188, 160, 136);
                break;
            case 3: // Pierre
                topColor = new Color(128, 128, 128);
                leftColor = new Color(105, 105, 105);
                rightColor = new Color(90, 90, 90);
                break;
            case 4: // Neige
                topColor = new Color(255, 250, 250);
                leftColor = new Color(230, 230, 250);
                rightColor = new Color(211, 211, 211);
                break;
            default: // Sol rocheux
                topColor = new Color(105, 77, 57);
                leftColor = new Color(85, 62, 46);
                rightColor = new Color(75, 55, 41);
                break;
        }

        // Dessin des faces avec ombrage

        // Face du dessus
        g2d.setColor(topColor);
        g2d.fillPolygon(topX, topY, 4);
        addGroundTexture(g2d, topX, topY, groundType, topColor);

        // Face gauche (plus sombre)
        g2d.setColor(leftColor);
        g2d.fillPolygon(leftX, leftY, 4);
        addSideTexture(g2d, leftX, leftY, groundType, leftColor, true);

        // Face droite (la plus sombre)
        g2d.setColor(rightColor);
        g2d.fillPolygon(rightX, rightY, 4);
        addSideTexture(g2d, rightX, rightY, groundType, rightColor, false);

        // Contours
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawPolygon(topX, topY, 4);
        g2d.drawPolygon(leftX, leftY, 4);
        g2d.drawPolygon(rightX, rightY, 4);
    }

    private static void drawIsometricWallCube(Graphics2D g2d, int centerX, int centerY, int wallType) {
        // Calcul identique aux cubes de sol
        int[] topX = new int[4];
        int[] topY = new int[4];
        int[] leftX = new int[4];
        int[] leftY = new int[4];
        int[] rightX = new int[4];
        int[] rightY = new int[4];

        // Points identiques au cube de sol
        topX[0] = centerX;
        topY[0] = centerY - CUBE_HEIGHT / 2;
        topX[1] = centerX + CUBE_WIDTH / 2;
        topY[1] = centerY - CUBE_HEIGHT / 2 + CUBE_DEPTH / 2;
        topX[2] = centerX;
        topY[2] = centerY + CUBE_DEPTH / 2;
        topX[3] = centerX - CUBE_WIDTH / 2;
        topY[3] = centerY - CUBE_HEIGHT / 2 + CUBE_DEPTH / 2;

        leftX[0] = centerX - CUBE_WIDTH / 2;
        leftY[0] = centerY - CUBE_HEIGHT / 2 + CUBE_DEPTH / 2;
        leftX[1] = centerX;
        leftY[1] = centerY + CUBE_DEPTH / 2;
        leftX[2] = centerX;
        leftY[2] = centerY + CUBE_DEPTH / 2 + CUBE_HEIGHT;
        leftX[3] = centerX - CUBE_WIDTH / 2;
        leftY[3] = centerY - CUBE_HEIGHT / 2 + CUBE_DEPTH / 2 + CUBE_HEIGHT;

        rightX[0] = centerX;
        rightY[0] = centerY + CUBE_DEPTH / 2;
        rightX[1] = centerX + CUBE_WIDTH / 2;
        rightY[1] = centerY - CUBE_HEIGHT / 2 + CUBE_DEPTH / 2;
        rightX[2] = centerX + CUBE_WIDTH / 2;
        rightY[2] = centerY - CUBE_HEIGHT / 2 + CUBE_DEPTH / 2 + CUBE_HEIGHT;
        rightX[3] = centerX;
        rightY[3] = centerY + CUBE_DEPTH / 2 + CUBE_HEIGHT;

        // Couleurs selon le type de mur
        Color topColor, leftColor, rightColor;

        switch (wallType) {
            case 0: // Pierre grise
                topColor = new Color(169, 169, 169);
                leftColor = new Color(128, 128, 128);
                rightColor = new Color(105, 105, 105);
                break;
            case 1: // Brique rouge
                topColor = new Color(205, 92, 92);
                leftColor = new Color(178, 34, 34);
                rightColor = new Color(139, 26, 26);
                break;
            case 2: // Bois
                topColor = new Color(160, 82, 45);
                leftColor = new Color(139, 69, 19);
                rightColor = new Color(115, 57, 16);
                break;
            case 3: // Métal
                topColor = new Color(192, 192, 192);
                leftColor = new Color(169, 169, 169);
                rightColor = new Color(128, 128, 128);
                break;
            case 4: // Pierre sombre
                topColor = new Color(85, 85, 85);
                leftColor = new Color(64, 64, 64);
                rightColor = new Color(47, 47, 47);
                break;
            default: // Béton
                topColor = new Color(211, 211, 211);
                leftColor = new Color(192, 192, 192);
                rightColor = new Color(169, 169, 169);
                break;
        }

        // Dessin des faces

        // Face du dessus
        g2d.setColor(topColor);
        g2d.fillPolygon(topX, topY, 4);
        addWallTexture(g2d, topX, topY, wallType, topColor, "top");

        // Face gauche
        g2d.setColor(leftColor);
        g2d.fillPolygon(leftX, leftY, 4);
        addWallTexture(g2d, leftX, leftY, wallType, leftColor, "left");

        // Face droite
        g2d.setColor(rightColor);
        g2d.fillPolygon(rightX, rightY, 4);
        addWallTexture(g2d, rightX, rightY, wallType, rightColor, "right");

        // Contours plus marqués pour les murs
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.drawPolygon(topX, topY, 4);
        g2d.drawPolygon(leftX, leftY, 4);
        g2d.drawPolygon(rightX, rightY, 4);
    }

    private static void addGroundTexture(Graphics2D g2d, int[] x, int[] y, int groundType, Color baseColor) {
        switch (groundType) {
            case 0: // Herbe - petits points verts
                g2d.setColor(new Color(34, 139, 34));
                for (int i = 0; i < 20; i++) {
                    int px = x[0] + random.nextInt(CUBE_WIDTH/2) - CUBE_WIDTH/4;
                    int py = y[0] + random.nextInt(CUBE_DEPTH/2) - CUBE_DEPTH/4;
                    if (isPointInPolygon(px, py, x, y)) {
                        g2d.fillOval(px, py, 2, 2);
                    }
                }
                break;
            case 1: // Terre - texture granuleuse
                for (int i = 0; i < 15; i++) {
                    Color dirtVariation = new Color(
                            Math.max(0, Math.min(255, baseColor.getRed() + random.nextInt(40) - 20)),
                            Math.max(0, Math.min(255, baseColor.getGreen() + random.nextInt(40) - 20)),
                            Math.max(0, Math.min(255, baseColor.getBlue() + random.nextInt(40) - 20))
                    );
                    g2d.setColor(dirtVariation);
                    int px = x[0] + random.nextInt(CUBE_WIDTH/2) - CUBE_WIDTH/4;
                    int py = y[0] + random.nextInt(CUBE_DEPTH/2) - CUBE_DEPTH/4;
                    if (isPointInPolygon(px, py, x, y)) {
                        g2d.fillOval(px, py, 3, 2);
                    }
                }
                break;
            case 2: // Sable - points fins
                g2d.setColor(new Color(210, 180, 140));
                for (int i = 0; i < 30; i++) {
                    int px = x[0] + random.nextInt(CUBE_WIDTH/2) - CUBE_WIDTH/4;
                    int py = y[0] + random.nextInt(CUBE_DEPTH/2) - CUBE_DEPTH/4;
                    if (isPointInPolygon(px, py, x, y)) {
                        g2d.fillOval(px, py, 1, 1);
                    }
                }
                break;
        }
    }

    private static void addSideTexture(Graphics2D g2d, int[] x, int[] y, int groundType, Color baseColor, boolean isLeft) {
        // Texture verticale simple pour les côtés
        Color darkerColor = new Color(
                Math.max(0, baseColor.getRed() - 30),
                Math.max(0, baseColor.getGreen() - 30),
                Math.max(0, baseColor.getBlue() - 30)
        );

        g2d.setColor(darkerColor);
        for (int i = 0; i < 10; i++) {
            int px = x[0] + random.nextInt(Math.abs(x[1] - x[0]));
            int py = y[0] + random.nextInt(Math.abs(y[2] - y[0]));
            g2d.fillOval(px, py, 1, 2);
        }
    }

    private static void addWallTexture(Graphics2D g2d, int[] x, int[] y, int wallType, Color baseColor, String face) {
        switch (wallType) {
            case 1: // Brique
                if (face.equals("left") || face.equals("right")) {
                    // Motif de briques
                    g2d.setColor(new Color(139, 26, 26));
                    g2d.setStroke(new BasicStroke(1.0f));
                    for (int i = 0; i < 4; i++) {
                        int lineY = y[0] + (y[2] - y[0]) * i / 4;
                        g2d.drawLine(x[0], lineY, x[1], lineY);
                    }
                }
                break;
            case 2: // Bois
                if (face.equals("left") || face.equals("right")) {
                    // Veines du bois
                    g2d.setColor(new Color(101, 67, 33));
                    g2d.setStroke(new BasicStroke(1.5f));
                    for (int i = 0; i < 3; i++) {
                        int lineY = y[0] + (y[2] - y[0]) * (i + 1) / 4;
                        g2d.drawLine(x[0] + 5, lineY, x[1] - 5, lineY);
                    }
                }
                break;
            case 3: // Métal
                // Reflets métalliques
                g2d.setColor(Color.WHITE);
                if (face.equals("top")) {
                    g2d.fillOval(x[0] - 10, y[0] + 5, 8, 4);
                }
                break;
        }
    }

    private static boolean isPointInPolygon(int testX, int testY, int[] polyX, int[] polyY) {
        boolean result = false;
        int j = polyX.length - 1;

        for (int i = 0; i < polyX.length; i++) {
            if ((polyY[i] > testY) != (polyY[j] > testY) &&
                    (testX < (polyX[j] - polyX[i]) * (testY - polyY[i]) / (polyY[j] - polyY[i]) + polyX[i])) {
                result = !result;
            }
            j = i;
        }

        return result;
    }

    private static String getGroundType(int type) {
        switch (type) {
            case 0: return "Herbe";
            case 1: return "Terre";
            case 2: return "Sable";
            case 3: return "Pierre";
            case 4: return "Neige";
            default: return "Sol rocheux";
        }
    }

    private static String getWallType(int type) {
        switch (type) {
            case 0: return "Pierre grise";
            case 1: return "Brique rouge";
            case 2: return "Bois";
            case 3: return "Métal";
            case 4: return "Pierre sombre";
            default: return "Béton";
        }
    }
}