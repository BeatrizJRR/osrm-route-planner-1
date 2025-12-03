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
import java.time.Duration;
import java.util.List;
import java.util.Locale;


/**
 * Cliente para a API OSRM (Open Source Routing Machine).
 *
 * Papel na arquitetura MVC:
 * - Camada API (infra): executa chamadas HTTP à OSRM.
 * - A camada Service (Controller) decide perfis/modos e pós-processa resultados.
 * - Model representa entidades como `Route` e `Point`; UI consome via Service.
 *
 * Endpoints utilizados:
 * - GET {@code /route/v1/{profile}/{coordinates}} com parâmetros {@code overview=full} e {@code geometries=geojson}.
 *   Referência: https://project-osrm.org/docs/v5.27.0/api/#route-service
 */
public class OSRMClient {
    private static final String BASE_URL = "https://router.project-osrm.org/route/v1/";

    private static final String PROFILE_DRIVING = "driving";
    private static final String PROFILE_CYCLING = "cycling";
    private static final String PROFILE_WALKING = "walking";

    private static final String QUERY_PARAMS = "?overview=full&geometries=geojson";
    private static final String HEADER_USER_AGENT = "User-Agent";
    private static final String USER_AGENT_VALUE = "ProjetoADS/1.0";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(DEFAULT_TIMEOUT)
            .build();

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
            case CAR -> PROFILE_DRIVING;
            case BIKE -> PROFILE_CYCLING;
            case FOOT -> PROFILE_WALKING;
            default -> PROFILE_DRIVING;
        };

        String coords = String.format(Locale.US, "%f,%f;%f,%f",
                origin.getLongitude(), origin.getLatitude(),
                destination.getLongitude(), destination.getLatitude());

        String url = BASE_URL + profile + "/" + URLEncoder.encode(coords, StandardCharsets.UTF_8)
            + QUERY_PARAMS;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
            .timeout(DEFAULT_TIMEOUT)
            .header(HEADER_USER_AGENT, USER_AGENT_VALUE)
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
            case CAR -> PROFILE_DRIVING;
            case BIKE -> PROFILE_CYCLING;
            case FOOT -> PROFILE_WALKING;
            default -> PROFILE_DRIVING;
        };

        StringBuilder url = new StringBuilder(BASE_URL);
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
        url.append(QUERY_PARAMS);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url.toString()))
            .timeout(DEFAULT_TIMEOUT)
            .header(HEADER_USER_AGENT, USER_AGENT_VALUE)
            .GET()
            .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }   
}