package com.example.catmeme.service;

import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CatService {
    private static final String CAT_API_URL = "https://api.thecatapi.com/v1/images/search";
    private final HttpClient httpClient;
    private final Pattern urlPattern = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");

    public CatService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public CompletableFuture<Image> getRandomCatImage() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Récupérer l'URL de l'image
                String imageUrl = getCatImageUrl();

                // Télécharger l'image
                return downloadImage(imageUrl);
            } catch (Exception e) {
                e.printStackTrace();
                // Retourner une image par défaut en cas d'erreur
                return getDefaultCatImage();
            }
        });
    }

    private String getCatImageUrl() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CAT_API_URL))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        // Parse JSON simple avec regex
        String jsonResponse = response.body();
        Matcher matcher = urlPattern.matcher(jsonResponse);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new IOException("Impossible de trouver l'URL de l'image dans la réponse JSON");
        }
    }

    private Image downloadImage(String imageUrl) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofByteArray());

        return new Image(new ByteArrayInputStream(response.body()));
    }

    private Image getDefaultCatImage() {
        // URL d'une image de chat par défaut
        String defaultImageUrl = "https://placekitten.com/400/300";
        try {
            return downloadImage(defaultImageUrl);
        } catch (Exception e) {
            // Si même l'image par défaut échoue, créer une image vide
            return new Image("data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");
        }
    }

    public void close() {
        // Le HttpClient de Java 11+ se ferme automatiquement
    }
}