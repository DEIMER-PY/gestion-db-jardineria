package com.jardineria.infrastructure.adapter.in.desktop.components;

import com.jardineria.infrastructure.adapter.in.desktop.support.Motion;
import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Treemap "squarified" (Bruls, Huizing y van Wijk): cada categoría ocupa un área
 * proporcional a su valor, con rectángulos lo más cuadrados posible para que sean
 * comparables. Las etiquetas se ocultan en las piezas demasiado pequeñas, que
 * conservan su ficha emergente.
 */
public class Treemap extends Region {

    /** Elemento: etiqueta, valor, texto mostrado y clase de tono CSS. */
    public record Item(String label, double value, String display, String tone) {
    }

    /** Rectángulo calculado (público para poder probar el algoritmo sin JavaFX). */
    public record Rect(double x, double y, double w, double h) {
    }

    private static final double GAP = 4;
    private final List<Item> items = new ArrayList<>();
    private final List<StackPane> tiles = new ArrayList<>();

    public Treemap() {
        getStyleClass().add("treemap");
        setMinHeight(200);
        setPrefHeight(260);
        setAccessibleRole(AccessibleRole.LIST_VIEW);
    }

    public void setItems(List<Item> data) {
        items.clear();
        data.stream().filter(i -> i.value() > 0)
                .sorted(Comparator.comparingDouble(Item::value).reversed())
                .forEach(items::add);
        getChildren().clear();
        tiles.clear();
        double total = items.stream().mapToDouble(Item::value).sum();
        int delay = 0;
        for (Item item : items) {
            Label name = new Label(item.label());
            name.getStyleClass().add("tile-name");
            Label value = new Label(item.display());
            value.getStyleClass().add("tile-value");
            Label pct = new Label(total == 0 ? "" : String.format(java.util.Locale.forLanguageTag("es-ES"),
                    "%.0f %%", item.value() * 100 / total));
            pct.getStyleClass().add("tile-pct");
            VBox text = new VBox(1, name, value, pct);
            text.setAlignment(Pos.TOP_LEFT);
            text.setMouseTransparent(true);
            StackPane tile = new StackPane(text);
            StackPane.setAlignment(text, Pos.TOP_LEFT);
            tile.getStyleClass().addAll("tile", item.tone());
            tile.setManaged(false);
            String description = item.label() + ": " + item.display() + " (" + pct.getText() + ")";
            Tooltip tip = new Tooltip(description);
            tip.setShowDelay(Duration.millis(120));
            Tooltip.install(tile, tip);
            tile.setAccessibleText(description);
            tile.setFocusTraversable(true);
            tiles.add(tile);
            getChildren().add(tile);
            if (!Motion.isReduced()) {
                tile.setOpacity(0);
                FadeTransition ft = new FadeTransition(Duration.millis(420), tile);
                ft.setToValue(1);
                ft.setDelay(Duration.millis(delay));
                ft.play();
                delay += 70;
            }
        }
        setAccessibleText("Distribución por categoría: " + items.size() + " elementos");
        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        List<Rect> rects = squarify(items.stream().mapToDouble(Item::value).toArray(), 0, 0, getWidth(), getHeight());
        for (int i = 0; i < tiles.size() && i < rects.size(); i++) {
            Rect r = rects.get(i);
            StackPane tile = tiles.get(i);
            double w = Math.max(0, r.w() - GAP);
            double h = Math.max(0, r.h() - GAP);
            tile.resizeRelocate(r.x() + GAP / 2, r.y() + GAP / 2, w, h);
            VBox text = (VBox) tile.getChildren().get(0);
            boolean roomy = w >= 92 && h >= 58;
            boolean tiny = w < 56 || h < 34;
            text.setVisible(!tiny);
            text.getChildren().get(2).setVisible(roomy);
            text.getChildren().get(1).setVisible(h >= 46);
        }
    }

    /** Algoritmo squarified. Los valores deben venir ordenados de mayor a menor. */
    public static List<Rect> squarify(double[] values, double x, double y, double w, double h) {
        List<Rect> out = new ArrayList<>();
        double total = 0;
        for (double v : values) {
            total += v;
        }
        if (values.length == 0 || total <= 0 || w <= 0 || h <= 0) {
            return out;
        }
        double scale = (w * h) / total;
        double[] areas = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            areas[i] = values[i] * scale;
        }
        int start = 0;
        double rx = x;
        double ry = y;
        double rw = w;
        double rh = h;
        while (start < areas.length) {
            double side = Math.min(rw, rh);
            int end = start + 1;
            double best = worst(areas, start, end, side);
            while (end < areas.length) {
                double next = worst(areas, start, end + 1, side);
                if (next > best) {
                    break;
                }
                best = next;
                end++;
            }
            double rowArea = 0;
            for (int i = start; i < end; i++) {
                rowArea += areas[i];
            }
            if (rw >= rh) {
                double colW = rowArea / rh;
                double cy = ry;
                for (int i = start; i < end; i++) {
                    double ch = areas[i] / colW;
                    out.add(new Rect(rx, cy, colW, ch));
                    cy += ch;
                }
                rx += colW;
                rw -= colW;
            } else {
                double rowH = rowArea / rw;
                double cx = rx;
                for (int i = start; i < end; i++) {
                    double cw = areas[i] / rowH;
                    out.add(new Rect(cx, ry, cw, rowH));
                    cx += cw;
                }
                ry += rowH;
                rh -= rowH;
            }
            start = end;
        }
        return out;
    }

    /** Peor relación de aspecto de una fila con las áreas [start, end) sobre un lado dado. */
    private static double worst(double[] areas, int start, int end, double side) {
        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = 0;
        for (int i = start; i < end; i++) {
            sum += areas[i];
            min = Math.min(min, areas[i]);
            max = Math.max(max, areas[i]);
        }
        double s2 = side * side;
        double sum2 = sum * sum;
        return Math.max(s2 * max / sum2, sum2 / (s2 * min));
    }
}
