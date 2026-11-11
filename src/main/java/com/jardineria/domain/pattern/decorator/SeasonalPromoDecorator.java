package com.jardineria.domain.pattern.decorator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Decorador Concreto: Campaña Promocional de Temporada Jardinería Primavera.
 * Aplica un 5% de descuento adicional sobre el subtotal acumulado.
 */
public class SeasonalPromoDecorator extends AbstractPriceDecorator {
    private static final BigDecimal PROMO_PORC = new BigDecimal("0.05");

    public SeasonalPromoDecorator(PriceCalculator wrappee) {
        super(wrappee);
    }

    @Override
    public BigDecimal calcularTotal(BigDecimal precioUnitario, int cantidad) {
        BigDecimal base = wrappee.calcularTotal(precioUnitario, cantidad);
        desgloseLocal.clear();
        BigDecimal descuento = base.multiply(PROMO_PORC).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = base.subtract(descuento);
        desgloseLocal.add(String.format("Promoción Primavera Verde (-5%%): -%s €", descuento));
        return total;
    }
}
