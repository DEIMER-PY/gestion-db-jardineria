package com.jardineria.infrastructure.adapter.in.desktop.views.dashboard;

import com.jardineria.application.dto.DashboardMetrics;
import com.jardineria.domain.model.analytics.CategoryMetric;
import com.jardineria.infrastructure.adapter.in.desktop.views.dashboard.DashboardInsights.Insight;
import com.jardineria.infrastructure.adapter.in.desktop.views.dashboard.DashboardInsights.Kind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardInsightsTest {

    private static DashboardSnapshot snapshot(DashboardMetrics m, BigDecimal ventas, Map<YearMonth, Long> actividad) {
        return new DashboardSnapshot(m, ventas, Map.of(), actividad, Map.of(), new double[0],
                List.of(new CategoryMetric("GoldFish Garden", 5, new BigDecimal("299.00"))),
                List.of(new CategoryMetric("Spain", 27, BigDecimal.ZERO), new CategoryMetric("France", 9, BigDecimal.ZERO)),
                List.of(new CategoryMetric("Frutales", 3, new BigDecimal("110.00")),
                        new CategoryMetric("Ornamentales", 2, new BigDecimal("40.00"))),
                List.of(), List.of(), Map.of(), LocalDateTime.now());
    }

    private static Map<YearMonth, Long> actividad() {
        Map<YearMonth, Long> a = new LinkedHashMap<>();
        a.put(YearMonth.of(2008, 12), 3L);
        a.put(YearMonth.of(2009, 1), 14L);
        a.put(YearMonth.of(2009, 2), 4L);
        return a;
    }

    @Test
    @DisplayName("Detecta pico, rechazo alto, cobros, líder, país, gama y stock")
    void lecturasCompletas() {
        DashboardMetrics m = new DashboardMetrics(36, 47, 10, 31, 9, new BigDecimal("632.50"), 24, 11, 12, 2);
        List<Insight> out = DashboardInsights.from(snapshot(m, new BigDecimal("379.00"), actividad()));

        assertThat(out).extracting(Insight::kind).containsExactly(
                Kind.PEAK, Kind.RISK, Kind.MONEY, Kind.LEADER, Kind.GEO, Kind.CATEGORY, Kind.WARNING);
        assertThat(out.get(0).title()).isEqualTo("Pico en enero de 2009");
        assertThat(out.get(1).title()).startsWith("Rechazo elevado");
        assertThat(out.get(3).title()).isEqualTo("GoldFish Garden lidera la cartera");
        assertThat(out.get(5).title()).isEqualTo("Frutales es la gama estrella");
        assertThat(out.get(6).title()).isEqualTo("2 productos bajo mínimos");
    }

    @Test
    @DisplayName("Con pocos rechazos, sin deuda y stock sano las lecturas son positivas")
    void lecturasPositivas() {
        DashboardMetrics m = new DashboardMetrics(36, 40, 10, 31, 9, new BigDecimal("100"), 38, 1, 1, 0);
        List<Insight> out = DashboardInsights.from(snapshot(m, new BigDecimal("150"), actividad()));

        assertThat(out).extracting(Insight::kind).contains(Kind.GOOD).doesNotContain(Kind.RISK);
        assertThat(out).anySatisfy(i -> assertThat(i.title()).startsWith("Pendiente de cobro"));
        assertThat(out).anySatisfy(i -> assertThat(i.title()).isEqualTo("Catálogo con stock suficiente"));
    }

    @Test
    @DisplayName("Sin datos no inventa lecturas")
    void sinDatos() {
        DashboardMetrics m = new DashboardMetrics(0, 0, 0, 0, 0, BigDecimal.ZERO, 0, 0, 0, 0);
        DashboardSnapshot s = new DashboardSnapshot(m, BigDecimal.ZERO, Map.of(), Map.of(), Map.of(), new double[0],
                List.of(), List.of(), List.of(), List.of(), List.of(), Map.of(), LocalDateTime.now());
        assertThat(DashboardInsights.from(s)).isEmpty();
    }
}
