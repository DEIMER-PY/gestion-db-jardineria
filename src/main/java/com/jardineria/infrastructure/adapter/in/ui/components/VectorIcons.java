package com.jardineria.infrastructure.adapter.in.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * Motor de iconos vectoriales ultraligeros basados en Java2D nativo.
 * Ofrece renderizado nítido a cualquier resolución o escala DPI sin dependencias externas
 * y con total soporte de paletas de color y temas.
 */
public final class VectorIcons {

    public enum IconType {
        DASHBOARD,
        CLIENTS,
        ORDERS,
        PRODUCTS,
        PAYMENTS,
        OFFICES,
        EMPLOYEES,
        GAMAS,
        PATTERNS,
        SEARCH,
        REFRESH,
        EXPORT,
        SUN,
        MOON,
        CHECK,
        ALERT_TRIANGLE,
        ALERT_CIRCLE,
        INFO,
        EXTERNAL_LINK,
        CHEVRON_RIGHT,
        TREND_UP,
        TREND_DOWN,
        DATABASE,
        FILTER,
        CLOSE
    }

    private VectorIcons() {
    }

    public static Icon get(IconType type, int size, Color color) {
        return new VectorIcon(type, size, color);
    }

    public static Icon get(IconType type, int size) {
        return new VectorIcon(type, size, null);
    }

    public static Icon get(IconType type) {
        return new VectorIcon(type, 18, null);
    }

    private static class VectorIcon implements Icon {
        private final IconType type;
        private final int size;
        private final Color color;

