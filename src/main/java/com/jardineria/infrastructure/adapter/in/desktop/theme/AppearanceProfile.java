package com.jardineria.infrastructure.adapter.in.desktop.theme;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Perfil de apariencia con nombre (equivalente a un perfil de la terminal de Ubuntu):
 * agrupa tema, acento, tipografía, densidad, bordes y preferencias de movimiento.
 * Es inmutable; los cambios se hacen con los métodos {@code with*}.
 *
 * @param name             nombre visible y clave del perfil
 * @param builtIn          los perfiles predefinidos no se pueden renombrar ni borrar
 * @param dark             tema usado en modo oscuro
 * @param light            tema usado en modo claro
 * @param mode             modo activo (oscuro o claro); Ctrl+T alterna entre {@code dark} y {@code light}
 * @param accent           color de acento
 * @param fontScale        escala de texto (0,85 – 1,40)
 * @param density          espaciado de tarjetas, tablas y menú
 * @param corners          radio de las esquinas
 * @param ambient          resplandor de fondo con el color de acento
 * @param reducedMotion    desactiva animaciones y transiciones
 * @param startRoute       nombre de la ruta que se abre al iniciar
 * @param sidebarCollapsed el menú lateral arranca contraído
 */
public record AppearanceProfile(
        String name,
        boolean builtIn,
        ThemePreset dark,
        ThemePreset light,
        ThemeManager.Mode mode,
        Accent accent,
        double fontScale,
        Density density,
        Corners corners,
        boolean ambient,
        boolean reducedMotion,
        String startRoute,
        boolean sidebarCollapsed
) {
    public static final double MIN_SCALE = 0.85;
    public static final double MAX_SCALE = 1.40;

    /** Espaciado general de la interfaz. */
    public enum Density {
        COMODA("Cómoda"), COMPACTA("Compacta");

        private final String label;

        Density(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** Radio de las esquinas de tarjetas, botones y diálogos. */
    public enum Corners {
        REDONDEADAS("Redondeadas"), SUAVES("Suaves"), RECTAS("Rectas");

        private final String label;

        Corners(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public AppearanceProfile {
        name = name == null || name.isBlank() ? "Sin nombre" : name.strip();
        dark = dark == null || !dark.isDark() ? ThemePreset.GRAFITO : dark;
        light = light == null || light.isDark() ? ThemePreset.NIEBLA : light;
        mode = mode == null ? ThemeManager.Mode.DARK : mode;
        accent = accent == null ? Accent.INDIGO : accent;
        fontScale = clampScale(fontScale);
        density = density == null ? Density.COMODA : density;
        corners = corners == null ? Corners.REDONDEADAS : corners;
        startRoute = startRoute == null || startRoute.isBlank() ? "DASHBOARD" : startRoute;
    }

    /** Perfil por defecto de la aplicación: Grafito + Índigo. */
    public static AppearanceProfile defaults() {
        return new AppearanceProfile("Predeterminado", true, ThemePreset.GRAFITO, ThemePreset.NIEBLA,
                ThemeManager.Mode.DARK, Accent.INDIGO, 1.0, Density.COMODA, Corners.REDONDEADAS,
                true, false, "DASHBOARD", false);
    }

    /** Perfiles incluidos de serie. */
    public static java.util.List<AppearanceProfile> builtIns() {
        return java.util.List.of(
                defaults(),
                new AppearanceProfile("Nocturno", true, ThemePreset.MEDIANOCHE, ThemePreset.NIEBLA,
                        ThemeManager.Mode.DARK, Accent.TURQUESA, 1.0, Density.COMODA, Corners.REDONDEADAS,
                        true, false, "DASHBOARD", false),
                new AppearanceProfile("Diurno", true, ThemePreset.GRAFITO, ThemePreset.NIEBLA,
                        ThemeManager.Mode.LIGHT, Accent.COBALTO, 1.0, Density.COMODA, Corners.REDONDEADAS,
                        false, false, "DASHBOARD", false),
                new AppearanceProfile("Presentación", true, ThemePreset.CONTRASTE, ThemePreset.ARENA,
                        ThemeManager.Mode.DARK, Accent.AMBAR, 1.2, Density.COMODA, Corners.SUAVES,
                        false, true, "DASHBOARD", true),
                new AppearanceProfile("Analista", true, ThemePreset.GRAFITO, ThemePreset.NIEBLA,
                        ThemeManager.Mode.DARK, Accent.VIOLETA, 0.92, Density.COMPACTA, Corners.SUAVES,
                        false, false, "ANALYTICS", false),
                new AppearanceProfile("Jardinería clásica", true, ThemePreset.BOSQUE, ThemePreset.ARENA,
                        ThemeManager.Mode.DARK, Accent.ESMERALDA, 1.0, Density.COMODA, Corners.REDONDEADAS,
                        true, false, "DASHBOARD", false));
    }

    /** Tema efectivo según el modo activo. */
    public ThemePreset activeTheme() {
        return mode == ThemeManager.Mode.DARK ? dark : light;
    }

    public AppearanceProfile withName(String value) {
        return new AppearanceProfile(value, false, dark, light, mode, accent, fontScale, density, corners,
                ambient, reducedMotion, startRoute, sidebarCollapsed);
    }

    /** Elige un tema y activa su modo (claro u oscuro). */
    public AppearanceProfile withTheme(ThemePreset theme) {
        return theme.isDark()
                ? new AppearanceProfile(name, builtIn, theme, light, ThemeManager.Mode.DARK, accent, fontScale,
                density, corners, ambient, reducedMotion, startRoute, sidebarCollapsed)
                : new AppearanceProfile(name, builtIn, dark, theme, ThemeManager.Mode.LIGHT, accent, fontScale,
                density, corners, ambient, reducedMotion, startRoute, sidebarCollapsed);
    }

    public AppearanceProfile withMode(ThemeManager.Mode value) {
        return new AppearanceProfile(name, builtIn, dark, light, value, accent, fontScale, density, corners,
                ambient, reducedMotion, startRoute, sidebarCollapsed);
    }

    public AppearanceProfile withAccent(Accent value) {
        return new AppearanceProfile(name, builtIn, dark, light, mode, value, fontScale, density, corners,
                ambient, reducedMotion, startRoute, sidebarCollapsed);
    }

    public AppearanceProfile withFontScale(double value) {
        return new AppearanceProfile(name, builtIn, dark, light, mode, accent, value, density, corners,
                ambient, reducedMotion, startRoute, sidebarCollapsed);
    }

    public AppearanceProfile withDensity(Density value) {
        return new AppearanceProfile(name, builtIn, dark, light, mode, accent, fontScale, value, corners,
                ambient, reducedMotion, startRoute, sidebarCollapsed);
    }

    public AppearanceProfile withCorners(Corners value) {
        return new AppearanceProfile(name, builtIn, dark, light, mode, accent, fontScale, density, value,
                ambient, reducedMotion, startRoute, sidebarCollapsed);
    }

    public AppearanceProfile withAmbient(boolean value) {
        return new AppearanceProfile(name, builtIn, dark, light, mode, accent, fontScale, density, corners,
                value, reducedMotion, startRoute, sidebarCollapsed);
    }

    public AppearanceProfile withReducedMotion(boolean value) {
        return new AppearanceProfile(name, builtIn, dark, light, mode, accent, fontScale, density, corners,
                ambient, value, startRoute, sidebarCollapsed);
    }

    public AppearanceProfile withStartRoute(String value) {
        return new AppearanceProfile(name, builtIn, dark, light, mode, accent, fontScale, density, corners,
                ambient, reducedMotion, value, sidebarCollapsed);
    }

    public AppearanceProfile withSidebarCollapsed(boolean value) {
        return new AppearanceProfile(name, builtIn, dark, light, mode, accent, fontScale, density, corners,
                ambient, reducedMotion, startRoute, value);
    }

    /** Serializa a pares clave/valor (formato de {@code java.util.prefs} y de exportación). */
    public Map<String, String> toMap() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("builtIn", String.valueOf(builtIn));
        m.put("dark", dark.id());
        m.put("light", light.id());
        m.put("mode", mode.name());
        m.put("accent", accent.id());
        m.put("fontScale", String.format(Locale.ROOT, "%.2f", fontScale));
        m.put("density", density.name());
        m.put("corners", corners.name());
        m.put("ambient", String.valueOf(ambient));
        m.put("reducedMotion", String.valueOf(reducedMotion));
        m.put("startRoute", startRoute);
        m.put("sidebarCollapsed", String.valueOf(sidebarCollapsed));
        return m;
    }

    /** Reconstruye un perfil tolerando claves ausentes o valores inválidos (usa los valores por defecto). */
    public static AppearanceProfile fromMap(Map<String, String> m) {
        Objects.requireNonNull(m);
        AppearanceProfile d = defaults();
        return new AppearanceProfile(
                m.getOrDefault("name", d.name()),
                Boolean.parseBoolean(m.getOrDefault("builtIn", "false")),
                ThemePreset.fromId(m.get("dark"), d.dark()),
                ThemePreset.fromId(m.get("light"), d.light()),
                parseEnum(ThemeManager.Mode.class, m.get("mode"), d.mode()),
                Accent.fromId(m.get("accent"), d.accent()),
                parseDouble(m.get("fontScale"), d.fontScale()),
                parseEnum(Density.class, m.get("density"), d.density()),
                parseEnum(Corners.class, m.get("corners"), d.corners()),
                Boolean.parseBoolean(m.getOrDefault("ambient", String.valueOf(d.ambient()))),
                Boolean.parseBoolean(m.getOrDefault("reducedMotion", "false")),
                m.getOrDefault("startRoute", d.startRoute()),
                Boolean.parseBoolean(m.getOrDefault("sidebarCollapsed", "false")));
    }

    static double clampScale(double scale) {
        if (Double.isNaN(scale) || scale <= 0) {
            return 1.0;
        }
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, Math.round(scale * 100) / 100.0));
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return value == null ? fallback : Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, E fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
