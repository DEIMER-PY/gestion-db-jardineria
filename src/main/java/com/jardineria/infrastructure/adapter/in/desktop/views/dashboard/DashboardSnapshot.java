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
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fotografía inmutable de todos los datos del panel general, obtenida en una sola
 * tarea en segundo plano para renderizar la pantalla de una vez (sin parpadeos).
 */
public record DashboardSnapshot(
        DashboardMetrics metrics,
        BigDecimal totalVentas,
        Map<YearMonth, Map<String, Long>> pedidosPorMes,
        double[] tendenciaPedidos,
        double[] tendenciaVentas,
        double[] tendenciaCobros,
        List<CategoryMetric> topClientes,
        List<CategoryMetric> clientesPorPais,
        List<Producto> stockCritico,
        List<Pedido> ultimosPedidos,
        Map<Integer, String> nombresClientes
) {
    /** Meses mostrados en la gráfica principal. */
    public static final int MESES = 12;

    /** Consulta los puertos de entrada y compone la fotografía (bloqueante: usar en segundo plano). */
    public static DashboardSnapshot load(DashboardUseCase dashboard, AnalyticsUseCase analytics, ClienteUseCase clientes) {
        DashboardMetrics metrics = dashboard.obtenerMetricas();
        List<MonthlyAmount> ventas = analytics.ventasPorMes(null);
        BigDecimal totalVentas = ventas.stream().map(MonthlyAmount::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<YearMonth, Map<String, Long>> matriz = Series.statusByMonth(analytics.pedidosPorMesYEstado(null), MESES);
        return new DashboardSnapshot(
                metrics,
                totalVentas,
                matriz,
                Series.totals(matriz),
                analytics.tendenciaVentas(MESES),
                analytics.tendenciaCobros(MESES),
                analytics.clientesMasActivos(5),
                analytics.clientesPorPais(),
                dashboard.obtenerAlertasStock(),
                dashboard.obtenerUltimosPedidos(6),
                clientes.listarTodos().stream().collect(Collectors.toMap(
                        Cliente::getCodigoCliente, Cliente::getNombreCliente, (a, b) -> a)));
    }
}
