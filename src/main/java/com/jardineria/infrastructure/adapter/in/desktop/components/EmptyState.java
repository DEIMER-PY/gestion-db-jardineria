package com.jardineria.infrastructure.adapter.in.desktop.components;

import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;

/**
 * Estado vacío / de error ilustrado con ícono, título, explicación y acción opcional.
 */
public class EmptyState extends VBox {

    public EmptyState(Ikon icon, String title, String message) {
        this(icon, title, message, null, null);
    }

    public EmptyState(Ikon icon, String title, String message, String actionText, Runnable action) {
        super(10);
        getStyleClass().add("empty-state");
        setAlignment(Pos.CENTER);
        setMaxWidth(440);

        StackPane badge = new StackPane(Icons.of(icon, 26));
        badge.getStyleClass().add("empty-state-badge");

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("empty-state-title");
        Label lblMessage = new Label(message);
        lblMessage.getStyleClass().add("empty-state-message");
        lblMessage.setWrapText(true);

        getChildren().addAll(badge, lblTitle, lblMessage);
        if (actionText != null && action != null) {
            Button btn = new Button(actionText);
            btn.getStyleClass().add("accent");
            btn.setOnAction(e -> action.run());
            getChildren().add(btn);
        }
    }

    /** Aplica la variante visual de error (insignia en tono coral). */
    public EmptyState asError() {
        getStyleClass().add("empty-state-error");
        return this;
    }
}
