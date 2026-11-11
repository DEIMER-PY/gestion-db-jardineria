package com.jardineria.infrastructure.adapter.in.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * Componente visual que dibuja una barra de distribución porcentual segmentada con esquinas redondeadas.
 */
public class DistributionBar extends JComponent {
    private long value1; // Entregados (Verde)
    private long value2; // Pendientes (Ámbar)
    private long value3; // Rechazados (Rojo)

    private Color color1 = UiHelper.COLOR_PRIMARY;
    private Color color2 = UiHelper.COLOR_WARNING;
    private Color color3 = UiHelper.COLOR_DANGER;

    public DistributionBar() {
        setPreferredSize(new Dimension(300, 16));
        setMinimumSize(new Dimension(200, 14));
    }

    public void setValues(long v1, long v2, long v3) {
        this.value1 = Math.max(0, v1);
        this.value2 = Math.max(0, v2);
        this.value3 = Math.max(0, v3);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int arc = height;

        long total = value1 + value2 + value3;
        if (total == 0) {
            g2.setColor(new Color(150, 150, 150, 40));
            g2.fillRoundRect(0, 0, width, height, arc, arc);
            g2.dispose();
            return;
        }

        // Crear clip redondeado para que todos los segmentos compartan los bordes redondeados
        Shape clip = new java.awt.geom.RoundRectangle2D.Float(0, 0, width, height, arc, arc);
        g2.setClip(clip);

        int w1 = (int) Math.round(((double) value1 / total) * width);
        int w2 = (int) Math.round(((double) value2 / total) * width);
        int w3 = width - w1 - w2;

        int currentX = 0;

        // Segmento 1
        g2.setColor(color1);
        g2.fillRect(currentX, 0, w1, height);
        currentX += w1;

        // Segmento 2
        g2.setColor(color2);
        g2.fillRect(currentX, 0, w2, height);
        currentX += w2;

        // Segmento 3
        g2.setColor(color3);
        g2.fillRect(currentX, 0, Math.max(0, w3), height);

        g2.dispose();
    }
}
