package com.myapp.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RouteTest {

    @Test
    void constructor_storesAllFields() {
        List<Point> points = List.of(
                new Point(41.0, -8.0, null),
                new Point(40.0, -7.0, null)
        );
        List<POI> pois = new ArrayList<>();
        pois.add(new POI("Café", "amenity:cafe", new Point(40.5, -7.5, "Café")));

        Route route = new Route(points, 100.5, 3600L, TransportMode.CAR, pois);

        assertEquals(2, route.getRoutePoints().size());
        assertEquals(100.5, route.getDistanceKm(), 0.01);
        assertEquals(3600L, route.getDurationSec());
        assertEquals(TransportMode.CAR, route.getMode());
        assertEquals(1, route.getPois().size());
    }

    @Test
    void addPOI_appendsToList() {
        List<Point> points = List.of(new Point(41.0, -8.0, null));
        List<POI> pois = new ArrayList<>();
        Route route = new Route(points, 50.0, 1800L, TransportMode.BIKE, pois);

        POI newPoi = new POI("Restaurante", "amenity:restaurant", new Point(41.1, -8.1, "Restaurante"));
        route.addPOI(newPoi);

        assertEquals(1, route.getPois().size());
        assertEquals("Restaurante", route.getPois().get(0).getName());
    }
}
