package com.jardineria.application.port.in;

import com.jardineria.domain.model.Pedido;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Puerto de entrada (driving port): Casos de uso del ciclo de vida de los pedidos.
 * <p>
 * Los adaptadores de entrada (GUI JavaFX, CLI) dependen de esta abstracción y no del
 * servicio concreto, cumpliendo el Principio de Inversión de Dependencias (DIP).
 */
public interface PedidoUseCase {

    Pedido crearPedido(Pedido pedido);

    Pedido actualizarPedido(Pedido pedido);

    Pedido cambiarEstado(Integer codigoPedido, String nuevoEstado, LocalDate fechaEntrega);

    Pedido obtenerPorId(Integer codigoPedido);

    List<Pedido> listarTodos();

    boolean eliminarPedido(Integer codigoPedido);

    List<Pedido> buscarPorCliente(Integer codigoCliente);

    List<Pedido> buscarPorEstado(String estado);

    long contarPorEstado(String estado);

    long contarPedidos();
}
