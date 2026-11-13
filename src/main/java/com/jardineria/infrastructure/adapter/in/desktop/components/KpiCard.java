package com.jardineria.infrastructure.adapter.in.desktop.components;

import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import com.jardineria.infrastructure.adapter.in.desktop.support.Motion;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;

import java.util.function.DoubleFunction;

/**
 * Tarjeta de indicador clave: ícono con tono, título, cifra con contador animado,
 * pie descriptivo y un elemento visual opcional (sparkline o barra de progreso).
 * Es navegable con teclado (Enter / Espacio) cuando tiene acción asociada.
 */
public class KpiCard extends VBox {
    /** Tono de color de la tarjeta (definido en app.css como {@code .tone-*}). */
    public enum Tone { JADE, LIME, AMBER, SKY, CORAL, LILAC }

    private final Label value = new Label("—");
    private final Label caption = new Label();
    private final Label chip = new Label();
    private final StackPane footer = new StackPane();
    private final DoubleProperty animated = new SimpleDoubleProperty();
    private DoubleFunction<String> formatter = v -> String.valueOf(Math.round(v));
    private Timeline counter;

    public KpiCard(Ikon icon, String title, Tone tone) {
        super(10);
        getStyleClass().addAll("panel", "kpi", "tone-" + tone.name().toLowerCase());
        setMinWidth(0);

        StackPane badge = new StackPane(Icons.of(icon, 18));
        badge.getStyleClass().add("kpi-badge");
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("kpi-title");
        chip.getStyleClass().add("kpi-chip");
        chip.setVisible(false);
        chip.setManaged(false);
        lblTitle.setMinWidth(Region.USE_PREF_SIZE);
        HBox head = new HBox(10, badge, lblTitle);
        head.setAlignment(Pos.CENTER_LEFT);

        value.getStyleClass().add("kpi-value");
        value.setMinWidth(0);
        caption.getStyleClass().add("kpi-caption");
        caption.setWrapText(true);
        footer.getStyleClass().add("kpi-footer");

        chip.setMinWidth(Region.USE_PREF_SIZE);
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox valueRow = new HBox(8, value, grow, chip);
        valueRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(head, valueRow, caption, footer);
        VBox.setVgrow(footer, Priority.ALWAYS);
        footer.setAlignment(Pos.BOTTOM_LEFT);

        animated.addListener((obs, o, n) -> value.setText(formatter.apply(n.doubleValue())));
        setAccessibleRole(AccessibleRole.TEXT);
        setAccessibleText(title);
    }

    /** Establece el valor con animación de conteo ascendente. */
    public void setValue(double target, DoubleFunction<String> format) {
        this.formatter = format;
        if (counter != null) {
            counter.stop();
        }
        if (Motion.isReduced()) {
            animated.set(target);
            value.setText(format.apply(target));
        } else {
            animated.set(0);
            counter = new Timeline(new KeyFrame(Duration.millis(900),
                    new KeyValue(animated, target, Motion.EASE_OUT)));
            counter.play();
        }
        setAccessibleText(((Label) ((HBox) getChildren().get(0)).getChildren().get(1)).getText()
                + ": " + format.apply(target) + ". " + caption.getText());
    }

    public void setCaption(String text) {
        caption.setText(text);
    }

    /** Chip de variación (p. ej. "+12 %"); {@code positive} decide el color. */
    public void setChip(String text, boolean positive) {
        chip.setText(text);
        chip.setGraphic(Icons.of(positive ? Feather.TRENDING_UP : Feather.TRENDING_DOWN, 12));
        chip.getStyleClass().removeAll("kpi-chip-up", "kpi-chip-down");
        chip.getStyleClass().add(positive ? "kpi-chip-up" : "kpi-chip-down");
        chip.setVisible(true);
        chip.setManaged(true);
    }

    public void setFooter(Node node) {
        footer.getChildren().setAll(node);
    }

    /** Convierte la tarjeta en un acceso directo (ratón y teclado). */
    public void setOnOpen(Runnable action) {
        getStyleClass().add("interactive");
        setFocusTraversable(true);
        setAccessibleRole(AccessibleRole.BUTTON);
        setOnMouseClicked(e -> action.run());
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                action.run();
            }
        });
    }
}
