package com.jardineria.domain.model.analytics;

import java.math.BigDecimal;

/**
 * Resumen financiero de un cliente: volumen comprado, pagado y uso de su línea de crédito.
 *
 * @param codigoCliente identificador del cliente
 * @param totalPedidos  número de pedidos realizados
 * @param totalComprado suma de (cantidad x precio) de todas sus líneas de pedido
 * @param totalPagado   suma de todos sus pagos registrados
 * @param limiteCredito límite de crédito concedido (cero si no tiene)
 */
public record ClienteFinancialSummary(int codigoCliente,
                                      long totalPedidos,
                                      BigDecimal totalComprado,
                                      BigDecimal totalPagado,
                                      BigDecimal limiteCredito) {
    public ClienteFinancialSummary {
        totalComprado = totalComprado == null ? BigDecimal.ZERO : totalComprado;
        totalPagado = totalPagado == null ? BigDecimal.ZERO : totalPagado;
        limiteCredito = limiteCredito == null ? BigDecimal.ZERO : limiteCredito;
    }

    /** Saldo pendiente: lo comprado que aún no se ha pagado (nunca negativo). */
    public BigDecimal saldoPendiente() {
        BigDecimal saldo = totalComprado.subtract(totalPagado);
        return saldo.signum() < 0 ? BigDecimal.ZERO : saldo;
    }

    /** Fracción (0..1) del límite de crédito consumida por el saldo pendiente. */
    public double usoCredito() {
        if (limiteCredito.signum() <= 0) {
            return 0d;
        }
        return Math.min(1d, saldoPendiente().doubleValue() / limiteCredito.doubleValue());
    }
}
