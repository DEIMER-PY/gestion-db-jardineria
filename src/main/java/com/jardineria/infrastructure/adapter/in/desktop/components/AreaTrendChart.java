package com.jardineria.infrastructure.adapter.in.desktop.components;

import com.jardineria.infrastructure.adapter.in.desktop.support.Motion;
import com.jardineria.infrastructure.adapter.in.desktop.support.Series;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleFunction;

/**
 * Gráfica de áreas suavizadas (curvas Catmull-Rom) para comparar series temporales.
 * <ul>
 *   <li>Rejilla con pasos redondos y etiquetas compactas en el eje Y.</li>
 *   <li>Cursor interactivo: al pasar el ratón (o con ← → desde el teclado) muestra una guía
 *       vertical, resalta cada serie y abre una ficha con los valores del mes.</li>
 *   <li>Animación de revelado de izquierda a derecha (se omite con "reducir movimiento").</li>
 * </ul>
 * Los colores salen de las clases de tono ({@code tone-*}) de cada serie.
 */
public class AreaTrendChart extends VBox {

    /** Serie: nombre, clase de tono CSS y un valor por etiqueta del eje X. */
    public record Serie(String name, String tone, double[] values) {
    }

    private final HBox legend = new HBox(18);
    private final Plot plot = new Plot();
    private List<String> labels = List.of();
    private List<Serie> series = List.of();
    private DoubleFunction<String> axisFormat = v -> String.valueOf(Math.round(v));
    private DoubleFunction<String> valueFormat = v -> String.valueOf(Math.round(v));

    public AreaTrendChart() {
        super(12);
        getStyleClass().add("trend-chart");
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.getStyleClass().add("trend-legend");
        getChildren().addAll(legend, plot);
        VBox.setVgrow(plot, javafx.scene.layout.Priority.ALWAYS);
    }

    /**
     * @param xLabels     etiquetas del eje X (una por punto)
     * @param data        series a dibujar (mismo número de puntos que etiquetas)
     * @param axisFmt     formato compacto del eje Y
     * @param valueFmt    formato completo de la ficha emergente
     */
    public void setData(List<String> xLabels, List<Serie> data,
                        DoubleFunction<String> axisFmt, DoubleFunction<String> valueFmt) {
        this.labels = List.copyOf(xLabels);
        this.series = List.copyOf(data);
        this.axisFormat = axisFmt;
        this.valueFormat = valueFmt;
        legend.getChildren().clear();
        StringBuilder a11y = new StringBuilder("Gráfica de tendencia de " + labels.size() + " meses. ");
        for (Serie s : series) {
            Region swatch = new Region();
            swatch.getStyleClass().addAll("legend-swatch", "legend-line", s.tone());
            Label name = new Label(s.name());
            name.getStyleClass().add("legend-label");
            double total = 0;
            for (double v : s.values()) {
                total += v;
            }
            Label sum = new Label(valueFormat.apply(total));
            sum.getStyleClass().add("legend-value");
            HBox item = new HBox(8, swatch, name, sum);
            item.setAlignment(Pos.CENTER_LEFT);
            legend.getChildren().add(item);
            a11y.append(s.name()).append(": total ").append(sum.getText()).append(". ");
        }
        plot.rebuild();
        plot.setAccessibleText(a11y + "Usa las flechas izquierda y derecha para recorrer los meses.");
    }

    /** Muestra la ficha de un punto concreto (navegación por teclado y capturas de documentación). */
    public void highlight(int index) {
        plot.showHover(index);
    }

    /** Lienzo de la gráfica: dibuja en {@link #layoutChildren()} para adaptarse a cualquier tamaño. */
    private final class Plot extends Region {
        private static final double LEFT = 64;
        private static final double RIGHT = 14;
        private static final double TOP = 12;
        private static final double BOTTOM = 26;
        private static final int TICKS = 4;

        private final Path grid = new Path();
        private final List<Label> yLabels = new ArrayList<>();
        private final List<Label> xLabels = new ArrayList<>();
        private final List<Path> areas = new ArrayList<>();
        private final List<Path> lines = new ArrayList<>();
        private final List<Circle> hoverDots = new ArrayList<>();
        private final Line cross = new Line();
        private final VBox tip = new VBox(4);
        private final DoubleProperty progress = new SimpleDoubleProperty(1);
        private int hover = -1;

