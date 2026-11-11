package com.jardineria.infrastructure.adapter.in.ui.views;

import com.jardineria.application.service.ProductoService;
import com.jardineria.domain.model.Producto;
import com.jardineria.infrastructure.adapter.in.ui.components.ExportHelper;
import com.jardineria.infrastructure.adapter.in.ui.components.UiHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Vista de catálogo de productos, control de stock crítico, márgenes comerciales y exportación.
 */
public class ProductoView extends JPanel {
    private final ProductoService service;
    private DefaultTableModel tableModel;
    private JTable table;
    private JComboBox<String> cbGama;
    private JTextField txtSearch;
    private JCheckBox chkBajoStock;
    private List<Producto> cachedProductos;

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
        cbGama.addActionListener(e -> filtrarEnTiempoReal());
        filterPanel.add(cbGama);

        chkBajoStock = new JCheckBox("⚠️ Solo Stock Crítico (< 10)");
        chkBajoStock.addActionListener(e -> filtrarEnTiempoReal());
        filterPanel.add(chkBajoStock);

        filterPanel.add(new JLabel("🔍 Buscar:"));
        txtSearch = new JTextField(12);
        UiHelper.styleSearchField(txtSearch, "Nombre o código...");
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

        JButton btnNew = UiHelper.createButton("Nuevo Producto", "➕", UiHelper.COLOR_PRIMARY);
        btnNew.addActionListener(e -> mostrarDialogoFormulario(null));

        JButton btnEdit = UiHelper.createButton("Editar", "✏️", UiHelper.COLOR_SECONDARY);
        btnEdit.addActionListener(e -> editarSeleccionado());

        JButton btnDelete = UiHelper.createButton("Eliminar", "🗑️", UiHelper.COLOR_DANGER);
        btnDelete.addActionListener(e -> eliminarSeleccionado());

        JButton btnExport = UiHelper.createButton("Exportar CSV", "📥", UiHelper.COLOR_PURPLE);
        btnExport.addActionListener(e -> ExportHelper.exportTableToCsv(this, table, "catalogo_productos"));

        JButton btnRefresh = UiHelper.createButton("Refrescar", "🔄", Color.DARK_GRAY);
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
        String[] cols = {"Código", "Nombre del Producto", "Gama", "Proveedor", "Nivel de Stock", "Precio de Venta", "Precio Proveedor", "Margen (€)", "Margen (%)"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = UiHelper.createStyledTable(tableModel);

        // Asignar renderizador de badge de stock para columna 4 (Nivel de Stock)
        table.getColumnModel().getColumn(4).setCellRenderer(UiHelper.createStockBadgeRenderer());

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void refreshTable() {
        try {
            cachedProductos = service.listarTodos();
            filtrarEnTiempoReal();
        } catch (Exception e) {
            UiHelper.showError(this, "Error al listar productos: " + e.getMessage());
        }
    }

    private void filtrarEnTiempoReal() {
        if (cachedProductos == null) return;
        String query = txtSearch.getText().trim().toLowerCase();
        String selectedGama = (String) cbGama.getSelectedItem();
        boolean soloCritico = chkBajoStock.isSelected();

        tableModel.setRowCount(0);
        for (Producto p : cachedProductos) {
            String cod = p.getCodigoProducto().toLowerCase();
            String nom = p.getNombre().toLowerCase();
            String gama = (p.getGama() != null) ? p.getGama() : "";
            int stock = p.getCantidadEnStock();

            if (soloCritico && stock > 10) {
                continue;
            }
            if (selectedGama != null && !selectedGama.equals("Todas") && !gama.equalsIgnoreCase(selectedGama)) {
                continue;
            }
            if (!query.isEmpty() && !cod.contains(query) && !nom.contains(query)) {
                continue;
            }

            BigDecimal pVenta = (p.getPrecioVenta() != null) ? p.getPrecioVenta() : BigDecimal.ZERO;
            BigDecimal pProv = (p.getPrecioProveedor() != null) ? p.getPrecioProveedor() : BigDecimal.ZERO;
            BigDecimal margenEur = pVenta.subtract(pProv);
            String margenPorc = "0.0%";
            if (pProv.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal porc = margenEur.divide(pProv, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
                margenPorc = String.format("%.1f%%", porc.doubleValue());
            }

            tableModel.addRow(new Object[]{
                    p.getCodigoProducto(),
                    p.getNombre(),
                    p.getGama(),
                    p.getProveedor(),
                    p.getCantidadEnStock(),
                    UiHelper.formatCurrency(pVenta),
                    UiHelper.formatCurrency(pProv),
                    UiHelper.formatCurrency(margenEur),
                    margenPorc
            });
        }
    }

    private void editarSeleccionado() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiHelper.showError(this, "Seleccione un producto de la tabla para editar");
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
        if (UiHelper.showConfirm(this, "¿Está seguro de eliminar el producto " + id + "?", "Confirmar Eliminación")) {
            try {
                service.eliminarProducto(id);
                UiHelper.showSuccess(this, "Producto eliminado exitosamente");
                refreshTable();
            } catch (Exception e) {
                UiHelper.showError(this, "Error al eliminar producto: " + e.getMessage());
            }
        }
    }

    private void mostrarDialogoFormulario(Producto pEdit) {
        boolean esEdicion = (pEdit != null);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                esEdicion ? "Editar Producto" : "Registrar Nuevo Producto", true);
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

        form.add(new JLabel("Código de Producto:"));
        form.add(txtCodigo);
        form.add(new JLabel("Nombre Comercial:"));
        form.add(txtNombre);
        form.add(new JLabel("Gama o Familia:"));
        form.add(cbFormGama);
        form.add(new JLabel("Proveedor:"));
        form.add(txtProveedor);
        form.add(new JLabel("Cantidad en Stock:"));
        form.add(txtStock);
        form.add(new JLabel("Precio de Venta al Público (€):"));
        form.add(txtPrecioVenta);
        form.add(new JLabel("Precio de Compra al Proveedor (€):"));
        form.add(txtPrecioProv);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnGuardar = UiHelper.createButton("Guardar Producto", "💾", UiHelper.COLOR_PRIMARY);
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
