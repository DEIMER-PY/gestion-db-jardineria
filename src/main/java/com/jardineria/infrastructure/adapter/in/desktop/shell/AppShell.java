package com.jardineria.infrastructure.adapter.in.desktop.shell;

import atlantafx.base.controls.ModalPane;
import com.jardineria.infrastructure.adapter.in.desktop.DesktopServices;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Navigator;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.View;
import com.jardineria.infrastructure.adapter.in.desktop.shell.CommandPalette.Command;
import com.jardineria.infrastructure.adapter.in.desktop.support.ModalHost;
import com.jardineria.infrastructure.adapter.in.desktop.support.Notifier;
import com.jardineria.infrastructure.adapter.in.desktop.theme.ThemeManager;
import com.jardineria.infrastructure.adapter.in.desktop.views.DesktopViews;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

import java.util.ArrayList;
import java.util.List;

/**
 * Contenedor principal de la ventana: menú lateral + barra superior + área de contenido,
 * con capas superpuestas para modales y notificaciones. Registra los atajos de teclado.
 */
public class AppShell extends StackPane {
    private final Navigator navigator;
    private final Sidebar sidebar;
    private final CommandPalette palette;
    private final ConnectionIndicator connection;

    public AppShell(DesktopServices services) {
        getStyleClass().add("app-shell");

        StackPane contentHost = new StackPane();
        contentHost.getStyleClass().add("content-host");
        navigator = new Navigator(contentHost);
        DesktopViews.registerAll(navigator, services);

        connection = new ConnectionIndicator(services.connection());
        connection.setOnReconnect(navigator::refreshCurrent);
        sidebar = new Sidebar(navigator, connection);

        ModalPane modalPane = new ModalPane();
        modalPane.getStyleClass().add("modal-layer");
        ModalHost.install(modalPane);
        palette = new CommandPalette(modalPane, buildCommands());

        TopBar topBar = new TopBar(navigator, palette::open, this::refresh);
        VBox main = new VBox(topBar, contentHost);
        main.getStyleClass().add("main-area");
        VBox.setVgrow(contentHost, Priority.ALWAYS);

        BorderPane layout = new BorderPane(main);
        layout.setLeft(sidebar);

        VBox toastHost = new VBox(10);
        toastHost.getStyleClass().add("toast-host");
        toastHost.setAlignment(Pos.TOP_RIGHT);
        toastHost.setPickOnBounds(false);
        toastHost.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        StackPane.setAlignment(toastHost, Pos.TOP_RIGHT);
        Notifier.install(toastHost);

        getChildren().addAll(layout, modalPane, toastHost);
    }

    /** Muestra la ruta inicial. */
    public void start(Route initial) {
        navigator.navigate(initial);
    }

    public Navigator getNavigator() {
        return navigator;
    }

    private void refresh() {
        navigator.refreshCurrent();
        connection.check();
    }

    private List<Command> buildCommands() {
        List<Command> commands = new ArrayList<>();
        for (Route route : Route.values()) {
            commands.add(new Command("Ir a " + route.title(), route.description(), route.icon(),
                    route.shortcutLabel(), () -> navigator.navigate(route)));
        }
        commands.add(new Command("Crear registro en la vista actual", "Abre el formulario de alta del módulo activo",
                Feather.PLUS_CIRCLE, "Ctrl N", this::createNew));
        commands.add(new Command("Actualizar datos", "Vuelve a consultar MySQL para la vista actual",
                Feather.REFRESH_CW, "F5", this::refresh));
        commands.add(new Command("Cambiar tema claro / oscuro", "Alterna entre Invernadero nocturno y Vivero diurno",
                Feather.SUN, "Ctrl T", ThemeManager::toggle));
        commands.add(new Command("Contraer / expandir menú lateral", "Gana espacio de trabajo",
                Feather.SIDEBAR, "Ctrl B", () -> sidebar.toggleCollapsed()));
        commands.add(new Command("Aumentar tamaño de texto", "Accesibilidad", Feather.ZOOM_IN, "Ctrl +",
                ThemeManager::increaseFont));
        commands.add(new Command("Reducir tamaño de texto", "Accesibilidad", Feather.ZOOM_OUT, "Ctrl -",
                ThemeManager::decreaseFont));
        commands.add(new Command("Volver a la vista anterior", "Historial de navegación", Feather.ARROW_LEFT,
                "Alt ←", navigator::back));
        return commands;
    }

    private void createNew() {
        View view = navigator.currentView();
        if (view != null) {
            view.createNew();
        }
    }

    /** Registra los atajos globales sobre la escena. */
    public void installShortcuts(Scene scene) {
        var acc = scene.getAccelerators();
        for (Route route : Route.values()) {
            if (route.shortcut() != null) {
                acc.put(new KeyCodeCombination(route.shortcut(), KeyCombination.SHORTCUT_DOWN),
                        () -> navigator.navigate(route));
            }
        }
        acc.put(new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN), palette::open);
        acc.put(new KeyCodeCombination(KeyCode.F5), this::refresh);
        acc.put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), this::createNew);
        acc.put(new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN), ThemeManager::toggle);
        acc.put(new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN), sidebar::toggleCollapsed);
        acc.put(new KeyCodeCombination(KeyCode.PLUS, KeyCombination.SHORTCUT_DOWN), ThemeManager::increaseFont);
        acc.put(new KeyCodeCombination(KeyCode.ADD, KeyCombination.SHORTCUT_DOWN), ThemeManager::increaseFont);
        acc.put(new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.SHORTCUT_DOWN), ThemeManager::increaseFont);
        acc.put(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN), ThemeManager::decreaseFont);
        acc.put(new KeyCodeCombination(KeyCode.SUBTRACT, KeyCombination.SHORTCUT_DOWN), ThemeManager::decreaseFont);
        acc.put(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.SHORTCUT_DOWN), ThemeManager::resetFont);
        acc.put(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.ALT_DOWN), navigator::back);
    }
}
