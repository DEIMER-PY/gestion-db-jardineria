package com.jardineria.infrastructure.adapter.in.desktop.theme;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import com.jardineria.infrastructure.adapter.in.desktop.support.Motion;
import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.UnaryOperator;

/**
 * Gestiona la apariencia de la aplicación a partir de <b>perfiles</b> (como los de la
 * terminal de Ubuntu): cada perfil combina un tema base ({@link ThemePreset}), un acento
 * ({@link Accent}), escala de texto, densidad, bordes y preferencias de movimiento.
 * <p>
 * Capas de estilo aplicadas a la escena, en orden:
 * <ol>
 *   <li>AtlantaFX Primer (claro u oscuro) como hoja de agente de usuario.</li>
 *   <li>{@code themes/<tema>.css}: superficies, texto, bordes y colores semánticos.</li>
 *   <li>{@code accents.css}: color de acento según {@code .accent-*} y el modo.</li>
 *   <li>{@code app.css}: componentes, que solo usan variables.</li>
 * </ol>
 * Los modificadores (densidad, esquinas, fondo ambiental) son clases en la raíz.
 */
public final class ThemeManager {
    private static final Logger log = LoggerFactory.getLogger(ThemeManager.class);

    public enum Mode { DARK, LIGHT }

    private static final double BASE_FONT_PX = 14;

    private static final List<String> FONT_FILES = List.of(
            "Manrope-400.ttf", "Manrope-500.ttf", "Manrope-600.ttf", "Manrope-700.ttf", "Manrope-800.ttf",
            "BricolageGrotesque-600.ttf", "BricolageGrotesque-700.ttf", "BricolageGrotesque-800.ttf",
            "JetBrainsMono-500.ttf", "JetBrainsMono-700.ttf");

    private static final ObjectProperty<Mode> MODE = new SimpleObjectProperty<>(Mode.DARK);
    private static final ObjectProperty<AppearanceProfile> PROFILE =
            new SimpleObjectProperty<>(AppearanceProfile.defaults());
    private static final ObservableList<AppearanceProfile> PROFILES =
            FXCollections.observableArrayList(AppearanceProfile.builtIns());

    private static ProfileStore store = ProfileStore.inMemory();
    private static Scene scene;
    private static boolean fontsLoaded;
    private static boolean syncingMotion;

    static {
        Motion.reducedProperty().addListener((obs, was, reduced) -> {
            if (!syncingMotion && reduced != getProfile().reducedMotion()) {
                update(p -> p.withReducedMotion(reduced));
            }
        });
    }

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

    /**
     * Conecta el almacén de perfiles y restaura el último perfil activo.
     * Sin llamarlo se trabaja en memoria (comportamiento de las pruebas).
     */
    public static void useStore(ProfileStore profileStore) {
        store = Objects.requireNonNull(profileStore);
        List<AppearanceProfile> saved = store.loadProfiles();
        List<AppearanceProfile> merged = new ArrayList<>(AppearanceProfile.builtIns());
        for (AppearanceProfile p : saved) {
            int idx = indexOf(merged, p.name());
            if (idx >= 0) {
                merged.set(idx, p);
            } else {
                merged.add(p);
            }
        }
        PROFILES.setAll(merged);
        String active = store.loadActive();
        PROFILE.set(find(active).orElse(PROFILES.get(0)));
        apply();
    }

    /** Aplica el perfil actual a la escena principal. */
    public static void install(Scene target) {
        scene = Objects.requireNonNull(target);
        loadFonts();
        apply();
    }

    // ------------------------------------------------------------------ API de modo (compatibilidad)

    public static ObjectProperty<Mode> modeProperty() {
        return MODE;
    }

    public static Mode getMode() {
        return MODE.get();
    }

    public static void setMode(Mode mode) {
        update(p -> p.withMode(mode));
    }

    /** Alterna entre el tema oscuro y el claro del perfil activo (Ctrl+T). */
    public static void toggle() {
        setMode(getMode() == Mode.DARK ? Mode.LIGHT : Mode.DARK);
    }

    public static void increaseFont() {
        update(p -> p.withFontScale(p.fontScale() + 0.05));
    }

    public static void decreaseFont() {
        update(p -> p.withFontScale(p.fontScale() - 0.05));
    }

    public static void resetFont() {
        update(p -> p.withFontScale(1.0));
    }

    public static double getFontScale() {
        return getProfile().fontScale();
    }

    // ------------------------------------------------------------------ Perfiles

    public static ReadOnlyObjectProperty<AppearanceProfile> profileProperty() {
        return PROFILE;
    }

    public static AppearanceProfile getProfile() {
        return PROFILE.get();
    }

    /** Lista observable de perfiles (predefinidos primero). No se debe modificar directamente. */
    public static ObservableList<AppearanceProfile> profiles() {
        return PROFILES;
    }

    /** Modifica el perfil activo, lo aplica y lo guarda. */
    public static void update(UnaryOperator<AppearanceProfile> change) {
        AppearanceProfile current = getProfile();
        AppearanceProfile next = Objects.requireNonNull(change.apply(current));
        if (next.equals(current)) {
            return;
        }
        int idx = indexOf(PROFILES, current.name());
        if (idx >= 0) {
            PROFILES.set(idx, next);
        }
        PROFILE.set(next);
        apply();
        persist();
    }

    public static void activate(String name) {
        find(name).ifPresent(p -> {
            PROFILE.set(p);
            apply();
            persist();
        });
    }

