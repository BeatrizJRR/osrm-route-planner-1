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
import java.util.Locale;


public class OSRMClient {
    private static final String BASE_URL = "https://router.project-osrm.org/route/v1/";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public String getRouteJson(Point origin, Point destination, TransportMode mode) throws IOException, InterruptedException {
        String profile = switch (mode) {
            case BIKE -> "bike";
            case FOOT -> "walking";
            default -> "driving";
        };

        String coords = String.format(Locale.US, "%f,%f;%f,%f",
                origin.getLongitude(), origin.getLatitude(),
                destination.getLongitude(), destination.getLatitude());

        String url = BASE_URL + profile + "/" + URLEncoder.encode(coords, StandardCharsets.UTF_8)
                + "?overview=full&geometries=geojson";
        System.out.println(url);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "ProjetoADS/1.0")
                .GET()
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body(); 
    }

    
}
