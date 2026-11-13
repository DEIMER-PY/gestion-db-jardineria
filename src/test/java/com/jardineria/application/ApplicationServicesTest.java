package com.jardineria.application;

import com.jardineria.application.service.*;
import com.jardineria.domain.exception.BusinessValidationException;
import com.jardineria.domain.exception.DuplicateEntityException;
import com.jardineria.domain.exception.EntityNotFoundException;
import com.jardineria.domain.model.Oficina;
import com.jardineria.domain.model.Pedido;
import com.jardineria.domain.model.Producto;
import com.jardineria.domain.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServicesTest {

    @Mock
    private OficinaRepository oficinaRepository;
    @Mock
    private EmpleadoRepository empleadoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private GamaProductoRepository gamaRepository;
    @Mock
    private DetallePedidoRepository detalleRepository;
    @Mock
    private PagoRepository pagoRepository;

    @Test
    @DisplayName("OficinaService debe lanzar DuplicateEntityException si la oficina ya existe")
    void shouldThrowDuplicateWhenOficinaExists() {
        OficinaService service = new OficinaService(oficinaRepository);
        Oficina of = new Oficina("BCN-ES", "Barcelona", "España", "CAT", "08019", "123", "Dir 1", null);

        when(oficinaRepository.existsById("BCN-ES")).thenReturn(true);

        assertThatThrownBy(() -> service.crearOficina(of))
                .isInstanceOf(DuplicateEntityException.class)
                .hasMessageContaining("BCN-ES");
    }

    @Test
    @DisplayName("PedidoService debe validar coherencia de fechas en el pedido")
    void shouldValidatePedidoDates() {
        PedidoService service = new PedidoService(pedidoRepository, clienteRepository, detalleRepository);
        Pedido pedidoInvalido = new Pedido(999, LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 5), null, "Pendiente", null, 1);

        assertThatThrownBy(() -> service.crearPedido(pedidoInvalido))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("fecha esperada");
    }

    @Test
    @DisplayName("ProductoService debe crear un producto válido")
    void shouldCreateValidProducto() {
        ProductoService service = new ProductoService(productoRepository, gamaRepository);
        Producto prod = new Producto("TEST-01", "Pala de jardín", "Herramientas", "20cm",
                "Bellota", "Pala robusta", 50, new BigDecimal("15.00"), new BigDecimal("8.00"));

        when(productoRepository.existsById("TEST-01")).thenReturn(false);
        when(gamaRepository.existsById("Herramientas")).thenReturn(true);
        when(productoRepository.save(any(Producto.class))).thenReturn(prod);

        Producto creado = service.crearProducto(prod);
        assertThat(creado).isNotNull();
        assertThat(creado.getCodigoProducto()).isEqualTo("TEST-01");
        verify(productoRepository).save(prod);
    }

    @Test
    @DisplayName("DashboardService debe agregar métricas correctamente")
    void shouldAggregateDashboardMetrics() {
        DashboardService service = new DashboardService(
                clienteRepository, pedidoRepository, productoRepository,
                empleadoRepository, oficinaRepository, pagoRepository
        );

        when(clienteRepository.count()).thenReturn(36L);
        when(pedidoRepository.count()).thenReturn(47L);
        when(productoRepository.count()).thenReturn(10L);
        when(empleadoRepository.count()).thenReturn(31L);
        when(oficinaRepository.count()).thenReturn(9L);
        when(pagoRepository.totalPagos()).thenReturn(new BigDecimal("15000.50"));
        when(pedidoRepository.countByEstado("Entregado")).thenReturn(30L);
        when(pedidoRepository.countByEstado("Pendiente")).thenReturn(12L);
        when(pedidoRepository.countByEstado("Rechazado")).thenReturn(5L);

        com.jardineria.application.dto.DashboardMetrics metricas = service.obtenerMetricas();

        assertThat(metricas.totalClientes()).isEqualTo(36L);
        assertThat(metricas.totalPedidos()).isEqualTo(47L);
        assertThat(metricas.totalIngresos()).isEqualByComparingTo(new BigDecimal("15000.50"));
        assertThat(metricas.pedidosEntregados()).isEqualTo(30L);
    }
}