        Plot() {
            getStyleClass().add("trend-plot");
            setMinHeight(200);
            setPrefHeight(270);
            setFocusTraversable(true);
            setAccessibleRole(AccessibleRole.IMAGE_VIEW);
            grid.getStyleClass().add("trend-grid");
            cross.getStyleClass().add("trend-cross");
            tip.getStyleClass().add("trend-tip");
            tip.setMouseTransparent(true);
            tip.setManaged(false);
            cross.setManaged(false);
            grid.setManaged(false);
            cross.setVisible(false);
            tip.setVisible(false);
            progress.addListener((o, a, b) -> requestLayout());

            setOnMouseMoved(e -> showHover(indexAt(e.getX())));
            setOnMouseExited(e -> hideHover());
            setOnKeyPressed(e -> {
                if (labels.isEmpty()) {
                    return;
                }
                if (e.getCode() == KeyCode.RIGHT) {
                    showHover(Math.min(labels.size() - 1, Math.max(0, hover + 1)));
                    e.consume();
                } else if (e.getCode() == KeyCode.LEFT) {
                    showHover(Math.max(0, hover < 0 ? labels.size() - 1 : hover - 1));
                    e.consume();
                } else if (e.getCode() == KeyCode.ESCAPE) {
                    hideHover();
                }
            });
            focusedProperty().addListener((o, was, now) -> {
                if (!now) {
                    hideHover();
                }
            });
        }

        void rebuild() {
            getChildren().clear();
            yLabels.clear();
            xLabels.clear();
            areas.clear();
            lines.clear();
            hoverDots.clear();
            getChildren().add(grid);
            for (Serie s : series) {
                Path area = new Path();
                area.getStyleClass().addAll("trend-area", s.tone());
                area.setManaged(false);
                Path line = new Path();
                line.getStyleClass().addAll("trend-line", s.tone());
                line.setManaged(false);
                Circle dot = new Circle(5);
                dot.getStyleClass().addAll("trend-dot", s.tone());
                dot.setManaged(false);
                dot.setVisible(false);
                areas.add(area);
                lines.add(line);
                hoverDots.add(dot);
            }
            getChildren().addAll(areas);
            getChildren().addAll(lines);
            getChildren().add(cross);
            getChildren().addAll(hoverDots);
            for (int i = 0; i <= TICKS; i++) {
                Label l = new Label();
                l.getStyleClass().add("trend-axis");
                l.setManaged(false);
                yLabels.add(l);
            }
            for (String text : labels) {
                Label l = new Label(text);
                l.getStyleClass().add("trend-axis");
                l.setManaged(false);
                xLabels.add(l);
            }
            getChildren().addAll(yLabels);
            getChildren().addAll(xLabels);
            getChildren().add(tip);
            hover = -1;
            animate();
            requestLayout();
        }

        private void animate() {
            if (Motion.isReduced()) {
                progress.set(1);
                return;
            }
            progress.set(0);
            new Timeline(new KeyFrame(Duration.millis(1100),
                    new KeyValue(progress, 1, Motion.EASE_OUT))).play();
        }

        private double max() {
            double max = 0;
            for (Serie s : series) {
                for (double v : s.values()) {
                    max = Math.max(max, v);
                }
            }
            return max;
        }

        private double step() {
            int n = labels.size();
            return n <= 1 ? 0 : (getWidth() - LEFT - RIGHT) / (n - 1);
        }

        private int indexAt(double x) {
            if (labels.isEmpty()) {
                return -1;
            }
            double step = step();
            if (step <= 0) {
                return 0;
            }
            int idx = (int) Math.round((x - LEFT) / step);
            return Math.max(0, Math.min(labels.size() - 1, idx));
        }

        @Override
        protected void layoutChildren() {
            double w = getWidth();
            double h = getHeight();
            int n = labels.size();
            grid.getElements().clear();
            if (n == 0 || w <= LEFT + RIGHT || h <= TOP + BOTTOM) {
                areas.forEach(p -> p.getElements().clear());
                lines.forEach(p -> p.getElements().clear());
                return;
            }
            double plotH = h - TOP - BOTTOM;
            double base = TOP + plotH;
            double stepY = Series.niceStep(max(), TICKS);
            double top = stepY * TICKS;

            for (int i = 0; i <= TICKS; i++) {
                double y = base - plotH * i / TICKS;
                grid.getElements().addAll(new MoveTo(LEFT, snap(y)), new LineTo(w - RIGHT, snap(y)));
                Label l = yLabels.get(i);
                l.setText(axisFormat.apply(stepY * i));
                l.autosize();
                l.relocate(LEFT - 10 - l.getWidth(), y - l.getHeight() / 2);
            }

            double step = n == 1 ? 0 : (w - LEFT - RIGHT) / (n - 1);
            int every = Math.max(1, (int) Math.ceil(n / Math.max(1, (w - LEFT - RIGHT) / 58)));
            for (int i = 0; i < n; i++) {
                Label l = xLabels.get(i);
                l.autosize();
                double x = LEFT + i * step;
                l.relocate(x - l.getWidth() / 2, base + 7);
                l.setVisible(i % every == 0 || i == n - 1);
            }

            double clipW = LEFT + (w - LEFT) * progress.get();
            for (int s = 0; s < series.size(); s++) {
                double[] v = series.get(s).values();
                double[] xs = new double[n];
                double[] ys = new double[n];
                for (int i = 0; i < n; i++) {
                    xs[i] = LEFT + i * step;
                    double value = i < v.length ? v[i] : 0;
                    ys[i] = base - (top == 0 ? 0 : plotH * value / top);
                }
                Path line = lines.get(s);
                Path area = areas.get(s);
                line.getElements().setAll(smooth(xs, ys, TOP, base));
                area.getElements().setAll(smooth(xs, ys, TOP, base));
                area.getElements().add(new LineTo(xs[n - 1], base));
                area.getElements().add(new LineTo(xs[0], base));
                area.getElements().add(new ClosePath());
                line.setClip(new Rectangle(0, 0, clipW, h));
                area.setClip(new Rectangle(0, 0, clipW, h));
            }
            if (hover >= 0) {
                positionHover(hover);
            }
        }

