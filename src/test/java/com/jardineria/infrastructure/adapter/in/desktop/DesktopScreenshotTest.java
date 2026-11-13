package com.jardineria.infrastructure.adapter.in.desktop;

import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.shell.AppShell;
import com.jardineria.infrastructure.adapter.in.desktop.support.ModalHost;
import com.jardineria.infrastructure.adapter.in.desktop.theme.ThemeManager;
import com.jardineria.infrastructure.config.ApplicationContext;
import com.jardineria.infrastructure.config.DatabaseConfig;
import com.jardineria.infrastructure.config.DatabaseConnectionManager;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
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
import java.time.Year;

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
    private Stage stage;

    @BeforeAll
    static void requiereBaseDeDatos() {
        assumeTrue(DatabaseConnectionManager.isAvailable(3), "MySQL no disponible: se omiten las capturas");
    }

    @Start
    void start(Stage stage) {
        this.stage = stage;
        ThemeManager.loadFonts();
        ApplicationContext ctx = ApplicationContext.getInstance();
        DesktopServices services = new DesktopServices(
                ctx.getDashboardService(), ctx.getAnalyticsService(), ctx.getClienteService(),
                ctx.getPedidoService(), ctx.getDetallePedidoService(), ctx.getPagoService(),
                ctx.getProductoService(), ctx.getGamaProductoService(), ctx.getOficinaService(),
                ctx.getEmpleadoService(),
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
        resize(1440, 900);
        for (ThemeManager.Mode mode : ThemeManager.Mode.values()) {
            WaitForAsyncUtils.asyncFx(() -> ThemeManager.setMode(mode)).get();
            for (Route route : Route.values()) {
                WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(route)).get();
                FxTestSupport.settle(1600);
                FxTestSupport.scrollCurrentViewToTop(scene);
                FxTestSupport.settle(200);
                String name = mode.name().toLowerCase() + "-" + route.name().toLowerCase();
                Path png = FxTestSupport.snapshot(scene, name);
                assertThat(Files.size(png)).isPositive();
                if (FxTestSupport.scrollCurrentViewToBottom(scene)) {
                    FxTestSupport.settle(300);
                    FxTestSupport.snapshot(scene, name + "-bottom");
                }
            }
            // Fuerza que la siguiente pasada vuelva a mostrar el panel general
            WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(Route.ANALYTICS)).get();
        }
    }

    @Test
    @DisplayName("Maestro-detalle y formularios: selección de fila y alta abierta en Clientes y Pedidos")
    @SuppressWarnings("unchecked")
    void capturaDetalleYFormulario() throws Exception {
        resize(1440, 900);
        WaitForAsyncUtils.asyncFx(() -> ThemeManager.setMode(ThemeManager.Mode.DARK)).get();
        for (Route route : new Route[]{Route.CLIENTES, Route.PEDIDOS, Route.EMPLEADOS}) {
            WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(route)).get();
            FxTestSupport.settle(1600);
            TableView<Object> table = (TableView<Object>) scene.getRoot().lookupAll(".crud-table").stream()
                    .filter(n -> n.getScene() != null && n.isVisible() && n.getParent().isVisible())
                    .reduce((a, b) -> b).orElseThrow();
            assertThat(table.getItems()).isNotEmpty();
            WaitForAsyncUtils.asyncFx(() -> table.getSelectionModel().select(0)).get();
            FxTestSupport.settle(1500);
            FxTestSupport.snapshot(scene, "detail-" + route.name().toLowerCase());
            WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().currentView().createNew()).get();
            FxTestSupport.settle(1200);
            FxTestSupport.snapshot(scene, "form-" + route.name().toLowerCase());
            WaitForAsyncUtils.asyncFx(ModalHost::hide).get();
            FxTestSupport.settle(400);
        }
    }

    @Test
    @DisplayName("En la ventana mínima (1100x720) los paneles se reorganizan sin recortarse")
    void capturaVentanaMinima() throws Exception {
        WaitForAsyncUtils.asyncFx(() -> {
            ThemeManager.setMode(ThemeManager.Mode.DARK);
            stage.setWidth(1100);
            stage.setHeight(720);
        }).get();
        for (Route route : new Route[]{Route.DASHBOARD, Route.ANALYTICS, Route.PRODUCTOS, Route.PEDIDOS, Route.AGENDA, Route.EMPLEADOS, Route.TABLERO}) {
            WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(route)).get();
            FxTestSupport.settle(1600);
            FxTestSupport.scrollCurrentViewToTop(scene);
            FxTestSupport.settle(200);
            FxTestSupport.snapshot(scene, "narrow-" + route.name().toLowerCase());
        }
    }

    private void resize(double w, double h) throws Exception {
        WaitForAsyncUtils.asyncFx(() -> {
            stage.setWidth(w);
            stage.setHeight(h);
        }).get();
        FxTestSupport.settle(300);
    }

    @Test
    @DisplayName("El filtro de año de Métricas recarga los datos del periodo")
    @SuppressWarnings("unchecked")
    void capturaFiltroAnio() throws Exception {
        WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(Route.ANALYTICS)).get();
        FxTestSupport.settle(1600);
        ComboBox<Year> filtro = (ComboBox<Year>) scene.lookup(".year-filter");
        assertThat(filtro.getItems()).contains(Year.of(2009));
        WaitForAsyncUtils.asyncFx(() -> filtro.setValue(Year.of(2009))).get();
        FxTestSupport.settle(1600);
        FxTestSupport.snapshot(scene, "dark-analytics-2009");
        assertThat(scene.getRoot().lookupAll(".panel-caption").stream()
                .map(n -> ((javafx.scene.control.Label) n).getText()))
                .contains("Meses con actividad en 2009");
    }

}
