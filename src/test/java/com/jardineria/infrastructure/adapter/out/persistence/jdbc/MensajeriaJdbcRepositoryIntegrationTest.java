package com.jardineria.infrastructure.adapter.out.persistence.jdbc;

import com.jardineria.domain.model.chat.Canal;
import com.jardineria.domain.model.chat.Mensaje;
import com.jardineria.infrastructure.config.DatabaseConnectionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** SQL real de la mensajería contra el MySQL de Docker (se omite si no está levantado o sin las tablas). */
class MensajeriaJdbcRepositoryIntegrationTest {
    private final MensajeriaJdbcRepository repo = new MensajeriaJdbcRepository();
    private final String nombre = "prueba-" + System.nanoTime();

    @BeforeAll
    static void requiereEsquema() throws Exception {
        assumeTrue(DatabaseConnectionManager.isAvailable(3), "MySQL no disponible");
        try (Connection c = DatabaseConnectionManager.getInstance().getConnection();
             ResultSet rs = c.getMetaData().getTables(null, null, "mensaje", null)) {
            assumeTrue(rs.next(), "Sin tablas de mensajería: aplica docker/init/02-chat.sql");
        }
    }

    @AfterEach
    void limpiar() throws Exception {
        try (Connection c = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM canal WHERE nombre = ?")) {
            ps.setString(1, nombre);
            ps.executeUpdate();
        }
    }

    @Test
    @DisplayName("Crea canales, publica, lee en orden, sondea y cuenta los no leídos sin retroceder")
    void cicloCompleto() {
        assertThat(repo.canales()).extracting(Canal::nombre).contains("general", "oficina-tal-es");

        Canal canal = repo.crearCanal(nombre, Canal.Tipo.GENERAL, "Canal de prueba");
        assertThat(repo.crearCanal(nombre, Canal.Tipo.GENERAL, "otra")).isEqualTo(canal);
        assertThat(repo.canalPorNombre(nombre)).contains(canal);

        long antes = repo.ultimoMensaje();
        Mensaje a = repo.publicar(canal.id(), 1, "Primero con tildes: ñandú");
        Mensaje b = repo.publicar(canal.id(), 2, "Segundo @marcos.magana");
        Mensaje c = repo.publicar(canal.id(), 2, "Tercero");
        assertThat(a.enviado()).isNotNull();
        assertThat(a.texto()).isEqualTo("Primero con tildes: ñandú");
        assertThat(repo.ultimoMensaje()).isEqualTo(c.id());

        assertThat(repo.ultimos(canal.id(), 2)).extracting(Mensaje::id).containsExactly(b.id(), c.id());
        List<Mensaje> nuevos = repo.posteriores(antes, 50);
        assertThat(nuevos).extracting(Mensaje::id).contains(a.id(), b.id(), c.id()).isSorted();

        // Marcos (1) no ha leído los dos de Ruben (2); sus propios mensajes no cuentan
        assertThat(repo.noLeidos(1)).containsEntry(canal.id(), 2L);
        repo.marcarLeido(canal.id(), 1, b.id());
        assertThat(repo.noLeidos(1)).containsEntry(canal.id(), 1L);
        repo.marcarLeido(canal.id(), 1, a.id());
        assertThat(repo.noLeidos(1)).containsEntry(canal.id(), 1L);
        repo.marcarLeido(canal.id(), 1, c.id());
        assertThat(repo.noLeidos(1)).doesNotContainKey(canal.id());
    }
}
