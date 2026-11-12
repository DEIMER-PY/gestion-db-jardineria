package com.jardineria.infrastructure.adapter.in.desktop.support;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;

/**
 * Notificaciones tipo "toast" no bloqueantes que se apilan en la esquina superior derecha.
 * Se pausan al pasar el ratón y se pueden cerrar con el botón o al expirar.
 */
public final class Notifier {
    private static final int MAX_VISIBLE = 4;
    private static VBox host;

    public enum Kind {
        SUCCESS(Feather.CHECK_CIRCLE, "toast-success"),
        INFO(Feather.INFO, "toast-info"),
        WARNING(Feather.ALERT_TRIANGLE, "toast-warning"),
        ERROR(Feather.ALERT_OCTAGON, "toast-error");

        final Ikon icon;
        final String styleClass;

        Kind(Ikon icon, String styleClass) {
            this.icon = icon;
            this.styleClass = styleClass;
        }
    }

    private Notifier() {
    }

    /** Registra el contenedor donde se apilan los toasts (lo crea la ventana principal). */
    public static void install(VBox toastHost) {
        host = toastHost;
    }

    public static void success(String title, String message) {
        show(Kind.SUCCESS, title, message);
    }

    public static void info(String title, String message) {
        show(Kind.INFO, title, message);
    }

    public static void warning(String title, String message) {
        show(Kind.WARNING, title, message);
    }

    public static void error(String title, String message) {
        show(Kind.ERROR, title, message);
    }

    public static void show(Kind kind, String title, String message) {
        FxTasks.runOnFx(() -> {
            if (host == null) {
                return;
            }
            StackPane toast = build(kind, title, message);
            host.getChildren().add(0, toast);
            while (host.getChildren().size() > MAX_VISIBLE) {
                host.getChildren().remove(host.getChildren().size() - 1);
            }
            animateIn(toast);

            PauseTransition life = new PauseTransition(Duration.seconds(kind == Kind.ERROR ? 7 : 4.5));
            life.setOnFinished(e -> dismiss(toast));
            toast.setOnMouseEntered(e -> life.pause());
            toast.setOnMouseExited(e -> life.play());
            life.play();
        });
    }

    private static StackPane build(Kind kind, String title, String message) {
        StackPane badge = new StackPane(Icons.of(kind.icon, 16));
        badge.getStyleClass().add("toast-badge");

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("toast-title");
        Label lblMessage = new Label(message);
        lblMessage.getStyleClass().add("toast-message");
        lblMessage.setWrapText(true);
        VBox text = new VBox(2, lblTitle, lblMessage);
        HBox.setHgrow(text, Priority.ALWAYS);

        Button close = new Button(null, Icons.of(Feather.X, 14));
        close.getStyleClass().addAll("toast-close", "flat");
        close.setAccessibleText("Cerrar notificación");
        close.setFocusTraversable(false);

        HBox row = new HBox(12, badge, text, close);
        row.setAlignment(Pos.TOP_LEFT);

        StackPane toast = new StackPane(row);
        toast.getStyleClass().addAll("toast", kind.styleClass);
        toast.setMaxWidth(380);
        toast.setMinWidth(320);
        toast.setAccessibleRole(AccessibleRole.TEXT);
        toast.setAccessibleText(title + ". " + message);
        close.setOnAction(e -> dismiss(toast));
        return toast;
    }

    private static void animateIn(StackPane toast) {
        if (Motion.isReduced()) {
            return;
        }
        toast.setOpacity(0);
        toast.setTranslateX(40);
        new Timeline(new KeyFrame(Duration.millis(360),
                new KeyValue(toast.opacityProperty(), 1, Motion.EASE_OUT),
                new KeyValue(toast.translateXProperty(), 0, Motion.EASE_OUT))).play();
    }

    private static void dismiss(StackPane toast) {
        if (host == null || !host.getChildren().contains(toast)) {
            return;
        }
        if (Motion.isReduced()) {
            host.getChildren().remove(toast);
            return;
        }
        Timeline out = new Timeline(new KeyFrame(Duration.millis(220),
                new KeyValue(toast.opacityProperty(), 0),
                new KeyValue(toast.translateXProperty(), 30)));
        out.setOnFinished(e -> host.getChildren().remove(toast));
        out.play();
    }
}
