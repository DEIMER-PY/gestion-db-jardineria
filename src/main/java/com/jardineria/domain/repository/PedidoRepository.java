package com.jardineria.domain.repository;

import com.jardineria.domain.model.Pedido;

import java.util.List;

/**
 * Puerto de repositorio para la entidad Pedido.
 */
public interface PedidoRepository extends CrudRepository<Pedido, Integer> {

    /**
     * Busca pedidos realizados por un cliente específico.
     *
     * @param codigoCliente Código del cliente
     * @return Lista de pedidos
     */
    List<Pedido> findByCliente(Integer codigoCliente);

    /**
     * Busca pedidos filtrando por su estado (ej: 'Entregado', 'Pendiente', 'Rechazado').
     *
     * @param estado Estado del pedido
     * @return Lista de pedidos
     */
    List<Pedido> findByEstado(String estado);

    /**
     * Retorna la cantidad de pedidos agrupados por estado.
     *
     * @param estado Estado a contabilizar
     * @return Cantidad de pedidos
     */
    long countByEstado(String estado);
}
