package com.jardineria.infrastructure.adapter.in.ui.views;

import com.jardineria.application.service.OficinaService;
import com.jardineria.domain.model.Oficina;
import com.jardineria.infrastructure.adapter.in.ui.components.ExportHelper;
import com.jardineria.infrastructure.adapter.in.ui.components.UiHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Vista de administración y CRUD para Oficinas con búsqueda en tiempo real y exportación CSV.
 */
public class OficinaView extends JPanel {
    private final com.jardineria.application.port.in.OficinaUseCase service;
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField txtSearch;
    private List<Oficina> cachedOficinas;

    public OficinaView(com.jardineria.application.port.in.OficinaUseCase service) {
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

        JLabel lblSearch = new JLabel("Buscar oficina:");
        lblSearch.setFont(new Font("SansSerif", Font.BOLD, 12));
        txtSearch = new JTextField(18);
        UiHelper.styleSearchField(txtSearch, "País, ciudad o código...");
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filtrarEnTiempoReal();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filtrarEnTiempoReal();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filtrarEnTiempoReal();
            }
        });

        searchPanel.add(lblSearch);
        searchPanel.add(txtSearch);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsPanel.setOpaque(false);

        JButton btnNew = UiHelper.createButton("Nueva Oficina", "➕", UiHelper.COLOR_PRIMARY);
        btnNew.addActionListener(e -> mostrarDialogoFormulario(null));

        JButton btnEdit = UiHelper.createButton("Editar", "✏️", UiHelper.COLOR_SECONDARY);
        btnEdit.addActionListener(e -> editarSeleccionada());

        JButton btnDelete = UiHelper.createButton("Eliminar", "", UiHelper.COLOR_DANGER);
        btnDelete.addActionListener(e -> eliminarSeleccionada());

        JButton btnExport = UiHelper.createButton("Exportar CSV", "", UiHelper.COLOR_PURPLE);
        btnExport.addActionListener(e -> ExportHelper.exportTableToCsv(this, table, "oficinas_jardineria"));

        JButton btnRefresh = UiHelper.createButton("Refrescar", "", Color.DARK_GRAY);
        btnRefresh.addActionListener(e -> refreshTable());

        actionsPanel.add(btnNew);
        actionsPanel.add(btnEdit);
        actionsPanel.add(btnDelete);
        actionsPanel.add(btnExport);
        actionsPanel.add(btnRefresh);

        toolbar.add(searchPanel, BorderLayout.WEST);
        toolbar.add(actionsPanel, BorderLayout.EAST);

        add(toolbar, BorderLayout.NORTH);
    }

    private void initTable() {
        String[] cols = {"Código", "Ciudad", "País", "Región", "Cód. Postal", "Teléfono", "Dirección Principal", "Dirección Adicional"};
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
            cachedOficinas = service.listarTodas();
            filtrarEnTiempoReal();
        } catch (Exception e) {
            UiHelper.showError(this, "Error al listar oficinas: " + e.getMessage());
        }
    }

    private void filtrarEnTiempoReal() {
        if (cachedOficinas == null) return;
        String query = txtSearch.getText().trim().toLowerCase();

        tableModel.setRowCount(0);
        for (Oficina o : cachedOficinas) {
            String cod = o.getCodigoOficina().toLowerCase();
            String ciu = o.getCiudad().toLowerCase();
            String pai = o.getPais().toLowerCase();

            if (query.isEmpty() || cod.contains(query) || ciu.contains(query) || pai.contains(query)) {
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
        }
    }

    private void editarSeleccionada() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiHelper.showError(this, "Por favor seleccione una oficina de la tabla para editar");
            return;
        }
        String id = (String) tableModel.getValueAt(row, 0);
        try {
            Oficina oficina = service.obtenerPorId(id);
            mostrarDialogoFormulario(oficina);
        } catch (Exception e) {
            UiHelper.showError(this, "Error al cargar datos de la oficina: " + e.getMessage());
        }
    }

    private void eliminarSeleccionada() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiHelper.showError(this, "Por favor seleccione una oficina de la tabla para eliminar");
            return;
        }
        String id = (String) tableModel.getValueAt(row, 0);
        if (UiHelper.showConfirm(this, "¿Está seguro de eliminar la oficina con código " + id + "?", "Confirmar Eliminación")) {
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
                esEdicion ? "Editar Oficina" : "Registrar Nueva Oficina", true);
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

        form.add(new JLabel("Código de Oficina:"));
        form.add(txtCodigo);
        form.add(new JLabel("Ciudad:"));
        form.add(txtCiudad);
        form.add(new JLabel("País:"));
        form.add(txtPais);
        form.add(new JLabel("Región / Comunidad:"));
        form.add(txtRegion);
        form.add(new JLabel("Código Postal:"));
        form.add(txtCp);
        form.add(new JLabel("Teléfono:"));
        form.add(txtTel);
        form.add(new JLabel("Dirección Principal (Línea 1):"));
        form.add(txtDir1);
        form.add(new JLabel("Dirección Adicional (Línea 2):"));
        form.add(txtDir2);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnGuardar = UiHelper.createButton("Guardar Oficina", "", UiHelper.COLOR_PRIMARY);
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
