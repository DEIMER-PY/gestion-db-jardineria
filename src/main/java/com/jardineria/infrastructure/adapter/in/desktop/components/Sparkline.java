package com.jardineria.infrastructure.adapter.in.desktop.components;

import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

/**
 * Microtendencia vectorial (línea + área degradada + punto final).
 * Al ser formas JavaFX, toma los colores del CSS ({@code -jd-tone}) y se adapta
 * automáticamente al tema claro/oscuro y a cualquier tamaño.
 */
public class Sparkline extends Region {
    private final Path area = new Path();
    private final Path line = new Path();
    private final Circle dot = new Circle(3.2);
    private double[] values = new double[0];

    public Sparkline() {
        getStyleClass().add("sparkline");
        area.getStyleClass().add("sparkline-area");
        line.getStyleClass().add("sparkline-line");
        dot.getStyleClass().add("sparkline-dot");
        area.setManaged(false);
        line.setManaged(false);
        dot.setManaged(false);
        getChildren().addAll(area, line, dot);
        setMinHeight(34);
        setPrefHeight(42);
    }

    public void setValues(double[] newValues) {
        this.values = newValues == null ? new double[0] : newValues.clone();
        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        area.getElements().clear();
        line.getElements().clear();
        double w = getWidth();
        double h = getHeight();
        if (values.length < 2 || w <= 0 || h <= 0) {
            dot.setVisible(false);
            return;
        }
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double v : values) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        double range = max - min == 0 ? 1 : max - min;
        double pad = 4;
        double stepX = (w - pad * 2) / (values.length - 1);

        double[] xs = new double[values.length];
        double[] ys = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            xs[i] = pad + i * stepX;
            ys[i] = pad + (h - pad * 2) * (1 - (values[i] - min) / range);
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
