package com.jardineria.infrastructure;

import com.jardineria.domain.model.*;
import com.jardineria.infrastructure.adapter.out.persistence.jdbc.*;
import com.jardineria.infrastructure.config.DatabaseConnectionManager;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcRepositoriesIntegrationTest {

    private OficinaJdbcRepository oficinaRepo;
    private EmpleadoJdbcRepository empleadoRepo;
    private GamaProductoJdbcRepository gamaRepo;
    private ClienteJdbcRepository clienteRepo;
    private PedidoJdbcRepository pedidoRepo;
    private ProductoJdbcRepository productoRepo;
    private DetallePedidoJdbcRepository detalleRepo;
    private PagoJdbcRepository pagoRepo;

    @BeforeAll
    static void requiereBaseDeDatos() {
        assumeTrue(DatabaseConnectionManager.isAvailable(3),
                "MySQL no disponible: ejecute 'docker compose -f docker/docker-compose.yml up -d'");
    }

    @BeforeEach
    void setUp() {
        oficinaRepo = new OficinaJdbcRepository();
        empleadoRepo = new EmpleadoJdbcRepository();
        gamaRepo = new GamaProductoJdbcRepository();
        clienteRepo = new ClienteJdbcRepository();
        pedidoRepo = new PedidoJdbcRepository();
        productoRepo = new ProductoJdbcRepository();
        detalleRepo = new DetallePedidoJdbcRepository();
        pagoRepo = new PagoJdbcRepository();
    }

    @Test
    @Order(1)
    @DisplayName("Debe validar el patrón Singleton en DatabaseConnectionManager")
    void shouldValidateSingletonInstance() throws Exception {
        DatabaseConnectionManager instance1 = DatabaseConnectionManager.getInstance();
        DatabaseConnectionManager instance2 = DatabaseConnectionManager.getInstance();

        assertThat(instance1).isSameAs(instance2);

        try (Connection conn = instance1.getConnection()) {
            assertThat(conn).isNotNull();
            assertThat(conn.isValid(2)).isTrue();
        }
    }

    @Test
    @Order(2)
    @DisplayName("Debe listar y verificar las oficinas sembradas en MySQL")
    void shouldFindAllOficinas() {
        List<Oficina> oficinas = oficinaRepo.findAll();
        assertThat(oficinas).isNotEmpty();
        assertThat(oficinaRepo.count()).isGreaterThanOrEqualTo(9);

        Optional<Oficina> bcn = oficinaRepo.findById("BCN-ES");
        assertThat(bcn).isPresent();
        assertThat(bcn.get().getCiudad()).isEqualTo("Barcelona");
    }

    @Test
    @Order(3)
    @DisplayName("Debe verificar CRUD de Oficina")
    void shouldPerformOficinaCrud() {
        String testId = "TEST-OF";
        Oficina testOficina = new Oficina(testId, "Valencia", "España", "Valencia", "46001",
                "+34 96 123456", "Calle Colon 1", "Piso 2");

        // 1. Save
        oficinaRepo.save(testOficina);
        assertThat(oficinaRepo.existsById(testId)).isTrue();

        // 2. Find
        Optional<Oficina> found = oficinaRepo.findById(testId);
        assertThat(found).isPresent();
        assertThat(found.get().getCiudad()).isEqualTo("Valencia");

        // 3. Update
        testOficina.setCiudad("Valencia Capital");
        oficinaRepo.update(testOficina);
        Optional<Oficina> updated = oficinaRepo.findById(testId);
        assertThat(updated).isPresent();
        assertThat(updated.get().getCiudad()).isEqualTo("Valencia Capital");

        // 4. Delete
        boolean deleted = oficinaRepo.deleteById(testId);
        assertThat(deleted).isTrue();
        assertThat(oficinaRepo.existsById(testId)).isFalse();
    }

    @Test
    @Order(4)
    @DisplayName("Debe listar empleados y consultar directores")
    void shouldFindEmpleadosAndDirectores() {
        List<Empleado> empleados = empleadoRepo.findAll();
        assertThat(empleados).isNotEmpty();

        List<Empleado> directores = empleadoRepo.findDirectores();
        assertThat(directores).isNotEmpty();
        assertThat(directores.getFirst().getCodigoJefe()).isNull();
    }

    @Test
    @Order(5)
    @DisplayName("Debe listar clientes y filtrar por país")
    void shouldFindClientes() {
        List<Cliente> clientes = clienteRepo.findAll();
        assertThat(clientes).isNotEmpty();

        List<Cliente> clientesSpain = clienteRepo.findByPais("Spain");
        if (clientesSpain.isEmpty()) {
            clientesSpain = clienteRepo.findByPais("España");
        }
        assertThat(clientesSpain).isNotEmpty();
    }

    @Test
    @Order(6)
    @DisplayName("Debe listar pedidos y contar por estado")
    void shouldFindPedidosAndCountByEstado() {
        List<Pedido> pedidos = pedidoRepo.findAll();
        assertThat(pedidos).isNotEmpty();

        long entregados = pedidoRepo.countByEstado("Entregado");
        long pendientes = pedidoRepo.countByEstado("Pendiente");
        long rechazados = pedidoRepo.countByEstado("Rechazado");

        assertThat(entregados + pendientes + rechazados).isGreaterThan(0);
    }

    @Test
    @Order(7)
    @DisplayName("Debe consultar productos y alertar bajo stock")
    void shouldFindProductosAndLowStock() {
        List<Producto> productos = productoRepo.findAll();
        assertThat(productos).isNotEmpty();

        List<Producto> bajoStock = productoRepo.findBajoStock(10);
        assertThat(bajoStock).isNotEmpty();
    }

    @Test
    @Order(8)
    @DisplayName("Debe consultar detalles de pedido y total de pagos")
    void shouldFindDetallesAndPagos() {
        List<DetallePedido> detalles = detalleRepo.findByPedido(8);
        assertThat(detalles).isNotEmpty();

        List<Pago> pagos = pagoRepo.findAll();
        assertThat(pagos).isNotEmpty();

        BigDecimal total = pagoRepo.totalPagos();
        assertThat(total).isGreaterThan(BigDecimal.ZERO);
    }
}
