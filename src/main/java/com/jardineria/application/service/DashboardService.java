package com.jardineria.application.service;

import com.jardineria.application.dto.DashboardMetrics;
import com.jardineria.application.port.in.DashboardUseCase;
import com.jardineria.domain.model.Pedido;
import com.jardineria.domain.model.Producto;
import com.jardineria.domain.repository.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Servicio de agregación para el panel ejecutivo y operativo del sistema.
 */
public class DashboardService implements DashboardUseCase {
    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final OficinaRepository oficinaRepository;
    private final PagoRepository pagoRepository;

    public DashboardService(ClienteRepository clienteRepository,
                            PedidoRepository pedidoRepository,
                            ProductoRepository productoRepository,
                            EmpleadoRepository empleadoRepository,
                            OficinaRepository oficinaRepository,
                            PagoRepository pagoRepository) {
        this.clienteRepository = Objects.requireNonNull(clienteRepository);
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository);
        this.productoRepository = Objects.requireNonNull(productoRepository);
        this.empleadoRepository = Objects.requireNonNull(empleadoRepository);
        this.oficinaRepository = Objects.requireNonNull(oficinaRepository);
        this.pagoRepository = Objects.requireNonNull(pagoRepository);
    }

    @Override
    public DashboardMetrics obtenerMetricas() {
        return new DashboardMetrics(
                clienteRepository.count(),
                pedidoRepository.count(),
                productoRepository.count(),
                empleadoRepository.count(),
                oficinaRepository.count(),
                pagoRepository.totalPagos(),
                pedidoRepository.countByEstado("Entregado"),
                pedidoRepository.countByEstado("Pendiente"),
                pedidoRepository.countByEstado("Rechazado"),
                productoRepository.findBajoStock(UMBRAL_STOCK_CRITICO).size()
        );
    }

    @Override
    public List<Pedido> obtenerUltimosPedidos(int limite) {
        return pedidoRepository.findAll().stream()
                .sorted(Comparator.comparing(Pedido::getFechaPedido,
                        Comparator.nullsLast(Comparator.<LocalDate>reverseOrder())))
                .limit(Math.max(0, limite))
                .toList();
    }

    @Override
    public List<Producto> obtenerAlertasStock() {
        return productoRepository.findBajoStock(UMBRAL_STOCK_CRITICO).stream()
                .sorted(Comparator.comparing(Producto::getCantidadEnStock,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
    }
}
