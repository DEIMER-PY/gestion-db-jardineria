package com.jardineria.infrastructure.adapter.in.desktop.components;

import com.jardineria.infrastructure.adapter.in.common.Formatters;
import com.jardineria.infrastructure.adapter.in.desktop.support.Series;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Barras apiladas de pedidos por mes y estado (Entregado / Pendiente / Rechazado)
 * con leyenda propia y tooltips por segmento.
 */
public class OrdersByStatusChart extends VBox {
    private static final Map<String, String> TONES = Map.of(
            "Entregado", "tone-jade",
            "Pendiente", "tone-amber",
            "Rechazado", "tone-coral");

    private final CategoryAxis xAxis = new CategoryAxis();
    private final NumberAxis yAxis = new NumberAxis();
    private final StackedBarChart<String, Number> chart = new StackedBarChart<>(xAxis, yAxis);

    public OrdersByStatusChart() {
        super(10);
        getStyleClass().add("orders-chart");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCategoryGap(14);
        chart.setVerticalGridLinesVisible(false);
        chart.setHorizontalZeroLineVisible(false);
        chart.getStyleClass().add("jd-chart");
        chart.setMinHeight(240);
        chart.setPrefHeight(290);
        yAxis.setMinorTickVisible(false);
        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis) {
            @Override
            public String toString(Number n) {
                return n.doubleValue() == Math.rint(n.doubleValue()) ? String.valueOf(n.intValue()) : "";
            }
        });
        xAxis.setTickMarkVisible(false);
        yAxis.setTickMarkVisible(false);

        HBox legend = new HBox(18);
        legend.setAlignment(Pos.CENTER_LEFT);
        for (String estado : Series.ESTADOS) {
            Region swatch = new Region();
            swatch.getStyleClass().addAll("legend-swatch", TONES.get(estado));
            Label lbl = new Label(estado);
            lbl.getStyleClass().add("legend-label");
            legend.getChildren().add(new HBox(8, swatch, lbl));
            ((HBox) legend.getChildren().get(legend.getChildren().size() - 1)).setAlignment(Pos.CENTER_LEFT);
        }
        getChildren().addAll(legend, chart);
        VBox.setVgrow(chart, Priority.ALWAYS);
    }

    public void setData(Map<YearMonth, Map<String, Long>> matrix) {
        List<XYChart.Series<String, Number>> all = new ArrayList<>();
        for (String estado : Series.ESTADOS) {
            XYChart.Series<String, Number> s = new XYChart.Series<>();
            s.setName(estado);
            matrix.forEach((ym, row) -> {
                s.getData().add(new XYChart.Data<>(Formatters.month(ym), row.getOrDefault(estado, 0L)));
            });
            all.add(s);
        }
        xAxis.getCategories().setAll(matrix.keySet().stream().map(Formatters::month).toList());
        chart.getData().setAll(all);
        // StackedBarChart reasigna las clases de cada barra al crearla (setAll), por eso
        // el tono se aplica después de añadir las series y no en un listener de nodo.
        for (XYChart.Series<String, Number> s : all) {
            String tone = TONES.get(s.getName());
            for (XYChart.Data<String, Number> d : s.getData()) {
                if (d.getNode() != null) {
                    d.getNode().getStyleClass().addAll("status-bar", tone);
                    Tooltip.install(d.getNode(), new Tooltip(s.getName() + " · " + d.getXValue() + ": " + d.getYValue()));
                }
            }
        }
        long total = matrix.values().stream().flatMap(r -> r.values().stream()).mapToLong(Long::longValue).sum();
        setAccessibleText("Pedidos por mes y estado: " + total + " pedidos en " + matrix.size() + " meses");
    }
}
