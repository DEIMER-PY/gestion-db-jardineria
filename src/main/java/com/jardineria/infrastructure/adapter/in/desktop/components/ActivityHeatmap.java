package com.jardineria.infrastructure.adapter.in.desktop.components;

import com.jardineria.infrastructure.adapter.in.common.Formatters;
import com.jardineria.infrastructure.adapter.in.desktop.support.Motion;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.YearMonth;
import java.util.Map;
import java.util.TreeSet;

/**
 * Mapa de calor de actividad: una fila por año y una columna por mes. La intensidad
 * del color (cinco niveles con el color de acento) indica cuántos pedidos hubo, como
 * el calendario de contribuciones de GitHub. Cada celda tiene ficha emergente y es
 * accesible por teclado.
 */
public class ActivityHeatmap extends VBox {
    private static final int LEVELS = 4;

    private final GridPane grid = new GridPane();
    private final Label summary = new Label();
    private final VBox years = new VBox(8);

    public ActivityHeatmap() {
        super(12);
        getStyleClass().add("heatmap");
        grid.getStyleClass().add("heatmap-grid");
        grid.setHgap(5);
        grid.setVgap(5);
        summary.getStyleClass().add("heatmap-summary");
        summary.setWrapText(true);
        years.getStyleClass().add("heatmap-years");
        getChildren().addAll(grid, legend(), summary, years);
    }

