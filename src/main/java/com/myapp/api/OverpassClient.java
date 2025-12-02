package com.myapp.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


/**
 * Cliente para a API Overpass (consulta de dados OSM via OverpassQL).
 *
 * Endpoint utilizado:
 * - POST `/api/interpreter` com parâmetro `data` contendo OverpassQL.
 *   Referência: https://overpass-api.de/
 */
public class OverpassClient {
    private static final String BASE_URL = "https://overpass-api.de/api/interpreter";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    //Overpass não tem múltiplas APIs, tem uma genérica para diferentes queries
    /**
     * Executa uma consulta OverpassQL e devolve a resposta textual.
     *
     * Mapeamento para Overpass: POST `/api/interpreter` com `data=...`.
     *
     * @param overpassQL consulta OverpassQL (string)
     * @return resposta da API Overpass (normalmente JSON ou XML dependendo da consulta)
     * @throws IOException          erro de I/O ao comunicar
     * @throws InterruptedException se a thread for interrompida
     */
    public String postOverpass(String overpassQL) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("User-Agent", "ProjetoADS/1.0")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("data=" + overpassQL))
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}