package com.jardineria.application.dto;

import java.math.BigDecimal;

/**
 * DTO inmutable con las métricas agregadas del panel ejecutivo.
 */
public record DashboardMetrics(
        long totalClientes,
        long totalPedidos,
        long totalProductos,
        long totalEmpleados,
        long totalOficinas,
        BigDecimal totalIngresos,
        long pedidosEntregados,
        long pedidosPendientes,
        long pedidosRechazados,
        long totalBajoStock
) {
    public DashboardMetrics {
        totalIngresos = totalIngresos == null ? BigDecimal.ZERO : totalIngresos;
    }

    /** Fracción (0..1) de pedidos entregados sobre el total. */
    public double tasaEntrega() {
        return totalPedidos == 0 ? 0d : (double) pedidosEntregados / totalPedidos;
    }
}
