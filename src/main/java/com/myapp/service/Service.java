package com.myapp.service;

import java.io.IOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.myapp.api.OSRMClient;
import com.myapp.model.Point;
import com.myapp.model.Route;
import com.myapp.model.TransportMode;


public class Service {

    private final OSRMClient client = new OSRMClient();
    
    public Route getRoute(Point origin, Point destination, TransportMode mode) {
        try {
            String routeJson = client.getRouteJson(origin, destination, mode);
            System.out.println(routeJson);
            JsonObject JsonObject = JsonParser.parseString(routeJson).getAsJsonObject();
        } catch (IOException | InterruptedException e) {
            System.out.println("Error fetching route: " + e.getMessage());
            return null;
        }
        return null;
    }

    public static void main(String[] args) {
        Service service = new Service();
        Point origin = new Point(40.7128, -74.0060, "New York");
        Point destination = new Point(34.0522, -118.2437, "Los Angeles");
        service.getRoute(origin, destination, TransportMode.CAR);
    }


    
}
