package com.jardineria.infrastructure.adapter.in.desktop;

import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.shell.AppShell;
import com.jardineria.infrastructure.adapter.in.desktop.theme.ThemeManager;
import com.jardineria.infrastructure.config.ApplicationContext;
import com.jardineria.infrastructure.config.DatabaseConfig;
import com.jardineria.infrastructure.config.DatabaseConnectionManager;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Recorre todas las pantallas contra el MySQL real (Docker) en modo headless y guarda
 * capturas en target/screenshots para revisión visual (tema oscuro y claro).
 * Se omite si la base de datos no está disponible.
 * <p>
 * Ejecución: {@code mvn test -Dtest=DesktopScreenshotTest -Dui.screenshots=true}
 */
@EnabledIfSystemProperty(named = "ui.screenshots", matches = "true")
@ExtendWith(ApplicationExtension.class)
class DesktopScreenshotTest {
    private AppShell shell;
    private Scene scene;

    @BeforeAll
    static void requiereBaseDeDatos() {
        assumeTrue(DatabaseConnectionManager.isAvailable(3), "MySQL no disponible: se omiten las capturas");
    }

    @Start
    void start(Stage stage) {
        ThemeManager.loadFonts();
        ApplicationContext ctx = ApplicationContext.getInstance();
        DesktopServices services = new DesktopServices(
                ctx.getDashboardService(), ctx.getAnalyticsService(), ctx.getClienteService(),
                ctx.getPedidoService(), ctx.getDetallePedidoService(), ctx.getPagoService(),
                ctx.getProductoService(), ctx.getGamaProductoService(), ctx.getOficinaService(),
                ctx.getEmpleadoService(), ctx.getBusinessFacade(), ctx.getCachedProductProxy(),
                new DesktopServices.ConnectionProbe() {
                    @Override
                    public boolean isAvailable() {
                        return DatabaseConnectionManager.isAvailable(3);
                    }

                    @Override
                    public String describe() {
                        return DatabaseConfig.getHost() + ":" + DatabaseConfig.getPort() + " / " + DatabaseConfig.getDatabaseName();
                    }
                });
        shell = new AppShell(services);
        scene = new Scene(shell, 1440, 900);
        ThemeManager.setMode(ThemeManager.Mode.DARK);
        ThemeManager.install(scene);
        shell.installShortcuts(scene);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    @DisplayName("Captura cada ruta en tema oscuro y claro")
    void capturaTodasLasRutas() throws Exception {
        for (ThemeManager.Mode mode : ThemeManager.Mode.values()) {
            WaitForAsyncUtils.asyncFx(() -> ThemeManager.setMode(mode)).get();
            for (Route route : Route.values()) {
                WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(route)).get();
                FxTestSupport.settle(1600);
                Path png = FxTestSupport.snapshot(scene, mode.name().toLowerCase() + "-" + route.name().toLowerCase());
                assertThat(Files.size(png)).isPositive();
            }
            // Fuerza que la siguiente pasada vuelva a mostrar el panel general
            WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(Route.ANALYTICS)).get();
        }
    }
}
