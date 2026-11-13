package com.jardineria.infrastructure.adapter.in.desktop.components;

import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;

/**
 * Tira compacta de indicadores para la cabecera de los módulos CRUD
 * (p. ej. "36 clientes · 5 países · 1,2 M€ de crédito").
 */
public class StatStrip extends ResponsiveGrid {

    public StatStrip() {
        super(165, 5, 12);
        getStyleClass().add("stat-strip");
    }

    /** Añade un indicador con ícono, valor destacado y etiqueta. */
    public StatStrip stat(Ikon icon, String value, String label, KpiCard.Tone tone) {
        StackPane badge = new StackPane(Icons.of(icon, 16));
        badge.getStyleClass().add("stat-badge");
        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        Label l = new Label(label);
        l.getStyleClass().add("stat-label");
        l.setWrapText(true);
        VBox text = new VBox(0, v, l);
        text.setMinWidth(0);
        HBox tile = new HBox(12, badge, text);
        tile.setAlignment(Pos.CENTER_LEFT);
        tile.getStyleClass().addAll("stat-tile", "tone-" + tone.name().toLowerCase());
        tile.setAccessibleText(label + ": " + value);
        add(tile, 1);
        return this;
    }
}
