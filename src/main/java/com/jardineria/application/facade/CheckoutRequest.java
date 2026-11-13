package com.jardineria.application.facade;

import java.math.BigDecimal;
import java.util.List;

/**
 * Petición de Checkout para la Fachada Empresarial.
 */
public record CheckoutRequest(
        int codigoCliente,
        List<ItemCheckout> items,
        String metodoPago, // "PayPal", "Tarjeta", "Transferencia", "Efectivo"
        String idTransaccion,
        boolean aplicarDescuentoTemporada
) {
    public record ItemCheckout(String codigoProducto, int cantidad, BigDecimal precioUnitario) {}
}
