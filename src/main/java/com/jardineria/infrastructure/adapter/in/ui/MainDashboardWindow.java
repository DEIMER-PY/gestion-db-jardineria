package com.jardineria.infrastructure.adapter.in.ui;

import com.jardineria.infrastructure.ServiceRegistry;
import com.jardineria.infrastructure.adapter.in.ui.components.ThemeManager;
import com.jardineria.infrastructure.adapter.in.ui.components.UiHelper;
import com.jardineria.infrastructure.adapter.in.ui.views.*;
import com.jardineria.infrastructure.config.DatabaseConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Ventana Principal de la Aplicación de Gestión de Jardinería con Interfaz Gráfica Moderna y Dashboard.
 */
public class MainDashboardWindow extends JFrame {
    private final ServiceRegistry registry;

    private CardLayout cardLayout;
    private JPanel contentPanel;

    private DashboardView dashboardView;
    private OficinaView oficinaView;
    private EmpleadoView empleadoView;
    private ClienteView clienteView;
    private ProductoView productoView;
    private PedidoView pedidoView;
    private PagoView pagoView;
    private GamaView gamaView;

    private JButton btnNavActive;

    public MainDashboardWindow(ServiceRegistry registry) {
        this.registry = registry;

        setTitle("🌿 Jardinería Enterprise — Hexagonal Architecture & Management Suite");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1024, 680));
        setLocationRelativeTo(null);

        initLayout();
    }

    private void initLayout() {
        setLayout(new BorderLayout());

        // Header Superior
        add(createHeaderPanel(), BorderLayout.NORTH);

        // Sidebar Izquierda
        add(createSidebarPanel(), BorderLayout.WEST);

        // Panel Central Dinámico (CardLayout)
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        dashboardView = new DashboardView(registry.getDashboardService());
        oficinaView = new OficinaView(registry.getOficinaService());
        empleadoView = new EmpleadoView(registry.getEmpleadoService());
        clienteView = new ClienteView(registry.getClienteService());
        productoView = new ProductoView(registry.getProductoService());
        pedidoView = new PedidoView(registry.getPedidoService(), registry.getDetallePedidoService());
        pagoView = new PagoView(registry.getPagoService());
        gamaView = new GamaView(registry.getGamaProductoService());

        contentPanel.add(dashboardView, "DASHBOARD");
        contentPanel.add(oficinaView, "OFICINAS");
        contentPanel.add(empleadoView, "EMPLEADOS");
        contentPanel.add(clienteView, "CLIENTES");
        contentPanel.add(productoView, "PRODUCTOS");
        contentPanel.add(pedidoView, "PEDIDOS");
        contentPanel.add(pagoView, "PAGOS");
        contentPanel.add(gamaView, "GAMAS");

        add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(50, 55, 65)),
                new EmptyBorder(12, 20, 12, 20)
        ));

        // Título y Branding
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brandPanel.setOpaque(false);

        JLabel lblLogo = new JLabel("🌿");
        lblLogo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));

        JLabel lblTitle = new JLabel("Jardinería Enterprise Suite");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 17));

        JLabel lblTag = new JLabel(" Arquitectura Hexagonal | Java 21 ");
        lblTag.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblTag.setOpaque(true);
        lblTag.setBackground(new Color(46, 160, 67, 50));
        lblTag.setForeground(UiHelper.COLOR_PRIMARY);
        lblTag.setBorder(new EmptyBorder(2, 6, 2, 6));

        brandPanel.add(lblLogo);
        brandPanel.add(lblTitle);
        brandPanel.add(lblTag);

        // Estado de Conexión y Theme Toggle
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightPanel.setOpaque(false);

        JLabel lblDbStatus = new JLabel(String.format("🟢 Conectado: %s:%d/%s",
                DatabaseConfig.getHost(), DatabaseConfig.getPort(), DatabaseConfig.getDatabaseName()));
        lblDbStatus.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblDbStatus.setForeground(UiHelper.COLOR_PRIMARY);

        JButton btnTheme = UiHelper.createButton("Modo Visual", "🌓", Color.GRAY);
        btnTheme.addActionListener(e -> ThemeManager.toggleTheme(this));

        rightPanel.add(lblDbStatus);
        rightPanel.add(btnTheme);

        header.add(brandPanel, BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(50, 55, 65)));

        JPanel menuItems = new JPanel(new GridLayout(9, 1, 0, 4));
        menuItems.setBorder(new EmptyBorder(12, 12, 12, 12));
        menuItems.setOpaque(false);

        JButton btnDash = createNavButton("📊 Dashboard General", "DASHBOARD");
        JButton btnOfi = createNavButton("🏢 Oficinas", "OFICINAS");
        JButton btnEmp = createNavButton("👥 Empleados", "EMPLEADOS");
        JButton btnCli = createNavButton("🛍️ Clientes", "CLIENTES");
        JButton btnProd = createNavButton("📦 Productos", "PRODUCTOS");
        JButton btnPed = createNavButton("📑 Pedidos y Líneas", "PEDIDOS");
        JButton btnPago = createNavButton("💳 Pagos y Caja", "PAGOS");
        JButton btnGama = createNavButton("🌿 Gamas de Producto", "GAMAS");

        menuItems.add(btnDash);
        menuItems.add(btnOfi);
        menuItems.add(btnEmp);
        menuItems.add(btnCli);
        menuItems.add(btnProd);
        menuItems.add(btnPed);
        menuItems.add(btnPago);
        menuItems.add(btnGama);

        btnNavActive = btnDash;
        highlightActiveButton(btnDash);

        // Footer Sidebar
        JPanel footer = new JPanel(new GridLayout(2, 1, 2, 2));
        footer.setBorder(new EmptyBorder(12, 16, 16, 16));
        footer.setOpaque(false);

        JLabel lblVer = new JLabel("Versión 1.0.0 (LTS)");
        lblVer.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblVer.setForeground(new Color(130, 135, 145));

        JLabel lblFooter = new JLabel("Patrones: Singleton & Ports");
        lblFooter.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblFooter.setForeground(new Color(100, 105, 115));

        footer.add(lblVer);
        footer.add(lblFooter);

        sidebar.add(menuItems, BorderLayout.NORTH);
        sidebar.add(footer, BorderLayout.SOUTH);

        return sidebar;
    }

    private JButton createNavButton(String text, String cardName) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.setMargin(new Insets(8, 12, 8, 12));

        btn.addActionListener(e -> {
            cardLayout.show(contentPanel, cardName);
            highlightActiveButton(btn);
            refreshActiveView(cardName);
        });

        return btn;
    }

    private void highlightActiveButton(JButton active) {
        if (btnNavActive != null) {
            btnNavActive.setForeground(UIManager.getColor("Button.foreground"));
        }
        btnNavActive = active;
        btnNavActive.setForeground(UiHelper.COLOR_PRIMARY);
    }

    private void refreshActiveView(String cardName) {
        switch (cardName) {
            case "DASHBOARD" -> dashboardView.refreshData();
            case "OFICINAS" -> oficinaView.refreshTable();
            case "EMPLEADOS" -> empleadoView.refreshTable();
            case "CLIENTES" -> clienteView.refreshTable();
            case "PRODUCTOS" -> productoView.refreshTable();
            case "PEDIDOS" -> pedidoView.refreshOrders();
            case "PAGOS" -> pagoView.refreshTable();
            case "GAMAS" -> gamaView.refreshTable();
        }
    }
}
