package com.jardineria.infrastructure.adapter.in.desktop;

import com.jardineria.infrastructure.adapter.in.desktop.shell.SplashScreen;
import com.jardineria.infrastructure.adapter.in.desktop.support.Motion;
import com.jardineria.infrastructure.adapter.in.desktop.theme.ThemeManager;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/** La pantalla de carga recorre los pasos reales, tolera fallos y deja una captura para la documentación. */
@ExtendWith(ApplicationExtension.class)
class SplashScreenTest {
    private Scene scene;
    private SplashScreen splash;

    @Start
    void start(Stage stage) {
        ThemeManager.loadFonts();
        Motion.reducedProperty().set(true);
        splash = new SplashScreen();
        scene = new Scene(splash, SplashScreen.WIDTH, SplashScreen.HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/desktop/css/app.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    @AfterEach
    void restoreMotion() {
        Motion.reducedProperty().set(false);
    }

    @Test
    @DisplayName("Ejecuta los pasos en orden (FX y segundo plano), sigue tras un error y termina al 100 %")
    void recorrePasos() throws Exception {
        List<String> orden = java.util.Collections.synchronizedList(new ArrayList<>());
        List<Throwable> errores = new ArrayList<>();
        AtomicBoolean fin = new AtomicBoolean();
        WaitForAsyncUtils.asyncFx(() -> splash.run(List.of(
                SplashScreen.Paso.interfaz("Tema", () -> orden.add("tema")),
                SplashScreen.Paso.segundoPlano("BD", () -> {
                    orden.add("bd");
                    throw new IllegalStateException("sin conexión");
                }),
                SplashScreen.Paso.interfaz("Interfaz", () -> orden.add("ui"))),
                Duration.ZERO, errores::add, () -> fin.set(true))).get();

        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, fin::get);
        assertThat(orden).containsExactly("tema", "bd", "ui");
        assertThat(errores).hasSize(1);
        assertThat(splash.getProgress()).isEqualTo(1.0);
        assertThat(splash.getStatusText()).isEqualTo("Listo");
    }

    @Test
    @DisplayName("Captura del paisaje con la barra a medio camino")
    void captura() throws Exception {
        CountDownLatch espera = new CountDownLatch(1);
        WaitForAsyncUtils.asyncFx(() -> splash.run(List.of(
                SplashScreen.Paso.interfaz("Aplicando el tema y tus preferencias…", () -> { }),
                SplashScreen.Paso.segundoPlano("Conectando con MySQL (localhost:3307 / jardineria)…",
                        () -> espera.await(10, TimeUnit.SECONDS))),
                Duration.ZERO, e -> { }, () -> { })).get();
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> splash.getStatusText().startsWith("Conectando"));
        FxTestSupport.settle(150);
        Path png = FxTestSupport.snapshot(scene, "splash");
        espera.countDown();
        assertThat(splash.getProgress()).isBetween(0.2, 0.6);
        assertThat(png).exists();
    }
}
