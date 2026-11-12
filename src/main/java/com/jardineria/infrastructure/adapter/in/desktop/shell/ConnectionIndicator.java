package com.jardineria.infrastructure.adapter.in.desktop.shell;

import com.jardineria.infrastructure.adapter.in.desktop.DesktopServices.ConnectionProbe;
import com.jardineria.infrastructure.adapter.in.desktop.support.FxTasks;
import com.jardineria.infrastructure.adapter.in.desktop.support.Motion;
import com.jardineria.infrastructure.adapter.in.desktop.support.Notifier;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Indicador vivo del estado de MySQL: un punto que "late" en verde cuando hay conexión
 * y se vuelve coral si se pierde. Sondea la base de datos cada 15 s en un hilo virtual
 * y avisa con un toast en cada cambio de estado.
 */
public class ConnectionIndicator extends HBox {
    private static final PseudoClass OFFLINE = PseudoClass.getPseudoClass("offline");
    private static final PseudoClass CHECKING = PseudoClass.getPseudoClass("checking");

    private final ConnectionProbe probe;
    private final BooleanProperty online = new SimpleBooleanProperty(true);
    private final BooleanProperty compact = new SimpleBooleanProperty(false);
    private final Label status = new Label("Comprobando…");
    private final Tooltip tooltip = new Tooltip();
    private Boolean lastKnown;
    private Runnable onReconnect = () -> { };

    public ConnectionIndicator(ConnectionProbe probe) {
        this.probe = probe;
        getStyleClass().add("connection");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);

        Circle halo = new Circle(9);
        halo.getStyleClass().add("connection-halo");
        Circle dot = new Circle(5);
        dot.getStyleClass().add("connection-dot");
        StackPane pulse = new StackPane(halo, dot);
        pulse.setMinSize(22, 22);
        pulse.setMaxSize(22, 22);
        startPulse(halo);

        Label engine = new Label("MySQL · " + probe.describe());
        engine.getStyleClass().add("connection-target");
        status.getStyleClass().add("connection-status");
        VBox text = new VBox(1, status, engine);
        text.managedProperty().bind(compact.not());
        text.visibleProperty().bind(compact.not());

        getChildren().addAll(pulse, text);
        Tooltip.install(this, tooltip);
        setFocusTraversable(false);

        online.addListener((obs, was, now) -> pseudoClassStateChanged(OFFLINE, !now));

        Timeline poll = new Timeline(new KeyFrame(Duration.seconds(15), e -> check()));
        poll.setCycleCount(Animation.INDEFINITE);
        poll.play();
        check();
    }

    private void startPulse(Circle halo) {
        if (Motion.isReduced()) {
            return;
        }
        ScaleTransition grow = new ScaleTransition(Duration.seconds(1.6), halo);
        grow.setFromX(0.6);
        grow.setFromY(0.6);
        grow.setToX(1.5);
        grow.setToY(1.5);
        FadeTransition fade = new FadeTransition(Duration.seconds(1.6), halo);
        fade.setFromValue(0.7);
        fade.setToValue(0);
        ParallelTransition beat = new ParallelTransition(grow, fade);
        beat.setCycleCount(Animation.INDEFINITE);
        beat.play();
        Motion.reducedProperty().addListener((obs, was, reduced) -> {
            if (reduced) {
                beat.stop();
                halo.setOpacity(0);
            } else {
                beat.play();
            }
        });
    }

    /** Fuerza una comprobación inmediata de la conexión. */
    public void check() {
        pseudoClassStateChanged(CHECKING, true);
        long start = System.nanoTime();
        FxTasks.run(probe::isAvailable, ok -> {
            long ms = (System.nanoTime() - start) / 1_000_000;
            pseudoClassStateChanged(CHECKING, false);
            update(ok, ms);
        }, error -> {
            pseudoClassStateChanged(CHECKING, false);
            update(false, 0);
        });
    }

    private void update(boolean ok, long latencyMs) {
        online.set(ok);
        status.setText(ok ? "En línea · " + latencyMs + " ms" : "Sin conexión");
        tooltip.setText(ok
                ? "Conectado a MySQL (" + probe.describe() + ") — latencia " + latencyMs + " ms"
                : "No se puede contactar con MySQL en " + probe.describe());
        setAccessibleText(tooltip.getText());

        if (lastKnown != null && lastKnown != ok) {
            if (ok) {
                Notifier.success("Conexión restablecida", "MySQL vuelve a responder. Actualizando datos…");
                onReconnect.run();
            } else {
                Notifier.error("Conexión perdida", "MySQL dejó de responder en " + probe.describe() + ".");
            }
        }
        lastKnown = ok;
    }

    public BooleanProperty onlineProperty() {
        return online;
    }

    public BooleanProperty compactProperty() {
        return compact;
    }

    public void setOnReconnect(Runnable onReconnect) {
        this.onReconnect = onReconnect == null ? () -> { } : onReconnect;
    }
}
