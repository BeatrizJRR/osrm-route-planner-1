package com.myapp.api;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;


/**
 * Cliente para a API Nominatim (geocodificação OpenStreetMap).
 *
 * Endpoint utilizado:
 * - GET {@code /search?q={query}&format=json&limit=1}
 *   Referência: https://nominatim.org/release-docs/latest/api/Search/
 */
public class NominatimClient {
    private static final String BASE_SEARCH_URL = "https://nominatim.openstreetmap.org/search";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

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
