package com.jardineria.infrastructure.adapter.in.desktop;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** El logo vectorial se renderiza con sus colores y esquinas transparentes; exporta PNG para la documentación. */
@ExtendWith(ApplicationExtension.class)
class BrandMarkTest {

    @Start
    void start(Stage stage) {
        // solo necesita el toolkit de JavaFX
    }

    @Test
    @DisplayName("El logo tiene fondo verde, sol cálido, hojas claras y esquinas redondeadas transparentes")
    void renderiza() throws Exception {
        WritableImage img = WaitForAsyncUtils.asyncFx(() -> JardineriaDesktopApp.renderLogo(256)).get();
        assertThat(img.getWidth()).isEqualTo(256);
        PixelReader px = img.getPixelReader();
        Path dir = Path.of(System.getProperty("screenshots.dir", "target/screenshots"));
        Files.createDirectories(dir);
        for (int size : new int[]{256, 64}) {
            WritableImage out = size == 256 ? img : WaitForAsyncUtils.asyncFx(() -> JardineriaDesktopApp.renderLogo(size)).get();
            ImageIO.write(SwingFXUtils.fromFXImage(out, null), "png", dir.resolve("logo-" + size + ".png").toFile());
        }

        assertThat(px.getColor(1, 1).getOpacity()).isLessThan(0.1);            // esquina redondeada
        Color fondo = px.getColor(110, 30);                                  // cielo, lejos del sol
        assertThat(fondo.getOpacity()).isGreaterThan(0.95);
        assertThat(fondo.getGreen()).isGreaterThan(fondo.getRed()).isGreaterThan(fondo.getBlue());
        Color sol = px.getColor(184, 68);                                      // (46, 17) × 4
        assertThat(sol.getRed()).isGreaterThan(0.9);
        assertThat(sol.getBlue()).isLessThan(0.5);
        Color hoja = px.getColor(96, 132);                                     // hoja izquierda (24, 33) × 4
        assertThat(hoja.getBrightness()).isGreaterThan(0.9);

        assertThat(dir.resolve("logo-256.png")).exists();
    }
}
