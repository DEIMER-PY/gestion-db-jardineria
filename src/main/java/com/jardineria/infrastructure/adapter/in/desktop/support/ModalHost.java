package com.jardineria.infrastructure.adapter.in.desktop.support;

import atlantafx.base.controls.ModalPane;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

/**
 * Punto único para mostrar contenido modal (formularios, confirmaciones) sobre la ventana,
 * con fondo atenuado y transición, sin abrir ventanas nativas adicionales.
 */
public final class ModalHost {
    private static ModalPane modalPane;

    private ModalHost() {
    }

    public static void install(ModalPane pane) {
        modalPane = pane;
    }

    public static void show(Node content) {
        if (modalPane != null) {
            modalPane.setAlignment(Pos.CENTER);
            modalPane.usePredefinedTransitionFactories(null);
            modalPane.show(content);
        }
    }

    public static void hide() {
        if (modalPane != null) {
            modalPane.hide(true);
        }
    }

    public static boolean isShowing() {
        return modalPane != null && modalPane.isDisplay();
    }

    /**
     * Diálogo de confirmación para acciones destructivas.
     *
     * @param title     pregunta principal
     * @param message   consecuencias de la acción
     * @param confirm   texto del botón de confirmación
     * @param onConfirm acción a ejecutar si el usuario confirma
     */
    public static void confirmDanger(String title, String message, String confirm, Runnable onConfirm) {
        StackPane badge = new StackPane(Icons.of(Feather.TRASH_2, 20));
        badge.getStyleClass().add("confirm-badge");
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("dialog-title");
        lblTitle.setWrapText(true);
        Label lblMessage = new Label(message);
        lblMessage.getStyleClass().add("dialog-message");
        lblMessage.setWrapText(true);

        Button cancel = new Button("Cancelar");
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> hide());
        Button ok = new Button(confirm, Icons.of(Feather.TRASH_2, 14));
        ok.getStyleClass().add("danger");
        ok.setDefaultButton(true);
        ok.setOnAction(e -> {
            hide();
            onConfirm.run();
        });
        HBox buttons = new HBox(10, cancel, ok);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox box = new VBox(14, badge, lblTitle, lblMessage, buttons);
        box.getStyleClass().addAll("dialog", "dialog-confirm");
        box.setMaxSize(440, VBox.USE_PREF_SIZE);
        show(box);
        Platform.runLater(cancel::requestFocus);
    }
}
