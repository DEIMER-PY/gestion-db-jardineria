package com.jardineria.domain.repository;

import com.jardineria.domain.model.Pago;
import com.jardineria.domain.model.PagoId;

import java.math.BigDecimal;
import java.util.List;

/**
 * Puerto de repositorio para la entidad Pago.
 */
public interface PagoRepository extends CrudRepository<Pago, PagoId> {

    /**
     * Busca los pagos efectuados por un cliente determinado.
     *
     * @param codigoCliente Código del cliente
     * @return Lista de pagos realizados
     */
    List<Pago> findByCliente(Integer codigoCliente);

    /**
     * Busca pagos filtrados por el método de pago utilizado.
     *
     * @param formaPago Forma de pago (ej: 'PayPal', 'Transferencia', 'Tarjeta', 'Cheque')
     * @return Lista de pagos
     */
    List<Pago> findByFormaPago(String formaPago);

    /**
     * Calcula la sumatoria total de todos los pagos registrados.
     *
     * @return Suma total monetaria
     */
    BigDecimal totalPagos();
}
