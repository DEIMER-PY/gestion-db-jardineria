package com.jardineria.infrastructure.adapter.in.ui.views;

import com.jardineria.application.service.ClienteService;
import com.jardineria.domain.model.Cliente;
import com.jardineria.infrastructure.adapter.in.ui.components.UiHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Vista de administración y CRUD para Clientes.
 */
public class ClienteView extends JPanel {
    private final ClienteService service;
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField txtSearch;

    public ClienteView(ClienteService service) {
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

        searchPanel.add(new JLabel("🔍 Buscar por País/Ciudad:"));
        txtSearch = new JTextField(15);
        JButton btnSearch = UiHelper.createButton("Filtrar", null, UiHelper.COLOR_SECONDARY);
        btnSearch.addActionListener(e -> filtrar());

        JButton btnClear = UiHelper.createButton("Limpiar", null, Color.GRAY);
        btnClear.addActionListener(e -> {
            txtSearch.setText("");
            refreshTable();
        });

        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnClear);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsPanel.setOpaque(false);

        JButton btnNew = UiHelper.createButton("Nuevo Cliente", "➕", UiHelper.COLOR_PRIMARY);
        btnNew.addActionListener(e -> mostrarDialogoFormulario(null));

        JButton btnEdit = UiHelper.createButton("Editar", "✏️", UiHelper.COLOR_SECONDARY);
        btnEdit.addActionListener(e -> editarSeleccionado());

        JButton btnDelete = UiHelper.createButton("Eliminar", "🗑️", UiHelper.COLOR_DANGER);
        btnDelete.addActionListener(e -> eliminarSeleccionado());

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
        String[] cols = {"Código", "Nombre Cliente", "Contacto", "Teléfono", "Ciudad", "País", "Límite Crédito", "Rep. Ventas"};
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
            List<Cliente> list = service.listarTodos();
            for (Cliente c : list) {
                tableModel.addRow(new Object[]{
                        c.getCodigoCliente(),
                        c.getNombreCliente(),
                        c.getContactoCompleto(),
                        c.getTelefono(),
                        c.getCiudad(),
                        c.getPais(),
                        UiHelper.formatCurrency(c.getLimiteCredito()),
                        c.getCodigoEmpleadoRepVentas() != null ? c.getCodigoEmpleadoRepVentas() : "Ninguno"
                });
            }
        } catch (Exception e) {
            UiHelper.showError(this, "Error al listar clientes: " + e.getMessage());
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
            List<Cliente> list = service.buscarPorPais(texto);
            if (list.isEmpty()) {
                list = service.buscarPorCiudad(texto);
            }
            for (Cliente c : list) {
                tableModel.addRow(new Object[]{
                        c.getCodigoCliente(),
                        c.getNombreCliente(),
                        c.getContactoCompleto(),
                        c.getTelefono(),
                        c.getCiudad(),
                        c.getPais(),
                        UiHelper.formatCurrency(c.getLimiteCredito()),
                        c.getCodigoEmpleadoRepVentas()
                });
            }
        } catch (Exception e) {
            UiHelper.showError(this, "Error al filtrar clientes: " + e.getMessage());
        }
    }

    private void editarSeleccionado() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiHelper.showError(this, "Seleccione un cliente de la tabla");
            return;
        }
        Integer id = (Integer) tableModel.getValueAt(row, 0);
        try {
            Cliente cliente = service.obtenerPorId(id);
            mostrarDialogoFormulario(cliente);
        } catch (Exception e) {
            UiHelper.showError(this, "Error: " + e.getMessage());
        }
    }

    private void eliminarSeleccionado() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiHelper.showError(this, "Seleccione un cliente para eliminar");
            return;
        }
        Integer id = (Integer) tableModel.getValueAt(row, 0);
        if (UiHelper.showConfirm(this, "¿Eliminar cliente con código " + id + "?", "Confirmar")) {
            try {
                service.eliminarCliente(id);
                UiHelper.showSuccess(this, "Cliente eliminado");
                refreshTable();
            } catch (Exception e) {
                UiHelper.showError(this, "Error: " + e.getMessage());
            }
        }
    }

    private void mostrarDialogoFormulario(Cliente cEdit) {
        boolean esEdicion = (cEdit != null);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                esEdicion ? "Editar Cliente" : "Nuevo Cliente", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(8, 2, 8, 8));
        form.setBorder(new EmptyBorder(16, 16, 16, 16));

        JTextField txtCodigo = new JTextField(esEdicion ? String.valueOf(cEdit.getCodigoCliente()) : "");
        txtCodigo.setEnabled(!esEdicion);
        JTextField txtNombre = new JTextField(esEdicion ? cEdit.getNombreCliente() : "");
        JTextField txtContactoNom = new JTextField(esEdicion && cEdit.getNombreContacto() != null ? cEdit.getNombreContacto() : "");
        JTextField txtContactoApe = new JTextField(esEdicion && cEdit.getApellidoContacto() != null ? cEdit.getApellidoContacto() : "");
        JTextField txtTel = new JTextField(esEdicion ? cEdit.getTelefono() : "");
        JTextField txtCiudad = new JTextField(esEdicion ? cEdit.getCiudad() : "");
        JTextField txtPais = new JTextField(esEdicion && cEdit.getPais() != null ? cEdit.getPais() : "España");
        JTextField txtLimite = new JTextField(esEdicion && cEdit.getLimiteCredito() != null ? cEdit.getLimiteCredito().toString() : "5000.00");

        form.add(new JLabel("Código Cliente:"));
        form.add(txtCodigo);
        form.add(new JLabel("Razón Social / Nombre:"));
        form.add(txtNombre);
        form.add(new JLabel("Nombre Contacto:"));
        form.add(txtContactoNom);
        form.add(new JLabel("Apellido Contacto:"));
        form.add(txtContactoApe);
        form.add(new JLabel("Teléfono:"));
        form.add(txtTel);
        form.add(new JLabel("Ciudad:"));
        form.add(txtCiudad);
        form.add(new JLabel("País:"));
        form.add(txtPais);
        form.add(new JLabel("Límite de Crédito:"));
        form.add(txtLimite);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnGuardar = UiHelper.createButton("Guardar", "💾", UiHelper.COLOR_PRIMARY);
        JButton btnCancelar = UiHelper.createButton("Cancelar", "❌", Color.GRAY);

        btnGuardar.addActionListener(e -> {
            try {
                int cod = Integer.parseInt(txtCodigo.getText().trim());
                BigDecimal lim = new BigDecimal(txtLimite.getText().trim());
                Cliente c = new Cliente(
                        cod,
                        txtNombre.getText().trim(),
                        txtContactoNom.getText().trim(),
                        txtContactoApe.getText().trim(),
                        txtTel.getText().trim(),
                        txtTel.getText().trim(), // fax
                        "Dirección Principal",
                        null,
                        txtCiudad.getText().trim(),
                        null,
                        txtPais.getText().trim(),
                        "00000",
                        null,
                        lim
                );
                if (esEdicion) {
                    service.actualizarCliente(c);
                } else {
                    service.crearCliente(c);
                }
                UiHelper.showSuccess(dialog, "Cliente guardado con éxito");
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
