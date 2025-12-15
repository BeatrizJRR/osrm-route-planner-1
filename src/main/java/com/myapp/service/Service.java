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

/**
 * Serviço de alto nível que integra os clientes de API (OSRM, Overpass,
 * Nominatim e Open-Elevation) para fornecer funcionalidades de
 * roteamento, geocodificação, recolha de POIs e perfil de elevação.
 *
 * Papel na arquitetura MVC:
 * - Controller/Service: orquestra chamadas às APIs, aplica regras e transforma
 * dados.
 * - Model: contém entidades (`Route`, `Point`, `POI`, `ElevationProfile`).
 * - UI (View): consome métodos deste serviço para apresentar dados.
 *
 * Este serviço converte respostas JSON das APIs em modelos da aplicação.
 */
public class Service {

    // Constantes para evitar números/strings mágicos
    private static final int DEFAULT_POI_SEGMENTS = 10;
    private static final long OVERPASS_RATE_LIMIT_SLEEP_MS = 550L;
    private static final int OVERPASS_SEARCH_RADIUS_M = 1500;
    private static final int OVERPASS_QUERY_TIMEOUT_S = 5;
    private static final long POI_SEARCH_MAX_DURATION_MS = 15_000L;
    private static final double DUPLICATE_COORD_THRESHOLD_DEG = 0.00001;
    private static final int MAX_UNIQUE_POIS = 100;
    private static final int MAX_ELEVATION_SAMPLES = 100;

    private final OSRMClient osrmClient;
    private final OverpassClient overpassClient;
    private final NominatimClient nominatimClient;
    private final ElevationClient elevationClient;

    /**
     * Construtor por omissão que instancia clientes reais.
     */
    public Service() {
        this.osrmClient = new OSRMClient();
        this.overpassClient = new OverpassClient();
        this.nominatimClient = new NominatimClient();
        this.elevationClient = new ElevationClient();
    }

    /**
     * Construtor alternativo para injeção de dependências (útil para testes).
     */
    public Service(OSRMClient osrmClient, OverpassClient overpassClient,
            NominatimClient nominatimClient, ElevationClient elevationClient) {
        this.osrmClient = osrmClient;
        this.overpassClient = overpassClient;
        this.nominatimClient = nominatimClient;
        this.elevationClient = elevationClient;
    }

    /**
     * Obtém uma rota entre dois pontos usando a OSRM e converte o resultado
     * num objeto {@link Route} com distância, duração e pontos do percurso.
     *
     * @param origin      ponto de origem
     * @param destination ponto de destino
     * @param mode        modo de transporte
     * @return {@link Route} construída a partir da resposta OSRM, ou {@code null}
     *         em caso de erro
     */
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

