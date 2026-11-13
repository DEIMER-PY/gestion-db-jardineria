package com.jardineria.infrastructure.adapter.in.desktop.navigation;

import javafx.scene.Node;

/**
 * Contrato de una pantalla navegable dentro del shell.
 */
public interface View {

    /** Nodo raíz de la vista. */
    Node getNode();

    /** Se invoca cada vez que la vista pasa a primer plano. */
    default void onShow() {
        refresh();
    }

    /** Vuelve a consultar los datos (F5). */
    void refresh();

    /**
     * Muestra un elemento concreto al llegar desde otra pantalla (p. ej. un clic en el
     * dashboard): las vistas de módulo filtran por el texto y seleccionan la coincidencia.
     * Por defecto no hace nada.
     */
    default void reveal(String query) {
    }

    /** Acción "Nuevo" (Ctrl+N); por defecto no hace nada. */
    default void createNew() {
    }
}
