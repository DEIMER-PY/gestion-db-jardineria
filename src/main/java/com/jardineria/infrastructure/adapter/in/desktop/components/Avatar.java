package com.jardineria.infrastructure.adapter.in.desktop.components;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * Avatar con las iniciales de una persona o empresa. El tono sale de un código estable
 * (cliente o empleado), así la misma ficha conserva su color en tablas, fichas y listas.
 * Es decorativo: quien lo usa ya muestra el nombre completo, por eso no es foco de teclado.
 */
public class Avatar extends StackPane {
    private static final String[] TONES = {"tone-jade", "tone-sky", "tone-lilac", "tone-amber", "tone-coral", "tone-lime"};

    /** Tamaños disponibles; el CSS fija las medidas de cada uno. */
    public enum Size {
        /** Cabecera de una ficha. */
        LARGE("avatar"),
        /** Lista de personas dentro de una ficha. */
        MEDIUM("avatar-md"),
        /** Fila de una tabla. */
        SMALL("avatar-sm");

        private final String styleClass;

        Size(String styleClass) {
            this.styleClass = styleClass;
        }
    }

    public Avatar(String name, int seed, Size size) {
        Label text = new Label(initials(name));
        text.getStyleClass().add("avatar-text");
        getChildren().add(text);
        getStyleClass().addAll(size.styleClass, toneFor(seed));
        setFocusTraversable(false);
        setMouseTransparent(true);
    }

    /** Clase CSS de tono para un código (estable y siempre dentro de la paleta). */
    public static String toneFor(int seed) {
        return TONES[Math.floorMod(seed, TONES.length)];
    }

    /** Hasta dos iniciales en mayúscula; «?» si no hay nombre utilizable. */
    public static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        StringBuilder sb = new StringBuilder();
        for (String p : name.trim().split("[\\s.\\-]+")) {
            if (!p.isEmpty() && Character.isLetterOrDigit(p.charAt(0))) {
                sb.append(Character.toUpperCase(p.charAt(0)));
            }
            if (sb.length() == 2) {
                break;
            }
        }
        return sb.isEmpty() ? "?" : sb.toString();
    }
}
