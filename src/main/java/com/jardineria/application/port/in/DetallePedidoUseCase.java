package com.jardineria.application.port.in;

import com.jardineria.domain.model.DetallePedido;
import com.jardineria.domain.model.DetallePedidoId;
import com.jardineria.domain.model.Producto;
import java.math.BigDecimal;
import java.util.List;

/**
 * Puerto de entrada (driving port): Casos de uso de las líneas de detalle de un pedido.
 * <p>
 * Los adaptadores de entrada (GUI JavaFX, CLI) dependen de esta abstracción y no del
 * servicio concreto, cumpliendo el Principio de Inversión de Dependencias (DIP).
 */
public interface DetallePedidoUseCase {

    DetallePedido agregarDetalle(DetallePedido detalle);

    DetallePedido actualizarDetalle(DetallePedido detalle);

    DetallePedido obtenerPorId(DetallePedidoId id);

    List<DetallePedido> listarPorPedido(Integer codigoPedido);

    boolean eliminarDetalle(DetallePedidoId id);

    long contarDetalles();
}
