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
 * Cliente para a API Nominatim (geocodificação OpenStreetMap).
 *
 * Papel na arquitetura MVC:
 * - Camada API (infra): resolve geocodificação, isolando dependência externa.
 * - Service (Controller) usa este cliente para obter coordenadas.
 * - Model armazena resultados estruturados; UI consome via Service.
 *
 * Endpoint utilizado:
 * - GET {@code /search?q={query}&format=json&limit=1}
 *   Referência: https://nominatim.org/release-docs/latest/api/Search/
 */
public class NominatimClient {
    private static final String BASE_SEARCH_URL = "https://nominatim.openstreetmap.org/search";

    private static final String HEADER_USER_AGENT = "User-Agent";
    private static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";
    private static final String USER_AGENT_VALUE = "ProjetoADS/1.0";
    private static final String ACCEPT_LANGUAGE_PT_PT = "pt-PT";
    private static final String QUERY_FORMAT_JSON_LIMIT_1 = "&format=json&limit=1";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(DEFAULT_TIMEOUT)
            .build();

    /**
     * Executa uma pesquisa de geocodificação e devolve o resultado em JSON.
     *
     * Mapeamento para Nominatim: GET {@code /search} com parâmetros {@code q}, {@code format=json} e {@code limit}.
     *
     * @param query termo a pesquisar (endereço, localidade, ponto de interesse)
     * @return resposta JSON devolvida pelo Nominatim
     * @throws IOException          erro de I/O ao comunicar
     * @throws InterruptedException se a thread for interrompida
     */
    public String searchJson(String query) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = BASE_SEARCH_URL + "?q=" + encoded + QUERY_FORMAT_JSON_LIMIT_1;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
            .timeout(DEFAULT_TIMEOUT)
            .header(HEADER_USER_AGENT, USER_AGENT_VALUE)
            .header(HEADER_ACCEPT_LANGUAGE, ACCEPT_LANGUAGE_PT_PT)
                .GET()
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
