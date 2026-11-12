package com.jardineria.application.port.in;

import com.jardineria.domain.model.analytics.CategoryMetric;
import com.jardineria.domain.model.analytics.ClienteFinancialSummary;
import com.jardineria.domain.model.analytics.MonthlyAmount;
import com.jardineria.domain.model.analytics.MonthlyStatusCount;

import java.time.Year;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de entrada: consultas analíticas para el panel de métricas.
 * Los filtros por año son opcionales: {@code null} significa "todos los años".
 */
public interface AnalyticsUseCase {

    List<MonthlyAmount> ventasPorMes(Year anio);

    List<MonthlyAmount> pagosPorMes(Year anio);

    List<MonthlyStatusCount> pedidosPorMesYEstado(Year anio);

    List<CategoryMetric> ventasPorGama();

    List<CategoryMetric> clientesPorPais();

    List<CategoryMetric> pagosPorFormaPago();

    List<CategoryMetric> topClientes(int limite);

    Optional<ClienteFinancialSummary> resumenCliente(int codigoCliente);

    /** Años con actividad comercial (pedidos), en orden ascendente. */
    List<Year> aniosDisponibles();

    /**
     * Últimos {@code puntos} meses con ventas, rellenando huecos con cero.
     * Útil para microtendencias (sparklines) de las tarjetas KPI.
     */
    double[] tendenciaVentas(int puntos);
}
