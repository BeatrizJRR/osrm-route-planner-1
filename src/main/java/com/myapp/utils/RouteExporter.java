package com.myapp.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.myapp.model.POI;
import com.myapp.model.Point;
import com.myapp.model.Route;

/**
 * Utilitário para exportação de rotas para formatos comuns.
 *
 * Papel na arquitetura MVC:
 * - Utils: fornece operações de I/O e serialização, independente de lógica de
 * negócio.
 * - Consumido pela UI (View) quando o utilizador exporta uma rota.
 * - Trabalha com modelos (Route, Point, POI) mas não os modifica.
 *
 * Atualmente suporta exportação para JSON e GPX.
 */
public class RouteExporter {

    /**
     * Exporta a rota para um ficheiro JSON.
     *
     * Estrutura gerada:
     * 
     * <pre>
     * {
     *   "distance_km": number,
     *   "duration_sec": number,
     *   "mode": string,
     *   "route_points": [ { "lat": number, "lon": number }, ... ],
     *   "pois": [ { "name": string, "category": string, "lat": number, "lon": number }, ... ]
     * }
     * </pre>
     *
     * @param route    a rota a exportar
     * @param filePath caminho do ficheiro de saída
     * @throws IOException quando ocorre erro de escrita
     */
    public static void exportToJson(Route route, String filePath) throws IOException {
        JsonObject root = new JsonObject();

        root.addProperty("distance_km", route.getDistanceKm());
        root.addProperty("duration_sec", route.getDurationSec());
        root.addProperty("mode", route.getMode().name());

        // pontos da rota
        JsonArray pts = new JsonArray();
        for (Point p : route.getRoutePoints()) {
            JsonObject o = new JsonObject();
            o.addProperty("lat", p.getLatitude());
            o.addProperty("lon", p.getLongitude());
            pts.add(o);
        }
        root.add("route_points", pts);

        // POIs
        JsonArray pois = new JsonArray();
        for (POI poi : route.getPois()) {
            JsonObject o = new JsonObject();
            o.addProperty("name", poi.getName());
            o.addProperty("category", poi.getCategory());
            o.addProperty("lat", poi.getCoordinate().getLatitude());
            o.addProperty("lon", poi.getCoordinate().getLongitude());
            pois.add(o);
        }
        root.add("pois", pois);

        try (FileWriter fw = new FileWriter(filePath)) {
            fw.write(root.toString());
        }
    }

    /**
     * Exporta a rota para um ficheiro GPX (versão 1.1).
     *
     * É gerado um segmento de trilho (trkseg) com um ponto (trkpt) por cada
     * coordenada da rota.
     *
     * @param route    a rota a exportar
     * @param filePath caminho do ficheiro de saída
     * @throws IOException quando ocorre erro de escrita
     */
    public static void exportToGPX(Route route, String filePath) throws IOException {
        StringBuilder gpx = new StringBuilder();
        gpx.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        gpx.append("<gpx version=\"1.1\" creator=\"MyApp\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n");
        gpx.append("  <trk>\n");
        gpx.append("    <name>Route Export</name>\n");
        gpx.append("    <trkseg>\n");

        for (Point p : route.getRoutePoints()) {
            gpx.append(String.format(Locale.US,
                "      <trkpt lat=\"%.6f\" lon=\"%.6f\">\n",
                p.getLatitude(),
                p.getLongitude()));
        }

        gpx.append("    </trkseg>\n");
        gpx.append("  </trk>\n");
        gpx.append("</gpx>\n");

        try (FileWriter fw = new FileWriter(filePath)) {
            fw.write(gpx.toString());
        }
    }
}