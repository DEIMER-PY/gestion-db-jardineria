package com.jardineria.domain.model.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatModelTest {

    @Test
    @DisplayName("Los canales directos se nombran igual en cualquier orden y solo los ven sus participantes")
    void canalesDirectos() {
        assertThat(Canal.nombreDirecto(7, 3)).isEqualTo("dm-3-7").isEqualTo(Canal.nombreDirecto(3, 7));
        Canal dm = new Canal(1, "dm-3-7", Canal.Tipo.DIRECTO, null, null);
        assertThat(dm.visiblePara(3)).isTrue();
        assertThat(dm.visiblePara(7)).isTrue();
        assertThat(dm.visiblePara(37)).isFalse();
        assertThat(dm.otroParticipante(3)).hasValue(7);
        assertThat(dm.otroParticipante(5)).isEmpty();
        assertThat(new Canal(2, "general", Canal.Tipo.GENERAL, null, null).visiblePara(99)).isTrue();
    }

    @Test
    @DisplayName("Las menciones se reconocen sin tildes ni mayúsculas y sin la puntuación final")
    void menciones() {
        Mensaje m = new Mensaje(1, 1, 3, "El #14 sigue pendiente; @Felipe.Rosas, ¿puedes llamar? Copio a @marcos.magaña.", null);
        assertThat(m.menciones()).containsExactly("felipe.rosas", "marcos.magana");
        assertThat(m.menciona("felipe.rosas")).isTrue();
        assertThat(m.menciona("Marcos.Magaña")).isTrue();
        assertThat(m.menciona("maria.solis")).isFalse();
        assertThat(new Mensaje(2, 1, 3, "correo: ventas@jardineria.es", null).menciones()).isEmpty();
    }

    @Test
    @DisplayName("El identificador usa el primer nombre y el primer apellido, sin tildes")
    void identificador() {
        assertThat(Mensaje.identificador("Juan Carlos", "Ortiz")).isEqualTo("juan.ortiz");
        assertThat(Mensaje.identificador("María", "Solís")).isEqualTo("maria.solis");
        assertThat(Mensaje.identificador("Ana", null)).isEqualTo("ana");
    }
}
