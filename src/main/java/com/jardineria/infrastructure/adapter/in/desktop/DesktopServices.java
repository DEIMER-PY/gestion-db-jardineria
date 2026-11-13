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
 * @param mensajeria mensajería del equipo (canales, mensajes directos y no leídos)
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
        AsistenteUseCase asistente,
        MensajeriaUseCase mensajeria
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
        Objects.requireNonNull(mensajeria);
    }

    /** Servicios sin mensajería (pruebas que no la usan). */
    public DesktopServices(DashboardUseCase dashboard, AnalyticsUseCase analytics, ClienteUseCase clientes,
                           PedidoUseCase pedidos, DetallePedidoUseCase detalles, PagoUseCase pagos,
                           ProductoUseCase productos, GamaProductoUseCase gamas, OficinaUseCase oficinas,
                           EmpleadoUseCase empleados, ConnectionProbe connection, AsistenteUseCase asistente) {
        this(dashboard, analytics, clientes, pedidos, detalles, pagos, productos, gamas, oficinas, empleados,
                connection, asistente, SIN_MENSAJERIA);
    }

    /** Servicios sin asistente (pruebas y arranques sin modelo de lenguaje). */
    public DesktopServices(DashboardUseCase dashboard, AnalyticsUseCase analytics, ClienteUseCase clientes,
                           PedidoUseCase pedidos, DetallePedidoUseCase detalles, PagoUseCase pagos,
                           ProductoUseCase productos, GamaProductoUseCase gamas, OficinaUseCase oficinas,
                           EmpleadoUseCase empleados, ConnectionProbe connection) {
        this(dashboard, analytics, clientes, pedidos, detalles, pagos, productos, gamas, oficinas, empleados,
                connection, null, SIN_MENSAJERIA);
    }

    /** Mensajería vacía para pruebas que no la usan: sin canales ni mensajes. */
    public static final MensajeriaUseCase SIN_MENSAJERIA = new MensajeriaUseCase() {
        @Override
        public java.util.List<com.jardineria.domain.model.chat.Canal> canales(int codigoEmpleado) {
            return java.util.List.of();
        }

        @Override
        public com.jardineria.domain.model.chat.Canal canalDirecto(int codigoEmpleado, int otroEmpleado) {
            throw new IllegalStateException("Mensajería no disponible");
        }

        @Override
        public java.util.List<com.jardineria.domain.model.chat.Mensaje> historial(int idCanal, int limite) {
            return java.util.List.of();
        }

        @Override
        public com.jardineria.domain.model.chat.Mensaje enviar(int idCanal, int codigoEmpleado, String texto) {
            throw new IllegalStateException("Mensajería no disponible");
        }

        @Override
        public long ultimoMensaje() {
            return 0;
        }

        @Override
        public void marcarLeido(int idCanal, int codigoEmpleado, long ultimoMensaje) {
        }

        @Override
        public Sondeo sondear(int codigoEmpleado, long despuesDe) {
            return new Sondeo(java.util.List.of(), java.util.Map.of(), java.util.List.of());
        }
    };

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
