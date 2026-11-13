package com.jardineria.infrastructure.adapter.in.desktop.views.analytics;

import com.jardineria.application.port.in.AnalyticsUseCase;
import com.jardineria.domain.model.analytics.CategoryMetric;
import com.jardineria.domain.model.analytics.MonthlyAmount;
import com.jardineria.domain.model.analytics.MonthlyStatusCount;
import com.jardineria.infrastructure.adapter.in.desktop.support.Series;

import java.math.BigDecimal;
import java.time.Year;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Datos del panel de métricas para un periodo (un año concreto o todo el histórico).
 *
 * @param anio            año filtrado; {@code null} = todos los años
 * @param pedidosPorMes   mes → (estado → pedidos), meses consecutivos
 * @param ventasCobros    mes → {ventas, cobros}, meses consecutivos
 * @param pedidosConLineas pedidos del periodo que tienen líneas de detalle (base del ticket medio)
 */
public record AnalyticsSnapshot(
        Year anio,
        List<Year> anios,
        BigDecimal ventas,
        BigDecimal cobros,
        long pedidosConLineas,
        double[] ticketPorMes,
        Map<String, Long> pedidosPorEstado,
        Map<YearMonth, Map<String, Long>> pedidosPorMes,
        Map<YearMonth, double[]> ventasCobros,
        List<CategoryMetric> ventasPorGama,
        List<CategoryMetric> clientesPorPais,
        List<CategoryMetric> formasPago,
        List<CategoryMetric> topClientes
) {
    /** Meses mostrados en la vista histórica (sin filtro de año) para que la gráfica sea legible. */
    static final int MESES_HISTORICO = 14;

    public static AnalyticsSnapshot load(AnalyticsUseCase analytics, Year anio) {
        List<MonthlyAmount> ventas = analytics.ventasPorMes(anio);
        List<MonthlyAmount> cobros = analytics.pagosPorMes(anio);
        List<MonthlyStatusCount> estados = analytics.pedidosPorMesYEstado(anio);
        Map<String, Long> porEstado = new LinkedHashMap<>();
        Series.statusByMonth(estados, 0).values()
                .forEach(row -> row.forEach((estado, n) -> porEstado.merge(estado, n, Long::sum)));
        return new AnalyticsSnapshot(
                anio,
                analytics.aniosDisponibles(),
                sum(ventas),
                sum(cobros),
                ventas.stream().mapToLong(MonthlyAmount::count).sum(),
                ventas.stream().mapToDouble(m -> m.count() == 0 ? 0 : m.amount().doubleValue() / m.count()).toArray(),
                porEstado,
                Series.statusByMonth(estados, anio == null ? MESES_HISTORICO : 0),
                Series.alignMonthly(ventas, cobros),
                analytics.ventasPorGama(),
                analytics.clientesPorPais(),
                analytics.pagosPorFormaPago(),
                analytics.topClientes(6));
    }

    /** Pedidos del periodo completo (no solo los meses visibles en la gráfica) por estado. */
    public long pedidos(String estado) {
        return pedidosPorEstado.getOrDefault(estado, 0L);
    }

    public long totalPedidos() {
        return Series.ESTADOS.stream().mapToLong(this::pedidos).sum();
    }

    public BigDecimal ticketMedio() {
        return pedidosConLineas == 0 ? BigDecimal.ZERO
                : ventas.divide(BigDecimal.valueOf(pedidosConLineas), 2, java.math.RoundingMode.HALF_UP);
    }

    private static BigDecimal sum(List<MonthlyAmount> serie) {
        return serie.stream().map(MonthlyAmount::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
