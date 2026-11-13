package com.jardineria.infrastructure.adapter.in.desktop.views.settings;

import atlantafx.base.controls.ToggleSwitch;
import com.jardineria.infrastructure.adapter.in.desktop.components.KpiCard;
import com.jardineria.infrastructure.adapter.in.desktop.components.Panel;
import com.jardineria.infrastructure.adapter.in.desktop.components.ResponsiveGrid;
import com.jardineria.infrastructure.adapter.in.desktop.components.Sparkline;
import com.jardineria.infrastructure.adapter.in.desktop.components.StatusBadge;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import com.jardineria.infrastructure.adapter.in.desktop.support.ModalHost;
import com.jardineria.infrastructure.adapter.in.desktop.support.Notifier;
import com.jardineria.infrastructure.adapter.in.desktop.theme.Accent;
import com.jardineria.infrastructure.adapter.in.desktop.theme.AppearanceProfile;
import com.jardineria.infrastructure.adapter.in.desktop.theme.AppearanceProfile.Corners;
import com.jardineria.infrastructure.adapter.in.desktop.theme.AppearanceProfile.Density;
import com.jardineria.infrastructure.adapter.in.desktop.theme.ThemeManager;
import com.jardineria.infrastructure.adapter.in.desktop.theme.ThemePreset;
import com.jardineria.infrastructure.adapter.in.desktop.views.BaseView;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.kordamp.ikonli.feather.Feather;

import java.io.File;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import com.jardineria.infrastructure.adapter.in.desktop.support.Keys;

/**
 * Pantalla de Ajustes: perfiles de apariencia al estilo de la terminal de Ubuntu.
 * <p>
 * Cada cambio se aplica al instante sobre toda la ventana y se guarda en el perfil
 * activo ({@code java.util.prefs}). Se pueden crear, renombrar, eliminar, exportar e
 * importar perfiles, y volver a los valores de fábrica.
 */
public class SettingsView extends BaseView {
    private final FlowPane profileList = new FlowPane(10, 10);
    private final ToggleGroup profileGroup = new ToggleGroup();
    private final Map<ThemePreset, ToggleButton> themeCards = new EnumMap<>(ThemePreset.class);
    private final Map<Accent, ToggleButton> accentSwatches = new EnumMap<>(Accent.class);
    private final ToggleGroup themeGroup = new ToggleGroup();
    private final ToggleGroup accentGroup = new ToggleGroup();
    private final Label accentName = new Label();

    private final Slider fontSlider = new Slider(AppearanceProfile.MIN_SCALE * 100, AppearanceProfile.MAX_SCALE * 100, 100);
    private final Label fontValue = new Label();
    private final ToggleGroup densityGroup = new ToggleGroup();
    private final ToggleGroup cornersGroup = new ToggleGroup();
    private final ToggleGroup modeGroup = new ToggleGroup();
    private final ToggleSwitch ambient = new ToggleSwitch("Resplandor de fondo con el color de acento");
    private final ToggleSwitch reducedMotion = new ToggleSwitch("Reducir movimiento (sin animaciones)");
    private final ToggleSwitch collapsedSidebar = new ToggleSwitch("Menú lateral contraído al iniciar");
    private final ComboBox<Route> startRoute = new ComboBox<>();

    private final Button rename = new Button("Renombrar", Icons.of(Feather.EDIT_3, 14));
    private final Button delete = new Button("Eliminar", Icons.of(Feather.TRASH_2, 14));
    private final Label profileSummary = new Label();

    private boolean syncing;

    public SettingsView() {
        super(Route.AJUSTES);
        Button reset = new Button("Restablecer perfil", Icons.of(Feather.ROTATE_CCW, 15));
        reset.setTooltip(new Tooltip("Devuelve el perfil activo a sus valores de fábrica"));
        reset.setOnAction(e -> {
            ThemeManager.resetActive();
            Notifier.info("Perfil restablecido", "\"" + ThemeManager.getProfile().name() + "\" vuelve a sus valores de fábrica.");
        });
        Button create = new Button("Nuevo perfil", Icons.of(Feather.PLUS, 15));
        create.getStyleClass().add("accent");
        create.setOnAction(e -> askName("Nuevo perfil", "Se copiará la configuración actual.",
                "Mi perfil", name -> {
                    String created = ThemeManager.createFromCurrent(name);
                    Notifier.success("Perfil creado", "\"" + created + "\" ya está activo.");
                }));
        addActions(reset, create);
    }

