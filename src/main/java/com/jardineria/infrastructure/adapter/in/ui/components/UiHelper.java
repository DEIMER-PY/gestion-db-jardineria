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
 * Utilitarios de interfaz para estandarizar estilos, botones vectoriales, renderizadores, tablas y notificaciones.
 * Libre de emojis planos y optimizado para accesibilidad visual de alta gama.
 */
public final class UiHelper {
    public static final Color COLOR_PRIMARY = new Color(16, 185, 129);   // Modern Emerald
    public static final Color COLOR_SECONDARY = new Color(59, 130, 246); // Modern Blue
    public static final Color COLOR_DANGER = new Color(239, 68, 68);     // Modern Rose / Red
    public static final Color COLOR_WARNING = new Color(245, 158, 11);   // Modern Amber
    public static final Color COLOR_PURPLE = new Color(139, 92, 246);    // Modern Indigo / Violet
    public static final Color COLOR_CYAN = new Color(6, 182, 212);       // Modern Cyan

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES"));

    private UiHelper() {
    }

    public static String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0,00 €";
        return CURRENCY_FORMAT.format(amount);
    }

    public static JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.putClientProperty("Component.arc", 10);
        btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        return btn;
    }

    // Sobrecarga de compatibilidad: Descarta emojis para mantener estética profesional limpia
    public static JButton createButton(String text, String ignoredEmoji, Color bg) {
        return createButton(text, bg);
    }

    public static JButton createIconButton(String text, VectorIcons.IconType iconType, Color bg) {
        JButton btn = new JButton(text);
        if (iconType != null) {
            btn.setIcon(VectorIcons.get(iconType, 16, Color.WHITE));
        }
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.putClientProperty("Component.arc", 10);
        btn.setBorder(new EmptyBorder(6, 12, 6, 12));
        return btn;
    }

    public static void styleButton(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.putClientProperty("Component.arc", 10);
        btn.setBorder(new EmptyBorder(6, 14, 6, 14));
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
                        lbl.setText("Entregado");
                    } else if (str.contains("PENDIENTE")) {
                        lbl.setForeground(COLOR_WARNING);
                        lbl.setText("Pendiente");
                    } else if (str.contains("RECHAZADO")) {
                        lbl.setForeground(COLOR_DANGER);
                        lbl.setText("Rechazado");
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
                        lbl.setText(stock + " [CRÍTICO]");
                    } else if (stock <= 30) {
                        lbl.setForeground(COLOR_WARNING);
                        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                        lbl.setText(stock + " [MEDIO]");
                    } else {
                        lbl.setForeground(COLOR_PRIMARY);
                        lbl.setText(stock + " [ÓPTIMO]");
                    }
                }
                return lbl;
            }
        };
    }

    public static void showSuccess(Component parent, String message) {
        ToastNotificationManager.showSuccess("Operación Exitosa", message);
    }

    public static void showError(Component parent, String message) {
        ToastNotificationManager.showError("Error", message);
    }

    public static void showInfo(Component parent, String message) {
        ToastNotificationManager.showInfo("Información", message);
    }

    public static boolean showConfirm(Component parent, String message, String title) {
        int result = JOptionPane.showConfirmDialog(parent, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }
}
