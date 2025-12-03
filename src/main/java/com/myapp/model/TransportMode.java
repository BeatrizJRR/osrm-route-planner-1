package com.myapp.model;

/**
 * Enum que representa o modo de transporte utilizado no cálculo de rotas.
 *
 * Papel na arquitetura MVC:
 * - Model: define tipos de dados imutáveis (enum) para modos de transporte.
 * - Service (Controller) usa estes valores para parametrizar chamadas às APIs de routing.
 * - UI (View) apresenta opções ao utilizador e passa o modo selecionado ao Service.
 *
 * Cada modo pode implicar diferentes perfis de velocidade, restrições
 * de acessibilidade e seleção de vias.
 */
public enum TransportMode {
    /** Automóvel: privilegia estradas e perfis de velocidade de carros. */
    CAR,  
    /** Bicicleta: considera vias cicláveis e restrições adequadas a bicicletas. */
    BIKE, 
    /** A pé: privilegia percursos pedonais e caminhos acessíveis a peões. */
    FOOT;
}