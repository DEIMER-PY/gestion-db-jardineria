package com.jardineria.infrastructure.adapter.in.desktop;

import com.jardineria.infrastructure.adapter.in.desktop.shell.AppShell;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import com.jardineria.infrastructure.adapter.in.desktop.support.Notifier;
import com.jardineria.infrastructure.adapter.in.desktop.theme.ProfileStore;
import com.jardineria.infrastructure.adapter.in.desktop.theme.ThemeManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.kordamp.ikonli.material2.Material2AL;
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
        ThemeManager.useStore(ProfileStore.preferences());
        AppShell shell = new AppShell(services);

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
        stage.getIcons().add(renderAppIcon());
        stage.show();

        shell.start(AppShell.startRoute());
        log.info("GUI JavaFX iniciada ({}x{})", (int) width, (int) height);
    }

    @Override
    public void stop() {
        Platform.exit();
    }

    /** Genera el ícono de la ventana a partir del glifo vectorial de la marca. */
    private static Image renderAppIcon() {
        StackPane badge = new StackPane(Icons.of(Material2AL.ECO, 40));
        badge.setStyle("-fx-background-color: linear-gradient(to bottom right, #A5ACFF, #4F46E5);"
                + "-fx-background-radius: 14; -fx-min-width: 64; -fx-min-height: 64;");
        badge.getChildren().get(0).setStyle("-fx-icon-color: #F2FFF7;");
        new Scene(badge, Color.TRANSPARENT);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage image = badge.snapshot(params, null);
        return image;
    }
}
