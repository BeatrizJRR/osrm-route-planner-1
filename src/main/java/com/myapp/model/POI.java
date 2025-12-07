package com.myapp.model;

/**
 * Representa um Ponto de Interesse (POI) com nome, categoria e coordenada.
 *
 * Papel na arquitetura MVC:
 * - Model: entidade de dados imutável que representa um POI.
 * - Service (Controller) cria POIs a partir de dados da Overpass API.
 * - UI (View) renderiza POIs no mapa; este modelo não contém lógica de UI.
 * 
 * O POI pode ser usado para anotar locais relevantes ao longo de uma rota,
 * como monumentos, serviços, alertas ou referências de navegação.
 */
public class POI {
    // Nome legível do ponto de interesse.
    private final String name;
    // Categoria do ponto de interesse (por exemplo: "monumento", "serviço").
    private final String category;
    // Coordenada do POI representada por um {@link Point}.
    private final Point coordinate;

    /**
     * Cria um novo {@link POI} com os atributos fornecidos.
     *
     * @param name       nome do ponto de interesse
     * @param category   categoria do ponto de interesse
     * @param coordinate coordenada geográfica do ponto
     */
    public POI(String name, String category, Point coordinate) {
        this.name = name;
        this.category = category;
        this.coordinate = coordinate;
    }

    /**
     * Devolve o nome do ponto de interesse.
     *
     * @return nome do POI
     */
    public String getName() {
        return name;
    }

    /**
     * Devolve a categoria do ponto de interesse.
     *
     * @return categoria do POI
     */
    public String getCategory() {
        return category;
    }

    /**
     * Devolve a coordenada do ponto de interesse.
     *
     * @return coordenada como {@link Point}
     */
    public Point getCoordinate() {
        return coordinate;
    }
}