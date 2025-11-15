package com.myapp.service;

import java.util.ArrayList;
import java.util.List;

import com.myapp.api.OverpassClient;
import com.myapp.model.POI;
import com.myapp.model.Point;
import com.myapp.model.Route;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Service {
    private final OverpassClient client = new OverpassClient();

    
    public List<POI> getPOIsAlongRoute(Route route, String overpassFilter) {
        if (route == null || route.getRoutePoints().isEmpty()) return List.of();

        double minLat = Double.POSITIVE_INFINITY, maxLat = Double.NEGATIVE_INFINITY;
        double minLon = Double.POSITIVE_INFINITY, maxLon = Double.NEGATIVE_INFINITY;
        for (Point p : route.getRoutePoints()) {
            minLat = Math.min(minLat, p.getLatitude());
            maxLat = Math.max(maxLat, p.getLatitude());
            minLon = Math.min(minLon, p.getLongitude());
            maxLon = Math.max(maxLon, p.getLongitude());
        }

        String ql = String.format("""
                [out:json][timeout:25];
                (
                  %s(%f,%f,%f,%f);
                );
                out center;
                """, overpassFilter, minLat, minLon, maxLat, maxLon);

        try {
            String json = client.postOverpass(ql);
            return parseOverpassPOIs(json);
        } catch (Exception e) {
            System.err.println("[PoiService] Erro Overpass: " + e.getMessage());
            return List.of();
        }
    }

    private List<POI> parseOverpassPOIs(String json) {
        List<POI> pois = new ArrayList<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("elements")) return pois;

        JsonArray elems = root.getAsJsonArray("elements");
        for (JsonElement el : elems) {
            JsonObject obj = el.getAsJsonObject();
            double lat = obj.has("lat") ? obj.get("lat").getAsDouble()
                                        : (obj.has("center") ? obj.getAsJsonObject("center").get("lat").getAsDouble() : Double.NaN);
            double lon = obj.has("lon") ? obj.get("lon").getAsDouble()
                                        : (obj.has("center") ? obj.getAsJsonObject("center").get("lon").getAsDouble() : Double.NaN);
            if (Double.isNaN(lat) || Double.isNaN(lon)) continue;

            String name = null, category = null;
            if (obj.has("tags")) {
                JsonObject tags = obj.getAsJsonObject("tags");
                if (tags.has("name")) name = tags.get("name").getAsString();
                if (tags.has("amenity")) category = "amenity:" + tags.get("amenity").getAsString();
                else if (tags.has("tourism")) category = "tourism:" + tags.get("tourism").getAsString();
                else if (tags.has("shop")) category = "shop:" + tags.get("shop").getAsString();
            }
            pois.add(new POI(name, category, new Point(lat, lon, name)));
        }
        return pois;
    }
}
