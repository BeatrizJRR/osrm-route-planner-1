package com.myapp.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.myapp.model.Point;
import com.myapp.model.Route;
import com.myapp.model.TransportMode;
import com.myapp.service.Service;
import com.sothawo.mapjfx.*;
import com.sothawo.mapjfx.event.MapViewEvent;

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

import java.util.List;

import javafx.scene.text.Text;

public class MapViewer extends Application {

    private final MapView mapView = new MapView();
    private Marker originMarker = null;
    private Marker destinationMarker = null;
    private Marker searchMarker = null;
    private CoordinateLine currentRouteLine = null;
    private Point lastSearchPoint = null;

    private final Service service = new Service();

    private final Label routeInfoLabel = new Label("Defina origem e destino...");
    private final ComboBox<TransportMode> modeBox = new ComboBox<>();

    private TextField origemField;
    private TextField destinoField;
    private TextField pesquisaField;
    private ContextMenu suggestionsMenu = new ContextMenu();

    @Override
    public void start(Stage stage) {
        stage.setTitle("Planeador de Rotas");

        String css = getClass().getResource("/mapstyle.css").toExternalForm();

        // --- SIDEBAR ---
        VBox sidebar = new VBox(15);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(20));

        origemField = new TextField();
        destinoField = new TextField();
        pesquisaField = new TextField();

        origemField.setPromptText("Origem");
        destinoField.setPromptText("Destino");

        Button btnOrigem = new Button("Definir Origem");
        Button btnDestino = new Button("Definir Destino");
        Button btnReset = new Button("Nova Pesquisa");

        pesquisaField.setPromptText("Pesquisar no mapa...");
        Button btnPesquisar = new Button("Pesquisar");

        setupAutocomplete();

        modeBox.getItems().setAll(TransportMode.values());
        modeBox.setValue(TransportMode.CAR);

        Button btnCalcular = new Button("Calcular Rota");

        sidebar.getChildren().addAll(
                new Label("🧭 Planeador"),
                origemField, btnOrigem,
                destinoField, btnDestino,
                new Separator(),
                new Label("🔍 Pesquisa"),
                pesquisaField, btnPesquisar,
                new Separator(),
                new Label("🚗 Modo de Transporte"),
                modeBox,
                btnCalcular,
                new Separator(),
                btnReset,
                routeInfoLabel);

        // --- MAPA ---
        StackPane mapContainer = new StackPane();
        mapContainer.getStyleClass().add("map-container");

        // Configura o tamanho máximo do MapView para ocupar todo o espaço do StackPane
        mapView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        StackPane.setAlignment(mapView, Pos.CENTER);
        StackPane.setMargin(mapView, new Insets(0));
        // StackPane não suporta setHgrow/setVgrow, então essas linhas foram removidas

        mapContainer.getChildren().add(mapView);

        // --- EVENTOS ---
        btnOrigem.setOnAction(e -> handleSetOrigem());
        btnDestino.setOnAction(e -> handleSetDestino());
        btnPesquisar.setOnAction(e -> handlePesquisar());
        btnCalcular.setOnAction(e -> calculateRoute(modeBox.getValue()));
        btnReset.setOnAction(e -> resetMap());

        // --- INIT MAP ---
        mapView.initialize(Configuration.builder()
                .projection(Projection.WEB_MERCATOR)
                .showZoomControls(true)
                .build());

        mapView.initializedProperty().addListener((obs, oldVal, in) -> {
            if (in) {
                Platform.runLater(() -> {
                    mapView.setCenter(new Coordinate(38.7223, -9.1393));
                    mapView.setZoom(10);
                });

                mapView.addEventHandler(MapViewEvent.MAP_CLICKED, event -> {
                    Coordinate c = event.getCoordinate();
                    if (originMarker == null)
                        setOrigin(c);
                    else if (destinationMarker == null)
                        setDestination(c);
                    else
                        routeInfoLabel.setText("Origem e destino já definidos.");
                });
            }
        });

        // --- ROOT LAYOUT ---
        BorderPane root = new BorderPane();
        root.setLeft(sidebar);
        root.setCenter(mapContainer);

        Scene scene = new Scene(root, 1300, 750);
        scene.getStylesheets().add(css);
        
