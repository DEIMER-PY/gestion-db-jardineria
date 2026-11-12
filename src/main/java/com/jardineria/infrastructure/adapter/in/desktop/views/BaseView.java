package com.jardineria.infrastructure.adapter.in.desktop.views;

import atlantafx.base.controls.RingProgressIndicator;
import com.jardineria.infrastructure.adapter.in.desktop.components.EmptyState;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.View;
import com.jardineria.infrastructure.adapter.in.desktop.support.FxTasks;
import com.jardineria.infrastructure.adapter.in.desktop.support.Motion;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Plantilla de pantalla (patrón Template Method): cabecera con título y acciones,
 * contenido desplazable y gestión uniforme de los estados cargando / error / sin conexión.
 * <p>
 * Las subclases solo implementan {@link #buildContent()} y {@link #refresh()},
 * y usan {@link #load(Callable, Consumer)} para consultar los puertos sin bloquear la UI.
 */
public abstract class BaseView extends StackPane implements View {
    private static final Duration LOADER_DELAY = Duration.millis(180);

    protected final Route route;
    private final HBox actions = new HBox(8);
    private final StackPane body = new StackPane();
    private final StackPane overlay = new StackPane();
    private final boolean scrollable;
    private Node content;
    private int pendingLoads;
    private PauseTransition loaderDelay;

    protected BaseView(Route route) {
        this(route, true);
    }

    protected BaseView(Route route, boolean scrollable) {
        this.route = route;
        this.scrollable = scrollable;
        getStyleClass().addAll("view", "view-" + route.name().toLowerCase());
        setAccessibleText(route.title());

        overlay.getStyleClass().add("view-overlay");
        overlay.setVisible(false);
        overlay.setManaged(false);
        overlay.setPickOnBounds(true);

        VBox page = new VBox(buildHeader(), body);
        page.getStyleClass().add("view-page");
        VBox.setVgrow(body, Priority.ALWAYS);

        getChildren().addAll(page, overlay);
    }

    private Node buildHeader() {
        Label eyebrow = new Label(route.section().label().toUpperCase());
        eyebrow.getStyleClass().add("view-eyebrow");
        Label title = new Label(route.title());
        title.getStyleClass().add("view-title");
        Label subtitle = new Label(route.description());
        subtitle.getStyleClass().add("view-subtitle");
        subtitle.setWrapText(true);

        VBox titles = new VBox(2, eyebrow, title, subtitle);
        HBox.setHgrow(titles, Priority.ALWAYS);
        titles.setMinWidth(0);

        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getStyleClass().add("view-actions");

        HBox header = new HBox(16, titles, actions);
        header.setAlignment(Pos.BOTTOM_LEFT);
        header.getStyleClass().add("view-header");
        return header;
    }

    /** Construye el contenido principal de la vista (se llama una única vez). */
    protected abstract Node buildContent();

    @Override
    public Node getNode() {
        ensureContent();
        return this;
    }

    private void ensureContent() {
        if (content != null) {
            return;
        }
        Node built = buildContent();
        if (scrollable) {
            ScrollPane scroll = new ScrollPane(built);
            scroll.setFitToWidth(true);
            scroll.getStyleClass().addAll("view-scroll", "edge-to-edge");
            content = scroll;
        } else {
            content = built;
        }
        body.getChildren().setAll(content);
    }

    /** Añade botones a la zona de acciones de la cabecera. */
    protected void addActions(Node... nodes) {
        actions.getChildren().addAll(nodes);
    }

    /**
     * Consulta en segundo plano y renderiza el resultado en el hilo de JavaFX.
     * Muestra el indicador de carga solo si la consulta tarda más de 180 ms
     * (evita parpadeos) y un estado de error con "Reintentar" si falla.
     */
    protected <T> void load(Callable<T> work, Consumer<T> render) {
        beginLoading();
        FxTasks.run(work, result -> {
            endLoading();
            render.accept(result);
        }, error -> {
            endLoading();
            showError(error);
        });
    }

    private void beginLoading() {
        pendingLoads++;
        if (loaderDelay == null) {
            loaderDelay = new PauseTransition(LOADER_DELAY);
            loaderDelay.setOnFinished(e -> {
                if (pendingLoads > 0) {
                    RingProgressIndicator ring = new RingProgressIndicator(-1, false);
                    ring.getStyleClass().add("view-loader");
                    Label lbl = new Label("Consultando MySQL…");
                    lbl.getStyleClass().add("view-loader-text");
                    VBox box = new VBox(12, ring, lbl);
                    box.setAlignment(Pos.CENTER);
                    showOverlay(box, "view-overlay-loading");
                }
            });
        }
        loaderDelay.playFromStart();
    }

    private void endLoading() {
        pendingLoads = Math.max(0, pendingLoads - 1);
        if (pendingLoads == 0) {
            if (loaderDelay != null) {
                loaderDelay.stop();
            }
            if (overlay.getStyleClass().contains("view-overlay-loading")) {
                hideOverlay();
            }
        }
    }

    /** Muestra un estado de error a pantalla completa con opción de reintentar. */
    protected void showError(Throwable error) {
        boolean offline = FxTasks.isConnectionError(error);
        EmptyState state = new EmptyState(
                offline ? Feather.WIFI_OFF : Feather.ALERT_TRIANGLE,
                offline ? "Sin conexión con la base de datos" : "No se pudieron cargar los datos",
                offline
                        ? "Levanta el contenedor con  docker compose -f docker/docker-compose.yml up -d  y pulsa Reintentar."
                        : FxTasks.friendlyMessage(error),
                "Reintentar", () -> {
                    hideOverlay();
                    refresh();
                }).asError();
        showOverlay(state, "view-overlay-error");
    }

    private void showOverlay(Node node, String styleClass) {
        overlay.getStyleClass().removeAll("view-overlay-loading", "view-overlay-error");
        overlay.getStyleClass().add(styleClass);
        overlay.getChildren().setAll(node);
        overlay.setManaged(true);
        overlay.setVisible(true);
        Motion.enter(node);
    }

    protected void hideOverlay() {
        overlay.getChildren().clear();
        overlay.setVisible(false);
        overlay.setManaged(false);
        overlay.getStyleClass().removeAll("view-overlay-loading", "view-overlay-error");
    }

    /** Separador flexible para filas de cabecera. */
    protected static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }
}