    /**
     * Pesquisa pontos de interesse (POIs) ao longo de uma rota, distribuindo
     * consultas por segmentos uniformes com recurso à Overpass API.
     *
     * @param route rota sobre a qual pesquisar
     * @param type  tipo de POI (ex.: "Restaurante", "Hotel") mapeado para tags
     *              Overpass
     * @return lista de POIs únicos encontrados (limitada a 100)
     */
    public List<POI> getPOIsAlongRoute(Route route, String type) {
        if (route == null || route.getRoutePoints().isEmpty()) {
            return List.of();
        }

        List<POI> result = new ArrayList<>();
        List<Point> points = route.getRoutePoints();

        int size = points.size();

        String tag = switch (type) {
            case "Restaurante" -> "amenity=restaurant";
            case "Café" -> "amenity=cafe";
            case "Fast Food" -> "amenity=fast_food";
            case "Bar" -> "amenity=bar";
            case "Sanitários" -> "amenity=toilets";
            case "ATM" -> "amenity=atm";
            case "Combustível" -> "amenity=fuel";
            case "Farmácia" -> "amenity=pharmacy";
            case "Hospital" -> "amenity=hospital";
            case "Estacionamento" -> "amenity=parking";
            case "Banco" -> "amenity=bank";

            case "Supermercado" -> "shop=supermarket";
            case "Padaria" -> "shop=bakery";
            case "Shopping" -> "shop=mall";
            case "Loja de Conveniência" -> "shop=convenience";

            case "Hotel" -> "tourism=hotel";
            case "Museu" -> "tourism=museum";
            case "Atração Turística" -> "tourism=attraction";

            default -> null;
        };

        if (tag == null) {
            return List.of(); // segurança
        }

        // Nova estratégia: dividir rota em SEGMENTOS IGUAIS e pegar poucos POIs de cada
        // Garante distribuição uniforme ao longo de TODA a rota

        int numSegments = DEFAULT_POI_SEGMENTS; // Dividir rota em segmentos iguais
        List<Point> selectedCheckpoints = new ArrayList<>();

        for (int i = 0; i < numSegments; i++) {
            // Calcular índice do ponto médio de cada segmento
            int index = (i * size) / numSegments + (size / numSegments / 2);
            if (index >= size) {
                index = size - 1;
            }
            selectedCheckpoints.add(points.get(index));
        }

        int checkpoints = selectedCheckpoints.size();
        System.out.println(String.format(
                "[POI Search] Rota dividida em %d segmentos iguais para garantir distribuição uniforme.",
                numSegments));

        long startTime = System.currentTimeMillis();
        long maxDuration = POI_SEARCH_MAX_DURATION_MS; // limite global para pesquisa de POIs

        for (int i = 0; i < checkpoints; i++) {
            // Timeout
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > maxDuration) {
                double elapsedSeconds = elapsed / 1000.0;
                System.out.println(String.format(
                        "[POI Search] Timeout %.1fs. POIs coletados: %d",
                        elapsedSeconds, result.size()));
                break;
            }

            Point p = selectedCheckpoints.get(i);

            System.out.println(String.format(
                    "[POI Search] Segmento %d/%d (lat=%.4f, lon=%.4f)",
                    i + 1, checkpoints, p.getLatitude(), p.getLongitude()));

            try {
                // Delay para rate limit
                if (i > 0) {
                    Thread.sleep(OVERPASS_RATE_LIMIT_SLEEP_MS);
                }

                // Usar raio grande desde início para áreas rurais
                // Limite de 2 POIs garante distribuição mesmo em áreas urbanas
                int searchRadius = OVERPASS_SEARCH_RADIUS_M; // raio grande para cobrir áreas rurais

                String ql = String.format(Locale.US, """
                        [out:json][timeout:%d];
                            node[%s](around:%d,%f,%f);
                            out body 2;
                        """, OVERPASS_QUERY_TIMEOUT_S, tag, searchRadius, p.getLatitude(), p.getLongitude());

                String json = overpassClient.postOverpass(ql);

                if (json != null && !json.trim().isEmpty() && !json.contains("error") && !json.contains("<")) {
                    List<POI> chunkPois = parseOverpassPOIs(json);

                    if (!chunkPois.isEmpty()) {
                        int total = result.size() + chunkPois.size();
                        System.out.println(String.format(
                                "[POI Search] Segmento %d - ✓ %d POIs | Total: %d",
                                i + 1, chunkPois.size(), total));
                        result.addAll(chunkPois);
                    } else {
                        System.out.println(String.format("[POI Search] Segmento %d - 0 POIs", i + 1));
                    }
                } else {
                    System.out.println("[POI Search] Segmento " + (i + 1) + " - resposta inválida");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (com.google.gson.JsonSyntaxException e) {
                System.out.println("[POI Search] Segmento " + (i + 1) + " - erro JSON, pulando");
            } catch (Exception e) {
                System.out.println("[POI Search] Segmento " + (i + 1) + " - erro: " + e.getClass().getSimpleName());
            }
        }

        // Remover duplicados (coordenadas iguais)
        List<POI> unique = new ArrayList<>();
        for (POI poi : result) {
            boolean exists = unique.stream().anyMatch(
                    x -> Math
                            .abs(x.getCoordinate().getLatitude()
                                    - poi.getCoordinate().getLatitude()) < DUPLICATE_COORD_THRESHOLD_DEG
                            &&
                            Math.abs(x.getCoordinate().getLongitude()
                                    - poi.getCoordinate().getLongitude()) < DUPLICATE_COORD_THRESHOLD_DEG);
            if (!exists) {
                unique.add(poi);
            }
        }

        // Aplicar limite final
        System.out
                .println("[POI Search] Total de POIs únicos: " + unique.size() + " (de " + result.size() + " brutos)");
        if (unique.size() > MAX_UNIQUE_POIS) {
            return unique.subList(0, MAX_UNIQUE_POIS);
        }

        return unique;
    }

    /**
     * Converte a resposta JSON da Overpass em objetos {@link POI}.
     *
     * @param json resposta JSON da Overpass
     * @return lista de POIs parseados
     */
    private List<POI> parseOverpassPOIs(String json) {
        List<POI> pois = new ArrayList<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("elements")) {
            return pois;
        }

        JsonArray elems = root.getAsJsonArray("elements");
        for (JsonElement el : elems) {
            JsonObject obj = el.getAsJsonObject();
            double lat = obj.has("lat") ? obj.get("lat").getAsDouble()
                    : (obj.has("center") ? obj.getAsJsonObject("center").get("lat").getAsDouble() : Double.NaN);
            double lon = obj.has("lon") ? obj.get("lon").getAsDouble()
                    : (obj.has("center") ? obj.getAsJsonObject("center").get("lon").getAsDouble() : Double.NaN);
            if (Double.isNaN(lat) || Double.isNaN(lon)) {
                continue;
            }

            String name = null;
            String category = null;
            if (obj.has("tags")) {
                JsonObject tags = obj.getAsJsonObject("tags");
                if (tags.has("name")) {
                    name = tags.get("name").getAsString();
                }
                if (tags.has("amenity")) {
                    category = "amenity:" + tags.get("amenity").getAsString();
                } else if (tags.has("tourism")) {
                    category = "tourism:" + tags.get("tourism").getAsString();
                } else if (tags.has("shop")) {
                    category = "shop:" + tags.get("shop").getAsString();
                }
            }
            pois.add(new POI(name, category, new Point(lat, lon, name)));
        }
        return pois;
    }

