package com.jardineria.infrastructure.adapter.in.desktop;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import org.testfx.util.WaitForAsyncUtils;

import javax.imageio.ImageIO;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Utilidades compartidas por las pruebas de la GUI.
 */
final class FxTestSupport {
    private FxTestSupport() {
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
