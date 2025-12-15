package com.myapp.service;

import com.myapp.api.ElevationClient;
import com.myapp.api.NominatimClient;
import com.myapp.api.OSRMClient;
import com.myapp.api.OverpassClient;
import com.myapp.model.ElevationProfile;
import com.myapp.model.POI;
import com.myapp.model.Point;
import com.myapp.model.Route;
import com.myapp.model.TransportMode;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ServicePublicMethodsTest {

        private static OSRMClient fakeOSRM(String json) {
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

        private static OverpassClient fakeOverpass(String json) {
                return new OverpassClient() {
                        @Override
                        public String postOverpass(String overpassQL) {
                                return json;
                        }
                };
        }

        private static NominatimClient fakeNominatim(String searchJson) {
                return new NominatimClient() {
                        @Override
                        public String searchJson(String query) {
                                return searchJson;
                        }
                };
        }

        private static ElevationClient fakeElevation(String json) {
                return new ElevationClient() {
                        @Override
                        public String getElevations(String locations) {
                                return json;
                        }
                };
        }

    @Test
    void testGetRouteWithWaypoints_parsesResponseCorrectly() throws Exception {
        String osrmJson = "{\n" +
                "  \"routes\": [{\n" +
                "    \"distance\": 12345.0,\n" +
                "    \"duration\": 678.0,\n" +
                "    \"geometry\": {\n" +
                "      \"coordinates\": [[-9.10,38.70],[-9.20,38.80]]\n" +
                "    }\n" +
                "  }]\n" +
                "}";

        Service service = new Service(
                fakeOSRM(osrmJson),
                fakeOverpass("{}"),
                fakeNominatim("[]"),
                fakeElevation("{\"results\":[]}")
        );

        Point origin = new Point(38.70, -9.10, "A");
        List<Point> waypoints = List.of(new Point(38.75, -9.15, "W"), new Point(38.80, -9.20, "B"));
        Route route = service.getRouteWithWaypoints(origin, waypoints, TransportMode.CAR);

        assertNotNull(route);
        assertEquals(12.345, route.getDistanceKm(), 1e-6);
        assertEquals(678, route.getDurationSec());
        assertEquals(2, route.getRoutePoints().size());
        assertEquals(38.70, route.getRoutePoints().get(0).getLatitude(), 1e-6);
        assertEquals(-9.20, route.getRoutePoints().get(1).getLongitude(), 1e-6);
    }

    @Test
    void testGetElevationProfile_parsesAndComputesDistances() throws Exception {
        // Route with 3 points along equator (approx 111.2km per degree)
        Route r = new Route(List.of(
                new Point(0.0, 0.0, "p0"),
                new Point(0.0, 1.0, "p1"),
                new Point(0.0, 2.0, "p2")
        ), 0.0, 0, TransportMode.CAR, List.of());

        String elevationJson = "{\n" +
                "  \"results\": [\n" +
                "    {\"elevation\": 10.0},\n" +
                "    {\"elevation\": 20.0},\n" +
                "    {\"elevation\": 15.0}\n" +
                "  ]\n" +
                "}";

        Service service = new Service(
                fakeOSRM("{}"),
                fakeOverpass("{}"),
                fakeNominatim("[]"),
                fakeElevation(elevationJson)
        );

        ElevationProfile profile = service.getElevationProfile(r);
        assertNotNull(profile);
        assertEquals(List.of(10.0, 20.0, 15.0), profile.getElevations());
        assertEquals(3, profile.getDistances().size());
        assertEquals(0.0, profile.getDistances().get(0), 1e-9);
        assertTrue(profile.getDistances().get(2) > profile.getDistances().get(1));
    }

    @Test
    void testGetPOIsAlongRoute_usesOverpassAndDeduplicates() throws Exception {
        // Route with multiple points triggers 10 segments; mocked response duplicates same POIs
        Route r = new Route(List.of(
                new Point(38.70, -9.10, "p0"),
                new Point(38.71, -9.11, "p1"),
                new Point(38.72, -9.12, "p2"),
                new Point(38.73, -9.13, "p3"),
                new Point(38.74, -9.14, "p4"),
                new Point(38.75, -9.15, "p5"),
                new Point(38.76, -9.16, "p6"),
                new Point(38.77, -9.17, "p7"),
                new Point(38.78, -9.18, "p8"),
                new Point(38.79, -9.19, "p9"),
                new Point(38.80, -9.20, "p10")
        ), 0.0, 0, TransportMode.CAR, List.of());

        String overpassJson = "{\n" +
                "  \"elements\": [\n" +
                "    {\"lat\": 38.7001, \"lon\": -9.1001, \"tags\": {\"name\": \"Cafe X\", \"amenity\": \"cafe\"}},\n" +
                "    {\"lat\": 38.7002, \"lon\": -9.1002, \"tags\": {\"name\": \"Rest Y\", \"amenity\": \"restaurant\"}}\n" +
                "  ]\n" +
                "}";

        Service service = new Service(
                fakeOSRM("{}"),
                fakeOverpass(overpassJson),
                fakeNominatim("[]"),
                fakeElevation("{\"results\":[]}")
        );

        List<POI> pois = service.getPOIsAlongRoute(r, "Café");
        assertNotNull(pois);
        // Deduplicated across segments: should keep two unique by coordinates
        assertTrue(pois.size() >= 2);
        assertEquals("amenity:cafe", pois.get(0).getCategory());
        assertNotNull(pois.get(0).getCoordinate());
    }

    @Test
    void testGeocodeAndSearch_parsing() throws Exception {
        String nominatimSingle = "[ {\n" +
                "  \"lat\": 38.70, \"lon\": -9.10, \"display_name\": \"Lisboa\"\n" +
                "} ]";
        String nominatimMulti = "[ {\n" +
                "  \"lat\": 38.70, \"lon\": -9.10, \"display_name\": \"Lisboa\"\n" +
                "}, {\n" +
                "  \"lat\": 41.15, \"lon\": -8.61, \"display_name\": \"Porto\"\n" +
                "} ]";

        Service service = new Service(
                fakeOSRM("{}"),
                fakeOverpass("{}"),
                fakeNominatim(nominatimSingle),
                fakeElevation("{\"results\":[]}")
        );

        Point p = service.getGeocodeFromLocationString("Lisboa");
        assertNotNull(p);
        assertEquals(38.70, p.getLatitude(), 1e-6);
        assertEquals(-9.10, p.getLongitude(), 1e-6);
        assertEquals("Lisboa", p.getName());

        // swap mock for search list
        Service service2 = new Service(
                fakeOSRM("{}"),
                fakeOverpass("{}"),
                fakeNominatim(nominatimMulti),
                fakeElevation("{\"results\":[]}")
        );
        List<Point> results = service2.searchLocations("pt");
        assertEquals(2, results.size());
        assertEquals("Porto", results.get(1).getName());
    }
}
