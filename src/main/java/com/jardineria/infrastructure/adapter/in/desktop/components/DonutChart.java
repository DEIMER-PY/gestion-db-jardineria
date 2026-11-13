package com.jardineria.infrastructure.adapter.in.desktop.components;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;

/**
 * Gráfico de dona: PieChart nativo con hueco central (total destacado) y leyenda
 * propia con porcentajes debajo (así nunca se trunca en columnas estrechas). Cada segmento usa la clase de tono indicada en {@link Slice}.
 */
public class DonutChart extends VBox {

    /** Segmento: etiqueta, valor y tono CSS (p. ej. "tone-jade"). */
    public record Slice(String label, double value, String tone) {
    }

    private final PieChart pie = new PieChart();
    private final Circle hole = new Circle();
    private final Label centerValue = new Label();
    private final Label centerCaption = new Label();
    private final VBox legend = new VBox(10);
    private final List<PieChart.Data> current = new ArrayList<>();
    private String defaultValue = "";
    private String defaultCaption = "";
    private double total;

    public DonutChart() {
        super(18);
        getStyleClass().add("donut");
        setAlignment(Pos.TOP_CENTER);

        pie.setLabelsVisible(false);
        pie.setLegendVisible(false);
        pie.setStartAngle(90);
        pie.setClockwise(true);
        pie.setAnimated(true);
        pie.getStyleClass().add("donut-pie");
        pie.setMinSize(170, 170);
        pie.setPrefSize(200, 200);
        pie.setMaxSize(220, 220);

        hole.getStyleClass().add("donut-hole");
        hole.radiusProperty().bind(pie.widthProperty().multiply(0.27));
        centerValue.getStyleClass().add("donut-value");
        centerCaption.getStyleClass().add("donut-caption");
        VBox center = new VBox(0, centerValue, centerCaption);
        center.setAlignment(Pos.CENTER);
        center.setMouseTransparent(true);
        StackPane ring = new StackPane(pie, hole, center);
        ring.setMinSize(170, 170);

        legend.getStyleClass().add("donut-legend");
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setMaxWidth(Double.MAX_VALUE);
        getChildren().addAll(ring, legend);
    }

    public void setData(List<Slice> slices, String centerText, String caption) {
        defaultValue = centerText;
        defaultCaption = caption;
        centerValue.setText(centerText);
        centerCaption.setText(caption);
        total = slices.stream().mapToDouble(Slice::value).sum();
        current.clear();

        List<PieChart.Data> data = new ArrayList<>();
        legend.getChildren().clear();
        for (Slice s : slices) {
            PieChart.Data d = new PieChart.Data(s.label(), s.value());
            data.add(d);
            int index = data.size() - 1;
            d.nodeProperty().addListener((obs, old, node) -> {
                if (node != null) {
                    decorate(node, s, index);
                }
            });
            double pct = total == 0 ? 0 : s.value() * 100 / total;
            HBox row = legendRow(s, pct);
            row.setOnMouseEntered(e -> focusSlice(index, s));
            row.setOnMouseExited(e -> focusSlice(-1, null));
            legend.getChildren().add(row);
        }
        current.addAll(data);
        pie.setData(FXCollections.observableArrayList(data));
        // Los nodos ya existentes (reutilizados por el chart) también reciben el tono
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getNode() != null) {
                decorate(data.get(i).getNode(), slices.get(i), i);
            }
        }
        setAccessibleText(caption + ": " + centerText);
    }

    private void decorate(javafx.scene.Node node, Slice s, int index) {
        if (!node.getStyleClass().contains("donut-slice")) {
            node.getStyleClass().addAll("donut-slice", s.tone());
        }
        Tooltip.install(node, new Tooltip(s.label() + ": " + (long) s.value()));
        node.setOnMouseEntered(e -> focusSlice(index, s));
        node.setOnMouseExited(e -> focusSlice(-1, null));
    }

    /** Atenúa el resto de segmentos y muestra el valor del elegido en el centro ({@code -1} restaura). */
    private void focusSlice(int index, Slice s) {
        for (int i = 0; i < current.size(); i++) {
            javafx.scene.Node n = current.get(i).getNode();
            if (n != null) {
                n.setOpacity(index < 0 || i == index ? 1 : 0.32);
            }
        }
        for (int i = 0; i < legend.getChildren().size(); i++) {
            legend.getChildren().get(i).setOpacity(index < 0 || i == index ? 1 : 0.5);
        }
        if (s == null) {
            centerValue.setText(defaultValue);
            centerCaption.setText(defaultCaption);
        } else {
            centerValue.setText(String.format(java.util.Locale.forLanguageTag("es-ES"), "%,.0f", s.value()));
            centerCaption.setText(s.label().toLowerCase(java.util.Locale.ROOT) + " · "
                    + String.format(java.util.Locale.forLanguageTag("es-ES"), "%.0f %%", total == 0 ? 0 : s.value() * 100 / total));
        }
    }

    private HBox legendRow(Slice s, double pct) {
        Region swatch = new Region();
        swatch.getStyleClass().addAll("legend-swatch", s.tone());
        Label name = new Label(s.label());
        name.getStyleClass().add("legend-label");
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        Label value = new Label(String.format(java.util.Locale.forLanguageTag("es-ES"), "%,.0f", s.value()));
        value.getStyleClass().add("legend-value");
        Label percent = new Label(String.format(java.util.Locale.forLanguageTag("es-ES"), "%.1f %%", pct));
        percent.getStyleClass().add("legend-percent");
        HBox row = new HBox(10, swatch, name, grow, value, percent);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("donut-legend-row");
        return row;
    }
}
