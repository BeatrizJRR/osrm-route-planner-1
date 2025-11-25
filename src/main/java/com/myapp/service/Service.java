package com.myapp.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.myapp.api.ElevationClient;
import com.myapp.api.NominatimClient;
import com.myapp.api.OSRMClient;
import com.myapp.api.OverpassClient;
import com.myapp.model.ElevationProfile;
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
    private final ElevationClient elevationClient = new ElevationClient();

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
            return new Route(path, distanceKm, durationSec, mode, new ArrayList<>());
        } catch (IOException | InterruptedException e) {
            System.out.println("Error fetching route: " + e.getMessage());
            return null;
        }
    }   
    public List<POI> getPOIsAlongRoute(Route route, String type) {
        if (route == null || route.getRoutePoints().isEmpty()) return List.of();

        List<POI> result = new ArrayList<>();
        List<Point> points = route.getRoutePoints();

        int size = points.size();

        String tag = switch (type) {
            case "Restaurant" -> "amenity=restaurant";
            case "Cafe" -> "amenity=cafe";
            case "Fast Food" -> "amenity=fast_food";
            case "Bar" -> "amenity=bar";
            case "Toilets" -> "amenity=toilets";
            case "ATM" -> "amenity=atm";
            case "Fuel" -> "amenity=fuel";
            case "Pharmacy" -> "amenity=pharmacy";
            case "Hospital" -> "amenity=hospital";
            case "Parking" -> "amenity=parking";
            case "Bank" -> "amenity=bank";

            case "Supermarket" -> "shop=supermarket";
            case "Bakery" -> "shop=bakery";
            case "Mall" -> "shop=mall";
            case "Convenience Store" -> "shop=convenience";

            case "Hotel" -> "tourism=hotel";
            case "Museum" -> "tourism=museum";
            case "Attraction" -> "tourism=attraction";

            default -> null;
        };

        if (tag == null) return List.of(); // segurança

        // Nova estratégia: dividir rota em SEGMENTOS IGUAIS e pegar poucos POIs de cada
        // Garante distribuição uniforme ao longo de TODA a rota
        
        int numSegments = 10; // Dividir rota em 10 segmentos
        List<Point> selectedCheckpoints = new ArrayList<>();
        
        for (int i = 0; i < numSegments; i++) {
            // Calcular índice do ponto médio de cada segmento
            int index = (i * size) / numSegments + (size / numSegments / 2);
            if (index >= size) index = size - 1;
            selectedCheckpoints.add(points.get(index));
        }
        
        int checkpoints = selectedCheckpoints.size();
        System.out.println("[POI Search] Rota dividida em " + numSegments + " segmentos iguais para garantir distribuição uniforme.");

        long startTime = System.currentTimeMillis();
        long maxDuration = 15000; // 15 segundos total

        for (int i = 0; i < checkpoints; i++) {
            // Timeout
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > maxDuration) {
                System.out.println("[POI Search] Timeout " + (elapsed/1000.0) + "s. POIs coletados: " + result.size());
                break;
            }
            
            Point p = selectedCheckpoints.get(i);
            
            System.out.println("[POI Search] Segmento " + (i+1) + "/" + checkpoints + 
                             " (lat=" + String.format("%.4f", p.getLatitude()) + 
                             ", lon=" + String.format("%.4f", p.getLongitude()) + ")");

            try {
                // Delay para rate limit
                if (i > 0) {
                    Thread.sleep(550);
                }
                
                // Usar raio grande desde início para áreas rurais
                // Limite de 2 POIs garante distribuição mesmo em áreas urbanas
                int searchRadius = 1500; // 1.5km - raio grande para cobrir áreas rurais
                
                String ql = String.format(Locale.US, """
                [out:json][timeout:5];
                node[%s](around:%d,%f,%f);
                out body 2;
                """, tag, searchRadius, p.getLatitude(), p.getLongitude());
                
                String json = overpassClient.postOverpass(ql);
                
                if (json != null && !json.trim().isEmpty() && !json.contains("error") && !json.contains("<")) {
                    List<POI> chunkPois = parseOverpassPOIs(json);
                    
                    if (!chunkPois.isEmpty()) {
                        System.out.println("[POI Search] Segmento " + (i+1) + " - ✓ " + chunkPois.size() + " POIs | Total: " + (result.size() + chunkPois.size()));
                        result.addAll(chunkPois);
                    } else {
                        System.out.println("[POI Search] Segmento " + (i+1) + " - 0 POIs");
                    }
                } else {
                    System.out.println("[POI Search] Segmento " + (i+1) + " - resposta inválida");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (com.google.gson.JsonSyntaxException e) {
                System.out.println("[POI Search] Segmento " + (i+1) + " - erro JSON, pulando");
            } catch (Exception e) {
                System.out.println("[POI Search] Segmento " + (i+1) + " - erro: " + e.getClass().getSimpleName());
            }
        }


        // Remover duplicados (coordenadas iguais)
        List<POI> unique = new ArrayList<>();
        for (POI poi : result) {
            boolean exists = unique.stream().anyMatch(
                x -> Math.abs(x.getCoordinate().getLatitude() - poi.getCoordinate().getLatitude()) < 0.00001 &&
                    Math.abs(x.getCoordinate().getLongitude() - poi.getCoordinate().getLongitude()) < 0.00001
            );
            if (!exists) unique.add(poi);
        }

        // Aplicar limite final
        System.out.println("[POI Search] Total de POIs únicos: " + unique.size() + " (de " + result.size() + " brutos)");
        if (unique.size() > 100) {  
            return unique.subList(0, 100);
        }

        return unique;
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

    public List<Point> searchLocations(String query) {
        List<Point> results = new ArrayList<>();
        try {
            String json = nominatimClient.searchJson(query);
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            for (JsonElement el : array) {
                JsonObject obj = el.getAsJsonObject();
                double lat = obj.get("lat").getAsDouble();
                double lon = obj.get("lon").getAsDouble();
                String displayName = obj.get("display_name").getAsString();
                results.add(new Point(lat, lon, displayName));
            }
        } catch (Exception e) {
            System.err.println("[GeocodingService] Erro a pesquisar Nominatim: " + e.getMessage());
        }
        return results;
    }

    public Route getRouteWithWaypoints(Point originPoint, List<Point> waypointPoints, TransportMode mode) {
        try {
            // Ask OSRM for a full multi-stop route
            String routeJson = osrmClient.getRouteJsonWithWaypoints(originPoint, waypointPoints, mode);

            JsonObject root = JsonParser.parseString(routeJson).getAsJsonObject();
            if (!root.has("routes")) {
                System.err.println("[RouteService] JSON sem 'routes'");
                return null;
            }

            JsonArray routes = root.getAsJsonArray("routes");
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

            return new Route(path, distanceKm, durationSec, mode, new ArrayList<>());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // Calcular distância em km entre dois pontos usando fórmula de Haversine
    private double calculateDistance(Point p1, Point p2) {
        final double R = 6371.0; // Raio da Terra em km
        
        double lat1 = Math.toRadians(p1.getLatitude());
        double lat2 = Math.toRadians(p2.getLatitude());
        double dLat = Math.toRadians(p2.getLatitude() - p1.getLatitude());
        double dLon = Math.toRadians(p2.getLongitude() - p1.getLongitude());
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    /**
     * Obter perfil de elevação para uma rota
     * Amostra pontos ao longo da rota para não sobrecarregar a API
     */
    public ElevationProfile getElevationProfile(Route route) {
        if (route == null || route.getRoutePoints().isEmpty()) return null;
        
        List<Point> points = route.getRoutePoints();
        
        // Amostrar pontos (máximo 100 para não sobrecarregar API)
        int sampleRate = Math.max(1, points.size() / 100);
        List<Point> sampledPoints = new ArrayList<>();
        for (int i = 0; i < points.size(); i += sampleRate) {
            sampledPoints.add(points.get(i));
        }
        // Garantir que o último ponto está incluído
        if (!sampledPoints.contains(points.get(points.size() - 1))) {
            sampledPoints.add(points.get(points.size() - 1));
        }
        
        // Construir string de localizações para a API
        StringBuilder locations = new StringBuilder();
        for (int i = 0; i < sampledPoints.size(); i++) {
            Point p = sampledPoints.get(i);
            locations.append(p.getLatitude()).append(",").append(p.getLongitude());
            if (i < sampledPoints.size() - 1) {
                locations.append("|");
            }
        }
        
        try {
            String json = elevationClient.getElevations(locations.toString());
            return parseElevationProfile(json, sampledPoints);
        } catch (Exception e) {
            System.err.println("[Elevation] Erro ao obter elevações: " + e.getMessage());
            return null;
        }
    }
    
    private ElevationProfile parseElevationProfile(String json, List<Point> points) {
        List<Double> elevations = new ArrayList<>();
        List<Double> distances = new ArrayList<>();
        
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray results = root.getAsJsonArray("results");
        
        double accumulatedDistance = 0.0;
        distances.add(0.0);
        
        for (int i = 0; i < results.size(); i++) {
            JsonObject result = results.get(i).getAsJsonObject();
            double elevation = result.get("elevation").getAsDouble();
            elevations.add(elevation);
            
            if (i > 0) {
                accumulatedDistance += calculateDistance(points.get(i - 1), points.get(i));
                distances.add(accumulatedDistance);
            }
        }
        
        return new ElevationProfile(elevations, distances);
    }

}