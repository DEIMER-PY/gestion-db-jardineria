package com.jardineria.infrastructure.adapter.in.desktop;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Scene;
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
        WritableImage[] holder = new WritableImage[1];
        WaitForAsyncUtils.asyncFx(() -> holder[0] = node.snapshot(null, null)).get(10, TimeUnit.SECONDS);
        ImageIO.write(SwingFXUtils.fromFXImage(holder[0], null), "png", new File(file.toString()));
        return file;
    }
}
