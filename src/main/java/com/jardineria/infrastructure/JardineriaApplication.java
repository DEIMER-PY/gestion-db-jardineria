package com.jardineria.infrastructure;

import com.jardineria.infrastructure.adapter.in.cli.ConsoleMenuRunner;
import com.jardineria.infrastructure.adapter.in.desktop.DesktopServices;
import com.jardineria.infrastructure.adapter.in.desktop.JardineriaDesktopApp;
import com.jardineria.infrastructure.config.ApplicationContext;
import com.jardineria.infrastructure.config.DatabaseConfig;
import com.jardineria.infrastructure.config.DatabaseConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.GraphicsEnvironment;
import java.util.List;

/**
 * Punto de entrada del Sistema de Gestión de Jardinería.
 * <ul>
 *   <li>Sin argumentos: interfaz gráfica de escritorio JavaFX.</li>
 *   <li>{@code --cli}: consola interactiva (también se usa automáticamente en entornos sin pantalla).</li>
 * </ul>
 * No extiende {@code javafx.application.Application} a propósito: así el JAR sombreado
 * arranca JavaFX desde el classpath sin necesidad de configurar el module-path.
 */
public final class JardineriaApplication {
    private static final Logger log = LoggerFactory.getLogger(JardineriaApplication.class);

    private JardineriaApplication() {
    }

    public static void main(String[] args) {
        List<String> argList = List.of(args);
        boolean cli = argList.contains("--cli") || argList.contains("cli") || GraphicsEnvironment.isHeadless();
        ApplicationContext context = ApplicationContext.getInstance();

        if (cli) {
            log.info("Iniciando consola interactiva (CLI)...");
            useUtf8WhenRedirected();
            new ConsoleMenuRunner(context).run();
            return;
        }
        log.info("Iniciando interfaz de escritorio JavaFX...");
        JardineriaDesktopApp.launchWith(desktopServices(context), args);
    }

    /**
     * Sin terminal real (consola del IDE, tubería o redirección) Java escribe con la página de
     * códigos ANSI de Windows y las tildes salen como "�". En ese caso se fuerza UTF-8, que es
     * lo que esperan IntelliJ, VS Code y los terminales modernos. Con una terminal real Java ya
     * usa la codificación de la consola y no se toca.
     */
    private static void useUtf8WhenRedirected() {
        if (System.console() == null) {
            System.setOut(new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out), true,
                    java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    /** Adapta el composition root al conjunto de puertos que consume la GUI. */
    static DesktopServices desktopServices(ApplicationContext ctx) {
        DesktopServices.ConnectionProbe probe = new DesktopServices.ConnectionProbe() {
            @Override
            public boolean isAvailable() {
                return DatabaseConnectionManager.isAvailable(3);
            }

            @Override
            public String describe() {
                return DatabaseConfig.getHost() + ":" + DatabaseConfig.getPort() + " / " + DatabaseConfig.getDatabaseName();
            }
        };
        return new DesktopServices(
                ctx.getDashboardService(),
                ctx.getAnalyticsService(),
                ctx.getClienteService(),
                ctx.getPedidoService(),
                ctx.getDetallePedidoService(),
                ctx.getPagoService(),
                ctx.getProductoService(),
                ctx.getGamaProductoService(),
                ctx.getOficinaService(),
                ctx.getEmpleadoService(),
                probe,
                ctx.getAsistenteService(),
                ctx.getMensajeriaService());
    }
}
