package com.jardineria.infrastructure.adapter.in.desktop.shell;

import com.jardineria.infrastructure.adapter.in.desktop.navigation.Navigator;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import com.jardineria.infrastructure.adapter.in.desktop.support.Motion;
import com.jardineria.infrastructure.adapter.in.desktop.theme.ThemeManager;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.feather.Feather;

import java.util.Arrays;

/**
 * Barra superior: historial (atrás / adelante), migas de pan navegables, buscador global
 * (abre la paleta de comandos), refresco, ayuda de atajos, accesibilidad, tema y ajustes.
 */
public class TopBar extends HBox {
    private final Hyperlink crumbSection = new Hyperlink();
    private final Label crumbPage = new Label();
    private final Navigator navigator;

    public TopBar(Navigator navigator, Runnable openPalette, Runnable refresh, Runnable showShortcuts) {
        this.navigator = navigator;
        getStyleClass().add("topbar");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);

        Button back = iconButton(Feather.ARROW_LEFT, "Volver a la vista anterior (Alt+←)");
        back.setOnAction(e -> navigator.back());
        back.disableProperty().bind(navigator.canGoBackProperty().not());
        Button forward = iconButton(Feather.ARROW_RIGHT, "Avanzar (Alt+→)");
        forward.setOnAction(e -> navigator.forward());
        forward.disableProperty().bind(navigator.canGoForwardProperty().not());
        HBox history = new HBox(2, back, forward);
        history.getStyleClass().add("history-buttons");