        stage.setScene(scene);
        stage.show();
    }

    // --- MARKERS ---
    private void setOrigin(Coordinate c) {
        if (originMarker != null)
            mapView.removeMarker(originMarker);
        originMarker = Marker.createProvided(Marker.Provided.RED).setPosition(c).setVisible(true);
        mapView.addMarker(originMarker);
        routeInfoLabel.setText("Origem definida.");
    }

    private void setDestination(Coordinate c) {
        if (destinationMarker != null)
            mapView.removeMarker(destinationMarker);
        destinationMarker = Marker.createProvided(Marker.Provided.BLUE).setPosition(c).setVisible(true);
        mapView.addMarker(destinationMarker);
        routeInfoLabel.setText("Destino definido.");
    }

    private void setSearchMarker(Coordinate c) {
        if (searchMarker != null)
            mapView.removeMarker(searchMarker);
        searchMarker = Marker.createProvided(Marker.Provided.ORANGE).setPosition(c).setVisible(true);
        mapView.addMarker(searchMarker);
    }

    // --- CALCULAR ROTA (com fix extents) ---
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

        Platform.runLater(() -> {
            mapView.addCoordinateLine(currentRouteLine);

            // FIX DO EXTENT
            Platform.runLater(() -> {
                mapView.setExtent(Extent.forCoordinates(coords));
            });
        });

        routeInfoLabel.setText(String.format(
                "Distância: %.2f km | Tempo: %d min | Modo: %s",
                route.getDistanceKm(),
                route.getDurationSec() / 60,
                mode));
    }

    private void resetMap() {
        if (originMarker != null)
            mapView.removeMarker(originMarker);
        if (destinationMarker != null)
            mapView.removeMarker(destinationMarker);
        if (searchMarker != null)
            mapView.removeMarker(searchMarker);
        if (currentRouteLine != null)
            mapView.removeCoordinateLine(currentRouteLine);

        originMarker = null;
        destinationMarker = null;
        searchMarker = null;
        currentRouteLine = null;

        origemField.setText("");
        destinoField.setText("");
        pesquisaField.setText("");

        mapView.setCenter(new Coordinate(38.7223, -9.1393));
        mapView.setZoom(10);

        routeInfoLabel.setText("Mapa limpo.");
    }

    // Trata o botão "Definir Origem"
    private void handleSetOrigem() {
        String input = origemField.getText().trim();

        // 1) Se o utilizador escreveu algo no campo, tenta geocodificar
        if (!input.isEmpty()) {
            new Thread(() -> {
                Point result = service.getGeocodeFromLocationString(input);

                if (result == null) {
                    Platform.runLater(() ->
                            routeInfoLabel.setText("Origem não encontrada: " + input));
                    return;
                }

                Coordinate coord = new Coordinate(result.getLatitude(), result.getLongitude());

                Platform.runLater(() -> {
                    setOrigin(coord);
                    origemField.setText(result.getName());
                    mapView.setCenter(coord);
                    mapView.setZoom(15);
                });
            }).start();

            return; // já tratámos este cenário
        }

        // 2) Senão usa o último ponto pesquisado
        if (lastSearchPoint == null) {
            routeInfoLabel.setText("Escreve um local ou faz uma pesquisa.");
            return;
        }

        Coordinate coord = new Coordinate(
                lastSearchPoint.getLatitude(),
                lastSearchPoint.getLongitude()
        );

        setOrigin(coord);
        origemField.setText(lastSearchPoint.getName());
    }


    // Trata o botão "Definir Destino"
    private void handleSetDestino() {
        String input = destinoField.getText().trim();

        // 1) Se o utilizador escreveu algo no campo, tenta geocodificar
        if (!input.isEmpty()) {
            new Thread(() -> {
                Point result = service.getGeocodeFromLocationString(input);

                if (result == null) {
                    Platform.runLater(() ->
                            routeInfoLabel.setText("Destino não encontrado: " + input));
                    return;
                }

                Coordinate coord = new Coordinate(result.getLatitude(), result.getLongitude());

                Platform.runLater(() -> {
                    setDestination(coord);
                    destinoField.setText(result.getName());
                    mapView.setCenter(coord);
                    mapView.setZoom(15);
                });
            }).start();

            return;
        }

        // 2) Senão usa o último ponto pesquisado
        if (lastSearchPoint == null) {
            routeInfoLabel.setText("Escreve um local ou faz uma pesquisa.");
            return;
        }

        Coordinate coord = new Coordinate(
                lastSearchPoint.getLatitude(),
                lastSearchPoint.getLongitude()
        );

        setDestination(coord);
        destinoField.setText(lastSearchPoint.getName());
    }


    // Trata o botão "Pesquisar"
    private void handlePesquisar() {
        String query = pesquisaField.getText().trim();
        if (query.isEmpty()) {
            routeInfoLabel.setText("Escreve um local a pesquisar.");
            return;
        }

        // Faz a chamada à API numa thread separada para não bloquear a UI
        new Thread(() -> {
            Point result = service.getGeocodeFromLocationString(query);

            if (result == null) {
                Platform.runLater(() -> routeInfoLabel.setText("Local não encontrado."));
                return;
            }

            lastSearchPoint = result; // guardar para usar nos botões origem/destino
            Coordinate coord = new Coordinate(result.getLatitude(), result.getLongitude());

            Platform.runLater(() -> {
                setSearchMarker(coord);
                mapView.setCenter(coord);
                mapView.setZoom(15);

                routeInfoLabel.setText("Encontrado: " + result.getName());
            });
        }).start();
    }

    private void setupAutocomplete() {
        // Aplica autocomplete a todos os campos que precisam
        setupFieldAutocomplete(pesquisaField);
        setupFieldAutocomplete(origemField);
        setupFieldAutocomplete(destinoField);
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
                                field.setText(p.getName());  // Atualiza o campo correto
                                lastSearchPoint = p;
                                suggestionsMenu.hide();
                            });
                            suggestionsMenu.getItems().add(item);
                        }

                        if (!results.isEmpty()) {
                            suggestionsMenu.show(field, Side.BOTTOM, 0, 0); // Mostrar sobre o campo correto
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


    


}
