package com.jardineria.infrastructure.adapter.in.desktop.components;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Superficie con título, descripción y acciones opcionales en la cabecera.
 */
public class Panel extends VBox {
    private final HBox header;
    private final Label lblTitle = new Label();
    private final Label lblCaption = new Label();

    public Panel(String title, String caption, Node body) {
        super(16);
        getStyleClass().add("panel");
        setMinWidth(0);

        lblTitle.setText(title);
        lblTitle.getStyleClass().add("panel-title");
        lblCaption.getStyleClass().add("panel-caption");
        lblCaption.setWrapText(true);
        VBox titles = new VBox(2, lblTitle, lblCaption);
        setCaption(caption);
        HBox.setHgrow(titles, Priority.ALWAYS);
        header = new HBox(10, titles);
        header.setAlignment(Pos.TOP_LEFT);

        getChildren().addAll(header, body);
        VBox.setVgrow(body, Priority.ALWAYS);
        setAccessibleText(title);
    }

    /** Cambia el título del panel (también su texto accesible). */
    public void setTitleText(String title) {
        lblTitle.setText(title);
        setAccessibleText(title);
    }

    /** Cambia el subtítulo del panel (se oculta si está vacío). */
    public void setCaption(String caption) {
        boolean visible = caption != null && !caption.isBlank();
        lblCaption.setText(visible ? caption : "");
        lblCaption.setVisible(visible);
        lblCaption.setManaged(visible);
    }

    /** Añade controles a la derecha de la cabecera (filtros, botones). */
    public Panel withActions(Node... actions) {
        header.getChildren().addAll(actions);
        return this;
    }
}
