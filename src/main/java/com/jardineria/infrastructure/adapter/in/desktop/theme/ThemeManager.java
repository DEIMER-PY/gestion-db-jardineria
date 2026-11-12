package com.jardineria.infrastructure.adapter.in.desktop.theme;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

/**
 * Gestiona el tema visual: base AtlantaFX (Primer) + hojas propias con la paleta
 * "Invernadero nocturno" (oscuro) y "Vivero diurno" (claro), fuentes empaquetadas
 * y escala de texto accesible.
 */
public final class ThemeManager {
    private static final Logger log = LoggerFactory.getLogger(ThemeManager.class);

    public enum Mode { DARK, LIGHT }

    private static final double BASE_FONT_PX = 14;
    private static final double MIN_SCALE = 0.85;
    private static final double MAX_SCALE = 1.40;

    private static final List<String> FONT_FILES = List.of(
            "Manrope-400.ttf", "Manrope-500.ttf", "Manrope-600.ttf", "Manrope-700.ttf", "Manrope-800.ttf",
            "BricolageGrotesque-600.ttf", "BricolageGrotesque-700.ttf", "BricolageGrotesque-800.ttf",
            "JetBrainsMono-500.ttf", "JetBrainsMono-700.ttf");

    private static final ObjectProperty<Mode> MODE = new SimpleObjectProperty<>(Mode.DARK);
    private static double fontScale = 1.0;
    private static Scene scene;
    private static boolean fontsLoaded;

    private ThemeManager() {
    }

    /** Carga las fuentes empaquetadas (Manrope, Bricolage Grotesque, JetBrains Mono). */
    public static synchronized void loadFonts() {
        if (fontsLoaded) {
            return;
        }
        for (String file : FONT_FILES) {
            try (InputStream in = ThemeManager.class.getResourceAsStream("/desktop/fonts/" + file)) {
                if (in == null || Font.loadFont(in, BASE_FONT_PX) == null) {
                    log.warn("No se pudo cargar la fuente {} (se usará la fuente del sistema)", file);
                }
            } catch (Exception e) {
                log.warn("Error cargando fuente {}: {}", file, e.getMessage());
            }
        }
        fontsLoaded = true;
    }

    /** Aplica el tema actual a la escena principal. */
    public static void install(Scene target) {
        scene = Objects.requireNonNull(target);
        loadFonts();
        apply();
    }

    public static ObjectProperty<Mode> modeProperty() {
        return MODE;
    }

    public static Mode getMode() {
        return MODE.get();
    }

    public static void setMode(Mode mode) {
        MODE.set(mode);
        apply();
    }

    public static void toggle() {
        setMode(getMode() == Mode.DARK ? Mode.LIGHT : Mode.DARK);
    }

    public static void increaseFont() {
        setFontScale(fontScale + 0.1);
    }

    public static void decreaseFont() {
        setFontScale(fontScale - 0.1);
    }

    public static void resetFont() {
        setFontScale(1.0);
    }

    public static double getFontScale() {
        return fontScale;
    }

    private static void setFontScale(double scale) {
        fontScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, Math.round(scale * 100) / 100.0));
        apply();
    }

    private static void apply() {
        boolean dark = getMode() == Mode.DARK;
        Application.setUserAgentStylesheet(dark
                ? new PrimerDark().getUserAgentStylesheet()
                : new PrimerLight().getUserAgentStylesheet());
        if (scene == null) {
            return;
        }
        scene.getStylesheets().setAll(
                css("tokens-" + (dark ? "dark" : "light") + ".css"),
                css("app.css"));
        scene.getRoot().getStyleClass().removeAll("theme-dark", "theme-light");
        scene.getRoot().getStyleClass().add(dark ? "theme-dark" : "theme-light");
        scene.getRoot().setStyle(String.format(java.util.Locale.US, "-fx-font-size: %.1fpx;", BASE_FONT_PX * fontScale));
    }

    private static String css(String name) {
        return Objects.requireNonNull(ThemeManager.class.getResource("/desktop/css/" + name),
                "Hoja de estilos no encontrada: " + name).toExternalForm();
    }
}
