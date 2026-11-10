package com.jardineria.infrastructure.adapter.in.ui.views;

import com.jardineria.application.service.OficinaService;
import com.jardineria.domain.model.Oficina;
import com.jardineria.infrastructure.adapter.in.ui.components.UiHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Vista de administración y CRUD para Oficinas.
 */
public class OficinaView extends JPanel {
    private final OficinaService service;
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField txtSearch;

    public OficinaView(OficinaService service) {
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

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchPanel.setOpaque(false);

        JLabel lblSearch = new JLabel("🔍 Filtrar por País/Ciudad:");
        lblSearch.setFont(new Font("SansSerif", Font.BOLD, 12));
        txtSearch = new JTextField(15);

        JButton btnSearch = UiHelper.createButton("Filtrar", null, UiHelper.COLOR_SECONDARY);
        btnSearch.addActionListener(e -> filtrar());

        JButton btnClear = UiHelper.createButton("Limpiar", null, Color.GRAY);
        btnClear.addActionListener(e -> {
            txtSearch.setText("");
            refreshTable();
        });

        searchPanel.add(lblSearch);
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnClear);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsPanel.setOpaque(false);

        JButton btnNew = UiHelper.createButton("Nueva Oficina", "➕", UiHelper.COLOR_PRIMARY);
        btnNew.addActionListener(e -> mostrarDialogoFormulario(null));

        JButton btnEdit = UiHelper.createButton("Editar", "✏️", UiHelper.COLOR_SECONDARY);
        btnEdit.addActionListener(e -> editarSeleccionada());

        JButton btnDelete = UiHelper.createButton("Eliminar", "🗑️", UiHelper.COLOR_DANGER);
        btnDelete.addActionListener(e -> eliminarSeleccionada());

        JButton btnRefresh = UiHelper.createButton("Refrescar", "🔄", Color.DARK_GRAY);
        btnRefresh.addActionListener(e -> refreshTable());

        actionsPanel.add(btnNew);
        actionsPanel.add(btnEdit);
        actionsPanel.add(btnDelete);
        actionsPanel.add(btnRefresh);

        toolbar.add(searchPanel, BorderLayout.WEST);
        toolbar.add(actionsPanel, BorderLayout.EAST);

