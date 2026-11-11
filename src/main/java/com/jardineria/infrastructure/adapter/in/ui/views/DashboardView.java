package com.jardineria.infrastructure.adapter.in.ui.views;

import com.jardineria.application.service.DashboardService;
import com.jardineria.domain.model.Pedido;
import com.jardineria.infrastructure.adapter.in.ui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Vista de Dashboard principal con métricas ejecutivas, distribución porcentual,
 * microtendencias Sparkline y filtrado reactivo de pedidos estilo JavaTMP.
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
    private SearchFilterBar searchFilterBar;
    private List<Pedido> cachedOrders;

    public DashboardView(DashboardService dashboardService, Consumer<String> navigationConsumer) {
        this.dashboardService = dashboardService;
        this.navigationConsumer = navigationConsumer;

        setLayout(new BorderLayout(16, 16));
        setBorder(new EmptyBorder(16, 20, 16, 20));

        initHeader();
        initCenterContent();

        refreshData();
    }

    private void initHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setOpaque(false);

        JLabel lblIcon = new JLabel(VectorIcons.get(VectorIcons.IconType.DASHBOARD, 24, UiHelper.COLOR_PRIMARY));
        
        JPanel textGroup = new JPanel(new GridLayout(2, 1, 2, 2));
        textGroup.setOpaque(false);

        JLabel lblTitle = new JLabel("Panel de Control y Métricas Ejecutivas");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 20));

        JLabel lblSubtitle = new JLabel("Visión general operacional en tiempo real de la base de datos Jardinería");
        lblSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(150, 155, 165));

        textGroup.add(lblTitle);
        textGroup.add(lblSubtitle);

        titlePanel.add(lblIcon);
        titlePanel.add(textGroup);

        // Acciones Rápidas del Header
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionsPanel.setOpaque(false);

        JButton btnElectron = UiHelper.createIconButton("Modo Escritorio Web", VectorIcons.IconType.EXTERNAL_LINK, UiHelper.COLOR_PURPLE);
        btnElectron.setToolTipText("Abrir experiencia web nativa en Google Chrome (Modo App)");
        btnElectron.addActionListener(e -> {
            String url = (com.jardineria.infrastructure.ServiceRegistry.getInstance().getWebServer() != null)
                    ? com.jardineria.infrastructure.ServiceRegistry.getInstance().getWebServer().getDashboardUrl()
                    : "http://localhost:8080/dashboard";
            ElectronModeLauncher.openElectronMode(url);
            ToastNotificationManager.showInfo("Modo Escritorio", "Iniciando panel web interactivo en ventana independiente.");
        });

        JButton btnExport = UiHelper.createIconButton("Exportar CSV", VectorIcons.IconType.EXPORT, UiHelper.COLOR_SECONDARY);
        btnExport.addActionListener(e -> {
            ExportHelper.exportTableToCsv(this, ordersTable, "pedidos_recientes");
            ToastNotificationManager.showSuccess("Exportación", "Tabla exportada a CSV exitosamente.");
        });

        JButton btnRefresh = UiHelper.createIconButton("Actualizar", VectorIcons.IconType.REFRESH, UiHelper.COLOR_PRIMARY);
        btnRefresh.addActionListener(e -> refreshData());

        actionsPanel.add(btnElectron);
        actionsPanel.add(btnExport);
        actionsPanel.add(btnRefresh);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(actionsPanel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
    }

    private void initCenterContent() {
        JPanel centerPanel = new JPanel(new BorderLayout(0, 16));
        centerPanel.setOpaque(false);

        // 1. Grid superior de KPI Cards con Microtendencias Sparkline
        JPanel kpiGrid = new JPanel(new GridLayout(1, 5, 12, 12));
        kpiGrid.setOpaque(false);

        cardClientes = new KpiCard(VectorIcons.IconType.CLIENTS, "Clientes Activos", "...", "Registrados", UiHelper.COLOR_SECONDARY, new double[]{12, 15, 18, 20, 24, 28, 30});
        cardClientes.setClickAction(() -> navigate("CLIENTES"));

        cardPedidos = new KpiCard(VectorIcons.IconType.ORDERS, "Pedidos Totales", "...", "Procesados", UiHelper.COLOR_PRIMARY, new double[]{20, 32, 28, 45, 52, 60, 68});
        cardPedidos.setClickAction(() -> navigate("PEDIDOS"));

        cardIngresos = new KpiCard(VectorIcons.IconType.PAYMENTS, "Facturación Total", "...", "Pagos confirmados", UiHelper.COLOR_PURPLE, new double[]{40, 55, 62, 70, 85, 92, 110});
        cardIngresos.setClickAction(() -> navigate("PAGOS"));

        cardStockBajo = new KpiCard(VectorIcons.IconType.ALERT_TRIANGLE, "Stock Crítico", "...", "Alertas < 10 uds", UiHelper.COLOR_DANGER, new double[]{8, 12, 10, 15, 14, 18, 16});
        cardStockBajo.setClickAction(() -> navigate("PRODUCTOS"));

        cardEmpleados = new KpiCard(VectorIcons.IconType.EMPLOYEES, "Equipo Laboral", "...", "Empleados activos", UiHelper.COLOR_WARNING, new double[]{10, 12, 14, 15, 18, 19, 21});
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

        // 3. Fila Inferior: Tabla de Actividad Reciente con Filtros tipo JavaTMP
        middleWrapper.add(createRecentActivityPanel(), BorderLayout.CENTER);

        centerPanel.add(middleWrapper, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel createDistributionPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 10));
        panel.putClientProperty("FlatLaf.style", "arc: 12");
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Distribución y Estado Logístico de Pedidos"),
                new EmptyBorder(8, 14, 10, 14)
        ));

        orderDistributionBar = new DistributionBar();

        JPanel labelsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 4));
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
                BorderFactory.createTitledBorder("Accesos Rápidos Operacionales"),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JButton btnPed = UiHelper.createIconButton("Gestión de Pedidos", VectorIcons.IconType.ORDERS, UiHelper.COLOR_PRIMARY);
        btnPed.addActionListener(e -> navigate("PEDIDOS"));

        JButton btnCli = UiHelper.createIconButton("Catálogo de Clientes", VectorIcons.IconType.CLIENTS, UiHelper.COLOR_SECONDARY);
        btnCli.addActionListener(e -> navigate("CLIENTES"));

        JButton btnProd = UiHelper.createIconButton("Inventario y Stock", VectorIcons.IconType.PRODUCTS, UiHelper.COLOR_WARNING);
        btnProd.addActionListener(e -> navigate("PRODUCTOS"));

        JButton btnPago = UiHelper.createIconButton("Caja y Liquidaciones", VectorIcons.IconType.PAYMENTS, UiHelper.COLOR_PURPLE);
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
        panel.setBorder(BorderFactory.createTitledBorder("Últimos Pedidos Registrados"));

        // Barra de búsqueda y filtrado estilo JavaTMP
        searchFilterBar = new SearchFilterBar(
                "Buscar por código o cliente...",
                new String[]{"Todos", "Entregado", "Pendiente", "Rechazado"},
                (query, filter) -> filtrarTablaLocal()
        );
        panel.add(searchFilterBar, BorderLayout.NORTH);

        String[] columns = {"Código Pedido", "Fecha de Pedido", "Fecha Esperada", "Fecha de Entrega", "Estado Operativo", "Código Cliente"};
        ordersTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        ordersTable = UiHelper.createStyledTable(ordersTableModel);
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

            ToastNotificationManager.showSuccess("Datos Sincronizados", "Panel ejecutivo actualizado correctamente.");
        } catch (Exception e) {
            ToastNotificationManager.showError("Error de Conexión", "No se pudieron obtener métricas: " + e.getMessage());
        }
    }

    private void filtrarTablaLocal() {
        if (cachedOrders == null) return;
        String query = (searchFilterBar != null) ? searchFilterBar.getSearchQuery().toLowerCase() : "";
        String filter = (searchFilterBar != null) ? searchFilterBar.getActiveFilter().toUpperCase() : "TODOS";

        ordersTableModel.setRowCount(0);
        int matched = 0;

        for (Pedido p : cachedOrders) {
            String cod = String.valueOf(p.getCodigoPedido());
            String cli = String.valueOf(p.getCodigoCliente());
            String est = (p.getEstado() != null) ? p.getEstado().toUpperCase() : "";

            boolean matchesStatus = filter.equals("TODOS") || est.contains(filter);
            boolean matchesQuery = query.isEmpty() || cod.contains(query) || cli.contains(query) || est.toLowerCase().contains(query);

            if (matchesStatus && matchesQuery) {
                ordersTableModel.addRow(new Object[]{
                        p.getCodigoPedido(),
                        p.getFechaPedido(),
                        p.getFechaEsperada(),
                        p.getFechaEntrega() != null ? p.getFechaEntrega() : "--",
                        p.getEstado(),
                        p.getCodigoCliente()
                });
                matched++;
            }
        }

        if (searchFilterBar != null) {
            searchFilterBar.updateCounter(matched, cachedOrders.size());
        }
    }
}
