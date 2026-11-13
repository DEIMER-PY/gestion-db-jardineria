package com.jardineria.infrastructure.adapter.in.desktop.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TreemapTest {

    @Test
    @DisplayName("squarify reparte toda el área en proporción a los valores")
    void areasProporcionales() {
        double[] values = {6, 6, 4, 3, 2, 2, 1};
        List<Treemap.Rect> rects = Treemap.squarify(values, 0, 0, 600, 400);
        assertThat(rects).hasSize(values.length);
        double total = 24;
        for (int i = 0; i < values.length; i++) {
            Treemap.Rect r = rects.get(i);
            assertThat(r.w() * r.h()).isCloseTo(600 * 400 * values[i] / total, within(1e-6));
            assertThat(r.x()).isBetween(-1e-9, 600.0);
            assertThat(r.y()).isBetween(-1e-9, 400.0);
            assertThat(r.x() + r.w()).isLessThanOrEqualTo(600 + 1e-6);
            assertThat(r.y() + r.h()).isLessThanOrEqualTo(400 + 1e-6);
        }
    }

    @Test
    @DisplayName("squarify mantiene relaciones de aspecto razonables")
    void aspectoRazonable() {
        List<Treemap.Rect> rects = Treemap.squarify(new double[]{6, 6, 4, 3, 2, 2, 1}, 0, 0, 600, 400);
        for (Treemap.Rect r : rects) {
            double ratio = Math.max(r.w() / r.h(), r.h() / r.w());
            assertThat(ratio).isLessThan(4);
        }
    }

    @Test
    @DisplayName("squarify sin datos o sin área devuelve lista vacía")
    void casosVacios() {
        assertThat(Treemap.squarify(new double[0], 0, 0, 100, 100)).isEmpty();
        assertThat(Treemap.squarify(new double[]{0, 0}, 0, 0, 100, 100)).isEmpty();
        assertThat(Treemap.squarify(new double[]{1}, 0, 0, 0, 100)).isEmpty();
    }
}
