package com.jardineria.application.facade;

import com.jardineria.application.port.in.ClienteUseCase;
import com.jardineria.application.port.in.DetallePedidoUseCase;
import com.jardineria.application.port.in.PagoUseCase;
import com.jardineria.application.port.in.PedidoUseCase;
import com.jardineria.application.port.in.ProductoUseCase;
import com.jardineria.domain.model.Cliente;
import com.jardineria.domain.model.DetallePedido;
import com.jardineria.domain.model.Pago;
import com.jardineria.domain.model.Pedido;
import com.jardineria.domain.model.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Patrón Facade: el checkout coordina cinco puertos de entrada (cliente, producto, pedido,
 * detalle y pago) y aplica Decorator (precio) y Adapter (pasarela de cobro).
 */
class JardineriaBusinessFacadeTest {

    private ClienteUseCase clientes;
    private ProductoUseCase productos;
    private PedidoUseCase pedidos;
    private DetallePedidoUseCase detalles;
    private PagoUseCase pagos;
    private JardineriaBusinessFacade facade;

    @BeforeEach
    void setUp() {
        clientes = mock(ClienteUseCase.class);
        productos = mock(ProductoUseCase.class);
        pedidos = mock(PedidoUseCase.class);
        detalles = mock(DetallePedidoUseCase.class);
        pagos = mock(PagoUseCase.class);
        facade = new JardineriaBusinessFacade(clientes, productos, pedidos, detalles, pagos);

        Cliente cliente = new Cliente();
        cliente.setCodigoCliente(7);
        cliente.setLimiteCredito(new BigDecimal("5000"));
        when(clientes.obtenerPorId(7)).thenReturn(cliente);
        when(productos.obtenerPorId("OR-001")).thenReturn(producto("OR-001", 50));
        when(pedidos.listarTodos()).thenReturn(List.of(
                new Pedido(41, LocalDate.now(), LocalDate.now(), null, "Entregado", null, 7)));
        when(pagos.registrarPago(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Producto producto(String codigo, int stock) {
        Producto p = new Producto();
        p.setCodigoProducto(codigo);
        p.setNombre("Orquídea");
        p.setCantidadEnStock(stock);
        return p;
    }

    private static CheckoutRequest pedido(int cantidad, String metodo) {
        return new CheckoutRequest(7, List.of(new CheckoutRequest.ItemCheckout("OR-001", cantidad, new BigDecimal("10.00"))),
                metodo, "TX-PRUEBA", false);
    }

    @Test
    @DisplayName("Checkout correcto: crea el pedido con el siguiente código, sus líneas y el pago")
    void checkoutCompleto() {
        CheckoutResult r = facade.procesarCheckoutCompleto(pedido(3, "PayPal"));

        assertThat(r.exito()).as(r.mensaje()).isTrue();
        ArgumentCaptor<Pedido> pedido = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidos).crearPedido(pedido.capture());
        assertThat(pedido.getValue().getCodigoPedido()).isEqualTo(42);
        assertThat(pedido.getValue().getEstado()).isEqualTo("Pendiente");
        verify(detalles, times(1)).agregarDetalle(any(DetallePedido.class));
        ArgumentCaptor<Pago> pago = ArgumentCaptor.forClass(Pago.class);
        verify(pagos).registrarPago(pago.capture());
        assertThat(pago.getValue().getTotal()).isEqualByComparingTo(r.totalFacturado());
        // Decorator: el total incluye IVA, así que supera el importe base (3 × 10 €).
        assertThat(r.totalFacturado()).isGreaterThan(new BigDecimal("30.00"));
        assertThat(r.desglosePrecios()).isNotEmpty();
    }

    @Test
    @DisplayName("Sin stock suficiente no crea nada")
    void stockInsuficiente() {
        CheckoutResult r = facade.procesarCheckoutCompleto(pedido(80, "Tarjeta"));

        assertThat(r.exito()).isFalse();
        assertThat(r.mensaje()).contains("Stock insuficiente");
        verify(pedidos, never()).crearPedido(any());
        verify(pagos, never()).registrarPago(any());
    }

    @Test
    @DisplayName("Si el total supera el límite de crédito se rechaza")
    void limiteDeCredito() {
        clientes.obtenerPorId(7).setLimiteCredito(new BigDecimal("5"));

        CheckoutResult r = facade.procesarCheckoutCompleto(pedido(3, "Tarjeta"));

        assertThat(r.exito()).isFalse();
        assertThat(r.mensaje()).contains("límite de crédito");
        verify(pedidos, never()).crearPedido(any());
    }

    @Test
    @DisplayName("Un pedido vacío se rechaza")
    void pedidoVacio() {
        CheckoutResult r = facade.procesarCheckoutCompleto(new CheckoutRequest(7, List.of(), "Tarjeta", null, false));

        assertThat(r.exito()).isFalse();
        verify(pedidos, never()).crearPedido(any());
    }
}
