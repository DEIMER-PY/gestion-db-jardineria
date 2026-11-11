package com.jardineria.infrastructure.adapter.in.ui;

import com.jardineria.infrastructure.ServiceRegistry;
import com.jardineria.infrastructure.adapter.in.ui.components.*;
import com.jardineria.infrastructure.adapter.in.ui.views.*;
import com.jardineria.infrastructure.config.DatabaseConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Ventana Principal de la Aplicación de Gestión de Jardinería con Interfaz Gráfica Moderna,
 * Iconografía Vectorial Java2D, Sistema Toast no bloqueante y Navegación Avanzada.
 */
public class MainDashboardWindow extends JFrame {
    private final ServiceRegistry registry;

    private CardLayout cardLayout;
    private JPanel contentPanel;
    private BreadcrumbBar breadcrumbBar;

    private DashboardView dashboardView;
    private OficinaView oficinaView;
    private EmpleadoView empleadoView;
    private ClienteView clienteView;
    private ProductoView productoView;
    private PedidoView pedidoView;
    private PagoView pagoView;
    private GamaView gamaView;
    private PatternsHubView patternsView;

    private JButton btnNavActive;
    private final Map<String, JButton> navButtons = new HashMap<>();
    private JLabel lblStatusSync;
    private JLabel lblStatusView;
    private String currentCard = "DASHBOARD";

    public MainDashboardWindow(ServiceRegistry registry) {
        this.registry = registry;

        setTitle("Jardinería Enterprise Suite — Hexagonal Architecture & Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1340, 860);
        setMinimumSize(new Dimension(1100, 720));
        setLocationRelativeTo(null);

        initLayout();
        setupShortcuts();

        // Inicializar Gestor de Notificaciones Toast sobre esta ventana
        ToastNotificationManager.initialize(this);
    }

    private void initLayout() {
        setLayout(new BorderLayout());

        // Header Superior y Migas de Pan (Breadcrumbs)
        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(createHeaderPanel(), BorderLayout.NORTH);

        breadcrumbBar = new BreadcrumbBar(this::navigateTo);
        topContainer.add(breadcrumbBar, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

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
        patternsView = new PatternsHubView(registry);

        contentPanel.add(dashboardView, "DASHBOARD");
        contentPanel.add(patternsView, "PATRONES");
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
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(120, 125, 135, 70)),
                new EmptyBorder(12, 20, 12, 20)
        ));

        // Título y Branding con Icono Vectorial
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brandPanel.setOpaque(false);

        JLabel lblLogo = new JLabel(VectorIcons.get(VectorIcons.IconType.PRODUCTS, 24, UiHelper.COLOR_PRIMARY));

