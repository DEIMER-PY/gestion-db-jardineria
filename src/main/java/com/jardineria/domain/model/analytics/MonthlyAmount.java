package com.jardineria.domain.model.analytics;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Importe monetario agregado en un mes concreto (punto de una serie temporal).
 *
 * @param month  mes calendario del agregado
 * @param amount importe acumulado en el mes (nunca nulo)
 * @param count  número de registros que componen el agregado
 */
public record MonthlyAmount(YearMonth month, BigDecimal amount, long count) {
    public MonthlyAmount {
        Objects.requireNonNull(month, "El mes es obligatorio");
        amount = amount == null ? BigDecimal.ZERO : amount;
    }
}
