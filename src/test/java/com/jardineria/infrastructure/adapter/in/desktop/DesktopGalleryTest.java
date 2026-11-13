package com.jardineria.infrastructure.adapter.in.desktop;

import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.shell.AppShell;
import com.jardineria.infrastructure.adapter.in.desktop.support.ModalHost;
import com.jardineria.infrastructure.adapter.in.desktop.theme.Accent;
import com.jardineria.infrastructure.adapter.in.desktop.theme.AppearanceProfile;
import com.jardineria.infrastructure.adapter.in.desktop.theme.ThemeManager;
import com.jardineria.infrastructure.adapter.in.desktop.theme.ThemePreset;
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

import java.util.Map;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Galería de apariencia para la documentación: el panel general en cada tema y acento,
 * la pantalla de Ajustes, la chuleta de atajos y la paleta de comandos.
 * Guarda las imágenes en {@code target/screenshots/gallery}.
 * <p>
 * Ejecución: {@code mvn test -Dtest=DesktopGalleryTest -Dui.screenshots=true}
 */
@EnabledIfSystemProperty(named = "ui.screenshots", matches = "true")
@ExtendWith(ApplicationExtension.class)
class DesktopGalleryTest {
    private static final Map<ThemePreset, Accent> SHOWCASE = Map.of(
            ThemePreset.GRAFITO, Accent.INDIGO,
            ThemePreset.MEDIANOCHE, Accent.TURQUESA,
            ThemePreset.BOSQUE, Accent.ESMERALDA,
            ThemePreset.CONTRASTE, Accent.AMBAR,
            ThemePreset.NIEBLA, Accent.COBALTO,
            ThemePreset.ARENA, Accent.CORAL);

    private AppShell shell;
    private Scene scene;
    private Stage stage;

    @BeforeAll
    static void requiereBaseDeDatos() {
        assumeTrue(DatabaseConnectionManager.isAvailable(3), "MySQL no disponible: se omite la galería");
    }

    @Start
    void start(Stage stage) {
        this.stage = stage;
        ThemeManager.loadFonts();
        ThemeManager.activate(AppearanceProfile.defaults().name());
        ThemeManager.resetActive();
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
        ThemeManager.install(scene);
        shell.installShortcuts(scene);
        stage.setScene(scene);
        stage.show();
    }

    private static String dir(String name) {
        return "gallery/" + name;
    }