        public VectorIcon(IconType type, int size, Color color) {
            this.type = type;
            this.size = size;
            this.color = color;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            Color paintColor = (color != null) ? color : (c != null ? c.getForeground() : Color.WHITE);
            g2.setColor(paintColor);

            // Escalar sistema de coordenadas relativo a 24x24 estándar
            g2.translate(x, y);
            double scale = size / 24.0;
            g2.scale(scale, scale);

            float strokeWidth = 2.0f;
            g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            switch (type) {
                case DASHBOARD -> {
                    g2.draw(new RoundRectangle2D.Float(3, 3, 7, 7, 2, 2));
                    g2.draw(new RoundRectangle2D.Float(14, 3, 7, 7, 2, 2));
                    g2.draw(new RoundRectangle2D.Float(14, 14, 7, 7, 2, 2));
                    g2.draw(new RoundRectangle2D.Float(3, 14, 7, 7, 2, 2));
                }
                case CLIENTS -> {
                    // Persona central
                    g2.draw(new Ellipse2D.Float(6, 4, 6, 6));
                    Path2D p = new Path2D.Float();
                    p.moveTo(4, 19);
                    p.curveTo(4, 15, 6, 13, 9, 13);
                    p.curveTo(12, 13, 14, 15, 14, 19);
                    g2.draw(p);
                    // Persona lateral
                    Path2D p2 = new Path2D.Float();
                    p2.moveTo(16, 4.5f);
                    p2.curveTo(17.5f, 4.5f, 18.5f, 5.5f, 18.5f, 7f);
                    p2.curveTo(18.5f, 8.5f, 17.5f, 9.5f, 16, 9.5f);
                    g2.draw(p2);
                    Path2D p3 = new Path2D.Float();
                    p3.moveTo(17, 13);
                    p3.curveTo(19, 13.5f, 20.5f, 15, 20.5f, 18);
                    g2.draw(p3);
                }
                case ORDERS -> {
                    // Caja isométrica
                    Path2D box = new Path2D.Float();
                    box.moveTo(12, 2.5f);
                    box.lineTo(21, 7.5f);
                    box.lineTo(21, 16.5f);
                    box.lineTo(12, 21.5f);
                    box.lineTo(3, 16.5f);
                    box.lineTo(3, 7.5f);
                    box.closePath();
                    g2.draw(box);
                    // Líneas interiores
                    g2.draw(new Line2D.Float(12, 2.5f, 12, 12));
                    g2.draw(new Line2D.Float(12, 12, 21, 7.5f));
                    g2.draw(new Line2D.Float(12, 12, 3, 7.5f));
                }
                case PRODUCTS -> {
                    // Hoja botánica
                    Path2D leaf = new Path2D.Float();
                    leaf.moveTo(4, 20);
                    leaf.curveTo(4, 10, 11, 3, 20, 3);
                    leaf.curveTo(20, 12, 14, 20, 4, 20);
                    leaf.closePath();
                    g2.draw(leaf);
                    g2.draw(new Line2D.Float(4, 20, 15, 9));
                }
                case PAYMENTS -> {
                    // Tarjeta de crédito
                    g2.draw(new RoundRectangle2D.Float(2, 5, 20, 14, 3, 3));
                    g2.draw(new Line2D.Float(2, 10, 22, 10));
                    g2.fill(new RoundRectangle2D.Float(5, 14, 4, 2.5f, 1, 1));
                }
                case OFFICES -> {
                    // Edificio corporativo
                    g2.draw(new RoundRectangle2D.Float(4, 3, 16, 18, 2, 2));
                    g2.draw(new Line2D.Float(8, 7, 10, 7));
                    g2.draw(new Line2D.Float(14, 7, 16, 7));
                    g2.draw(new Line2D.Float(8, 11, 10, 11));
                    g2.draw(new Line2D.Float(14, 11, 16, 11));
                    g2.draw(new RoundRectangle2D.Float(10, 16, 4, 5, 1, 1));
                }
                case EMPLOYEES -> {
                    // Lanyard / Badge
                    g2.draw(new RoundRectangle2D.Float(5, 5, 14, 16, 3, 3));
                    g2.draw(new Ellipse2D.Float(9.5f, 8, 5, 5));
                    g2.draw(new Line2D.Float(8, 17, 16, 17));
                    g2.draw(new RoundRectangle2D.Float(10, 2, 4, 3, 1, 1));
                }
                case GAMAS -> {
                    // Etiqueta de categoría
                    Path2D tag = new Path2D.Float();
                    tag.moveTo(3, 12);
                    tag.lineTo(12, 3);
                    tag.lineTo(20, 3);
                    tag.lineTo(20, 11);
                    tag.lineTo(11, 20);
                    tag.closePath();
                    g2.draw(tag);
                    g2.fill(new Ellipse2D.Float(15, 6, 2.5f, 2.5f));
                }
                case PATTERNS -> {
                    // Capas apiladas GoF
                    Path2D l1 = new Path2D.Float();
                    l1.moveTo(12, 2);
                    l1.lineTo(21, 6.5f);
                    l1.lineTo(12, 11);
                    l1.lineTo(3, 6.5f);
                    l1.closePath();
                    g2.draw(l1);

                    Path2D l2 = new Path2D.Float();
                    l2.moveTo(3, 11.5f);
                    l2.lineTo(12, 16);
                    l2.lineTo(21, 11.5f);
                    g2.draw(l2);

                    Path2D l3 = new Path2D.Float();
                    l3.moveTo(3, 16.5f);
                    l3.lineTo(12, 21);
                    l3.lineTo(21, 16.5f);
                    g2.draw(l3);
                }
                case SEARCH -> {
                    g2.draw(new Ellipse2D.Float(4, 4, 11, 11));
                    g2.draw(new Line2D.Float(12.5f, 12.5f, 20, 20));
                }
                case REFRESH -> {
                    Path2D arc = new Path2D.Float();
                    arc.append(new Arc2D.Float(3, 3, 18, 18, 45, 270, Arc2D.OPEN), true);
                    g2.draw(arc);
                    // Flecha
                    Path2D arrow = new Path2D.Float();
                    arrow.moveTo(18, 2);
                    arrow.lineTo(21, 5.5f);
                    arrow.lineTo(17.5f, 9);
                    g2.draw(arrow);
                }
                case EXPORT -> {
                    g2.draw(new Line2D.Float(12, 3, 12, 14));
                    Path2D down = new Path2D.Float();
                    down.moveTo(8, 10);
                    down.lineTo(12, 14);
                    down.lineTo(16, 10);
                    g2.draw(down);
                    Path2D tray = new Path2D.Float();
                    tray.moveTo(4, 14);
                    tray.lineTo(4, 19);
                    tray.lineTo(20, 19);
                    tray.lineTo(20, 14);
                    g2.draw(tray);
                }
                case SUN -> {
                    g2.draw(new Ellipse2D.Float(7.5f, 7.5f, 9, 9));
                    float[][] rays = {
                            {12, 2, 12, 4.5f}, {12, 19.5f, 12, 22},
                            {2, 12, 4.5f, 12}, {19.5f, 12, 22, 12},
                            {4.9f, 4.9f, 6.7f, 6.7f}, {17.3f, 17.3f, 19.1f, 19.1f},
                            {4.9f, 19.1f, 6.7f, 17.3f}, {17.3f, 6.7f, 19.1f, 4.9f}
                    };
                    for (float[] r : rays) {
                        g2.draw(new Line2D.Float(r[0], r[1], r[2], r[3]));
                    }
                }
                case MOON -> {
                    Path2D moon = new Path2D.Float();
                    moon.moveTo(21, 12.79f);
                    moon.curveTo(19.84f, 13.56f, 18.47f, 14, 17, 14);
                    moon.curveTo(11.48f, 14, 7, 9.52f, 7, 4);
                    moon.curveTo(7, 2.53f, 7.44f, 1.16f, 8.21f, 0);
                    moon.curveTo(4.56f, 1.63f, 2, 5.28f, 2, 9.5f);
                    moon.curveTo(2, 15.3f, 6.7f, 20, 12.5f, 20);
                    moon.curveTo(16.72f, 20, 20.37f, 17.44f, 21, 12.79f);
                    moon.closePath();
                    g2.draw(moon);
                }
                case CHECK -> {
                    Path2D check = new Path2D.Float();
                    check.moveTo(4, 12.5f);
                    check.lineTo(9.5f, 17.5f);
                    check.lineTo(20, 6.5f);
                    g2.draw(check);
                }
                case ALERT_TRIANGLE -> {
                    Path2D tri = new Path2D.Float();
                    tri.moveTo(12, 3);
                    tri.lineTo(22, 20);
                    tri.lineTo(2, 20);
                    tri.closePath();
                    g2.draw(tri);
                    g2.draw(new Line2D.Float(12, 9, 12, 13.5f));
                    g2.fill(new Ellipse2D.Float(11, 15.5f, 2, 2));
                }
                case ALERT_CIRCLE -> {
                    g2.draw(new Ellipse2D.Float(2.5f, 2.5f, 19, 19));
                    g2.draw(new Line2D.Float(12, 7, 12, 12.5f));
                    g2.fill(new Ellipse2D.Float(11, 14.5f, 2, 2));
                }
                case INFO -> {
                    g2.draw(new Ellipse2D.Float(2.5f, 2.5f, 19, 19));
                    g2.fill(new Ellipse2D.Float(11, 6.5f, 2, 2));
                    g2.draw(new Line2D.Float(12, 10.5f, 12, 16.5f));
                }
                case EXTERNAL_LINK -> {
                    g2.draw(new Line2D.Float(18, 13, 18, 19));
                    Path2D box = new Path2D.Float();
                    box.moveTo(18, 13);
                    box.lineTo(18, 19);
                    box.lineTo(5, 19);
                    box.lineTo(5, 6);
                    box.lineTo(11, 6);
                    g2.draw(box);
                    g2.draw(new Line2D.Float(13, 5, 20, 5));
                    g2.draw(new Line2D.Float(20, 5, 20, 12));
                    g2.draw(new Line2D.Float(9, 16, 20, 5));
                }
                case CHEVRON_RIGHT -> {
                    Path2D chev = new Path2D.Float();
                    chev.moveTo(9, 6);
                    chev.lineTo(15, 12);
                    chev.lineTo(9, 18);
                    g2.draw(chev);
                }
                case TREND_UP -> {
                    Path2D tUp = new Path2D.Float();
                    tUp.moveTo(3, 17);
                    tUp.lineTo(9, 11);
                    tUp.lineTo(14, 16);
                    tUp.lineTo(21, 7);
                    g2.draw(tUp);
                    g2.draw(new Line2D.Float(15, 7, 21, 7));
                    g2.draw(new Line2D.Float(21, 7, 21, 13));
                }
                case TREND_DOWN -> {
                    Path2D tDn = new Path2D.Float();
                    tDn.moveTo(3, 7);
                    tDn.lineTo(9, 13);
                    tDn.lineTo(14, 8);
                    tDn.lineTo(21, 17);
                    g2.draw(tDn);
                    g2.draw(new Line2D.Float(15, 17, 21, 17));
                    g2.draw(new Line2D.Float(21, 17, 21, 11));
                }
                case DATABASE -> {
                    g2.draw(new Arc2D.Float(4, 3, 16, 5, 0, 360, Arc2D.OPEN));
                    Path2D d1 = new Path2D.Float();
                    d1.moveTo(4, 5.5f);
                    d1.lineTo(4, 11);
                    d1.append(new Arc2D.Float(4, 8.5f, 16, 5, 180, 180, Arc2D.OPEN), true);
                    d1.lineTo(20, 5.5f);
                    g2.draw(d1);

                    Path2D d2 = new Path2D.Float();
                    d2.moveTo(4, 11);
                    d2.lineTo(4, 16.5f);
                    d2.append(new Arc2D.Float(4, 14, 16, 5, 180, 180, Arc2D.OPEN), true);
                    d2.lineTo(20, 11);
                    g2.draw(d2);
                }
                case FILTER -> {
                    Path2D flt = new Path2D.Float();
                    flt.moveTo(3, 4);
                    flt.lineTo(21, 4);
                    flt.lineTo(14, 12);
                    flt.lineTo(14, 18);
                    flt.lineTo(10, 20);
                    flt.lineTo(10, 12);
                    flt.closePath();
                    g2.draw(flt);
                }
                case CLOSE -> {
                    g2.draw(new Line2D.Float(6, 6, 18, 18));
                    g2.draw(new Line2D.Float(18, 6, 6, 18));
                }
            }

            g2.dispose();
        }
    }
}