        /** Curva Catmull-Rom convertida a Bézier cúbicas, con los puntos de control acotados al área. */
        private List<javafx.scene.shape.PathElement> smooth(double[] xs, double[] ys, double minY, double maxY) {
            List<javafx.scene.shape.PathElement> els = new ArrayList<>();
            els.add(new MoveTo(xs[0], ys[0]));
            if (xs.length == 1) {
                return els;
            }
            for (int i = 0; i < xs.length - 1; i++) {
                double x0 = i > 0 ? xs[i - 1] : xs[i];
                double y0 = i > 0 ? ys[i - 1] : ys[i];
                double x3 = i + 2 < xs.length ? xs[i + 2] : xs[i + 1];
                double y3 = i + 2 < ys.length ? ys[i + 2] : ys[i + 1];
                double c1x = xs[i] + (xs[i + 1] - x0) / 6;
                double c1y = clamp(ys[i] + (ys[i + 1] - y0) / 6, minY, maxY);
                double c2x = xs[i + 1] - (x3 - xs[i]) / 6;
                double c2y = clamp(ys[i + 1] - (y3 - ys[i]) / 6, minY, maxY);
                els.add(new CubicCurveTo(c1x, c1y, c2x, c2y, xs[i + 1], ys[i + 1]));
            }
            return els;
        }

        void showHover(int index) {
            if (index < 0 || index >= labels.size()) {
                hideHover();
                return;
            }
            hover = index;
            tip.getChildren().clear();
            Label title = new Label(labels.get(index));
            title.getStyleClass().add("trend-tip-title");
            tip.getChildren().add(title);
            StringBuilder a11y = new StringBuilder(labels.get(index)).append(": ");
            for (Serie s : series) {
                double value = index < s.values().length ? s.values()[index] : 0;
                Region sw = new Region();
                sw.getStyleClass().addAll("legend-swatch", s.tone());
                Label name = new Label(s.name());
                name.getStyleClass().add("trend-tip-name");
                Region grow = new Region();
                HBox.setHgrow(grow, javafx.scene.layout.Priority.ALWAYS);
                Label val = new Label(valueFormat.apply(value));
                val.getStyleClass().add("trend-tip-value");
                HBox row = new HBox(8, sw, name, grow, val);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setMinWidth(170);
                tip.getChildren().add(row);
                a11y.append(s.name()).append(" ").append(valueFormat.apply(value)).append(". ");
            }
            if (series.size() == 2) {
                double a = index < series.get(0).values().length ? series.get(0).values()[index] : 0;
                double b = index < series.get(1).values().length ? series.get(1).values()[index] : 0;
                Label diff = new Label("Diferencia " + valueFormat.apply(b - a));
                diff.getStyleClass().addAll("trend-tip-diff", b - a >= 0 ? "diff-up" : "diff-down");
                tip.getChildren().add(diff);
            }
            setAccessibleHelp(a11y.toString());
            tip.setVisible(true);
            cross.setVisible(true);
            hoverDots.forEach(d -> d.setVisible(true));
            positionHover(index);
        }

        private void positionHover(int index) {
            double w = getWidth();
            double h = getHeight();
            double plotH = h - TOP - BOTTOM;
            double base = TOP + plotH;
            double top = Series.niceStep(max(), TICKS) * TICKS;
            double x = LEFT + index * step();
            cross.setStartX(snap(x));
            cross.setEndX(snap(x));
            cross.setStartY(TOP);
            cross.setEndY(base);
            for (int s = 0; s < series.size(); s++) {
                double[] v = series.get(s).values();
                double value = index < v.length ? v[index] : 0;
                Circle dot = hoverDots.get(s);
                dot.setCenterX(x);
                dot.setCenterY(base - (top == 0 ? 0 : plotH * value / top));
            }
            tip.applyCss();
            tip.autosize();
            double tw = tip.getWidth();
            double tx = x + 14 + tw > w ? x - 14 - tw : x + 14;
            tip.relocate(Math.max(0, tx), TOP + 4);
        }

        private void hideHover() {
            hover = -1;
            tip.setVisible(false);
            cross.setVisible(false);
            hoverDots.forEach(d -> d.setVisible(false));
        }

        private static double clamp(double v, double min, double max) {
            return Math.max(min, Math.min(max, v));
        }

        private static double snap(double v) {
            return Math.round(v) + 0.5;
        }
    }
}
