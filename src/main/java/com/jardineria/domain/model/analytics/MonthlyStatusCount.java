package com.jardineria.domain.model.analytics;

import java.time.YearMonth;
import java.util.Objects;

/**
 * Número de pedidos de un mes agrupados por su estado logístico.
 */
public record MonthlyStatusCount(YearMonth month, String estado, long count) {
    public MonthlyStatusCount {
        Objects.requireNonNull(month, "El mes es obligatorio");
        Objects.requireNonNull(estado, "El estado es obligatorio");
    }
}
