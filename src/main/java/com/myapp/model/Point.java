package com.myapp.model;

/**
 * Representa um ponto geográfico com latitude, longitude e um nome opcional.
 * <p>
 * A latitude e a longitude são guardadas em graus decimais usando o datum WGS84.
 * O {@code name} pode ser {@code null} para indicar um ponto sem nome.
 */
public class Point {
    /** A latitude em graus decimais. */
    private final double latitude;
    /** A longitude em graus decimais. */
    private final double longitude;
    /** Um nome legível para o ponto; pode ser {@code null}. */
    private final String name;

    /**
     * Cria um novo {@link Point}.
     *
     * @param latitude  latitude em graus decimais; positiva para norte, negativa para sul
     * @param longitude longitude em graus decimais; positiva para este, negativa para oeste
     * @param name      nome opcional do ponto; pode ser {@code null}
     */
    public Point(double latitude, double longitude, String name) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
    }

    /**
     * Devolve a latitude em graus decimais.
     *
     * @return a latitude
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Devolve a longitude em graus decimais.
     *
     * @return a longitude
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Devolve o nome legível do ponto (opcional).
     *
     * @return o nome, ou {@code null} se não tiver nome
     */
    public String getName() {
        return name;
    }

    @Override
    /**
     * Devolve uma representação textual concisa do ponto no formato
     * {@code nome(latitude, longitude)}. Se o ponto não tiver nome, é usado {@code ?}.
     *
     * @return representação textual deste ponto
     */
    public String toString() {
        return String.format("%s(%.5f, %.5f)", name == null ? "?" : name, latitude, longitude);
    }
}