    @Override
    protected Node buildContent() {
        VBox body = new VBox(18, buildProfiles(), buildThemeRow(), buildOptionsRow());
        body.getStyleClass().addAll("view-body", "settings");
        ThemeManager.profileProperty().addListener((obs, old, now) -> sync());
        ThemeManager.profiles().addListener((ListChangeListener<AppearanceProfile>) c -> rebuildProfiles());
        rebuildProfiles();
        sync();
        return body;
    }

    @Override
    public void refresh() {
        sync();
    }

    // ------------------------------------------------------------------ Perfiles

    private Node buildProfiles() {
        profileList.getStyleClass().add("profile-list");
        profileSummary.getStyleClass().add("panel-caption");
        profileSummary.setWrapText(true);

        rename.setOnAction(e -> askName("Renombrar perfil", "Elige un nombre que no esté en uso.",
                ThemeManager.getProfile().name(), name -> {
                    String old = ThemeManager.getProfile().name();
                    if (!ThemeManager.rename(old, name)) {
                        Notifier.warning("No se pudo renombrar", "Ya existe un perfil con ese nombre.");
                    }
                }));
        delete.getStyleClass().add("danger-outline");
        delete.setOnAction(e -> {
            String name = ThemeManager.getProfile().name();
            ModalHost.confirmDanger("¿Eliminar el perfil \"" + name + "\"?",
                    "Se volverá al perfil Predeterminado. Esta acción no se puede deshacer.",
                    "Eliminar perfil", () -> {
                        ThemeManager.delete(name);
                        Notifier.success("Perfil eliminado", "\"" + name + "\" se ha borrado.");
                    });
        });
        Button export = new Button("Exportar", Icons.of(Feather.UPLOAD, 14));
        export.setTooltip(new Tooltip("Guarda el perfil activo en un fichero .properties para compartirlo"));
        export.setOnAction(e -> exportProfile());
        Button importBtn = new Button("Importar", Icons.of(Feather.DOWNLOAD, 14));
        importBtn.setTooltip(new Tooltip("Añade un perfil desde un fichero exportado"));
        importBtn.setOnAction(e -> importProfile());

        HBox actions = new HBox(8, rename, delete, export, importBtn);
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox content = new VBox(14, profileList, profileSummary, actions);
        return new Panel("Perfiles", "Como en la terminal de Ubuntu: cada perfil guarda su tema, acento, texto y "
                + "comportamiento. Los cambios se aplican al instante y se guardan en el perfil activo.", content);
    }

    private void rebuildProfiles() {
        profileList.getChildren().clear();
        profileGroup.getToggles().clear();
        for (AppearanceProfile p : ThemeManager.profiles()) {
            Region dot = new Region();
            dot.getStyleClass().addAll("profile-dot", "swatch-" + p.accent().id());
            Label name = new Label(p.name());
            name.getStyleClass().add("profile-name");
            Label detail = new Label(p.activeTheme().label() + " · " + p.accent().label());
            detail.getStyleClass().add("profile-detail");
            VBox text = new VBox(0, name, detail);
            HBox graphic = new HBox(10, dot, text);
            graphic.setAlignment(Pos.CENTER_LEFT);
            if (p.builtIn()) {
                graphic.getChildren().add(Icons.of(Feather.LOCK, 11, "profile-lock"));
            }
            ToggleButton card = new ToggleButton(null, graphic);
            card.getStyleClass().add("profile-card");
            card.setToggleGroup(profileGroup);
            card.setUserData(p.name());
            card.setAccessibleText("Perfil " + p.name() + (p.builtIn() ? ", predefinido" : ""));
            card.setOnAction(e -> {
                ThemeManager.activate(p.name());
                card.setSelected(true);
            });
            profileList.getChildren().add(card);
        }
        sync();
    }

