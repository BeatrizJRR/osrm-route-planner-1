package com.myapp.utils;

import com.myapp.ui.MapViewer;
import javafx.application.Application;

/**
 * Ponto de entrada da aplicação.
 *
 * Papel na arquitetura MVC:
 * - Utils: bootstrap da aplicação; não contém lógica MVC.
 * - Lança a UI (View) através do JavaFX Application framework.
 *
 * Responsável por iniciar a aplicação JavaFX através da classe {@link MapViewer}.
 */
public class App {
    /**
     * Método principal (entrypoint) que lança a aplicação JavaFX.
     *
     * @param args argumentos de linha de comandos passados à aplicação
     */
    public static void main(String[] args) {
        Application.launch(MapViewer.class, args);
    }
}