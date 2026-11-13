package com.jardineria.infrastructure.adapter.in.desktop.shell;

import com.jardineria.infrastructure.adapter.in.desktop.components.BrandMark;
import com.jardineria.infrastructure.adapter.in.desktop.support.FxTasks;
import com.jardineria.infrastructure.adapter.in.desktop.support.Motion;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pantalla de carga: un paisaje de jardín que se dibuja por capas (cielo al amanecer, sol que
 * sale, colinas y plantas que brotan) mientras se ejecutan los pasos reales del arranque. La barra
 * avanza con cada paso completado y el texto dice qué se está haciendo. Con movimiento reducido
 * el paisaje aparece completo y solo avanza la barra.
 */
public class SplashScreen extends StackPane {
    public static final double WIDTH = 880;
    public static final double HEIGHT = 520;

    /** Paso del arranque. {@code enFx} indica si debe ejecutarse en el hilo de JavaFX. */
    public record Paso(String texto, boolean enFx, FxTasks.ThrowingRunnable trabajo) {
        public static Paso segundoPlano(String texto, FxTasks.ThrowingRunnable trabajo) {
            return new Paso(texto, false, trabajo);
        }

        public static Paso interfaz(String texto, FxTasks.ThrowingRunnable trabajo) {
            return new Paso(texto, true, trabajo);
        }
    }

    private final Pane landscape = new Pane();
    private final Label status = new Label("Preparando el jardín…");
    private final Region fill = new Region();
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final List<Node> plants = new ArrayList<>();
    private final Circle sun = new Circle(640, 330, 54);
    private final Circle glow = new Circle(640, 330, 108);
    private final List<Node> hills = new ArrayList<>();
    private Timeline barAnim;

    public SplashScreen() {
        getStyleClass().add("splash");
        setPrefSize(WIDTH, HEIGHT);
        setMinSize(WIDTH, HEIGHT);
        setMaxSize(WIDTH, HEIGHT);
        setAccessibleRole(AccessibleRole.PROGRESS_INDICATOR);
        setAccessibleText("Cargando Jardinería");

        buildLandscape();
        getChildren().addAll(landscape, buildCard());
        status.textProperty().addListener((o, a, b) -> setAccessibleHelp(b));
    }

    // ---------------------------------------------------------------- paisaje

    private void buildLandscape() {
        Rectangle sky = new Rectangle(WIDTH, HEIGHT);
        sky.getStyleClass().add("splash-sky");
        glow.getStyleClass().add("splash-sun-glow");
        sun.getStyleClass().add("splash-sun");

        Group birds = new Group(bird(470, 150), bird(502, 132), bird(534, 158));

        hills.add(hill("M0 330 C 140 280 290 292 430 322 C 580 352 740 300 880 270 L880 520 L0 520 Z", "splash-hill-far"));
        hills.add(hill("M0 392 C 170 342 350 360 520 392 C 680 420 790 380 880 362 L880 520 L0 520 Z", "splash-hill-mid"));
        hills.add(hill("M0 452 C 200 420 420 430 600 456 C 740 476 820 462 880 446 L880 520 L0 520 Z", "splash-hill-near"));

        landscape.getChildren().addAll(sky, glow, sun, birds);
        landscape.getChildren().addAll(hills);

        // Plantas sobre la colina cercana: altura, inclinación y flor variadas
        double[][] spec = {
                {70, 462, 58, -6, 1}, {128, 456, 84, 4, 0}, {182, 452, 46, -10, 2},
                {640, 460, 72, 8, 2}, {706, 466, 96, -4, 1}, {770, 462, 52, 10, 0}, {826, 456, 66, -8, 1}};
        for (double[] p : spec) {
            Node plant = plant(p[0], p[1], p[2], p[3], (int) p[4]);
            plants.add(plant);
            landscape.getChildren().add(plant);
        }
        Rectangle clip = new Rectangle(WIDTH, HEIGHT);
        landscape.setClip(clip);
        landscape.setMouseTransparent(true);
    }

    private static SVGPath hill(String d, String styleClass) {
        SVGPath p = new SVGPath();
        p.setContent(d);
        p.getStyleClass().add(styleClass);
        return p;
    }

