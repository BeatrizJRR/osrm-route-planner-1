package com.myapp.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PointTest {

    @Test
    void constructor_storesLatitudeLongitudeAndName() {
        Point point = new Point(38.7223, -9.1393, "Lisboa");

        assertEquals(38.7223, point.getLatitude(), 0.0001);
        assertEquals(-9.1393, point.getLongitude(), 0.0001);
        assertEquals("Lisboa", point.getName());
    }

    @Test
    void constructor_allowsNullName() {
        Point point = new Point(41.1579, -8.6291, null);

        assertEquals(41.1579, point.getLatitude(), 0.0001);
        assertEquals(-8.6291, point.getLongitude(), 0.0001);
        assertNull(point.getName());
    }

    @Test
    void toString_includesNameAndCoordinates() {
        Point point = new Point(40.0, -8.0, "Test");

        String result = point.toString();

        assertTrue(result.contains("Test"));
        assertTrue(result.contains("40.00000"));
        assertTrue(result.contains("-8.00000"));
    }

    @Test
    void toString_usesQuestionMarkForNullName() {
        Point point = new Point(40.0, -8.0, null);

        String result = point.toString();

        assertTrue(result.contains("?"));
        assertTrue(result.contains("40.00000"));
    }
}
