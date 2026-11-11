package com.jardineria.domain.pattern.decorator;

import java.math.BigDecimal;
import java.util.List;

/**
 * Componente base del Patrón Decorator (GoF).
 * Define el contrato para el cálculo de importes y desglose de capas de precios.
 */
public interface PriceCalculator {
    BigDecimal calcularTotal(BigDecimal precioUnitario, int cantidad);
    List<String> getDesglose();
}
