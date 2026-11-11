package com.jardineria.infrastructure.adapter.in.ui.views;

import com.jardineria.application.service.DetallePedidoService;
import com.jardineria.application.service.PedidoService;
import com.jardineria.domain.model.DetallePedido;
import com.jardineria.domain.model.Pedido;
import com.jardineria.infrastructure.adapter.in.ui.components.ExportHelper;
import com.jardineria.infrastructure.adapter.in.ui.components.UiHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Vista de administración Maestro-Detalle para Pedidos y Líneas de Pedido con exportación y filtros en tiempo real.
 */
public class PedidoView extends JPanel {
    private final PedidoService pedidoService;
    private final DetallePedidoService detalleService;

    private DefaultTableModel ordersTableModel;
    private JTable ordersTable;

    private DefaultTableModel detailsTableModel;
    private JTable detailsTable;
    private JLabel lblDetallesTitle;

    private JComboBox<String> cbEstadoFilter;
    private JTextField txtSearch;
    private List<Pedido> cachedPedidos;

    public PedidoView(PedidoService pedidoService, DetallePedidoService detalleService) {
        this.pedidoService = pedidoService;
        this.detalleService = detalleService;

        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(16, 16, 16, 16));

        initToolbar();
        initMasterDetailView();
        refreshOrders();
    }

    private void initToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(8, 8));
        toolbar.setOpaque(false);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterPanel.setOpaque(false);

        filterPanel.add(new JLabel("Estado:"));
        cbEstadoFilter = new JComboBox<>(new String[]{"Todos", "Pendiente", "Entregado", "Rechazado"});
        cbEstadoFilter.addActionListener(e -> filtrarEnTiempoReal());
        filterPanel.add(cbEstadoFilter);

        filterPanel.add(new JLabel("Buscar:"));
        txtSearch = new JTextField(12);
        UiHelper.styleSearchField(txtSearch, "Código o cliente...");
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

        JButton btnNew = UiHelper.createButton("Nuevo Pedido", "➕", UiHelper.COLOR_PRIMARY);
        btnNew.addActionListener(e -> mostrarDialogoNuevoPedido());

        JButton btnEntregar = UiHelper.createButton("Marcar Entregado", "✅", UiHelper.COLOR_PRIMARY);
        btnEntregar.addActionListener(e -> cambiarEstadoSeleccionado("Entregado"));

        JButton btnRechazar = UiHelper.createButton("Rechazar", "", UiHelper.COLOR_DANGER);
        btnRechazar.addActionListener(e -> cambiarEstadoSeleccionado("Rechazado"));

        JButton btnDelete = UiHelper.createButton("Eliminar", "", Color.DARK_GRAY);
        btnDelete.addActionListener(e -> eliminarSeleccionado());

        JButton btnExport = UiHelper.createButton("Exportar CSV", "", UiHelper.COLOR_PURPLE);
        btnExport.addActionListener(e -> ExportHelper.exportTableToCsv(this, ordersTable, "pedidos_jardineria"));

        JButton btnRefresh = UiHelper.createButton("Refrescar", "", Color.GRAY);
        btnRefresh.addActionListener(e -> refreshOrders());

        actionsPanel.add(btnNew);
        actionsPanel.add(btnEntregar);
        actionsPanel.add(btnRechazar);
        actionsPanel.add(btnDelete);
        actionsPanel.add(btnExport);
        actionsPanel.add(btnRefresh);

        toolbar.add(filterPanel, BorderLayout.WEST);
        toolbar.add(actionsPanel, BorderLayout.EAST);

        add(toolbar, BorderLayout.NORTH);
    }

    private void initMasterDetailView() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.55);
        splitPane.setContinuousLayout(true);

        // Panel Superior: Pedidos (Maestro)
        JPanel masterPanel = new JPanel(new BorderLayout(0, 4));
        masterPanel.setOpaque(false);
        masterPanel.add(new JLabel("Listado General de Pedidos:"), BorderLayout.NORTH);

        String[] orderCols = {"Código Pedido", "Fecha de Pedido", "Fecha Esperada", "Fecha de Entrega", "Estado Operativo", "Código de Cliente"};
        ordersTableModel = new DefaultTableModel(orderCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        ordersTable = UiHelper.createStyledTable(ordersTableModel);
        ordersTable.getColumnModel().getColumn(4).setCellRenderer(UiHelper.createStatusBadgeRenderer());

        ordersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarDetallesPedidoSeleccionado();
            }
        });
        masterPanel.add(new JScrollPane(ordersTable), BorderLayout.CENTER);

        // Panel Inferior: Detalles de Pedido
        JPanel detailPanel = new JPanel(new BorderLayout(0, 4));
        detailPanel.setOpaque(false);

        JPanel detailHeader = new JPanel(new BorderLayout());
        detailHeader.setOpaque(false);
        lblDetallesTitle = new JLabel("Líneas del Pedido (Seleccione un pedido para ver los productos)");
        lblDetallesTitle.setFont(new Font("SansSerif", Font.BOLD, 12));

        JPanel detailActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        detailActions.setOpaque(false);

        JButton btnExportDetalles = UiHelper.createButton("Exportar Líneas CSV", "", UiHelper.COLOR_SECONDARY);
        btnExportDetalles.addActionListener(e -> ExportHelper.exportTableToCsv(this, detailsTable, "lineas_pedido"));

        JButton btnAddDetail = UiHelper.createButton("Agregar Producto a Pedido", "➕", UiHelper.COLOR_PRIMARY);
        btnAddDetail.addActionListener(e -> mostrarDialogoAgregarLinea());

        detailActions.add(btnExportDetalles);
        detailActions.add(btnAddDetail);

        detailHeader.add(lblDetallesTitle, BorderLayout.WEST);
        detailHeader.add(detailActions, BorderLayout.EAST);
        detailPanel.add(detailHeader, BorderLayout.NORTH);

        String[] detailCols = {"Número de Línea", "Código de Producto", "Cantidad", "Precio Unitario", "Subtotal"};
        detailsTableModel = new DefaultTableModel(detailCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        detailsTable = UiHelper.createStyledTable(detailsTableModel);
        detailPanel.add(new JScrollPane(detailsTable), BorderLayout.CENTER);

        splitPane.setTopComponent(masterPanel);
        splitPane.setBottomComponent(detailPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    public void refreshOrders() {
        try {
            cachedPedidos = pedidoService.listarTodos();
            filtrarEnTiempoReal();
        } catch (Exception e) {
            UiHelper.showError(this, "Error al listar pedidos: " + e.getMessage());
        }
    }

    private void filtrarEnTiempoReal() {
        if (cachedPedidos == null) return;
        String query = txtSearch.getText().trim().toLowerCase();
        String estado = (String) cbEstadoFilter.getSelectedItem();

        ordersTableModel.setRowCount(0);
        for (Pedido p : cachedPedidos) {
            String est = (p.getEstado() != null) ? p.getEstado() : "";
            if (estado != null && !estado.equals("Todos") && !est.equalsIgnoreCase(estado)) {
                continue;
            }

            String cod = String.valueOf(p.getCodigoPedido());
            String cli = String.valueOf(p.getCodigoCliente());
            if (!query.isEmpty() && !cod.contains(query) && !cli.contains(query)) {
                continue;
            }

            ordersTableModel.addRow(new Object[]{
                    p.getCodigoPedido(),
                    p.getFechaPedido(),
                    p.getFechaEsperada(),
                    p.getFechaEntrega() != null ? p.getFechaEntrega() : "--",
                    p.getEstado(),
                    p.getCodigoCliente()
            });
        }
        if (ordersTable.getRowCount() > 0) {
            ordersTable.setRowSelectionInterval(0, 0);
        } else {
            detailsTableModel.setRowCount(0);
        }
    }

    private void cargarDetallesPedidoSeleccionado() {
        int row = ordersTable.getSelectedRow();
        if (row < 0) {
            detailsTableModel.setRowCount(0);
            lblDetallesTitle.setText("Líneas del Pedido: Ningún pedido seleccionado");
            return;
        }
        Integer idPedido = (Integer) ordersTableModel.getValueAt(row, 0);
        lblDetallesTitle.setText("Líneas del Pedido #" + idPedido + ":");
        try {
            detailsTableModel.setRowCount(0);
            List<DetallePedido> detalles = detalleService.listarPorPedido(idPedido);
            BigDecimal totalAcumulado = BigDecimal.ZERO;
            for (DetallePedido d : detalles) {
                totalAcumulado = totalAcumulado.add(d.getSubtotal());
                detailsTableModel.addRow(new Object[]{
                        d.getNumeroLinea(),
                        d.getCodigoProducto(),
                        d.getCantidad(),
                        UiHelper.formatCurrency(d.getPrecioUnidad()),
                        UiHelper.formatCurrency(d.getSubtotal())
                });
            }
            lblDetallesTitle.setText(String.format("Líneas del Pedido #%d — Total Facturado: %s (%d artículos)",
                    idPedido, UiHelper.formatCurrency(totalAcumulado), detalles.size()));
        } catch (Exception e) {
            UiHelper.showError(this, "Error al cargar detalles del pedido: " + e.getMessage());
        }
    }

    private void cambiarEstadoSeleccionado(String nuevoEstado) {
        int row = ordersTable.getSelectedRow();
        if (row < 0) {
            UiHelper.showError(this, "Seleccione un pedido de la tabla");
            return;
        }
        Integer id = (Integer) ordersTableModel.getValueAt(row, 0);
        try {
            pedidoService.cambiarEstado(id, nuevoEstado, LocalDate.now());
            UiHelper.showSuccess(this, "Pedido #" + id + " actualizado a estado: " + nuevoEstado);
            refreshOrders();
        } catch (Exception e) {
            UiHelper.showError(this, "Error: " + e.getMessage());
        }
    }

    private void eliminarSeleccionado() {
        int row = ordersTable.getSelectedRow();
        if (row < 0) {
            UiHelper.showError(this, "Seleccione un pedido para eliminar");
            return;
        }
        Integer id = (Integer) ordersTableModel.getValueAt(row, 0);
        if (UiHelper.showConfirm(this, "¿Está seguro de eliminar el pedido #" + id + " y todas sus líneas asociadas?", "Confirmar Eliminación")) {
            try {
                pedidoService.eliminarPedido(id);
                UiHelper.showSuccess(this, "Pedido eliminado exitosamente");
                refreshOrders();
            } catch (Exception e) {
                UiHelper.showError(this, "Error: " + e.getMessage());
            }
        }
    }

    private void mostrarDialogoNuevoPedido() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Registrar Nuevo Pedido", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        form.setBorder(new EmptyBorder(16, 16, 16, 16));

        JTextField txtCodigo = new JTextField("999");
        JTextField txtCliente = new JTextField("1");
        JTextField txtFechaPed = new JTextField(LocalDate.now().toString());
        JTextField txtFechaEsp = new JTextField(LocalDate.now().plusDays(7).toString());
        JComboBox<String> cbEstado = new JComboBox<>(new String[]{"Pendiente", "Entregado", "Rechazado"});

        form.add(new JLabel("Código de Pedido:"));
        form.add(txtCodigo);
        form.add(new JLabel("Código de Cliente:"));
        form.add(txtCliente);
        form.add(new JLabel("Fecha de Pedido (AAAA-MM-DD):"));
        form.add(txtFechaPed);
        form.add(new JLabel("Fecha Esperada de Entrega:"));
        form.add(txtFechaEsp);
        form.add(new JLabel("Estado Inicial:"));
        form.add(cbEstado);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnGuardar = UiHelper.createButton("Crear Pedido", "", UiHelper.COLOR_PRIMARY);
        JButton btnCancelar = UiHelper.createButton("Cancelar", "❌", Color.GRAY);

        btnGuardar.addActionListener(e -> {
            try {
                int cod = Integer.parseInt(txtCodigo.getText().trim());
                int cli = Integer.parseInt(txtCliente.getText().trim());
                LocalDate fPed = LocalDate.parse(txtFechaPed.getText().trim());
                LocalDate fEsp = LocalDate.parse(txtFechaEsp.getText().trim());
                String est = (String) cbEstado.getSelectedItem();

                Pedido p = new Pedido(cod, fPed, fEsp, null, est, "Pedido creado desde interfaz gráfica", cli);
                pedidoService.crearPedido(p);
                UiHelper.showSuccess(dialog, "Pedido creado exitosamente");
                dialog.dispose();
                refreshOrders();
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

    private void mostrarDialogoAgregarLinea() {
        int row = ordersTable.getSelectedRow();
        if (row < 0) {
            UiHelper.showError(this, "Seleccione un pedido primero");
            return;
        }
        Integer idPedido = (Integer) ordersTableModel.getValueAt(row, 0);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Agregar Línea al Pedido #" + idPedido, true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.setBorder(new EmptyBorder(16, 16, 16, 16));

        JTextField txtProducto = new JTextField("AR-001");
        JTextField txtCantidad = new JTextField("1");
        JTextField txtPrecio = new JTextField("9.50");
        JTextField txtLinea = new JTextField(String.valueOf(detailsTable.getRowCount() + 1));

        form.add(new JLabel("Código de Producto:"));
        form.add(txtProducto);
        form.add(new JLabel("Cantidad Solicitada:"));
        form.add(txtCantidad);
        form.add(new JLabel("Precio Unitario (€):"));
        form.add(txtPrecio);
        form.add(new JLabel("Número de Línea:"));
        form.add(txtLinea);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnGuardar = UiHelper.createButton("Agregar a Pedido", "➕", UiHelper.COLOR_PRIMARY);
        JButton btnCancelar = UiHelper.createButton("Cancelar", "❌", Color.GRAY);

        btnGuardar.addActionListener(e -> {
            try {
                int cant = Integer.parseInt(txtCantidad.getText().trim());
                BigDecimal prec = new BigDecimal(txtPrecio.getText().trim());
                short lin = Short.parseShort(txtLinea.getText().trim());
                DetallePedido dp = new DetallePedido(idPedido, txtProducto.getText().trim(), cant, prec, lin);
                detalleService.agregarDetalle(dp);
                UiHelper.showSuccess(dialog, "Línea agregada al pedido exitosamente");
                dialog.dispose();
                cargarDetallesPedidoSeleccionado();
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
