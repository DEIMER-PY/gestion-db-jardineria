package com.jardineria.infrastructure.adapter.in.desktop;

import com.jardineria.application.dto.DashboardMetrics;
import com.jardineria.application.port.in.AnalyticsUseCase;
import com.jardineria.application.port.in.ClienteUseCase;
import com.jardineria.application.port.in.DashboardUseCase;
import com.jardineria.application.port.in.DetallePedidoUseCase;
import com.jardineria.application.port.in.EmpleadoUseCase;
import com.jardineria.application.port.in.GamaProductoUseCase;
import com.jardineria.application.port.in.OficinaUseCase;
import com.jardineria.application.port.in.PagoUseCase;
import com.jardineria.application.port.in.PedidoUseCase;
import com.jardineria.application.port.in.ProductoUseCase;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.shell.AppShell;
import com.jardineria.infrastructure.adapter.in.desktop.support.ModalHost;
import com.jardineria.infrastructure.adapter.in.desktop.theme.ThemeManager;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Prueba de humo de la GUI sin base de datos: el shell se alimenta con puertos de entrada
 * simulados (Mockito devuelve listas vacías) y se recorren todas las rutas en ambos temas.
 * <p>
 * Comprueba que ninguna vista lanza excepciones en el hilo de JavaFX ni muestra el estado
 * de error cuando la base de datos está vacía, y que los formularios abren y cierran.
 */
@ExtendWith(ApplicationExtension.class)
class DesktopSmokeTest {
    private final List<Throwable> uncaught = new CopyOnWriteArrayList<>();
    private Thread.UncaughtExceptionHandler previousHandler;
    private AppShell shell;
    private Scene scene;

    @Start
    void start(Stage stage) {
        DashboardUseCase dashboard = mock(DashboardUseCase.class, Mockito.RETURNS_SMART_NULLS);
        when(dashboard.obtenerMetricas()).thenReturn(
                new DashboardMetrics(0, 0, 0, 0, 0, BigDecimal.ZERO, 0, 0, 0, 0));
        DesktopServices services = new DesktopServices(
                dashboard,
                mock(AnalyticsUseCase.class, Mockito.RETURNS_SMART_NULLS),
                mock(ClienteUseCase.class, Mockito.RETURNS_SMART_NULLS),
                mock(PedidoUseCase.class, Mockito.RETURNS_SMART_NULLS),
                mock(DetallePedidoUseCase.class, Mockito.RETURNS_SMART_NULLS),
                mock(PagoUseCase.class, Mockito.RETURNS_SMART_NULLS),
                mock(ProductoUseCase.class, Mockito.RETURNS_SMART_NULLS),
                mock(GamaProductoUseCase.class, Mockito.RETURNS_SMART_NULLS),
                mock(OficinaUseCase.class, Mockito.RETURNS_SMART_NULLS),
                mock(EmpleadoUseCase.class, Mockito.RETURNS_SMART_NULLS),
                null, null,
                new DesktopServices.ConnectionProbe() {
                    @Override
                    public boolean isAvailable() {
                        return true;
                    }

                    @Override
                    public String describe() {
                        return "simulada";
                    }
                });
        shell = new AppShell(services);
        scene = new Scene(shell, 1280, 800);
        ThemeManager.setMode(ThemeManager.Mode.DARK);
        ThemeManager.install(scene);
        shell.installShortcuts(scene);
        stage.setScene(scene);
        stage.show();
    }

    @BeforeEach
    void captureUncaught() {
        previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> uncaught.add(e));
    }

    @AfterEach
    void restoreHandler() {
        Thread.setDefaultUncaughtExceptionHandler(previousHandler);
    }

    @Test
    @DisplayName("Todas las rutas se muestran con la base de datos vacía, sin errores, en tema oscuro y claro")
    void recorreTodasLasRutas() throws Exception {
        for (ThemeManager.Mode mode : ThemeManager.Mode.values()) {
            WaitForAsyncUtils.asyncFx(() -> ThemeManager.setMode(mode)).get();
            for (Route route : Route.values()) {
                WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(route)).get();
                FxTestSupport.settle(350);
                assertThat(shell.getNavigator().getCurrent()).as("ruta activa").isEqualTo(route);
                assertThat(visibleErrorOverlays()).as("estado de error visible en %s (%s)", route, mode).isEmpty();
            }
            // Fuerza que la siguiente pasada vuelva a navegar al panel general.
            WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(Route.PATRONES)).get();
        }
        assertThat(uncaught).as("excepciones no capturadas en el hilo de JavaFX").isEmpty();
    }

    @Test
    @DisplayName("Ctrl+N abre el formulario de alta de cada módulo CRUD y Esc/ModalHost lo cierra")
    void abreYCierraFormularios() throws Exception {
        for (Route route : new Route[]{Route.CLIENTES, Route.PEDIDOS, Route.PAGOS, Route.PRODUCTOS,
                Route.GAMAS, Route.OFICINAS, Route.EMPLEADOS}) {
            WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(route)).get();
            FxTestSupport.settle(300);
            WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().currentView().createNew()).get();
            FxTestSupport.settle(500);
            assertThat(ModalHost.isShowing()).as("formulario abierto en %s", route).isTrue();
            WaitForAsyncUtils.asyncFx(ModalHost::hide).get();
            FxTestSupport.settle(400);
            assertThat(ModalHost.isShowing()).as("formulario cerrado en %s", route).isFalse();
        }
        assertThat(uncaught).isEmpty();
    }

    private List<Node> visibleErrorOverlays() throws Exception {
        return WaitForAsyncUtils.asyncFx(() -> scene.getRoot().lookupAll(".view-overlay-error").stream()
                .filter(Node::isVisible)
                .filter(n -> n.getScene() != null)
                .toList()).get();
    }
}
