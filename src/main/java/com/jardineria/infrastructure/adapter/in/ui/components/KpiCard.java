package com.jardineria.infrastructure.adapter.in.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Tarjeta KPI moderna y accionable para métricas del dashboard ejecutivo inspirada en Tremor, shadcn y JavaTMP.
 * Integra iconos vectoriales nítidos, gráfico Sparkline de microtendencia, insignias delta y animaciones hover.
 */
public class KpiCard extends JPanel {
    private final JLabel lblValue;
    private final JLabel lblSubtitle;
    private final JLabel lblBadge;
    private final Color accentColor;
    private final SparklineChart sparkline;
    private Runnable clickAction;

    public KpiCard(VectorIcons.IconType vectorIcon, String title, String initialValue, String subtitle, Color accentColor, double[] trendPoints) {
        this.accentColor = accentColor;
        setLayout(new BorderLayout(8, 6));
        setOpaque(true);
        putClientProperty("FlatLaf.style", "arc: 16");
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 60), 1, true),
                new EmptyBorder(14, 16, 12, 16)
        ));

        // Header Superior: Icono Vectorial + Título a la izquierda, Badge Delta a la derecha
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel leftTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftTitle.setOpaque(false);

        // Contenedor redondeado con el icono vectorial
        JPanel iconBubble = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 35));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 70));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        iconBubble.setPreferredSize(new Dimension(32, 32));
        iconBubble.setOpaque(false);
        JLabel lblVector = new JLabel(VectorIcons.get(vectorIcon, 18, accentColor));
        iconBubble.add(lblVector);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblTitle.setForeground(new Color(150, 155, 165));

        leftTitle.add(iconBubble);
        leftTitle.add(lblTitle);

        lblBadge = new JLabel("");
        lblBadge.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblBadge.setVisible(false);

        headerPanel.add(leftTitle, BorderLayout.WEST);
        headerPanel.add(lblBadge, BorderLayout.EAST);

        // Centro: Valor numérico grande + Sparkline al lado derecho
        JPanel centerPanel = new JPanel(new BorderLayout(8, 0));
        centerPanel.setOpaque(false);

        lblValue = new JLabel(initialValue);
        lblValue.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblValue.setForeground(accentColor);
        lblValue.setBorder(new EmptyBorder(6, 2, 4, 2));

        sparkline = new SparklineChart(trendPoints, accentColor);

        centerPanel.add(lblValue, BorderLayout.WEST);
        centerPanel.add(sparkline, BorderLayout.EAST);

        // Subtítulo Inferior y Barra de Acento
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 6));
        bottomPanel.setOpaque(false);

        lblSubtitle = new JLabel(subtitle);
        lblSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblSubtitle.setForeground(new Color(130, 135, 145));

        // Mini barra de acento decorativa tipo Tremor
        JPanel accentBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 120));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
            }
        };
        accentBar.setPreferredSize(new Dimension(0, 3));
        accentBar.setOpaque(false);

        bottomPanel.add(lblSubtitle, BorderLayout.NORTH);
        bottomPanel.add(accentBar, BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Efecto hover interactivo
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (clickAction != null) {
                    setCursor(new Cursor(Cursor.HAND_CURSOR));
                    setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(accentColor, 1, true),
                            new EmptyBorder(14, 16, 12, 16)
                    ));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (clickAction != null) {
                    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 60), 1, true),
                            new EmptyBorder(14, 16, 12, 16)
                    ));
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (clickAction != null) {
                    clickAction.run();
                }
            }
        });
    }

    // Sobrecarga de compatibilidad con emoji anterior
    public KpiCard(String emojiIcon, String title, String initialValue, String subtitle, Color accentColor) {
        this(VectorIcons.IconType.DASHBOARD, title, initialValue, subtitle, accentColor, new double[]{10, 14, 12, 18, 16, 22, 25});
    }

    public void setValue(String value) {
        lblValue.setText(value);
    }

    public void setSubtitle(String subtitle) {
        lblSubtitle.setText(subtitle);
    }

    public void setTrendPoints(double[] points) {
        sparkline.setDataPoints(points);
    }

    public void setDeltaBadge(String text, Color badgeColor) {
        if (text == null || text.trim().isEmpty()) {
            lblBadge.setVisible(false);
            return;
        }
        lblBadge.setText(" " + text + " ");
        lblBadge.setForeground(badgeColor);
        lblBadge.setOpaque(true);
        lblBadge.setBackground(new Color(badgeColor.getRed(), badgeColor.getGreen(), badgeColor.getBlue(), 35));
        lblBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(badgeColor.getRed(), badgeColor.getGreen(), badgeColor.getBlue(), 90), 1, true),
                new EmptyBorder(2, 6, 2, 6)
        ));
        lblBadge.setVisible(true);
    }

    public void setClickAction(Runnable clickAction) {
        this.clickAction = clickAction;
        if (clickAction != null) {
            setToolTipText("Haga clic para ver el módulo correspondiente");
        }
    }
}
