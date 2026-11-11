package com.jardineria.domain.pattern.adapter;

/**
 * Sistema Adaptado (Adaptee) - SDK Externo de Stripe.
 * Maneja cargos en formato de micro-unidades y nomenclatura propia de Stripe.
 */
public class StripeExternalSdk {
    public record StripeChargeResponse(String chargeId, String outcome, double amountCharged, String currency) {}

    public StripeChargeResponse createCharge(String stripeCustomerId, double amountEur, String currency) {
        // Simula la llamada a la API de Stripe Charges
        return new StripeChargeResponse("ch_stripe_" + System.currentTimeMillis(), "succeeded", amountEur, currency);
    }
}
