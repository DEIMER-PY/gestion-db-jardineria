package com.jardineria.infrastructure.adapter.in.desktop.components;

import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;

/**
 * Logo de la aplicación, dibujado en vector: insignia verde bosque con sol, colinas y un brote
 * de dos hojas. Se escala a cualquier tamaño sin perder nitidez (menú, icono de ventana, login y
 * pantalla de carga). Los colores están en {@code app.css} ({@code .brand-mark-*}); la versión
 * SVG equivalente está en {@code docs/img/logo.svg}.
 */
public class BrandMark extends StackPane {
    /** Lado del diseño original; las coordenadas de los trazos están en esta escala. */
    static final double BASE = 64;

    static final String HILL_BACK = "M0 44 C 12 37 24 36 36 41 C 46 45 56 44 64 38 L64 64 L0 64 Z";
    static final String HILL_FRONT = "M0 52 C 14 46 30 46 44 51 C 52 54 58 54 64 51 L64 64 L0 64 Z";
    static final String STEM = "M31 51 C 31 44 31.5 38 33.5 31";
    static final String LEAF_LEFT = "M31.5 40 C 23 40 17.5 34.5 17 26.5 C 25.5 26.5 31 31.5 31.5 40 Z";
    static final String LEAF_RIGHT = "M33.5 32 C 34.5 23 41 17.5 49.5 18 C 49 26.5 42.5 31.5 33.5 32 Z";
    static final String VEIN_RIGHT = "M35.5 30 C 39 25.5 42.5 22.5 46 20.5";

    public BrandMark(double size) {
        Rectangle bg = new Rectangle(BASE, BASE);
        bg.setArcWidth(30);
        bg.setArcHeight(30);
        bg.getStyleClass().add("brand-mark-bg");

        Circle sun = new Circle(46, 17, 6.5);
        sun.getStyleClass().add("brand-mark-sun");
        Circle glow = new Circle(46, 17, 11);
        glow.getStyleClass().add("brand-mark-glow");

        SVGPath back = path(HILL_BACK, "brand-mark-hill-back");
        SVGPath front = path(HILL_FRONT, "brand-mark-hill-front");
        SVGPath stem = path(STEM, "brand-mark-stem");
        stem.setStrokeLineCap(StrokeLineCap.ROUND);
        SVGPath left = path(LEAF_LEFT, "brand-mark-leaf");
        SVGPath right = path(LEAF_RIGHT, "brand-mark-leaf-light");
        SVGPath vein = path(VEIN_RIGHT, "brand-mark-vein");
        vein.setStrokeLineCap(StrokeLineCap.ROUND);

        Group art = new Group(bg, glow, sun, back, front, stem, left, right, vein);
        Rectangle clip = new Rectangle(BASE, BASE);
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        art.setClip(clip);
        double k = size / BASE;
        art.setScaleX(k);
        art.setScaleY(k);

        getChildren().add(new Group(art));
        getStyleClass().add("brand-mark");
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);
        setMouseTransparent(true);
        setFocusTraversable(false);
    }

    private static SVGPath path(String d, String styleClass) {
        SVGPath p = new SVGPath();
        p.setContent(d);
        p.getStyleClass().add(styleClass);
        return p;
    }
}