    @Test
    @DisplayName("Panel general en cada tema con su acento de muestra")
    void panelPorTema() throws Exception {
        for (ThemePreset theme : ThemePreset.values()) {
            WaitForAsyncUtils.asyncFx(() -> ThemeManager.update(p -> p.withTheme(theme)
                    .withAccent(SHOWCASE.get(theme)))).get();
            WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(Route.ANALYTICS)).get();
            WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(Route.DASHBOARD)).get();
            FxTestSupport.settle(1800);
            FxTestSupport.scrollCurrentViewToTop(scene);
            FxTestSupport.settle(250);
            FxTestSupport.snapshot(scene, dir("dashboard-" + theme.id()));
        }
        WaitForAsyncUtils.asyncFx(ThemeManager::resetActive).get();
    }

    @Test
    @DisplayName("Panel general completo (desplazado) en Grafito e Índigo")
    void panelCompleto() throws Exception {
        WaitForAsyncUtils.asyncFx(ThemeManager::resetActive).get();
        WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(Route.DASHBOARD)).get();
        FxTestSupport.settle(1800);
        FxTestSupport.scrollCurrentViewToTop(scene);
        FxTestSupport.settle(250);
        FxTestSupport.snapshot(scene, dir("dashboard-top"));
        WaitForAsyncUtils.asyncFx(() -> scene.getRoot().lookupAll(".view-scroll").stream()
                .filter(n -> n.isVisible() && n.getScene() != null)
                .map(javafx.scene.control.ScrollPane.class::cast)
                .forEach(sp -> sp.setVvalue(0.22))).get();
        FxTestSupport.settle(1300);
        WaitForAsyncUtils.asyncFx(() -> ((com.jardineria.infrastructure.adapter.in.desktop.views.dashboard.DashboardView)
                shell.getNavigator().currentView()).highlightTrend(8)).get();
        FxTestSupport.settle(300);
        FxTestSupport.snapshot(scene, dir("dashboard-hover"));
        for (double v : new double[]{0.62, 1.0}) {
            WaitForAsyncUtils.asyncFx(() -> scene.getRoot().lookupAll(".view-scroll").stream()
                    .filter(n -> n.isVisible() && n.getScene() != null)
                    .map(javafx.scene.control.ScrollPane.class::cast)
                    .forEach(sp -> sp.setVvalue(v))).get();
            FxTestSupport.settle(700);
            FxTestSupport.snapshot(scene, dir("dashboard-" + (v < 1 ? "middle" : "bottom")));
        }
    }

    @Test
    @DisplayName("Ajustes, atajos y paleta de comandos")
    void ajustesYAyuda() throws Exception {
        WaitForAsyncUtils.asyncFx(ThemeManager::resetActive).get();
        WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(Route.AJUSTES)).get();
        FxTestSupport.settle(900);
        FxTestSupport.scrollCurrentViewToTop(scene);
        FxTestSupport.snapshot(scene, dir("settings-dark"));
        FxTestSupport.scrollCurrentViewToBottom(scene);
        FxTestSupport.settle(300);
        FxTestSupport.snapshot(scene, dir("settings-dark-bottom"));
        WaitForAsyncUtils.asyncFx(() -> ThemeManager.setMode(ThemeManager.Mode.LIGHT)).get();
        FxTestSupport.settle(500);
        FxTestSupport.scrollCurrentViewToTop(scene);
        FxTestSupport.settle(300);
        FxTestSupport.snapshot(scene, dir("settings-light"));
        WaitForAsyncUtils.asyncFx(() -> ThemeManager.setMode(ThemeManager.Mode.DARK)).get();

        WaitForAsyncUtils.asyncFx(shell::showShortcuts).get();
        FxTestSupport.settle(700);
        FxTestSupport.snapshot(scene, dir("shortcuts"));
        WaitForAsyncUtils.asyncFx(ModalHost::hide).get();
        FxTestSupport.settle(400);

        WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(Route.DASHBOARD)).get();
        FxTestSupport.settle(1500);
        WaitForAsyncUtils.asyncFx(() -> shell.openPalette("tema")).get();
        FxTestSupport.settle(700);
        FxTestSupport.snapshot(scene, dir("palette"));
        WaitForAsyncUtils.asyncFx(ModalHost::hide).get();
        FxTestSupport.settle(300);
    }

    @Test
    @DisplayName("Ventana mínima (1100x720) con perfil compacto")
    void ventanaMinima() throws Exception {
        WaitForAsyncUtils.asyncFx(() -> {
            ThemeManager.resetActive();
            stage.setWidth(1100);
            stage.setHeight(720);
        }).get();
        WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(Route.DASHBOARD)).get();
        FxTestSupport.settle(1800);
        FxTestSupport.scrollCurrentViewToTop(scene);
        FxTestSupport.snapshot(scene, dir("narrow-dashboard"));
        WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(Route.AJUSTES)).get();
        FxTestSupport.settle(800);
        FxTestSupport.snapshot(scene, dir("narrow-settings"));
        WaitForAsyncUtils.asyncFx(() -> ThemeManager.update(p -> p.withDensity(AppearanceProfile.Density.COMPACTA))).get();
        WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(Route.DASHBOARD)).get();
        FxTestSupport.settle(1500);
        FxTestSupport.scrollCurrentViewToTop(scene);
        FxTestSupport.snapshot(scene, dir("narrow-dashboard-compact"));
        WaitForAsyncUtils.asyncFx(() -> {
            ThemeManager.resetActive();
            stage.setWidth(1440);
            stage.setHeight(900);
        }).get();
    }
}
