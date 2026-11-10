package com.jardineria.infrastructure.adapter.in.ui.views;

import com.jardineria.application.service.PagoService;
import com.jardineria.domain.model.Pago;
import com.jardineria.domain.model.PagoId;
import com.jardineria.infrastructure.adapter.in.ui.components.UiHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Vista de administración de pagos y conciliación financiera.
 */
public class PagoView extends JPanel {
    private final PagoService service;
    private DefaultTableModel tableModel;
    private JTable table;
    private JComboBox<String> cbFormaPago;
    private JLabel lblTotalIngresos;

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
        cbFormaPago.addActionListener(e -> filtrar());
        filterPanel.add(cbFormaPago);

        lblTotalIngresos = new JLabel("💰 Recaudación Total: 0,00 €");
        lblTotalIngresos.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblTotalIngresos.setForeground(UiHelper.COLOR_PRIMARY);
        filterPanel.add(Box.createHorizontalStrut(15));
        filterPanel.add(lblTotalIngresos);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsPanel.setOpaque(false);

        JButton btnNew = UiHelper.createButton("Registrar Pago", "➕", UiHelper.COLOR_PRIMARY);
        btnNew.addActionListener(e -> mostrarDialogoNuevoPago());

        JButton btnDelete = UiHelper.createButton("Eliminar", "🗑️", UiHelper.COLOR_DANGER);
        btnDelete.addActionListener(e -> eliminarSeleccionado());

        JButton btnRefresh = UiHelper.createButton("Refrescar", "🔄", Color.DARK_GRAY);
        btnRefresh.addActionListener(e -> refreshTable());

        actionsPanel.add(btnNew);
        actionsPanel.add(btnDelete);
        actionsPanel.add(btnRefresh);

        toolbar.add(filterPanel, BorderLayout.WEST);
        toolbar.add(actionsPanel, BorderLayout.EAST);

        add(toolbar, BorderLayout.NORTH);
    }

    private void initTable() {
        String[] cols = {"Código Cliente", "Forma de Pago", "ID Transacción", "Fecha Pago", "Importe"};
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
            tableModel.setRowCount(0);
            List<Pago> list = service.listarTodos();
            for (Pago p : list) {
                tableModel.addRow(new Object[]{
                        p.getCodigoCliente(),
                        p.getFormaPago(),
                        p.getIdTransaccion(),
                        p.getFechaPago(),
                        UiHelper.formatCurrency(p.getTotal())
                });
            }
            BigDecimal total = service.calcularTotalIngresos();
            lblTotalIngresos.setText("💰 Recaudación Total: " + UiHelper.formatCurrency(total));
        } catch (Exception e) {
            UiHelper.showError(this, "Error al listar pagos: " + e.getMessage());
        }
    }

    private void filtrar() {
        String forma = (String) cbFormaPago.getSelectedItem();
        try {
            tableModel.setRowCount(0);
            List<Pago> list;
            if (forma != null && !forma.equals("Todos")) {
                list = service.buscarPorFormaPago(forma);
            } else {
                list = service.listarTodos();
            }
            for (Pago p : list) {
                tableModel.addRow(new Object[]{
                        p.getCodigoCliente(),
                        p.getFormaPago(),
                        p.getIdTransaccion(),
                        p.getFechaPago(),
                        UiHelper.formatCurrency(p.getTotal())
                });
            }
        } catch (Exception e) {
            UiHelper.showError(this, "Error al filtrar pagos: " + e.getMessage());
        }
    }

    private void eliminarSeleccionado() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiHelper.showError(this, "Seleccione un pago para eliminar");
            return;
        }
        Integer cliente = (Integer) tableModel.getValueAt(row, 0);
        String tx = (String) tableModel.getValueAt(row, 2);
        PagoId id = new PagoId(cliente, tx);
        if (UiHelper.showConfirm(this, "¿Eliminar pago de transacción " + tx + "?", "Confirmar")) {
            try {
                service.eliminarPago(id);
                UiHelper.showSuccess(this, "Pago eliminado");
                refreshTable();
            } catch (Exception e) {
                UiHelper.showError(this, "Error: " + e.getMessage());
            }
        }
    }

    private void mostrarDialogoNuevoPago() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Registrar Pago", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        form.setBorder(new EmptyBorder(16, 16, 16, 16));

        JTextField txtCliente = new JTextField("1");
        JComboBox<String> cbForma = new JComboBox<>(new String[]{"PayPal", "Transferencia", "Tarjeta", "Cheque"});
        JTextField txtTx = new JTextField("ak-std-" + System.currentTimeMillis() % 1000000);
        JTextField txtFecha = new JTextField(LocalDate.now().toString());
        JTextField txtTotal = new JTextField("100.00");

        form.add(new JLabel("Código Cliente:"));
        form.add(txtCliente);
        form.add(new JLabel("Forma de Pago:"));
        form.add(cbForma);
        form.add(new JLabel("ID Transacción:"));
        form.add(txtTx);
        form.add(new JLabel("Fecha Pago (AAAA-MM-DD):"));
        form.add(txtFecha);
        form.add(new JLabel("Importe Total (€):"));
        form.add(txtTotal);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnGuardar = UiHelper.createButton("Registrar", "💾", UiHelper.COLOR_PRIMARY);
        JButton btnCancelar = UiHelper.createButton("Cancelar", "❌", Color.GRAY);

        btnGuardar.addActionListener(e -> {
            try {
                int codCli = Integer.parseInt(txtCliente.getText().trim());
                BigDecimal tot = new BigDecimal(txtTotal.getText().trim());
                LocalDate f = LocalDate.parse(txtFecha.getText().trim());
                Pago p = new Pago(codCli, (String) cbForma.getSelectedItem(), txtTx.getText().trim(), f, tot);
                service.registrarPago(p);
                UiHelper.showSuccess(dialog, "Pago registrado con éxito");
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
