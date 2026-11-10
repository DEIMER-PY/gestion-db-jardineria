package com.jardineria.infrastructure;

import com.jardineria.application.service.*;
import com.jardineria.domain.repository.*;
import com.jardineria.infrastructure.adapter.out.persistence.jdbc.*;

/**
 * Registro y contenedor de inyección de dependencias para los repositorios y servicios de la aplicación.
 */
public final class ServiceRegistry {
    private static volatile ServiceRegistry instance;

    // Repositorios
    private final OficinaRepository oficinaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final GamaProductoRepository gamaProductoRepository;
    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final PagoRepository pagoRepository;

    // Servicios
    private final OficinaService oficinaService;
    private final EmpleadoService empleadoService;
    private final GamaProductoService gamaProductoService;
    private final ClienteService clienteService;
    private final PedidoService pedidoService;
    private final ProductoService productoService;
    private final DetallePedidoService detallePedidoService;
    private final PagoService pagoService;
    private final DashboardService dashboardService;

    private ServiceRegistry() {
        // Inicialización de adaptadores de persistencia (JDBC)
        this.oficinaRepository = new OficinaJdbcRepository();
        this.empleadoRepository = new EmpleadoJdbcRepository();
        this.gamaProductoRepository = new GamaProductoJdbcRepository();
        this.clienteRepository = new ClienteJdbcRepository();
        this.pedidoRepository = new PedidoJdbcRepository();
        this.productoRepository = new ProductoJdbcRepository();
        this.detallePedidoRepository = new DetallePedidoJdbcRepository();
        this.pagoRepository = new PagoJdbcRepository();

        // Inicialización de la capa de aplicación (Servicios)
        this.oficinaService = new OficinaService(oficinaRepository);
        this.empleadoService = new EmpleadoService(empleadoRepository, oficinaRepository);
        this.gamaProductoService = new GamaProductoService(gamaProductoRepository);
        this.clienteService = new ClienteService(clienteRepository, empleadoRepository);
        this.pedidoService = new PedidoService(pedidoRepository, clienteRepository, detallePedidoRepository);
        this.productoService = new ProductoService(productoRepository, gamaProductoRepository);
        this.detallePedidoService = new DetallePedidoService(detallePedidoRepository, pedidoRepository, productoRepository);
        this.pagoService = new PagoService(pagoRepository, clienteRepository);
        this.dashboardService = new DashboardService(
                clienteRepository, pedidoRepository, productoRepository,
                empleadoRepository, oficinaRepository, pagoRepository
        );
    }

    public static ServiceRegistry getInstance() {
        if (instance == null) {
            synchronized (ServiceRegistry.class) {
                if (instance == null) {
                    instance = new ServiceRegistry();
                }
            }
        }
        return instance;
    }

    public OficinaService getOficinaService() {
        return oficinaService;
    }

    public EmpleadoService getEmpleadoService() {
        return empleadoService;
    }

    public GamaProductoService getGamaProductoService() {
        return gamaProductoService;
    }

    public ClienteService getClienteService() {
        return clienteService;
    }

    public PedidoService getPedidoService() {
        return pedidoService;
    }

    public ProductoService getProductoService() {
        return productoService;
    }

    public DetallePedidoService getDetallePedidoService() {
        return detallePedidoService;
    }

    public PagoService getPagoService() {
        return pagoService;
    }

    public DashboardService getDashboardService() {
        return dashboardService;
    }
}
