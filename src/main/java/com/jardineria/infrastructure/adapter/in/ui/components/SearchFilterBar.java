package com.jardineria.infrastructure.adapter.in.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Barra de Búsqueda y Filtrado Avanzado inspirada en los componentes DataTables de JavaTMP.
 * Integra buscador predictivo, chips de filtrado interactivo por estado y contador dinámico de registros.
 */
public class SearchFilterBar extends JPanel {
    private final JTextField txtSearch;
    private final JLabel lblCounter;
    private final JPanel chipsPanel;
    private final Map<String, JButton> chipButtons = new HashMap<>();
    private String activeFilter = "TODOS";
    private BiConsumer<String, String> filterConsumer; // (query, filterStatus)

    public SearchFilterBar(String searchPlaceholder, String[] filterOptions, BiConsumer<String, String> filterConsumer) {
        this.filterConsumer = filterConsumer;

        setLayout(new BorderLayout(12, 8));
        setOpaque(false);
        setBorder(new EmptyBorder(6, 4, 10, 4));

        // Panel Izquierdo: Campo de Búsqueda
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPanel.setOpaque(false);

        JLabel lblSearchIcon = new JLabel(VectorIcons.get(VectorIcons.IconType.SEARCH, 16, new Color(140, 145, 155)));
        leftPanel.add(lblSearchIcon);

        txtSearch = new JTextField(18);
        UiHelper.styleSearchField(txtSearch, searchPlaceholder);
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                notifyChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                notifyChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                notifyChange();
            }
        });
        leftPanel.add(txtSearch);

        // Panel Central: Chips de Filtrado por Estado
        chipsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chipsPanel.setOpaque(false);

        if (filterOptions != null) {
            for (String opt : filterOptions) {
                JButton chip = createFilterChip(opt);
                chipButtons.put(opt.toUpperCase(), chip);
                chipsPanel.add(chip);
            }
        }
        leftPanel.add(chipsPanel);

        // Panel Derecho: Contador de Resultados
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setOpaque(false);

        lblCounter = new JLabel("Mostrando 0 registros");
        lblCounter.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblCounter.setForeground(new Color(140, 145, 155));
        rightPanel.add(lblCounter);

        add(leftPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        // Seleccionar primer chip por defecto
        if (filterOptions != null && filterOptions.length > 0) {
            selectChip(filterOptions[0].toUpperCase());
        }
    }

    private JButton createFilterChip(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.putClientProperty("Component.arc", 12);
        btn.setBorder(new EmptyBorder(4, 10, 4, 10));

        btn.addActionListener(e -> {
            selectChip(text.toUpperCase());
            notifyChange();
        });
        return btn;
    }

    public void selectChip(String filterKey) {
        this.activeFilter = filterKey;
        for (Map.Entry<String, JButton> entry : chipButtons.entrySet()) {
            JButton btn = entry.getValue();
            boolean isSelected = entry.getKey().equalsIgnoreCase(filterKey);
            if (isSelected) {
                btn.setBackground(UiHelper.COLOR_PRIMARY);
                btn.setForeground(Color.WHITE);
            } else {
                btn.setBackground(new Color(150, 150, 150, 25));
                btn.setForeground(ThemeManager.isDarkMode() ? new Color(180, 185, 195) : new Color(71, 85, 105));
            }
        }
    }

    private void notifyChange() {
        if (filterConsumer != null) {
            String query = txtSearch.getText().trim();
            filterConsumer.accept(query, activeFilter);
        }
    }

    public void updateCounter(int current, int total) {
        lblCounter.setText(String.format("Mostrando %d de %d registros", current, total));
    }

    public String getSearchQuery() {
        return txtSearch.getText().trim();
    }

    public String getActiveFilter() {
        return activeFilter;
    }

    public void clear() {
        txtSearch.setText("");
        if (!chipButtons.isEmpty()) {
            selectChip(chipButtons.keySet().iterator().next());
        }
    }
}
