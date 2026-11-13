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
 * @param asistente  asistente conversacional (desactivado si no hay modelo configurado)
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
        ConnectionProbe connection,
        AsistenteUseCase asistente
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
        asistente = asistente == null ? SIN_ASISTENTE : asistente;
    }

    /** Servicios sin asistente (pruebas y arranques sin modelo de lenguaje). */
    public DesktopServices(DashboardUseCase dashboard, AnalyticsUseCase analytics, ClienteUseCase clientes,
                           PedidoUseCase pedidos, DetallePedidoUseCase detalles, PagoUseCase pagos,
                           ProductoUseCase productos, GamaProductoUseCase gamas, OficinaUseCase oficinas,
                           EmpleadoUseCase empleados, ConnectionProbe connection) {
        this(dashboard, analytics, clientes, pedidos, detalles, pagos, productos, gamas, oficinas, empleados,
                connection, null);
    }

    /** Asistente desactivado: la vista muestra cómo configurarlo. */
    public static final AsistenteUseCase SIN_ASISTENTE = new AsistenteUseCase() {
        @Override
        public boolean disponible() {
            return false;
        }

        @Override
        public String descripcionProveedor() {
            return "Sin configurar: define la variable de entorno GROQ_API_KEY";
        }

        @Override
        public Respuesta preguntar(java.util.List<Turno> historial, String pregunta) {
            throw new IllegalStateException("El asistente no está configurado: define la variable de entorno GROQ_API_KEY.");
        }
    };

    /** Puerto mínimo para conocer el estado de la conexión con la base de datos. */
    public interface ConnectionProbe {
        /** Comprueba la conectividad (bloqueante; se invoca desde un hilo virtual). */
        boolean isAvailable();

        /** Descripción del destino, p. ej. "localhost:3307 / jardineria". */
        String describe();
    }
}
