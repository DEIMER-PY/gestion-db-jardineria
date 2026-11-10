package com.jardineria.infrastructure;

import com.jardineria.infrastructure.adapter.in.cli.ConsoleMenuRunner;
import com.jardineria.infrastructure.adapter.in.ui.MainDashboardWindow;
import com.jardineria.infrastructure.adapter.in.ui.components.ThemeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Punto de entrada principal para el Sistema de Gestión de Jardinería.
 * Permite ejecución dual:
 *  1. Interfaz Gráfica Moderna con Dashboard (por defecto en entornos con Display).
 *  2. Consola Interactiva (CLI) pasando el argumento --cli o en entornos headless.
 */
public class JardineriaApplication {
    private static final Logger log = LoggerFactory.getLogger(JardineriaApplication.class);

    public static void main(String[] args) {
        log.info("Iniciando Sistema de Gestión de Jardinería...");

        boolean forceCli = Arrays.asList(args).contains("--cli");
        boolean isHeadless = GraphicsEnvironment.isHeadless();

        ServiceRegistry registry = ServiceRegistry.getInstance();

        if (forceCli || isHeadless) {
            if (isHeadless && !forceCli) {
                log.info("Entorno gráfico no detectado (headless mode). Iniciando en modo Consola (CLI).");
            } else {
                log.info("Argumento --cli recibido. Iniciando en modo Consola interactiva (CLI).");
            }
            ConsoleMenuRunner cli = new ConsoleMenuRunner(registry);
            cli.run();
        } else {
            log.info("Iniciando Interfaz Gráfica Moderna (GUI Dashboard con FlatLaf)...");
            ThemeManager.setupInitialTheme();

            SwingUtilities.invokeLater(() -> {
                try {
                    MainDashboardWindow window = new MainDashboardWindow(registry);
                    window.setVisible(true);
                    log.info("Ventana de Dashboard mostrada con éxito.");
                } catch (Exception e) {
                    log.error("Error al iniciar la interfaz gráfica, cambiando a consola", e);
                    new ConsoleMenuRunner(registry).run();
                }
            });
        }
    }
}
