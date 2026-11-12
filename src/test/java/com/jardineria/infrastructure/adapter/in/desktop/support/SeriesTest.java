package com.jardineria.infrastructure.adapter.in.desktop.support;

import com.jardineria.domain.model.analytics.MonthlyAmount;
import com.jardineria.domain.model.analytics.MonthlyStatusCount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SeriesTest {

    @Test
    @DisplayName("statusByMonth rellena meses vacíos, normaliza estados y respeta el límite de meses")
    void statusByMonth() {
        List<MonthlyStatusCount> data = List.of(
                new MonthlyStatusCount(YearMonth.of(2009, 1), "entregado", 3),
                new MonthlyStatusCount(YearMonth.of(2009, 1), "Rechazado", 1),
                new MonthlyStatusCount(YearMonth.of(2009, 3), "PENDIENTE", 2));

        Map<YearMonth, Map<String, Long>> all = Series.statusByMonth(data, 0);
        assertThat(all.keySet()).containsExactly(YearMonth.of(2009, 1), YearMonth.of(2009, 2), YearMonth.of(2009, 3));
        assertThat(all.get(YearMonth.of(2009, 1))).containsEntry("Entregado", 3L).containsEntry("Rechazado", 1L);
        assertThat(all.get(YearMonth.of(2009, 2)).values()).containsOnly(0L);
        assertThat(all.get(YearMonth.of(2009, 3))).containsEntry("Pendiente", 2L);

        assertThat(Series.statusByMonth(data, 2).keySet()).containsExactly(YearMonth.of(2009, 2), YearMonth.of(2009, 3));
        assertThat(Series.totals(all)).containsExactly(4d, 0d, 2d);
        assertThat(Series.statusByMonth(List.of(), 12)).isEmpty();
    }

    @Test
    @DisplayName("alignMonthly alinea ventas y cobros sobre el mismo rango consecutivo")
    void alignMonthly() {
        List<MonthlyAmount> ventas = List.of(new MonthlyAmount(YearMonth.of(2008, 11), new BigDecimal("50"), 1));
        List<MonthlyAmount> cobros = List.of(new MonthlyAmount(YearMonth.of(2009, 1), new BigDecimal("20"), 1));

        Map<YearMonth, double[]> aligned = Series.alignMonthly(ventas, cobros);
        assertThat(aligned.keySet()).containsExactly(YearMonth.of(2008, 11), YearMonth.of(2008, 12), YearMonth.of(2009, 1));
        assertThat(aligned.get(YearMonth.of(2008, 11))).containsExactly(50d, 0d);
        assertThat(aligned.get(YearMonth.of(2008, 12))).containsExactly(0d, 0d);
        assertThat(aligned.get(YearMonth.of(2009, 1))).containsExactly(0d, 20d);
        assertThat(Series.alignMonthly(List.of(), List.of())).isEmpty();
    }

    @Test
    @DisplayName("trend compara la segunda mitad de la serie con la primera")
    void trend() {
        assertThat(Series.trend(new double[]{1, 1, 2, 2})).isCloseTo(1.0, within(1e-9));
        assertThat(Series.trend(new double[]{4, 4, 2, 2})).isCloseTo(-0.5, within(1e-9));
        assertThat(Series.trend(new double[]{0, 0, 3})).isEqualTo(1);
        assertThat(Series.trend(new double[]{5})).isZero();
        assertThat(Series.trend(null)).isZero();
    }
}
