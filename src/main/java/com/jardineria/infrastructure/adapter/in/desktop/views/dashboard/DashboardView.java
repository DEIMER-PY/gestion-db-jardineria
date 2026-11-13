package com.jardineria.infrastructure.adapter.in.desktop.views.dashboard;

import atlantafx.base.controls.RingProgressIndicator;
import com.jardineria.application.dto.DashboardMetrics;
import com.jardineria.application.port.in.DashboardUseCase;
import com.jardineria.domain.model.Pedido;
import com.jardineria.domain.model.Producto;
import com.jardineria.domain.model.analytics.CategoryMetric;
import com.jardineria.infrastructure.adapter.in.common.Formatters;
import com.jardineria.infrastructure.adapter.in.desktop.DesktopServices;
import com.jardineria.infrastructure.adapter.in.desktop.components.ActivityHeatmap;
import com.jardineria.infrastructure.adapter.in.desktop.components.AreaTrendChart;
import com.jardineria.infrastructure.adapter.in.desktop.components.BarList;
import com.jardineria.infrastructure.adapter.in.desktop.components.DonutChart;
import com.jardineria.infrastructure.adapter.in.desktop.components.EmptyState;
import com.jardineria.infrastructure.adapter.in.desktop.components.KpiCard;
import com.jardineria.infrastructure.adapter.in.desktop.components.OrdersByStatusChart;
import com.jardineria.infrastructure.adapter.in.desktop.components.Panel;
import com.jardineria.infrastructure.adapter.in.desktop.components.ResponsiveGrid;
import com.jardineria.infrastructure.adapter.in.desktop.components.Sparkline;
import com.jardineria.infrastructure.adapter.in.desktop.components.StatusBadge;
import com.jardineria.infrastructure.adapter.in.desktop.components.Treemap;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import com.jardineria.infrastructure.adapter.in.desktop.support.Motion;
import com.jardineria.infrastructure.adapter.in.desktop.support.Series;
import com.jardineria.infrastructure.adapter.in.desktop.views.BaseView;
import com.jardineria.infrastructure.adapter.in.desktop.views.dashboard.DashboardInsights.Insight;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.material2.Material2AL;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;

/**
 * Panel general: saludo con resumen narrativo, KPIs animados con microtendencias
 * interactivas, ventas frente a cobros, estado logístico, pedidos por mes, ventas por
 * gama (treemap), mapa de calor de actividad, lecturas automáticas, mejores clientes,
 * stock crítico y actividad reciente. Todo se obtiene en una única consulta en segundo plano.
 */
