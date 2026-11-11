package com.jardineria.infrastructure.adapter.in.ui.views;

import com.jardineria.application.service.ClienteService;
import com.jardineria.domain.model.Cliente;
import com.jardineria.infrastructure.adapter.in.ui.components.ExportHelper;
import com.jardineria.infrastructure.adapter.in.ui.components.UiHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Vista de administración y CRUD para Clientes con búsqueda en tiempo real y exportación.
 */
public class ClienteView extends JPanel {
    private final ClienteService service;
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField txtSearch;
    private List<Cliente> cachedClientes;

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

        JLabel lblSearch = new JLabel("Buscar cliente:");
        lblSearch.setFont(new Font("SansSerif", Font.BOLD, 12));
        txtSearch = new JTextField(18);
        UiHelper.styleSearchField(txtSearch, "Nombre, contacto, ciudad o país...");
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

        JButton btnNew = UiHelper.createButton("Nuevo Cliente", "➕", UiHelper.COLOR_PRIMARY);
        btnNew.addActionListener(e -> mostrarDialogoFormulario(null));

        JButton btnEdit = UiHelper.createButton("Editar", "✏️", UiHelper.COLOR_SECONDARY);
        btnEdit.addActionListener(e -> editarSeleccionado());

        JButton btnDelete = UiHelper.createButton("Eliminar", "", UiHelper.COLOR_DANGER);
        btnDelete.addActionListener(e -> eliminarSeleccionado());

        JButton btnExport = UiHelper.createButton("Exportar CSV", "", UiHelper.COLOR_PURPLE);
        btnExport.addActionListener(e -> ExportHelper.exportTableToCsv(this, table, "clientes_jardineria"));

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
        String[] cols = {"Código", "Razón Social / Nombre", "Contacto Principal", "Teléfono", "Ciudad", "País", "Límite de Crédito", "Rep. Ventas"};
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
            cachedClientes = service.listarTodos();
            filtrarEnTiempoReal();
        } catch (Exception e) {
            UiHelper.showError(this, "Error al listar clientes: " + e.getMessage());
        }
    }

    private void filtrarEnTiempoReal() {
        if (cachedClientes == null) return;
        String query = txtSearch.getText().trim().toLowerCase();

        tableModel.setRowCount(0);
        for (Cliente c : cachedClientes) {
            String nom = (c.getNombreCliente() != null) ? c.getNombreCliente().toLowerCase() : "";
            String contacto = (c.getContactoCompleto() != null) ? c.getContactoCompleto().toLowerCase() : "";
            String ciudad = (c.getCiudad() != null) ? c.getCiudad().toLowerCase() : "";
            String pais = (c.getPais() != null) ? c.getPais().toLowerCase() : "";
            String cod = String.valueOf(c.getCodigoCliente());

            if (query.isEmpty() || nom.contains(query) || contacto.contains(query) ||
                    ciudad.contains(query) || pais.contains(query) || cod.contains(query)) {
                tableModel.addRow(new Object[]{
                        c.getCodigoCliente(),
                        c.getNombreCliente(),
                        c.getContactoCompleto(),
                        c.getTelefono(),
                        c.getCiudad(),
                        c.getPais(),
                        UiHelper.formatCurrency(c.getLimiteCredito()),
                        c.getCodigoEmpleadoRepVentas() != null ? "#" + c.getCodigoEmpleadoRepVentas() : "Sin asignar"
                });
            }
        }
    }

    private void editarSeleccionado() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiHelper.showError(this, "Seleccione un cliente de la tabla para editar");
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
        if (UiHelper.showConfirm(this, "¿Está seguro de eliminar al cliente con código #" + id + "?", "Confirmar Eliminación")) {
            try {
                service.eliminarCliente(id);
                UiHelper.showSuccess(this, "Cliente eliminado exitosamente");
                refreshTable();
            } catch (Exception e) {
                UiHelper.showError(this, "Error al eliminar cliente: " + e.getMessage());
            }
        }
    }

    private void mostrarDialogoFormulario(Cliente cEdit) {
        boolean esEdicion = (cEdit != null);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                esEdicion ? "Editar Datos del Cliente" : "Registrar Nuevo Cliente", true);
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
        JTextField txtLimite = new JTextField(esEdicion && cEdit.getLimiteCredito() != null ? cEdit.getLimiteCredito().toString() : "6000.00");

        form.add(new JLabel("Código de Cliente:"));
        form.add(txtCodigo);
        form.add(new JLabel("Razón Social / Nombre Comercial:"));
        form.add(txtNombre);
        form.add(new JLabel("Nombre de Contacto:"));
        form.add(txtContactoNom);
        form.add(new JLabel("Apellido de Contacto:"));
        form.add(txtContactoApe);
        form.add(new JLabel("Teléfono de Contacto:"));
        form.add(txtTel);
        form.add(new JLabel("Ciudad:"));
        form.add(txtCiudad);
        form.add(new JLabel("País:"));
        form.add(txtPais);
        form.add(new JLabel("Límite de Crédito (€):"));
        form.add(txtLimite);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnGuardar = UiHelper.createButton("Guardar Cliente", "", UiHelper.COLOR_PRIMARY);
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
                        txtTel.getText().trim(),
                        "Dirección Principal",
                        null,
                        txtCiudad.getText().trim(),
                        null,
                        txtPais.getText().trim(),
                        "28001",
                        null,
                        lim
                );
                if (esEdicion) {
                    service.actualizarCliente(c);
                } else {
                    service.crearCliente(c);
                }
                UiHelper.showSuccess(dialog, "Cliente guardado exitosamente");
                dialog.dispose();
                refreshTable();
            } catch (Exception ex) {
                UiHelper.showError(dialog, "Error de validación: " + ex.getMessage());
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
