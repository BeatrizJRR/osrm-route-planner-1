package com.myapp.api;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;


public class NominatimClient {
    private static final String BASE_SEARCH_URL = "https://nominatim.openstreetmap.org/search";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public String searchJson(String query) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = BASE_SEARCH_URL + "?q=" + encoded + "&format=json&limit=1";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "ProjetoADS/1.0")
                .header("Accept-Language", "pt-PT")
                .GET()
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
