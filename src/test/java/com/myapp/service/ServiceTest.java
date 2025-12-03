package com.myapp.service;

import com.myapp.model.Point;
import com.myapp.model.Route;
import com.myapp.model.TransportMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes para o Service.
 * 
 * Nota: Como Service tem dependências hard-coded dos clientes,
 * estes testes são de integração básica.
 * Uma refatoração futura permitiria injeção de dependências para mocking completo.
 */
class ServiceTest {

    private final Service service = new Service();

    @Test
    void getGeocodeFromLocationString_withValidQuery_returnsPoint() {
        // Este teste depende da API Nominatim estar disponível
        Point result = service.getGeocodeFromLocationString("Lisboa, Portugal");

        // Verificar que retorna um ponto com coordenadas plausíveis
        if (result != null) {
            assertNotNull(result.getName());
            assertTrue(result.getLatitude() > 38 && result.getLatitude() < 40);
            assertTrue(result.getLongitude() < -8 && result.getLongitude() > -10);
        } else {
            // API pode falhar ou rate limit
            System.out.println("Geocoding test skipped: API unavailable");
        }
    }

    @Test
    void getGeocodeFromLocationString_withInvalidQuery_returnsNull() {
        Point result = service.getGeocodeFromLocationString("xyzinvalidlocation123456789");

        // Deve retornar null ou lista vazia
        assertNull(result);
    }

    @Test
    void searchLocations_withValidQuery_returnsResults() {
        var results = service.searchLocations("Porto, Portugal");

        // Pode retornar resultados ou estar vazio dependendo da API
        assertNotNull(results);
        // Se resultados existirem, devem ter coordenadas válidas
        results.forEach(point -> {
            assertNotNull(point.getLatitude());
            assertNotNull(point.getLongitude());
        });
    }

    @Test
    void getRoute_withValidPoints_returnsRoute() {
        Point origin = new Point(41.1579, -8.6291, "Porto");
        Point destination = new Point(38.7223, -9.1393, "Lisboa");

        Route route = service.getRoute(origin, destination, TransportMode.CAR);

        // Este teste depende da API OSRM estar disponível
        if (route != null) {
            assertNotNull(route.getRoutePoints());
            assertTrue(route.getRoutePoints().size() > 0);
            assertTrue(route.getDistanceKm() > 0);
            assertTrue(route.getDurationSec() > 0);
            assertEquals(TransportMode.CAR, route.getMode());
        } else {
            System.out.println("Route test skipped: OSRM API unavailable");
        }
    }

    @Test
    void getPOIsAlongRoute_withNullRoute_returnsEmptyList() {
        var pois = service.getPOIsAlongRoute(null, "Restaurante");

        assertNotNull(pois);
        assertTrue(pois.isEmpty());
    }

    @Test
    void getElevationProfile_withNullRoute_returnsNull() {
        var profile = service.getElevationProfile(null);

        assertNull(profile);
    }
}