        JLabel lblTitle = new JLabel("Jardinería Enterprise Suite");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 18));

        JLabel lblTag = new JLabel(" Arquitectura Hexagonal | Java 21 ");
        lblTag.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblTag.setOpaque(true);
        lblTag.setBackground(new Color(16, 185, 129, 35));
        lblTag.setForeground(UiHelper.COLOR_PRIMARY);
        lblTag.setBorder(new EmptyBorder(3, 8, 3, 8));

        brandPanel.add(lblLogo);
        brandPanel.add(lblTitle);
        brandPanel.add(lblTag);

        // Estado de Conexión, Modo Web y Theme Toggle
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setOpaque(false);

        JLabel lblDbStatus = new JLabel(
                String.format("Conectado: %s:%d/%s", DatabaseConfig.getHost(), DatabaseConfig.getPort(), DatabaseConfig.getDatabaseName()),
                VectorIcons.get(VectorIcons.IconType.DATABASE, 14, UiHelper.COLOR_PRIMARY),
                SwingConstants.LEFT
        );
        lblDbStatus.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblDbStatus.setForeground(UiHelper.COLOR_PRIMARY);

        JButton btnElectron = UiHelper.createIconButton("Modo Escritorio Web", VectorIcons.IconType.EXTERNAL_LINK, UiHelper.COLOR_PURPLE);
        btnElectron.setToolTipText("Abrir panel interactivo web nativo en Google Chrome (Modo App)");
        btnElectron.addActionListener(e -> {
            String url = (registry.getWebServer() != null) ? registry.getWebServer().getDashboardUrl() : "http://localhost:8080/dashboard";
            ElectronModeLauncher.openElectronMode(url);
            ToastNotificationManager.showInfo("Modo Escritorio", "Abriendo aplicación web en ventana independiente.");
        });

        JButton btnTheme = new JButton(VectorIcons.get(VectorIcons.IconType.SUN, 16, Color.GRAY));
        btnTheme.setToolTipText("Cambiar tema Claro / Oscuro (Ctrl+T)");
        btnTheme.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTheme.putClientProperty("JButton.buttonType", "roundRect");
        btnTheme.putClientProperty("Component.arc", 10);
        btnTheme.addActionListener(e -> {
            ThemeManager.toggleTheme(this);
            btnTheme.setIcon(VectorIcons.get(ThemeManager.isDarkMode() ? VectorIcons.IconType.SUN : VectorIcons.IconType.MOON, 16, Color.GRAY));
            updateStatusTimestamp();
        });

        rightPanel.add(lblDbStatus);
        rightPanel.add(btnElectron);
        rightPanel.add(btnTheme);

        header.add(brandPanel, BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(245, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(120, 125, 135, 70)));

        JPanel menuItems = new JPanel(new GridLayout(9, 1, 0, 4));
        menuItems.setBorder(new EmptyBorder(12, 10, 12, 10));
        menuItems.setOpaque(false);

        JButton btnDash = createNavButton("Panel General", "DASHBOARD", VectorIcons.IconType.DASHBOARD);
        JButton btnPat = createNavButton("Patrones GoF", "PATRONES", VectorIcons.IconType.PATTERNS);
        JButton btnOfi = createNavButton("Oficinas Globales", "OFICINAS", VectorIcons.IconType.OFFICES);
        JButton btnEmp = createNavButton("Equipo Laboral", "EMPLEADOS", VectorIcons.IconType.EMPLOYEES);
        JButton btnCli = createNavButton("Cartera de Clientes", "CLIENTES", VectorIcons.IconType.CLIENTS);
        JButton btnProd = createNavButton("Inventario y Stock", "PRODUCTOS", VectorIcons.IconType.PRODUCTS);
        JButton btnPed = createNavButton("Gestión de Pedidos", "PEDIDOS", VectorIcons.IconType.ORDERS);
        JButton btnPago = createNavButton("Caja y Liquidaciones", "PAGOS", VectorIcons.IconType.PAYMENTS);
        JButton btnGama = createNavButton("Gamas Botánicas", "GAMAS", VectorIcons.IconType.GAMAS);

        navButtons.put("DASHBOARD", btnDash);
        navButtons.put("PATRONES", btnPat);
        navButtons.put("OFICINAS", btnOfi);
        navButtons.put("EMPLEADOS", btnEmp);
        navButtons.put("CLIENTES", btnCli);
        navButtons.put("PRODUCTOS", btnProd);
        navButtons.put("PEDIDOS", btnPed);
        navButtons.put("PAGOS", btnPago);
        navButtons.put("GAMAS", btnGama);

        menuItems.add(btnDash);
        menuItems.add(btnPat);
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

        JLabel lblVer = new JLabel("Versión 1.3.0 Enterprise");
        lblVer.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblVer.setForeground(new Color(140, 145, 155));

        JLabel lblFooter = new JLabel("Arquitectura Hexagonal & DDD");
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

        lblStatusView = new JLabel("Vista activa: Panel General");
        lblStatusView.setFont(new Font("SansSerif", Font.PLAIN, 11));

        JPanel rightStatus = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightStatus.setOpaque(false);

        JLabel lblHint = new JLabel("F5: Refrescar | Ctrl+1..9: Módulos | Ctrl+T: Tema | Ctrl+W: Web App");
        lblHint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblHint.setForeground(new Color(140, 145, 155));

        lblStatusSync = new JLabel("Sincronizado: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        lblStatusSync.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblStatusSync.setForeground(UiHelper.COLOR_PRIMARY);

        rightStatus.add(lblHint);
        rightStatus.add(lblStatusSync);

        bar.add(lblStatusView, BorderLayout.WEST);
        bar.add(rightStatus, BorderLayout.EAST);

        return bar;
    }

    private JButton createNavButton(String text, String cardName, VectorIcons.IconType iconType) {
        JButton btn = new JButton(text);
        btn.setIcon(VectorIcons.get(iconType, 16, UiHelper.COLOR_PRIMARY));
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setIconTextGap(10);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.putClientProperty("Component.arc", 12);
        btn.setMargin(new Insets(8, 12, 8, 12));

        btn.addActionListener(e -> navigateTo(cardName));

        return btn;
    }

    public void navigateTo(String cardName) {
        cardLayout.show(contentPanel, cardName);
        this.currentCard = cardName;

        JButton btn = navButtons.get(cardName);
        if (btn != null) {
            highlightActiveButton(btn);
        }

        refreshActiveView(cardName);
        lblStatusView.setText("Vista activa: " + getHumanReadableName(cardName));
        updateStatusTimestamp();

        // Actualizar migas de pan
        VectorIcons.IconType icon = getIconForCard(cardName);
        String shortcut = getShortcutHintForCard(cardName);
        breadcrumbBar.updatePath(getHumanReadableName(cardName), shortcut, icon);
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
            case "DASHBOARD" -> "Panel General";
            case "PATRONES" -> "Patrones Estructurales GoF";
            case "OFICINAS" -> "Gestión de Oficinas";
            case "EMPLEADOS" -> "Gestión de Empleados";
            case "CLIENTES" -> "Gestión de Clientes";
            case "PRODUCTOS" -> "Catálogo de Productos";
            case "PEDIDOS" -> "Pedidos y Líneas de Detalle";
            case "PAGOS" -> "Pagos y Finanzas";
            case "GAMAS" -> "Gamas Botánicas";
            default -> cardName;
        };
    }

    private VectorIcons.IconType getIconForCard(String cardName) {
        return switch (cardName) {
            case "DASHBOARD" -> VectorIcons.IconType.DASHBOARD;
            case "PATRONES" -> VectorIcons.IconType.PATTERNS;
            case "OFICINAS" -> VectorIcons.IconType.OFFICES;
            case "EMPLEADOS" -> VectorIcons.IconType.EMPLOYEES;
            case "CLIENTES" -> VectorIcons.IconType.CLIENTS;
            case "PRODUCTOS" -> VectorIcons.IconType.PRODUCTS;
            case "PEDIDOS" -> VectorIcons.IconType.ORDERS;
            case "PAGOS" -> VectorIcons.IconType.PAYMENTS;
            case "GAMAS" -> VectorIcons.IconType.GAMAS;
            default -> VectorIcons.IconType.DASHBOARD;
        };
    }

    private String getShortcutHintForCard(String cardName) {
        return switch (cardName) {
            case "DASHBOARD" -> "Ctrl+1";
            case "PATRONES" -> "Ctrl+2";
            case "OFICINAS" -> "Ctrl+3";
            case "EMPLEADOS" -> "Ctrl+4";
            case "CLIENTES" -> "Ctrl+5";
            case "PRODUCTOS" -> "Ctrl+6";
            case "PEDIDOS" -> "Ctrl+7";
            case "PAGOS" -> "Ctrl+8";
            case "GAMAS" -> "Ctrl+9";
            default -> null;
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
            lblStatusSync.setText("Sincronizado: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }
    }

    private void setupShortcuts() {
        // Atajo F5 para refrescar la vista activa
        getRootPane().registerKeyboardAction(
                e -> {
                    refreshActiveView(currentCard);
                    updateStatusTimestamp();
                    ToastNotificationManager.showInfo("Sincronización", "Vista refrescada mediante atajo F5.");
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // Atajo Ctrl+T para alternar tema
        getRootPane().registerKeyboardAction(
                e -> {
                    ThemeManager.toggleTheme(this);
                    updateStatusTimestamp();
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // Atajo Ctrl+W para abrir modo Web App
        getRootPane().registerKeyboardAction(
                e -> {
                    String url = (registry.getWebServer() != null) ? registry.getWebServer().getDashboardUrl() : "http://localhost:8080/dashboard";
                    ElectronModeLauncher.openElectronMode(url);
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // Atajos de Navegación Ctrl+1..9
        String[] modules = {"DASHBOARD", "PATRONES", "OFICINAS", "EMPLEADOS", "CLIENTES", "PRODUCTOS", "PEDIDOS", "PAGOS", "GAMAS"};
        for (int i = 0; i < modules.length; i++) {
            final String mod = modules[i];
            int keyCode = KeyEvent.VK_1 + i;
            getRootPane().registerKeyboardAction(
                    e -> navigateTo(mod),
                    KeyStroke.getKeyStroke(keyCode, KeyEvent.CTRL_DOWN_MASK),
                    JComponent.WHEN_IN_FOCUSED_WINDOW
            );
        }
    }
}
