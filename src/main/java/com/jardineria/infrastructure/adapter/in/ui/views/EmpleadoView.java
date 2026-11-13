package com.jardineria.infrastructure.adapter.in.ui.views;

import com.jardineria.application.service.EmpleadoService;
import com.jardineria.domain.model.Empleado;
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
 * Vista de administración y CRUD para Empleados con búsqueda en tiempo real y exportación.
 */
public class EmpleadoView extends JPanel {
    private final com.jardineria.application.port.in.EmpleadoUseCase service;
    private DefaultTableModel tableModel;
    private JTable table;
    private JComboBox<String> cbFilter;
    private JTextField txtSearch;
    private List<Empleado> cachedEmpleados;

    public EmpleadoView(com.jardineria.application.port.in.EmpleadoUseCase service) {
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

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterPanel.setOpaque(false);

        filterPanel.add(new JLabel("Rol / Cargo:"));
        cbFilter = new JComboBox<>(new String[]{"Todos los Empleados", "Solo Directores", "Solo Representantes de Ventas"});
        cbFilter.addActionListener(e -> filtrarEnTiempoReal());
        filterPanel.add(cbFilter);

        filterPanel.add(new JLabel("Buscar:"));
        txtSearch = new JTextField(14);
        UiHelper.styleSearchField(txtSearch, "Nombre, puesto o correo...");
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
        filterPanel.add(txtSearch);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsPanel.setOpaque(false);

        JButton btnNew = UiHelper.createButton("Nuevo Empleado", "➕", UiHelper.COLOR_PRIMARY);
        btnNew.addActionListener(e -> mostrarDialogoFormulario(null));

        JButton btnEdit = UiHelper.createButton("Editar", "✏️", UiHelper.COLOR_SECONDARY);
        btnEdit.addActionListener(e -> editarSeleccionado());

        JButton btnDelete = UiHelper.createButton("Eliminar", "", UiHelper.COLOR_DANGER);
        btnDelete.addActionListener(e -> eliminarSeleccionado());

        JButton btnExport = UiHelper.createButton("Exportar CSV", "", UiHelper.COLOR_PURPLE);
        btnExport.addActionListener(e -> ExportHelper.exportTableToCsv(this, table, "empleados_jardineria"));

        JButton btnRefresh = UiHelper.createButton("Refrescar", "", Color.DARK_GRAY);
        btnRefresh.addActionListener(e -> refreshTable());

        actionsPanel.add(btnNew);
        actionsPanel.add(btnEdit);
        actionsPanel.add(btnDelete);
        actionsPanel.add(btnExport);
        actionsPanel.add(btnRefresh);

        toolbar.add(filterPanel, BorderLayout.WEST);
        toolbar.add(actionsPanel, BorderLayout.EAST);

        add(toolbar, BorderLayout.NORTH);
    }

