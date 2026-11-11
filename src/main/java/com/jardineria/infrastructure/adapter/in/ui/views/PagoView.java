package com.jardineria.infrastructure.adapter.in.ui.views;

import com.jardineria.application.service.PagoService;
import com.jardineria.domain.model.Pago;
import com.jardineria.domain.model.PagoId;
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
 * Vista de administración de pagos, conciliación financiera y exportación CSV.
 */
public class PagoView extends JPanel {
    private final PagoService service;
    private DefaultTableModel tableModel;
    private JTable table;
    private JComboBox<String> cbFormaPago;
    private JTextField txtSearch;
    private JLabel lblTotalIngresos;
    private List<Pago> cachedPagos;

    public PagoView(PagoService service) {
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

        filterPanel.add(new JLabel("Método de Pago:"));
        cbFormaPago = new JComboBox<>(new String[]{"Todos", "PayPal", "Transferencia", "Tarjeta", "Cheque"});
        cbFormaPago.addActionListener(e -> filtrarEnTiempoReal());
        filterPanel.add(cbFormaPago);

        filterPanel.add(new JLabel("🔍 Buscar:"));
        txtSearch = new JTextField(12);
        UiHelper.styleSearchField(txtSearch, "Cliente o transacción...");
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

        lblTotalIngresos = new JLabel("💰 Total Recaudado: 0,00 €");
        lblTotalIngresos.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblTotalIngresos.setForeground(UiHelper.COLOR_PRIMARY);
        filterPanel.add(Box.createHorizontalStrut(12));
        filterPanel.add(lblTotalIngresos);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsPanel.setOpaque(false);

        JButton btnNew = UiHelper.createButton("Registrar Pago", "➕", UiHelper.COLOR_PRIMARY);
        btnNew.addActionListener(e -> mostrarDialogoNuevoPago());

        JButton btnDelete = UiHelper.createButton("Eliminar", "🗑️", UiHelper.COLOR_DANGER);
        btnDelete.addActionListener(e -> eliminarSeleccionado());

        JButton btnExport = UiHelper.createButton("Exportar CSV", "📥", UiHelper.COLOR_PURPLE);
        btnExport.addActionListener(e -> ExportHelper.exportTableToCsv(this, table, "pagos_jardineria"));

        JButton btnRefresh = UiHelper.createButton("Refrescar", "🔄", Color.DARK_GRAY);
        btnRefresh.addActionListener(e -> refreshTable());

        actionsPanel.add(btnNew);
        actionsPanel.add(btnDelete);
        actionsPanel.add(btnExport);
        actionsPanel.add(btnRefresh);

        toolbar.add(filterPanel, BorderLayout.WEST);
        toolbar.add(actionsPanel, BorderLayout.EAST);

        add(toolbar, BorderLayout.NORTH);
    }

    private void initTable() {
        String[] cols = {"Código de Cliente", "Forma de Pago", "Identificador de Transacción", "Fecha de Pago", "Importe Abonado"};
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
            cachedPagos = service.listarTodos();
            filtrarEnTiempoReal();
            BigDecimal total = service.calcularTotalIngresos();
            lblTotalIngresos.setText("💰 Total Recaudado: " + UiHelper.formatCurrency(total));
        } catch (Exception e) {
            UiHelper.showError(this, "Error al listar pagos: " + e.getMessage());
        }
    }

    private void filtrarEnTiempoReal() {
        if (cachedPagos == null) return;
        String query = txtSearch.getText().trim().toLowerCase();
        String forma = (String) cbFormaPago.getSelectedItem();

        tableModel.setRowCount(0);
        for (Pago p : cachedPagos) {
            String fPago = (p.getFormaPago() != null) ? p.getFormaPago() : "";
            if (forma != null && !forma.equals("Todos") && !fPago.equalsIgnoreCase(forma)) {
                continue;
            }

            String cli = String.valueOf(p.getCodigoCliente());
            String tx = (p.getIdTransaccion() != null) ? p.getIdTransaccion().toLowerCase() : "";

            if (!query.isEmpty() && !cli.contains(query) && !tx.contains(query)) {
                continue;
            }

            tableModel.addRow(new Object[]{
                    p.getCodigoCliente(),
                    p.getFormaPago(),
                    p.getIdTransaccion(),
                    p.getFechaPago(),
                    UiHelper.formatCurrency(p.getTotal())
            });
        }
    }

    private void eliminarSeleccionado() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiHelper.showError(this, "Seleccione un pago de la tabla para eliminar");
            return;
        }
        Integer cliente = (Integer) tableModel.getValueAt(row, 0);
        String tx = (String) tableModel.getValueAt(row, 2);
        PagoId id = new PagoId(cliente, tx);
        if (UiHelper.showConfirm(this, "¿Está seguro de eliminar el pago de transacción " + tx + "?", "Confirmar Eliminación")) {
            try {
                service.eliminarPago(id);
                UiHelper.showSuccess(this, "Pago eliminado exitosamente");
                refreshTable();
            } catch (Exception e) {
                UiHelper.showError(this, "Error: " + e.getMessage());
            }
        }
    }

    private void mostrarDialogoNuevoPago() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Registrar Nuevo Pago", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        form.setBorder(new EmptyBorder(16, 16, 16, 16));

        JTextField txtCliente = new JTextField("1");
        JComboBox<String> cbForma = new JComboBox<>(new String[]{"PayPal", "Transferencia", "Tarjeta", "Cheque"});
        JTextField txtTx = new JTextField("ak-std-" + System.currentTimeMillis() % 1000000);
        JTextField txtFecha = new JTextField(LocalDate.now().toString());
        JTextField txtTotal = new JTextField("120.00");

        form.add(new JLabel("Código de Cliente:"));
        form.add(txtCliente);
        form.add(new JLabel("Forma o Método de Pago:"));
        form.add(cbForma);
        form.add(new JLabel("ID de Transacción:"));
        form.add(txtTx);
        form.add(new JLabel("Fecha de Pago (AAAA-MM-DD):"));
        form.add(txtFecha);
        form.add(new JLabel("Importe Total (€):"));
        form.add(txtTotal);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnGuardar = UiHelper.createButton("Registrar Pago", "💾", UiHelper.COLOR_PRIMARY);
        JButton btnCancelar = UiHelper.createButton("Cancelar", "❌", Color.GRAY);

        btnGuardar.addActionListener(e -> {
            try {
                int codCli = Integer.parseInt(txtCliente.getText().trim());
                BigDecimal tot = new BigDecimal(txtTotal.getText().trim());
                LocalDate f = LocalDate.parse(txtFecha.getText().trim());
                Pago p = new Pago(codCli, (String) cbForma.getSelectedItem(), txtTx.getText().trim(), f, tot);
                service.registrarPago(p);
                UiHelper.showSuccess(dialog, "Pago registrado exitosamente");
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
