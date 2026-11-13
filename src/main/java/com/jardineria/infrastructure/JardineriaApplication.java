package com.jardineria.infrastructure;

import com.jardineria.infrastructure.adapter.in.cli.ConsoleMenuRunner;
import com.jardineria.infrastructure.config.ApplicationContext;
import com.jardineria.infrastructure.adapter.in.ui.MainDashboardWindow;
import com.jardineria.infrastructure.adapter.in.ui.components.ElectronModeLauncher;
import com.jardineria.infrastructure.adapter.in.ui.components.ThemeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * Punto de entrada principal para el Sistema de Gestión de Jardinería.
 * Soporta modos de ejecución múltiple:
 *  1. Interfaz Gráfica Nativa Moderna (GUI con FlatLaf + Iconografía Vectorial).
 *  2. Modo Aplicación de Escritorio Web (--app / --desktop-app vía Chromium/Chrome en modo ventana independiente sin barra de direcciones).
 *  3. Consola Interactiva (--cli o en entornos sin display/headless).
 */
public class JardineriaApplication {
    private static final Logger log = LoggerFactory.getLogger(JardineriaApplication.class);

    public static void main(String[] args) {
        log.info("Iniciando Sistema de Gestión de Jardinería...");

        List<String> argList = Arrays.asList(args);
        boolean forceCli = argList.contains("--cli");
        boolean forceApp = argList.contains("--app") || argList.contains("--desktop-app");
        boolean isHeadless = GraphicsEnvironment.isHeadless();

        ApplicationContext registry = ApplicationContext.getInstance();

        if (forceCli || (isHeadless && !forceApp)) {
            if (isHeadless && !forceCli) {
                log.info("Entorno gráfico no detectado (headless mode). Iniciando en modo Consola (CLI).");
            } else {
                log.info("Argumento --cli recibido. Iniciando en modo Consola interactiva (CLI).");
            }
            ConsoleMenuRunner cli = new ConsoleMenuRunner(registry);
            cli.run();
        } else if (forceApp) {
            log.info("Argumento --app / --desktop-app recibido. Lanzando Aplicación de Escritorio enriquecida...");
            String url = (com.jardineria.infrastructure.adapter.in.web.JardineriaWebServer.shared(com.jardineria.infrastructure.config.ApplicationContext.getInstance()) != null) ? com.jardineria.infrastructure.adapter.in.web.JardineriaWebServer.shared(com.jardineria.infrastructure.config.ApplicationContext.getInstance()).getDashboardUrl() : "http://localhost:8080/dashboard";
            ElectronModeLauncher.openElectronMode(url);
            log.info("Aplicación de Escritorio en ejecución en {}", url);
        } else {
            log.info("Iniciando Interfaz Gráfica Moderna (GUI con FlatLaf y Componentes Vectoriales)...");
            ThemeManager.setupInitialTheme();

            SwingUtilities.invokeLater(() -> {
                try {
                    MainDashboardWindow window = new MainDashboardWindow(registry);
                    window.setVisible(true);
                    log.info("Ventana de Dashboard mostrada con éxito.");
                } catch (Exception e) {
                    log.error("Error al iniciar la interfaz gráfica Swing, iniciando en modo Web App...", e);
                    String url = (com.jardineria.infrastructure.adapter.in.web.JardineriaWebServer.shared(com.jardineria.infrastructure.config.ApplicationContext.getInstance()) != null) ? com.jardineria.infrastructure.adapter.in.web.JardineriaWebServer.shared(com.jardineria.infrastructure.config.ApplicationContext.getInstance()).getDashboardUrl() : "http://localhost:8080/dashboard";
                    ElectronModeLauncher.openElectronMode(url);
                }
            });
        }
    }
}
