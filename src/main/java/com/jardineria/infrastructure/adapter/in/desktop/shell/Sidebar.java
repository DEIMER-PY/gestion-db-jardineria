package com.jardineria.infrastructure.adapter.in.desktop.shell;

import com.jardineria.infrastructure.adapter.in.desktop.navigation.Navigator;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import com.jardineria.infrastructure.adapter.in.desktop.support.Motion;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.material2.Material2AL;

import java.util.EnumMap;
import java.util.Map;

/**
 * Menú lateral agrupado por secciones, colapsable (Ctrl+B) a solo íconos,
 * con indicador de la ruta activa y estado de la conexión a MySQL al pie.
 */
public class Sidebar extends VBox {
    static final double EXPANDED_WIDTH = 252;
    static final double COLLAPSED_WIDTH = 76;

    private final Map<Route, Button> items = new EnumMap<>(Route.class);
    private final BooleanProperty collapsed = new SimpleBooleanProperty(false);
    private final VBox brandText;
    private final VBox nav = new VBox(2);

    public Sidebar(Navigator navigator, ConnectionIndicator connection) {
        getStyleClass().add("sidebar");
        setPrefWidth(EXPANDED_WIDTH);
        setMinWidth(COLLAPSED_WIDTH);
        setMaxWidth(EXPANDED_WIDTH);

        // Marca
        StackPane logo = new StackPane(Icons.of(Material2AL.ECO, 20));
        logo.getStyleClass().add("brand-logo");
        Label name = new Label("Jardinería");
        name.getStyleClass().add("brand-name");
        Label tagline = new Label("Suite de gestión");
        tagline.getStyleClass().add("brand-tagline");
        brandText = new VBox(0, name, tagline);
        HBox brand = new HBox(12, logo, brandText);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getStyleClass().add("brand");

        // Navegación por secciones
        nav.getStyleClass().add("sidebar-nav");
        Route.Section currentSection = null;
        for (Route route : Route.values()) {
            if (!route.inSidebar()) {
                continue;
            }
            if (route.section() != currentSection) {
                currentSection = route.section();
                Label section = new Label(currentSection.label().toUpperCase());
                section.getStyleClass().add("sidebar-section");
                section.managedProperty().bind(collapsed.not());
                section.visibleProperty().bind(collapsed.not());
                nav.getChildren().add(section);
            }
            Button item = createItem(route, navigator);
            items.put(route, item);
            nav.getChildren().add(item);
        }
        ScrollPane navScroll = new ScrollPane(nav);
        navScroll.setFitToWidth(true);
        navScroll.getStyleClass().addAll("sidebar-scroll", "edge-to-edge");
        navScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(navScroll, Priority.ALWAYS);

        // Pie: conexión y botón de colapsar
        Button toggle = new Button(null, Icons.of(Feather.CHEVRONS_LEFT, 16));
        toggle.getStyleClass().addAll("sidebar-toggle", "flat", "button-icon");
        toggle.setAccessibleText("Contraer o expandir menú lateral");
        toggle.setTooltip(new Tooltip("Contraer / expandir menú (Ctrl+B)"));
        toggle.setOnAction(e -> toggleCollapsed());
        collapsed.addListener((obs, was, now) ->
                toggle.setGraphic(Icons.of(now ? Feather.CHEVRONS_RIGHT : Feather.CHEVRONS_LEFT, 16)));

        Button settings = createItem(Route.AJUSTES, navigator);
        settings.getStyleClass().add("nav-item-settings");
        items.put(Route.AJUSTES, settings);

        connection.compactProperty().bind(collapsed);
        HBox footerActions = new HBox(4, toggle);
        footerActions.setAlignment(Pos.CENTER_LEFT);
        VBox footer = new VBox(10, connection, settings, footerActions);
        footer.getStyleClass().add("sidebar-footer");
        footer.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(brand, navScroll, footer);

        navigator.currentProperty().addListener((obs, old, now) -> highlight(now));
        collapsed.addListener((obs, was, now) -> applyCollapsed(now));
    }

    private Button createItem(Route route, Navigator navigator) {
        // Contenido: ícono · texto · (espacio) · atajo. Texto y atajo se ocultan al colapsar.
        Label text = new Label(route.title());
        text.getStyleClass().add("nav-text");
        text.managedProperty().bind(collapsed.not());
        text.visibleProperty().bind(collapsed.not());

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox graphic = new HBox(12, Icons.of(route.icon(), 18), text, grow);
        graphic.setAlignment(Pos.CENTER_LEFT);

        if (route.shortcut() != null) {
            Label kbd = new Label(route.keyText());
            kbd.getStyleClass().add("nav-kbd");
            kbd.managedProperty().bind(collapsed.not());
            kbd.visibleProperty().bind(collapsed.not());
            graphic.getChildren().add(kbd);
        }

        Button item = new Button(null, graphic);
        item.getStyleClass().add("nav-item");
        item.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        item.setMaxWidth(Double.MAX_VALUE);
        item.setAlignment(Pos.CENTER_LEFT);
        // 24 px = padding horizontal del .nav-item definido en app.css (12 px por lado)
        graphic.prefWidthProperty().bind(item.widthProperty().subtract(24));
        item.setAccessibleText(route.title() + (route.shortcut() == null ? "" : ", atajo " + route.shortcutLabel()));
        item.setTooltip(new Tooltip(route.title() + (route.shortcut() == null ? "" : "   " + route.shortcutLabel())));
        item.setOnAction(e -> navigator.navigate(route));
        return item;
    }

    private void highlight(Route active) {
        items.forEach((route, btn) -> {
            btn.getStyleClass().remove("active");
            if (route == active) {
                btn.getStyleClass().add("active");
            }
        });
    }

    public void toggleCollapsed() {
        collapsed.set(!collapsed.get());
    }

    public BooleanProperty collapsedProperty() {
        return collapsed;
    }

    private void applyCollapsed(boolean now) {
        brandText.setManaged(!now);
        brandText.setVisible(!now);
        pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("collapsed"), now);
        double target = now ? COLLAPSED_WIDTH : EXPANDED_WIDTH;
        if (Motion.isReduced()) {
            setPrefWidth(target);
            return;
        }
        new Timeline(new KeyFrame(Duration.millis(260),
                new KeyValue(prefWidthProperty(), target, Motion.EASE_OUT))).play();
    }
}
