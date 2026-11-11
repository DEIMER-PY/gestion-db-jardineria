package com.jardineria.domain.pattern.decorator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Componente Concreto del Patrón Decorator.
 * Representa la base del cálculo (precio base × cantidad) sin recargos ni descuentos.
 */
public class BasePriceCalculator implements PriceCalculator {
    private final List<String> desglose = new ArrayList<>();

    @Override
    public BigDecimal calcularTotal(BigDecimal precioUnitario, int cantidad) {
        desglose.clear();
        BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
        desglose.add(String.format("Precio base (%d uds × %s €): %s €", cantidad, precioUnitario, subtotal));
        return subtotal;
    }

    @Override
    public List<String> getDesglose() {
        return new ArrayList<>(desglose);
    }
}
