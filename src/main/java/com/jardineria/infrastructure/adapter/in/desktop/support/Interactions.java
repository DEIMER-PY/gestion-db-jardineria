package com.jardineria.infrastructure.adapter.in.desktop.support;

import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;

/**
 * Comportamiento común de los elementos «clicables» que no son botones (filas de listas,
 * leyendas): foco con Tab, activación con clic primario, Intro o Espacio, rol de botón
 * para lectores de pantalla y la clase {@code interactive-row} para el hover.
 */
public final class Interactions {

    private Interactions() {
    }

    /** Fila activable: {@link #onActivate} más la clase {@code interactive-row} (cursor, foco visible). */
    public static void activatable(Node node, Runnable action) {
        node.getStyleClass().add("interactive-row");
        onActivate(node, action);
    }

    /** Foco, rol de botón y activación con clic primario, Intro o Espacio, sin tocar el estilo. */
    public static void onActivate(Node node, Runnable action) {
        node.setFocusTraversable(true);
        node.setAccessibleRole(AccessibleRole.BUTTON);
        node.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                action.run();
            }
        });
        node.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                action.run();
                e.consume();
            }
        });
    }
}
