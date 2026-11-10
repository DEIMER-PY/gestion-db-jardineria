package com.jardineria.domain.repository;

import com.jardineria.domain.model.DetallePedido;
import com.jardineria.domain.model.DetallePedidoId;

import java.util.List;

/**
 * Puerto de repositorio para la entidad DetallePedido.
 */
public interface DetallePedidoRepository extends CrudRepository<DetallePedido, DetallePedidoId> {

    /**
     * Obtiene todas las líneas de detalle pertenecientes a un pedido.
     *
     * @param codigoPedido Código del pedido
     * @return Lista de detalles ordenados por número de línea
     */
    List<DetallePedido> findByPedido(Integer codigoPedido);

    /**
     * Elimina todos los detalles pertenecientes a un pedido.
     *
     * @param codigoPedido Código del pedido
     * @return true si se eliminaron registros
     */
    boolean deleteByPedido(Integer codigoPedido);
}
