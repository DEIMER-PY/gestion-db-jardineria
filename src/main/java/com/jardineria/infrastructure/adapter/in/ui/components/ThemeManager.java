package com.jardineria.infrastructure.adapter.in.ui.components;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Gestor de temas y estilos visuales con soporte FlatLaf moderno.
 */
public final class ThemeManager {
    private static final Logger log = LoggerFactory.getLogger(ThemeManager.class);
    private static boolean isDark = true;

    private ThemeManager() {
    }

    public static void setupInitialTheme() {
        try {
            FlatDarkLaf.setup();
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
            UIManager.put("ProgressBar.arc", 10);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("ScrollBar.showButtons", false);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
            UIManager.put("Table.rowHeight", 32);
            UIManager.put("TableHeader.height", 36);
            isDark = true;
        } catch (Exception e) {
            log.error("Error al inicializar FlatDarkLaf", e);
        }
    }

    public static void toggleTheme(JFrame frame) {
        try {
            if (isDark) {
                FlatLightLaf.setup();
                isDark = false;
            } else {
                FlatDarkLaf.setup();
                isDark = true;
            }
            FlatLaf.updateUI();
            if (frame != null) {
                SwingUtilities.updateComponentTreeUI(frame);
            }
        } catch (Exception e) {
            log.error("Error al alternar tema visual", e);
        }
    }

    public static boolean isDarkMode() {
        return isDark;
    }
}
