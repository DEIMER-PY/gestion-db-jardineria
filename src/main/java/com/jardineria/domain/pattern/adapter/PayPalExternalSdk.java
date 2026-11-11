package com.jardineria.domain.pattern.adapter;

/**
 * Sistema Adaptado (Adaptee) - SDK Externo de PayPal.
 * Posee métodos, tipos de datos y firmas incompatibles con nuestro dominio.
 */
public class PayPalExternalSdk {
    public record PayPalTransactionResult(String payKey, String status, long centsAmount, String buyerEmail) {}

    public PayPalTransactionResult executeCapture(String customTrackingId, long amountInCents, String email) {
        // Simula llamada a la API REST de PayPal v2/checkout/orders
        return new PayPalTransactionResult("PAYPAL-TX-" + customTrackingId, "COMPLETED", amountInCents, email);
    }
}
