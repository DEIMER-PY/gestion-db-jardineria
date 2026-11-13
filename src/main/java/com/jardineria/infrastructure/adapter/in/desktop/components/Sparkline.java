package com.jardineria.infrastructure.adapter.in.desktop.components;

import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import java.util.function.DoubleFunction;

/**
 * Microtendencia vectorial (línea + área degradada + punto final).
 * Al ser formas JavaFX, toma los colores del CSS ({@code -jd-tone}) y se adapta
 * automáticamente al tema y a cualquier tamaño.
 * <p>
 * Si se le dan etiquetas ({@link #setValues(double[], String[], DoubleFunction)}), al pasar
 * el ratón muestra el valor del punto más cercano con una guía vertical.
 */
public class Sparkline extends Region {
    private static final double PAD = 4;

    private final Path area = new Path();
    private final Path line = new Path();
    private final Circle dot = new Circle(3.2);
    private final Circle hoverDot = new Circle(4);
    private final Line guide = new Line();
    private final Label bubble = new Label();
    private double[] values = new double[0];
    private String[] labels;
    private DoubleFunction<String> format = v -> String.valueOf(Math.round(v));
    private double[] xs = new double[0];
    private double[] ys = new double[0];

    public Sparkline() {
        getStyleClass().add("sparkline");
        area.getStyleClass().add("sparkline-area");
        line.getStyleClass().add("sparkline-line");
        dot.getStyleClass().add("sparkline-dot");
        hoverDot.getStyleClass().add("sparkline-dot");
        guide.getStyleClass().add("sparkline-guide");
        bubble.getStyleClass().add("sparkline-bubble");
        for (javafx.scene.Node n : new javafx.scene.Node[]{area, line, dot, hoverDot, guide, bubble}) {
            n.setManaged(false);
        }
        hoverDot.setVisible(false);
        guide.setVisible(false);
        bubble.setVisible(false);
        bubble.setMouseTransparent(true);
        getChildren().addAll(area, line, guide, dot, hoverDot, bubble);
        setMinHeight(34);
        setPrefHeight(42);

        setOnMouseMoved(e -> hover(e.getX()));
        setOnMouseExited(e -> {
            hoverDot.setVisible(false);
            guide.setVisible(false);
            bubble.setVisible(false);
        });
    }

    public void setValues(double[] newValues) {
        this.values = newValues == null ? new double[0] : newValues.clone();
        requestLayout();
    }

    /** Valores con etiqueta por punto (p. ej. meses) y formato para la burbuja de hover. */
    public void setValues(double[] newValues, String[] pointLabels, DoubleFunction<String> valueFormat) {
        this.labels = pointLabels == null ? null : pointLabels.clone();
        this.format = valueFormat == null ? format : valueFormat;
        setValues(newValues);
    }

    private void hover(double x) {
        if (xs.length < 2 || labels == null) {
            return;
        }
        int idx = 0;
        double best = Double.MAX_VALUE;
        for (int i = 0; i < xs.length; i++) {
            double d = Math.abs(xs[i] - x);
            if (d < best) {
                best = d;
                idx = i;
            }
        }
        hoverDot.setCenterX(xs[idx]);
        hoverDot.setCenterY(ys[idx]);
        hoverDot.setVisible(true);
        guide.setStartX(Math.round(xs[idx]) + 0.5);
        guide.setEndX(Math.round(xs[idx]) + 0.5);
        guide.setStartY(0);
        guide.setEndY(getHeight());
        guide.setVisible(true);
        String label = idx < labels.length ? labels[idx] + " · " : "";
        bubble.setText(label + format.apply(values[idx]));
        bubble.applyCss();
        bubble.autosize();
        double bx = Math.max(0, Math.min(getWidth() - bubble.getWidth(), xs[idx] - bubble.getWidth() / 2));
        bubble.relocate(bx, -bubble.getHeight() - 4);
        bubble.setVisible(true);
    }

    @Override
    protected void layoutChildren() {
        area.getElements().clear();
        line.getElements().clear();
        double w = getWidth();
        double h = getHeight();
        if (values.length < 2 || w <= 0 || h <= 0) {
            dot.setVisible(false);
            xs = new double[0];
            ys = new double[0];
            return;
        }
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double v : values) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        double range = max - min == 0 ? 1 : max - min;
        double stepX = (w - PAD * 2) / (values.length - 1);

        xs = new double[values.length];
        ys = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            xs[i] = PAD + i * stepX;
            ys[i] = PAD + (h - PAD * 2) * (1 - (values[i] - min) / range);
        }
        line.getElements().add(new MoveTo(xs[0], ys[0]));
        area.getElements().add(new MoveTo(xs[0], h));
        area.getElements().add(new LineTo(xs[0], ys[0]));
        for (int i = 1; i < values.length; i++) {
            line.getElements().add(new LineTo(xs[i], ys[i]));
            area.getElements().add(new LineTo(xs[i], ys[i]));
        }
        area.getElements().add(new LineTo(xs[xs.length - 1], h));
        area.getElements().add(new ClosePath());

        dot.setVisible(true);
        dot.setCenterX(xs[xs.length - 1]);
        dot.setCenterY(ys[ys.length - 1]);
    }
}
