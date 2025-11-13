package com.myapp.model;

import java.util.List;

public class Route {
    private final double distanceKm;
    private final long durationSec;
    private final TransportMode mode;
    private final List<POI> pois;
    private final List<Point> path;

    public Route(double distanceKm, long durationSec, TransportMode mode, List<POI> pois, List<Point> path) {
        this.distanceKm = distanceKm;
        this.durationSec = durationSec;
        this.mode = mode;
        this.pois = pois;
        this.path = path;
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

    public List<Point> getPath() {
        return path;
    }


}
