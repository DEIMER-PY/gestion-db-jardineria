package com.jardineria.infrastructure.adapter.in.ui;

import com.jardineria.infrastructure.ServiceRegistry;
import com.jardineria.infrastructure.adapter.in.ui.components.ThemeManager;
import com.jardineria.infrastructure.adapter.in.ui.components.UiHelper;
import com.jardineria.infrastructure.adapter.in.ui.views.*;
import com.jardineria.infrastructure.config.DatabaseConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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
    private JLabel lblStatusSync;
    private JLabel lblStatusView;
    private String currentCard = "DASHBOARD";

    public MainDashboardWindow(ServiceRegistry registry) {
        this.registry = registry;

        setTitle("🌿 Jardinería Enterprise Suite — Hexagonal Architecture & Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1320, 840);
        setMinimumSize(new Dimension(1080, 700));
        setLocationRelativeTo(null);

        initLayout();
        setupShortcuts();
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

        dashboardView = new DashboardView(registry.getDashboardService(), this::navigateTo);
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

        // Barra de Estado Inferior
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(120, 125, 135, 80)),
                new EmptyBorder(12, 20, 12, 20)
        ));

        // Título y Branding
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brandPanel.setOpaque(false);

        JLabel lblLogo = new JLabel("🌿");
        lblLogo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));

        JLabel lblTitle = new JLabel("Jardinería Enterprise Suite");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 18));

        JLabel lblTag = new JLabel(" Arquitectura Hexagonal | Java 21 ");
        lblTag.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblTag.setOpaque(true);
        lblTag.setBackground(new Color(46, 160, 67, 40));
        lblTag.setForeground(UiHelper.COLOR_PRIMARY);
        lblTag.setBorder(new EmptyBorder(3, 8, 3, 8));

        brandPanel.add(lblLogo);
        brandPanel.add(lblTitle);
        brandPanel.add(lblTag);

        // Estado de Conexión y Theme Toggle
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightPanel.setOpaque(false);

        JLabel lblDbStatus = new JLabel(String.format("🟢 Conectado: %s:%d/%s",
                DatabaseConfig.getHost(), DatabaseConfig.getPort(), DatabaseConfig.getDatabaseName()));
        lblDbStatus.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblDbStatus.setForeground(UiHelper.COLOR_PRIMARY);

        JButton btnTheme = UiHelper.createButton("Modo Visual", "🌓", Color.GRAY);
        btnTheme.addActionListener(e -> {
            ThemeManager.toggleTheme(this);
            updateStatusTimestamp();
        });

        rightPanel.add(lblDbStatus);
        rightPanel.add(btnTheme);

        header.add(brandPanel, BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(120, 125, 135, 80)));

        JPanel menuItems = new JPanel(new GridLayout(8, 1, 0, 4));
        menuItems.setBorder(new EmptyBorder(12, 10, 12, 10));
        menuItems.setOpaque(false);

        JButton btnDash = createNavButton("📊 Dashboard General", "DASHBOARD");
        JButton btnOfi = createNavButton("🏢 Oficinas", "OFICINAS");
        JButton btnEmp = createNavButton("👥 Empleados", "EMPLEADOS");
        JButton btnCli = createNavButton("🛍️ Clientes", "CLIENTES");
        JButton btnProd = createNavButton("📦 Productos y Stock", "PRODUCTOS");
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

        JLabel lblVer = new JLabel("Versión 1.2.0 (LTS)");
        lblVer.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblVer.setForeground(new Color(140, 145, 155));

        JLabel lblFooter = new JLabel("Patrones: Singleton & Ports");
        lblFooter.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblFooter.setForeground(new Color(110, 115, 125));

        footer.add(lblVer);
        footer.add(lblFooter);

        sidebar.add(menuItems, BorderLayout.NORTH);
        sidebar.add(footer, BorderLayout.SOUTH);

        return sidebar;
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(120, 125, 135, 60)),
                new EmptyBorder(6, 16, 6, 16)
        ));

        lblStatusView = new JLabel("Vista activa: Dashboard General");
        lblStatusView.setFont(new Font("SansSerif", Font.PLAIN, 11));

        JPanel rightStatus = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightStatus.setOpaque(false);

        JLabel lblHint = new JLabel("⌨️ F5: Refrescar datos | Exportación CSV activa");
        lblHint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblHint.setForeground(new Color(140, 145, 155));

        lblStatusSync = new JLabel("🕒 Sincronizado: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        lblStatusSync.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblStatusSync.setForeground(UiHelper.COLOR_PRIMARY);

        rightStatus.add(lblHint);
        rightStatus.add(lblStatusSync);

        bar.add(lblStatusView, BorderLayout.WEST);
        bar.add(rightStatus, BorderLayout.EAST);

        return bar;
    }

    private JButton createNavButton(String text, String cardName) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.putClientProperty("Component.arc", 12);
        btn.setMargin(new Insets(8, 12, 8, 12));

        btn.addActionListener(e -> navigateTo(cardName, btn));

        return btn;
    }

    public void navigateTo(String cardName) {
        // Encontrar el botón correspondiente si existe
        cardLayout.show(contentPanel, cardName);
        this.currentCard = cardName;
        refreshActiveView(cardName);
        lblStatusView.setText("Vista activa: " + getHumanReadableName(cardName));
        updateStatusTimestamp();
    }

    private void navigateTo(String cardName, JButton btn) {
        cardLayout.show(contentPanel, cardName);
        this.currentCard = cardName;
        highlightActiveButton(btn);
        refreshActiveView(cardName);
        lblStatusView.setText("Vista activa: " + getHumanReadableName(cardName));
        updateStatusTimestamp();
    }

    private void highlightActiveButton(JButton active) {
        if (btnNavActive != null) {
            btnNavActive.setFont(new Font("SansSerif", Font.PLAIN, 12));
            btnNavActive.setForeground(UIManager.getColor("Button.foreground"));
        }
        btnNavActive = active;
        btnNavActive.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnNavActive.setForeground(UiHelper.COLOR_PRIMARY);
    }

    private String getHumanReadableName(String cardName) {
        return switch (cardName) {
            case "DASHBOARD" -> "Dashboard General";
            case "OFICINAS" -> "Gestión de Oficinas";
            case "EMPLEADOS" -> "Gestión de Empleados";
            case "CLIENTES" -> "Gestión de Clientes";
            case "PRODUCTOS" -> "Catálogo de Productos";
            case "PEDIDOS" -> "Pedidos y Líneas de Detalle";
            case "PAGOS" -> "Pagos y Finanzas";
            case "GAMAS" -> "Gamas de Productos";
            default -> cardName;
        };
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

    public void updateStatusTimestamp() {
        if (lblStatusSync != null) {
            lblStatusSync.setText("🕒 Sincronizado: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }
    }

    private void setupShortcuts() {
        // Atajo F5 para refrescar la vista activa
        getRootPane().registerKeyboardAction(
                e -> {
                    refreshActiveView(currentCard);
                    updateStatusTimestamp();
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }
}
