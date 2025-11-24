package com.myapp.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.FileWriter;
import java.io.IOException;

public class RouteExporter {

    /** Exporta a rota para JSON */
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

        // pois
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

    /** Exporta a rota para GPX */
    public static void exportToGPX(Route route, String filePath) throws IOException {
        StringBuilder gpx = new StringBuilder();
        gpx.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <gpx version="1.1" creator="MyApp" xmlns="http://www.topografix.com/GPX/1/1">
                  <trk>
                    <name>Route Export</name>
                    <trkseg>
                """);

        for (Point p : route.getRoutePoints()) {
            gpx.append(String.format(
                    "      <trkpt lat=\"%.6f\" lon=\"%.6f\"></trkpt>\n",
                    p.getLatitude(),
                    p.getLongitude()
            ));
        }

        gpx.append("""
                    </trkseg>
                  </trk>
                </gpx>
                """);

        try (FileWriter fw = new FileWriter(filePath)) {
            fw.write(gpx.toString());
        }
    }
}
