package com.example.catmeme;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class ImageGenerator {

    private static final int IMAGE_WIDTH = 200;
    private static final int IMAGE_HEIGHT = 200;
    private static final int NUM_IMAGES = 1000; // Nombre d'images à générer
    private static final String OUTPUT_DIR = "src/main/resources/images";

    private static Random random = new Random();

    public static void main(String[] args) {
        System.out.println("Génération de " + NUM_IMAGES + " images JPG colorées...");

        // Création du dossier de sortie
        File outputDir = new File(OUTPUT_DIR);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
            System.out.println("Dossier créé: " + OUTPUT_DIR);
        }

        // Génération des images
        for (int i = 1; i <= NUM_IMAGES; i++) {
            try {
                BufferedImage image = generateColoredImage(i);
                String filename = OUTPUT_DIR + "/" + i + ".jpg";

                // Sauvegarde en JPG avec qualité élevée
                ImageIO.write(image, "jpg", new File(filename));
                System.out.println("Image générée: " + filename);

            } catch (IOException e) {
                System.err.println("Erreur lors de la génération de l'image " + i + ": " + e.getMessage());
            }
        }

        System.out.println("Génération terminée ! " + NUM_IMAGES + " images créées dans le dossier '" + OUTPUT_DIR + "'");
    }

    private static BufferedImage generateColoredImage(int imageNumber) {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Activation de l'anti-aliasing pour un rendu plus lisse
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

        // Choix du type d'image à générer aléatoirement
        int imageType = random.nextInt(6);

        switch (imageType) {
            case 0:
                generateGradientImage(g2d);
                break;
            case 1:
                generateGeometricPattern(g2d);
                break;
            case 2:
                generateRandomShapes(g2d);
                break;
            case 3:
                generateConcentricCircles(g2d);
                break;
            case 4:
                generateStripedPattern(g2d);
                break;
            case 5:
                generateNoisePattern(g2d);
                break;
        }

        // Ajout d'un numéro dans le coin pour identifier l'image
        addImageNumber(g2d, imageNumber);

        g2d.dispose();
        return image;
    }

    private static void generateGradientImage(Graphics2D g2d) {
        // Dégradé avec couleurs aléatoires
        Color color1 = getRandomColor();
        Color color2 = getRandomColor();

        GradientPaint gradient = new GradientPaint(
                0, 0, color1,
                IMAGE_WIDTH, IMAGE_HEIGHT, color2
        );

        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    private static void generateGeometricPattern(Graphics2D g2d) {
        // Fond coloré
        g2d.setColor(getRandomColor());
        g2d.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        // Formes géométriques
        int numShapes = 5 + random.nextInt(10);
        for (int i = 0; i < numShapes; i++) {
            g2d.setColor(getRandomColor());

            int shapeType = random.nextInt(3);
            int x = random.nextInt(IMAGE_WIDTH);
            int y = random.nextInt(IMAGE_HEIGHT);
            int size = 20 + random.nextInt(80);

            switch (shapeType) {
                case 0: // Rectangle
                    g2d.fillRect(x, y, size, size);
                    break;
                case 1: // Cercle
                    g2d.fillOval(x, y, size, size);
                    break;
                case 2: // Triangle
                    int[] xPoints = {x, x + size/2, x + size};
                    int[] yPoints = {y + size, y, y + size};
                    g2d.fillPolygon(xPoints, yPoints, 3);
                    break;
            }
        }
    }

    private static void generateRandomShapes(Graphics2D g2d) {
        // Fond aléatoire
        g2d.setColor(getRandomColor());
        g2d.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        // Formes organiques aléatoires
        int numShapes = 3 + random.nextInt(7);
        for (int i = 0; i < numShapes; i++) {
            g2d.setColor(getRandomColorWithAlpha());

            // Création d'une forme libre
            Polygon polygon = new Polygon();
            int centerX = random.nextInt(IMAGE_WIDTH);
            int centerY = random.nextInt(IMAGE_HEIGHT);
            int numPoints = 6 + random.nextInt(8);

            for (int j = 0; j < numPoints; j++) {
                double angle = (2 * Math.PI * j) / numPoints;
                int radius = 30 + random.nextInt(50);
                int x = centerX + (int)(radius * Math.cos(angle));
                int y = centerY + (int)(radius * Math.sin(angle));
                polygon.addPoint(x, y);
            }

            g2d.fillPolygon(polygon);
        }
    }

    private static void generateConcentricCircles(Graphics2D g2d) {
        // Fond
        g2d.setColor(getRandomColor());
        g2d.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        // Cercles concentriques
        int centerX = IMAGE_WIDTH / 2;
        int centerY = IMAGE_HEIGHT / 2;
        int maxRadius = Math.min(IMAGE_WIDTH, IMAGE_HEIGHT) / 2;

        int numCircles = 5 + random.nextInt(8);
        for (int i = 0; i < numCircles; i++) {
            g2d.setColor(getRandomColorWithAlpha());
            int radius = maxRadius * (numCircles - i) / numCircles;
            g2d.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        }
    }

    private static void generateStripedPattern(Graphics2D g2d) {
        // Rayures colorées
        boolean horizontal = random.nextBoolean();
        int stripeWidth = 10 + random.nextInt(20);

        if (horizontal) {
            for (int y = 0; y < IMAGE_HEIGHT; y += stripeWidth) {
                g2d.setColor(getRandomColor());
                g2d.fillRect(0, y, IMAGE_WIDTH, stripeWidth);
            }
        } else {
            for (int x = 0; x < IMAGE_WIDTH; x += stripeWidth) {
                g2d.setColor(getRandomColor());
                g2d.fillRect(x, 0, stripeWidth, IMAGE_HEIGHT);
            }
        }
    }

    private static void generateNoisePattern(Graphics2D g2d) {
        // Pattern de bruit coloré
        Color baseColor = getRandomColor();

        for (int x = 0; x < IMAGE_WIDTH; x += 4) {
            for (int y = 0; y < IMAGE_HEIGHT; y += 4) {
                // Variation de couleur basée sur la couleur de base
                int r = Math.max(0, Math.min(255, baseColor.getRed() + random.nextInt(100) - 50));
                int g = Math.max(0, Math.min(255, baseColor.getGreen() + random.nextInt(100) - 50));
                int b = Math.max(0, Math.min(255, baseColor.getBlue() + random.nextInt(100) - 50));

                g2d.setColor(new Color(r, g, b));
                g2d.fillRect(x, y, 4, 4);
            }
        }
    }

    private static void addImageNumber(Graphics2D g2d, int number) {
        // Ajout du numéro de l'image
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));

        // Ombre pour la lisibilité
        g2d.setColor(Color.BLACK);
        g2d.drawString(String.valueOf(number), 11, 21);

        // Texte principal
        g2d.setColor(Color.WHITE);
        g2d.drawString(String.valueOf(number), 10, 20);
    }

    private static Color getRandomColor() {
        return new Color(
                random.nextInt(256),
                random.nextInt(256),
                random.nextInt(256)
        );
    }

    private static Color getRandomColorWithAlpha() {
        return new Color(
                random.nextInt(256),
                random.nextInt(256),
                random.nextInt(256),
                100 + random.nextInt(156) // Alpha entre 100 et 255
        );
    }
}