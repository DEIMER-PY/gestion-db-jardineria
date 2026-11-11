package com.jardineria.domain.pattern.facade;

import com.jardineria.application.service.*;
import com.jardineria.domain.model.Cliente;
import com.jardineria.domain.model.DetallePedido;
import com.jardineria.domain.model.Pago;
import com.jardineria.domain.model.Pedido;
import com.jardineria.domain.model.Producto;
import com.jardineria.domain.pattern.adapter.PayPalAdapter;
import com.jardineria.domain.pattern.adapter.PaymentGatewayPort;
import com.jardineria.domain.pattern.adapter.StripeAdapter;
import com.jardineria.domain.pattern.decorator.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Fachada Empresarial (GoF Facade).
 * Proporciona una interfaz unificada de alto nivel que oculta la complejidad
 * de coordinar múltiples servicios, repositorios, validaciones de stock,
 * cálculos de precios con decoradores y adaptadores de pago.
 */
public class JardineriaBusinessFacade {
    private final ClienteService clienteService;
    private final ProductoService productoService;
    private final PedidoService pedidoService;
    private final DetallePedidoService detalleService;
    private final PagoService pagoService;

    public JardineriaBusinessFacade(ClienteService clienteService,
                                   ProductoService productoService,
                                   PedidoService pedidoService,
                                   DetallePedidoService detalleService,
                                   PagoService pagoService) {
        this.clienteService = clienteService;
        this.productoService = productoService;
        this.pedidoService = pedidoService;
        this.detalleService = detalleService;
        this.pagoService = pagoService;
    }

    /**
     * Orquesta el proceso completo de compra:
     * 1. Valida existencia de cliente.
     * 2. Comprueba disponibilidad de stock de cada producto.
     * 3. Calcula total usando la cadena del Patrón Decorator.
     * 4. Valida límite de crédito del cliente.
     * 5. Crea el Pedido y sus Detalles.
     * 6. Descuenta el stock en almacén.
     * 7. Procesa el pago usando el Patrón Adapter.
     */
    public synchronized CheckoutResult procesarCheckoutCompleto(CheckoutRequest request) {
        try {
            // 1. Validar cliente
            Cliente cliente = clienteService.obtenerPorId(request.codigoCliente());

            if (request.items() == null || request.items().isEmpty()) {
                return new CheckoutResult(false, "El pedido no contiene ningún producto.", null, null, BigDecimal.ZERO, List.of());
            }

            // 2. Comprobar stock de cada producto
            for (CheckoutRequest.ItemCheckout item : request.items()) {
                Producto prod = productoService.obtenerPorId(item.codigoProducto());
                if (prod.getCantidadEnStock() < item.cantidad()) {
                    return new CheckoutResult(false,
                            String.format("Stock insuficiente para el producto '%s' (%s). Disponible: %d, Solicitado: %d",
                                    prod.getNombre(), prod.getCodigoProducto(), prod.getCantidadEnStock(), item.cantidad()),
                            null, null, BigDecimal.ZERO, List.of());
                }
            }

            // 3. Cálculo de precio usando Patrón Decorator
            PriceCalculator calculator = new BasePriceCalculator();
            calculator = new BulkDiscountDecorator(calculator);
            if (request.aplicarDescuentoTemporada()) {
                calculator = new SeasonalPromoDecorator(calculator);
            }
            calculator = new VatTaxDecorator(calculator);

            BigDecimal totalCalculado = BigDecimal.ZERO;
            List<String> desgloseTotal = new ArrayList<>();

            for (CheckoutRequest.ItemCheckout item : request.items()) {
                BigDecimal totalItem = calculator.calcularTotal(item.precioUnitario(), item.cantidad());
                totalCalculado = totalCalculado.add(totalItem);
                desgloseTotal.add("--- Producto: " + item.codigoProducto() + " ---");
                desgloseTotal.addAll(calculator.getDesglose());
            }

            // 4. Validar límite de crédito del cliente
            if (cliente.getLimiteCredito() != null && cliente.getLimiteCredito().compareTo(totalCalculado) < 0) {
                return new CheckoutResult(false,
                        String.format("El total (%s €) supera el límite de crédito del cliente (%s €)",
                                totalCalculado, cliente.getLimiteCredito()),
                        null, null, totalCalculado, desgloseTotal);
            }

            // 5. Determinar nuevo código de pedido
            int nuevoCodigoPedido = generarSiguienteCodigoPedido();
            Pedido nuevoPedido = new Pedido(
                    nuevoCodigoPedido,
                    LocalDate.now(),
                    LocalDate.now().plusDays(3),
                    null,
                    "Pendiente",
                    "Pedido procesado mediante JardineriaBusinessFacade",
                    cliente.getCodigoCliente()
            );
            pedidoService.crearPedido(nuevoPedido);

            // 6. Insertar Detalles de Pedido (agregarDetalle deduce el stock automáticamente)
            short numeroLinea = 1;
            for (CheckoutRequest.ItemCheckout item : request.items()) {
                DetallePedido dp = new DetallePedido(
                        nuevoCodigoPedido,
                        item.codigoProducto(),
                        item.cantidad(),
                        item.precioUnitario(),
                        numeroLinea++
                );
                detalleService.agregarDetalle(dp);
            }

            // 7. Procesar Cobro usando Patrón Adapter
            PaymentGatewayPort gateway = resolverPasarela(request.metodoPago());
            String txId = (request.idTransaccion() != null && !request.idTransaccion().isBlank())
                    ? request.idTransaccion()
                    : "TX-" + System.currentTimeMillis();

            Pago nuevoPago = gateway.procesarCobro(cliente.getCodigoCliente(), txId, totalCalculado);
            pagoService.registrarPago(nuevoPago);

            return new CheckoutResult(true, "Checkout completado con éxito.", nuevoPedido, nuevoPago, totalCalculado, desgloseTotal);

        } catch (Exception e) {
            return new CheckoutResult(false, "Fallo al procesar checkout: " + e.getMessage(), null, null, BigDecimal.ZERO, List.of());
        }
    }

    private PaymentGatewayPort resolverPasarela(String metodo) {
        if (metodo == null) return new StripeAdapter(null);
        String m = metodo.trim().toLowerCase();
        if (m.contains("paypal")) {
            return new PayPalAdapter(null);
        } else {
            return new StripeAdapter(null);
        }
    }

    private int generarSiguienteCodigoPedido() {
        return pedidoService.listarTodos().stream()
                .mapToInt(Pedido::getCodigoPedido)
                .max()
                .orElse(100) + 1;
    }
}
