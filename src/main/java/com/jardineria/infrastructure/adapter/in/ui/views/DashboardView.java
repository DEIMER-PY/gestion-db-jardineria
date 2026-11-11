package com.jardineria.infrastructure.adapter.in.ui.views;

import com.jardineria.application.service.DashboardService;
import com.jardineria.domain.model.Pedido;
import com.jardineria.infrastructure.adapter.in.ui.components.DistributionBar;
import com.jardineria.infrastructure.adapter.in.ui.components.ExportHelper;
import com.jardineria.infrastructure.adapter.in.ui.components.KpiCard;
import com.jardineria.infrastructure.adapter.in.ui.components.UiHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Vista de Dashboard principal con métricas ejecutivas, distribución porcentual y accesos rápidos.
 */
public class DashboardView extends JPanel {
    private final DashboardService dashboardService;
    private final Consumer<String> navigationConsumer;

    private KpiCard cardClientes;
    private KpiCard cardPedidos;
    private KpiCard cardIngresos;
    private KpiCard cardStockBajo;
    private KpiCard cardEmpleados;

    private DistributionBar orderDistributionBar;
    private JLabel lblBreakdownEntregados;
    private JLabel lblBreakdownPendientes;
    private JLabel lblBreakdownRechazados;

    private DefaultTableModel ordersTableModel;
    private JTable ordersTable;
    private JTextField txtSearchOrders;
    private List<Pedido> cachedOrders;

    public DashboardView(DashboardService dashboardService, Consumer<String> navigationConsumer) {
        this.dashboardService = dashboardService;
        this.navigationConsumer = navigationConsumer;

        setLayout(new BorderLayout(16, 16));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        initHeader();
        initCenterContent();

        refreshData();
    }

