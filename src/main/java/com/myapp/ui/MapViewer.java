package com.myapp.ui;

import com.myapp.model.POI;
import com.myapp.model.Point;
import com.myapp.model.Route;
import com.myapp.model.RouteExporter;
import com.myapp.model.TransportMode;
import com.myapp.service.Service;
import com.sothawo.mapjfx.*;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import com.sothawo.mapjfx.event.MapViewEvent;

import java.util.ArrayList;
import java.util.List;

public class MapViewer extends Application {

    // --- MAP ELEMENTS ---
    private final MapView mapView = new MapView();
    private Marker originMarker = null;
    private CoordinateLine currentRouteLine = null;

    private final List<Point> waypointPoints = new ArrayList<>();
    private final List<Marker> waypointMarkers = new ArrayList<>();
    private final List<Marker> poiMarkers = new ArrayList<>();

    // --- UI ---
    private TextField origemField;
    private TextField pesquisaField;
    private VBox waypointListUI = new VBox(8);

    private Label routeSummaryLabel = new Label("Defina origem e pontos de paragem...");
    private Label poiSummaryLabel = new Label("POIs: 0");

    private ComboBox<TransportMode> modeBox = new ComboBox<>();
    private ComboBox<String> poiFilterBox = new ComboBox<>();

    private Point lastSearchPoint = null;
    private Route lastRoute = null;

    private final Service service = new Service();
    private ContextMenu suggestionsMenu = new ContextMenu();

    @Override
    public void start(Stage stage) {
        stage.setTitle("Planeador de Rotas");

        // --- SIDEBAR ---
        VBox sidebar = buildSidebarUI();

        // --- MAP CONTAINER ---
        StackPane mapContainer = new StackPane(mapView);
        mapContainer.setAlignment(Pos.CENTER);

        // --- INIT MAP ---
        initMap();

        // --- ROOT LAYOUT ---
        BorderPane root = new BorderPane();
        root.setLeft(sidebar);
        root.setCenter(mapContainer);

        Scene scene = new Scene(root, 1350, 800);
        stage.setScene(scene);
        stage.show();
    }

    // ============================================================
    // ✅ UI CONSTRUCTION
    // ============================================================
    private VBox buildSidebarUI() {
        VBox sidebar = new VBox(20);
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(330);

        // ORIGIN
        Label lblOrigem = new Label("📍 Origem");
        origemField = new TextField();
        origemField.setPromptText("Introduzir localização...");
        Button btnSetOrigem = new Button("Definir Origem");
        btnSetOrigem.setOnAction(e -> handleSetOrigem());

        // WAYPOINT LIST
        Label lblStops = new Label("🛑 Paragens");
        Button btnClearStops = new Button("Limpar Paragens");
        btnClearStops.setOnAction(e -> clearWaypoints());

        waypointListUI.setPadding(new Insets(5));

        // SEARCH
        Label lblSearch = new Label("🔍 Pesquisa");
        pesquisaField = new TextField();
        pesquisaField.setPromptText("Pesquisar local...");
        Button btnPesquisar = new Button("Pesquisar");
        btnPesquisar.setOnAction(e -> handlePesquisar());

        Button btnAddSearchAsStop = new Button("Adicionar como Paragem");
        btnAddSearchAsStop.setOnAction(e -> addLastSearchAsWaypoint());

        setupAutocomplete();

        // MODE
        Label lblMode = new Label("🚗 Modo");
        modeBox.getItems().setAll(TransportMode.values());
        modeBox.setValue(TransportMode.CAR);

        Button btnCalcular = new Button("Calcular Rota");
        btnCalcular.setOnAction(e -> calculateRoute());

        // ROUTE SUMMARY
        Label lblResumo = new Label("📊 Resumo da Rota");

        // POI FILTER
        Label lblPOI = new Label("📍 Tipo de POI");
        poiFilterBox.getItems().addAll(
                "Nenhum", "Restaurant", "Cafe", "Fast Food", "Bar", "Toilets",
                "ATM", "Fuel", "Pharmacy", "Hospital", "Parking", "Bank",
                "Supermarket", "Bakery", "Mall", "Convenience Store",
                "Hotel", "Museum", "Attraction"
        );
        poiFilterBox.setValue("Nenhum");

        Button btnSearchPOIs = new Button("Pesquisar POIs");
        btnSearchPOIs.setOnAction(e -> handleSearchPois());

        // EXPORT
        Label lblExport = new Label("📤 Exportar");
        Button btnJson = new Button("Exportar JSON");
        btnJson.setOnAction(e -> handleExportJson());

        Button btnGpx = new Button("Exportar GPX");
        btnGpx.setOnAction(e -> handleExportGpx());

        // RESET
        Button btnReset = new Button("Limpar Tudo");
        btnReset.setOnAction(e -> resetAll());

        sidebar.getChildren().addAll(
                lblOrigem, origemField, btnSetOrigem,
                new Separator(),
                lblStops, waypointListUI, btnClearStops,
                new Separator(),
                lblSearch, pesquisaField, btnPesquisar, btnAddSearchAsStop,
                new Separator(),
                lblMode, modeBox, btnCalcular,
                new Separator(),
                lblResumo, routeSummaryLabel,
                new Separator(),
                lblPOI, poiFilterBox, btnSearchPOIs, poiSummaryLabel,
                new Separator(),
                lblExport, btnJson, btnGpx,
                new Separator(),
                btnReset
        );

        return sidebar;
    }

