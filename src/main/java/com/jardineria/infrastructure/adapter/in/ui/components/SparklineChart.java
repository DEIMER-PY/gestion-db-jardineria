package com.jardineria.infrastructure.adapter.in.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Gráfico Sparkline ultraligero que renderiza microtendencias visuales tipo SaaS / Fintech.
 * Incorpora curva suavizada, gradiente de relleno translúcido y punto de dato final.
 */
public class SparklineChart extends JComponent {
    private double[] dataPoints;
    private Color strokeColor;
    private boolean showGradientFill;

    public SparklineChart(double[] initialPoints, Color strokeColor) {
        this.dataPoints = initialPoints != null ? initialPoints : new double[]{10, 15, 13, 22, 19, 28, 32};
        this.strokeColor = strokeColor != null ? strokeColor : UiHelper.COLOR_PRIMARY;
        this.showGradientFill = true;
        setPreferredSize(new Dimension(84, 28));
        setMinimumSize(new Dimension(50, 20));
        setOpaque(false);
    }

    public void setDataPoints(double[] points) {
        this.dataPoints = points;
        repaint();
    }

    public void setStrokeColor(Color color) {
        this.strokeColor = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (dataPoints == null || dataPoints.length < 2) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = getWidth() - 4;
        int h = getHeight() - 6;
        int startX = 2;
        int startY = 3;

        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (double d : dataPoints) {
            if (d < min) min = d;
            if (d > max) max = d;
        }
        if (min == max) {
            max += 1.0;
        }

        int n = dataPoints.length;
        float[] px = new float[n];
        float[] py = new float[n];

        for (int i = 0; i < n; i++) {
            px[i] = startX + (float) i * w / (n - 1);
            py[i] = startY + h - (float) ((dataPoints[i] - min) / (max - min) * (h - 4));
        }

        // 1. Relleno con gradiente hacia abajo
        if (showGradientFill) {
            Path2D area = new Path2D.Float();
            area.moveTo(px[0], py[0]);
            for (int i = 1; i < n; i++) {
                area.lineTo(px[i], py[i]);
            }
            area.lineTo(px[n - 1], startY + h);
            area.lineTo(px[0], startY + h);
            area.closePath();

            Color topColor = new Color(strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue(), 60);
            Color bottomColor = new Color(strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue(), 0);
            GradientPaint gradient = new GradientPaint(0, startY, topColor, 0, startY + h, bottomColor);
            g2.setPaint(gradient);
            g2.fill(area);
        }

        // 2. Línea principal suavizada
        Path2D line = new Path2D.Float();
        line.moveTo(px[0], py[0]);
        for (int i = 1; i < n; i++) {
            line.lineTo(px[i], py[i]);
        }
        g2.setColor(strokeColor);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(line);

        // 3. Punto final con halo
        float lastX = px[n - 1];
        float lastY = py[n - 1];
        g2.setColor(new Color(strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue(), 90));
        g2.fillOval((int) (lastX - 4), (int) (lastY - 4), 8, 8);
        g2.setColor(strokeColor);
        g2.fillOval((int) (lastX - 2.5f), (int) (lastY - 2.5f), 5, 5);

        g2.dispose();
    }
}
