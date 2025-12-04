package com.myapp.ui;

import com.myapp.model.POI;
import com.myapp.model.Point;
import com.myapp.model.Route;
import com.myapp.model.TransportMode;
import com.myapp.service.Service;
import com.myapp.utils.RouteExporter;
import com.sothawo.mapjfx.*;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import com.sothawo.mapjfx.event.MapViewEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface gráfica (View) principal da aplicação de planeamento de rotas.
 *
 * Papel na arquitetura MVC:
 * - View (UI): apresenta mapa interativo, formulários e controlos ao utilizador.
 * - Consome Service (Controller) para obter rotas, POIs, geocodificação e elevação.
 * - Renderiza modelos (Route, Point, POI) no mapa; não contém lógica de negócio.
 *
 * Utiliza JavaFX e mapjfx para renderização de mapas e marcadores.
 */
public class MapViewer extends Application {

    // UI Layout constants
    private static final int SIDEBAR_WIDTH = 360;
    private static final int SIDEBAR_SPACING = 20;
    private static final int SIDEBAR_PADDING = 20;
    private static final int SIDEBAR_TRANSLATE_OFFSET = 20;
    private static final int SIDEBAR_SCROLL_PADDING = 10;
    private static final int WINDOW_WIDTH = 1350;
    private static final int WINDOW_HEIGHT = 800;
    private static final int POI_LIST_PREF_HEIGHT = 150;
    private static final int POI_LIST_MAX_HEIGHT = 200;
    private static final int WAYPOINT_ROW_SPACING = 10;
    private static final int ELEVATION_DIALOG_SPACING = 15;

    // Map constants
    private static final double DEFAULT_MAP_CENTER_LAT = 38.7223;
    private static final double DEFAULT_MAP_CENTER_LON = -9.1393;
    private static final int DEFAULT_MAP_ZOOM = 10;
    private static final int LOCATION_ZOOM = 14;
    private static final int POI_ZOOM = 15;

    // Time conversion
    private static final long SECONDS_PER_MINUTE = 60L;
    
    // Autocomplete debounce
    private static final long AUTOCOMPLETE_DEBOUNCE_MS = 500L;

    // --- MAP ELEMENTS ---
    private final MapView mapView = new MapView();
    private Marker originMarker = null;
    private CoordinateLine currentRouteLine = null;

    private final List<Point> waypointPoints = new ArrayList<>();
    private final List<Marker> waypointMarkers = new ArrayList<>();
    private final List<Marker> poiMarkers = new ArrayList<>();
    private final List<POI> currentPOIs = new ArrayList<>();

    // --- UI ---
    private TextField origemField;
    private TextField pesquisaField;
    private VBox waypointListUI = new VBox(8);
    private VBox poiListUI = new VBox(5);

    private Label routeSummaryLabel = new Label("Defina origem e pontos de paragem...");
    private Label poiSummaryLabel = new Label("Pontos de Interesse: 0");

    private ComboBox<String> poiFilterBox = new ComboBox<>();

    private Point lastSearchPoint = null;
    private Route lastRoute = null;

    private TransportMode selectedMode = TransportMode.CAR;

    private final Service service = new Service();
    private ContextMenu suggestionsMenu = new ContextMenu();
    
    // Debounce timer for autocomplete
    private javafx.animation.Timeline debounceTimer;

    private final com.myapp.utils.HistoryManager historyManager = new com.myapp.utils.HistoryManager();

