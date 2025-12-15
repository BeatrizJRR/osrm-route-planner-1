package com.myapp.model;

import java.util.List;

/**
 * Representa o perfil de elevação ao longo de uma rota, incluindo amostras de
 * altitude, distâncias acumuladas e estatísticas como máximos, mínimos, subida
 * total e descida total.
 *
 * Papel na arquitetura MVC:
 * - Model: agrega dados de elevação processados pelo Service (Controller).
 * - Service obtém dados da ElevationClient API e constrói este modelo.
 * - UI (View) renderiza gráficos e estatísticas; este modelo encapsula
 * cálculos.
 * 
 * As unidades são: elevação em metros (m) e distância acumulada em quilómetros
 * (km).
 */
public class ElevationProfile {
    // Lista de elevações em metros (m) para cada amostra do percurso.
    private final List<Double> elevations;
    // Lista de distâncias acumuladas em quilómetros (km) para cada amostra.
    private final List<Double> distances;
    // Maior valor de elevação observado.
    private final double maxElevation;
    // Menor valor de elevação observado.
    private final double minElevation;
    // Subida total ao longo do percurso, em metros (m).
    private final double totalAscent;
    // Descida total ao longo do percurso, em metros (m).
    private final double totalDescent;

    /**
     * Cria um novo {@link ElevationProfile} e calcula estatísticas com base nas
     * listas fornecidas.
     *
     * @param elevations lista de elevações em metros
     * @param distances  lista de distâncias acumuladas em quilómetros (mesmo
     *                   tamanho que {@code elevations})
     */
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
                if (diff > 0) {
                    ascent += diff;
                } else {
                    descent += Math.abs(diff);
                }
            }
        }

        this.maxElevation = max;
        this.minElevation = min;
        this.totalAscent = ascent;
        this.totalDescent = descent;
    }

    /**
     * Devolve a lista de elevações (m).
     *
     * @return lista de elevações em metros
     */
    public List<Double> getElevations() {
        return elevations;
    }

    /**
     * Devolve a lista de distâncias acumuladas (km).
     *
     * @return lista de distâncias em quilómetros
     */
    public List<Double> getDistances() {
        return distances;
    }

    /**
     * Devolve a elevação máxima observada.
     *
     * @return elevação máxima em metros
     */
    public double getMaxElevation() {
        return maxElevation;
    }

    /**
     * Devolve a elevação mínima observada.
     *
     * @return elevação mínima em metros
     */
    public double getMinElevation() {
        return minElevation;
    }

    /**
     * Devolve a subida total ao longo do percurso.
     *
     * @return subida total em metros
     */
    public double getTotalAscent() {
        return totalAscent;
    }

    /**
     * Devolve a descida total ao longo do percurso.
     *
     * @return descida total em metros
     */
    public double getTotalDescent() {
        return totalDescent;
    }
}