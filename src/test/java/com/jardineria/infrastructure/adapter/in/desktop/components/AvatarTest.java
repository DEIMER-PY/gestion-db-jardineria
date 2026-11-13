package com.jardineria.infrastructure.adapter.in.desktop.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AvatarTest {

    @Test
    @DisplayName("Las iniciales toman como máximo dos palabras y toleran puntos, guiones y vacíos")
    void iniciales() {
        assertThat(Avatar.initials("Jardinería Huerta")).isEqualTo("JH");
        assertThat(Avatar.initials("  gardening   associates  inc")).isEqualTo("GA");
        assertThat(Avatar.initials("J.M. Sport")).isEqualTo("JM");
        assertThat(Avatar.initials("Tierra-Verde")).isEqualTo("TV");
        assertThat(Avatar.initials("Mardoqueo")).isEqualTo("M");
        assertThat(Avatar.initials("   ")).isEqualTo("?");
        assertThat(Avatar.initials(null)).isEqualTo("?");
    }

    @Test
    @DisplayName("El tono es estable para un mismo código y válido también con códigos negativos")
    void tonoEstable() {
        assertThat(Avatar.toneFor(7)).isEqualTo(Avatar.toneFor(7)).isEqualTo(Avatar.toneFor(13));
        assertThat(Avatar.toneFor(-1)).startsWith("tone-");
    }
}
