package com.myapp.utils;

import com.myapp.model.POI;
import com.myapp.model.Point;
import com.myapp.model.Route;
import com.myapp.model.TransportMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RouteExporterTest {

    private final List<Path> tempFiles = new ArrayList<>();

    @AfterEach
    void cleanup() throws IOException {
        for (Path p : tempFiles) {
            if (Files.exists(p)) {
                Files.delete(p);
            }
        }
        tempFiles.clear();
    }

    private Route sampleRoute() {
        List<Point> points = List.of(
                new Point(41.1579, -8.6291, null),
                new Point(38.7223, -9.1393, null)
        );
        List<POI> pois = List.of(
                new POI("Café Central", "amenity:cafe", new Point(41.15, -8.62, "Café Central"))
        );
        return new Route(points, 313.0, 10800L, TransportMode.CAR, new ArrayList<>(pois));
    }

    @Test
    void exportToJson_writesExpectedStructure() throws IOException {
        Route route = sampleRoute();
        Path temp = Files.createTempFile("route", ".json");
        tempFiles.add(temp);

        RouteExporter.exportToJson(route, temp.toString());

        String content = Files.readString(temp);
        assertTrue(content.contains("\"distance_km\""));
        assertTrue(content.contains("\"duration_sec\""));
        assertTrue(content.contains("\"mode\""));
        assertTrue(content.contains("\"route_points\""));
        assertTrue(content.contains("\"pois\""));
        assertTrue(content.contains("amenity:cafe"));
    }

    @Test
    void exportToGPX_writesGpxTemplateAndPoints() throws IOException {
        Route route = sampleRoute();
        Path temp = Files.createTempFile("route", ".gpx");
        tempFiles.add(temp);

        RouteExporter.exportToGPX(route, temp.toString());

        String content = Files.readString(temp);
        assertTrue(content.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(content.contains("<gpx version=\"1.1\""));
        assertTrue(content.contains("<trkseg>"));
        // Expect two trkpt entries
        assertTrue(content.contains("<trkpt lat=\"41.157900\" lon=\"-8.629100\">"));
        assertTrue(content.contains("<trkpt lat=\"38.722300\" lon=\"-9.139300\">"));
    }
}
