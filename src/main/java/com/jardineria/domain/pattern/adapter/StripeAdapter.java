package com.jardineria.domain.pattern.adapter;

import com.jardineria.domain.model.Pago;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Adaptador Concreto (Adapter) para Stripe.
 * Traduce los parámetros al API de Stripe y retorna un Pago de dominio.
 */
public class StripeAdapter implements PaymentGatewayPort {
    private final StripeExternalSdk stripeSdk;

    public StripeAdapter(StripeExternalSdk stripeSdk) {
        this.stripeSdk = (stripeSdk != null) ? stripeSdk : new StripeExternalSdk();
    }

    @Override
    public String getNombrePasarela() {
        return "Tarjeta (Stripe)";
    }

    @Override
    public Pago procesarCobro(int codigoCliente, String idTransaccion, BigDecimal importe) {
        double amountDouble = importe.doubleValue();
        String customerRef = "cus_jardin_" + codigoCliente;

        // Delegación al SDK externo
        StripeExternalSdk.StripeChargeResponse resp = stripeSdk.createCharge(customerRef, amountDouble, "EUR");

        // Retorno de entidad de dominio normalizada
        return new Pago(codigoCliente, "Tarjeta", resp.chargeId(), LocalDate.now(), importe);
    }
}
