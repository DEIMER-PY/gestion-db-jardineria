package com.jardineria.domain.pattern.decorator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Decorador Concreto: Aplicación del Impuesto al Valor Añadido (IVA 21%).
 */
public class VatTaxDecorator extends AbstractPriceDecorator {
    private static final BigDecimal IVA_PORC = new BigDecimal("0.21");

    public VatTaxDecorator(PriceCalculator wrappee) {
        super(wrappee);
    }

    @Override
    public BigDecimal calcularTotal(BigDecimal precioUnitario, int cantidad) {
        BigDecimal base = wrappee.calcularTotal(precioUnitario, cantidad);
        desgloseLocal.clear();
        BigDecimal impuesto = base.multiply(IVA_PORC).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalConIva = base.add(impuesto);
        desgloseLocal.add(String.format("IVA Estándar (21%%): +%s €", impuesto));
        return totalConIva;
    }
}