    // ============================================================
    // ✅ MAP & CLICK BEHAVIOR
    // ============================================================
    private void initMap() {
        mapView.initialize(Configuration.builder()
                .projection(Projection.WEB_MERCATOR)
                .showZoomControls(true)
                .build()
        );

        mapView.initializedProperty().addListener((obs, old, initialized) -> {
            if (initialized) {
                mapView.setCenter(new Coordinate(38.7223, -9.1393));
                mapView.setZoom(10);

                mapView.addEventHandler(MapViewEvent.MAP_CLICKED, event -> {
                    Coordinate c = event.getCoordinate();
                    if (originMarker == null) setOrigin(c);
                    else addWaypoint(c);
                });
            }
        });
    }

    // ============================================================
    // ✅ ORIGIN
    // ============================================================
    private void setOrigin(Coordinate c) {
        if (originMarker != null) mapView.removeMarker(originMarker);

        originMarker = Marker.createProvided(Marker.Provided.RED)
                .setPosition(c)
                .setVisible(true);

        mapView.addMarker(originMarker);
        routeSummaryLabel.setText("Origem definida.");
    }

    private void handleSetOrigem() {
        String input = origemField.getText().trim();
        if (input.isEmpty()) return;

        new Thread(() -> {
            Point result = service.getGeocodeFromLocationString(input);
            if (result == null) return;

            Platform.runLater(() -> {
                Coordinate c = new Coordinate(result.getLatitude(), result.getLongitude());
                setOrigin(c);
                origemField.setText(result.getName());
                mapView.setCenter(c);
                mapView.setZoom(14);
            });
        }).start();
    }

    // ============================================================
    // ✅ WAYPOINTS
    // ============================================================
    private void addWaypoint(Coordinate c) {
        Point p = new Point(c.getLatitude(), c.getLongitude(), null);

        waypointPoints.add(p);

        Marker m = Marker.createProvided(Marker.Provided.BLUE)
                .setPosition(c)
                .setVisible(true);

        waypointMarkers.add(m);
        mapView.addMarker(m);

        updateWaypointUI();
    }

