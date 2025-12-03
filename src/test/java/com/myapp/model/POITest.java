package com.myapp.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class POITest {

    @Test
    void constructor_storesNameCategoryAndCoordinate() {
        Point coord = new Point(38.7, -9.1, "POI Location");
        POI poi = new POI("Museu", "tourism:museum", coord);

        assertEquals("Museu", poi.getName());
        assertEquals("tourism:museum", poi.getCategory());
        assertEquals(coord, poi.getCoordinate());
        assertEquals(38.7, poi.getCoordinate().getLatitude(), 0.01);
    }

    @Test
    void coordinate_canBeRetrieved() {
        Point coord = new Point(40.0, -8.0, null);
        POI poi = new POI("ATM", "amenity:atm", coord);

        Point retrieved = poi.getCoordinate();

        assertNotNull(retrieved);
        assertEquals(40.0, retrieved.getLatitude(), 0.01);
        assertEquals(-8.0, retrieved.getLongitude(), 0.01);
    }
}
