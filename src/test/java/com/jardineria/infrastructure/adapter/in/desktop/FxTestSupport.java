package com.jardineria.infrastructure.adapter.in.desktop;

import com.jardineria.application.dto.DashboardMetrics;
import com.jardineria.application.port.in.AnalyticsUseCase;
import com.jardineria.application.port.in.AsistenteUseCase;
import com.jardineria.application.port.in.ClienteUseCase;
import com.jardineria.application.port.in.DashboardUseCase;
import com.jardineria.application.port.in.DetallePedidoUseCase;
import com.jardineria.application.port.in.EmpleadoUseCase;
import com.jardineria.application.port.in.MensajeriaUseCase;
import com.jardineria.application.port.in.GamaProductoUseCase;
import com.jardineria.application.port.in.OficinaUseCase;
import com.jardineria.application.port.in.PagoUseCase;
import com.jardineria.application.port.in.PedidoUseCase;
import com.jardineria.application.port.in.ProductoUseCase;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import org.testfx.util.WaitForAsyncUtils;

import javax.imageio.ImageIO;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Utilidades compartidas por las pruebas de la GUI.
 */
final class FxTestSupport {
    private FxTestSupport() {
    }

    /**
     * Servicios simulados con conexión disponible: cada puerto es un mock vacío salvo los
     * que se pasan en {@code overrides} (se asignan por tipo).
     */
    static DesktopServices services(Object... overrides) {
        DashboardUseCase dashboard = mock(DashboardUseCase.class, RETURNS_SMART_NULLS);
        when(dashboard.obtenerMetricas()).thenReturn(new DashboardMetrics(0, 0, 0, 0, 0, BigDecimal.ZERO, 0, 0, 0, 0));
        return new DesktopServices(
                pick(DashboardUseCase.class, dashboard, overrides),
                pick(AnalyticsUseCase.class, null, overrides),
                pick(ClienteUseCase.class, null, overrides),
                pick(PedidoUseCase.class, null, overrides),
                pick(DetallePedidoUseCase.class, null, overrides),
                pick(PagoUseCase.class, null, overrides),
                pick(ProductoUseCase.class, null, overrides),
                pick(GamaProductoUseCase.class, null, overrides),
                pick(OficinaUseCase.class, null, overrides),
                pick(EmpleadoUseCase.class, null, overrides),
                new DesktopServices.ConnectionProbe() {
                    @Override
                    public boolean isAvailable() {
                        return true;
                    }

                    @Override
                    public String describe() {
                        return "simulada";
                    }
                },
                pick(AsistenteUseCase.class, DesktopServices.SIN_ASISTENTE, overrides),
                pick(MensajeriaUseCase.class, DesktopServices.SIN_MENSAJERIA, overrides));
    }

    private static <P> P pick(Class<P> port, P fallback, Object[] overrides) {
        return Arrays.stream(overrides).filter(port::isInstance).map(port::cast).findFirst()
                .orElseGet(() -> fallback != null ? fallback : mock(port, RETURNS_SMART_NULLS));
    }

    /** Espera a que se vacíe la cola de eventos FX y a que terminen las consultas asíncronas. */
    static void settle(long millis) {
        WaitForAsyncUtils.waitForFxEvents();
        WaitForAsyncUtils.sleep(millis, TimeUnit.MILLISECONDS);
        WaitForAsyncUtils.waitForFxEvents();
    }

    /** Guarda una captura PNG de la escena en target/screenshots. */
    static Path snapshot(Scene scene, String name) throws Exception {
        Path dir = Path.of(System.getProperty("screenshots.dir", "target/screenshots"));
        Files.createDirectories(dir);
        Path file = dir.resolve(name + ".png");
        Files.createDirectories(file.getParent());
        WritableImage[] holder = new WritableImage[1];
        WaitForAsyncUtils.asyncFx(() -> holder[0] = scene.snapshot(null)).get(10, TimeUnit.SECONDS);
        ImageIO.write(SwingFXUtils.fromFXImage(holder[0], null), "png", new File(file.toString()));
        return file;
    }

    /** Guarda una captura PNG de un nodo concreto. */
    static Path snapshot(Node node, String name) throws Exception {
        Path dir = Path.of(System.getProperty("screenshots.dir", "target/screenshots"));
        Files.createDirectories(dir);
        Path file = dir.resolve(name + ".png");
        Files.createDirectories(file.getParent());
        WritableImage[] holder = new WritableImage[1];
        WaitForAsyncUtils.asyncFx(() -> holder[0] = node.snapshot(null, null)).get(10, TimeUnit.SECONDS);
        ImageIO.write(SwingFXUtils.fromFXImage(holder[0], null), "png", new File(file.toString()));
        return file;
    }

    /**
     * Desplaza al final el ScrollPane de la vista visible.
     *
     * @return {@code true} si había contenido por debajo del pliegue
     */
    static boolean scrollCurrentViewToBottom(Scene scene) throws Exception {
        return scrollCurrentView(scene, true);
    }

    /** Devuelve la vista visible a su posición inicial (las vistas se cachean entre capturas). */
    static void scrollCurrentViewToTop(Scene scene) throws Exception {
        scrollCurrentView(scene, false);
    }

    private static boolean scrollCurrentView(Scene scene, boolean bottom) throws Exception {
        return WaitForAsyncUtils.asyncFx(() -> scene.getRoot().lookupAll(".view-scroll").stream()
                .filter(n -> n instanceof ScrollPane && n.isVisible() && n.getParent() != null
                        && n.getParent().isVisible() && n.getScene() != null)
                .map(ScrollPane.class::cast)
                .filter(sp -> sp.getContent() != null
                        && sp.getContent().getBoundsInLocal().getHeight() > sp.getViewportBounds().getHeight() + 4)
                .peek(sp -> sp.setVvalue(bottom ? sp.getVmax() : sp.getVmin()))
                .count() > 0).get(10, TimeUnit.SECONDS);
    }
}
