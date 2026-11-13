package com.jardineria.infrastructure.adapter.in.desktop.views.dashboard;

import atlantafx.base.controls.RingProgressIndicator;
import com.jardineria.application.dto.DashboardMetrics;
import com.jardineria.application.port.in.DashboardUseCase;
import com.jardineria.domain.model.Pedido;
import com.jardineria.domain.model.Producto;
import com.jardineria.domain.model.analytics.CategoryMetric;
import com.jardineria.infrastructure.adapter.in.common.Formatters;
import com.jardineria.infrastructure.adapter.in.desktop.DesktopServices;
import com.jardineria.infrastructure.adapter.in.desktop.components.BarList;
import com.jardineria.infrastructure.adapter.in.desktop.components.DonutChart;
import com.jardineria.infrastructure.adapter.in.desktop.components.EmptyState;
import com.jardineria.infrastructure.adapter.in.desktop.components.KpiCard;
import com.jardineria.infrastructure.adapter.in.desktop.components.OrdersByStatusChart;
import com.jardineria.infrastructure.adapter.in.desktop.components.Panel;
import com.jardineria.infrastructure.adapter.in.desktop.components.ResponsiveGrid;
import com.jardineria.infrastructure.adapter.in.desktop.components.Sparkline;
import com.jardineria.infrastructure.adapter.in.desktop.components.StatusBadge;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import com.jardineria.infrastructure.adapter.in.desktop.support.Motion;
import com.jardineria.infrastructure.adapter.in.desktop.support.Series;
import com.jardineria.infrastructure.adapter.in.desktop.views.BaseView;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panel general: saludo con resumen narrativo, KPIs animados con microtendencias reales,
 * pedidos por mes y estado, distribución logística, mejores clientes, stock crítico
 * y actividad reciente. Todo se obtiene en una única consulta en segundo plano.
 */
