package com.myapp.api;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class ElevationClient {
    private static final String BASE_URL = "https://api.open-elevation.com/api/v1/lookup";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    /**
     * Obter elevações para múltiplos pontos
     * @param locations String no formato "lat1,lon1|lat2,lon2|..."
     */
    public String getElevations(String locations) throws IOException, InterruptedException {
        // URL encode do parâmetro locations
        String encodedLocations = URLEncoder.encode(locations, StandardCharsets.UTF_8);
        String url = BASE_URL + "?locations=" + encodedLocations;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "ProjetoADS/1.0")
                .GET()
                .build();
        
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