    /**
     * Geocodifica uma string de localização usando Nominatim e devolve um
     * {@link Point} com coordenadas e nome de exibição.
     *
     * @param query texto a geocodificar (endereço ou local)
     * @return ponto correspondente ou {@code null} se não houver resultados
     */
    public Point getGeocodeFromLocationString(String query) {
        try {
            String json = nominatimClient.searchJson(query);
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            if (array.isEmpty()) {
                return null;
            }

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

    /**
     * Pesquisa várias localizações pelo texto fornecido usando Nominatim.
     *
     * @param query termo de pesquisa
     * @return lista de pontos encontrados (pode estar vazia)
     */
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

    /**
     * Obtém uma rota com pontos intermédios (waypoints) usando a OSRM.
     *
     * @param originPoint    ponto de origem
     * @param waypointPoints lista de waypoints (na ordem de passagem)
     * @param mode           modo de transporte
     * @return {@link Route} construída a partir da resposta OSRM, ou {@code null}
     *         em caso de erro
     */
    public Route getRouteWithWaypoints(Point originPoint, List<Point> waypointPoints, TransportMode mode) {
        try {
            // Pergunta à OSRM por uma rota completa com múltiplas paragens
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
     * Obtém o perfil de elevação para uma rota.
     * Amostra pontos ao longo da rota para não sobrecarregar a API.
     *
     * @param route rota para a qual obter o perfil de elevação
     * @return perfil de elevação ou {@code null} em caso de erro
     */
    public ElevationProfile getElevationProfile(Route route) {
        if (route == null || route.getRoutePoints().isEmpty()) {
            return null;
        }

        List<Point> points = route.getRoutePoints();

        // Amostrar pontos (máximo configurado para não sobrecarregar API)
        int sampleRate = Math.max(1, points.size() / MAX_ELEVATION_SAMPLES);
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

    /**
     * Converte a resposta JSON da Open-Elevation em {@link ElevationProfile}
     * calculando também as distâncias acumuladas entre pontos amostrados.
     *
     * @param json   resposta JSON da Open-Elevation
     * @param points pontos amostrados correspondentes ao pedido
     * @return perfil de elevação com métricas agregadas
     */
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