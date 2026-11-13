package com.jardineria.infrastructure.adapter.in.desktop.components;

import javafx.scene.control.Label;
import javafx.scene.shape.Circle;

import com.jardineria.infrastructure.adapter.in.common.Formatters;

/**
 * Insignia de estado de pedido con punto de color: Entregado (jade),
 * Pendiente (ámbar), Rechazado (coral) u otro (neutro).
 */
public class StatusBadge extends Label {

    public StatusBadge(String estado) {
        super(estado == null ? "—" : estado);
        Circle dot = new Circle(3.5);
        dot.getStyleClass().add("badge-dot");
        setGraphic(dot);
        setGraphicTextGap(7);
        getStyleClass().addAll("badge", toneFor(estado));
        setAccessibleText("Estado: " + getText());
    }

    /** Clase CSS de tono según el estado del pedido. */
    public static String toneFor(String estado) {
        String e = Formatters.fold(estado);
        if (e.startsWith("entregado")) {
            return "tone-jade";
        }
        if (e.startsWith("pendiente")) {
            return "tone-amber";
        }
        if (e.startsWith("rechazado")) {
            return "tone-coral";
        }
        return "tone-neutral";
    }
}