public class DashboardView extends BaseView {
    private static final DateTimeFormatter LONG_DATE =
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Formatters.ES);

    private final DesktopServices services;
    private final Consumer<Route> navigate;

    private final Label greeting = new Label();
    private final Label summary = new Label();
    private final RingProgressIndicator deliveryRing = new RingProgressIndicator(0, false);
    private final Label deliveryValue = new Label("—");

    private final KpiCard kpiPedidos = new KpiCard(Feather.SHOPPING_BAG, "Pedidos", KpiCard.Tone.JADE);
    private final KpiCard kpiVentas = new KpiCard(Feather.TRENDING_UP, "Ventas brutas", KpiCard.Tone.LIME);
    private final KpiCard kpiCobrado = new KpiCard(Feather.CREDIT_CARD, "Cobrado", KpiCard.Tone.SKY);
    private final KpiCard kpiClientes = new KpiCard(Feather.USERS, "Clientes", KpiCard.Tone.LILAC);
    private final KpiCard kpiStock = new KpiCard(Feather.ALERT_TRIANGLE, "Stock crítico", KpiCard.Tone.CORAL);

    private final OrdersByStatusChart ordersChart = new OrdersByStatusChart();
    private final DonutChart statusDonut = new DonutChart();
    private final BarList topClientes = new BarList();
    private final VBox stockList = new VBox(12);
    private final VBox activity = new VBox(4);

    public DashboardView(DesktopServices services, Consumer<Route> navigate) {
        super(Route.DASHBOARD);
        this.services = services;
        this.navigate = navigate;

        Button export = new Button("Ver métricas", Icons.of(Feather.BAR_CHART_2, 15));
        export.setOnAction(e -> navigate.accept(Route.ANALYTICS));
        Button refresh = new Button("Actualizar", Icons.of(Feather.REFRESH_CW, 15));
        refresh.getStyleClass().add("accent");
        refresh.setOnAction(e -> refresh());
        addActions(export, refresh);
    }

    @Override
    protected Node buildContent() {
        kpiPedidos.setOnOpen(() -> navigate.accept(Route.PEDIDOS));
        kpiVentas.setOnOpen(() -> navigate.accept(Route.ANALYTICS));
        kpiCobrado.setOnOpen(() -> navigate.accept(Route.PAGOS));
        kpiClientes.setOnOpen(() -> navigate.accept(Route.CLIENTES));
        kpiStock.setOnOpen(() -> navigate.accept(Route.PRODUCTOS));

        ResponsiveGrid kpis = new ResponsiveGrid(205, 5, 16);
        for (KpiCard card : List.of(kpiPedidos, kpiVentas, kpiCobrado, kpiClientes, kpiStock)) {
            kpis.add(card, 1);
        }

        ResponsiveGrid charts = new ResponsiveGrid(360, 3, 16);
        charts.add(new Panel("Pedidos por mes", "Últimos " + DashboardSnapshot.MESES
                + " meses con actividad, apilados por estado logístico", ordersChart), 2);
        charts.add(new Panel("Estado logístico", "Distribución de todos los pedidos", statusDonut), 1);

        ResponsiveGrid lists = new ResponsiveGrid(300, 3, 16);
        Button verClientes = linkButton("Ver cartera", Route.CLIENTES);
        Button verProductos = linkButton("Ir al catálogo", Route.PRODUCTOS);
        Button verPedidos = linkButton("Ver todos", Route.PEDIDOS);
        lists.add(new Panel("Clientes más activos", "Por número de pedidos realizados", topClientes).withActions(verClientes), 1);
        lists.add(new Panel("Stock crítico", "Productos por debajo de " + DashboardUseCase.UMBRAL_STOCK_CRITICO
                + " unidades", stockList).withActions(verProductos), 1);
        lists.add(new Panel("Actividad reciente", "Últimos pedidos registrados", activity).withActions(verPedidos), 1);

        VBox body = new VBox(18, buildHero(), kpis, charts, lists);
        body.getStyleClass().add("view-body");
        int delay = 0;
        for (Node n : body.getChildren()) {
            Motion.enter(n, delay);
            delay += 70;
        }
        return body;
    }

    private Node buildHero() {
        greeting.getStyleClass().add("hero-title");
        Label date = new Label(capitalize(LocalDate.now().format(LONG_DATE)));
        date.getStyleClass().add("hero-date");
        summary.getStyleClass().add("hero-summary");
        summary.setWrapText(true);

        Button nuevoPedido = new Button("Gestionar pedidos", Icons.of(Feather.SHOPPING_BAG, 15));
        nuevoPedido.getStyleClass().add("accent");
        nuevoPedido.setOnAction(e -> navigate.accept(Route.PEDIDOS));
        Button nuevoCliente = new Button("Cartera de clientes", Icons.of(Feather.USERS, 15));
        nuevoCliente.setOnAction(e -> navigate.accept(Route.CLIENTES));
        HBox cta = new HBox(10, nuevoPedido, nuevoCliente);

        VBox left = new VBox(6, date, greeting, summary, cta);
        VBox.setMargin(cta, new javafx.geometry.Insets(10, 0, 0, 0));
        HBox.setHgrow(left, Priority.ALWAYS);
        left.setMinWidth(0);

        deliveryRing.getStyleClass().add("hero-ring");
        deliveryRing.setMinSize(118, 118);
        deliveryRing.setPrefSize(118, 118);
        deliveryValue.getStyleClass().add("hero-ring-value");
        Label ringCaption = new Label("entregados");
        ringCaption.getStyleClass().add("hero-ring-caption");
        VBox ringText = new VBox(0, deliveryValue, ringCaption);
        ringText.setAlignment(Pos.CENTER);
        deliveryRing.setGraphic(ringText);
        StackPane ring = new StackPane(deliveryRing);
        ring.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        Tooltip.install(ring, new Tooltip("Tasa de entrega: pedidos entregados sobre el total"));

        HBox hero = new HBox(28, left, ring);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.getStyleClass().addAll("panel", "hero");
        return hero;
    }

    private Button linkButton(String text, Route route) {
        Button b = new Button(text, Icons.of(Feather.ARROW_UP_RIGHT, 13));
        b.getStyleClass().addAll("flat", "link-button");
        b.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        b.setOnAction(e -> navigate.accept(route));
        return b;
    }

    @Override
    public void refresh() {
        load(() -> DashboardSnapshot.load(services.dashboard(), services.analytics(), services.clientes()), this::render);
    }

    private void render(DashboardSnapshot s) {
        DashboardMetrics m = s.metrics();
        greeting.setText(greetingForNow());
        summary.setText(String.format("%s pedidos registrados, %s entregados y %s pendientes. %s en stock crítico.",
                Formatters.integer(m.totalPedidos()), Formatters.integer(m.pedidosEntregados()),
                Formatters.integer(m.pedidosPendientes()),
                m.totalBajoStock() == 1 ? "1 producto" : m.totalBajoStock() + " productos"));
        animateRing(m.tasaEntrega());

        // KPIs
        kpiPedidos.setValue(m.totalPedidos(), v -> Formatters.integer(Math.round(v)));
        kpiPedidos.setCaption(Formatters.integer(m.pedidosEntregados()) + " entregados · "
                + Formatters.integer(m.pedidosRechazados()) + " rechazados");
        kpiPedidos.setFooter(sparkline(s.tendenciaPedidos()));
        double trend = Series.trend(s.tendenciaPedidos());
        kpiPedidos.setChip((trend >= 0 ? "+" : "") + Formatters.decimal(trend * 100, 0) + " %", trend >= 0);

        kpiVentas.setValue(s.totalVentas().doubleValue(), v -> Formatters.compactCurrency(BigDecimal.valueOf(v)));
        kpiVentas.setCaption("Importe de líneas de pedido");
        kpiVentas.setFooter(sparkline(s.tendenciaVentas()));

        BigDecimal cobrado = m.totalIngresos();
        kpiCobrado.setValue(cobrado.doubleValue(), v -> Formatters.compactCurrency(BigDecimal.valueOf(v)));
        double ratio = s.totalVentas().signum() == 0 ? 0
                : cobrado.divide(s.totalVentas(), 4, RoundingMode.HALF_UP).doubleValue();
        kpiCobrado.setCaption(ratio >= 1
                ? "Pagos registrados · cubren el total de las ventas"
                : "Pagos registrados · " + Formatters.percent(ratio) + " de lo vendido");
        kpiCobrado.setFooter(sparkline(s.tendenciaCobros()));

        kpiClientes.setValue(m.totalClientes(), v -> Formatters.integer(Math.round(v)));
        kpiClientes.setCaption("En " + s.clientesPorPais().size() + " países · " + m.totalEmpleados()
                + " empleados en " + m.totalOficinas() + " oficinas");
        kpiClientes.setFooter(countrySegments(s.clientesPorPais(), m.totalClientes()));

        kpiStock.setValue(m.totalBajoStock(), v -> Formatters.integer(Math.round(v)));
        kpiStock.setCaption("de " + m.totalProductos() + " productos bajo mínimos");
        kpiStock.setFooter(progress(m.totalProductos() == 0 ? 0 : (double) m.totalBajoStock() / m.totalProductos(),
                "Proporción del catálogo en stock crítico"));

        // Gráficas
        ordersChart.setData(s.pedidosPorMes());
        statusDonut.setData(List.of(
                new DonutChart.Slice("Entregados", m.pedidosEntregados(), "tone-jade"),
                new DonutChart.Slice("Pendientes", m.pedidosPendientes(), "tone-amber"),
                new DonutChart.Slice("Rechazados", m.pedidosRechazados(), "tone-coral")),
                Formatters.integer(m.totalPedidos()), "pedidos");

        // Listas
        topClientes.setItems(s.topClientes().stream()
                .map(c -> new BarList.Item(c.label(),
                        c.amount().signum() > 0 ? Formatters.currency(c.amount()) + " en líneas" : null,
                        c.count(), c.count() + (c.count() == 1 ? " pedido" : " pedidos"), null))
                .toList());
        if (s.topClientes().isEmpty()) {
            topClientes.getChildren().setAll(new EmptyState(Feather.USERS, "Sin pedidos", "Ningún cliente ha realizado pedidos todavía."));
        }
        renderStock(s.stockCritico(), m.totalProductos());
        renderActivity(s);
    }

    private void renderStock(List<Producto> productos, long totalProductos) {
        if (productos.isEmpty()) {
            stockList.getChildren().setAll(new EmptyState(Feather.CHECK_CIRCLE, "Todo en orden",
                    "Ningún producto está por debajo del mínimo."));
            return;
        }
        BarList bars = new BarList();
        bars.setItems(productos.stream().map(p -> new BarList.Item(
                p.getNombre(), p.getCodigoProducto(),
                p.getCantidadEnStock(), p.getCantidadEnStock() + " uds",
                p.getCantidadEnStock() <= DashboardUseCase.UMBRAL_STOCK_CRITICO / 2 ? "bar-danger" : "bar-warning"))
                .toList());
        long sanos = Math.max(0, totalProductos - productos.size());
        double fraccionSana = totalProductos == 0 ? 1 : (double) sanos / totalProductos;
        Label saludValor = new Label(Formatters.percent(fraccionSana));
        saludValor.getStyleClass().add("health-value");
        Label saludTexto = new Label(sanos + " de " + totalProductos
                + " productos con stock suficiente. Reponer antes de confirmar pedidos nuevos.");
        saludTexto.getStyleClass().add("health-text");
        saludTexto.setWrapText(true);
        saludTexto.setMinWidth(0);
        HBox.setHgrow(saludTexto, Priority.ALWAYS);
        HBox salud = new HBox(14, saludValor, saludTexto);
        salud.setAlignment(Pos.CENTER_LEFT);
        salud.getStyleClass().add("health");
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        stockList.getChildren().setAll(bars, spacer, salud);
        VBox.setVgrow(stockList, Priority.ALWAYS);
    }

    private void renderActivity(DashboardSnapshot s) {
        activity.getChildren().clear();
        for (Pedido p : s.ultimosPedidos()) {
            Region dot = new Region();
            dot.getStyleClass().addAll("timeline-dot", StatusBadge.toneFor(p.getEstado()));
            Label title = new Label("Pedido #" + p.getCodigoPedido());
            title.getStyleClass().add("timeline-title");
            String cliente = s.nombresClientes().getOrDefault(p.getCodigoCliente(), "Cliente " + p.getCodigoCliente());
            Label detail = new Label(cliente + " · " + Formatters.date(p.getFechaPedido()));
            detail.getStyleClass().add("timeline-detail");
            detail.setMinWidth(0);
            VBox text = new VBox(1, title, detail);
            text.setMinWidth(0);
            HBox.setHgrow(text, Priority.ALWAYS);
            HBox row = new HBox(12, dot, text, new StatusBadge(p.getEstado()));
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("timeline-row");
            activity.getChildren().add(row);
        }
        if (activity.getChildren().isEmpty()) {
            activity.getChildren().add(new EmptyState(Feather.INBOX, "Sin pedidos", "Todavía no hay actividad."));
        }
    }

    private static Sparkline sparkline(double[] values) {
        Sparkline spark = new Sparkline();
        spark.setValues(values);
        return spark;
    }

    private static Node progress(double fraction, String description) {
        ProgressBar bar = new ProgressBar(0);
        bar.getStyleClass().add("kpi-progress");
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setAccessibleText(description + ": " + Formatters.percent(fraction));
        if (Motion.isReduced()) {
            bar.setProgress(fraction);
        } else {
            new Timeline(new KeyFrame(Duration.millis(900),
                    new KeyValue(bar.progressProperty(), fraction, Motion.EASE_OUT))).play();
        }
        Tooltip.install(bar, new Tooltip(description + ": " + Formatters.percent(fraction)));
        return bar;
    }

    /** Barra segmentada con la distribución de clientes por país. */
    private static Node countrySegments(List<CategoryMetric> paises, long total) {
        HBox bar = new HBox(3);
        bar.getStyleClass().add("segments");
        String[] tones = {"tone-lilac", "tone-jade", "tone-sky", "tone-amber", "tone-coral", "tone-lime"};
        int i = 0;
        for (CategoryMetric c : paises) {
            Region seg = new Region();
            seg.getStyleClass().addAll("segment", tones[i++ % tones.length]);
            seg.setMinWidth(4);
            seg.prefWidthProperty().bind(bar.widthProperty().multiply(total == 0 ? 0 : (double) c.count() / total));
            Tooltip.install(seg, new Tooltip(Formatters.display(c.label()) + ": " + c.count() + " clientes"));
            bar.getChildren().add(seg);
        }
        return bar;
    }

    private void animateRing(double fraction) {
        deliveryValue.setText(Formatters.decimal(fraction * 100, 0) + " %");
        if (Motion.isReduced()) {
            deliveryRing.setProgress(fraction);
            return;
        }
        deliveryRing.setProgress(0);
        new Timeline(new KeyFrame(Duration.millis(1100),
                new KeyValue(deliveryRing.progressProperty(), fraction, Motion.EASE_OUT))).play();
    }

    private static String greetingForNow() {
        int h = LocalTime.now().getHour();
        if (h < 12) {
            return "Buenos días";
        }
        return h < 20 ? "Buenas tardes" : "Buenas noches";
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
