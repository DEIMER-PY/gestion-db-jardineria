package com.jardineria.domain.model.analytics;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Métrica agregada por categoría (gama, país, forma de pago, cliente...).
 *
 * @param label  etiqueta legible de la categoría
 * @param count  número de registros (o unidades) agrupados
 * @param amount importe asociado (cero cuando la métrica es solo de conteo)
 */
public record CategoryMetric(String label, long count, BigDecimal amount) {
    public CategoryMetric {
        Objects.requireNonNull(label, "La etiqueta es obligatoria");
        amount = amount == null ? BigDecimal.ZERO : amount;
    }

    public static CategoryMetric ofCount(String label, long count) {
        return new CategoryMetric(label, count, BigDecimal.ZERO);
    }
}
