package com.jardineria.application.facade;

import com.jardineria.domain.model.Pago;
import com.jardineria.domain.model.Pedido;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado completo devuelto por la Fachada tras orquestar el checkout transaccional.
 */
public record CheckoutResult(
        boolean exito,
        String mensaje,
        Pedido pedido,
        Pago pago,
        BigDecimal totalFacturado,
        List<String> desglosePrecios
) {}
