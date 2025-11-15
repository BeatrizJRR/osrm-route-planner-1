package com.myapp.ui;

import com.myapp.model.Point;
import com.myapp.model.Route;
import com.myapp.model.TransportMode;
import com.myapp.service.Service;
import com.sothawo.mapjfx.*;
import com.sothawo.mapjfx.event.MapViewEvent;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.List;

public class MapViewer extends Application {

    private final MapView mapView = new MapView();
    private Marker originMarker = null;
    private Marker destinationMarker = null;
    private Marker searchMarker = null;
    private CoordinateLine currentRouteLine = null;

    private final Service service = new Service();

    private final Label routeInfoLabel = new Label("Defina origem e destino...");
    private final ComboBox<TransportMode> modeBox = new ComboBox<>();

    @Override
    public void start(Stage stage) {
        stage.setTitle("Mapa OSM - Planeador de Rotas");

        // --- UI FIELDS ---
        TextField origemField = new TextField();
        origemField.setPromptText("Origem (ex: Lisboa)");

        TextField destinoField = new TextField();
        destinoField.setPromptText("Destino (ex: Porto)");

        Button btnOrigem = new Button("Definir Origem");
        Button btnDestino = new Button("Definir Destino");
        Button btnReset = new Button("Reset");

        HBox bar1 = new HBox(10, new Label("Origem:"), origemField, btnOrigem,
                new Label("Destino:"), destinoField, btnDestino, btnReset);
        bar1.setPadding(new Insets(8));

        // Search bar
        TextField pesquisaField = new TextField();
        pesquisaField.setPromptText("Pesquisar no mapa...");
        Button btnPesquisar = new Button("Pesquisar");

        HBox bar2 = new HBox(10, new Label("Pesquisar:"), pesquisaField, btnPesquisar);
        bar2.setPadding(new Insets(0, 8, 8, 8));

        // Mode + Calculate button
        modeBox.getItems().setAll(TransportMode.values());
        modeBox.setValue(TransportMode.CAR);

        Button btnCalcular = new Button("Calcular Rota");

        HBox bar3 = new HBox(10, new Label("Modo:"), modeBox, btnCalcular);
        bar3.setPadding(new Insets(0, 8, 8, 8));

        VBox top = new VBox(bar1, bar2, bar3);

        // --- Event handlers ---
        btnOrigem.setOnAction(e -> {
            //Point p = service.searchLocation(origemField.getText());
            //if (p == null) { routeInfoLabel.setText("Origem não encontrada."); return; }
            //Coordinate c = new Coordinate(p.getLatitude(), p.getLongitude());
            //setOrigin(c);
            //mapView.setCenter(c);
        });

        btnDestino.setOnAction(e -> {
            //Point p = service.searchLocation(destinoField.getText());
            //if (p == null) { routeInfoLabel.setText("Destino não encontrado."); return; }
            //Coordinate c = new Coordinate(p.getLatitude(), p.getLongitude());
            //setDestination(c);
            //mapView.setCenter(c);
        });

        btnPesquisar.setOnAction(e -> {
            //Point p = service.searchLocation(pesquisaField.getText());
            //if (p == null) { routeInfoLabel.setText("Pesquisa sem resultados."); return; }
            //Coordinate c = new Coordinate(p.getLatitude(), p.getLongitude());
            //setSearchMarker(c);
            //mapView.setCenter(c);
        });

        btnCalcular.setOnAction(e -> calculateRoute(modeBox.getValue()));
        btnReset.setOnAction(e -> resetMap());

        // Initialize MapJFX
        mapView.initialize(Configuration.builder()
                .projection(Projection.WEB_MERCATOR)
                .showZoomControls(true)
                .build());

        mapView.initializedProperty().addListener((obs, oldVal, in) -> {
            if (in) {
                mapView.setCenter(new Coordinate(38.7223, -9.1393));
                mapView.setZoom(10);

                mapView.addEventHandler(MapViewEvent.MAP_CLICKED, event -> {
                    Coordinate c = event.getCoordinate();
                    if (originMarker == null) {
                        setOrigin(c);
                    } else if (destinationMarker == null) {
                        setDestination(c);
                    } else {
                        routeInfoLabel.setText("Origem e destino já definidos. Use Reset.");
                    }
                });
            }
        });

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(mapView);
        root.setBottom(routeInfoLabel);
        BorderPane.setMargin(routeInfoLabel, new Insets(8));

        Scene scene = new Scene(root, 1100, 650);
        stage.setScene(scene);
        stage.show();
    }

    private void setOrigin(Coordinate c) {
        if (originMarker != null) mapView.removeMarker(originMarker);
        originMarker = Marker.createProvided(Marker.Provided.RED).setPosition(c).setVisible(true);
        mapView.addMarker(originMarker);
        routeInfoLabel.setText("Origem definida.");
    }

    private void setDestination(Coordinate c) {
        if (destinationMarker != null) mapView.removeMarker(destinationMarker);
        destinationMarker = Marker.createProvided(Marker.Provided.BLUE).setPosition(c).setVisible(true);
        mapView.addMarker(destinationMarker);
        routeInfoLabel.setText("Destino definido.");
    }

    private void setSearchMarker(Coordinate c) {
        if (searchMarker != null) mapView.removeMarker(searchMarker);
        searchMarker = Marker.createProvided(Marker.Provided.ORANGE).setPosition(c).setVisible(true);
        mapView.addMarker(searchMarker);
    }

    private void calculateRoute(TransportMode mode) {
        if (originMarker == null || destinationMarker == null) {
            routeInfoLabel.setText("Defina origem e destino primeiro.");
            return;
        }

        if (currentRouteLine != null) {
            mapView.removeCoordinateLine(currentRouteLine);
            currentRouteLine = null;
        }

        Point origin = new Point(originMarker.getPosition().getLatitude(),
                originMarker.getPosition().getLongitude(), null);
        Point dest = new Point(destinationMarker.getPosition().getLatitude(),
                destinationMarker.getPosition().getLongitude(), null);

        Route route = service.getRoute(origin, dest, mode);
        if (route == null || route.getRoutePoints().isEmpty()) {
            routeInfoLabel.setText("Erro ao calcular rota.");
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

        routeInfoLabel.setText(String.format(
                "Distância: %.2f km | Tempo: %d min | Modo: %s",
                route.getDistanceKm(),
                route.getDurationSec() / 60,
                mode
        ));
    }

    private void resetMap() {
        if (originMarker != null) mapView.removeMarker(originMarker);
        if (destinationMarker != null) mapView.removeMarker(destinationMarker);
        if (searchMarker != null) mapView.removeMarker(searchMarker);
        if (currentRouteLine != null) mapView.removeCoordinateLine(currentRouteLine);

        originMarker = null;
        destinationMarker = null;
        searchMarker = null;
        currentRouteLine = null;

        mapView.setCenter(new Coordinate(38.7223, -9.1393));
        mapView.setZoom(10);

        routeInfoLabel.setText("Mapa limpo.");
    }
}
