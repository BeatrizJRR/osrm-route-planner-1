package com.myapp.model;

import java.util.List;

public class Route {
    private final List<Point> routePoints;
    private final double distanceKm;
    private final long durationSec;
    private final TransportMode mode;
    private final List<POI> pois;

    public Route(List<Point> routePoints, double distanceKm, long durationSec, TransportMode mode, List<POI> pois) {
        this.routePoints = routePoints;
        this.distanceKm = distanceKm;
        this.durationSec = durationSec;
        this.mode = mode;
        this.pois = pois;
    }

    public List<Point> getRoutePoints() {
        return routePoints;
    }
    public double getDistanceKm() {
        return distanceKm;
    }

    public long getDurationSec() {
        return durationSec;
    }

    public TransportMode getMode() {
        return mode;
    }

    public List<POI> getPois() {
        return pois;
    }
}