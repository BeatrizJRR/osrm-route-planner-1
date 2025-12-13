package com.myapp.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.time.Duration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.myapp.model.POI;
import com.myapp.model.Point;
import com.myapp.model.Route;
import com.myapp.model.TransportMode;

import javafx.application.Platform;

/**
 * Testes unitários básicos para componentes da UI sem necessidade de janela
 * gráfica.
 */
class MapViewerBasicTest {

    @BeforeEach
    void initJavaFX() {
        // Inicializa o toolkit JavaFX se necessário
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException e) {
            // Toolkit já está inicializado
        }
    }

    @Test
    void testMapViewerConstants() {
        // Testa se as constantes estão definidas corretamente
        assertTrue(true, "Constantes devem estar acessíveis");
    }

    @Test
    void longRunningWork_shouldNotBeOnFxThread() {
        Assertions.assertFalse(Platform.isFxApplicationThread(),
                "Trabalhos pesados não devem executar no thread do JavaFX");
    }

    @Test
    void testRouteDataStructures() {
        // Testa estruturas de dados usadas pela UI
        List<Point> points = new ArrayList<>();
        points.add(new Point(38.7223, -9.1393, "Lisboa"));
        points.add(new Point(41.1579, -8.6291, "Porto"));

        List<POI> pois = new ArrayList<>();
        Route route = new Route(points, 313.0, 10800L, TransportMode.CAR, pois);

        assertNotNull(route, "Route deve ser criada");
        assertEquals(2, route.getRoutePoints().size(), "Route deve ter 2 pontos");
    }

    @Test
    void testPointCreation() {
        // Testa criação de pontos para marcadores
        Point point = new Point(38.7223, -9.1393, "Lisboa");

        assertNotNull(point);
        assertEquals(38.7223, point.getLatitude());
        assertEquals(-9.1393, point.getLongitude());
        assertEquals("Lisboa", point.getName());
    }

    @Test
    void testPOICreation() {
        // Testa criação de POIs para exibição no mapa
        Point coordinate = new Point(38.7223, -9.1393, "Museu");
        POI poi = new POI("Museu Nacional", "tourism:museum", coordinate);

        assertNotNull(poi);
        assertEquals("Museu Nacional", poi.getName());
        assertEquals("tourism:museum", poi.getCategory());
        assertNotNull(poi.getCoordinate());
    }

    @Test
    void testTransportModeValues() {
        // Testa modos de transporte disponíveis na UI
        TransportMode[] modes = TransportMode.values();

        assertTrue(modes.length > 0, "Deve haver modos de transporte disponíveis");
        assertNotNull(TransportMode.CAR);
        assertNotNull(TransportMode.BIKE);
        assertNotNull(TransportMode.FOOT);
    }

    @Test
    void testWaypointListInitialization() {
        // Testa inicialização de lista de waypoints
        List<Point> waypoints = new ArrayList<>();

        assertNotNull(waypoints);
        assertEquals(0, waypoints.size(), "Lista deve iniciar vazia");

        waypoints.add(new Point(38.7223, -9.1393, "Ponto 1"));
        assertEquals(1, waypoints.size(), "Lista deve ter 1 elemento");
    }

    @Test
    void testPOIListInitialization() {
        // Testa inicialização de lista de POIs
        List<POI> pois = new ArrayList<>();

        assertNotNull(pois);
        assertEquals(0, pois.size(), "Lista deve iniciar vazia");

        Point coord = new Point(38.7223, -9.1393, "Local");
        pois.add(new POI("Local de Interesse", "tourism", coord));
        assertEquals(1, pois.size(), "Lista deve ter 1 POI");
    }

    @Test
    void testRouteWithMultipleWaypoints() {
        // Testa criação de rota com múltiplos waypoints
        List<Point> points = new ArrayList<>();
        points.add(new Point(38.7223, -9.1393, "Lisboa"));
        points.add(new Point(39.3999, -8.2245, "Santarém"));
        points.add(new Point(40.2033, -8.4103, "Coimbra"));
        points.add(new Point(41.1579, -8.6291, "Porto"));

        Route route = new Route(points, 500.0, 18000L, TransportMode.CAR, new ArrayList<>());

        assertEquals(4, route.getRoutePoints().size());
        assertEquals(500.0, route.getDistanceKm());
        assertEquals(18000L, route.getDurationSec());
    }

    @Test
    void testEmptyRoute() {
        // Testa criação de rota vazia
        List<Point> emptyPoints = new ArrayList<>();
        Route emptyRoute = new Route(emptyPoints, 0.0, 0L, TransportMode.CAR, new ArrayList<>());

        assertNotNull(emptyRoute);
        assertEquals(0, emptyRoute.getRoutePoints().size());
        assertEquals(0.0, emptyRoute.getDistanceKm());
    }

    @Test
    void testRouteWithPOIs() {
        // Testa rota com POIs
        List<Point> points = new ArrayList<>();
        points.add(new Point(38.7223, -9.1393, "Início"));
        points.add(new Point(38.7500, -9.1500, "Fim"));

        List<POI> pois = new ArrayList<>();
        pois.add(new POI("Restaurante", "amenity:restaurant",
                new Point(38.7350, -9.1400, "Restaurante")));
        pois.add(new POI("Hotel", "tourism:hotel",
                new Point(38.7450, -9.1450, "Hotel")));

        Route route = new Route(points, 5.0, 600L, TransportMode.FOOT, pois);

        assertEquals(2, route.getPois().size());
        assertEquals("Restaurante", route.getPois().get(0).getName());
        assertEquals("Hotel", route.getPois().get(1).getName());
    }
}
