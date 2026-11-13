package com.jardineria.infrastructure.adapter.in.desktop;

import com.jardineria.application.port.in.*;

import java.util.Objects;

/**
 * Conjunto de puertos de entrada que consume la GUI de escritorio.
 * <p>
 * La GUI no conoce repositorios ni JDBC: recibe este agregado desde el composition root.
 * En las pruebas se construye con implementaciones falsas en memoria.
 *
 * @param connection sonda de conectividad con la base de datos y su descripción
 */
public record DesktopServices(
        DashboardUseCase dashboard,
        AnalyticsUseCase analytics,
        ClienteUseCase clientes,
        PedidoUseCase pedidos,
        DetallePedidoUseCase detalles,
        PagoUseCase pagos,
        ProductoUseCase productos,
        GamaProductoUseCase gamas,
        OficinaUseCase oficinas,
        EmpleadoUseCase empleados,
        ConnectionProbe connection
) {
    public DesktopServices {
        Objects.requireNonNull(dashboard);
        Objects.requireNonNull(analytics);
        Objects.requireNonNull(clientes);
        Objects.requireNonNull(pedidos);
        Objects.requireNonNull(detalles);
        Objects.requireNonNull(pagos);
        Objects.requireNonNull(productos);
        Objects.requireNonNull(gamas);
        Objects.requireNonNull(oficinas);
        Objects.requireNonNull(empleados);
        Objects.requireNonNull(connection);
    }

    /** Puerto mínimo para conocer el estado de la conexión con la base de datos. */
    public interface ConnectionProbe {
        /** Comprueba la conectividad (bloqueante; se invoca desde un hilo virtual). */
        boolean isAvailable();

        /** Descripción del destino, p. ej. "localhost:3307 / jardineria". */
        String describe();
    }
}
