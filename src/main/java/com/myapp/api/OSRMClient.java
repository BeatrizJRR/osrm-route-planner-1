package com.myapp.api;

import com.myapp.model.Point;
import com.myapp.model.TransportMode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;


public class OSRMClient {
    private static final String BASE_URL = "https://router.project-osrm.org/route/v1/";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public String getRouteJson(Point origin, Point destination, TransportMode mode) throws IOException, InterruptedException {
        String profile = switch (mode) {
            case BIKE -> "bike";
            case FOOT -> "walking";
            default -> "driving";
        };

        String coords = String.format("%f,%f;%f,%f",
                origin.getLongitude(), origin.getLatitude(),
                destination.getLongitude(), destination.getLatitude());

        String url = BASE_URL + profile + "/" + URLEncoder.encode(coords, StandardCharsets.UTF_8)
                + "?overview=full&geometries=geojson";
        System.out.println(url);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "ProjetoEscolarADS/1.0 (email@escola.pt)")
                .GET()
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body(); 
    }
}
