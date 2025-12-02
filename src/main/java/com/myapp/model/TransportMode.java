package com.myapp.model;

/**
 * Enum que representa o modo de transporte utilizado no cálculo de rotas.
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