    private static Node bird(double x, double y) {
        SVGPath p = new SVGPath();
        p.setContent("M0 0 Q 6 -6 12 0 Q 18 -6 24 0");
        p.getStyleClass().add("splash-bird");
        p.setLayoutX(x);
        p.setLayoutY(y);
        p.setStrokeLineCap(StrokeLineCap.ROUND);
        return p;
    }

    /** Brote con tallo curvo, dos hojas y, según {@code flower}, una flor (0 ninguna, 1 dorada, 2 rosa). */
    private static Node plant(double x, double baseY, double h, double lean, int flower) {
        SVGPath stem = new SVGPath();
        stem.setContent("M0 0 C " + lean * 0.3 + " " + (-h * 0.35) + " " + lean + " " + (-h * 0.7) + " " + lean + " " + (-h));
        stem.getStyleClass().add("splash-stem");
        stem.setStrokeLineCap(StrokeLineCap.ROUND);
        double ly = -h * 0.45;
        SVGPath left = new SVGPath();
        left.setContent("M" + lean * 0.25 + " " + ly + " C -14 " + (ly - 2) + " -22 " + (ly - 12) + " -22 " + (ly - 22)
                + " C -10 " + (ly - 22) + " " + lean * 0.25 + " " + (ly - 12) + " " + lean * 0.25 + " " + ly + " Z");
        left.getStyleClass().add("splash-leaf");
        double ry = -h * 0.62;
        SVGPath right = new SVGPath();
        right.setContent("M" + lean * 0.5 + " " + ry + " C 12 " + (ry - 2) + " 22 " + (ry - 12) + " 24 " + (ry - 24)
                + " C 10 " + (ry - 24) + " " + lean * 0.5 + " " + (ry - 12) + " " + lean * 0.5 + " " + ry + " Z");
        right.getStyleClass().add("splash-leaf-light");
        Group g = new Group(stem, left, right);
        if (flower > 0) {
            Circle petals = new Circle(lean, -h - 4, 7);
            petals.getStyleClass().add(flower == 1 ? "splash-flower-gold" : "splash-flower-rose");
            Circle core = new Circle(lean, -h - 4, 2.6);
            core.getStyleClass().add("splash-flower-core");
            g.getChildren().addAll(petals, core);
        }
        g.setLayoutX(x);
        g.setLayoutY(baseY);
        return g;
    }

    // ---------------------------------------------------------------- tarjeta

    private Node buildCard() {
        BrandMark mark = new BrandMark(56);
        Label name = new Label("Jardinería");
        name.getStyleClass().add("splash-title");
        Label tagline = new Label("Suite de gestión");
        tagline.getStyleClass().add("splash-tagline");
        VBox titles = new VBox(0, name, tagline);
        titles.setAlignment(Pos.CENTER_LEFT);
        HBox brand = new HBox(14, mark, titles);
        brand.setAlignment(Pos.CENTER_LEFT);

        Region track = new Region();
        track.getStyleClass().add("splash-track");
        track.setPrefHeight(6);
        track.setMaxWidth(Double.MAX_VALUE);
        fill.getStyleClass().add("splash-fill");
        fill.setPrefHeight(6);
        fill.setManaged(false);
        fill.resizeRelocate(0, 0, 0, 6);
        Pane bar = new Pane(track, fill);
        bar.setPrefHeight(6);
        bar.setMaxWidth(Double.MAX_VALUE);
        track.prefWidthProperty().bind(bar.widthProperty());
        progress.addListener((o, a, b) -> fill.resize(bar.getWidth() * b.doubleValue(), 6));
        bar.widthProperty().addListener((o, a, b) -> fill.resize(b.doubleValue() * progress.get(), 6));
        status.getStyleClass().add("splash-status");

        VBox card = new VBox(14, brand, bar, status);
        card.getStyleClass().add("splash-card");
        card.setMaxSize(360, Region.USE_PREF_SIZE);
        StackPane.setAlignment(card, Pos.TOP_LEFT);
        card.setTranslateX(40);
        card.setTranslateY(40);

        Label version = new Label("v2.0.0");
        version.getStyleClass().add("splash-version");
        StackPane.setAlignment(version, Pos.BOTTOM_RIGHT);
        version.setTranslateX(-18);
        version.setTranslateY(-12);
        getChildren().add(version);
        return card;
    }

