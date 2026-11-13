package com.jardineria.infrastructure.adapter.in.desktop.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeysTest {

    @Test
    @DisplayName("Detecta macOS por el nombre del sistema")
    void deteccion() {
        assertThat(Keys.isMac("Mac OS X")).isTrue();
        assertThat(Keys.isMac("Windows 11")).isFalse();
        assertThat(Keys.isMac("Linux")).isFalse();
    }

    @Test
    @DisplayName("En Windows y Linux los textos no cambian")
    void sinCambios() {
        assertThat(Keys.text("Nuevo (Ctrl+N) · Alt ← · Supr", false)).isEqualTo("Nuevo (Ctrl+N) · Alt ← · Supr");
    }

    @Test
    @DisplayName("En macOS se usan los símbolos ⌘ ⌥ ⌫")
    void mac() {
        assertThat(Keys.text("Nuevo (Ctrl+N)", true)).isEqualTo("Nuevo (⌘N)");
        assertThat(Keys.text("Ctrl K", true)).isEqualTo("⌘ K");
        assertThat(Keys.text("Alt ←", true)).isEqualTo("⌥ ←");
        assertThat(Keys.text("Supr", true)).isEqualTo("⌫");
        assertThat(Keys.text("Alto contraste", true)).isEqualTo("Alto contraste");
    }
}
