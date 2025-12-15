package com.myapp.service;

import com.myapp.model.ElevationProfile;
import com.myapp.model.POI;
import com.myapp.model.Point;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServiceParsingTest {

    @SuppressWarnings("unchecked")
    @Test
    void parseOverpassPOIs_parsesVariousShapes() throws Exception {
        String json = """
            {"elements": [
              {"lat": 1.0, "lon": 2.0, "tags": {"name": "Cafe A", "amenity": "cafe"}},
              {"center": {"lat": 3.0, "lon": 4.0}, "tags": {"shop": "bakery"}}
            ]}
            """;
        Service service = new Service();
        Method m = Service.class.getDeclaredMethod("parseOverpassPOIs", String.class);
        m.setAccessible(true);
        List<POI> pois = (List<POI>) m.invoke(service, json);

        assertEquals(2, pois.size());
        assertEquals("amenity:cafe", pois.get(0).getCategory());
        assertEquals("shop:bakery", pois.get(1).getCategory());
        assertEquals(1.0, pois.get(0).getCoordinate().getLatitude(), 1e-9);
        assertEquals(4.0, pois.get(1).getCoordinate().getLongitude(), 1e-9);
    }

    @Test
    void calculateDistance_haversineApproximation() throws Exception {
        Service service = new Service();
        Method m = Service.class.getDeclaredMethod("calculateDistance", Point.class, Point.class);
        m.setAccessible(true);

        Point p1 = new Point(0.0, 0.0, null);
        Point p2 = new Point(0.0, 1.0, null); // ~111.195 km at equator per degree lon
        double d = (double) m.invoke(service, p1, p2);
        assertEquals(111.2, d, 1.5); // allow ~1.5 km tolerance
    }

    @Test
    void parseElevationProfile_buildsProfileAndDistances() throws Exception {
        String json = """
          {"results": [
            {"elevation": 100.0},
            {"elevation": 110.0},
            {"elevation": 90.0}
          ]}
        """;
        List<Point> pts = new ArrayList<>();
        pts.add(new Point(0.0, 0.0, null));
        pts.add(new Point(0.0, 0.5, null));
        pts.add(new Point(0.0, 1.0, null));

        Service service = new Service();
        Method m = Service.class.getDeclaredMethod("parseElevationProfile", String.class, List.class);
        m.setAccessible(true);
        ElevationProfile profile = (ElevationProfile) m.invoke(service, json, pts);

        assertNotNull(profile);
        assertEquals(3, profile.getElevations().size());
        assertEquals(3, profile.getDistances().size());
        assertEquals(100.0, profile.getElevations().get(0), 1e-9);
        assertEquals(110.0, profile.getElevations().get(1), 1e-9);
        assertEquals(90.0, profile.getElevations().get(2), 1e-9);
        // distances should be non-decreasing and start at 0
        assertEquals(0.0, profile.getDistances().get(0), 1e-6);
        assertTrue(profile.getDistances().get(1) > 0.0);
        assertTrue(profile.getDistances().get(2) > profile.getDistances().get(1));
    }
}