    // ------------------------------------------------------------------ Tema y vista previa

    private Node buildThemeRow() {
        FlowPane themes = new FlowPane(12, 12);
        themes.getStyleClass().add("theme-grid");
        for (ThemePreset t : ThemePreset.values()) {
            ToggleButton card = themeCard(t);
            themeCards.put(t, card);
            themes.getChildren().add(card);
        }
        HBox mode = segmented(modeGroup,
                option("Oscuro", Feather.MOON, ThemeManager.Mode.DARK),
                option("Claro", Feather.SUN, ThemeManager.Mode.LIGHT));
        modeGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (!syncing && n != null) {
                ThemeManager.setMode((ThemeManager.Mode) n.getUserData());
            }
        });
        Label hint = new Label(Keys.text("Ctrl+T alterna entre el tema oscuro y el claro elegidos en este perfil."));
        hint.getStyleClass().add("settings-hint");
        hint.setWrapText(true);
        VBox content = new VBox(14, themes, new HBox(12, mode, hint));
        ((HBox) content.getChildren().get(1)).setAlignment(Pos.CENTER_LEFT);

        ResponsiveGrid row = new ResponsiveGrid(330, 3, 16);
        row.add(new Panel("Tema", "Superficies, texto y bordes. El color de acento se elige aparte.", content), 2);
        row.add(new Panel("Vista previa", "Componentes reales con el perfil actual", buildPreview()), 1);
        return row;
    }

    private ToggleButton themeCard(ThemePreset t) {
        Region canvas = new Region();
        canvas.getStyleClass().add("tp-canvas");
        Region side = new Region();
        side.getStyleClass().add("tp-side");
        Region card1 = new Region();
        card1.getStyleClass().add("tp-card");
        Region card2 = new Region();
        card2.getStyleClass().add("tp-card");
        Region bar = new Region();
        bar.getStyleClass().add("tp-accent");
        Region line1 = new Region();
        line1.getStyleClass().add("tp-line");
        Region line2 = new Region();
        line2.getStyleClass().addAll("tp-line", "tp-line-short");
        VBox lines = new VBox(5, bar, line1, line2);
        lines.getStyleClass().add("tp-content");
        HBox cards = new HBox(6, card1, card2);
        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        VBox main = new VBox(8, lines, cards);
        main.getStyleClass().add("tp-main");
        VBox.setVgrow(cards, Priority.ALWAYS);
        HBox mock = new HBox(side, main);
        HBox.setHgrow(main, Priority.ALWAYS);
        StackPane preview = new StackPane(canvas, mock);
        preview.getStyleClass().addAll("theme-preview", "preview-" + t.id());

        Label name = new Label(t.label());
        name.getStyleClass().add("theme-name");
        Label mode = new Label(t.isDark() ? "Oscuro" : "Claro");
        mode.getStyleClass().add("theme-mode");
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox title = new HBox(6, name, grow, mode);
        title.setAlignment(Pos.CENTER_LEFT);
        Label desc = new Label(t.description());
        desc.getStyleClass().add("theme-desc");
        desc.setWrapText(true);
        desc.setMinHeight(Region.USE_PREF_SIZE);
        desc.setPrefHeight(34);
        VBox box = new VBox(8, preview, title, desc);

        ToggleButton card = new ToggleButton(null, box);
        card.getStyleClass().add("theme-card");
        card.setToggleGroup(themeGroup);
        card.setAccessibleText("Tema " + t.label() + ", " + (t.isDark() ? "oscuro" : "claro") + ". " + t.description());
        card.setOnAction(e -> {
            ThemeManager.update(p -> p.withTheme(t));
            card.setSelected(true);
        });
        return card;
    }

    private Node buildPreview() {
        KpiCard kpi = new KpiCard(Feather.SHOPPING_BAG, "Pedidos", KpiCard.Tone.ACCENT);
        kpi.setValue(128, v -> String.valueOf(Math.round(v)));
        kpi.setCaption("96 entregados · 8 rechazados");
        kpi.setChip("+12 %", true);
        Sparkline spark = new Sparkline();
        spark.setValues(new double[]{4, 6, 5, 9, 7, 11, 10, 14});
        kpi.setFooter(spark);
        kpi.setMinHeight(0);

        Button primary = new Button("Principal", Icons.of(Feather.CHECK, 14));
        primary.getStyleClass().add("accent");
        Button secondary = new Button("Secundario");
        HBox buttons = new HBox(8, primary, secondary);
        HBox badges = new HBox(6, new StatusBadge("Entregado"), new StatusBadge("Pendiente"), new StatusBadge("Rechazado"));
        ProgressBar bar = new ProgressBar(0.64);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add("kpi-progress");
        bar.getStyleClass().add("tone-accent");
        VBox box = new VBox(12, kpi, buttons, badges, bar);
        box.setMouseTransparent(false);
        return box;
    }

    // ------------------------------------------------------------------ Opciones

    private Node buildOptionsRow() {
        ResponsiveGrid row = new ResponsiveGrid(300, 3, 16);
        row.add(new Panel("Color de acento", "Botones, selección, enlaces y gráficas destacadas", buildAccents()), 1);
        row.add(new Panel("Texto y densidad", "Tamaño de letra, espaciado y forma de las esquinas", buildText()), 1);
        row.add(new Panel("Comportamiento", "Movimiento, fondo y arranque", buildBehaviour()), 1);
        return row;
    }

    private Node buildAccents() {
        FlowPane swatches = new FlowPane(10, 10);
        for (Accent a : Accent.values()) {
            Region fill = new Region();
            fill.getStyleClass().add("swatch-fill");
            StackPane graphic = new StackPane(fill, Icons.of(Feather.CHECK, 14, "swatch-check"));
            ToggleButton sw = new ToggleButton(null, graphic);
            sw.getStyleClass().addAll("accent-swatch", "swatch-" + a.id());
            sw.setToggleGroup(accentGroup);
            sw.setTooltip(new Tooltip(a.label()));
            sw.setAccessibleText("Color de acento " + a.label());
            sw.setOnAction(e -> {
                ThemeManager.update(p -> p.withAccent(a));
                sw.setSelected(true);
            });
            accentSwatches.put(a, sw);
            swatches.getChildren().add(sw);
        }
        accentName.getStyleClass().add("settings-value");
        Label note = new Label("Los colores de estado (entregado, pendiente, rechazado) no cambian: "
                + "mantienen su significado en cualquier perfil.");
        note.getStyleClass().add("settings-hint");
        note.setWrapText(true);
        return new VBox(12, swatches, accentName, note);
    }

    private Node buildText() {
        fontSlider.setMajorTickUnit(5);
        fontSlider.setMinorTickCount(0);
        fontSlider.setSnapToTicks(true);
        fontSlider.setBlockIncrement(5);
        fontSlider.setAccessibleText("Tamaño del texto");
        fontSlider.valueProperty().addListener((obs, o, n) -> {
            fontValue.setText(Math.round(n.doubleValue()) + " %");
            if (!syncing && !fontSlider.isValueChanging()) {
                applyFont();
            }
        });
        fontSlider.valueChangingProperty().addListener((obs, was, changing) -> {
            if (!changing && !syncing) {
                applyFont();
            }
        });
        fontValue.getStyleClass().add("settings-value");
        HBox.setHgrow(fontSlider, Priority.ALWAYS);
        HBox font = new HBox(10, Icons.of(Feather.TYPE, 13), fontSlider, fontValue);
        font.setAlignment(Pos.CENTER_LEFT);

        HBox density = segmented(densityGroup,
                option(Density.COMODA.label(), Feather.MAXIMIZE_2, Density.COMODA),
                option(Density.COMPACTA.label(), Feather.MINIMIZE_2, Density.COMPACTA));
        densityGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (!syncing && n != null) {
                ThemeManager.update(p -> p.withDensity((Density) n.getUserData()));
            }
        });
        HBox corners = segmented(cornersGroup,
                option(Corners.REDONDEADAS.label(), null, Corners.REDONDEADAS),
                option(Corners.SUAVES.label(), null, Corners.SUAVES),
                option(Corners.RECTAS.label(), null, Corners.RECTAS));
        cornersGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (!syncing && n != null) {
                ThemeManager.update(p -> p.withCorners((Corners) n.getUserData()));
            }
        });
        return new VBox(10, label("Tamaño del texto"), font, label("Densidad"), density, label("Esquinas"), corners);
    }

    private Node buildBehaviour() {
        ambient.selectedProperty().addListener((obs, o, n) -> {
            if (!syncing) {
                ThemeManager.update(p -> p.withAmbient(n));
            }
        });
        reducedMotion.selectedProperty().addListener((obs, o, n) -> {
            if (!syncing) {
                ThemeManager.update(p -> p.withReducedMotion(n));
            }
        });
        collapsedSidebar.selectedProperty().addListener((obs, o, n) -> {
            if (!syncing) {
                ThemeManager.update(p -> p.withSidebarCollapsed(n));
            }
        });
        startRoute.getItems().setAll(java.util.Arrays.stream(Route.values()).filter(Route::inSidebar).toList());
        startRoute.setCellFactory(v -> new RouteCell());
        startRoute.setButtonCell(new RouteCell());
        startRoute.setMaxWidth(Double.MAX_VALUE);
        startRoute.setAccessibleText("Vista al iniciar");
        startRoute.valueProperty().addListener((obs, o, n) -> {
            if (!syncing && n != null) {
                ThemeManager.update(p -> p.withStartRoute(n.name()));
            }
        });
        for (ToggleSwitch s : new ToggleSwitch[]{ambient, reducedMotion, collapsedSidebar}) {
            s.getStyleClass().add("settings-switch");
            s.setMaxWidth(Double.MAX_VALUE);
        }
        return new VBox(12, ambient, reducedMotion, collapsedSidebar, label("Vista al iniciar"), startRoute);
    }

    // ------------------------------------------------------------------ Sincronización

    /** Refleja el perfil activo en los controles sin volver a disparar cambios. */
    private void sync() {
        AppearanceProfile p = ThemeManager.getProfile();
        syncing = true;
        try {
            profileGroup.getToggles().forEach(t -> t.setSelected(p.name().equals(t.getUserData())));
            ToggleButton theme = themeCards.get(p.activeTheme());
            if (theme != null) {
                theme.setSelected(true);
            }
            themeCards.forEach((t, card) -> card.pseudoClassStateChanged(
                    javafx.css.PseudoClass.getPseudoClass("paired"), t == p.dark() || t == p.light()));
            ToggleButton accent = accentSwatches.get(p.accent());
            if (accent != null) {
                accent.setSelected(true);
            }
            accentName.setText("Acento: " + p.accent().label());
            selectByData(modeGroup, p.mode());
            selectByData(densityGroup, p.density());
            selectByData(cornersGroup, p.corners());
            fontSlider.setValue(Math.round(p.fontScale() * 100));
            fontValue.setText(Math.round(p.fontScale() * 100) + " %");
            ambient.setSelected(p.ambient());
            reducedMotion.setSelected(p.reducedMotion());
            collapsedSidebar.setSelected(p.sidebarCollapsed());
            startRoute.setValue(safeRoute(p.startRoute()));
            rename.setDisable(p.builtIn());
            delete.setDisable(p.builtIn());
            profileSummary.setText(String.format(Locale.ROOT,
                    "Activo: %s%s. Oscuro: %s · Claro: %s · Acento %s · Texto %d %% · %s · Esquinas %s.",
                    p.name(), p.builtIn() ? " (predefinido: se puede ajustar, no renombrar ni borrar)" : "",
                    p.dark().label(), p.light().label(), p.accent().label(), Math.round(p.fontScale() * 100),
                    p.density().label(), p.corners().label().toLowerCase(Locale.ROOT)));
        } finally {
            syncing = false;
        }
    }

    private void applyFont() {
        double scale = fontSlider.getValue() / 100.0;
        ThemeManager.update(p -> p.withFontScale(scale));
    }

    private static void selectByData(ToggleGroup group, Object data) {
        group.getToggles().forEach(t -> t.setSelected(data.equals(t.getUserData())));
    }

    static Route safeRoute(String name) {
        try {
            Route r = Route.valueOf(name);
            return r.inSidebar() ? r : Route.DASHBOARD;
        } catch (IllegalArgumentException | NullPointerException e) {
            return Route.DASHBOARD;
        }
    }

    // ------------------------------------------------------------------ Ficheros y diálogos

    private void exportProfile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar perfil de apariencia");
        chooser.setInitialFileName(ThemeManager.getProfile().name().replaceAll("[^\\p{L}\\p{N}_-]+", "-")
                + ".jardineria-perfil.properties");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Perfil de Jardinería", "*.properties"));
        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }
        try {
            ThemeManager.exportActive(file.toPath());
            Notifier.success("Perfil exportado", file.getName());
        } catch (Exception ex) {
            Notifier.error("No se pudo exportar", ex.getMessage());
        }
    }

    private void importProfile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importar perfil de apariencia");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Perfil de Jardinería", "*.properties"));
        File file = chooser.showOpenDialog(window());
        if (file == null) {
            return;
        }
        try {
            String name = ThemeManager.importFrom(Path.of(file.toURI()));
            Notifier.success("Perfil importado", "\"" + name + "\" ya está activo.");
        } catch (Exception ex) {
            Notifier.error("No se pudo importar", ex.getMessage());
        }
    }

    private Window window() {
        return getScene() == null ? null : getScene().getWindow();
    }

    /** Diálogo modal con un campo de texto (validación de nombre vacío). */
    private void askName(String title, String message, String initial, Consumer<String> onAccept) {
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("dialog-title");
        Label lblMessage = new Label(message);
        lblMessage.getStyleClass().add("dialog-message");
        lblMessage.setWrapText(true);
        TextField field = new TextField(initial);
        field.setAccessibleText("Nombre del perfil");
        Label error = new Label();
        error.getStyleClass().add("form-error");
        Button cancel = new Button("Cancelar");
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> ModalHost.hide());
        Button ok = new Button("Guardar", Icons.of(Feather.CHECK, 14));
        ok.getStyleClass().add("accent");
        ok.setDefaultButton(true);
        ok.setOnAction(e -> {
            String value = field.getText() == null ? "" : field.getText().strip();
            if (value.isEmpty()) {
                error.setText("El nombre no puede estar vacío.");
                return;
            }
            if (value.length() > 40) {
                error.setText("Máximo 40 caracteres.");
                return;
            }
            ModalHost.hide();
            onAccept.accept(value);
        });
        HBox buttons = new HBox(10, cancel, ok);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        VBox box = new VBox(12, lblTitle, lblMessage, field, error, buttons);
        box.getStyleClass().add("dialog");
        box.setMaxSize(420, VBox.USE_PREF_SIZE);
        ModalHost.show(box);
        Platform.runLater(() -> {
            field.requestFocus();
            field.selectAll();
        });
    }

    // ------------------------------------------------------------------ Utilidades de UI

    private static ToggleButton option(String text, Feather icon, Object data) {
        ToggleButton b = icon == null ? new ToggleButton(text) : new ToggleButton(text, Icons.of(icon, 13));
        b.setUserData(data);
        return b;
    }

    private static HBox segmented(ToggleGroup group, ToggleButton... options) {
        HBox box = new HBox();
        box.getStyleClass().add("segmented");
        for (ToggleButton b : options) {
            b.setToggleGroup(group);
            box.getChildren().add(b);
        }
        // Un control segmentado nunca queda sin selección
        group.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null && old != null) {
                old.setSelected(true);
            }
        });
        box.setMaxWidth(Region.USE_PREF_SIZE);
        return box;
    }

    private static Label label(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("settings-label");
        return l;
    }

    private static final class RouteCell extends ListCell<Route> {
        @Override
        protected void updateItem(Route item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.title());
            setGraphic(empty || item == null ? null : Icons.of(item.icon(), 14));
        }
    }
}
