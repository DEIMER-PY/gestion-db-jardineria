package com.jardineria.infrastructure.adapter.in.desktop.components;

import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

import java.util.HashMap;
import java.util.Map;

/**
 * Rejilla adaptable: calcula cuántas columnas caben según el ancho disponible y el
 * ancho mínimo de celda, y permite que un hijo ocupe varias columnas ({@link #setSpan}).
 * Así ninguna tarjeta se comprime por debajo de su tamaño legible: en ventanas
 * estrechas los elementos bajan a la siguiente fila en lugar de colapsar.
 */
public class ResponsiveGrid extends Pane {
    private final double minCellWidth;
    private final int maxColumns;
    private final double gap;
    private final Map<Node, Integer> spans = new HashMap<>();

    public ResponsiveGrid(double minCellWidth, int maxColumns, double gap) {
        this.minCellWidth = minCellWidth;
        this.maxColumns = maxColumns;
        this.gap = gap;
        getStyleClass().add("responsive-grid");
    }

    /** Número de columnas que ocupa un hijo (se limita a las columnas disponibles). */
    public ResponsiveGrid setSpan(Node child, int columns) {
        spans.put(child, Math.max(1, columns));
        requestLayout();
        return this;
    }

    public ResponsiveGrid add(Node child, int columns) {
        getChildren().add(child);
        return setSpan(child, columns);
    }

    private int columnsFor(double width) {
        int cols = (int) Math.floor((width + gap) / (minCellWidth + gap));
        return balance(Math.max(1, Math.min(maxColumns, cols)));
    }

    /**
     * Si todos los hijos ocupan una columna, reduce columnas cuando así se evitan
     * huecos en la última fila (p. ej. 4 tarjetas en 3 columnas pasan a 2 × 2).
     */
    private int balance(int cols) {
        java.util.List<Node> children = getManagedChildren();
        int n = children.size();
        if (n <= cols || children.stream().anyMatch(c -> spans.getOrDefault(c, 1) > 1)) {
            return cols;
        }
        int best = cols;
        int bestWaste = (cols - n % cols) % cols;
        for (int c = cols - 1; c >= Math.max(2, (cols + 1) / 2) && bestWaste > 0; c--) {
            int waste = (c - n % c) % c;
            if (waste < bestWaste) {
                best = c;
                bestWaste = waste;
            }
        }
        return best;
    }

    @Override
    protected double computePrefHeight(double width) {
        double w = width > 0 ? width : getWidth();
        if (w <= 0) {
            w = minCellWidth * maxColumns;
        }
        return layoutRows(w - snappedLeftInset() - snappedRightInset(), false) + snappedTopInset() + snappedBottomInset();
    }

    /** La altura depende del ancho disponible (las celdas bajan de fila al estrechar). */
    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    @Override
    protected double computeMinWidth(double height) {
        return minCellWidth + snappedLeftInset() + snappedRightInset();
    }

    @Override
    protected void layoutChildren() {
        layoutRows(getWidth() - snappedLeftInset() - snappedRightInset(), true);
    }

    private double layoutRows(double width, boolean apply) {
        int cols = columnsFor(width);
        double cellW = (width - gap * (cols - 1)) / cols;
        double y = snappedTopInset();
        int col = 0;
        double rowHeight = 0;
        java.util.List<Node> row = new java.util.ArrayList<>();
        java.util.List<double[]> boxes = new java.util.ArrayList<>();

        for (Node child : getManagedChildren()) {
            int span = Math.min(cols, spans.getOrDefault(child, 1));
            if (col + span > cols) {
                if (apply) {
                    place(row, boxes, rowHeight);
                }
                row.clear();
                boxes.clear();
                y += rowHeight + gap;
                col = 0;
                rowHeight = 0;
            }
            double w = cellW * span + gap * (span - 1);
            double x = snappedLeftInset() + col * (cellW + gap);
            double h = child.prefHeight(w);
            rowHeight = Math.max(rowHeight, h);
            row.add(child);
            boxes.add(new double[]{x, y, w});
            col += span;
        }
        if (apply && !row.isEmpty()) {
            place(row, boxes, rowHeight);
        }
        return y + rowHeight - snappedTopInset();
    }

    private void place(java.util.List<Node> row, java.util.List<double[]> boxes, double rowHeight) {
        for (int i = 0; i < row.size(); i++) {
            double[] b = boxes.get(i);
            layoutInArea(row.get(i), b[0], b[1], b[2], rowHeight, 0, HPos.LEFT, VPos.TOP);
        }
    }
}
