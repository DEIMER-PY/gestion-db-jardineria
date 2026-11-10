package com.jardineria.infrastructure.adapter.in.ui.views;

import com.jardineria.application.service.EmpleadoService;
import com.jardineria.domain.model.Empleado;
import com.jardineria.infrastructure.adapter.in.ui.components.UiHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Vista de administración y CRUD para Empleados.
 */
public class EmpleadoView extends JPanel {
    private final EmpleadoService service;
    private DefaultTableModel tableModel;
    private JTable table;
    private JComboBox<String> cbFilter;

    public EmpleadoView(EmpleadoService service) {
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

        filterPanel.add(new JLabel("Filtro:"));
        cbFilter = new JComboBox<>(new String[]{"Todos los Empleados", "Solo Directores", "Solo Representantes de Ventas"});
        cbFilter.addActionListener(e -> aplicarFiltro());
        filterPanel.add(cbFilter);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsPanel.setOpaque(false);

        JButton btnNew = UiHelper.createButton("Nuevo Empleado", "➕", UiHelper.COLOR_PRIMARY);
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

        toolbar.add(filterPanel, BorderLayout.WEST);
        toolbar.add(actionsPanel, BorderLayout.EAST);

        add(toolbar, BorderLayout.NORTH);
    }

    private void initTable() {
        String[] cols = {"Código", "Nombre", "Primer Apellido", "Segundo Apellido", "Puesto", "Ext.", "Email", "Oficina", "Cód. Jefe"};
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
            List<Empleado> list = service.listarTodos();
            for (Empleado emp : list) {
                tableModel.addRow(new Object[]{
                        emp.getCodigoEmpleado(),
                        emp.getNombre(),
                        emp.getApellido1(),
                        emp.getApellido2() != null ? emp.getApellido2() : "",
                        emp.getPuesto(),
                        emp.getExtension(),
                        emp.getEmail(),
                        emp.getCodigoOficina(),
                        emp.getCodigoJefe() != null ? emp.getCodigoJefe() : "Ninguno (Director)"
                });
            }
        } catch (Exception e) {
            UiHelper.showError(this, "Error al listar empleados: " + e.getMessage());
        }
    }

    private void aplicarFiltro() {
        int idx = cbFilter.getSelectedIndex();
        try {
            tableModel.setRowCount(0);
            List<Empleado> list;
            if (idx == 1) {
                list = service.listarDirectores();
            } else if (idx == 2) {
                list = service.listarRepresentantesVentas();
            } else {
                list = service.listarTodos();
            }
            for (Empleado emp : list) {
                tableModel.addRow(new Object[]{
                        emp.getCodigoEmpleado(),
                        emp.getNombre(),
                        emp.getApellido1(),
                        emp.getApellido2() != null ? emp.getApellido2() : "",
                        emp.getPuesto(),
                        emp.getExtension(),
                        emp.getEmail(),
                        emp.getCodigoOficina(),
                        emp.getCodigoJefe() != null ? emp.getCodigoJefe() : "Ninguno"
                });
            }
        } catch (Exception e) {
            UiHelper.showError(this, "Error al aplicar filtro: " + e.getMessage());
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
        if (UiHelper.showConfirm(this, "¿Está seguro de eliminar al empleado con código " + id + "?", "Confirmar Eliminación")) {
            try {
                service.eliminarEmpleado(id);
                UiHelper.showSuccess(this, "Empleado eliminado");
                refreshTable();
            } catch (Exception e) {
                UiHelper.showError(this, "Error al eliminar empleado: " + e.getMessage());
            }
        }
    }

    private void mostrarDialogoFormulario(Empleado empEdit) {
        boolean esEdicion = (empEdit != null);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                esEdicion ? "Editar Empleado" : "Nuevo Empleado", true);
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

        form.add(new JLabel("Código Empleado:"));
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
        form.add(new JLabel("Código Oficina:"));
        form.add(txtOficina);
        form.add(new JLabel("Código Jefe (opcional):"));
        form.add(txtJefe);
        form.add(new JLabel("Puesto de Trabajo:"));
        form.add(txtPuesto);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnGuardar = UiHelper.createButton("Guardar", "💾", UiHelper.COLOR_PRIMARY);
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
