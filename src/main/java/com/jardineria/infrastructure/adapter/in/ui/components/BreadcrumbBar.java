package com.jardineria.infrastructure.adapter.in.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Barra de Navegación y Migas de Pan (Breadcrumbs) para mejorar la orientación y accesibilidad del usuario.
 */
public class BreadcrumbBar extends JPanel {
    private final JLabel lblRoot;
    private final JLabel lblSeparator;
    private final JLabel lblCurrentView;
    private final JLabel lblShortcutBadge;

    public BreadcrumbBar(Consumer<String> navigateConsumer) {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(8, 20, 8, 20));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPanel.setOpaque(false);

        lblRoot = new JLabel("Inicio", VectorIcons.get(VectorIcons.IconType.DASHBOARD, 14, UiHelper.COLOR_PRIMARY), SwingConstants.LEFT);
        lblRoot.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblRoot.setForeground(new Color(140, 145, 155));
        lblRoot.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblRoot.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (navigateConsumer != null) {
                    navigateConsumer.accept("DASHBOARD");
                }
            }
        });

        lblSeparator = new JLabel(VectorIcons.get(VectorIcons.IconType.CHEVRON_RIGHT, 12, new Color(120, 125, 135)));

        lblCurrentView = new JLabel("Panel de Control");
        lblCurrentView.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblCurrentView.setForeground(ThemeManager.isDarkMode() ? Color.WHITE : new Color(30, 41, 59));

        lblShortcutBadge = new JLabel(" Ctrl+1 ");
        lblShortcutBadge.setFont(new Font("Monospaced", Font.BOLD, 10));
        lblShortcutBadge.setForeground(new Color(140, 145, 155));
        lblShortcutBadge.setBackground(new Color(150, 150, 150, 30));
        lblShortcutBadge.setOpaque(true);
        lblShortcutBadge.setBorder(new EmptyBorder(2, 4, 2, 4));

        leftPanel.add(lblRoot);
        leftPanel.add(lblSeparator);
        leftPanel.add(lblCurrentView);
        leftPanel.add(lblShortcutBadge);

        add(leftPanel, BorderLayout.WEST);
    }

    public void updatePath(String moduleName, String shortcutHint, VectorIcons.IconType iconType) {
        lblCurrentView.setText(moduleName);
        if (iconType != null) {
            lblCurrentView.setIcon(VectorIcons.get(iconType, 14, UiHelper.COLOR_PRIMARY));
        } else {
            lblCurrentView.setIcon(null);
        }

        if (shortcutHint != null) {
            lblShortcutBadge.setText(" " + shortcutHint + " ");
            lblShortcutBadge.setVisible(true);
        } else {
            lblShortcutBadge.setVisible(false);
        }

        revalidate();
        repaint();
    }
}
