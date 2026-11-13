package com.jardineria.infrastructure.adapter.out.persistence.jdbc;

import com.jardineria.domain.model.analytics.CategoryMetric;
import com.jardineria.domain.model.analytics.MonthlyAmount;
import com.jardineria.infrastructure.config.DatabaseConnectionManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pruebas de integración de las consultas agregadas contra el MySQL del contenedor Docker.
 * Se omiten automáticamente si la base de datos no está levantada.
 */
class AnalyticsJdbcRepositoryIntegrationTest {

    private final AnalyticsJdbcRepository repository = new AnalyticsJdbcRepository();

    @BeforeAll
    static void requiereBaseDeDatos() {
        assumeTrue(DatabaseConnectionManager.isAvailable(3),
                "MySQL no disponible: ejecute 'docker compose -f docker/docker-compose.yml up -d'");
    }

    @Test
    @DisplayName("Las ventas por mes vienen ordenadas cronológicamente y con importes positivos")
    void ventasPorMesOrdenadas() {
        var serie = repository.ventasPorMes();
        assertThat(serie).isNotEmpty();
        assertThat(serie).extracting(MonthlyAmount::month).isSorted();
        assertThat(serie).allSatisfy(m -> assertThat(m.amount()).isGreaterThan(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Clientes por país suma el total de clientes")
    void clientesPorPaisCuadraConElTotal() throws Exception {
        long total = repository.clientesPorPais().stream().mapToLong(CategoryMetric::count).sum();
        try (var conn = DatabaseConnectionManager.getInstance().getConnection();
             var rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM cliente")) {
            rs.next();
            assertThat(total).isEqualTo(rs.getLong(1));
        }
    }

    @Test
    @DisplayName("Ventas por gama incluye todas las gamas y el top de clientes respeta el límite")
    void gamasYTopClientes() {
        assertThat(repository.ventasPorGama()).isNotEmpty();
        assertThat(repository.topClientesPorCompras(3)).hasSizeLessThanOrEqualTo(3);
        assertThat(repository.clientesMasActivos(5)).hasSize(5)
                .isSortedAccordingTo((a, b) -> Long.compare(b.count(), a.count()));
        assertThat(repository.pagosPorFormaPago()).isNotEmpty();
        assertThat(repository.pedidosPorMesYEstado()).isNotEmpty();
    }

    @Test
    @DisplayName("El resumen financiero de un cliente existente se resuelve y uno inexistente devuelve vacío")
    void resumenCliente() {
        assertThat(repository.resumenCliente(1)).isPresent();
        assertThat(repository.resumenCliente(-999)).isEmpty();
    }
}
