package com.jardineria.infrastructure.adapter.in.ui.views;

import com.jardineria.application.service.DashboardService;
import com.jardineria.domain.model.Pedido;
import com.jardineria.infrastructure.adapter.in.ui.components.KpiCard;
import com.jardineria.infrastructure.adapter.in.ui.components.UiHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Vista de Dashboard principal con métricas ejecutivas, KPIs y actividad reciente.
 */
public class DashboardView extends JPanel {
    private final DashboardService dashboardService;

    private KpiCard cardClientes;
    private KpiCard cardPedidos;
    private KpiCard cardIngresos;
    private KpiCard cardStockBajo;
    private KpiCard cardEmpleados;

    private DefaultTableModel ordersTableModel;
    private JTable ordersTable;

    public DashboardView(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
        setLayout(new BorderLayout(16, 16));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        initHeader();
        initKpiGrid();
        initRecentActivity();

        refreshData();
    }

    private void initHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 2, 2));
        titlePanel.setOpaque(false);

        JLabel lblTitle = new JLabel("📊 Panel de Control y Métricas Ejecutivas");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 22));

        JLabel lblSubtitle = new JLabel("Resumen operacional en tiempo real de la base de datos Jardinería");
        lblSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(150, 155, 165));

        titlePanel.add(lblTitle);
        titlePanel.add(lblSubtitle);

        JButton btnRefresh = UiHelper.createButton("Actualizar Datos", "🔄", UiHelper.COLOR_SECONDARY);
        btnRefresh.addActionListener(e -> refreshData());

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(btnRefresh, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
    }

    private void initKpiGrid() {
        JPanel gridPanel = new JPanel(new GridLayout(1, 5, 12, 12));
        gridPanel.setOpaque(false);

        cardClientes = new KpiCard("👥", "Clientes Activos", "...", "Registrados", UiHelper.COLOR_SECONDARY);
        cardPedidos = new KpiCard("📦", "Pedidos Totales", "...", "Procesados", UiHelper.COLOR_PRIMARY);
        cardIngresos = new KpiCard("💰", "Facturación Total", "...", "Pagos confirmados", UiHelper.COLOR_PURPLE);
        cardStockBajo = new KpiCard("⚠️", "Stock Crítico", "...", "Alertas < 10 uds", UiHelper.COLOR_DANGER);
        cardEmpleados = new KpiCard("👔", "Equipo Laboral", "...", "Empleados activos", UiHelper.COLOR_WARNING);

        gridPanel.add(cardClientes);
        gridPanel.add(cardPedidos);
        gridPanel.add(cardIngresos);
        gridPanel.add(cardStockBajo);
        gridPanel.add(cardEmpleados);

        JPanel centerWrapper = new JPanel(new BorderLayout(0, 16));
        centerWrapper.setOpaque(false);
        centerWrapper.add(gridPanel, BorderLayout.NORTH);

        add(centerWrapper, BorderLayout.CENTER);
    }

    private void initRecentActivity() {
        JPanel activityPanel = new JPanel(new BorderLayout(0, 8));
        activityPanel.setOpaque(false);
        activityPanel.setBorder(BorderFactory.createTitledBorder("📦 Últimos Pedidos Registrados"));

        String[] columns = {"Código Pedido", "Fecha Pedido", "Fecha Esperada", "Fecha Entrega", "Estado", "Código Cliente"};
        ordersTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        ordersTable = UiHelper.createStyledTable(ordersTableModel);
        JScrollPane scrollPane = new JScrollPane(ordersTable);
        scrollPane.setPreferredSize(new Dimension(600, 220));

        activityPanel.add(scrollPane, BorderLayout.CENTER);

        // Añadir al panel central
        Component centerComp = ((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (centerComp instanceof JPanel centerWrapper) {
            centerWrapper.add(activityPanel, BorderLayout.CENTER);
        }
    }

    public void refreshData() {
        try {
            DashboardService.DashboardMetrics m = dashboardService.obtenerMetricas();
            cardClientes.setValue(String.valueOf(m.totalClientes()));
            cardPedidos.setValue(String.valueOf(m.totalPedidos()));
            cardPedidos.setSubtitle(String.format("✔ %d Entregados | ⏳ %d Pendientes", m.pedidosEntregados(), m.pedidosPendientes()));
            cardIngresos.setValue(UiHelper.formatCurrency(m.totalIngresos()));
            cardStockBajo.setValue(String.valueOf(m.totalBajoStock()));
            cardEmpleados.setValue(String.valueOf(m.totalEmpleados()));
            cardEmpleados.setSubtitle(String.format("En %d oficinas globales", m.totalOficinas()));

            ordersTableModel.setRowCount(0);
            List<Pedido> ultimos = dashboardService.obtenerUltimosPedidos();
            for (Pedido p : ultimos) {
                ordersTableModel.addRow(new Object[]{
                        p.getCodigoPedido(),
                        p.getFechaPedido(),
                        p.getFechaEsperada(),
                        p.getFechaEntrega() != null ? p.getFechaEntrega() : "--",
                        p.getEstado(),
                        p.getCodigoCliente()
                });
            }
        } catch (Exception e) {
            UiHelper.showError(this, "Error al actualizar métricas del dashboard: " + e.getMessage());
        }
    }
}
