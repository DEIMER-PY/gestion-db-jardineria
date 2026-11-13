package com.jardineria.infrastructure.adapter.in.desktop.components;

import com.jardineria.infrastructure.adapter.in.desktop.support.Interactions;
import com.jardineria.infrastructure.adapter.in.desktop.support.Motion;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;

/**
 * Ranking de barras horizontales con etiqueta, valor y barra proporcional animada.
 * Más legible que un BarChart cuando las etiquetas son largas (nombres de clientes,
 * productos, países).
 */
public class BarList extends VBox {

    /**
     * Fila del ranking.
     *
     * @param label  texto principal
     * @param detail texto secundario opcional (puede ser null)
     * @param value  magnitud que determina el largo de la barra
     * @param display valor formateado mostrado a la derecha
     * @param tone   clase de tono opcional para la barra (p. ej. "bar-danger")
     */
    public record Item(String label, String detail, double value, String display, String tone) {
        public Item(String label, double value, String display) {
            this(label, null, value, display, null);
        }
    }

    private Consumer<Item> onOpen;
    private String openHint;

    public BarList() {
        super(12);
        getStyleClass().add("bar-list");
    }

    /**
     * Hace cada fila activable (clic, Intro o Espacio) para abrir el detalle del elemento.
     * Debe llamarse antes de {@link #setItems(List)}.
     *
     * @param action acción con el elemento elegido
     * @param hint   descripción de la acción para tooltip y lector de pantalla
     */
    public void setOnOpen(Consumer<Item> action, String hint) {
        this.onOpen = action;
        this.openHint = hint;
    }

    public void setItems(List<Item> items) {
        getChildren().clear();
        double max = items.stream().mapToDouble(Item::value).max().orElse(1);
        if (max <= 0) {
            max = 1;
        }
        int index = 0;
        for (Item item : items) {
            getChildren().add(row(item, item.value() / max, index++));
        }
    }

    private VBox row(Item item, double fraction, int index) {
        Label label = new Label(item.label());
        label.getStyleClass().add("bar-label");
        label.setMinWidth(0);
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        Label value = new Label(item.display());
        value.getStyleClass().add("bar-value");
        value.setMinWidth(Region.USE_PREF_SIZE);
        HBox head = new HBox(8, label, grow, value);
        head.setAlignment(Pos.BASELINE_LEFT);
        if (item.detail() != null) {
            Label detail = new Label(item.detail());
            detail.getStyleClass().add("bar-detail");
            head.getChildren().add(1, detail);
        }

        Region fill = new Region();
        fill.getStyleClass().add("bar-fill");
        if (item.tone() != null) {
            fill.getStyleClass().add(item.tone());
        }
        StackPane track = new StackPane(fill);
        track.getStyleClass().add("bar-track");
        track.setAlignment(Pos.CENTER_LEFT);
        fill.setMaxWidth(Region.USE_PREF_SIZE);
        double target = Math.max(0.015, fraction);
        if (Motion.isReduced()) {
            fill.prefWidthProperty().bind(track.widthProperty().multiply(target));
        } else {
            javafx.beans.property.DoubleProperty progress = new javafx.beans.property.SimpleDoubleProperty(0);
            fill.prefWidthProperty().bind(track.widthProperty().multiply(progress));
            Timeline t = new Timeline(new KeyFrame(Duration.millis(800),
                    new KeyValue(progress, target, Motion.EASE_OUT)));
            t.setDelay(Duration.millis(80L * index));
            t.play();
        }

        VBox row = new VBox(6, head, track);
        row.getStyleClass().add("bar-row");
        String text = item.label() + ": " + item.display();
        if (onOpen == null) {
            Tooltip.install(row, new Tooltip(text));
            row.setAccessibleText(item.label() + ", " + item.display());
            return row;
        }
        Tooltip.install(row, new Tooltip(text + "\n" + openHint));
        row.setAccessibleText(item.label() + ", " + item.display() + ". " + openHint);
        Interactions.activatable(row, () -> onOpen.accept(item));
        return row;
    }
}
