package com.jardineria.domain.pattern.adapter;

import com.jardineria.domain.model.Pago;

import java.math.BigDecimal;

/**
 * Puerto Objetivo (Target) del Patrón Adapter (GoF).
 * Define la interfaz homogénea esperada por el sistema de jardinería
 * para procesar cobros y generar la entidad de dominio Pago.
 */
public interface PaymentGatewayPort {
    String getNombrePasarela();
    Pago procesarCobro(int codigoCliente, String idTransaccion, BigDecimal importe);
}
