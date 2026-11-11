package com.jardineria.infrastructure.adapter.in.ui.views;

import com.jardineria.domain.pattern.adapter.PayPalAdapter;
import com.jardineria.domain.pattern.adapter.StripeAdapter;
import com.jardineria.domain.pattern.bridge.*;
import com.jardineria.domain.pattern.composite.ProductBundleComposite;
import com.jardineria.domain.pattern.composite.ProductLeaf;
import com.jardineria.domain.pattern.decorator.*;
import com.jardineria.domain.pattern.facade.CheckoutRequest;
import com.jardineria.domain.pattern.facade.CheckoutResult;
import com.jardineria.domain.pattern.facade.JardineriaBusinessFacade;
import com.jardineria.domain.pattern.flyweight.GamaFlyweight;
import com.jardineria.domain.pattern.flyweight.GamaFlyweightFactory;
import com.jardineria.domain.pattern.proxy.CachedProductLookupProxy;
import com.jardineria.infrastructure.ServiceRegistry;
import com.jardineria.infrastructure.adapter.in.ui.components.ImageHelper;
import com.jardineria.infrastructure.adapter.in.ui.components.UiHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Vista interactiva del Centro de Patrones de Diseño GoF (7 Patrones Estructurales).
 * Permite explorar los diagramas de referencia y ejecutar demostraciones interactivas en vivo.
 */
public class PatternsHubView extends JPanel {
    private final ServiceRegistry registry;
    private final JTextArea txtConsole;
    private final JLabel lblProxyStats;

