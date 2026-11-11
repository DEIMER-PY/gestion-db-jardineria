package com.jardineria.infrastructure.adapter.in.ui.views;

import com.jardineria.application.service.GamaProductoService;
import com.jardineria.domain.model.GamaProducto;
import com.jardineria.domain.pattern.flyweight.GamaFlyweight;
import com.jardineria.domain.pattern.flyweight.GamaFlyweightFactory;
import com.jardineria.infrastructure.adapter.in.ui.components.ExportHelper;
import com.jardineria.infrastructure.adapter.in.ui.components.UiHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Vista de administración para Gamas o Familias de Productos con galería visual tipo tarjeta y exportación CSV.
 */
public class GamaView extends JPanel {
    private final GamaProductoService service;
    private DefaultTableModel tableModel;
    private JTable table;
    private JPanel cardsPanel;
    private List<GamaProducto> cachedGamas;

    public GamaView(GamaProductoService service) {
        this.service = service;
        setLayout(new BorderLayout(14, 14));
        setBorder(new EmptyBorder(16, 16, 16, 16));

        initToolbar();
        initVisualGallery();
        initTable();
        refreshTable();
    }

    private void initToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(8, 8));
        toolbar.setOpaque(false);

        JLabel lblTitle = new JLabel("Catálogo de Familias y Gamas de Productos (Galería Visual & CRUD)");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 15));

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsPanel.setOpaque(false);

        JButton btnNew = UiHelper.createButton("Nueva Gama", "➕", UiHelper.COLOR_PRIMARY);
        btnNew.addActionListener(e -> mostrarDialogoFormulario(null));

        JButton btnEdit = UiHelper.createButton("Editar", "✏️", UiHelper.COLOR_SECONDARY);
        btnEdit.addActionListener(e -> editarSeleccionada());

        JButton btnDelete = UiHelper.createButton("Eliminar", "", UiHelper.COLOR_DANGER);
        btnDelete.addActionListener(e -> eliminarSeleccionada());

        JButton btnExport = UiHelper.createButton("Exportar CSV", "", UiHelper.COLOR_PURPLE);
        btnExport.addActionListener(e -> ExportHelper.exportTableToCsv(this, table, "gamas_productos"));

        JButton btnRefresh = UiHelper.createButton("Refrescar", "", Color.DARK_GRAY);
        btnRefresh.addActionListener(e -> refreshTable());

        actionsPanel.add(btnNew);
        actionsPanel.add(btnEdit);
        actionsPanel.add(btnDelete);
        actionsPanel.add(btnExport);
        actionsPanel.add(btnRefresh);

        toolbar.add(lblTitle, BorderLayout.WEST);
        toolbar.add(actionsPanel, BorderLayout.EAST);

        add(toolbar, BorderLayout.NORTH);
    }

    private void initVisualGallery() {
        cardsPanel = new JPanel(new GridLayout(1, 5, 10, 10));
        cardsPanel.setOpaque(false);
        cardsPanel.setBorder(new EmptyBorder(4, 0, 8, 0));
    }

    private void initTable() {
        String[] cols = {"Nombre de la Gama", "Descripción Detallada", "Referencia Visual / Imagen"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table = UiHelper.createStyledTable(tableModel);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(cardsPanel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
    }

    public void refreshTable() {
        try {
            cachedGamas = service.listarTodas();
            tableModel.setRowCount(0);
            cardsPanel.removeAll();

            for (GamaProducto g : cachedGamas) {
                tableModel.addRow(new Object[]{
                        g.getGama(),
                        g.getDescripcionTexto() != null ? g.getDescripcionTexto() : "",
                        g.getImagen() != null ? g.getImagen() : "Sin imagen asignada"
                });

                // Crear tarjeta visual para la galería superior
                GamaFlyweight fw = GamaFlyweightFactory.getFlyweight(g.getGama());
                Color color = Color.decode(fw.colorHex());
                cardsPanel.add(createGamaCard(g.getGama(), fw.emojiVisual(), color, g.getDescripcionTexto()));
            }

            cardsPanel.revalidate();
            cardsPanel.repaint();

        } catch (Exception e) {
            UiHelper.showError(this, "Error al listar gamas: " + e.getMessage());
        }
    }

    private JPanel createGamaCard(String gamaName, String emoji, Color accentColor, String desc) {
        JPanel card = new JPanel(new BorderLayout(6, 6));
        card.setOpaque(true);
        card.putClientProperty("FlatLaf.style", "arc: 14");
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 90), 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel lblIcon = new JLabel(emoji);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

        JLabel lblName = new JLabel(gamaName);
        lblName.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblName.setForeground(accentColor);

        JLabel lblDesc = new JLabel("<html><body style='width: 140px;'>" + (desc != null && !desc.isBlank() ? desc : "Familia botánica de jardín") + "</body></html>");
        lblDesc.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblDesc.setForeground(new Color(140, 145, 155));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        top.setOpaque(false);
        top.add(lblIcon);
        top.add(lblName);

        card.add(top, BorderLayout.NORTH);
        card.add(lblDesc, BorderLayout.CENTER);

        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                seleccionarFilaGama(gamaName);
            }
        });

        return card;
    }

    private void seleccionarFilaGama(String gamaName) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getValueAt(i, 0).toString().equalsIgnoreCase(gamaName)) {
                table.setRowSelectionInterval(i, i);
                table.scrollRectToVisible(table.getCellRect(i, 0, true));
                break;
            }
        }
    }

    private void editarSeleccionada() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiHelper.showError(this, "Seleccione una gama de la tabla");
            return;
        }
        String gama = (String) tableModel.getValueAt(row, 0);
        try {
            GamaProducto g = service.obtenerPorId(gama);
            mostrarDialogoFormulario(g);
        } catch (Exception e) {
            UiHelper.showError(this, "Error: " + e.getMessage());
        }
    }

    private void eliminarSeleccionada() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiHelper.showError(this, "Seleccione una gama para eliminar");
            return;
        }
        String gama = (String) tableModel.getValueAt(row, 0);
        if (UiHelper.showConfirm(this, "¿Está seguro de eliminar la gama '" + gama + "'?", "Confirmar Eliminación")) {
            try {
                service.eliminarGama(gama);
                UiHelper.showSuccess(this, "Gama eliminada exitosamente");
                refreshTable();
            } catch (Exception e) {
                UiHelper.showError(this, "Error: " + e.getMessage());
            }
        }
    }

    private void mostrarDialogoFormulario(GamaProducto gEdit) {
        boolean esEdicion = (gEdit != null);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                esEdicion ? "Editar Gama" : "Registrar Nueva Gama", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.setBorder(new EmptyBorder(16, 16, 16, 16));

        JTextField txtGama = new JTextField(esEdicion ? gEdit.getGama() : "");
        txtGama.setEnabled(!esEdicion);
        JTextField txtDesc = new JTextField(esEdicion && gEdit.getDescripcionTexto() != null ? gEdit.getDescripcionTexto() : "");
        JTextField txtImg = new JTextField(esEdicion && gEdit.getImagen() != null ? gEdit.getImagen() : "");

        form.add(new JLabel("Nombre de la Gama:"));
        form.add(txtGama);
        form.add(new JLabel("Descripción de la Familia:"));
        form.add(txtDesc);
        form.add(new JLabel("Imagen o URL de Referencia:"));
        form.add(txtImg);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnGuardar = UiHelper.createButton("Guardar Gama", "", UiHelper.COLOR_PRIMARY);
        JButton btnCancelar = UiHelper.createButton("Cancelar", "❌", Color.GRAY);

        btnGuardar.addActionListener(e -> {
            try {
                GamaProducto g = new GamaProducto(txtGama.getText().trim(), txtDesc.getText().trim(), null, txtImg.getText().trim());
                if (esEdicion) {
                    service.actualizarGama(g);
                } else {
                    service.crearGama(g);
                }
                UiHelper.showSuccess(dialog, "Gama guardada correctamente");
                dialog.dispose();
                refreshTable();
            } catch (Exception ex) {
                UiHelper.showError(dialog, "Error: " + ex.getMessage());
            }
        });

        btnCancelar.addActionListener(e -> dialog.dispose());

        btnPanel.add(btnGuardar);
        btnPanel.add(btnCancelar);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}
