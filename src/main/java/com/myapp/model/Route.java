package com.myapp.model;

import java.util.List;

/**
 * Representa um percurso/rota calculado, incluindo pontos do trajeto, distância,
 * duração estimada, modo de transporte e pontos de interesse (POIs) associados.
 *
 * Papel na arquitetura MVC:
 * - Model: agrega dados da rota processados pelo Service (Controller).
 * - Service constrói instâncias de Route a partir de respostas das APIs.
 * - UI (View) apresenta rotas ao utilizador; pode invocar `addPOI` para enriquecer dados.
 * <p>
 * Os pontos da rota estão em ordem sequencial, descrevendo o caminho desde a
 * origem até ao destino.
 */
public class Route {
    /** Lista sequencial dos pontos que compõem a rota. */
    private final List<Point> routePoints;
    /** Distância total da rota em quilómetros. */
    private final double distanceKm;
    /** Duração estimada da rota em segundos. */
    private final long durationSec;
    /** Modo de transporte utilizado para o cálculo da rota. */
    private final TransportMode mode;
    /** Pontos de interesse associados à rota. */
    private final List<POI> pois;

    /**
     * Cria uma nova {@link Route} com os detalhes fornecidos.
     *
     * @param routePoints lista de pontos que definem o trajeto, em ordem
     * @param distanceKm  distância total em quilómetros
     * @param durationSec duração estimada em segundos
     * @param mode        modo de transporte
     * @param pois        lista de pontos de interesse associados (pode ser vazia)
     */
    public Route(List<Point> routePoints, double distanceKm, long durationSec, TransportMode mode, List<POI> pois) {
        this.routePoints = routePoints;
        this.distanceKm = distanceKm;
        this.durationSec = durationSec;
        this.mode = mode;
        this.pois = pois;
    }

    /**
     * Devolve a lista sequencial dos pontos da rota.
     *
     * @return lista de {@link Point}
     */
    public List<Point> getRoutePoints() {
        return routePoints;
    }

    /**
     * Devolve a distância total da rota em quilómetros.
     *
     * @return distância em km
     */
    public double getDistanceKm() {
        return distanceKm;
    }

    /**
     * Devolve a duração estimada da rota em segundos.
     *
     * @return duração em segundos
     */
    public long getDurationSec() {
        return durationSec;
    }

    /**
     * Devolve o modo de transporte utilizado para a rota.
     *
     * @return modo de transporte
     */
    public TransportMode getMode() {
        return mode;
    }

    /**
     * Devolve a lista de pontos de interesse (POIs) associados à rota.
     *
     * @return lista de {@link POI}
     */
    public List<POI> getPois() {
        return pois;
    }

    /**
     * Adiciona um ponto de interesse (POI) à lista associada à rota.
     *
     * @param poi o ponto de interesse a adicionar
     */
    public void addPOI(POI poi) {
        this.pois.add(poi);
    }
}