public class DashboardView extends BaseView {
    private static final DateTimeFormatter LONG_DATE =
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Formatters.ES);
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm", Formatters.ES);
    private static final int VENTANA_TENDENCIA = 3;
    private static final String TENDENCIA_AYUDA =
            "Últimos " + VENTANA_TENDENCIA + " meses con datos frente a los " + VENTANA_TENDENCIA + " anteriores";

    private final DesktopServices services;
    private final Consumer<Route> navigate;

    private final Label greeting = new Label();
    private final Label summary = new Label();
    private final Label updated = new Label("Consultando…");
    private final RingProgressIndicator deliveryRing = new RingProgressIndicator(0, false);
    private final Label deliveryValue = new Label("—");

    private final KpiCard kpiPedidos = new KpiCard(Feather.SHOPPING_BAG, "Pedidos", KpiCard.Tone.ACCENT);
    private final KpiCard kpiVentas = new KpiCard(Feather.TRENDING_UP, "Ventas brutas", KpiCard.Tone.JADE);
    private final KpiCard kpiCobrado = new KpiCard(Feather.CREDIT_CARD, "Cobrado", KpiCard.Tone.SKY);
    private final KpiCard kpiClientes = new KpiCard(Feather.USERS, "Clientes", KpiCard.Tone.LILAC);
    private final KpiCard kpiStock = new KpiCard(Feather.ALERT_TRIANGLE, "Stock crítico", KpiCard.Tone.CORAL);

    private final AreaTrendChart trendChart = new AreaTrendChart();
    private final OrdersByStatusChart ordersChart = new OrdersByStatusChart();
    private final DonutChart statusDonut = new DonutChart();
    private final Treemap gamas = new Treemap();
    private final ActivityHeatmap heatmap = new ActivityHeatmap();
    private final VBox insights = new VBox(4);
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
        refresh.setTooltip(new Tooltip("Vuelve a consultar MySQL (F5)"));
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

        ResponsiveGrid trends = new ResponsiveGrid(360, 3, 16);
        trends.add(new Panel("Ventas frente a cobros", "Últimos " + DashboardSnapshot.MESES
                + " meses · pasa el ratón o usa ← → para ver cada mes", trendChart)
                .withActions(linkButton("Analizar", Route.ANALYTICS)), 2);
        trends.add(new Panel("Estado logístico", "Distribución de todos los pedidos · pasa el ratón por un estado",
                statusDonut), 1);

        ResponsiveGrid breakdown = new ResponsiveGrid(360, 3, 16);
        breakdown.add(new Panel("Pedidos por mes", "Últimos " + DashboardSnapshot.MESES
                + " meses con actividad, apilados por estado logístico", ordersChart), 2);
        breakdown.add(new Panel("Ventas por gama", "Área proporcional al importe vendido", gamas)
                .withActions(linkButton("Gamas", Route.GAMAS)), 1);

        ResponsiveGrid pulse = new ResponsiveGrid(360, 3, 16);
        pulse.add(new Panel("Mapa de actividad", "Pedidos por mes en todo el histórico", heatmap), 2);
        pulse.add(new Panel("Lecturas clave", "Conclusiones calculadas a partir de los datos", insights), 1);

        ResponsiveGrid lists = new ResponsiveGrid(300, 3, 16);
        lists.add(new Panel("Clientes más activos", "Por número de pedidos realizados", topClientes)
                .withActions(linkButton("Ver cartera", Route.CLIENTES)), 1);
        lists.add(new Panel("Stock crítico", "Productos por debajo de " + DashboardUseCase.UMBRAL_STOCK_CRITICO
                + " unidades", stockList).withActions(linkButton("Ir al catálogo", Route.PRODUCTOS)), 1);
        lists.add(new Panel("Actividad reciente", "Últimos pedidos registrados", activity)
                .withActions(linkButton("Ver todos", Route.PEDIDOS)), 1);

        VBox body = new VBox(18, buildHero(), kpis, trends, breakdown, pulse, lists);
        body.getStyleClass().addAll("view-body", "dashboard");
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
        Region liveDot = new Region();
        liveDot.getStyleClass().add("live-dot");
        if (!Motion.isReduced()) {
            FadeTransition blink = new FadeTransition(Duration.millis(1200), liveDot);
            blink.setFromValue(1);
            blink.setToValue(0.25);
            blink.setAutoReverse(true);
            blink.setCycleCount(Animation.INDEFINITE);
            blink.play();
        }
        updated.getStyleClass().add("hero-updated");
        HBox meta = new HBox(10, date, new HBox(6, liveDot, updated));
        ((HBox) meta.getChildren().get(1)).setAlignment(Pos.CENTER_LEFT);
        ((HBox) meta.getChildren().get(1)).getStyleClass().add("hero-live");
        meta.setAlignment(Pos.CENTER_LEFT);
        summary.getStyleClass().add("hero-summary");
        summary.setWrapText(true);

        Button pedidos = new Button("Gestionar pedidos", Icons.of(Feather.SHOPPING_BAG, 15));
        pedidos.getStyleClass().add("accent");
        pedidos.setOnAction(e -> navigate.accept(Route.PEDIDOS));
        Button clientes = new Button("Cartera de clientes", Icons.of(Feather.USERS, 15));
        clientes.setOnAction(e -> navigate.accept(Route.CLIENTES));
        Button ajustes = new Button("Personalizar", Icons.of(Feather.SLIDERS, 15));
        ajustes.getStyleClass().add("flat");
        ajustes.setTooltip(new Tooltip("Tema, color de acento y perfiles (Ctrl+,)"));
        ajustes.setOnAction(e -> navigate.accept(Route.AJUSTES));
        HBox cta = new HBox(10, pedidos, clientes, ajustes);

        VBox left = new VBox(6, meta, greeting, summary, cta);
        VBox.setMargin(cta, new Insets(10, 0, 0, 0));
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
        b.setContentDisplay(ContentDisplay.RIGHT);
        b.setMinWidth(Region.USE_PREF_SIZE);
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
        updated.setText("Actualizado a las " + s.consultado().format(HOUR));
        summary.setText(String.format("%s pedidos registrados, %s entregados y %s pendientes. %s en stock crítico.",
                Formatters.integer(m.totalPedidos()), Formatters.integer(m.pedidosEntregados()),
                Formatters.integer(m.pedidosPendientes()),
                m.totalBajoStock() == 1 ? "1 producto" : m.totalBajoStock() + " productos"));
        animateRing(m.tasaEntrega());

        String[] mesesPedidos = s.pedidosPorMes().keySet().stream().map(Formatters::month).toArray(String[]::new);
        String[] mesesImportes = s.ventasCobros().keySet().stream().map(Formatters::month).toArray(String[]::new);
        double[] ventas = s.ventasCobros().values().stream().mapToDouble(v -> v[0]).toArray();
        double[] cobros = s.ventasCobros().values().stream().mapToDouble(v -> v[1]).toArray();
        DoubleFunction<String> euros = v -> Formatters.currency(BigDecimal.valueOf(v));
        DoubleFunction<String> unidades = v -> Formatters.integer(Math.round(v)) + " pedidos";

        // KPIs
        kpiPedidos.setValue(m.totalPedidos(), v -> Formatters.integer(Math.round(v)));
        kpiPedidos.setCaption(Formatters.integer(m.pedidosEntregados()) + " entregados · "
                + Formatters.integer(m.pedidosRechazados()) + " rechazados");
        kpiPedidos.setFooter(sparkline(s.tendenciaPedidos(), mesesPedidos, unidades));
        chip(kpiPedidos, Series.recentChange(s.tendenciaPedidos(), VENTANA_TENDENCIA));

        kpiVentas.setValue(s.totalVentas().doubleValue(), v -> Formatters.compactCurrency(BigDecimal.valueOf(v)));
        kpiVentas.setCaption("Importe de líneas de pedido");
        kpiVentas.setFooter(sparkline(ventas, mesesImportes, euros));
        chip(kpiVentas, Series.recentChange(ventas, VENTANA_TENDENCIA));

        BigDecimal cobrado = m.totalIngresos();
        kpiCobrado.setValue(cobrado.doubleValue(), v -> Formatters.compactCurrency(BigDecimal.valueOf(v)));
        double ratio = s.totalVentas().signum() == 0 ? 0
                : cobrado.divide(s.totalVentas(), 4, RoundingMode.HALF_UP).doubleValue();
        kpiCobrado.setCaption(ratio >= 1
                ? "Pagos registrados · cubren el total de las ventas"
                : "Pagos registrados · " + Formatters.percent(ratio) + " de lo vendido");
        kpiCobrado.setFooter(sparkline(cobros, mesesImportes, euros));
        chip(kpiCobrado, Series.recentChange(cobros, VENTANA_TENDENCIA));

        kpiClientes.setValue(m.totalClientes(), v -> Formatters.integer(Math.round(v)));
        kpiClientes.setCaption("En " + s.clientesPorPais().size() + " países · " + m.totalEmpleados()
                + " empleados en " + m.totalOficinas() + " oficinas");
        kpiClientes.setFooter(countrySegments(s.clientesPorPais(), m.totalClientes()));

        kpiStock.setValue(m.totalBajoStock(), v -> Formatters.integer(Math.round(v)));
        kpiStock.setCaption("de " + m.totalProductos() + " productos bajo mínimos");
        kpiStock.setFooter(progress(m.totalProductos() == 0 ? 0 : (double) m.totalBajoStock() / m.totalProductos(),
                "Proporción del catálogo en stock crítico"));

        // Gráficas
        trendChart.setData(List.of(mesesImportes),
                List.of(new AreaTrendChart.Serie("Ventas", "tone-accent", ventas),
                        new AreaTrendChart.Serie("Cobros", "tone-sky", cobros)),
                DashboardView::axisEuros, euros);
        ordersChart.setData(s.pedidosPorMes());
        statusDonut.setData(List.of(
                        new DonutChart.Slice("Entregados", m.pedidosEntregados(), "tone-jade"),
                        new DonutChart.Slice("Pendientes", m.pedidosPendientes(), "tone-amber"),
                        new DonutChart.Slice("Rechazados", m.pedidosRechazados(), "tone-coral")),
                Formatters.integer(m.totalPedidos()), "pedidos");
        renderGamas(s.ventasPorGama());
        heatmap.setData(s.actividadMensual());
        renderInsights(DashboardInsights.from(s));

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

    private static void chip(KpiCard card, OptionalDouble change) {
        if (change.isEmpty()) {
            card.clearChip();
            return;
        }
        double c = change.getAsDouble();
        card.setChip((c >= 0 ? "+" : "") + Formatters.decimal(c * 100, 0) + " %", c >= 0, TENDENCIA_AYUDA);
    }

    private void renderGamas(List<CategoryMetric> data) {
        String[] tones = {"tone-accent", "tone-jade", "tone-sky", "tone-amber", "tone-lilac", "tone-coral", "tone-lime"};
        List<Treemap.Item> items = new ArrayList<>();
        int i = 0;
        for (CategoryMetric c : data) {
            items.add(new Treemap.Item(Formatters.display(c.label()), c.amount().doubleValue(),
                    Formatters.currency(c.amount()), tones[i++ % tones.length]));
        }
        gamas.setItems(items);
    }

    private void renderInsights(List<Insight> list) {
        insights.getChildren().clear();
        if (list.isEmpty()) {
            insights.getChildren().add(new EmptyState(Feather.INFO, "Sin lecturas", "Aún no hay datos suficientes."));
            return;
        }
        int delay = 0;
        for (Insight in : list) {
            StackPane badge = new StackPane(Icons.of(iconFor(in.kind()), 15));
            badge.getStyleClass().addAll("insight-badge", toneFor(in.kind()));
            badge.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            Label title = new Label(in.title());
            title.getStyleClass().add("insight-title");
            title.setWrapText(true);
            Label detail = new Label(in.detail());
            detail.getStyleClass().add("insight-detail");
            detail.setWrapText(true);
            VBox text = new VBox(1, title, detail);
            text.setMinWidth(0);
            HBox.setHgrow(text, Priority.ALWAYS);
            HBox row = new HBox(12, badge, text);
            row.setAlignment(Pos.TOP_LEFT);
            row.getStyleClass().add("insight-row");
            row.setAccessibleText(in.title() + ". " + in.detail());
            insights.getChildren().add(row);
            Motion.enter(row, delay);
            delay += 60;
        }
    }

    private static Ikon iconFor(DashboardInsights.Kind kind) {
        return switch (kind) {
            case PEAK -> Feather.ZAP;
            case GOOD -> Feather.CHECK_CIRCLE;
            case WARNING -> Feather.ALERT_TRIANGLE;
            case RISK -> Feather.ALERT_OCTAGON;
            case LEADER -> Feather.AWARD;
            case GEO -> Feather.GLOBE;
            case CATEGORY -> Material2AL.ECO;
            case MONEY -> Feather.CREDIT_CARD;
        };
    }

    private static String toneFor(DashboardInsights.Kind kind) {
        return switch (kind) {
            case PEAK -> "tone-accent";
            case GOOD, MONEY -> "tone-jade";
            case WARNING -> "tone-amber";
            case RISK -> "tone-coral";
            case LEADER -> "tone-lilac";
            case GEO -> "tone-sky";
            case CATEGORY -> "tone-lime";
        };
    }

    /** Etiqueta compacta del eje de importes: "0 €", "250 €", "1,5 k€". */
    static String axisEuros(double v) {
        if (Math.abs(v) >= 1000) {
            return Formatters.decimal(v / 1000, v % 1000 == 0 ? 0 : 1) + " k€";
        }
        return Formatters.integer(Math.round(v)) + " €";
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
        // Sin ancho mínimo el porcentaje se truncaba a "..." cuando el texto de al lado ocupaba dos líneas
        saludValor.setMinWidth(Region.USE_PREF_SIZE);
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
            StatusBadge badge = new StatusBadge(p.getEstado());
            badge.setMinWidth(Region.USE_PREF_SIZE);
            HBox row = new HBox(12, dot, text, badge);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().addAll("timeline-row", "interactive-row");
            row.setOnMouseClicked(e -> navigate.accept(Route.PEDIDOS));
            row.setFocusTraversable(true);
            row.setAccessibleRole(javafx.scene.AccessibleRole.BUTTON);
            row.setAccessibleText(title.getText() + ", " + detail.getText() + ", " + p.getEstado()
                    + ". Abrir el módulo de pedidos");
            row.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ENTER || e.getCode() == javafx.scene.input.KeyCode.SPACE) {
                    navigate.accept(Route.PEDIDOS);
                }
            });
            Tooltip.install(row, new Tooltip("Abrir el módulo de pedidos"));
            activity.getChildren().add(row);
        }
        if (activity.getChildren().isEmpty()) {
            activity.getChildren().add(new EmptyState(Feather.INBOX, "Sin pedidos", "Todavía no hay actividad."));
        }
    }

    private static Sparkline sparkline(double[] values, String[] labels, DoubleFunction<String> format) {
        Sparkline spark = new Sparkline();
        spark.setValues(values, labels, format);
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

    /** Para las capturas de documentación: abre la ficha de un mes en la gráfica de tendencia. */
    public void highlightTrend(int index) {
        trendChart.highlight(index);
    }

    private static String greetingForNow() {
        int h = LocalTime.now().getHour();
        if (h < 6) {
            return "Buenas noches";
        }
        if (h < 12) {
            return "Buenos días";
        }
        return h < 20 ? "Buenas tardes" : "Buenas noches";
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
