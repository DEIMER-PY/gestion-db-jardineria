package com.jardineria.infrastructure.adapter.in.ui.views;

import com.jardineria.application.service.GamaProductoService;
import com.jardineria.domain.model.GamaProducto;
import com.jardineria.infrastructure.adapter.in.ui.components.ExportHelper;
import com.jardineria.infrastructure.adapter.in.ui.components.UiHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Vista de administración para Gamas o Familias de Productos con exportación CSV.
 */
public class GamaView extends JPanel {
    private final GamaProductoService service;
    private DefaultTableModel tableModel;
    private JTable table;

    public GamaView(GamaProductoService service) {
        this.service = service;
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(16, 16, 16, 16));

        initToolbar();
        initTable();
        refreshTable();
    }

    private void initToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(8, 8));
        toolbar.setOpaque(false);

        JLabel lblTitle = new JLabel("🌿 Catálogo de Familias y Gamas de Productos");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 14));

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsPanel.setOpaque(false);

        JButton btnNew = UiHelper.createButton("Nueva Gama", "➕", UiHelper.COLOR_PRIMARY);
        btnNew.addActionListener(e -> mostrarDialogoFormulario(null));

        JButton btnEdit = UiHelper.createButton("Editar", "✏️", UiHelper.COLOR_SECONDARY);
        btnEdit.addActionListener(e -> editarSeleccionada());

        JButton btnDelete = UiHelper.createButton("Eliminar", "🗑️", UiHelper.COLOR_DANGER);
        btnDelete.addActionListener(e -> eliminarSeleccionada());

        JButton btnExport = UiHelper.createButton("Exportar CSV", "📥", UiHelper.COLOR_PURPLE);
        btnExport.addActionListener(e -> ExportHelper.exportTableToCsv(this, table, "gamas_productos"));

        JButton btnRefresh = UiHelper.createButton("Refrescar", "🔄", Color.DARK_GRAY);
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

    private void initTable() {
        String[] cols = {"Nombre de la Gama", "Descripción Detallada", "Referencia Visual / Imagen"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table = UiHelper.createStyledTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void refreshTable() {
        try {
            tableModel.setRowCount(0);
            List<GamaProducto> list = service.listarTodas();
            for (GamaProducto g : list) {
                tableModel.addRow(new Object[]{
                        g.getGama(),
                        g.getDescripcionTexto() != null ? g.getDescripcionTexto() : "",
                        g.getImagen() != null ? g.getImagen() : "Sin imagen asignada"
                });
            }
        } catch (Exception e) {
            UiHelper.showError(this, "Error al listar gamas: " + e.getMessage());
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
        JButton btnGuardar = UiHelper.createButton("Guardar Gama", "💾", UiHelper.COLOR_PRIMARY);
        JButton btnCancelar = UiHelper.createButton("Cancelar", "❌", Color.GRAY);

        btnGuardar.addActionListener(e -> {
            try {
                GamaProducto g = new GamaProducto(txtGama.getText().trim(), txtDesc.getText().trim(), null, txtImg.getText().trim());
                if (esEdicion) {
                    service.actualizarGama(g);
                } else {
                    service.crearGama(g);
                }
                UiHelper.showSuccess(dialog, "Gama guardada exitosamente");
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
