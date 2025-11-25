package com.myapp.model;

import java.util.List;

public class ElevationProfile {
    private final List<Double> elevations; // em metros
    private final List<Double> distances;  // distância acumulada em km
    private final double maxElevation;
    private final double minElevation;
    private final double totalAscent;     // subida total em m
    private final double totalDescent;    // descida total em m

    public ElevationProfile(List<Double> elevations, List<Double> distances) {
        this.elevations = elevations;
        this.distances = distances;
        
        // Calcular estatísticas
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        double ascent = 0;
        double descent = 0;
        
        for (int i = 0; i < elevations.size(); i++) {
            double elev = elevations.get(i);
            max = Math.max(max, elev);
            min = Math.min(min, elev);
            
            if (i > 0) {
                double diff = elev - elevations.get(i - 1);
                if (diff > 0) ascent += diff;
                else descent += Math.abs(diff);
            }
        }
        
        this.maxElevation = max;
        this.minElevation = min;
        this.totalAscent = ascent;
        this.totalDescent = descent;
    }

    public List<Double> getElevations() { return elevations; }
    public List<Double> getDistances() { return distances; }
    public double getMaxElevation() { return maxElevation; }
    public double getMinElevation() { return minElevation; }
    public double getTotalAscent() { return totalAscent; }
    public double getTotalDescent() { return totalDescent; }
}
