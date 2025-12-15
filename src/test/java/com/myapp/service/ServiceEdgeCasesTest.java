package com.myapp.service;

import com.myapp.api.ElevationClient;
import com.myapp.api.NominatimClient;
import com.myapp.api.OSRMClient;
import com.myapp.api.OverpassClient;
import com.myapp.model.Point;
import com.myapp.model.Route;
import com.myapp.model.TransportMode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServiceEdgeCasesTest {

    private static OSRMClient osrmReturning(String json) {
        return new OSRMClient() {
            @Override
            public String getRouteJson(Point origin, Point destination, TransportMode mode) {
                return json;
            }

            @Override
            public String getRouteJsonWithWaypoints(Point origin, List<Point> waypoints, TransportMode mode) {
                return json;
            }
        };
    }

    private static OSRMClient osrmThrowing(IOException ex) {
        return new OSRMClient() {
            @Override
            public String getRouteJson(Point origin, Point destination, TransportMode mode) throws IOException {
                throw ex;
            }
        };
    }

    private static OverpassClient overpassReturning(String json) {
        return new OverpassClient() {
            @Override
            public String postOverpass(String overpassQL) {
                return json;
            }
        };
    }

    private static NominatimClient nominatimReturning(String json) {
        return new NominatimClient() {
            @Override
            public String searchJson(String query) {
                return json;
            }
        };
    }

    private static NominatimClient nominatimThrowing(RuntimeException ex) {
        return new NominatimClient() {
            @Override
            public String searchJson(String query) {
                throw ex;
            }
        };
    }

    private static ElevationClient elevationThrowing(RuntimeException ex) {
        return new ElevationClient() {
            @Override
            public String getElevations(String locations) {
                throw ex;
            }
        };
    }

    private static ElevationClient elevationReturning(String json) {
        return new ElevationClient() {
            @Override
            public String getElevations(String locations) {
                return json;
            }
        };
    }

    @Test
    void getRoute_handlesMissingAndEmptyRoutesAndException() {
        Service withMissing = new Service(
            osrmReturning("{ }"),
            overpassReturning("{}"),
            nominatimReturning("[]"),
            elevationReturning("{\"results\":[]}")
        );
        assertNull(withMissing.getRoute(new Point(1,1,null), new Point(2,2,null), TransportMode.CAR));

        Service withEmpty = new Service(
            osrmReturning("{ \"routes\": [] }"),
            overpassReturning("{}"),
            nominatimReturning("[]"),
            elevationReturning("{\"results\":[]}")
        );
        assertNull(withEmpty.getRoute(new Point(1,1,null), new Point(2,2,null), TransportMode.CAR));

        Service withException = new Service(
            osrmThrowing(new IOException("network")),
            overpassReturning("{}"),
            nominatimReturning("[]"),
            elevationReturning("{\"results\":[]}")
        );
        assertNull(withException.getRoute(new Point(1,1,null), new Point(2,2,null), TransportMode.CAR));
    }

    @Test
    void getRouteWithWaypoints_handlesMissingRoutes() {
        Service svc = new Service(
            osrmReturning("{ }"),
            overpassReturning("{}"),
            nominatimReturning("[]"),
            elevationReturning("{\"results\":[]}")
        );
        assertNull(svc.getRouteWithWaypoints(new Point(0,0,null), List.of(new Point(1,1,null)), TransportMode.CAR));
    }

    @Test
    void getPOIsAlongRoute_unknownTypeAndInvalidJson() {
        Route route = new Route(List.of(new Point(0,0,null), new Point(0,1,null)), 0.0, 0, TransportMode.CAR, List.of());

        Service unknownType = new Service(
            osrmReturning("{}"),
            overpassReturning("{}"),
            nominatimReturning("[]"),
            elevationReturning("{\"results\":[]}")
        );
        assertTrue(unknownType.getPOIsAlongRoute(route, "Tipo Desconhecido").isEmpty());

        // invalid JSON (will reach JsonSyntaxException path as it's non-empty and doesn't contain 'error' or '<')
        Service invalidJson = new Service(
            osrmReturning("{}"),
            overpassReturning("{"),
            nominatimReturning("[]"),
            elevationReturning("{\"results\":[]}")
        );
        assertNotNull(invalidJson.getPOIsAlongRoute(route, "Café"));
    }

    @Test
    void geocoding_emptyAndExceptionPaths() {
        Service empty = new Service(
            osrmReturning("{}"),
            overpassReturning("{}"),
            nominatimReturning("[]"),
            elevationReturning("{\"results\":[]}")
        );
        assertNull(empty.getGeocodeFromLocationString("nowhere"));

        Service throwing = new Service(
            osrmReturning("{}"),
            overpassReturning("{}"),
            nominatimThrowing(new RuntimeException("boom")),
            elevationReturning("{\"results\":[]}")
        );
        assertNull(throwing.getGeocodeFromLocationString("nowhere"));
        assertTrue(throwing.searchLocations("any").isEmpty());
    }

    @Test
    void elevationProfile_nullEmptyAndException() {
        Service svc = new Service(
                osrmReturning("{}"),
                overpassReturning("{}"),
                nominatimReturning("[]"),
                elevationThrowing(new RuntimeException("fail"))
        );
        assertNull(svc.getElevationProfile(null));

        Route emptyRoute = new Route(List.of(), 0.0, 0, TransportMode.CAR, List.of());
        assertNull(svc.getElevationProfile(emptyRoute));

        Route r = new Route(List.of(new Point(0,0,null), new Point(0,1,null)), 0.0, 0, TransportMode.CAR, List.of());
        assertNull(svc.getElevationProfile(r)); // exception from client
    }
}
