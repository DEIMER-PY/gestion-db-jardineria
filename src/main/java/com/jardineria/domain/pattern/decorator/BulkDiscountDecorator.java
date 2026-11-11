package com.jardineria.domain.pattern.decorator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Decorador Concreto: Descuento por Volumen de Compra.
 * Aplica un 10% de descuento si la cantidad supera las 10 unidades.
 */
public class BulkDiscountDecorator extends AbstractPriceDecorator {
    private static final BigDecimal DESCUENTO_PORC = new BigDecimal("0.10");

    public BulkDiscountDecorator(PriceCalculator wrappee) {
        super(wrappee);
    }

    @Override
    public BigDecimal calcularTotal(BigDecimal precioUnitario, int cantidad) {
        BigDecimal base = wrappee.calcularTotal(precioUnitario, cantidad);
        desgloseLocal.clear();
        if (cantidad >= 10) {
            BigDecimal descuento = base.multiply(DESCUENTO_PORC).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalConDescuento = base.subtract(descuento);
            desgloseLocal.add(String.format("Descuento por Volumen (>= 10 uds, -10%%): -%s €", descuento));
            return totalConDescuento;
        } else {
            desgloseLocal.add("Descuento por Volumen: No aplica (< 10 uds)");
            return base;
        }
    }
}
