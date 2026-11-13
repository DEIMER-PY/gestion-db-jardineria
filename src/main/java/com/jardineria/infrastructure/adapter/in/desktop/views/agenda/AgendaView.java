package com.jardineria.infrastructure.adapter.in.desktop.views.agenda;

import com.jardineria.application.port.in.ClienteUseCase;
import com.jardineria.application.port.in.PagoUseCase;
import com.jardineria.application.port.in.PedidoUseCase;
import com.jardineria.domain.model.Cliente;
import com.jardineria.infrastructure.adapter.in.common.Formatters;
import com.jardineria.infrastructure.adapter.in.desktop.components.EmptyState;
import com.jardineria.infrastructure.adapter.in.desktop.components.KpiCard;
import com.jardineria.infrastructure.adapter.in.desktop.components.Panel;
import com.jardineria.infrastructure.adapter.in.desktop.components.ResponsiveGrid;
import com.jardineria.infrastructure.adapter.in.desktop.components.StatStrip;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import com.jardineria.infrastructure.adapter.in.desktop.support.Interactions;
import com.jardineria.infrastructure.adapter.in.desktop.support.Motion;
import com.jardineria.infrastructure.adapter.in.desktop.views.BaseView;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.feather.Feather;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Agenda mensual: entregas previstas, entregas realizadas y cobros en un calendario,
 * el detalle del día elegido y los pedidos retrasados. Cada pedido o cobro abre su
 * módulo ya filtrado. Se maneja con el ratón o con las flechas del teclado.
 */
public class AgendaView extends BaseView {
    private static final int MAX_OVERDUE = 8;

    private final PedidoUseCase pedidos;
    private final PagoUseCase pagos;
    private final ClienteUseCase clientes;
    private final BiConsumer<Route, String> navigate;

    private final Label monthLabel = new Label();
    private final GridPane calendar = new GridPane();
    private final VBox stats = new VBox();
    private final VBox dayList = new VBox(4);
    private final VBox overdueList = new VBox(4);
    private final Panel dayPanel = new Panel("Día", "", dayList);
    private final Panel overduePanel = new Panel("Pedidos retrasados", "", overdueList);
    private final Button prev = new Button(null, Icons.of(Feather.CHEVRON_LEFT, 16));
    private final Button next = new Button(null, Icons.of(Feather.CHEVRON_RIGHT, 16));

    private AgendaModel model;
    private YearMonth month;
    private LocalDate selected;

    public AgendaView(PedidoUseCase pedidos, PagoUseCase pagos, ClienteUseCase clientes, BiConsumer<Route, String> navigate) {
        super(Route.AGENDA);
        this.pedidos = pedidos;
        this.pagos = pagos;
        this.clientes = clientes;
        this.navigate = navigate;

        prev.getStyleClass().add("icon-button");
        prev.setAccessibleText("Mes anterior");
        prev.setTooltip(new Tooltip("Mes anterior"));
        prev.setOnAction(e -> showMonth(month.minusMonths(1), null));
        next.getStyleClass().add("icon-button");
        next.setAccessibleText("Mes siguiente");
        next.setTooltip(new Tooltip("Mes siguiente"));
        next.setOnAction(e -> showMonth(month.plusMonths(1), null));
        monthLabel.getStyleClass().add("agenda-month");
        monthLabel.setMinWidth(Region.USE_PREF_SIZE);

        Button jump = new Button(null, Icons.of(Feather.CROSSHAIR, 16));
        jump.getStyleClass().add("icon-button");
        jump.setAccessibleText("Ir al último día con actividad");
        jump.setTooltip(new Tooltip("Vuelve al último día con actividad (los retrasos se calculan a esa fecha)"));
        jump.setOnAction(e -> {
            if (model != null) {
                showMonth(YearMonth.from(model.lastEventDay()), model.lastEventDay());
            }
        });
        Button refresh = new Button("Actualizar", Icons.of(Feather.REFRESH_CW, 15));
        refresh.getStyleClass().add("accent");
        refresh.setOnAction(e -> refresh());
        HBox nav = new HBox(4, prev, monthLabel, next);
        nav.setAlignment(Pos.CENTER);
        nav.getStyleClass().add("agenda-nav");
        addActions(nav, jump, refresh);
    }

    @Override
    protected Node buildContent() {
        calendar.getStyleClass().add("agenda-grid");
        calendar.setHgap(6);
        calendar.setVgap(6);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(100.0 / 7);
            c.setHgrow(Priority.ALWAYS);
            calendar.getColumnConstraints().add(c);
        }
        calendar.setOnKeyPressed(e -> {
            if (selected == null) {
                return;
            }
            int delta = switch (e.getCode()) {
                case LEFT -> -1;
                case RIGHT -> 1;
                case UP -> -7;
                case DOWN -> 7;
                default -> 0;
            };
            if (delta != 0) {
                LocalDate target = selected.plusDays(delta);
                showMonth(YearMonth.from(target), target);
                focusSelected();
                e.consume();
            }
        });

