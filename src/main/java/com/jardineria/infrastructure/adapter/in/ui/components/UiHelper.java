package com.jardineria.infrastructure.adapter.in.ui.components;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utilitarios de interfaz para estandarizar estilos, botones y tablas.
 */
public final class UiHelper {
    public static final Color COLOR_PRIMARY = new Color(46, 160, 67);    // Emerald Green
    public static final Color COLOR_SECONDARY = new Color(56, 139, 253); // Royal Blue
    public static final Color COLOR_DANGER = new Color(248, 81, 73);     // Crimson Red
    public static final Color COLOR_WARNING = new Color(210, 153, 34);   // Amber
    public static final Color COLOR_PURPLE = new Color(163, 113, 247);   // Purple

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES"));

    private UiHelper() {
    }

    public static String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0,00 €";
        return CURRENCY_FORMAT.format(amount);
    }

    public static JButton createButton(String text, String emoji, Color bg) {
        JButton btn = new JButton((emoji != null ? emoji + " " : "") + text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("JButton.buttonType", "roundRect");
        return btn;
    }

    public static JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Integer.class, centerRenderer);

        return table;
    }

    public static void showSuccess(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Operación Exitosa", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean showConfirm(Component parent, String message, String title) {
        int result = JOptionPane.showConfirmDialog(parent, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }
}
