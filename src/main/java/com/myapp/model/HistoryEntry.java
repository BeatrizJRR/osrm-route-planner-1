package com.myapp.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Representa uma entrada no histórico de rotas.
 * Guarda os parâmetros necessários para reconstruir a rota.
 */
public class HistoryEntry {
    private Point origin;
    private Point destination;
    private List<Point> waypoints;
    private TransportMode mode;
    private String timestamp; // Guardamos como String para facilitar o JSON
    private String description; // Ex: "Lisboa -> Porto (Carro)"

    public HistoryEntry(Point origin, Point destination, List<Point> waypoints, TransportMode mode) {
        this.origin = origin;
        this.destination = destination;
        this.waypoints = waypoints;
        this.mode = mode;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        this.description = generateDescription();
    }

    private String generateDescription() {
        return String.format("%s ➔ %s (%s)", 
            origin.getName(), 
            destination.getName(), 
            mode.toString());
    }

    // Getters
    public Point getOrigin() { return origin; }
    public Point getDestination() { return destination; }
    public List<Point> getWaypoints() { return waypoints; }
    public TransportMode getMode() { return mode; }
    
    @Override
    public String toString() {
        return "[" + timestamp + "] " + description;
    }
}