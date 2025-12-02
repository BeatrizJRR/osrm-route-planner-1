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
import java.util.List;
import java.util.Locale;


/**
 * Cliente para a API OSRM (Open Source Routing Machine).
 *
 * Endpoints utilizados:
 * - GET {@code /route/v1/{profile}/{coordinates}} com parâmetros {@code overview=full} e {@code geometries=geojson}.
 *   Referência: https://project-osrm.org/docs/v5.27.0/api/#route-service
 */
public class OSRMClient {
    private static final String BASE_URL = "https://router.project-osrm.org/route/v1/";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    /**
     * Obtém uma rota simples entre origem e destino no formato JSON da OSRM.
     *
     * Mapeamento para OSRM: GET
     * {@code /route/v1/{profile}/{lon,lat;lon,lat}?overview=full&geometries=geojson}
     *
     * @param origin      ponto de origem
     * @param destination ponto de destino
     * @param mode        modo de transporte (perfil OSRM: driving/cycling/walking)
     * @return resposta JSON devolvida pela OSRM
     * @throws IOException            erro de I/O ao comunicar
     * @throws InterruptedException   se a thread for interrompida durante o pedido
     */
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

    /**
     * Obtém uma rota com pontos intermédios (waypoints) no formato JSON da OSRM.
     *
     * Mapeamento para OSRM: GET
     * {@code /route/v1/{profile}/{lon,lat;lon,lat;...}?overview=full&geometries=geojson}
     *
     * @param origin    ponto de origem
     * @param waypoints lista de pontos intermédios na ordem desejada
     * @param mode      modo de transporte (perfil OSRM)
     * @return resposta JSON devolvida pela OSRM
     * @throws IOException          erro de I/O ao comunicar
     * @throws InterruptedException se a thread for interrompida
     */
    public String getRouteJsonWithWaypoints(Point origin, List<Point> waypoints, TransportMode mode)
        throws IOException, InterruptedException {

        String profile = switch (mode) {
            case CAR -> "driving";
            case BIKE -> "cycling";
            case FOOT -> "walking";
            default -> "driving";
        };

        StringBuilder url = new StringBuilder("http://router.project-osrm.org/route/v1/");
        url.append(profile).append("/");

        // Origin
        url.append(origin.getLongitude()).append(",").append(origin.getLatitude());

        // Waypoints
        for (Point wp : waypoints) {
            url.append(";")
                    .append(wp.getLongitude())
                    .append(",")
                    .append(wp.getLatitude());
        }

        // Full geometry and overview
        url.append("?overview=full&geometries=geojson");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .GET()
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }   
}