    public PatternsHubView(ServiceRegistry registry) {
        this.registry = registry;
        setLayout(new BorderLayout(14, 14));
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // Header Superior
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel lblTitle = new JLabel("🏛️ Centro de Arquitectura: 7 Patrones de Diseño Estructurales GoF");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 18));

        JLabel lblSub = new JLabel("Implementación en código e infografías educativas integradas directamente en el sistema");
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSub.setForeground(new Color(150, 155, 165));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 2, 2));
        titlePanel.setOpaque(false);
        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);

        header.add(titlePanel, BorderLayout.WEST);

        lblProxyStats = new JLabel("Proxy Cache: 0 Hits | 0 Misses");
        lblProxyStats.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblProxyStats.setForeground(UiHelper.COLOR_PURPLE);
        header.add(lblProxyStats, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // Centro: Split Pane entre Pestañas de Patrones y Consola de Demostración
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.60);
        split.setContinuousLayout(true);

        // Panel Izquierdo: Pestañas de los 7 Patrones
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 12));

        tabbedPane.addTab("🍽️ Facade", createFacadePanel());
        tabbedPane.addTab("🔌 Adapter", createAdapterPanel());
        tabbedPane.addTab("🫓 Decorator", createDecoratorPanel());
        tabbedPane.addTab("📦 Composite", createCompositePanel());
        tabbedPane.addTab("🛡️ Proxy", createProxyPanel());
        tabbedPane.addTab("🌲 Flyweight", createFlyweightPanel());
        tabbedPane.addTab("🌉 Bridge", createBridgePanel());

        split.setLeftComponent(tabbedPane);

        // Panel Derecho: Consola de Salida en Vivo
        JPanel consolePanel = new JPanel(new BorderLayout(0, 6));
        consolePanel.setOpaque(false);
        consolePanel.setBorder(BorderFactory.createTitledBorder("📟 Consola de Ejecución en Vivo"));

        txtConsole = new JTextArea();
        txtConsole.setEditable(false);
        txtConsole.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtConsole.setBackground(new Color(20, 24, 33));
        txtConsole.setForeground(new Color(220, 225, 235));
        txtConsole.setMargin(new Insets(10, 10, 10, 10));
        txtConsole.setText("Bienvenido al Centro de Patrones de Diseño GoF.\nSeleccione cualquier patrón a la izquierda y presione 'Ejecutar Demostración' para ver el flujo en tiempo real.\n\n");

        JButton btnClear = UiHelper.createButton("Limpiar Consola", "🧹", Color.DARK_GRAY);
        btnClear.addActionListener(e -> txtConsole.setText(""));

        consolePanel.add(new JScrollPane(txtConsole), BorderLayout.CENTER);
        consolePanel.add(btnClear, BorderLayout.SOUTH);

        split.setRightComponent(consolePanel);

        add(split, BorderLayout.CENTER);
    }

    private JPanel createFacadePanel() {
        return createPatternCard(
                "facade",
                "Patrón Facade (Fachada)",
                "Simplifica el acceso a un sistema complejo con una sola interfaz unificada (Metáfora: Mesero en restaurante).",
                "Coordina en un solo método transaccional: validación de cliente, comprobación de crédito, stock en bodega, creación atómica de Pedido y Detalles, deducción de inventario y registro de cobro.",
                "▶ Ejecutar Checkout Atómico con Fachada",
                this::runFacadeDemo
        );
    }

    private JPanel createAdapterPanel() {
        return createPatternCard(
                "adapter",
                "Patrón Adapter (Adaptador)",
                "Traduce interfaces incompatibles para que puedan colaborar (Metáfora: Enchufe y adaptador de viaje).",
                "Permite que el sistema acepte pasarelas de pago externas heterogéneas como PayPal y Stripe traduciéndolas a la entidad unificada Pago de dominio.",
                "▶ Probar Adaptadores PayPal & Stripe",
                this::runAdapterDemo
        );
    }

    private JPanel createDecoratorPanel() {
        return createPatternCard(
                "decorator",
                "Patrón Decorator (Decorador)",
                "Añade responsabilidades a los objetos dinámicamente en capas (Metáfora: Arepa base con queso, huevo y chorizo).",
                "Calcula importes comerciales en capas sucesivas: Precio Base ➔ Descuento por Volumen (-10%) ➔ Promoción Primavera Verde (-5%) ➔ Recargo de IVA (+21%).",
                "▶ Calcular Precio por Capas Decorator",
                this::runDecoratorDemo
        );
    }

    private JPanel createCompositePanel() {
        return createPatternCard(
                "composite",
                "Patrón Composite (Compuesto)",
                "Estructuras de objetos en forma de árbol (Metáfora: Cajas de mudanza con cajas y objetos hojas).",
                "Modela artículos individuales (Hojas) y kits/paquetes combinados (Compuestos) con cálculo automático de precio con descuento de kit y stock disponible ensamblable.",
                "▶ Ver Árbol de Kits Compuestos",
                this::runCompositeDemo
        );
    }

    private JPanel createProxyPanel() {
        return createPatternCard(
                "proxy",
                "Patrón Proxy (Intermediario)",
                "Controla el acceso al objeto real, agregando caché en memoria y auditoría (Metáfora: Portero de seguridad).",
                "Intercepta las búsquedas de productos en MySQL. Si el producto ya está en RAM, lo devuelve instantáneamente (Cache Hit); si no, delega al repositorio JDBC y lo almacena.",
                "▶ Consultar Producto con Proxy Cache",
                this::runProxyDemo
        );
    }

    private JPanel createFlyweightPanel() {
        return createPatternCard(
                "flyweight",
                "Patrón Flyweight (Peso Ligero)",
                "Comparte recursos intrínsecos pesados para ahorrar memoria (Metáfora: Bosque de árboles compartiendo textura 3D).",
                "Reutiliza instancias compartidas inmutables para las familias botánicas (ícono, colores, descripción general) para miles de productos en el catálogo.",
                "▶ Inspeccionar Flyweight Pool",
                this::runFlyweightDemo
        );
    }

    private JPanel createBridgePanel() {
        return createPatternCard(
                "bridge",
                "Patrón Bridge (Puente)",
                "Desacopla una abstracción de su implementación para que puedan variar independientemente (Metáfora: Control remoto vs TV/Radio).",
                "La abstracción Reporte Ejecutivo se comunica mediante un puente con implementadores concretos para exportar en CSV, HTML moderno y JSON sin modificar el modelo de reporte.",
                "▶ Generar Reporte con el Puente (CSV/HTML/JSON)",
                this::runBridgeDemo
        );
    }

    private JPanel createPatternCard(String imgKey, String title, String subtitle, String description, String buttonText, Runnable action) {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));
        panel.setOpaque(false);

        // Imagen de la infografía de referencia
        JLabel lblImg = ImageHelper.createPatternImageThumbnail(imgKey, 480, 240);
        panel.add(lblImg, BorderLayout.NORTH);

        // Texto descriptivo
        JPanel textPanel = new JPanel(new GridLayout(3, 1, 4, 4));
        textPanel.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 15));

        JLabel lblSub = new JLabel("💡 " + subtitle);
        lblSub.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblSub.setForeground(UiHelper.COLOR_PRIMARY);

        JLabel lblDesc = new JLabel("<html>" + description + "</html>");
        lblDesc.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblDesc.setForeground(new Color(170, 175, 185));

        textPanel.add(lblTitle);
        textPanel.add(lblSub);
        textPanel.add(lblDesc);

        panel.add(textPanel, BorderLayout.CENTER);

        // Botón de acción interactivo
        JButton btnAction = UiHelper.createButton(buttonText, "⚡", UiHelper.COLOR_PRIMARY);
        btnAction.addActionListener(e -> action.run());

        panel.add(btnAction, BorderLayout.SOUTH);

        return panel;
    }

    private void runFacadeDemo() {
        logToConsole("==================================================");
        logToConsole("🚀 EJECUTANDO DEMOSTRACIÓN: PATRÓN FACADE");
        logToConsole("==================================================");
        try {
            JardineriaBusinessFacade facade = registry.getBusinessFacade();
            CheckoutRequest req = new CheckoutRequest(
                    1,
                    List.of(
                            new CheckoutRequest.ItemCheckout("FR-001", 2, new BigDecimal("25.00")),
                            new CheckoutRequest.ItemCheckout("AR-001", 12, new BigDecimal("4.50"))
                    ),
                    "PayPal",
                    "TX-FACADE-" + System.currentTimeMillis(),
                    true
            );

            logToConsole("Petición recibida por la Fachada: Cliente #1, 2 productos, pago PayPal, promo primavera: true");
            CheckoutResult result = facade.procesarCheckoutCompleto(req);

            if (result.exito()) {
                logToConsole("✔ Éxito en Checkout coordinado por la Fachada:");
                logToConsole("  • Pedido generado: #" + result.pedido().getCodigoPedido() + " (" + result.pedido().getEstado() + ")");
                logToConsole("  • Pago procesado: " + result.pago().getFormaPago() + " (" + result.pago().getIdTransaccion() + ")");
                logToConsole("  • Total Facturado: " + UiHelper.formatCurrency(result.totalFacturado()));
                logToConsole("\nDesglose de cálculo de precios:");
                for (String line : result.desglosePrecios()) {
                    logToConsole("    " + line);
                }
            } else {
                logToConsole("❌ Resultado del Checkout: " + result.mensaje());
            }
        } catch (Exception e) {
            logToConsole("❌ Error en demo Facade: " + e.getMessage());
        }
    }

    private void runAdapterDemo() {
        logToConsole("==================================================");
        logToConsole("🔌 EJECUTANDO DEMOSTRACIÓN: PATRÓN ADAPTER");
        logToConsole("==================================================");
        try {
            PayPalAdapter payPalAdapter = new PayPalAdapter(null);
            var pagoPayPal = payPalAdapter.procesarCobro(1, "TX-PP-4455", new BigDecimal("89.90"));
            logToConsole("1. Adaptador PayPal:");
            logToConsole("   - Payload externo recibido y traducido a entidad Pago.");
            logToConsole("   - ID Generado: " + pagoPayPal.getIdTransaccion());
            logToConsole("   - Importe: " + UiHelper.formatCurrency(pagoPayPal.getTotal()));

            StripeAdapter stripeAdapter = new StripeAdapter(null);
            var pagoStripe = stripeAdapter.procesarCobro(2, "TX-STRIPE-8899", new BigDecimal("145.00"));
            logToConsole("\n2. Adaptador Stripe:");
            logToConsole("   - Cargo en micro-unidades traducido al dominio.");
            logToConsole("   - ID Generado: " + pagoStripe.getIdTransaccion());
            logToConsole("   - Importe: " + UiHelper.formatCurrency(pagoStripe.getTotal()));
            logToConsole("✔ Ambas pasarelas interoperan sin modificar el dominio.");
        } catch (Exception e) {
            logToConsole("❌ Error en demo Adapter: " + e.getMessage());
        }
    }

    private void runDecoratorDemo() {
        logToConsole("==================================================");
        logToConsole("🫓 EJECUTANDO DEMOSTRACIÓN: PATRÓN DECORATOR");
        logToConsole("==================================================");
        PriceCalculator calc = new BasePriceCalculator();
        calc = new BulkDiscountDecorator(calc);
        calc = new SeasonalPromoDecorator(calc);
        calc = new VatTaxDecorator(calc);

        BigDecimal unitario = new BigDecimal("15.00");
        int cantidad = 15;
        logToConsole(String.format("Calculando precio para %d unidades de producto a %s €/ud:", cantidad, unitario));

        BigDecimal finalPrice = calc.calcularTotal(unitario, cantidad);
        logToConsole("\nCapas acumuladas aplicadas:");
        for (String paso : calc.getDesglose()) {
            logToConsole("  • " + paso);
        }
        logToConsole("\nTotal Final con Decoradores: " + UiHelper.formatCurrency(finalPrice));
    }

    private void runCompositeDemo() {
        logToConsole("==================================================");
        logToConsole("📦 EJECUTANDO DEMOSTRACIÓN: PATRÓN COMPOSITE");
        logToConsole("==================================================");
        try {
            var p1 = registry.getProductoService().listarTodos().stream().findFirst().orElse(null);
            if (p1 == null) {
                logToConsole("No hay productos en BD.");
                return;
            }

            ProductLeaf hoja1 = new ProductLeaf(p1);
            ProductBundleComposite kitJardin = new ProductBundleComposite("KIT-PRIMAVERA", "Kit Integral Primavera", new BigDecimal("0.15"));
            kitJardin.agregarComponente(hoja1);

            StringBuilder sb = new StringBuilder();
            kitJardin.renderEstructura(sb, "");
            logToConsole("Estructura de Componente Compuesto renderizada:");
            logToConsole(sb.toString());
            logToConsole("✔ El cliente trata de forma idéntica a un producto individual o a un lote compuesto.");
        } catch (Exception e) {
            logToConsole("❌ Error: " + e.getMessage());
        }
    }

    private void runProxyDemo() {
        logToConsole("==================================================");
        logToConsole("🛡️ EJECUTANDO DEMOSTRACIÓN: PATRÓN PROXY");
        logToConsole("==================================================");
        try {
            CachedProductLookupProxy proxy = registry.getCachedProductProxy();
            String cod = "FR-001";

            logToConsole("Llamada 1 para producto '" + cod + "':");
            long t1 = System.nanoTime();
            var p1 = proxy.buscarPorCodigo(cod);
            long t2 = System.nanoTime();
            logToConsole("   - Resultado: " + (p1.isPresent() ? p1.get().getNombre() : "No encontrado"));
            logToConsole("   - Tiempo: " + ((t2 - t1) / 1000) + " µs (Consulta a BD MySQL)");

            logToConsole("\nLlamada 2 para producto '" + cod + "' (Inmediatamente después):");
            long t3 = System.nanoTime();
            var p2 = proxy.buscarPorCodigo(cod);
            long t4 = System.nanoTime();
            logToConsole("   - Resultado: " + (p2.isPresent() ? p2.get().getNombre() : "No encontrado"));
            logToConsole("   - Tiempo: " + ((t4 - t3) / 1000) + " µs (Servido desde Caché RAM del Proxy)");

            logToConsole(String.format("\nMétricas del Proxy: Hits=%d | Misses=%d | Hit Ratio=%.1f%%",
                    proxy.getCacheHits(), proxy.getCacheMisses(), proxy.getHitRatio()));

            lblProxyStats.setText(String.format("Proxy Cache: %d Hits | %d Misses (%.1f%%)",
                    proxy.getCacheHits(), proxy.getCacheMisses(), proxy.getHitRatio()));
        } catch (Exception e) {
            logToConsole("❌ Error: " + e.getMessage());
        }
    }

    private void runFlyweightDemo() {
        logToConsole("==================================================");
        logToConsole("🌲 EJECUTANDO DEMOSTRACIÓN: PATRÓN FLYWEIGHT");
        logToConsole("==================================================");
        GamaFlyweight f1 = GamaFlyweightFactory.getFlyweight("Aromáticas");
        GamaFlyweight f2 = GamaFlyweightFactory.getFlyweight("aromáticas");
        GamaFlyweight f3 = GamaFlyweightFactory.getFlyweight("Herramientas");

        logToConsole("Instancia f1 (Aromáticas): " + System.identityHashCode(f1));
        logToConsole("Instancia f2 (aromáticas): " + System.identityHashCode(f2));
        logToConsole("¿Son la misma referencia física en RAM?: " + (f1 == f2 ? "✔ SÍ (Flyweight compartido)" : "NO"));
        logToConsole("Instancia f3 (Herramientas): " + System.identityHashCode(f3));
        logToConsole("Total de Flyweights compartidos en el pool: " + GamaFlyweightFactory.getFlyweightsCount());
    }

    private void runBridgeDemo() {
        logToConsole("==================================================");
        logToConsole("🌉 EJECUTANDO DEMOSTRACIÓN: PATRÓN BRIDGE");
        logToConsole("==================================================");
        List<String> columnas = List.of("Código Pedido", "Estado", "Cliente", "Importe");
        List<List<String>> filas = List.of(
                List.of("101", "Entregado", "Jardines del Sur", "150.00 €"),
                List.of("102", "Pendiente", "Viveros Madrid", "420.50 €")
        );

        ReportBridge csvBridge = new ExecutiveReport(new CsvOutputImplementor(), "Reporte de Ventas", "Periodo 2026", columnas);
        ReportBridge jsonBridge = new ExecutiveReport(new JsonOutputImplementor(), "Reporte de Ventas", "Periodo 2026", columnas);

        logToConsole("1. Salida generada mediante CsvOutputImplementor:");
        logToConsole(csvBridge.generarReporte(filas));

        logToConsole("2. Salida generada mediante JsonOutputImplementor:");
        logToConsole(jsonBridge.generarReporte(filas));
        logToConsole("✔ La abstracción del reporte no conoce los detalles de los formatos.");
    }

    private void logToConsole(String text) {
        txtConsole.append(text + "\n");
        txtConsole.setCaretPosition(txtConsole.getDocument().getLength());
    }
}
