package com.jardineria.infrastructure.adapter.in.desktop.theme;

/**
 * Colores de acento disponibles. Los valores concretos (uno para tema oscuro y otro
 * para tema claro, ambos con contraste AA) están en {@code /desktop/css/accents.css}
 * bajo las clases {@code .accent-<id>}.
 */
public enum Accent {
    INDIGO("indigo", "Índigo"),
    COBALTO("cobalto", "Cobalto"),
    VIOLETA("violeta", "Violeta"),
    TURQUESA("turquesa", "Turquesa"),
    ESMERALDA("esmeralda", "Esmeralda"),
    AMBAR("ambar", "Ámbar"),
    CORAL("coral", "Coral");

    private final String id;
    private final String label;

    Accent(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public String styleClass() {
        return "accent-" + id;
    }

    public static Accent fromId(String id, Accent fallback) {
        for (Accent a : values()) {
            if (a.id.equalsIgnoreCase(id) || a.name().equalsIgnoreCase(id)) {
                return a;
            }
        }
        return fallback;
    }
}