    // ---------------------------------------------------------------- animación y pasos

    /** Inicia el dibujo del paisaje (amanecer, colinas y brotes escalonados). */
    public void animate() {
        if (Motion.isReduced()) {
            return;
        }
        sun.setTranslateY(150);
        glow.setTranslateY(150);
        glow.setOpacity(0);
        Timeline sunrise = new Timeline(new KeyFrame(Duration.millis(2200),
                new KeyValue(sun.translateYProperty(), 0, Motion.EASE_OUT),
                new KeyValue(glow.translateYProperty(), 0, Motion.EASE_OUT),
                new KeyValue(glow.opacityProperty(), 1, Interpolator.EASE_OUT)));
        sunrise.play();

        for (int i = 0; i < hills.size(); i++) {
            Node h = hills.get(i);
            h.setTranslateY(120);
            h.setOpacity(0);
            Timeline t = new Timeline(new KeyFrame(Duration.millis(1100),
                    new KeyValue(h.translateYProperty(), 0, Motion.EASE_OUT),
                    new KeyValue(h.opacityProperty(), 1, Interpolator.EASE_OUT)));
            t.setDelay(Duration.millis(250 + i * 260));
            t.play();
        }
        for (int i = 0; i < plants.size(); i++) {
            Node p = plants.get(i);
            Scale s = new Scale(1, 0, 0, 0);
            p.getTransforms().add(s);
            Timeline grow = new Timeline(new KeyFrame(Duration.millis(900),
                    new KeyValue(s.yProperty(), 1, Motion.EASE_OUT),
                    new KeyValue(s.xProperty(), 1, Motion.EASE_OUT)));
            s.setX(0.4);
            grow.setDelay(Duration.millis(1100 + i * 170));
            grow.play();
        }
    }

    /**
     * Ejecuta los pasos en orden. Cada uno dura al menos {@code minimoPorPaso} para que la animación
     * se vea; la barra avanza al completarse. Si un paso falla, se informa y se continúa: la
     * aplicación ya sabe mostrar los errores (p. ej. «Sin conexión»).
     */
    public void run(List<Paso> pasos, Duration minimoPorPaso, Consumer<Throwable> onError, Runnable onDone) {
        siguiente(pasos, 0, minimoPorPaso, onError, onDone);
    }

    private void siguiente(List<Paso> pasos, int i, Duration minimo, Consumer<Throwable> onError, Runnable onDone) {
        if (i >= pasos.size()) {
            status.setText("Listo");
            avanzar(1, onDone);
            return;
        }
        Paso paso = pasos.get(i);
        status.setText(paso.texto());
        long t0 = System.nanoTime();
        Runnable continuar = () -> {
            double transcurrido = (System.nanoTime() - t0) / 1_000_000.0;
            double espera = Math.max(0, minimo.toMillis() - transcurrido);
            avanzar((i + 1.0) / (pasos.size() + 0.5), null);
            PauseTransition p = new PauseTransition(Duration.millis(espera));
            p.setOnFinished(e -> siguiente(pasos, i + 1, minimo, onError, onDone));
            p.play();
        };
        if (paso.enFx()) {
            try {
                paso.trabajo().run();
            } catch (Exception e) {
                onError.accept(e);
            }
            continuar.run();
        } else {
            FxTasks.runVoid(paso.trabajo(), continuar, e -> {
                onError.accept(e);
                continuar.run();
            });
        }
    }

    private void avanzar(double to, Runnable after) {
        if (barAnim != null) {
            barAnim.stop();
        }
        if (Motion.isReduced()) {
            progress.set(to);
            if (after != null) {
                after.run();
            }
            return;
        }
        barAnim = new Timeline(new KeyFrame(Duration.millis(320), new KeyValue(progress, to, Motion.EASE_OUT)));
        if (after != null) {
            barAnim.setOnFinished(e -> after.run());
        }
        barAnim.play();
    }

    public double getProgress() {
        return progress.get();
    }

    public String getStatusText() {
        return status.getText();
    }
}
