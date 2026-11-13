package com.jardineria.infrastructure.config;

import com.jardineria.application.port.in.*;
import com.jardineria.application.service.*;
import com.jardineria.domain.repository.*;
import com.jardineria.infrastructure.adapter.out.persistence.jdbc.*;

/**
 * Composition Root de la aplicación (inyección de dependencias manual).
 * <p>
 * Es el único punto que conoce las implementaciones concretas: instancia los adaptadores
 * de salida (JDBC), los inyecta en los servicios de aplicación y expone a los adaptadores
 * de entrada (GUI y CLI) únicamente los <b>puertos de entrada</b> ({@code *UseCase}).
 * Construirlo no abre conexiones ni arranca hilos: el pool de HikariCP se crea de forma
 * perezosa en la primera consulta.
 */
public final class ApplicationContext {
    private static volatile ApplicationContext instance;

    private final OficinaUseCase oficinaService;
    private final EmpleadoUseCase empleadoService;
    private final GamaProductoUseCase gamaProductoService;
    private final ClienteUseCase clienteService;
    private final PedidoUseCase pedidoService;
    private final ProductoUseCase productoService;
    private final DetallePedidoUseCase detallePedidoService;
    private final PagoUseCase pagoService;
    private final DashboardUseCase dashboardService;
    private final AnalyticsUseCase analyticsService;

    private ApplicationContext() {
        // Adaptadores de salida (persistencia JDBC sobre MySQL)
        OficinaRepository oficinaRepository = new OficinaJdbcRepository();
        EmpleadoRepository empleadoRepository = new EmpleadoJdbcRepository();
        GamaProductoRepository gamaProductoRepository = new GamaProductoJdbcRepository();
        ClienteRepository clienteRepository = new ClienteJdbcRepository();
        PedidoRepository pedidoRepository = new PedidoJdbcRepository();
        ProductoRepository productoRepository = new ProductoJdbcRepository();
        DetallePedidoRepository detallePedidoRepository = new DetallePedidoJdbcRepository();
        PagoRepository pagoRepository = new PagoJdbcRepository();
        AnalyticsRepository analyticsRepository = new AnalyticsJdbcRepository();

        // Capa de aplicación
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
                empleadoRepository, oficinaRepository, pagoRepository);
        this.analyticsService = new AnalyticsService(analyticsRepository);
    }

    /** Devuelve el contexto único de la aplicación (Singleton con doble verificación). */
    public static ApplicationContext getInstance() {
        if (instance == null) {
            synchronized (ApplicationContext.class) {
                if (instance == null) {
                    instance = new ApplicationContext();
                }
            }
        }
        return instance;
    }

    public OficinaUseCase getOficinaService() {
        return oficinaService;
    }

    public EmpleadoUseCase getEmpleadoService() {
        return empleadoService;
    }

    public GamaProductoUseCase getGamaProductoService() {
        return gamaProductoService;
    }

    public ClienteUseCase getClienteService() {
        return clienteService;
    }

    public PedidoUseCase getPedidoService() {
        return pedidoService;
    }

    public ProductoUseCase getProductoService() {
        return productoService;
    }

    public DetallePedidoUseCase getDetallePedidoService() {
        return detallePedidoService;
    }

    public PagoUseCase getPagoService() {
        return pagoService;
    }

    public DashboardUseCase getDashboardService() {
        return dashboardService;
    }

    public AnalyticsUseCase getAnalyticsService() {
        return analyticsService;
    }
}