    @Override
    public void start(Stage stage) {
        stage.setTitle("Planeador de Rotas");

        // --- SIDEBAR ---
        VBox sidebar = buildSidebarUI();
        sidebar.getStyleClass().add("sidebar");

        // Wrap sidebar in ScrollPane
        ScrollPane sidebarScroll = new ScrollPane(sidebar);
        sidebarScroll.getStyleClass().add("sidebar-scroll");
        sidebarScroll.setFitToWidth(true);
        sidebarScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sidebarScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sidebarScroll.setMaxWidth(SIDEBAR_WIDTH);
        sidebarScroll.setPadding(new Insets(SIDEBAR_SCROLL_PADDING));
        sidebarScroll.setTranslateX(SIDEBAR_TRANSLATE_OFFSET);
        sidebarScroll.setTranslateY(SIDEBAR_TRANSLATE_OFFSET);
        sidebarScroll.setPickOnBounds(false); // Allow clicks through transparent areas

        // --- MAP CONTAINER ---
        StackPane mapWrapper = new StackPane(mapView);
        mapWrapper.setStyle("-fx-background-color: #ddd;");

        // --- INIT MAP ---
        initMap();

        // --- ROOT LAYOUT (Floating Sidebar) ---
        StackPane root = new StackPane();
        StackPane.setAlignment(sidebarScroll, Pos.TOP_LEFT);
        root.getChildren().addAll(mapWrapper, sidebarScroll);

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/mapstyle.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    
    private VBox buildSidebarUI() {
        VBox sidebar = new VBox(SIDEBAR_SPACING);
        sidebar.setPadding(new Insets(SIDEBAR_PADDING));
        sidebar.setPrefWidth(SIDEBAR_WIDTH);

        // ORIGIN
        Label lblOrigem = new Label("📍 Origem");
        lblOrigem.setUnderline(true);
        lblOrigem.setStyle("-fx-font-weight: bold");
        origemField = new TextField();
        origemField.setPromptText("Introduzir localização...");
        Button btnSetOrigem = new Button("📍 Definir Origem");
        btnSetOrigem.getStyleClass().add("btn-secondary");
        btnSetOrigem.setMaxWidth(Double.MAX_VALUE);
        btnSetOrigem.setOnAction(e -> handleSetOrigem());

        // WAYPOINT LIST
        Label lblStops = new Label("🛑 Paragens");
        lblStops.setStyle("-fx-font-weight: bold");
        lblStops.setUnderline(true);
        Button btnClearStops = new Button("🔄 Limpar Paragens");
        btnClearStops.getStyleClass().add("btn-danger-outline");
        btnClearStops.setMaxWidth(Double.MAX_VALUE);
        btnClearStops.setOnAction(e -> clearWaypoints());

        waypointListUI.setPadding(new Insets(5));

        // SEARCH
        Label lblSearch = new Label("🔍 Pesquisa");
        pesquisaField = new TextField();
        pesquisaField.setPromptText("Pesquisar local...");
        Button btnPesquisar = new Button("🔍 Pesquisar");
        btnPesquisar.getStyleClass().add("btn-secondary");
        btnPesquisar.setMaxWidth(Double.MAX_VALUE);
        btnPesquisar.setOnAction(e -> handlePesquisar());

        Button btnAddSearchAsStop = new Button("➕ Adicionar como Paragem");
        btnAddSearchAsStop.getStyleClass().add("btn-secondary-outline");
        btnAddSearchAsStop.setMaxWidth(Double.MAX_VALUE);
        btnAddSearchAsStop.setOnAction(e -> addLastSearchAsWaypoint());

        setupAutocomplete();

        // MODE
        Label lblMode = new Label("Modo de Transporte");
        lblMode.setUnderline(true);
        lblMode.setStyle("-fx-font-weight: bold");

        ToggleGroup modeGroup = new ToggleGroup();

        ToggleButton btnCar = new ToggleButton("🚗 Carro");
        btnCar.setToggleGroup(modeGroup);
        btnCar.setSelected(true);
        btnCar.setMaxWidth(Double.MAX_VALUE);
        btnCar.setUserData(TransportMode.CAR);

        ToggleButton btnBike = new ToggleButton("🚲 Bicicleta");
        btnBike.setToggleGroup(modeGroup);
        btnBike.setMaxWidth(Double.MAX_VALUE);
        btnBike.setUserData(TransportMode.BIKE);

        ToggleButton btnFoot = new ToggleButton("🚶 A Pé");
        btnFoot.setToggleGroup(modeGroup);
        btnFoot.setMaxWidth(Double.MAX_VALUE);
        btnFoot.setUserData(TransportMode.FOOT);

        modeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle != null) {
                selectedMode = (TransportMode) newToggle.getUserData();
            }
        });

        HBox modeButtons = new HBox(8);
        HBox.setHgrow(btnCar, Priority.ALWAYS);
        HBox.setHgrow(btnBike, Priority.ALWAYS);
        HBox.setHgrow(btnFoot, Priority.ALWAYS);
        modeButtons.getChildren().addAll(btnCar, btnBike, btnFoot);

        Button btnCalcular = new Button("🚀 Calcular Rota");
        btnCalcular.getStyleClass().add("btn-primary");
        btnCalcular.setMaxWidth(Double.MAX_VALUE);
        btnCalcular.setOnAction(e -> calculateRoute());

        // ROUTE SUMMARY
        Label lblResumo = new Label("📊 Resumo da Rota");
        lblResumo.setUnderline(true);
        lblResumo.setStyle("-fx-font-weight: bold");

        // POI FILTER
        Label lblPOI = new Label("📍 Tipo de Ponto de Interesse");
        lblPOI.setUnderline(true);
        lblPOI.setStyle("-fx-font-weight: bold");
        poiFilterBox.getItems().addAll(
                "Nenhum", "Restaurante", "Café", "Fast Food", "Bar", "Sanitários",
                "ATM", "Combustível", "Farmácia", "Hospital", "Estacionamento", "Banco",
                "Supermercado", "Padaria", "Shopping", "Loja de Conveniência",
                "Hotel", "Museu", "Atração Turística");
        poiFilterBox.setValue("Nenhum");

        Button btnSearchPOIs = new Button("🔎 Pesquisar Pontos de Interesse");
        btnSearchPOIs.getStyleClass().add("btn-secondary-outline");
        btnSearchPOIs.setMaxWidth(Double.MAX_VALUE);
        btnSearchPOIs.setOnAction(e -> handleSearchPois());

        // POI LIST
        ScrollPane poiListScroll = new ScrollPane(poiListUI);
        poiListScroll.setFitToWidth(true);
        poiListScroll.setPrefHeight(POI_LIST_PREF_HEIGHT);
        poiListScroll.setMaxHeight(POI_LIST_MAX_HEIGHT);
        poiListUI.setPadding(new Insets(5));
        poiListUI.getStyleClass().add("poi-list-container");

        // ELEVATION
        Label lblElevation = new Label("📈 Perfil Altimétrico");
        lblElevation.setUnderline(true);
        lblElevation.setStyle("-fx-font-weight: bold");
        Button btnElevation = new Button("📊 Mostrar Elevação");
        btnElevation.getStyleClass().add("btn-info-outline");
        btnElevation.setMaxWidth(Double.MAX_VALUE);
        btnElevation.setOnAction(e -> handleShowElevation());

        // HISTORY
        Button btnHistory = new Button("📜 Ver Histórico");
        btnHistory.getStyleClass().add("btn-info-outline"); // Ou o teu estilo preferido
        btnHistory.setMaxWidth(Double.MAX_VALUE);
        btnHistory.setOnAction(e -> showHistoryDialog());

        // EXPORT
        Label lblExport = new Label("📤 Exportar");
        lblExport.setUnderline(true);
        lblExport.setStyle("-fx-font-weight: bold");
        HBox exportButtons = new HBox(8);
        Button btnJson = new Button("📄 JSON");
        btnJson.getStyleClass().add("btn-export");
        btnJson.setMaxWidth(Double.MAX_VALUE);
        btnJson.setOnAction(e -> handleExportJson());

        Button btnGpx = new Button("🗺️ GPX");
        btnGpx.getStyleClass().add("btn-export");
        btnGpx.setMaxWidth(Double.MAX_VALUE);
        btnGpx.setOnAction(e -> handleExportGpx());

        HBox.setHgrow(btnJson, Priority.ALWAYS);
        HBox.setHgrow(btnGpx, Priority.ALWAYS);
        exportButtons.getChildren().addAll(btnJson, btnGpx);

        // RESET
        Button btnReset = new Button("🔄 Limpar Tudo");
        btnReset.getStyleClass().add("btn-danger");
        btnReset.setMaxWidth(Double.MAX_VALUE);
        btnReset.setOnAction(e -> resetAll());

        sidebar.getChildren().addAll(
                lblOrigem, origemField, btnSetOrigem,
                new Separator(),
                lblStops, waypointListUI, btnClearStops,
                new Separator(),
                lblSearch, pesquisaField, btnPesquisar, btnAddSearchAsStop,
                new Separator(),
                lblMode, modeButtons, btnCalcular,
                new Separator(),
                lblResumo, routeSummaryLabel,
                new Separator(),
                lblPOI, poiFilterBox, btnSearchPOIs, poiSummaryLabel,
                poiListScroll,
                new Separator(),
                lblElevation, btnElevation,
                new Separator(),
                btnHistory,
                new Separator(),
                lblExport, exportButtons,
                new Separator(),
                btnReset);

        return sidebar;
    }

    /**
     * Inicializa o mapa e define eventos iniciais.
     */
    private void initMap() {
        mapView.initialize(Configuration.builder()
                .projection(Projection.WEB_MERCATOR)
                .showZoomControls(true)
                .build());

        mapView.initializedProperty().addListener((obs, old, initialized) -> {
            if (initialized) {
                mapView.setCenter(new Coordinate(DEFAULT_MAP_CENTER_LAT, DEFAULT_MAP_CENTER_LON));
                mapView.setZoom(DEFAULT_MAP_ZOOM);

                mapView.addEventHandler(MapViewEvent.MAP_CLICKED, event -> {
                    Coordinate c = event.getCoordinate();
                    if (originMarker == null)
                        setOrigin(c);
                    else
                        addWaypoint(c, null);
                });
            }
        });
    }

    /**
     * Define a origem no mapa e atualiza o marcador.
     * @param c
     */
    private void setOrigin(Coordinate c) {
        if (originMarker != null)
            mapView.removeMarker(originMarker);

        originMarker = Marker.createProvided(Marker.Provided.RED)
                .setPosition(c)
                .setVisible(true);

        mapView.addMarker(originMarker);
        routeSummaryLabel.setText("Origem definida.");
    }

    private void handleSetOrigem() {
        String input = origemField.getText().trim();
        if (input.isEmpty())
            return;

        new Thread(() -> {
            Point result = service.getGeocodeFromLocationString(input);
            if (result == null)
                return;

            Platform.runLater(() -> {
                Coordinate c = new Coordinate(result.getLatitude(), result.getLongitude());
                setOrigin(c);
                origemField.setText(result.getName());
                mapView.setCenter(c);
                mapView.setZoom(LOCATION_ZOOM);
            });
        }).start();
    }

    /**
     * Adiciona um ponto de paragem (waypoint) no mapa.
     * @param c
     */
    private void addWaypoint(Coordinate c, String name) {
        Point p = new Point(c.getLatitude(), c.getLongitude(), name);

        waypointPoints.add(p);

        Marker m = Marker.createProvided(Marker.Provided.BLUE)
                .setPosition(c)
                .setVisible(true);

        waypointMarkers.add(m);
        mapView.addMarker(m);

        updateWaypointUI();
    }

    /**
     * Atualiza a lista de paragens (waypoints) na UI.
     */
    private void updateWaypointUI() {
        waypointListUI.getChildren().clear();

        for (int i = 0; i < waypointPoints.size(); i++) {
            int index = i;

            HBox row = new HBox(WAYPOINT_ROW_SPACING);
            row.setAlignment(Pos.CENTER_LEFT);

            Label label = new Label((i + 1) + " - Paragem");
            Button removeBtn = new Button("✖ Remover");
            removeBtn.getStyleClass().add("btn-remove-small");

            removeBtn.setOnAction(e -> {
                removeWaypoint(index);
                calculateRoute();
            });

            row.getChildren().addAll(label, removeBtn);
            waypointListUI.getChildren().add(row);
        }
    }

    /**
     * Remove um ponto de paragem (waypoint) pelo índice.
     * @param index
     */
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
        if (lastSearchPoint == null)
            return;
        Coordinate c = new Coordinate(lastSearchPoint.getLatitude(), lastSearchPoint.getLongitude());
        addWaypoint(c, lastSearchPoint.getName());
    }

    /**
     * Calcula a rota com a origem e paragens definidas.
     */
    private void calculateRoute() {
        if (originMarker == null) {
            routeSummaryLabel.setText("Defina uma origem.");
            return;
        }

        if (currentRouteLine != null)
            mapView.removeCoordinateLine(currentRouteLine);
        
        String originName = origemField.getText();
        
        if (originName != null && originName.isBlank()) originName = null;

        Point origin = new Point(
            originMarker.getPosition().getLatitude(),
            originMarker.getPosition().getLongitude(), 
            originName
        );

        Route route = service.getRouteWithWaypoints(origin, waypointPoints, selectedMode);

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

        
        if (!waypointPoints.isEmpty()) {
        Point destination = waypointPoints.get(waypointPoints.size() - 1);
        List<Point> savedWaypoints = new ArrayList<>(waypointPoints);
        com.myapp.model.HistoryEntry entry = new com.myapp.model.HistoryEntry(
            origin, destination, savedWaypoints, selectedMode
        );
        
        historyManager.addEntry(entry);
        System.out.println("Rota adicionada ao histórico.");
        }

        routeSummaryLabel.setText(
                String.format("Distância: %.2f km | Tempo: %d min | Paragens: %d",
                        route.getDistanceKm(),
                        route.getDurationSec() / SECONDS_PER_MINUTE,
                        waypointPoints.size()));
    }

    /**
     * Pesquisa a localização introduzida no campo de pesquisa e centra o mapa.
     */
    private void handlePesquisar() {
        String query = pesquisaField.getText().trim();
        if (query.isEmpty())
            return;

        new Thread(() -> {
            Point result = service.getGeocodeFromLocationString(query);
            if (result == null)
                return;

            lastSearchPoint = result;

            Platform.runLater(() -> {
                Coordinate c = new Coordinate(result.getLatitude(), result.getLongitude());
                mapView.setCenter(c);
                mapView.setZoom(LOCATION_ZOOM);
            });
        }).start();
    }

    /**
     * Configura o sistema de autocompletar para os campos de texto.
     */
    private void setupAutocomplete() {
        setupFieldAutocomplete(pesquisaField);
        setupFieldAutocomplete(origemField);
    }
    
    /**
     * Configura o autocompletar para um campo de texto específico.
     * @param field
     */
    private void setupFieldAutocomplete(TextField field) {
        PauseTransition pause = new PauseTransition(javafx.util.Duration.millis(400));

        field.textProperty().addListener((obs, oldText, newText) -> {
            if (newText == null || newText.isBlank()) {
                suggestionsMenu.hide();
                return;
            }

            pause.setOnFinished(event -> {
                String queryAtRequestTime = field.getText(); // Pode ser null se mudou entretanto
                
                // Outra verificação de segurança
                if (queryAtRequestTime == null || queryAtRequestTime.trim().length() < 3) return;
                
                final String queryFinal = queryAtRequestTime.trim();

                new Thread(() -> {
                    try {
                        List<Point> results = service.searchLocations(queryFinal);

                        Platform.runLater(() -> {
                            // Verificar se o texto mudou ou ficou null entretanto
                            String currentText = field.getText();
                            if (currentText == null || !currentText.trim().equals(queryFinal)) {
                                return;
                            }

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
                                if(field.isFocused()) {
                                    suggestionsMenu.show(field, Side.BOTTOM, 0, 0);
                                }
                            } else {
                                suggestionsMenu.hide();
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            });

            pause.playFromStart();
        });

        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                suggestionsMenu.hide();
            }
        });
    }

    /**
     * Pesquisa Pontos de Interesse ao longo da rota atual.
     */
    private void handleSearchPois() {
        if (lastRoute == null) {
            poiSummaryLabel.setText("Calcula a rota primeiro.");
            return;
        }

        String selected = poiFilterBox.getValue();

        if (selected.equals("Nenhum")) {
            clearPOIsFromMap();
            poiSummaryLabel.setText("Pontos de Interesse: 0");
            return;
        }

        poiSummaryLabel.setText("A procurar Pontos de Interesse...");
        poiListUI.getChildren().clear();

        new Thread(() -> {
            List<POI> pois = service.getPOIsAlongRoute(lastRoute, selected);

            Platform.runLater(() -> {
                currentPOIs.clear();
                currentPOIs.addAll(pois);
                
                // Add POIs to route for export
                if (lastRoute != null) {
                    lastRoute.getPois().clear();
                    lastRoute.getPois().addAll(pois);
                }
                
                showPoisOnMap(pois);
                updatePOIList(pois);
                poiSummaryLabel.setText("Pontos de Interesse: " + pois.size());
            });
        }).start();
    }

    /**
     * Mostra os Pontos de Interesse no mapa.
     * @param pois
     */
    private void showPoisOnMap(List<POI> pois) {
        clearPOIsFromMap();

        for (int i = 0; i < pois.size(); i++) {
            POI poi = pois.get(i);
            Coordinate coord = new Coordinate(
                    poi.getCoordinate().getLatitude(),
                    poi.getCoordinate().getLongitude());

            Marker marker = Marker.createProvided(Marker.Provided.ORANGE)
                    .setPosition(coord)
                    .setVisible(true);

            poiMarkers.add(marker);
            mapView.addMarker(marker);

            // Adicionar label ao marker
            marker.attachLabel(new MapLabel(poi.getName() != null ? poi.getName() : "Ponto de Interesse #" + (i + 1))
                    .setCssClass("poi-label"));
        }
    }

    /**
     * Atualiza a lista de Pontos de Interesse na UI.
     * @param pois
     */
    private void updatePOIList(List<POI> pois) {
        poiListUI.getChildren().clear();

        if (pois.isEmpty()) {
            Label emptyLabel = new Label("Nenhum Ponto de Interesse encontrado");
            emptyLabel.getStyleClass().add("poi-empty-label");
            poiListUI.getChildren().add(emptyLabel);
            return;
        }

        for (int i = 0; i < pois.size(); i++) {
            POI poi = pois.get(i);

            String name = poi.getName() != null ? poi.getName() : "Sem nome";
            String category = poi.getCategory() != null ? poi.getCategory() : "Desconhecido";
            String address = String.format("%.4f, %.4f", 
                poi.getCoordinate().getLatitude(), 
                poi.getCoordinate().getLongitude());

            Label poiLabel = new Label("● " + name);
            poiLabel.getStyleClass().add("poi-label-text");

            Label categoryLabel = new Label("   " + category);
            categoryLabel.getStyleClass().add("poi-category-label");
            
            Label addressLabel = new Label("   " + address);
            addressLabel.getStyleClass().add("poi-category-label");
            addressLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #666;");

            VBox poiEntry = new VBox(2, poiLabel, categoryLabel, addressLabel);
            poiEntry.setPadding(new Insets(3, 5, 3, 5));
            poiEntry.getStyleClass().add("poi-entry");

            // Click para centrar no mapa
            poiEntry.setOnMouseClicked(e -> {
                Coordinate coord = new Coordinate(
                        poi.getCoordinate().getLatitude(),
                        poi.getCoordinate().getLongitude());
                mapView.setCenter(coord);
                mapView.setZoom(POI_ZOOM);
            });

            poiListUI.getChildren().add(poiEntry);
        }
    }

    /**
     * Remove todos os Pontos de Interesse do mapa.
     */
    private void clearPOIsFromMap() {
        for (Marker m : poiMarkers)
            mapView.removeMarker(m);
        poiMarkers.clear();
        currentPOIs.clear();
        poiListUI.getChildren().clear();
    }

    /**
     * Exporta a rota atual para ficheiro JSON.
     */
    private void handleExportJson() {
        if (lastRoute == null)
            return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Rota como JSON");
        fileChooser.setInitialFileName("route.json");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json")
        );
        
        java.io.File file = fileChooser.showSaveDialog(mapView.getScene().getWindow());
        if (file != null) {
            try {
                RouteExporter.exportToJson(lastRoute, file.getAbsolutePath());
                showAlert("Exportação bem-sucedida", "Rota guardada em: " + file.getAbsolutePath());
            } catch (Exception ex) {
                showAlert("Erro na exportação", "Não foi possível guardar o ficheiro: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    /**
     * Exporta a rota atual para ficheiro GPX.
     */
    private void handleExportGpx() {
        if (lastRoute == null)
            return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Rota como GPX");
        fileChooser.setInitialFileName("route.gpx");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("GPX files (*.gpx)", "*.gpx")
        );
        
        java.io.File file = fileChooser.showSaveDialog(mapView.getScene().getWindow());
        if (file != null) {
            try {
                RouteExporter.exportToGPX(lastRoute, file.getAbsolutePath());
                showAlert("Exportação bem-sucedida", "Rota guardada em: " + file.getAbsolutePath());
            } catch (Exception ex) {
                showAlert("Erro na exportação", "Não foi possível guardar o ficheiro: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    /**
     *  Mostra o perfil altimétrico da rota atual.
     */
    private void handleShowElevation() {
        if (lastRoute == null) {
            showAlert("Erro", "Calcule uma rota primeiro.");
            return;
        }

        Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Perfil Altimétrico");
        loadingAlert.setHeaderText("A obter dados de elevação...");
        loadingAlert.setContentText("Por favor aguarde...");
        loadingAlert.show();

        new Thread(() -> {
            var profile = service.getElevationProfile(lastRoute);

            Platform.runLater(() -> {
                loadingAlert.close();

                if (profile == null) {
                    showAlert("Erro", "Não foi possível obter dados de elevação.");
                    return;
                }

                showElevationChart(profile);
            });
        }).start();
    }

    /**
     * Mostra o gráfico do perfil altimétrico.
     * @param profile
     */
    private void showElevationChart(com.myapp.model.ElevationProfile profile) {
        Stage chartStage = new Stage();
        chartStage.setTitle("Perfil Altimétrico da Rota");

        VBox root = new VBox(ELEVATION_DIALOG_SPACING);
        root.setPadding(new Insets(20));

        // Estatísticas
        Label stats = new Label(String.format(
                "Elevação Máxima: %.0fm | Elevação Mínima: %.0fm | Subida Total: %.0fm | Descida Total: %.0fm",
                profile.getMaxElevation(), profile.getMinElevation(),
                profile.getTotalAscent(), profile.getTotalDescent()));
        stats.getStyleClass().add("elevation-stats-label");

        // Canvas para desenhar gráfico
        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(800, 400);
        var gc = canvas.getGraphicsContext2D();

        // Fundo
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.fillRect(0, 0, 800, 400);

        // Dados
        var elevations = profile.getElevations();
        var distances = profile.getDistances();

        if (elevations.isEmpty())
            return;

        // Margens
        double marginLeft = 60, marginRight = 20, marginTop = 30, marginBottom = 50;
        double chartWidth = 800 - marginLeft - marginRight;
        double chartHeight = 400 - marginTop - marginBottom;

        // Escala
        double maxDist = distances.get(distances.size() - 1);
        double maxElev = profile.getMaxElevation();
        double minElev = profile.getMinElevation();
        double elevRange = maxElev - minElev;
        if (elevRange < 10)
            elevRange = 10; // Evitar divisão por zero

        // Eixos
        gc.setStroke(javafx.scene.paint.Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeLine(marginLeft, marginTop, marginLeft, marginTop + chartHeight); // Y
        gc.strokeLine(marginLeft, marginTop + chartHeight, marginLeft + chartWidth, marginTop + chartHeight); // X

        // Labels eixos
        gc.setFill(javafx.scene.paint.Color.BLACK);
        gc.fillText("Elevação (m)", 5, marginTop + chartHeight / 2);
        gc.fillText("Distância (km)", marginLeft + chartWidth / 2 - 40, marginTop + chartHeight + 40);

        // Desenhar perfil
        gc.setStroke(javafx.scene.paint.Color.BLUE);
        gc.setLineWidth(2);

        for (int i = 0; i < elevations.size() - 1; i++) {
            double x1 = marginLeft + (distances.get(i) / maxDist) * chartWidth;
            double y1 = marginTop + chartHeight - ((elevations.get(i) - minElev) / elevRange) * chartHeight;
            double x2 = marginLeft + (distances.get(i + 1) / maxDist) * chartWidth;
            double y2 = marginTop + chartHeight - ((elevations.get(i + 1) - minElev) / elevRange) * chartHeight;

            gc.strokeLine(x1, y1, x2, y2);
        }

        // Marcadores no eixo X
        gc.setFill(javafx.scene.paint.Color.BLACK);
        for (int i = 0; i <= 5; i++) {
            double dist = (maxDist / 5) * i;
            double x = marginLeft + (dist / maxDist) * chartWidth;
            gc.fillText(String.format("%.1f", dist), x - 10, marginTop + chartHeight + 20);
        }

        // Marcadores no eixo Y
        for (int i = 0; i <= 5; i++) {
            double elev = minElev + (elevRange / 5) * i;
            double y = marginTop + chartHeight - (i / 5.0) * chartHeight;
            gc.fillText(String.format("%.0f", elev), marginLeft - 50, y + 5);
        }

        root.getChildren().addAll(stats, canvas);

        Scene scene = new Scene(root, 850, 500);
        chartStage.setScene(scene);
        chartStage.show();
    }

    /**
     * Mostra um alerta com título e mensagem.
     * @param title
     * @param message
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Reseta todos os dados e o mapa para o estado inicial.
     */
    private void resetAll() {
        if (originMarker != null)
            mapView.removeMarker(originMarker);
        originMarker = null;

        clearWaypoints();
        clearPOIsFromMap();

        if (currentRouteLine != null)
            mapView.removeCoordinateLine(currentRouteLine);
        currentRouteLine = null;

        origemField.setText("");
        pesquisaField.setText("");
        routeSummaryLabel.setText("Defina origem e pontos de paragem...");
        poiSummaryLabel.setText("Pontos de Interesse: 0");

        mapView.setCenter(new Coordinate(38.7223, -9.1393));
        mapView.setZoom(10);
    }

    private void showHistoryDialog() {
    Stage historyStage = new Stage();
    historyStage.setTitle("Histórico de Rotas");

    ListView<com.myapp.model.HistoryEntry> listView = new ListView<>();
    listView.getItems().addAll(historyManager.getHistory());

    // Botão para carregar a rota selecionada
    Button btnLoad = new Button("Carregar Rota");
    btnLoad.setDisable(true);
    btnLoad.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

    // Botão para limpar histórico
    Button btnClear = new Button("Limpar Histórico");
    btnClear.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
    
    listView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
        btnLoad.setDisable(newVal == null);
    });

    // Ação de carregar
    btnLoad.setOnAction(e -> {
        com.myapp.model.HistoryEntry entry = listView.getSelectionModel().getSelectedItem();
        if (entry != null) {
            loadRouteFromHistory(entry);
            historyStage.close();
        }
    });
    
    // Ação de duplo clique na lista também carrega
    listView.setOnMouseClicked(e -> {
        if (e.getClickCount() == 2 && listView.getSelectionModel().getSelectedItem() != null) {
            loadRouteFromHistory(listView.getSelectionModel().getSelectedItem());
            historyStage.close();
        }
    });

    // Ação de limpar
    btnClear.setOnAction(e -> {
        historyManager.clearHistory();
        listView.getItems().clear();
    });

    HBox buttons = new HBox(10, btnLoad, btnClear);
    buttons.setAlignment(Pos.CENTER);
    buttons.setPadding(new Insets(10));

    VBox root = new VBox(10, new Label("As tuas rotas recentes:"), listView, buttons);
    root.setPadding(new Insets(15));
    
    Scene scene = new Scene(root, 400, 500);
    historyStage.setScene(scene);
    historyStage.show();
}

    /**
     * Reconstrói a UI e o Mapa a partir de um item do histórico
     */
    private void loadRouteFromHistory(com.myapp.model.HistoryEntry entry) {
        resetAll();

        if (entry.getOrigin() != null) {
        Coordinate originCoord = new Coordinate(entry.getOrigin().getLatitude(), entry.getOrigin().getLongitude());
        setOrigin(originCoord);
        String name = entry.getOrigin().getName();
        origemField.setText(name != null ? name : ""); 
    }

        if (entry.getWaypoints() != null) {
            for (Point wp : entry.getWaypoints()) {
                addWaypoint(new Coordinate(wp.getLatitude(), wp.getLongitude()), wp.getName());
            }
        }
        
        if (entry.getMode() != null) {
            selectedMode = entry.getMode();
        }

        Platform.runLater(this::calculateRoute);
    }
}