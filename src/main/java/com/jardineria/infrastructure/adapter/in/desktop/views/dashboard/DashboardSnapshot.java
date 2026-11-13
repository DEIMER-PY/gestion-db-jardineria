package com.jardineria.infrastructure.adapter.in.desktop.views.dashboard;

import com.jardineria.application.dto.DashboardMetrics;
import com.jardineria.application.port.in.AnalyticsUseCase;
import com.jardineria.application.port.in.ClienteUseCase;
import com.jardineria.application.port.in.DashboardUseCase;
import com.jardineria.domain.model.Cliente;
import com.jardineria.domain.model.Pedido;
import com.jardineria.domain.model.Producto;
import com.jardineria.domain.model.analytics.CategoryMetric;
import com.jardineria.domain.model.analytics.MonthlyAmount;
import com.jardineria.infrastructure.adapter.in.desktop.support.Series;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fotografía inmutable de todos los datos del panel general, obtenida en una sola
 * tarea en segundo plano para renderizar la pantalla de una vez (sin parpadeos).
 *
 * @param pedidosPorMes    últimos {@link #MESES} meses: mes → (estado → pedidos)
 * @param actividadMensual todo el histórico: mes → pedidos (meses consecutivos, para el mapa de calor)
 * @param ventasCobros     últimos {@link #MESES} meses: mes → {ventas, cobros}
 * @param ventasPorGama    importe vendido por gama (treemap)
 * @param consultado       momento de la consulta (se muestra como "actualizado a las…")
 */
public record DashboardSnapshot(
        DashboardMetrics metrics,
        BigDecimal totalVentas,
        Map<YearMonth, Map<String, Long>> pedidosPorMes,
        Map<YearMonth, Long> actividadMensual,
        Map<YearMonth, double[]> ventasCobros,
        double[] tendenciaPedidos,
        List<CategoryMetric> topClientes,
        List<CategoryMetric> clientesPorPais,
        List<CategoryMetric> ventasPorGama,
        List<Producto> stockCritico,
        List<Pedido> ultimosPedidos,
        Map<Integer, String> nombresClientes,
        LocalDateTime consultado
) {
    /** Meses mostrados en las gráficas temporales. */
    public static final int MESES = 12;

    /** Consulta los puertos de entrada y compone la fotografía (bloqueante: usar en segundo plano). */
    public static DashboardSnapshot load(DashboardUseCase dashboard, AnalyticsUseCase analytics, ClienteUseCase clientes) {
        DashboardMetrics metrics = dashboard.obtenerMetricas();
        List<MonthlyAmount> ventas = analytics.ventasPorMes(null);
        List<MonthlyAmount> cobros = analytics.pagosPorMes(null);
        BigDecimal totalVentas = ventas.stream().map(MonthlyAmount::amount).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<YearMonth, Map<String, Long>> historico = Series.statusByMonth(analytics.pedidosPorMesYEstado(null), 0);
        Map<YearMonth, Long> actividad = new LinkedHashMap<>();
        historico.forEach((ym, row) -> actividad.put(ym, row.values().stream().mapToLong(Long::longValue).sum()));
        Map<YearMonth, Map<String, Long>> recientes = Series.lastEntries(historico, MESES);

        return new DashboardSnapshot(
                metrics,
                totalVentas,
                recientes,
                actividad,
                Series.lastEntries(Series.alignMonthly(ventas, cobros), MESES),
                Series.totals(recientes),
                analytics.clientesMasActivos(5),
                analytics.clientesPorPais(),
                analytics.ventasPorGama(),
                dashboard.obtenerAlertasStock(),
                dashboard.obtenerUltimosPedidos(6),
                clientes.listarTodos().stream().collect(Collectors.toMap(
                        Cliente::getCodigoCliente, Cliente::getNombreCliente, (a, b) -> a)),
                LocalDateTime.now());
    }
}
