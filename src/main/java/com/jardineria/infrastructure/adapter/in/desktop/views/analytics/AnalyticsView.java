package com.jardineria.infrastructure.adapter.in.desktop.views.analytics;

import com.jardineria.application.port.in.AnalyticsUseCase;
import com.jardineria.domain.model.analytics.CategoryMetric;
import com.jardineria.infrastructure.adapter.in.common.Formatters;
import com.jardineria.infrastructure.adapter.in.desktop.components.AreaTrendChart;
import com.jardineria.infrastructure.adapter.in.desktop.components.BarList;
import com.jardineria.infrastructure.adapter.in.desktop.components.DonutChart;
import com.jardineria.infrastructure.adapter.in.desktop.components.EmptyState;
import com.jardineria.infrastructure.adapter.in.desktop.components.KpiCard;
import com.jardineria.infrastructure.adapter.in.desktop.components.OrdersByStatusChart;
import com.jardineria.infrastructure.adapter.in.desktop.components.Panel;
import com.jardineria.infrastructure.adapter.in.desktop.components.ResponsiveGrid;
import com.jardineria.infrastructure.adapter.in.desktop.components.Sparkline;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.support.CsvExport;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import com.jardineria.infrastructure.adapter.in.desktop.support.Motion;
import com.jardineria.infrastructure.adapter.in.desktop.support.Notifier;
import com.jardineria.infrastructure.adapter.in.desktop.support.Series;
import com.jardineria.infrastructure.adapter.in.desktop.views.BaseView;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.kordamp.ikonli.feather.Feather;

import java.math.BigDecimal;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Panel de métricas: filtro por año, indicadores del periodo, evolución mensual de
 * pedidos por estado, ventas frente a cobros, distribución por gama, país y forma de
 * pago, y ranking de clientes por facturación. Exportable a CSV.
 */
public class AnalyticsView extends BaseView {
    private static final String[] TONES = {"tone-jade", "tone-sky", "tone-lime", "tone-amber", "tone-lilac", "tone-coral"};

    private final AnalyticsUseCase analytics;
    private final ComboBox<Year> yearFilter = new ComboBox<>();
    private boolean updatingYears;
    private AnalyticsSnapshot current;

    private final KpiCard kpiVentas = new KpiCard(Feather.TRENDING_UP, "Ventas del periodo", KpiCard.Tone.LIME);
    private final KpiCard kpiCobros = new KpiCard(Feather.CREDIT_CARD, "Cobrado", KpiCard.Tone.SKY);
    private final KpiCard kpiTicket = new KpiCard(Feather.SHOPPING_CART, "Ticket medio", KpiCard.Tone.LILAC);
    private final KpiCard kpiRechazo = new KpiCard(Feather.X_OCTAGON, "Tasa de rechazo", KpiCard.Tone.CORAL);

    private final OrdersByStatusChart ordersChart = new OrdersByStatusChart();
    private final Panel ordersPanel = new Panel("Pedidos por mes y estado", "", ordersChart);
    private final AreaTrendChart cashChart = new AreaTrendChart();
    private final StackPane cashHost = new StackPane(cashChart);
    private final BarList gamas = new BarList();
    private final BarList paises = new BarList();
    private final BarList clientes = new BarList();
    private final DonutChart formasPago = new DonutChart();