    /** Crea un perfil nuevo copiando el activo y lo activa. Devuelve el nombre final (único). */
    public static String createFromCurrent(String desiredName) {
        String name = uniqueName(desiredName == null || desiredName.isBlank() ? "Mi perfil" : desiredName.strip());
        AppearanceProfile copy = getProfile().withName(name);
        PROFILES.add(copy);
        PROFILE.set(copy);
        apply();
        persist();
        return name;
    }

    /** Renombra un perfil propio. Devuelve {@code false} si es predefinido o el nombre ya existe. */
    public static boolean rename(String oldName, String newName) {
        if (newName == null || newName.isBlank() || find(newName.strip()).isPresent()) {
            return false;
        }
        int idx = indexOf(PROFILES, oldName);
        if (idx < 0 || PROFILES.get(idx).builtIn()) {
            return false;
        }
        AppearanceProfile renamed = PROFILES.get(idx).withName(newName.strip());
        boolean wasActive = getProfile().name().equals(oldName);
        PROFILES.set(idx, renamed);
        if (wasActive) {
            PROFILE.set(renamed);
        }
        persist();
        return true;
    }

    /** Elimina un perfil propio; si era el activo se vuelve al predeterminado. */
    public static boolean delete(String name) {
        int idx = indexOf(PROFILES, name);
        if (idx < 0 || PROFILES.get(idx).builtIn()) {
            return false;
        }
        PROFILES.remove(idx);
        if (getProfile().name().equals(name)) {
            PROFILE.set(PROFILES.get(0));
            apply();
        }
        persist();
        return true;
    }

    /** Devuelve el perfil activo a sus valores de fábrica (predefinidos) o a los del predeterminado (propios). */
    public static void resetActive() {
        AppearanceProfile current = getProfile();
        AppearanceProfile factory = AppearanceProfile.builtIns().stream()
                .filter(p -> p.name().equals(current.name()))
                .findFirst()
                .orElse(AppearanceProfile.defaults().withName(current.name()));
        update(p -> factory);
    }

    /** Exporta el perfil activo a un fichero {@code .properties} (UTF-8). */
    public static void exportActive(Path file) throws IOException {
        Properties props = new Properties();
        props.putAll(getProfile().toMap());
        props.remove("builtIn");
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            props.store(w, "Perfil de apariencia de Jardinería Suite");
        }
    }

    /** Importa un perfil desde un fichero exportado; lo añade como perfil propio y lo activa. */
    public static String importFrom(Path file) throws IOException {
        Properties props = new Properties();
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(r);
        }
        Map<String, String> values = new HashMap<>();
        props.stringPropertyNames().forEach(k -> values.put(k, props.getProperty(k)));
        values.put("builtIn", "false");
        AppearanceProfile imported = AppearanceProfile.fromMap(values);
        String name = uniqueName(imported.name());
        AppearanceProfile finalProfile = imported.withName(name);
        PROFILES.add(finalProfile);
        PROFILE.set(finalProfile);
        apply();
        persist();
        return name;
    }

    // ------------------------------------------------------------------ Aplicación de estilos

    private static void apply() {
        AppearanceProfile p = getProfile();
        ThemePreset theme = p.activeTheme();
        boolean dark = theme.isDark();
        if (MODE.get() != p.mode()) {
            MODE.set(p.mode());
        }
        syncingMotion = true;
        try {
            Motion.reducedProperty().set(p.reducedMotion());
        } finally {
            syncingMotion = false;
        }
        Application.setUserAgentStylesheet(dark
                ? new PrimerDark().getUserAgentStylesheet()
                : new PrimerLight().getUserAgentStylesheet());
        if (scene == null) {
            return;
        }
        scene.getStylesheets().setAll(
                css("themes/" + theme.id() + ".css"),
                css("accents.css"),
                css("app.css"));
        List<String> classes = scene.getRoot().getStyleClass();
        classes.removeIf(c -> c.startsWith("theme-") || c.startsWith("accent-") || c.startsWith("density-")
                || c.startsWith("corners-") || c.equals("ambient-off"));
        classes.addAll(List.of(
                dark ? "theme-dark" : "theme-light",
                "theme-" + theme.id(),
                p.accent().styleClass(),
                "density-" + p.density().name().toLowerCase(Locale.ROOT),
                "corners-" + p.corners().name().toLowerCase(Locale.ROOT)));
        if (!p.ambient()) {
            classes.add("ambient-off");
        }
        scene.getRoot().setStyle(String.format(Locale.US, "-fx-font-size: %.1fpx;", BASE_FONT_PX * p.fontScale()));
    }

    private static void persist() {
        store.save(List.copyOf(PROFILES), getProfile().name());
    }

    private static Optional<AppearanceProfile> find(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return PROFILES.stream().filter(p -> p.name().equalsIgnoreCase(name.strip())).findFirst();
    }

    private static int indexOf(List<AppearanceProfile> list, String name) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).name().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    private static String uniqueName(String base) {
        String candidate = base;
        int n = 2;
        while (find(candidate).isPresent()) {
            candidate = base + " (" + n++ + ")";
        }
        return candidate;
    }

    private static String css(String name) {
        return Objects.requireNonNull(ThemeManager.class.getResource("/desktop/css/" + name),
                "Hoja de estilos no encontrada: " + name).toExternalForm();
    }
}
