package com.jardineria.infrastructure.adapter.in.desktop.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityHeatmapTest {

    @Test
    @DisplayName("El nivel de intensidad va de 0 (vacío) a 4 (máximo)")
    void niveles() {
        assertThat(ActivityHeatmap.level(0, 14)).isZero();
        assertThat(ActivityHeatmap.level(1, 14)).isEqualTo(1);
        assertThat(ActivityHeatmap.level(7, 14)).isEqualTo(2);
        assertThat(ActivityHeatmap.level(11, 14)).isEqualTo(4);
        assertThat(ActivityHeatmap.level(14, 14)).isEqualTo(4);
        assertThat(ActivityHeatmap.level(3, 0)).isZero();
    }

    @Test
    @DisplayName("La variación anual se omite sin año base")
    void variacionAnual() {
        assertThat(ActivityHeatmap.delta(null, 10)).isEqualTo("—");
        assertThat(ActivityHeatmap.delta(0L, 10)).isEqualTo("—");
        assertThat(ActivityHeatmap.delta(10L, 15)).startsWith("+").contains("50");
        assertThat(ActivityHeatmap.delta(10L, 5)).startsWith("-").contains("50");
    }
}