    public AnalyticsView(AnalyticsUseCase analytics, BiConsumer<Route, String> navigate) {
        super(Route.ANALYTICS);
        this.analytics = analytics;
        gamas.setOnOpen(item -> navigate.accept(Route.PRODUCTOS, item.label()), "Ver los productos de la gama");
        paises.setOnOpen(item -> navigate.accept(Route.CLIENTES, item.label()), "Ver los clientes de este país");
        clientes.setOnOpen(item -> navigate.accept(Route.CLIENTES, item.label()), "Abrir la ficha del cliente");

        yearFilter.getStyleClass().add("year-filter");
        yearFilter.setPromptText("Todos los años");
        yearFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(Year year) {
                return year == null ? "Todos los años" : "Año " + year;
            }

            @Override
            public Year fromString(String s) {
                return null;
            }
        });
        yearFilter.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Year item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "Todos los años" : "Año " + item);
            }
        });
        yearFilter.setAccessibleText("Filtrar métricas por año");
        yearFilter.setTooltip(new Tooltip("Filtra ventas, cobros y pedidos por año"));
        yearFilter.valueProperty().addListener((obs, o, n) -> {
            if (!updatingYears) {
                refresh();
            }
        });

        Button export = new Button("Exportar CSV", Icons.of(Feather.DOWNLOAD, 15));
        export.setTooltip(new Tooltip("Exporta el resumen mensual del periodo"));
        export.setOnAction(e -> exportCsv());
        Button refresh = new Button("Actualizar", Icons.of(Feather.REFRESH_CW, 15));
        refresh.getStyleClass().add("accent");
        refresh.setOnAction(e -> refresh());
        addActions(yearFilter, export, refresh);
    }

    @Override
    protected Node buildContent() {
        ResponsiveGrid kpis = new ResponsiveGrid(220, 4, 16);
        for (KpiCard card : List.of(kpiVentas, kpiCobros, kpiTicket, kpiRechazo)) {
            kpis.add(card, 1);
        }

        cashChart.setMinHeight(240);

        ResponsiveGrid row1 = new ResponsiveGrid(360, 3, 16);
        row1.add(ordersPanel, 2);
        row1.add(new Panel("Formas de pago", "Importe cobrado por medio (histórico)", formasPago), 1);

        ResponsiveGrid row2 = new ResponsiveGrid(360, 3, 16);
        row2.add(new Panel("Ventas frente a cobros", "Importe facturado en líneas y pagos recibidos por mes", cashHost), 2);
        row2.add(new Panel("Ventas por gama", "Unidades e importe vendidos (histórico)", gamas), 1);

        ResponsiveGrid row3 = new ResponsiveGrid(360, 2, 16);
        row3.add(new Panel("Clientes por país", "Distribución geográfica de la cartera", paises), 1);
        row3.add(new Panel("Clientes con mayor facturación", "Importe total de sus líneas de pedido", clientes), 1);

        VBox body = new VBox(18, kpis, row1, row2, row3);
        body.getStyleClass().add("view-body");
        int delay = 0;
        for (Node n : body.getChildren()) {
            Motion.enter(n, delay);
            delay += 70;
        }
        return body;
    }

    @Override
    public void refresh() {
        Year anio = yearFilter.getValue();
        load(() -> AnalyticsSnapshot.load(analytics, anio), this::render);
    }

    private void render(AnalyticsSnapshot s) {
        current = s;
        syncYears(s);
        String periodo = s.anio() == null ? "todo el histórico" : "el año " + s.anio();

        kpiVentas.setValue(s.ventas().doubleValue(), v -> Formatters.compactCurrency(BigDecimal.valueOf(v)));
        kpiVentas.setCaption(s.pedidosConLineas() + " pedidos con líneas en " + periodo);
        kpiVentas.setFooter(sparkline(s.ventasCobros(), 0));

        kpiCobros.setValue(s.cobros().doubleValue(), v -> Formatters.compactCurrency(BigDecimal.valueOf(v)));
        kpiCobros.setCaption("Pagos recibidos en " + periodo);
        kpiCobros.setFooter(sparkline(s.ventasCobros(), 1));

        kpiTicket.setValue(s.ticketMedio().doubleValue(), v -> Formatters.currency(BigDecimal.valueOf(v)));
        kpiTicket.setCaption("Importe medio por pedido con líneas");
        kpiTicket.setFooter(sparkline(s.ticketPorMes()));

        long total = s.totalPedidos();
        double rechazo = total == 0 ? 0 : (double) s.pedidos("Rechazado") / total;
        kpiRechazo.setValue(rechazo * 100, v -> Formatters.decimal(v, 1) + " %");
        kpiRechazo.setCaption(s.pedidos("Rechazado") + " de " + total + " pedidos en " + periodo);
        kpiRechazo.setFooter(statusSegments(s));

        ordersPanel.setCaption(s.anio() == null
                ? "Últimos " + AnalyticsSnapshot.MESES_HISTORICO + " meses con actividad"
                : "Meses con actividad en " + s.anio());
        ordersChart.setData(s.pedidosPorMes());
        renderCash(s.ventasCobros());

        gamas.setItems(toItems(s.ventasPorGama(),
                c -> c.count() + (c.count() == 1 ? " unidad" : " unidades"),
                c -> c.amount().doubleValue(), c -> Formatters.currency(c.amount()), true));
        paises.setItems(toItems(s.clientesPorPais(), c -> null,
                c -> c.count(), c -> c.count() + (c.count() == 1 ? " cliente" : " clientes"), true));
        clientes.setItems(toItems(s.topClientes(),
                c -> c.count() + (c.count() == 1 ? " pedido" : " pedidos"),
                c -> c.amount().doubleValue(), c -> Formatters.currency(c.amount()), false));
        if (s.topClientes().isEmpty()) {
            clientes.getChildren().setAll(new EmptyState(Feather.USERS, "Sin facturación", "No hay líneas de pedido registradas."));
        }

        List<DonutChart.Slice> slices = new ArrayList<>();
        BigDecimal totalPagos = BigDecimal.ZERO;
        for (int i = 0; i < s.formasPago().size(); i++) {
            CategoryMetric c = s.formasPago().get(i);
            slices.add(new DonutChart.Slice(c.label(), c.amount().doubleValue(), TONES[(i + 1) % TONES.length]));
            totalPagos = totalPagos.add(c.amount());
        }
        formasPago.setData(slices, Formatters.compactCurrency(totalPagos), "cobrado");
    }

    private void syncYears(AnalyticsSnapshot s) {
        updatingYears = true;
        try {
            List<Year> items = new ArrayList<>();
            items.add(null);
            items.addAll(s.anios());
            if (!yearFilter.getItems().equals(items)) {
                Year selected = yearFilter.getValue();
                yearFilter.getItems().setAll(items);
                yearFilter.setValue(selected);
            }
        } finally {
            updatingYears = false;
        }
    }

    private void renderCash(Map<YearMonth, double[]> data) {
        if (data.isEmpty()) {
            cashHost.getChildren().setAll(new EmptyState(Feather.BAR_CHART_2, "Sin movimientos",
                    "No hay ventas ni cobros en el periodo seleccionado."));
            return;
        }
        cashHost.getChildren().setAll(cashChart);
        List<String> meses = data.keySet().stream().map(Formatters::month).toList();
        cashChart.setData(meses,
                List.of(new AreaTrendChart.Serie("Ventas", "tone-accent", data.values().stream().mapToDouble(v -> v[0]).toArray()),
                        new AreaTrendChart.Serie("Cobros", "tone-sky", data.values().stream().mapToDouble(v -> v[1]).toArray())),
                v -> Formatters.compactCurrency(BigDecimal.valueOf(v)),
                v -> Formatters.currency(BigDecimal.valueOf(v)));
    }

    private static Sparkline sparkline(Map<YearMonth, double[]> data, int index) {
        return sparkline(data.values().stream().mapToDouble(v -> v[index]).toArray());
    }

    private static Sparkline sparkline(double[] values) {
        Sparkline spark = new Sparkline();
        spark.setValues(values);
        return spark;
    }

    /** Barra segmentada con el reparto de pedidos por estado en el periodo. */
    private static Node statusSegments(AnalyticsSnapshot s) {
        HBox bar = new HBox(3);
        bar.getStyleClass().add("segments");
        long total = Math.max(1, s.totalPedidos());
        String[] tones = {"tone-jade", "tone-amber", "tone-coral"};
        for (int i = 0; i < Series.ESTADOS.size(); i++) {
            String estado = Series.ESTADOS.get(i);
            long n = s.pedidos(estado);
            if (n == 0) {
                continue;
            }
            Region seg = new Region();
            seg.getStyleClass().addAll("segment", tones[i]);
            seg.prefWidthProperty().bind(bar.widthProperty().multiply((double) n / total));
            Tooltip.install(seg, new Tooltip(estado + ": " + n));
            bar.getChildren().add(seg);
        }
        bar.setAccessibleText("Reparto de pedidos por estado");
        return bar;
    }

    private static List<BarList.Item> toItems(List<CategoryMetric> metrics,
                                              Function<CategoryMetric, String> detail,
                                              Function<CategoryMetric, Number> value,
                                              Function<CategoryMetric, String> display,
                                              boolean toned) {
        List<BarList.Item> items = new ArrayList<>();
        for (int i = 0; i < metrics.size(); i++) {
            CategoryMetric c = metrics.get(i);
            items.add(new BarList.Item(Formatters.display(c.label()), detail.apply(c), value.apply(c).doubleValue(),
                    display.apply(c), toned ? TONES[i % TONES.length] : null));
        }
        return items;
    }

    private void exportCsv() {
        if (current == null) {
            Notifier.info("Sin datos", "Espera a que terminen de cargar las métricas.");
            return;
        }
        List<List<String>> rows = new ArrayList<>();
        TreeSet<YearMonth> meses = new TreeSet<>(current.pedidosPorMes().keySet());
        meses.addAll(current.ventasCobros().keySet());
        for (YearMonth ym : meses) {
            Map<String, Long> estados = current.pedidosPorMes().getOrDefault(ym, Map.of());
            double[] vc = current.ventasCobros().getOrDefault(ym, new double[2]);
            List<String> row = new ArrayList<>();
            row.add(ym.toString());
            for (String estado : Series.ESTADOS) {
                row.add(String.valueOf(estados.getOrDefault(estado, 0L)));
            }
            row.add(Formatters.decimal(vc[0], 2));
            row.add(Formatters.decimal(vc[1], 2));
            rows.add(row);
        }
        List<String> header = new ArrayList<>(List.of("Mes"));
        Series.ESTADOS.forEach(e -> header.add("Pedidos " + e.toLowerCase()));
        header.add("Ventas (EUR)");
        header.add("Cobros (EUR)");
        String nombre = "metricas-jardineria-" + (current.anio() == null ? "historico" : current.anio());
        CsvExport.saveWithDialog(getScene() == null ? null : getScene().getWindow(), nombre, header, rows);
    }
}