    private void updateWaypointUI() {
        waypointListUI.getChildren().clear();

        for (int i = 0; i < waypointPoints.size(); i++) {
            int index = i;

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);

            Label label = new Label((i + 1) + " - Paragem");
            Button removeBtn = new Button("Remover");

            removeBtn.setOnAction(e -> {
                removeWaypoint(index);
                calculateRoute();
            });

            row.getChildren().addAll(label, removeBtn);
            waypointListUI.getChildren().add(row);
        }
    }

    private void removeWaypoint(int index) {
        mapView.removeMarker(waypointMarkers.get(index));
        waypointMarkers.remove(index);
        waypointPoints.remove(index);
        updateWaypointUI();
    }

    private void clearWaypoints() {
        waypointMarkers.forEach(mapView::removeMarker);
        waypointMarkers.clear();
        waypointPoints.clear();
        updateWaypointUI();
    }

    private void addLastSearchAsWaypoint() {
        if (lastSearchPoint == null) return;
        Coordinate c = new Coordinate(lastSearchPoint.getLatitude(), lastSearchPoint.getLongitude());
        addWaypoint(c);
    }

    // ============================================================
    // ✅ ROUTE
    // ============================================================
    private void calculateRoute() {
        if (originMarker == null) {
            routeSummaryLabel.setText("Defina uma origem.");
            return;
        }

        if (currentRouteLine != null) mapView.removeCoordinateLine(currentRouteLine);

        Point origin = new Point(originMarker.getPosition().getLatitude(),
                originMarker.getPosition().getLongitude(), null);

        Route route = service.getRouteWithWaypoints(origin, waypointPoints, modeBox.getValue());

        if (route == null || route.getRoutePoints().isEmpty()) {
            routeSummaryLabel.setText("Erro ao calcular rota.");
            return;
        }

        List<Coordinate> coords = route.getRoutePoints().stream()
                .map(p -> new Coordinate(p.getLatitude(), p.getLongitude()))
                .toList();

        currentRouteLine = new CoordinateLine(coords)
                .setColor(Color.BLUE)
                .setVisible(true);

        mapView.addCoordinateLine(currentRouteLine);
        mapView.setExtent(Extent.forCoordinates(coords));

        lastRoute = route;

        routeSummaryLabel.setText(
                String.format("Distância: %.2f km | Tempo: %d min | Paragens: %d",
                        route.getDistanceKm(),
                        route.getDurationSec() / 60,
                        waypointPoints.size())
        );
    }

    // ============================================================
    // ✅ SEARCH
    // ============================================================
    private void handlePesquisar() {
        String query = pesquisaField.getText().trim();
        if (query.isEmpty()) return;

        new Thread(() -> {
            Point result = service.getGeocodeFromLocationString(query);
            if (result == null) return;

            lastSearchPoint = result;

            Platform.runLater(() -> {
                Coordinate c = new Coordinate(result.getLatitude(), result.getLongitude());
                mapView.setCenter(c);
                mapView.setZoom(14);
            });
        }).start();
    }

    private void setupAutocomplete() {
        setupFieldAutocomplete(pesquisaField);
        setupFieldAutocomplete(origemField);
    }

    private void setupFieldAutocomplete(TextField field) {
        field.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.isBlank()) {
                suggestionsMenu.hide();
                return;
            }

            new Thread(() -> {
                try {
                    List<Point> results = service.searchLocations(newText);

                    Platform.runLater(() -> {
                        suggestionsMenu.getItems().clear();

                        for (Point p : results) {
                            MenuItem item = new MenuItem(p.getName());
                            item.setOnAction(e -> {
                                field.setText(p.getName());
                                lastSearchPoint = p;
                                suggestionsMenu.hide();
                            });
                            suggestionsMenu.getItems().add(item);
                        }

                        if (!results.isEmpty()) {
                            suggestionsMenu.show(field, Side.BOTTOM, 0, 0);
                        } else {
                            suggestionsMenu.hide();
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        });
    }

    // ============================================================
    // ✅ POIs
    // ============================================================
    private void handleSearchPois() {
        if (lastRoute == null) {
            poiSummaryLabel.setText("Calcula a rota primeiro.");
            return;
        }

        String selected = poiFilterBox.getValue();

        if (selected.equals("Nenhum")) {
            clearPOIsFromMap();
            poiSummaryLabel.setText("POIs: 0");
            return;
        }

        poiSummaryLabel.setText("A procurar POIs...");

        new Thread(() -> {
            List<POI> pois = service.getPOIsAlongRoute(lastRoute, selected);

            Platform.runLater(() -> {
                showPoisOnMap(pois);
                poiSummaryLabel.setText("POIs: " + pois.size());
            });
        }).start();
    }

    private void showPoisOnMap(List<POI> pois) {
        clearPOIsFromMap();

        for (POI poi : pois) {
            Coordinate coord = new Coordinate(
                    poi.getCoordinate().getLatitude(),
                    poi.getCoordinate().getLongitude()
            );

            Marker marker = Marker.createProvided(Marker.Provided.ORANGE)
                    .setPosition(coord)
                    .setVisible(true);

            poiMarkers.add(marker);
            mapView.addMarker(marker);
        }
    }

    private void clearPOIsFromMap() {
        for (Marker m : poiMarkers) mapView.removeMarker(m);
        poiMarkers.clear();
    }

    // ============================================================
    // ✅ EXPORT
    // ============================================================
    private void handleExportJson() {
        if (lastRoute == null) return;

        try {
            RouteExporter.exportToJson(lastRoute, "route.json");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handleExportGpx() {
        if (lastRoute == null) return;

        try {
            RouteExporter.exportToGPX(lastRoute, "route.gpx");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ============================================================
    // ✅ RESET
    // ============================================================
    private void resetAll() {
        if (originMarker != null) mapView.removeMarker(originMarker);
        originMarker = null;

        clearWaypoints();
        clearPOIsFromMap();

        if (currentRouteLine != null) mapView.removeCoordinateLine(currentRouteLine);
        currentRouteLine = null;

        origemField.setText("");
        pesquisaField.setText("");
        routeSummaryLabel.setText("Defina origem e pontos de paragem...");
        poiSummaryLabel.setText("POIs: 0");

        mapView.setCenter(new Coordinate(38.7223, -9.1393));
        mapView.setZoom(10);
    }
}
