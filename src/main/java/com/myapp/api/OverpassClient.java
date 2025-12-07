package com.myapp.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cliente para a API Overpass (consulta de dados OSM via OverpassQL).
 *
 * Papel na arquitetura MVC:
 * - Camada API (infra): encapsula chamadas HTTP externas e fornece dados
 * brutos.
 * - Consumido pela camada Service (Controller) que orquestra lógica de negócio.
 * - Model recebe/representa os dados parseados; UI não deve chamar este cliente
 * diretamente.
 *
 * Endpoint utilizado:
 * - POST `/api/interpreter` com parâmetro `data` contendo OverpassQL.
 * Referência: https://overpass-api.de/
 */
public class OverpassClient {
    private static final String BASE_URL = "https://overpass-api.de/api/interpreter";

    // Constantes chamadas para evitar strings mágicas
    private static final String HEADER_USER_AGENT = "User-Agent";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String USER_AGENT_VALUE = "ProjetoADS/1.0";
    private static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    private static final String FORM_DATA_PREFIX = "data=";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(DEFAULT_TIMEOUT)
            .build();

    // Overpass não tem múltiplas APIs, tem uma genérica para diferentes queries
    /**
     * Executa uma consulta OverpassQL e devolve a resposta textual.
     *
     * Mapeamento para Overpass: POST `/api/interpreter` com `data=...`.
     *
     * @param overpassQL consulta OverpassQL (string)
     * @return resposta da API Overpass (normalmente JSON ou XML dependendo da
     *         consulta)
     * @throws IOException          erro de I/O ao comunicar
     * @throws InterruptedException se a thread for interrompida
     */
    public String postOverpass(String overpassQL) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .timeout(DEFAULT_TIMEOUT)
                .header(HEADER_USER_AGENT, USER_AGENT_VALUE)
                .header(HEADER_CONTENT_TYPE, CONTENT_TYPE_FORM)
                .POST(HttpRequest.BodyPublishers.ofString(FORM_DATA_PREFIX + overpassQL))
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}