package com.jardineria.application.port.in;

import com.jardineria.application.dto.DashboardMetrics;
import com.jardineria.domain.model.Pedido;
import com.jardineria.domain.model.Producto;

import java.util.List;

/**
 * Puerto de entrada: consultas del panel ejecutivo (KPIs, actividad reciente y alertas).
 */
public interface DashboardUseCase {

    /** Umbral de unidades por debajo del cual un producto se considera en stock crítico. */
    int UMBRAL_STOCK_CRITICO = 10;

    DashboardMetrics obtenerMetricas();

    /** Pedidos más recientes ordenados por fecha de pedido descendente. */
    List<Pedido> obtenerUltimosPedidos(int limite);

    /** Productos con stock por debajo de {@link #UMBRAL_STOCK_CRITICO}, del más crítico al menos. */
    List<Producto> obtenerAlertasStock();
}
