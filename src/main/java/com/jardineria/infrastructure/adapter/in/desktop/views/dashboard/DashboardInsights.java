package com.jardineria.infrastructure.adapter.in.desktop.views.dashboard;

import com.jardineria.application.dto.DashboardMetrics;
import com.jardineria.application.port.in.DashboardUseCase;
import com.jardineria.domain.model.analytics.CategoryMetric;
import com.jardineria.infrastructure.adapter.in.common.Formatters;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Lecturas automáticas de los datos del panel ("insights"): frases cortas que destacan
 * lo relevante para que no haya que deducirlo de las gráficas. Lógica pura y testeable,
 * sin JavaFX; la vista decide icono y color según {@link Kind}.
 */
public final class DashboardInsights {

    /** Tipo de lectura: decide el icono y el tono en la vista. */
    public enum Kind { PEAK, GOOD, WARNING, RISK, LEADER, GEO, CATEGORY, MONEY }

    /** Lectura: tipo, titular y explicación. */
    public record Insight(Kind kind, String title, String detail) {
    }

    /** Tasa de rechazo a partir de la cual se considera un riesgo. */
    static final double RECHAZO_ALTO = 0.20;

    private DashboardInsights() {
    }

    public static List<Insight> from(DashboardSnapshot s) {
        List<Insight> out = new ArrayList<>();
        DashboardMetrics m = s.metrics();

        // 1. Mes con más pedidos
        s.actividadMensual().entrySet().stream()
                .max(Map.Entry.<YearMonth, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .filter(e -> e.getValue() > 0)
                .ifPresent(e -> {
                    double share = m.totalPedidos() == 0 ? 0 : (double) e.getValue() / m.totalPedidos();
                    out.add(new Insight(Kind.PEAK, "Pico en " + Formatters.monthLong(e.getKey()),
                            e.getValue() + " pedidos, el " + Formatters.percent(share) + " de todo el histórico."));
                });

        // 2. Rechazos
        if (m.totalPedidos() > 0) {
            double rechazo = (double) m.pedidosRechazados() / m.totalPedidos();
            out.add(rechazo >= RECHAZO_ALTO
                    ? new Insight(Kind.RISK, "Rechazo elevado: " + Formatters.percent(rechazo),
                    m.pedidosRechazados() + " de " + m.totalPedidos() + " pedidos rechazados; el umbral sano es "
                            + Formatters.percent(RECHAZO_ALTO) + ".")
                    : new Insight(Kind.GOOD, "Rechazo bajo control: " + Formatters.percent(rechazo),
                    "Por debajo del umbral del " + Formatters.percent(RECHAZO_ALTO) + "."));
        }

        // 3. Cobros frente a ventas
        BigDecimal ventas = s.totalVentas();
        BigDecimal cobrado = m.totalIngresos();
        if (ventas.signum() > 0 || cobrado.signum() > 0) {
            BigDecimal diff = cobrado.subtract(ventas);
            out.add(diff.signum() >= 0
                    ? new Insight(Kind.MONEY, "Cobros por encima de lo vendido",
                    "Se ha cobrado " + Formatters.currency(diff) + " más que el importe de las líneas de pedido "
                            + "(pagos de pedidos sin líneas registradas).")
                    : new Insight(Kind.WARNING, "Pendiente de cobro: " + Formatters.currency(diff.negate()),
                    "Las ventas superan a los pagos registrados."));
        }

        // 4. Cliente líder
        s.topClientes().stream().findFirst().filter(c -> c.count() > 0).ifPresent(c ->
                out.add(new Insight(Kind.LEADER, c.label() + " lidera la cartera",
                        c.count() + (c.count() == 1 ? " pedido" : " pedidos")
                                + (c.amount().signum() > 0 ? " · " + Formatters.currency(c.amount()) + " en líneas" : "")
                                + ".")));

        // 5. Concentración geográfica
        if (m.totalClientes() > 0) {
            s.clientesPorPais().stream().max(Comparator.comparingLong(CategoryMetric::count)).ifPresent(c -> {
                double share = (double) c.count() / m.totalClientes();
                out.add(new Insight(Kind.GEO, Formatters.display(c.label()) + " concentra el " + Formatters.percent(share),
                        c.count() + " de " + m.totalClientes() + " clientes en " + s.clientesPorPais().size() + " países."));
            });
        }

        // 6. Gama que más factura
        BigDecimal totalGamas = s.ventasPorGama().stream().map(CategoryMetric::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalGamas.signum() > 0) {
            s.ventasPorGama().stream().max(Comparator.comparing(CategoryMetric::amount)).ifPresent(g -> {
                double share = g.amount().doubleValue() / totalGamas.doubleValue();
                out.add(new Insight(Kind.CATEGORY, Formatters.display(g.label()) + " es la gama estrella",
                        Formatters.currency(g.amount()) + " en ventas, el " + Formatters.percent(share) + " del total."));
            });
        }

        // 7. Stock
        if (m.totalBajoStock() > 0) {
            out.add(new Insight(Kind.WARNING, m.totalBajoStock() == 1 ? "1 producto bajo mínimos"
                    : m.totalBajoStock() + " productos bajo mínimos",
                    "Menos de " + DashboardUseCase.UMBRAL_STOCK_CRITICO + " unidades: conviene reponer antes de "
                            + "confirmar pedidos nuevos."));
        } else if (m.totalProductos() > 0) {
            out.add(new Insight(Kind.GOOD, "Catálogo con stock suficiente",
                    "Ningún producto está por debajo de " + DashboardUseCase.UMBRAL_STOCK_CRITICO + " unidades."));
        }
        return out;
    }
}
