package com.jardineria.application.service;

import com.jardineria.domain.model.Pedido;
import com.jardineria.domain.model.Producto;
import com.jardineria.domain.repository.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Servicio de agregación para el Dashboard ejecutivo y operativo del sistema.
 */
public class DashboardService {
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

    public DashboardMetrics obtenerMetricas() {
        long totalClientes = clienteRepository.count();
        long totalPedidos = pedidoRepository.count();
        long totalProductos = productoRepository.count();
        long totalEmpleados = empleadoRepository.count();
        long totalOficinas = oficinaRepository.count();
        BigDecimal totalIngresos = pagoRepository.totalPagos();

        long pedidosEntregados = pedidoRepository.countByEstado("Entregado");
        long pedidosPendientes = pedidoRepository.countByEstado("Pendiente");
        long pedidosRechazados = pedidoRepository.countByEstado("Rechazado");

        List<Producto> productosBajoStock = productoRepository.findBajoStock(10);
        long totalBajoStock = productosBajoStock.size();

        return new DashboardMetrics(
                totalClientes,
                totalPedidos,
                totalProductos,
                totalEmpleados,
                totalOficinas,
                totalIngresos,
                pedidosEntregados,
                pedidosPendientes,
                pedidosRechazados,
                totalBajoStock
        );
    }

    public List<Pedido> obtenerUltimosPedidos() {
        List<Pedido> todos = pedidoRepository.findAll();
        return todos.stream().limit(10).toList();
    }

    public List<Producto> obtenerAlertasStock() {
        return productoRepository.findBajoStock(10);
    }

    /**
     * DTO inmutable con las métricas del dashboard.
     */
    public record DashboardMetrics(
            long totalClientes,
            long totalPedidos,
            long totalProductos,
            long totalEmpleados,
            long totalOficinas,
            BigDecimal totalIngresos,
            long pedidosEntregados,
            long pedidosPendientes,
            long pedidosRechazados,
            long totalBajoStock
    ) {
    }
}
