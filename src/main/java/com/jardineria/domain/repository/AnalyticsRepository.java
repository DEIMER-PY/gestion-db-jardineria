package com.jardineria.domain.repository;

import com.jardineria.domain.model.analytics.CategoryMetric;
import com.jardineria.domain.model.analytics.ClienteFinancialSummary;
import com.jardineria.domain.model.analytics.MonthlyAmount;
import com.jardineria.domain.model.analytics.MonthlyStatusCount;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida (solo lectura) para consultas analíticas agregadas.
 * <p>
 * Separado de los repositorios CRUD por el Principio de Segregación de Interfaces (ISP):
 * los casos de uso de métricas no necesitan operaciones de escritura.
 */
public interface AnalyticsRepository {

    /** Ventas brutas (cantidad x precio_unidad de las líneas de pedido) agregadas por mes. */
    List<MonthlyAmount> ventasPorMes();

    /** Pagos cobrados agregados por mes. */
    List<MonthlyAmount> pagosPorMes();

    /** Número de pedidos por mes y estado logístico. */
    List<MonthlyStatusCount> pedidosPorMesYEstado();

    /** Unidades vendidas (count) e importe por gama de producto. */
    List<CategoryMetric> ventasPorGama();

    /** Número de clientes por país. */
    List<CategoryMetric> clientesPorPais();

    /** Número de pagos e importe total por forma de pago. */
    List<CategoryMetric> pagosPorFormaPago();

    /** Clientes con mayor volumen comprado (label = nombre del cliente, count = pedidos). */
    List<CategoryMetric> topClientesPorCompras(int limite);

    /** Resumen financiero de un cliente concreto. */
    Optional<ClienteFinancialSummary> resumenCliente(int codigoCliente);
}