    /** @param counts pedidos por mes (los meses ausentes cuentan como cero) */
    public void setData(Map<YearMonth, Long> counts) {
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();
        years.getChildren().clear();
        if (counts.isEmpty()) {
            summary.setText("Sin pedidos registrados todavía.");
            return;
        }
        TreeSet<Integer> yearSet = new TreeSet<>();
        counts.keySet().forEach(ym -> yearSet.add(ym.getYear()));
        long max = counts.values().stream().mapToLong(Long::longValue).max().orElse(0);

        ColumnConstraints yearCol = new ColumnConstraints();
        yearCol.setMinWidth(44);
        grid.getColumnConstraints().add(yearCol);
        for (int m = 1; m <= 12; m++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setHgrow(Priority.ALWAYS);
            c.setPercentWidth(-1);
            c.setHalignment(HPos.CENTER);
            c.setMinWidth(18);
            grid.getColumnConstraints().add(c);
            Label month = new Label(Formatters.monthShort(m));
            month.getStyleClass().add("heatmap-axis");
            grid.add(month, m, 0);
        }

        int row = 1;
        int delay = 0;
        YearMonth best = null;
        long activeMonths = 0;
        for (int year : yearSet) {
            Label y = new Label(String.valueOf(year));
            y.getStyleClass().add("heatmap-axis");
            grid.add(y, 0, row);
            for (int m = 1; m <= 12; m++) {
                YearMonth ym = YearMonth.of(year, m);
                long n = counts.getOrDefault(ym, 0L);
                if (n > 0) {
                    activeMonths++;
                }
                if (best == null || n > counts.getOrDefault(best, 0L)) {
                    best = ym;
                }
                Region cell = cell(ym, n, level(n, max));
                grid.add(cell, m, row);
                if (!Motion.isReduced()) {
                    cell.setOpacity(0);
                    FadeTransition ft = new FadeTransition(Duration.millis(260), cell);
                    ft.setToValue(1);
                    ft.setDelay(Duration.millis(delay));
                    ft.play();
                    delay += 12;
                }
            }
            row++;
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        summary.setText(best == null || max == 0 ? "Sin pedidos registrados todavía."
                : "Mes más intenso: " + Formatters.monthLong(best) + " con " + max + " pedidos · "
                + activeMonths + " meses con actividad · " + Formatters.integer(total) + " pedidos en total.");
        buildYears(counts, yearSet);
        setAccessibleText("Mapa de actividad mensual. " + summary.getText());
    }

    /** Totales por año con barra proporcional y variación frente al año anterior. */
    private void buildYears(Map<YearMonth, Long> counts, TreeSet<Integer> yearSet) {
        Map<Integer, Long> totals = new java.util.TreeMap<>();
        counts.forEach((ym, n) -> totals.merge(ym.getYear(), n, Long::sum));
        long top = totals.values().stream().mapToLong(Long::longValue).max().orElse(1);
        Label title = new Label("Pedidos por año");
        title.getStyleClass().add("heatmap-years-title");
        years.getChildren().add(title);
        YearMonth last = counts.keySet().stream().max(YearMonth::compareTo).orElseThrow();
        Long previous = null;
        int delay = 0;
        for (int year : yearSet) {
            long n = totals.getOrDefault(year, 0L);
            boolean partial = year == last.getYear() && last.getMonthValue() < 12;
            Label y = new Label(String.valueOf(year));
            y.getStyleClass().add("heatmap-axis");
            y.setMinWidth(44);
            ProgressBarLite bar = new ProgressBarLite(top == 0 ? 0 : (double) n / top, delay);
            HBox.setHgrow(bar, Priority.ALWAYS);
            Label count = new Label(Formatters.integer(n));
            count.getStyleClass().add("heatmap-year-count");
            count.setMinWidth(Region.USE_PREF_SIZE);
            if (partial) {
                count.setText(Formatters.integer(n) + " · ene–" + Formatters.monthShort(last.getMonthValue()));
            }
            // Un año incompleto no se compara con uno completo: la variación sería engañosa.
            Label delta = new Label(partial ? "parcial" : delta(previous, n));
            delta.getStyleClass().addAll("heatmap-year-delta",
                    partial || previous == null || n == previous ? "year-flat" : n > previous ? "year-up" : "year-down");
            delta.setMinWidth(64);
            delta.setAlignment(Pos.CENTER_RIGHT);
            HBox row = new HBox(10, y, bar, count, delta);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setAccessibleRole(AccessibleRole.TEXT);
            row.setAccessibleText(year + ": " + n + " pedidos. " + delta.getText());
            years.getChildren().add(row);
            previous = n;
            delay += 60;
        }
    }

    /** Variación frente al año anterior ("—" si no hay base). */
    static String delta(Long previous, long current) {
        if (previous == null || previous == 0) {
            return "—";
        }
        long pct = Math.round(100.0 * (current - previous) / previous);
        return (pct > 0 ? "+" : "") + pct + " %";
    }

    /** Barra horizontal ligera que crece con animación (respeta el movimiento reducido). */
    private static final class ProgressBarLite extends javafx.scene.layout.StackPane {
        ProgressBarLite(double ratio, int delay) {
            getStyleClass().add("heatmap-year-track");
            Region fill = new Region();
            fill.getStyleClass().add("heatmap-year-fill");
            fill.setManaged(false);
            getChildren().add(fill);
            setMinHeight(8);
            setPrefHeight(8);
            setMaxHeight(8);
            javafx.beans.property.DoubleProperty progress = new javafx.beans.property.SimpleDoubleProperty(
                    Motion.isReduced() ? ratio : 0);
            Runnable relayout = () -> fill.resizeRelocate(0, 0, Math.max(ratio > 0 ? 8 : 0, getWidth() * progress.get()), getHeight());
            widthProperty().addListener((o, a, b) -> relayout.run());
            heightProperty().addListener((o, a, b) -> relayout.run());
            progress.addListener((o, a, b) -> relayout.run());
            if (!Motion.isReduced()) {
                javafx.animation.Timeline t = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.millis(700),
                        new javafx.animation.KeyValue(progress, ratio, Motion.EASE_OUT)));
                t.setDelay(Duration.millis(200 + delay));
                t.play();
            }
        }
    }

    /** Nivel de intensidad 0..4 (0 = sin pedidos). */
    static int level(long value, long max) {
        if (value <= 0 || max <= 0) {
            return 0;
        }
        return (int) Math.max(1, Math.min(LEVELS, Math.ceil(LEVELS * (double) value / max)));
    }

    private Region cell(YearMonth ym, long n, int level) {
        Region cell = new Region();
        cell.getStyleClass().addAll("heat-cell", "heat-" + level);
        cell.setMinSize(16, 22);
        cell.setPrefHeight(26);
        cell.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(cell, Priority.ALWAYS);
        String text = Formatters.monthLong(ym) + ": " + (n == 0 ? "sin pedidos" : n + (n == 1 ? " pedido" : " pedidos"));
        Tooltip tip = new Tooltip(text);
        tip.setShowDelay(Duration.millis(120));
        Tooltip.install(cell, tip);
        cell.setFocusTraversable(n > 0);
        cell.setAccessibleRole(AccessibleRole.TEXT);
        cell.setAccessibleText(text);
        cell.setOnMouseEntered(e -> pulse(cell, 1.18));
        cell.setOnMouseExited(e -> pulse(cell, 1.0));
        return cell;
    }

    private static void pulse(Region cell, double scale) {
        if (Motion.isReduced()) {
            return;
        }
        ScaleTransition st = new ScaleTransition(Duration.millis(140), cell);
        st.setToX(scale);
        st.setToY(scale);
        st.play();
    }

    private static HBox legend() {
        Label less = new Label("Menos");
        less.getStyleClass().add("heatmap-axis");
        HBox box = new HBox(4, less);
        for (int i = 0; i <= LEVELS; i++) {
            Region r = new Region();
            r.getStyleClass().addAll("heat-cell", "heat-" + i, "heat-legend");
            r.setMinSize(12, 12);
            r.setMaxSize(12, 12);
            box.getChildren().add(r);
        }
        Label more = new Label("Más");
        more.getStyleClass().add("heatmap-axis");
        box.getChildren().add(more);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }
}