        Panel calendarPanel = new Panel("Calendario", "Entregas previstas, entregas realizadas y cobros", calendar)
                .withActions(legend());
        ResponsiveGrid main = new ResponsiveGrid(320, 3, 16);
        main.add(calendarPanel, 2);
        main.add(dayPanel, 1);

        VBox body = new VBox(18, stats, main, overduePanel);
        body.getStyleClass().add("view-body");
        int delay = 0;
        for (Node n : body.getChildren()) {
            Motion.enter(n, delay);
            delay += 60;
        }
        refresh();
        return body;
    }

    private static Node legend() {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_RIGHT);
        for (AgendaModel.Kind k : AgendaModel.Kind.values()) {
            Label l = new Label(capitalize(k.count(2).replaceFirst("^2 ", "")), dot(k));
            l.getStyleClass().add("agenda-legend");
            box.getChildren().add(l);
        }
        return box;
    }

    private static Circle dot(AgendaModel.Kind k) {
        Circle c = new Circle(4);
        c.getStyleClass().addAll("badge-dot", k.tone());
        return c;
    }

    @Override
    public void refresh() {
        load(() -> {
            Map<Integer, String> nombres = clientes.listarTodos().stream()
                    .collect(Collectors.toMap(Cliente::getCodigoCliente, Cliente::getNombreCliente, (a, b) -> a));
            return AgendaModel.build(pedidos.listarTodos(), pagos.listarTodos(), nombres::get, LocalDate.now());
        }, m -> {
            model = m;
            if (month == null) {
                selected = m.lastEventDay();
                month = YearMonth.from(selected);
            }
            renderOverdue();
            showMonth(month, selected);
        });
    }

    private void showMonth(YearMonth target, LocalDate day) {
        if (model == null) {
            return;
        }
        month = target;
        selected = day != null ? day : target.atDay(1);
        monthLabel.setText(capitalize(Formatters.monthLong(month)));
        renderStats();
        renderCalendar();
        renderDay();
    }

    private void renderStats() {
        AgendaModel.MonthStats s = model.stats(month);
        stats.getChildren().setAll(new StatStrip()
                .stat(Feather.CLOCK, Formatters.integer(s.previstas()), "entregas previstas abiertas", KpiCard.Tone.AMBER)
                .stat(Feather.CHECK_CIRCLE, Formatters.integer(s.entregadas()), "pedidos entregados", KpiCard.Tone.ACCENT)
                .stat(Feather.TARGET, s.puntualidad() == null ? "—" : Formatters.percent(s.puntualidad()),
                        "entregados a tiempo", KpiCard.Tone.LIME)
                .stat(Feather.CREDIT_CARD, Formatters.compactCurrency(s.cobrado()),
                        Formatters.integer(s.cobros()) + (s.cobros() == 1 ? " cobro" : " cobros") + " en el mes", KpiCard.Tone.SKY));
    }

    private void renderCalendar() {
        calendar.getChildren().clear();
        for (int i = 0; i < 7; i++) {
            Label h = new Label(capitalize(DayOfWeek.of(i + 1).getDisplayName(TextStyle.SHORT, Formatters.ES)));
            h.getStyleClass().add("agenda-weekday");
            h.setMaxWidth(Double.MAX_VALUE);
            calendar.add(h, i, 0);
        }
        LocalDate first = month.atDay(1);
        LocalDate start = first.minusDays(first.getDayOfWeek().getValue() - 1L);
        for (int i = 0; i < 42; i++) {
            LocalDate d = start.plusDays(i);
            calendar.add(dayCell(d), i % 7, 1 + i / 7);
        }
    }

    private Node dayCell(LocalDate d) {
        Label num = new Label(String.valueOf(d.getDayOfMonth()));
        num.getStyleClass().add("agenda-day-number");
        VBox cell = new VBox(3, num);
        cell.getStyleClass().add("agenda-day");
        cell.setMaxWidth(Double.MAX_VALUE);
        cell.setMinWidth(0);
        StringBuilder spoken = new StringBuilder(Formatters.date(d));
        for (AgendaModel.Kind k : AgendaModel.Kind.values()) {
            long n = model.count(d, k);
            if (n > 0) {
                Label chip = new Label(String.valueOf(n), dot(k));
                chip.getStyleClass().add("agenda-chip");
                chip.setTooltip(new Tooltip(k.count(n)));
                cell.getChildren().add(chip);
                spoken.append(", ").append(k.count(n));
            }
        }
        if (!YearMonth.from(d).equals(month)) {
            cell.getStyleClass().add("agenda-day-outside");
        }
        if (d.equals(model.reference())) {
            cell.getStyleClass().add("agenda-day-reference");
            spoken.append(", fecha de referencia");
        }
        if (d.equals(selected)) {
            cell.getStyleClass().add("agenda-day-selected");
        }
        Interactions.activatable(cell, () -> {
            if (YearMonth.from(d).equals(month)) {
                selected = d;
                renderCalendar();
                renderDay();
                focusSelected();
            } else {
                showMonth(YearMonth.from(d), d);
            }
        });
        cell.setUserData(d);
        cell.setAccessibleText(spoken.toString());
        return cell;
    }

    private void focusSelected() {
        calendar.getChildren().stream().filter(n -> selected.equals(n.getUserData())).findFirst().ifPresent(Node::requestFocus);
    }

    private void renderDay() {
        List<AgendaModel.Entry> entries = model.on(selected);
        dayPanel.setTitleText(capitalize(selected.getDayOfWeek().getDisplayName(TextStyle.FULL, Formatters.ES))
                + " " + Formatters.date(selected));
        dayPanel.setCaption(entries.isEmpty() ? "Sin actividad" : entries.size() + (entries.size() == 1 ? " evento" : " eventos"));
        if (entries.isEmpty()) {
            dayList.getChildren().setAll(new EmptyState(Feather.CALENDAR, "Día sin actividad",
                    "Usa las flechas del teclado o elige otro día del calendario."));
            return;
        }
        dayList.getChildren().setAll(entries.stream().map(this::entryRow).toList());
    }

    private Node entryRow(AgendaModel.Entry e) {
        Label t = new Label(e.title(), dot(e.kind()));
        t.getStyleClass().add("timeline-title");
        t.setGraphicTextGap(8);
        Label d = new Label(capitalize(e.kind().count(1).replaceFirst("^1 ", "")) + " · " + e.detail());
        d.getStyleClass().add("timeline-detail");
        d.setWrapText(true);
        VBox text = new VBox(1, t, d);
        text.setMinWidth(0);
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox row = new HBox(10, text);
        if (e.amount() != null) {
            Label amount = new Label(Formatters.currency(e.amount()));
            amount.getStyleClass().add("line-amount");
            amount.setMinWidth(Region.USE_PREF_SIZE);
            row.getChildren().add(amount);
        }
        row.getChildren().add(Icons.of(Feather.CHEVRON_RIGHT, 14));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("timeline-row");
        Route target = e.kind() == AgendaModel.Kind.COBRO ? Route.PAGOS : Route.PEDIDOS;
        Interactions.activatable(row, () -> navigate.accept(target, e.query()));
        row.setAccessibleText(e.title() + ", " + d.getText() + ". Abrir en " + target.title());
        return row;
    }

    private void renderOverdue() {
        List<AgendaModel.Overdue> all = model.overdue();
        overduePanel.setCaption(all.isEmpty() ? "Ningún pedido abierto ha superado su fecha prevista"
                : all.size() + (all.size() == 1 ? " pedido abierto" : " pedidos abiertos")
                + " con la fecha prevista superada a " + Formatters.date(model.reference()));
        if (all.isEmpty()) {
            overdueList.getChildren().setAll(new EmptyState(Feather.CHECK_CIRCLE, "Todo al día",
                    "No hay entregas pendientes fuera de plazo."));
            return;
        }
        ResponsiveGrid grid = new ResponsiveGrid(300, 2, 8);
        all.stream().limit(MAX_OVERDUE).forEach(o -> grid.add(overdueRow(o), 1));
        overdueList.getChildren().setAll(grid);
        if (all.size() > MAX_OVERDUE) {
            Button more = new Button("Ver los " + all.size() + " en Pedidos", Icons.of(Feather.ARROW_RIGHT, 14));
            more.getStyleClass().add("flat");
            more.setOnAction(e -> navigate.accept(Route.PEDIDOS, "Pendiente"));
            overdueList.getChildren().add(more);
        }
    }

    private Node overdueRow(AgendaModel.Overdue o) {
        Label t = new Label("Pedido #" + o.pedido().getCodigoPedido());
        t.getStyleClass().add("timeline-title");
        Label d = new Label(o.cliente() + " · previsto el " + Formatters.date(o.pedido().getFechaEsperada()));
        d.getStyleClass().add("timeline-detail");
        VBox text = new VBox(1, t, d);
        text.setMinWidth(0);
        HBox.setHgrow(text, Priority.ALWAYS);
        Label late = new Label(Formatters.integer(o.dias()) + (o.dias() == 1 ? " día" : " días"));
        late.getStyleClass().addAll("badge", o.dias() > 30 ? "tone-coral" : "tone-amber");
        late.setMinWidth(Region.USE_PREF_SIZE);
        HBox row = new HBox(10, text, late);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("timeline-row");
        Interactions.activatable(row, () -> navigate.accept(Route.PEDIDOS, "#" + o.pedido().getCodigoPedido()));
        row.setAccessibleText("Pedido " + o.pedido().getCodigoPedido() + " de " + o.cliente() + ", "
                + late.getText() + " de retraso. Abrir en Pedidos");
        return row;
    }

    private static String capitalize(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
