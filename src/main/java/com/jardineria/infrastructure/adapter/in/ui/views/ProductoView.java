package com.jardineria.infrastructure.adapter.in.ui.views;

import com.jardineria.application.service.ProductoService;
import com.jardineria.domain.model.Producto;
import com.jardineria.infrastructure.adapter.in.ui.components.UiHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Vista de catálogo de productos, control de stock crítico y precios.
 */
public class ProductoView extends JPanel {
    private final ProductoService service;
    private DefaultTableModel tableModel;
    private JTable table;
    private JComboBox<String> cbGama;
    private JTextField txtSearch;
    private JCheckBox chkBajoStock;

    public ProductoView(ProductoService service) {
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

        filterPanel.add(new JLabel("Gama:"));
        cbGama = new JComboBox<>(new String[]{"Todas", "Aromáticas", "Frutales", "Herbaceas", "Herramientas", "Ornamentales"});
        cbGama.addActionListener(e -> filtrar());
        filterPanel.add(cbGama);

        chkBajoStock = new JCheckBox("⚠️ Solo bajo stock (< 10)");
        chkBajoStock.addActionListener(e -> filtrar());
        filterPanel.add(chkBajoStock);

        filterPanel.add(new JLabel("Buscar:"));
        txtSearch = new JTextField(10);
        JButton btnBuscar = UiHelper.createButton("Buscar", null, UiHelper.COLOR_SECONDARY);
        btnBuscar.addActionListener(e -> filtrar());
        filterPanel.add(txtSearch);
        filterPanel.add(btnBuscar);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsPanel.setOpaque(false);

        JButton btnNew = UiHelper.createButton("Nuevo Producto", "➕", UiHelper.COLOR_PRIMARY);
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
        String[] cols = {"Código", "Nombre", "Gama", "Proveedor", "Stock", "Precio Venta", "Precio Prov."};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = UiHelper.createStyledTable(tableModel);

        // Renderizado especial para alertar stock bajo
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof Integer stock && stock <= 10) {
                    c.setForeground(UiHelper.COLOR_DANGER);
                    setFont(getFont().deriveFont(Font.BOLD));
                    setText(stock + " ⚠️ BAJO");
                } else {
                    c.setForeground(table.getForeground());
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void refreshTable() {
        try {
            tableModel.setRowCount(0);
            List<Producto> list = service.listarTodos();
            cargarEnTabla(list);
        } catch (Exception e) {
            UiHelper.showError(this, "Error al listar productos: " + e.getMessage());
        }
    }

    private void filtrar() {
        try {
            tableModel.setRowCount(0);
            List<Producto> list;
            if (chkBajoStock.isSelected()) {
                list = service.buscarBajoStock(10);
            } else {
                String texto = txtSearch.getText().trim();
                String gama = (String) cbGama.getSelectedItem();
                if (!texto.isEmpty()) {
                    list = service.buscarPorNombre(texto);
                } else if (gama != null && !gama.equals("Todas")) {
                    list = service.buscarPorGama(gama);
                } else {
                    list = service.listarTodos();
                }
            }
            cargarEnTabla(list);
        } catch (Exception e) {
            UiHelper.showError(this, "Error al filtrar: " + e.getMessage());
        }
    }

    private void cargarEnTabla(List<Producto> list) {
        for (Producto p : list) {
            tableModel.addRow(new Object[]{
                    p.getCodigoProducto(),
                    p.getNombre(),
                    p.getGama(),
                    p.getProveedor(),
                    p.getCantidadEnStock(),
                    UiHelper.formatCurrency(p.getPrecioVenta()),
                    UiHelper.formatCurrency(p.getPrecioProveedor())
            });
        }
    }

    private void editarSeleccionado() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiHelper.showError(this, "Seleccione un producto");
            return;
        }
        String id = (String) tableModel.getValueAt(row, 0);
        try {
            Producto p = service.obtenerPorId(id);
            mostrarDialogoFormulario(p);
        } catch (Exception e) {
            UiHelper.showError(this, "Error: " + e.getMessage());
        }
    }

    private void eliminarSeleccionado() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiHelper.showError(this, "Seleccione un producto para eliminar");
            return;
        }
        String id = (String) tableModel.getValueAt(row, 0);
        if (UiHelper.showConfirm(this, "¿Eliminar producto " + id + "?", "Confirmar")) {
            try {
                service.eliminarProducto(id);
                UiHelper.showSuccess(this, "Producto eliminado");
                refreshTable();
            } catch (Exception e) {
                UiHelper.showError(this, "Error: " + e.getMessage());
            }
        }
    }

    private void mostrarDialogoFormulario(Producto pEdit) {
        boolean esEdicion = (pEdit != null);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                esEdicion ? "Editar Producto" : "Nuevo Producto", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(7, 2, 8, 8));
        form.setBorder(new EmptyBorder(16, 16, 16, 16));

        JTextField txtCodigo = new JTextField(esEdicion ? pEdit.getCodigoProducto() : "");
        txtCodigo.setEnabled(!esEdicion);
        JTextField txtNombre = new JTextField(esEdicion ? pEdit.getNombre() : "");
        JComboBox<String> cbFormGama = new JComboBox<>(new String[]{"Aromáticas", "Frutales", "Herbaceas", "Herramientas", "Ornamentales"});
        if (esEdicion && pEdit.getGama() != null) {
            cbFormGama.setSelectedItem(pEdit.getGama());
        }
        JTextField txtProveedor = new JTextField(esEdicion && pEdit.getProveedor() != null ? pEdit.getProveedor() : "Viveros del Sur");
        JTextField txtStock = new JTextField(esEdicion ? String.valueOf(pEdit.getCantidadEnStock()) : "50");
        JTextField txtPrecioVenta = new JTextField(esEdicion && pEdit.getPrecioVenta() != null ? pEdit.getPrecioVenta().toString() : "19.99");
        JTextField txtPrecioProv = new JTextField(esEdicion && pEdit.getPrecioProveedor() != null ? pEdit.getPrecioProveedor().toString() : "10.00");

        form.add(new JLabel("Código Producto:"));
        form.add(txtCodigo);
        form.add(new JLabel("Nombre:"));
        form.add(txtNombre);
        form.add(new JLabel("Gama:"));
        form.add(cbFormGama);
        form.add(new JLabel("Proveedor:"));
        form.add(txtProveedor);
        form.add(new JLabel("Cantidad en Stock:"));
        form.add(txtStock);
        form.add(new JLabel("Precio de Venta (€):"));
        form.add(txtPrecioVenta);
        form.add(new JLabel("Precio Proveedor (€):"));
        form.add(txtPrecioProv);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnGuardar = UiHelper.createButton("Guardar", "💾", UiHelper.COLOR_PRIMARY);
        JButton btnCancelar = UiHelper.createButton("Cancelar", "❌", Color.GRAY);

        btnGuardar.addActionListener(e -> {
            try {
                int stock = Integer.parseInt(txtStock.getText().trim());
                BigDecimal pVenta = new BigDecimal(txtPrecioVenta.getText().trim());
                BigDecimal pProv = new BigDecimal(txtPrecioProv.getText().trim());
                Producto p = new Producto(
                        txtCodigo.getText().trim(),
                        txtNombre.getText().trim(),
                        (String) cbFormGama.getSelectedItem(),
                        "Estándar",
                        txtProveedor.getText().trim(),
                        "Descripción del producto",
                        stock,
                        pVenta,
                        pProv
                );
                if (esEdicion) {
                    service.actualizarProducto(p);
                } else {
                    service.crearProducto(p);
                }
                UiHelper.showSuccess(dialog, "Producto guardado con éxito");
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