        Hyperlink home = new Hyperlink("Jardinería");
        home.getStyleClass().addAll("crumb", "crumb-link");
        home.setOnAction(e -> navigator.navigate(Route.DASHBOARD));
        home.setTooltip(new Tooltip("Ir al panel general"));
        crumbSection.getStyleClass().addAll("crumb", "crumb-link");
        crumbSection.setOnAction(e -> openSection());
        crumbPage.getStyleClass().addAll("crumb", "crumb-current");
        Label homeSep = sep();
        HBox crumbs = new HBox(6, home, homeSep, crumbSection, sep(), crumbPage);
        crumbs.setAlignment(Pos.CENTER_LEFT);
        crumbs.getStyleClass().add("crumbs");
        crumbs.setMinWidth(0);
        crumbPage.setMinWidth(Region.USE_PREF_SIZE);
        crumbSection.setMinWidth(Region.USE_PREF_SIZE);
        // En ventanas estrechas se oculta la raíz de la miga para que la página actual se lea entera.
        widthProperty().addListener((obs, o, w) -> {
            boolean compact = w.doubleValue() < 960;
            home.setVisible(!compact);
            home.setManaged(!compact);
            homeSep.setVisible(!compact);
            homeSep.setManaged(!compact);
        });

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);

        // Buscador global → paleta de comandos
        Label searchText = new Label("Buscar módulo, acción o tema…");
        searchText.getStyleClass().add("search-pill-text");
        searchText.setMinWidth(0);
        Label kbd = new Label("Ctrl K");
        kbd.getStyleClass().add("kbd");
        kbd.setMinWidth(Region.USE_PREF_SIZE);
        Region pillGrow = new Region();
        HBox.setHgrow(pillGrow, Priority.ALWAYS);
        HBox pillContent = new HBox(10, Icons.of(Feather.SEARCH, 15), searchText, pillGrow, kbd);
        pillContent.setAlignment(Pos.CENTER_LEFT);
        Button search = new Button(null, pillContent);
        search.getStyleClass().add("search-pill");
        search.setAccessibleText("Abrir búsqueda y paleta de comandos, atajo Control K");
        search.setOnAction(e -> openPalette.run());
        pillContent.prefWidthProperty().bind(search.widthProperty().subtract(28));

        Button btnRefresh = iconButton(Feather.REFRESH_CW, "Actualizar datos (F5)");
        btnRefresh.setOnAction(e -> {
            spin(btnRefresh);
            refresh.run();
        });

        Button btnHelp = iconButton(Feather.HELP_CIRCLE, "Atajos de teclado (F1)");
        btnHelp.setOnAction(e -> showShortcuts.run());

        Button btnTheme = iconButton(themeIcon(), "Cambiar tema claro / oscuro (Ctrl+T)");
        btnTheme.setOnAction(e -> ThemeManager.toggle());
        ThemeManager.modeProperty().addListener((obs, o, n) -> btnTheme.setGraphic(Icons.of(themeIcon(), 17)));

        Button btnSettings = iconButton(Feather.SLIDERS, "Ajustes y perfiles de apariencia (Ctrl+,)");
        btnSettings.setOnAction(e -> navigator.navigate(Route.AJUSTES));

        getChildren().addAll(history, crumbs, grow, search, btnRefresh, btnHelp, accessibilityMenu(), btnTheme, btnSettings);

        navigator.currentProperty().addListener((obs, old, route) -> updateCrumbs(route));
    }

    private void openSection() {
        Route current = navigator.getCurrent();
        if (current == null) {
            return;
        }
        Arrays.stream(Route.values())
                .filter(r -> r.section() == current.section())
                .findFirst()
                .ifPresent(navigator::navigate);
    }

    private static void spin(Button button) {
        if (Motion.isReduced() || button.getGraphic() == null) {
            return;
        }
        javafx.animation.RotateTransition rt = new javafx.animation.RotateTransition(
                javafx.util.Duration.millis(650), button.getGraphic());
        rt.setFromAngle(0);
        rt.setToAngle(360);
        rt.setInterpolator(Motion.EASE_OUT);
        rt.play();
    }

    private static Feather themeIcon() {
        return ThemeManager.getMode() == ThemeManager.Mode.DARK ? Feather.SUN : Feather.MOON;
    }

    private MenuButton accessibilityMenu() {
        MenuItem bigger = new MenuItem("Aumentar texto", Icons.of(Feather.ZOOM_IN, 14));
        bigger.setOnAction(e -> ThemeManager.increaseFont());
        MenuItem smaller = new MenuItem("Reducir texto", Icons.of(Feather.ZOOM_OUT, 14));
        smaller.setOnAction(e -> ThemeManager.decreaseFont());
        MenuItem reset = new MenuItem("Tamaño original", Icons.of(Feather.TYPE, 14));
        reset.setOnAction(e -> ThemeManager.resetFont());
        CheckMenuItem reduceMotion = new CheckMenuItem("Reducir movimiento");
        reduceMotion.selectedProperty().bindBidirectional(Motion.reducedProperty());
        MenuItem contrast = new MenuItem("Tema de alto contraste", Icons.of(Feather.APERTURE, 14));
        contrast.setOnAction(e -> ThemeManager.update(p -> p.withTheme(
                com.jardineria.infrastructure.adapter.in.desktop.theme.ThemePreset.CONTRASTE)));

        MenuButton menu = new MenuButton(null, Icons.of(Feather.EYE, 17), bigger, smaller, reset,
                new SeparatorMenuItem(), reduceMotion, contrast);
        menu.getStyleClass().addAll("topbar-icon", "flat", "button-icon", "no-arrow");
        menu.setTooltip(new Tooltip("Accesibilidad: tamaño de texto (Ctrl + / Ctrl -), movimiento y contraste"));
        menu.setAccessibleText("Opciones de accesibilidad");
        return menu;
    }

    private static Button iconButton(Feather icon, String tooltip) {
        Button b = new Button(null, Icons.of(icon, 17));
        b.getStyleClass().addAll("topbar-icon", "flat", "button-icon");
        b.setTooltip(new Tooltip(tooltip));
        b.setAccessibleText(tooltip);
        return b;
    }

    private static Label sep() {
        Label l = new Label("/");
        l.getStyleClass().add("crumb-sep");
        return l;
    }

    private void updateCrumbs(Route route) {
        if (route == null) {
            return;
        }
        crumbSection.setText(route.section().label());
        crumbPage.setText(route.title());
    }
}
