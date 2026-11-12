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
        centerValue.setText(centerText);
        centerCaption.setText(caption);
        double total = slices.stream().mapToDouble(Slice::value).sum();

        List<PieChart.Data> data = new ArrayList<>();
        legend.getChildren().clear();
        for (Slice s : slices) {
            PieChart.Data d = new PieChart.Data(s.label(), s.value());
            data.add(d);
            d.nodeProperty().addListener((obs, old, node) -> {
                if (node != null) {
                    node.getStyleClass().addAll("donut-slice", s.tone());
                    Tooltip.install(node, new Tooltip(s.label() + ": " + (long) s.value()));
                }
            });
            double pct = total == 0 ? 0 : s.value() * 100 / total;
            legend.getChildren().add(legendRow(s, pct));
        }
        pie.setData(FXCollections.observableArrayList(data));
        // Los nodos ya existentes (reutilizados por el chart) también reciben el tono
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getNode() != null) {
                data.get(i).getNode().getStyleClass().addAll("donut-slice", slices.get(i).tone());
            }
        }
        setAccessibleText(caption + ": " + centerText);
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
        return row;
    }
}
