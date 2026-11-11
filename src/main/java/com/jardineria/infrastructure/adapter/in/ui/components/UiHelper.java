package com.jardineria.infrastructure.adapter.in.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utilitarios de interfaz para estandarizar estilos, botones, renderizadores y tablas.
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
        btn.putClientProperty("Component.arc", 10);
        return btn;
    }

    public static void styleButton(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.putClientProperty("Component.arc", 10);
        btn.setBorder(new EmptyBorder(6, 12, 6, 12));
    }

    public static void styleSearchField(JTextField field, String placeholder) {
        field.putClientProperty("JTextField.placeholderText", placeholder);
        field.putClientProperty("JTextField.showClearButton", true);
        field.putClientProperty("Component.arc", 12);
        field.setFont(new Font("SansSerif", Font.PLAIN, 12));
        field.setMargin(new Insets(4, 8, 4, 8));
    }

    public static JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    Color baseBg = getBackground();
                    if (row % 2 == 1) {
                        int delta = ThemeManager.isDarkMode() ? 10 : -8;
                        int r = Math.max(0, Math.min(255, baseBg.getRed() + delta));
                        int g = Math.max(0, Math.min(255, baseBg.getGreen() + delta));
                        int b = Math.max(0, Math.min(255, baseBg.getBlue() + delta));
                        c.setBackground(new Color(r, g, b));
                    } else {
                        c.setBackground(baseBg);
                    }
                }
                return c;
            }
        };

        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setRowHeight(28);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Integer.class, centerRenderer);

        return table;
    }

    public static DefaultTableCellRenderer createStatusBadgeRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));

                if (value != null && !isSelected) {
                    String str = value.toString().trim().toUpperCase();
                    if (str.contains("ENTREGADO")) {
                        lbl.setForeground(COLOR_PRIMARY);
                        lbl.setText("● Entregado");
                    } else if (str.contains("PENDIENTE")) {
                        lbl.setForeground(COLOR_WARNING);
                        lbl.setText("⏳ Pendiente");
                    } else if (str.contains("RECHAZADO")) {
                        lbl.setForeground(COLOR_DANGER);
                        lbl.setText("✖ Rechazado");
                    }
                }
                return lbl;
            }
        };
    }

    public static DefaultTableCellRenderer createStockBadgeRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);

                if (value instanceof Integer stock && !isSelected) {
                    if (stock <= 10) {
                        lbl.setForeground(COLOR_DANGER);
                        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                        lbl.setText(stock + " ⚠️ CRÍTICO");
                    } else if (stock <= 30) {
                        lbl.setForeground(COLOR_WARNING);
                        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                        lbl.setText(stock + " ⚡ MEDIO");
                    } else {
                        lbl.setForeground(COLOR_PRIMARY);
                        lbl.setText(stock + " ✔ ÓPTIMO");
                    }
                }
                return lbl;
            }
        };
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
