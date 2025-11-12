package com.myapp.model;

public class Point {
    private final double latitude;
    private final double longitude;
    private final String name;

    public Point(double latitude, double longitude, String name) {
        this.latitude = latitude; this.longitude = longitude; this.name = name;
    }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getName() { return name; }
    @Override public String toString() {
        return String.format("%s(%.5f, %.5f)", name == null ? "?" : name, latitude, longitude);
    }
}
