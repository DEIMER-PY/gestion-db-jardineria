package com.jardineria.infrastructure.adapter.in.desktop;

import com.jardineria.infrastructure.adapter.in.desktop.shell.AppShell;
import com.jardineria.infrastructure.adapter.in.desktop.shell.SplashScreen;
import com.jardineria.infrastructure.adapter.in.desktop.support.Motion;
import javafx.animation.FadeTransition;
import javafx.scene.shape.Rectangle;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import java.util.List;
import com.jardineria.infrastructure.adapter.in.desktop.support.Notifier;
import com.jardineria.infrastructure.adapter.in.desktop.theme.ProfileStore;
import com.jardineria.infrastructure.adapter.in.desktop.views.chat.ChatIdentity;
import com.jardineria.infrastructure.adapter.in.desktop.theme.ThemeManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import com.jardineria.infrastructure.adapter.in.desktop.components.BrandMark;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Aplicación de escritorio JavaFX (adaptador de entrada).
 * Recibe los puertos de entrada ya construidos mediante {@link #launchWith(DesktopServices, String...)}.
 */
public class JardineriaDesktopApp extends Application {
    private static final Logger log = LoggerFactory.getLogger(JardineriaDesktopApp.class);
    private static DesktopServices services;
    /** Duración mínima de cada paso de la pantalla de carga, para que la animación se aprecie. */
    private static final long SPLASH_MIN_STEP_MS = 650;

    /** Arranca la GUI con los servicios provistos por el composition root (bloquea hasta cerrar). */
    public static void launchWith(DesktopServices desktopServices, String... args) {
        services = Objects.requireNonNull(desktopServices);
        Application.launch(JardineriaDesktopApp.class, args);
    }

    @Override
    public void start(Stage stage) {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            log.error("Error no controlado en {}", thread.getName(), error);
            Notifier.error("Error inesperado", error.getMessage() == null
                    ? error.getClass().getSimpleName() : error.getMessage());
        });

        ThemeManager.loadFonts();
        if (!Boolean.parseBoolean(System.getProperty("jardineria.splash", "true"))) {
            ThemeManager.useStore(ProfileStore.preferences());
            ChatIdentity.usePreferences();
            showMain(stage, new AppShell(services));
            return;
        }

        Stage splashStage = new Stage(StageStyle.TRANSPARENT);
        SplashScreen splash = new SplashScreen();
        Rectangle clip = new Rectangle(SplashScreen.WIDTH, SplashScreen.HEIGHT);
        clip.setArcWidth(36);
        clip.setArcHeight(36);
        splash.setClip(clip);
        Scene splashScene = new Scene(splash, SplashScreen.WIDTH, SplashScreen.HEIGHT, Color.TRANSPARENT);
        splashScene.getStylesheets().add(JardineriaDesktopApp.class.getResource("/desktop/css/app.css").toExternalForm());
        splashStage.setScene(splashScene);
        splashStage.setTitle("Jardinería Suite");
        splashStage.getIcons().addAll(renderAppIcons());
        splashStage.centerOnScreen();
        splashStage.show();
        splash.animate();

        AppShell[] shell = new AppShell[1];
        List<SplashScreen.Paso> pasos = List.of(
                SplashScreen.Paso.interfaz("Aplicando el tema y tus preferencias…", () -> {
                    ThemeManager.useStore(ProfileStore.preferences());
                    ChatIdentity.usePreferences();
                }),
                SplashScreen.Paso.segundoPlano("Conectando con MySQL (" + services.connection().describe() + ")…", () -> {
                    boolean ok = services.connection().isAvailable();
                    log.info("Base de datos {} al arrancar", ok ? "disponible" : "no disponible");
                }),
                SplashScreen.Paso.interfaz("Preparando los módulos…", () -> shell[0] = new AppShell(services)));
        splash.run(pasos, Duration.millis(SPLASH_MIN_STEP_MS),
                error -> log.warn("Paso de arranque con error: {}", error.getMessage()),
                () -> {
                    if (shell[0] == null) {
                        shell[0] = new AppShell(services);
                    }
                    showMain(stage, shell[0]);
                    FadeTransition fade = new FadeTransition(Duration.millis(Motion.isReduced() ? 1 : 380), splash);
                    fade.setToValue(0);
                    fade.setOnFinished(e -> splashStage.close());
                    fade.play();
                });
    }

    private void showMain(Stage stage, AppShell shell) {
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double width = Math.min(1480, screen.getWidth() * 0.92);
        double height = Math.min(940, screen.getHeight() * 0.92);
        Scene scene = new Scene(shell, width, height);
        ThemeManager.install(scene);
        shell.installShortcuts(scene);

        stage.setTitle("Jardinería Suite — Gestión comercial");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.getIcons().addAll(renderAppIcons());
        stage.show();
        stage.toFront();

        shell.start(AppShell.startRoute());
        log.info("GUI JavaFX iniciada ({}x{})", (int) width, (int) height);
    }

    @Override
    public void stop() {
        Platform.exit();
    }

    /** Icono de la ventana en varios tamaños, renderizado desde el logo vectorial. */
    static java.util.List<Image> renderAppIcons() {
        return java.util.List.of(renderLogo(16), renderLogo(32), renderLogo(64), renderLogo(128), renderLogo(256));
    }

    /** Logo en PNG de {@code size}×{@code size} con fondo transparente. */
    public static WritableImage renderLogo(double size) {
        BrandMark mark = new BrandMark(size);
        Scene scene = new Scene(new StackPane(mark), Color.TRANSPARENT);
        scene.getRoot().setStyle("-fx-background-color: transparent;");
        scene.getStylesheets().add(JardineriaDesktopApp.class.getResource("/desktop/css/app.css").toExternalForm());
        mark.applyCss();
        mark.layout();
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return mark.snapshot(params, null);
    }
}
