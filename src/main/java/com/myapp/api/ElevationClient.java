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
     * Cliente para a API Open-Elevation.
     *
     * Endpoint utilizado:
     * - GET {@code /api/v1/lookup?locations=lat,lon|lat,lon|...}
     *   Referência: https://open-elevation.com/
     */
    /**
     * Obtém elevações para múltiplos pontos.
     *
     * @param locations string no formato {@code lat1,lon1|lat2,lon2|...}
     * @return resposta JSON da Open-Elevation
     * @throws IOException erro de I/O ao comunicar
     * @throws InterruptedException se a thread for interrompida
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