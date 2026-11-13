package com.jardineria.infrastructure.adapter.in.desktop.theme;

/**
 * Paletas base de superficies y texto. Cada una vive en {@code /desktop/css/themes/<id>.css}
 * y define solo colores neutros y semánticos: el color de acento se elige aparte
 * ({@link Accent}), igual que en los perfiles de la terminal de Ubuntu.
 */
public enum ThemePreset {
    GRAFITO("grafito", "Grafito", "Gris pizarra neutro, cómodo para muchas horas", ThemeManager.Mode.DARK),
    MEDIANOCHE("medianoche", "Medianoche", "Azul profundo con más profundidad de capas", ThemeManager.Mode.DARK),
    BOSQUE("bosque", "Bosque", "La paleta verde original de Jardinería", ThemeManager.Mode.DARK),
    CONTRASTE("contraste", "Alto contraste", "Negro puro, bordes marcados y texto blanco (AAA)", ThemeManager.Mode.DARK),
    NIEBLA("niebla", "Niebla", "Claro gris azulado de baja saturación", ThemeManager.Mode.LIGHT),
    ARENA("arena", "Arena", "Claro cálido tipo papel, suave para la vista", ThemeManager.Mode.LIGHT);

    private final String id;
    private final String label;
    private final String description;
    private final ThemeManager.Mode mode;

    ThemePreset(String id, String label, String description, ThemeManager.Mode mode) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.mode = mode;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public ThemeManager.Mode mode() {
        return mode;
    }

    public boolean isDark() {
        return mode == ThemeManager.Mode.DARK;
    }

    /** Busca por identificador; devuelve {@code fallback} si no existe (p. ej. valores guardados antiguos). */
    public static ThemePreset fromId(String id, ThemePreset fallback) {
        for (ThemePreset t : values()) {
            if (t.id.equalsIgnoreCase(id) || t.name().equalsIgnoreCase(id)) {
                return t;
            }
        }
        return fallback;
    }
}
