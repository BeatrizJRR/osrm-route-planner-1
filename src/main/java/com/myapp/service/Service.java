package com.myapp.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.myapp.api.NominatimClient;
import com.myapp.api.OSRMClient;
import com.myapp.api.OverpassClient;
import com.myapp.model.POI;
import com.myapp.model.Point;
import com.myapp.model.Route;
import com.myapp.model.TransportMode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Service {
    
    private final OSRMClient osrmClient = new OSRMClient();
    private final OverpassClient overpassClient = new OverpassClient();
    private final NominatimClient nominatimClient = new NominatimClient();

    public Route getRoute(Point origin, Point destination, TransportMode mode) {
        try {
            String route = osrmClient.getRouteJson(origin, destination, mode);
            JsonObject osrmResponseJSON = JsonParser.parseString(route).getAsJsonObject();
            if (!osrmResponseJSON.has("routes")) {
                System.err.println("[RouteService] JSON sem 'routes'");
                return null;
            }
            JsonArray routes = osrmResponseJSON.getAsJsonArray("routes");
            if (routes.size() == 0) {
                System.err.println("[RouteService] Nenhuma rota encontrada");
                return null;
            }

            JsonObject firstRoute = routes.get(0).getAsJsonObject();
            double distanceKm = firstRoute.get("distance").getAsDouble() / 1000.0;
            long durationSec = Math.round(firstRoute.get("duration").getAsDouble());

            List<Point> path = new ArrayList<>();
            JsonArray coordsArray = firstRoute
                    .getAsJsonObject("geometry")
                    .getAsJsonArray("coordinates");

            for (JsonElement el : coordsArray) {
                JsonArray coord = el.getAsJsonArray();
                double lon = coord.get(0).getAsDouble();
                double lat = coord.get(1).getAsDouble();
                path.add(new Point(lat, lon, null));
            }
            return new Route(path, distanceKm, durationSec, mode, List.of());
        } catch (IOException | InterruptedException e) {
            System.out.println("Error fetching route: " + e.getMessage());
            return null;
        }
    }   

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
            String json = overpassClient.postOverpass(ql);
            System.out.println("[PoiService] Overpass response: " + json);
            List<POI> routePOIs = parseOverpassPOIs(json);
            for (POI poi : routePOIs) {
                route.addPOI(poi);
            }
            return routePOIs;
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

    public Point getGeocodeFromLocationString(String query) {
        try {
            String json = nominatimClient.searchJson(query);
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            if (array.isEmpty()) return null;

            JsonElement first = array.get(0);
            double lat = first.getAsJsonObject().get("lat").getAsDouble();
            double lon = first.getAsJsonObject().get("lon").getAsDouble();
            String displayName = first.getAsJsonObject().get("display_name").getAsString();
            return new Point(lat, lon, displayName);
        } catch (Exception e) {
            System.err.println("[GeocodingService] Erro a parsear Nominatim: " + e.getMessage());
            return null;
        }
    }
}