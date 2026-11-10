package com.jardineria.infrastructure.adapter.in.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Tarjeta KPI moderna para métricas del dashboard.
 */
public class KpiCard extends JPanel {
    private final JLabel lblValue;
    private final JLabel lblSubtitle;

    public KpiCard(String emojiIcon, String title, String initialValue, String subtitle, Color accentColor) {
        setLayout(new BorderLayout(10, 8));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 60), 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));

        // Header (Emoji + Title)
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        headerPanel.setOpaque(false);

        JLabel lblIcon = new JLabel(emojiIcon);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblTitle.setForeground(new Color(170, 175, 185));

        headerPanel.add(lblIcon);
        headerPanel.add(lblTitle);

        // Center Value
        lblValue = new JLabel(initialValue);
        lblValue.setFont(new Font("SansSerif", Font.BOLD, 26));
        lblValue.setForeground(accentColor);
        lblValue.setBorder(new EmptyBorder(6, 4, 2, 4));

        // Bottom Subtitle
        lblSubtitle = new JLabel(subtitle);
        lblSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblSubtitle.setForeground(new Color(140, 145, 155));
        lblSubtitle.setBorder(new EmptyBorder(0, 4, 0, 4));

        add(headerPanel, BorderLayout.NORTH);
        add(lblValue, BorderLayout.CENTER);
        add(lblSubtitle, BorderLayout.SOUTH);
    }

    public void setValue(String value) {
        lblValue.setText(value);
    }

    public void setSubtitle(String subtitle) {
        lblSubtitle.setText(subtitle);
    }
}