    private void initTable() {
        String[] cols = {"Código", "Nombre", "Primer Apellido", "Segundo Apellido", "Puesto Laboral", "Extensión", "Correo Electrónico", "Oficina", "Código Jefe"};
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
            cachedEmpleados = service.listarTodos();
            filtrarEnTiempoReal();
        } catch (Exception e) {
            UiHelper.showError(this, "Error al listar empleados: " + e.getMessage());
        }
    }

    private void filtrarEnTiempoReal() {
        if (cachedEmpleados == null) return;
        int roleIdx = cbFilter.getSelectedIndex();
        String query = txtSearch.getText().trim().toLowerCase();

        tableModel.setRowCount(0);
        for (Empleado emp : cachedEmpleados) {
            // Filtro por rol
            if (roleIdx == 1 && emp.getCodigoJefe() != null) {
                continue; // Solo directores
            }
            if (roleIdx == 2 && (emp.getPuesto() == null || !emp.getPuesto().toLowerCase().contains("representante"))) {
                continue; // Solo representantes
            }

            // Filtro por texto
            String full = emp.getNombreCompleto().toLowerCase();
            String puesto = (emp.getPuesto() != null) ? emp.getPuesto().toLowerCase() : "";
            String email = (emp.getEmail() != null) ? emp.getEmail().toLowerCase() : "";
            String cod = String.valueOf(emp.getCodigoEmpleado());

            if (!query.isEmpty() && !full.contains(query) && !puesto.contains(query) &&
                    !email.contains(query) && !cod.contains(query)) {
                continue;
            }

            tableModel.addRow(new Object[]{
                    emp.getCodigoEmpleado(),
                    emp.getNombre(),
                    emp.getApellido1(),
                    emp.getApellido2() != null ? emp.getApellido2() : "",
                    emp.getPuesto(),
                    emp.getExtension(),
                    emp.getEmail(),
                    emp.getCodigoOficina(),
                    emp.getCodigoJefe() != null ? "#" + emp.getCodigoJefe() : "Director General"
            });
        }
    }

    private void editarSeleccionado() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiHelper.showError(this, "Seleccione un empleado de la tabla");
            return;
        }
        Integer id = (Integer) tableModel.getValueAt(row, 0);
        try {
            Empleado emp = service.obtenerPorId(id);
            mostrarDialogoFormulario(emp);
        } catch (Exception e) {
            UiHelper.showError(this, "Error: " + e.getMessage());
        }
    }

    private void eliminarSeleccionado() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiHelper.showError(this, "Seleccione un empleado de la tabla para eliminar");
            return;
        }
        Integer id = (Integer) tableModel.getValueAt(row, 0);
        if (UiHelper.showConfirm(this, "¿Está seguro de eliminar al empleado con código #" + id + "?", "Confirmar Eliminación")) {
            try {
                service.eliminarEmpleado(id);
                UiHelper.showSuccess(this, "Empleado eliminado exitosamente");
                refreshTable();
            } catch (Exception e) {
                UiHelper.showError(this, "Error al eliminar empleado: " + e.getMessage());
            }
        }
    }

    private void mostrarDialogoFormulario(Empleado empEdit) {
        boolean esEdicion = (empEdit != null);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                esEdicion ? "Editar Datos del Empleado" : "Registrar Nuevo Empleado", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(9, 2, 8, 8));
        form.setBorder(new EmptyBorder(16, 16, 16, 16));

        JTextField txtCodigo = new JTextField(esEdicion ? String.valueOf(empEdit.getCodigoEmpleado()) : "");
        txtCodigo.setEnabled(!esEdicion);
        JTextField txtNombre = new JTextField(esEdicion ? empEdit.getNombre() : "");
        JTextField txtAp1 = new JTextField(esEdicion ? empEdit.getApellido1() : "");
        JTextField txtAp2 = new JTextField(esEdicion && empEdit.getApellido2() != null ? empEdit.getApellido2() : "");
        JTextField txtExt = new JTextField(esEdicion ? empEdit.getExtension() : "");
        JTextField txtEmail = new JTextField(esEdicion ? empEdit.getEmail() : "");
        JTextField txtOficina = new JTextField(esEdicion ? empEdit.getCodigoOficina() : "TAL-ES");
        JTextField txtJefe = new JTextField(esEdicion && empEdit.getCodigoJefe() != null ? String.valueOf(empEdit.getCodigoJefe()) : "");
        JTextField txtPuesto = new JTextField(esEdicion && empEdit.getPuesto() != null ? empEdit.getPuesto() : "");

        form.add(new JLabel("Código de Empleado:"));
        form.add(txtCodigo);
        form.add(new JLabel("Nombre:"));
        form.add(txtNombre);
        form.add(new JLabel("Primer Apellido:"));
        form.add(txtAp1);
        form.add(new JLabel("Segundo Apellido:"));
        form.add(txtAp2);
        form.add(new JLabel("Extensión Telefónica:"));
        form.add(txtExt);
        form.add(new JLabel("Correo Electrónico:"));
        form.add(txtEmail);
        form.add(new JLabel("Código de Oficina:"));
        form.add(txtOficina);
        form.add(new JLabel("Código Jefe Superior (opcional):"));
        form.add(txtJefe);
        form.add(new JLabel("Puesto / Cargo Laboral:"));
        form.add(txtPuesto);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnGuardar = UiHelper.createButton("Guardar Empleado", "", UiHelper.COLOR_PRIMARY);
        JButton btnCancelar = UiHelper.createButton("Cancelar", "❌", Color.GRAY);

        btnGuardar.addActionListener(e -> {
            try {
                int cod = Integer.parseInt(txtCodigo.getText().trim());
                Integer jefe = null;
                if (!txtJefe.getText().trim().isEmpty()) {
                    jefe = Integer.parseInt(txtJefe.getText().trim());
                }
                Empleado emp = new Empleado(
                        cod,
                        txtNombre.getText().trim(),
                        txtAp1.getText().trim(),
                        txtAp2.getText().trim(),
                        txtExt.getText().trim(),
                        txtEmail.getText().trim(),
                        txtOficina.getText().trim(),
                        jefe,
                        txtPuesto.getText().trim()
                );
                if (esEdicion) {
                    service.actualizarEmpleado(emp);
                } else {
                    service.crearEmpleado(emp);
                }
                UiHelper.showSuccess(dialog, "Empleado guardado correctamente");
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
