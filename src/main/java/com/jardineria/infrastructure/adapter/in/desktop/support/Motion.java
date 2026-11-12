package com.jardineria.infrastructure.adapter.in.desktop.support;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Animaciones de la interfaz con soporte de accesibilidad "reducir movimiento":
 * cuando está activo, las transiciones se aplican de forma instantánea.
 */
public final class Motion {
    private static final BooleanProperty REDUCED = new SimpleBooleanProperty(false);

    /** Curva de desaceleración suave (ease-out exponencial). */
    public static final Interpolator EASE_OUT = Interpolator.SPLINE(0.16, 1, 0.3, 1);

    private Motion() {
    }

    public static BooleanProperty reducedProperty() {
        return REDUCED;
    }

    public static boolean isReduced() {
        return REDUCED.get();
    }

    /** Entrada de una vista: aparece desde 14 px más abajo con fundido. */
    public static void enter(Node node) {
        enter(node, 0);
    }

    /** Entrada escalonada: {@code delayMs} permite encadenar tarjetas una tras otra. */
    public static void enter(Node node, int delayMs) {
        if (isReduced()) {
            node.setOpacity(1);
            node.setTranslateY(0);
            return;
        }
        node.setOpacity(0);
        node.setTranslateY(14);
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(node.opacityProperty(), 0),
                        new KeyValue(node.translateYProperty(), 14)),
                new KeyFrame(Duration.millis(420),
                        new KeyValue(node.opacityProperty(), 1, EASE_OUT),
                        new KeyValue(node.translateYProperty(), 0, EASE_OUT)));
        t.setDelay(Duration.millis(delayMs));
        t.play();
    }

    /** Ejecuta una animación respetando la preferencia de movimiento reducido. */
    public static void play(Animation animation) {
        if (isReduced()) {
            animation.jumpTo("end");
            animation.stop();
            if (animation.getOnFinished() != null) {
                animation.getOnFinished().handle(null);
            }
        } else {
            animation.play();
        }
    }

    /** Crea una transición paralela vacía (utilidad para componer). */
    public static ParallelTransition parallel(Animation... animations) {
        return new ParallelTransition(animations);
    }
}
