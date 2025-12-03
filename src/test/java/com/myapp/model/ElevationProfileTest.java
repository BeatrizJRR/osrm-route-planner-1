package com.myapp.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ElevationProfileTest {

    @Test
    void constructor_calculatesStatistics() {
        List<Double> elevations = List.of(100.0, 150.0, 120.0, 180.0);
        List<Double> distances = List.of(0.0, 1.0, 2.0, 3.0);

        ElevationProfile profile = new ElevationProfile(elevations, distances);

        assertEquals(180.0, profile.getMaxElevation(), 0.01);
        assertEquals(100.0, profile.getMinElevation(), 0.01);
        // Ascents: 100->150 (+50), 120->180 (+60) = 110
        assertEquals(110.0, profile.getTotalAscent(), 0.01);
        // Descents: 150->120 (30) = 30
        assertEquals(30.0, profile.getTotalDescent(), 0.01);
    }

    @Test
    void getElevations_returnsOriginalList() {
        List<Double> elevations = List.of(100.0, 200.0);
        List<Double> distances = List.of(0.0, 1.0);

        ElevationProfile profile = new ElevationProfile(elevations, distances);

        assertEquals(elevations, profile.getElevations());
        assertEquals(2, profile.getElevations().size());
    }

    @Test
    void getDistances_returnsOriginalList() {
        List<Double> elevations = List.of(100.0, 200.0);
        List<Double> distances = List.of(0.0, 5.0);

        ElevationProfile profile = new ElevationProfile(elevations, distances);

        assertEquals(distances, profile.getDistances());
        assertEquals(5.0, profile.getDistances().get(1), 0.01);
    }
}