    private void initHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 2, 2));
        titlePanel.setOpaque(false);

        JLabel lblTitle = new JLabel("📊 Panel de Control y Métricas Ejecutivas");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 22));

        JLabel lblSubtitle = new JLabel("Visión general operacional en tiempo real de la base de datos Jardinería");
        lblSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(150, 155, 165));

        titlePanel.add(lblTitle);
        titlePanel.add(lblSubtitle);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionsPanel.setOpaque(false);

        JButton btnExport = UiHelper.createButton("Exportar CSV", "📥", UiHelper.COLOR_SECONDARY);
        btnExport.addActionListener(e -> ExportHelper.exportTableToCsv(this, ordersTable, "pedidos_recientes"));

        JButton btnRefresh = UiHelper.createButton("Actualizar Datos", "🔄", UiHelper.COLOR_PRIMARY);
        btnRefresh.addActionListener(e -> refreshData());

        actionsPanel.add(btnExport);
        actionsPanel.add(btnRefresh);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(actionsPanel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
    }

    private void initCenterContent() {
        JPanel centerPanel = new JPanel(new BorderLayout(0, 16));
        centerPanel.setOpaque(false);

        // 1. Grid superior de KPI Cards
        JPanel kpiGrid = new JPanel(new GridLayout(1, 5, 12, 12));
        kpiGrid.setOpaque(false);

        cardClientes = new KpiCard("👥", "Clientes Activos", "...", "Registrados", UiHelper.COLOR_SECONDARY);
        cardClientes.setClickAction(() -> navigate("CLIENTES"));

        cardPedidos = new KpiCard("📦", "Pedidos Totales", "...", "Procesados", UiHelper.COLOR_PRIMARY);
        cardPedidos.setClickAction(() -> navigate("PEDIDOS"));

        cardIngresos = new KpiCard("💰", "Facturación Total", "...", "Pagos confirmados", UiHelper.COLOR_PURPLE);
        cardIngresos.setClickAction(() -> navigate("PAGOS"));

        cardStockBajo = new KpiCard("⚠️", "Stock Crítico", "...", "Alertas < 10 uds", UiHelper.COLOR_DANGER);
        cardStockBajo.setClickAction(() -> navigate("PRODUCTOS"));

        cardEmpleados = new KpiCard("👔", "Equipo Laboral", "...", "Empleados activos", UiHelper.COLOR_WARNING);
        cardEmpleados.setClickAction(() -> navigate("EMPLEADOS"));

        kpiGrid.add(cardClientes);
        kpiGrid.add(cardPedidos);
        kpiGrid.add(cardIngresos);
        kpiGrid.add(cardStockBajo);
        kpiGrid.add(cardEmpleados);

        centerPanel.add(kpiGrid, BorderLayout.NORTH);

        // 2. Fila Media: Distribución de Pedidos + Accesos Rápidos
        JPanel middleRow = new JPanel(new GridLayout(1, 2, 16, 0));
        middleRow.setOpaque(false);

        middleRow.add(createDistributionPanel());
        middleRow.add(createQuickActionsPanel());

        JPanel middleWrapper = new JPanel(new BorderLayout(0, 16));
        middleWrapper.setOpaque(false);
        middleWrapper.add(middleRow, BorderLayout.NORTH);

        // 3. Fila Inferior: Tabla de Actividad Reciente
        middleWrapper.add(createRecentActivityPanel(), BorderLayout.CENTER);

        centerPanel.add(middleWrapper, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel createDistributionPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 10));
        panel.putClientProperty("FlatLaf.style", "arc: 12");
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("📈 Distribución y Estado de Pedidos"),
                new EmptyBorder(8, 12, 8, 12)
        ));

        orderDistributionBar = new DistributionBar();

        JPanel labelsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 4));
        labelsPanel.setOpaque(false);

        lblBreakdownEntregados = new JLabel("● Entregados: 0%");
        lblBreakdownEntregados.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblBreakdownEntregados.setForeground(UiHelper.COLOR_PRIMARY);

        lblBreakdownPendientes = new JLabel("● Pendientes: 0%");
        lblBreakdownPendientes.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblBreakdownPendientes.setForeground(UiHelper.COLOR_WARNING);

        lblBreakdownRechazados = new JLabel("● Rechazados: 0%");
        lblBreakdownRechazados.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblBreakdownRechazados.setForeground(UiHelper.COLOR_DANGER);

        labelsPanel.add(lblBreakdownEntregados);
        labelsPanel.add(lblBreakdownPendientes);
        labelsPanel.add(lblBreakdownRechazados);

        panel.add(orderDistributionBar, BorderLayout.CENTER);
        panel.add(labelsPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createQuickActionsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.putClientProperty("FlatLaf.style", "arc: 12");
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("⚡ Accesos Rápidos Operacionales"),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JButton btnPed = UiHelper.createButton("Gestionar Pedidos", "📑", UiHelper.COLOR_PRIMARY);
        btnPed.addActionListener(e -> navigate("PEDIDOS"));

        JButton btnCli = UiHelper.createButton("Catálogo Clientes", "🛍️", UiHelper.COLOR_SECONDARY);
        btnCli.addActionListener(e -> navigate("CLIENTES"));

        JButton btnProd = UiHelper.createButton("Inventario y Stock", "📦", UiHelper.COLOR_WARNING);
        btnProd.addActionListener(e -> navigate("PRODUCTOS"));

        JButton btnPago = UiHelper.createButton("Caja y Pagos", "💳", UiHelper.COLOR_PURPLE);
        btnPago.addActionListener(e -> navigate("PAGOS"));

        panel.add(btnPed);
        panel.add(btnCli);
        panel.add(btnProd);
        panel.add(btnPago);

        return panel;
    }

    private JPanel createRecentActivityPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder("📋 Últimos Pedidos Registrados"));

        // Barra de búsqueda en la tabla de pedidos
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBar.setOpaque(false);

        JLabel lblSearch = new JLabel("🔍 Buscar pedido por código o cliente:");
        lblSearch.setFont(new Font("SansSerif", Font.BOLD, 11));

        txtSearchOrders = new JTextField(18);
        UiHelper.styleSearchField(txtSearchOrders, "Escriba para filtrar...");
        txtSearchOrders.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filtrarTablaLocal();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filtrarTablaLocal();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filtrarTablaLocal();
            }
        });

        searchBar.add(lblSearch);
        searchBar.add(txtSearchOrders);

        panel.add(searchBar, BorderLayout.NORTH);

        String[] columns = {"Código Pedido", "Fecha de Pedido", "Fecha Esperada", "Fecha de Entrega", "Estado Operativo", "Código Cliente"};
        ordersTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        ordersTable = UiHelper.createStyledTable(ordersTableModel);
        // Asignar renderizador de estado para la columna 4 (Estado Operativo)
        ordersTable.getColumnModel().getColumn(4).setCellRenderer(UiHelper.createStatusBadgeRenderer());

        JScrollPane scrollPane = new JScrollPane(ordersTable);
        scrollPane.setPreferredSize(new Dimension(600, 240));

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void navigate(String module) {
        if (navigationConsumer != null) {
            navigationConsumer.accept(module);
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

            // Actualizar barra de distribución
            long total = m.totalPedidos();
            if (total > 0) {
                orderDistributionBar.setValues(m.pedidosEntregados(), m.pedidosPendientes(), m.pedidosRechazados());
                double pEnt = (m.pedidosEntregados() * 100.0) / total;
                double pPen = (m.pedidosPendientes() * 100.0) / total;
                double pRec = (m.pedidosRechazados() * 100.0) / total;

                lblBreakdownEntregados.setText(String.format("● Entregados: %.1f%% (%d)", pEnt, m.pedidosEntregados()));
                lblBreakdownPendientes.setText(String.format("● Pendientes: %.1f%% (%d)", pPen, m.pedidosPendientes()));
                lblBreakdownRechazados.setText(String.format("● Rechazados: %.1f%% (%d)", pRec, m.pedidosRechazados()));
            }

            cachedOrders = dashboardService.obtenerUltimosPedidos();
            filtrarTablaLocal();

        } catch (Exception e) {
            UiHelper.showError(this, "Error al actualizar métricas del panel de control: " + e.getMessage());
        }
    }

    private void filtrarTablaLocal() {
        if (cachedOrders == null) return;
        String query = (txtSearchOrders != null) ? txtSearchOrders.getText().trim().toLowerCase() : "";

        ordersTableModel.setRowCount(0);
        for (Pedido p : cachedOrders) {
            String cod = String.valueOf(p.getCodigoPedido());
            String cli = String.valueOf(p.getCodigoCliente());
            String est = (p.getEstado() != null) ? p.getEstado().toLowerCase() : "";

            if (query.isEmpty() || cod.contains(query) || cli.contains(query) || est.contains(query)) {
                ordersTableModel.addRow(new Object[]{
                        p.getCodigoPedido(),
                        p.getFechaPedido(),
                        p.getFechaEsperada(),
                        p.getFechaEntrega() != null ? p.getFechaEntrega() : "--",
                        p.getEstado(),
                        p.getCodigoCliente()
                });
            }
        }
    }
}