        add(toolbar, BorderLayout.NORTH);
    }

    private void initTable() {
        String[] cols = {"Código", "Ciudad", "País", "Región", "Cód. Postal", "Teléfono", "Dirección 1", "Dirección 2"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = UiHelper.createStyledTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void refreshTable() {
        try {
            tableModel.setRowCount(0);
            List<Oficina> list = service.listarTodas();
            for (Oficina o : list) {
                tableModel.addRow(new Object[]{
                        o.getCodigoOficina(),
                        o.getCiudad(),
                        o.getPais(),
                        o.getRegion() != null ? o.getRegion() : "",
                        o.getCodigoPostal(),
                        o.getTelefono(),
                        o.getLineaDireccion1(),
                        o.getLineaDireccion2() != null ? o.getLineaDireccion2() : ""
                });
            }
        } catch (Exception e) {
            UiHelper.showError(this, "Error al listar oficinas: " + e.getMessage());
        }
    }

    private void filtrar() {
        String texto = txtSearch.getText().trim();
        if (texto.isEmpty()) {
            refreshTable();
            return;
        }
        try {
            tableModel.setRowCount(0);
            List<Oficina> list = service.buscarPorPais(texto);
            if (list.isEmpty()) {
                list = service.buscarPorCiudad(texto);
            }
            for (Oficina o : list) {
                tableModel.addRow(new Object[]{
                        o.getCodigoOficina(), o.getCiudad(), o.getPais(),
                        o.getRegion(), o.getCodigoPostal(), o.getTelefono(),
                        o.getLineaDireccion1(), o.getLineaDireccion2()
                });
            }
        } catch (Exception e) {
            UiHelper.showError(this, "Error al filtrar oficinas: " + e.getMessage());
        }
    }

    private void editarSeleccionada() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiHelper.showError(this, "Por favor seleccione una oficina de la tabla");
            return;
        }
        String id = (String) tableModel.getValueAt(row, 0);
        try {
            Oficina oficina = service.obtenerPorId(id);
            mostrarDialogoFormulario(oficina);
        } catch (Exception e) {
            UiHelper.showError(this, "Error al cargar oficina: " + e.getMessage());
        }
    }

    private void eliminarSeleccionada() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiHelper.showError(this, "Por favor seleccione una oficina de la tabla para eliminar");
            return;
        }
        String id = (String) tableModel.getValueAt(row, 0);
        if (UiHelper.showConfirm(this, "¿Está seguro de eliminar la oficina " + id + "?", "Confirmar Eliminación")) {
            try {
                service.eliminarOficina(id);
                UiHelper.showSuccess(this, "Oficina eliminada correctamente");
                refreshTable();
            } catch (Exception e) {
                UiHelper.showError(this, "Error al eliminar oficina: " + e.getMessage());
            }
        }
    }

    private void mostrarDialogoFormulario(Oficina oficinaEdit) {
        boolean esEdicion = (oficinaEdit != null);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                esEdicion ? "Editar Oficina" : "Nueva Oficina", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(8, 2, 8, 8));
        form.setBorder(new EmptyBorder(16, 16, 16, 16));

        JTextField txtCodigo = new JTextField(esEdicion ? oficinaEdit.getCodigoOficina() : "");
        txtCodigo.setEnabled(!esEdicion);
        JTextField txtCiudad = new JTextField(esEdicion ? oficinaEdit.getCiudad() : "");
        JTextField txtPais = new JTextField(esEdicion ? oficinaEdit.getPais() : "");
        JTextField txtRegion = new JTextField(esEdicion && oficinaEdit.getRegion() != null ? oficinaEdit.getRegion() : "");
        JTextField txtCp = new JTextField(esEdicion ? oficinaEdit.getCodigoPostal() : "");
        JTextField txtTel = new JTextField(esEdicion ? oficinaEdit.getTelefono() : "");
        JTextField txtDir1 = new JTextField(esEdicion ? oficinaEdit.getLineaDireccion1() : "");
        JTextField txtDir2 = new JTextField(esEdicion && oficinaEdit.getLineaDireccion2() != null ? oficinaEdit.getLineaDireccion2() : "");

        form.add(new JLabel("Código Oficina:"));
        form.add(txtCodigo);
        form.add(new JLabel("Ciudad:"));
        form.add(txtCiudad);
        form.add(new JLabel("País:"));
        form.add(txtPais);
        form.add(new JLabel("Región:"));
        form.add(txtRegion);
        form.add(new JLabel("Código Postal:"));
        form.add(txtCp);
        form.add(new JLabel("Teléfono:"));
        form.add(txtTel);
        form.add(new JLabel("Línea Dirección 1:"));
        form.add(txtDir1);
        form.add(new JLabel("Línea Dirección 2:"));
        form.add(txtDir2);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnGuardar = UiHelper.createButton("Guardar", "💾", UiHelper.COLOR_PRIMARY);
        JButton btnCancelar = UiHelper.createButton("Cancelar", "❌", Color.GRAY);

        btnGuardar.addActionListener(e -> {
            try {
                Oficina of = new Oficina(
                        txtCodigo.getText().trim(),
                        txtCiudad.getText().trim(),
                        txtPais.getText().trim(),
                        txtRegion.getText().trim(),
                        txtCp.getText().trim(),
                        txtTel.getText().trim(),
                        txtDir1.getText().trim(),
                        txtDir2.getText().trim()
                );
                if (esEdicion) {
                    service.actualizarOficina(of);
                } else {
                    service.crearOficina(of);
                }
                UiHelper.showSuccess(dialog, "Oficina guardada con éxito");
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
