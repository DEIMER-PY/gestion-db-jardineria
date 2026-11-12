package com.jardineria.infrastructure.adapter.in.desktop.shell;

import com.jardineria.infrastructure.adapter.in.desktop.navigation.Navigator;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import com.jardineria.infrastructure.adapter.in.desktop.support.Motion;
import com.jardineria.infrastructure.adapter.in.desktop.theme.ThemeManager;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.feather.Feather;

/**
 * Barra superior: migas de pan, buscador global (abre la paleta de comandos),
 * refresco, menú de accesibilidad y cambio de tema.
 */
public class TopBar extends HBox {
    private final Label crumbSection = new Label();
    private final Label crumbPage = new Label();

    public TopBar(Navigator navigator, Runnable openPalette, Runnable refresh) {
        getStyleClass().add("topbar");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(10);

        Label home = new Label("Jardinería");
        home.getStyleClass().add("crumb");
        crumbSection.getStyleClass().add("crumb");
        crumbPage.getStyleClass().addAll("crumb", "crumb-current");
        HBox crumbs = new HBox(8, home, sep(), crumbSection, sep(), crumbPage);
        crumbs.setAlignment(Pos.CENTER_LEFT);
        crumbs.getStyleClass().add("crumbs");
        crumbs.setMinWidth(0);

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);

        // Buscador global → paleta de comandos
        Label searchText = new Label("Buscar o ir a…");
        searchText.getStyleClass().add("search-pill-text");
        Label kbd = new Label("Ctrl K");
        kbd.getStyleClass().add("kbd");
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
        btnRefresh.setOnAction(e -> refresh.run());

        Button btnTheme = iconButton(themeIcon(), "Cambiar tema claro / oscuro (Ctrl+T)");
        btnTheme.setOnAction(e -> ThemeManager.toggle());
        ThemeManager.modeProperty().addListener((obs, o, n) -> btnTheme.setGraphic(Icons.of(themeIcon(), 17)));

        getChildren().addAll(crumbs, grow, search, btnRefresh, accessibilityMenu(), btnTheme);

        navigator.currentProperty().addListener((obs, old, route) -> updateCrumbs(route));
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

        MenuButton menu = new MenuButton(null, Icons.of(Feather.EYE, 17), bigger, smaller, reset,
                new SeparatorMenuItem(), reduceMotion);
        menu.getStyleClass().addAll("topbar-icon", "flat", "button-icon", "no-arrow");
        menu.setTooltip(new Tooltip("Accesibilidad: tamaño de texto (Ctrl + / Ctrl -) y movimiento"));
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
