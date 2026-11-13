package com.jardineria.infrastructure.adapter.in.desktop.views.agenda;

import com.jardineria.domain.model.Pago;
import com.jardineria.domain.model.Pedido;
import com.jardineria.infrastructure.adapter.in.common.Formatters;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.IntFunction;
import java.util.stream.Stream;

/**
 * Agenda calculada a partir de pedidos y pagos, sin tablas nuevas: entregas previstas
 * (pedidos abiertos), entregas realizadas y cobros, agrupados por día. Es lógica pura,
 * sin JavaFX, para poder probarla aparte.
 */
public final class AgendaModel {

    /** Tipo de evento; el orden define cómo se listan dentro de un día. */
    public enum Kind {
        PREVISTA("entrega prevista", "entregas previstas", "tone-amber"),
        ENTREGADO("entregado", "entregados", "tone-jade"),
        COBRO("cobro", "cobros", "tone-sky");

        private final String singular;
        private final String plural;
        private final String tone;

        Kind(String singular, String plural, String tone) {
            this.singular = singular;
            this.plural = plural;
            this.tone = tone;
        }

        public String count(long n) {
            return n + " " + (n == 1 ? singular : plural);
        }

        public String tone() {
            return tone;
        }
    }

    /** Evento de un día. {@code query} es lo que se busca al abrir su módulo. */
    public record Entry(LocalDate date, Kind kind, String title, String detail, BigDecimal amount, String query) {
    }

    /** Pedido abierto cuya fecha prevista ya pasó respecto a la fecha de referencia. */
    public record Overdue(Pedido pedido, String cliente, long dias) {
    }

    /** Cifras de un mes. {@code puntualidad} es null si no hubo entregas con fecha prevista. */
    public record MonthStats(long previstas, long entregadas, Double puntualidad, long cobros, BigDecimal cobrado) {
    }

    private final Map<LocalDate, List<Entry>> byDay;
    private final List<Overdue> overdue;
    private final List<Pedido> pedidos;
    private final LocalDate reference;

    private AgendaModel(Map<LocalDate, List<Entry>> byDay, List<Overdue> overdue, List<Pedido> pedidos, LocalDate reference) {
        this.byDay = byDay;
        this.overdue = overdue;
        this.pedidos = pedidos;
        this.reference = reference;
    }

    /**
     * @param today fecha real de hoy; la referencia es el último pedido o cobro registrado si es anterior
     *              (los datos de ejemplo terminan en 2009 y, si no, todo saldría «retrasado» desde hace años).
     *              Las fechas de entrega no cuentan: entregar hoy un pedido antiguo no mueve el reloj del negocio.
     */
    public static AgendaModel build(List<Pedido> pedidos, List<Pago> pagos, IntFunction<String> cliente, LocalDate today) {
        Map<LocalDate, List<Entry>> byDay = new TreeMap<>();
        for (Pedido p : pedidos) {
            String who = Formatters.orDash(cliente.apply(p.getCodigoCliente() == null ? -1 : p.getCodigoCliente()));
            if (isOpen(p) && p.getFechaEsperada() != null) {
                add(byDay, new Entry(p.getFechaEsperada(), Kind.PREVISTA, "Pedido #" + p.getCodigoPedido(), who, null,
                        "#" + p.getCodigoPedido()));
            }
            if (p.getFechaEntrega() != null) {
                long tarde = p.getFechaEsperada() == null ? 0 : ChronoUnit.DAYS.between(p.getFechaEsperada(), p.getFechaEntrega());
                add(byDay, new Entry(p.getFechaEntrega(), Kind.ENTREGADO, "Pedido #" + p.getCodigoPedido(),
                        who + (tarde > 0 ? " · " + tarde + (tarde == 1 ? " día tarde" : " días tarde") : " · a tiempo"),
                        null, "#" + p.getCodigoPedido()));
            }
        }
        for (Pago p : pagos) {
            if (p.getFechaPago() != null) {
                add(byDay, new Entry(p.getFechaPago(), Kind.COBRO, Formatters.orDash(p.getFormaPago()),
                        Formatters.orDash(cliente.apply(p.getCodigoCliente() == null ? -1 : p.getCodigoCliente())),
                        p.getTotal(), p.getIdTransaccion()));
            }
        }
        byDay.values().forEach(l -> l.sort(Comparator.comparing(Entry::kind).thenComparing(Entry::title)));

        LocalDate last = Stream.concat(
                        pedidos.stream().map(Pedido::getFechaPedido),
                        pagos.stream().map(Pago::getFechaPago))
                .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(today);
        LocalDate reference = last.isBefore(today) ? last : today;

        List<Overdue> overdue = pedidos.stream()
                .filter(p -> isOpen(p) && p.getFechaEsperada() != null && p.getFechaEsperada().isBefore(reference))
                .map(p -> new Overdue(p, Formatters.orDash(cliente.apply(p.getCodigoCliente() == null ? -1 : p.getCodigoCliente())),
                        ChronoUnit.DAYS.between(p.getFechaEsperada(), reference)))
                .sorted(Comparator.comparingLong(Overdue::dias).reversed())
                .toList();
        return new AgendaModel(byDay, overdue, List.copyOf(pedidos), reference);
    }

    /** Abierto = ni entregado ni rechazado. */
    static boolean isOpen(Pedido p) {
        String e = Formatters.fold(p.getEstado());
        return p.getFechaEntrega() == null && !e.startsWith("entregado") && !e.startsWith("rechazado");
    }

    private static void add(Map<LocalDate, List<Entry>> byDay, Entry e) {
        byDay.computeIfAbsent(e.date(), d -> new ArrayList<>()).add(e);
    }

    public List<Entry> on(LocalDate day) {
        return byDay.getOrDefault(day, List.of());
    }

    public long count(LocalDate day, Kind kind) {
        return on(day).stream().filter(e -> e.kind() == kind).count();
    }

    /** Último día con algún evento hasta la referencia (incluida); la propia referencia si no hay ninguno. */
    public LocalDate lastEventDay() {
        return byDay.keySet().stream().filter(d -> !d.isAfter(reference))
                .max(Comparator.naturalOrder()).orElse(reference);
    }

    public List<Overdue> overdue() {
        return overdue;
    }

    public LocalDate reference() {
        return reference;
    }

    public MonthStats stats(YearMonth month) {
        List<Entry> entries = byDay.entrySet().stream()
                .filter(e -> YearMonth.from(e.getKey()).equals(month))
                .flatMap(e -> e.getValue().stream()).toList();
        List<Pedido> entregados = pedidos.stream()
                .filter(p -> p.getFechaEntrega() != null && p.getFechaEsperada() != null
                        && YearMonth.from(p.getFechaEntrega()).equals(month))
                .toList();
        Double puntualidad = entregados.isEmpty() ? null
                : entregados.stream().filter(p -> !p.getFechaEntrega().isAfter(p.getFechaEsperada())).count()
                / (double) entregados.size();
        return new MonthStats(
                entries.stream().filter(e -> e.kind() == Kind.PREVISTA).count(),
                entries.stream().filter(e -> e.kind() == Kind.ENTREGADO).count(),
                puntualidad,
                entries.stream().filter(e -> e.kind() == Kind.COBRO).count(),
                entries.stream().filter(e -> e.kind() == Kind.COBRO).map(Entry::amount).filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
