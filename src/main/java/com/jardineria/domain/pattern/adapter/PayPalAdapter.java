package com.jardineria.domain.pattern.adapter;

import com.jardineria.domain.model.Pago;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Adaptador Concreto (Adapter) para PayPal.
 * Traduce la solicitud del dominio de Jardinería al SDK de PayPal
 * y convierte el resultado en la entidad de dominio Pago.
 */
public class PayPalAdapter implements PaymentGatewayPort {
    private final PayPalExternalSdk payPalSdk;

    public PayPalAdapter(PayPalExternalSdk payPalSdk) {
        this.payPalSdk = (payPalSdk != null) ? payPalSdk : new PayPalExternalSdk();
    }

    @Override
    public String getNombrePasarela() {
        return "PayPal";
    }

    @Override
    public Pago procesarCobro(int codigoCliente, String idTransaccion, BigDecimal importe) {
        // Conversión a formato de la API de PayPal (céntimos enteros)
        long centimos = importe.multiply(BigDecimal.valueOf(100)).longValue();
        String fakeEmail = "cliente" + codigoCliente + "@jardineria.com";

        // Delegación al SDK externo
        PayPalExternalSdk.PayPalTransactionResult result = payPalSdk.executeCapture(idTransaccion, centimos, fakeEmail);

        // Mapeo inverso a entidad de dominio Pago
        return new Pago(codigoCliente, "PayPal", result.payKey(), LocalDate.now(), importe);
    }
}
