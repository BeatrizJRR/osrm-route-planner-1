package com.myapp.api;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Cliente para a API Open-Elevation.
 *
 * Papel na arquitetura MVC:
 * - Camada API (infra): obtém dados de elevação.
 * - Service (Controller) agrega/transforma perfis; Model guarda resultados.
 * - UI consome perfis via Service; não chama diretamente o cliente.
 */
public class ElevationClient {
    private static final String BASE_URL = "https://api.open-elevation.com/api/v1/lookup";

    private static final String HEADER_USER_AGENT = "User-Agent";
    private static final String USER_AGENT_VALUE = "ProjetoADS/1.0";
    private static final String QUERY_PARAM_LOCATIONS_PREFIX = "?locations=";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(DEFAULT_TIMEOUT)
            .build();

    /**
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
        String url = BASE_URL + QUERY_PARAM_LOCATIONS_PREFIX + encodedLocations;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
            .timeout(DEFAULT_TIMEOUT)
            .header(HEADER_USER_AGENT, USER_AGENT_VALUE)
                .GET()
                .build();
        
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}