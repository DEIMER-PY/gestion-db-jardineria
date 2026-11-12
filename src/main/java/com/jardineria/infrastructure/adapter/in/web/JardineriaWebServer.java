package com.jardineria.infrastructure.adapter.in.web;

import com.jardineria.application.service.DashboardService;
import com.jardineria.domain.model.Cliente;
import com.jardineria.domain.model.Oficina;
import com.jardineria.domain.model.Pedido;
import com.jardineria.domain.model.Producto;
import com.jardineria.infrastructure.config.ApplicationContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Servidor Web Embebido reactivo basado en el HttpServer nativo de Java 21 y Virtual Threads.
 * Proporciona una API REST completa y sirve la interfaz Web moderna (estilo Electron / Tauri / SaaS).
 */
public class JardineriaWebServer {
    private static final Logger log = LoggerFactory.getLogger(JardineriaWebServer.class);

    private final ApplicationContext registry;
    private HttpServer server;
    private int port;
    private boolean running = false;

    public JardineriaWebServer(ApplicationContext registry) {
        this.registry = registry;
    }

    public synchronized void start() {
        if (running) return;
        int[] candidatePorts = {8080, 8081, 8082, 8083, 8084, 8085};
        for (int p : candidatePorts) {
            try {
                server = HttpServer.create(new InetSocketAddress(p), 0);
                this.port = p;
                break;
            } catch (IOException ignored) {
            }
        }

        if (server == null) {
            try {
                server = HttpServer.create(new InetSocketAddress(0), 0);
                this.port = server.getAddress().getPort();
            } catch (IOException e) {
                log.error("No se pudo iniciar el servidor web embebido: {}", e.getMessage());
                return;
            }
        }

        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        // Rutas estáticas y páginas
        server.createContext("/", new StaticPageHandler());
        server.createContext("/dashboard", new StaticPageHandler());

        // Endpoints REST API
        server.createContext("/api/metrics", new MetricsApiHandler());
        server.createContext("/api/orders", new OrdersApiHandler());
        server.createContext("/api/products", new ProductsApiHandler());
        server.createContext("/api/clients", new ClientsApiHandler());
        server.createContext("/api/offices", new OfficesApiHandler());
        server.createContext("/api/analytics", new AnalyticsApiHandler());
        server.createContext("/api/patterns", new PatternsApiHandler());
        server.createContext("/api/health", new HealthApiHandler());

        server.start();
        running = true;
        log.info("🚀 Servidor Web Embebido de Jardinería activo en http://localhost:{}/dashboard", port);
    }

    private static volatile JardineriaWebServer shared;

    /** Devuelve el servidor compartido, arrancándolo bajo demanda (sin efectos al construir el contexto). */
    public static synchronized JardineriaWebServer shared(ApplicationContext context) {
        if (shared == null) {
            shared = new JardineriaWebServer(context);
            shared.start();
            Runtime.getRuntime().addShutdownHook(new Thread(shared::stop));
        }
        return shared;
    }

    public synchronized void stop() {
        if (server != null && running) {
            server.stop(1);
            running = false;
            log.info("Servidor Web Embebido detenido.");
        }
    }

    public int getPort() {
        return port;
    }

    public boolean isRunning() {
        return running;
    }

    public String getDashboardUrl() {
        return "http://localhost:" + port + "/dashboard";
    }

    private static class StaticPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try (InputStream in = getClass().getResourceAsStream("/web/index.html")) {
                if (in == null) {
                    sendResponse(exchange, 404, "text/plain", "Archivo web/index.html no encontrado en recursos.");
                    return;
                }
                byte[] bytes = in.readAllBytes();
                sendResponseBytes(exchange, 200, "text/html; charset=UTF-8", bytes);
            }
        }
    }

    private class MetricsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                com.jardineria.application.dto.DashboardMetrics m = registry.getDashboardService().obtenerMetricas();
                long totalPedidos = m.totalPedidos();
                double pEntregados = (totalPedidos > 0) ? (m.pedidosEntregados() * 100.0) / totalPedidos : 0.0;
                double pPendientes = (totalPedidos > 0) ? (m.pedidosPendientes() * 100.0) / totalPedidos : 0.0;
                double pRechazados = (totalPedidos > 0) ? (m.pedidosRechazados() * 100.0) / totalPedidos : 0.0;

                String json = String.format(Locale.US, """
                        {
                          "totalClientes": %d,
                          "totalPedidos": %d,
                          "pedidosEntregados": %d,
                          "pedidosPendientes": %d,
                          "pedidosRechazados": %d,
                          "porcentajeEntregados": %.1f,
                          "porcentajePendientes": %.1f,
                          "porcentajeRechazados": %.1f,
                          "totalIngresos": %s,
                          "totalProductos": %d,
                          "totalBajoStock": %d,
                          "totalEmpleados": %d,
                          "totalOficinas": %d
                        }""",
                        m.totalClientes(),
                        m.totalPedidos(),
                        m.pedidosEntregados(),
                        m.pedidosPendientes(),
                        m.pedidosRechazados(),
                        pEntregados,
                        pPendientes,
                        pRechazados,
                        (m.totalIngresos() != null ? m.totalIngresos().toString() : "0.00"),
                        m.totalProductos(),
                        m.totalBajoStock(),
                        m.totalEmpleados(),
                        m.totalOficinas()
                );
                sendResponse(exchange, 200, "application/json; charset=UTF-8", json);
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\": \"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private class OrdersApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                List<Pedido> pedidos = registry.getDashboardService().obtenerUltimosPedidos(10);
                StringBuilder json = new StringBuilder("[\n");
                for (int i = 0; i < pedidos.size(); i++) {
                    Pedido p = pedidos.get(i);
                    json.append(String.format(Locale.US, "  {\"codigoPedido\": %d, \"fechaPedido\": \"%s\", \"fechaEsperada\": \"%s\", \"fechaEntrega\": \"%s\", \"estado\": \"%s\", \"codigoCliente\": %d}",
                            p.getCodigoPedido(),
                            p.getFechaPedido(),
                            p.getFechaEsperada(),
                            p.getFechaEntrega() != null ? p.getFechaEntrega() : "--",
                            escapeJson(p.getEstado()),
                            p.getCodigoCliente()
                    ));
                    if (i < pedidos.size() - 1) json.append(",");
                    json.append("\n");
                }
                json.append("]");
                sendResponse(exchange, 200, "application/json; charset=UTF-8", json.toString());
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\": \"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private class ProductsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                List<Producto> productos = registry.getProductoService().listarTodos();
                StringBuilder json = new StringBuilder("[\n");
                for (int i = 0; i < productos.size(); i++) {
                    Producto p = productos.get(i);
                    json.append(String.format(Locale.US, "  {\"codigoProducto\": \"%s\", \"nombre\": \"%s\", \"gama\": \"%s\", \"stock\": %d, \"precioVenta\": %s, \"proveedor\": \"%s\"}",
                            escapeJson(p.getCodigoProducto()),
                            escapeJson(p.getNombre()),
                            escapeJson(p.getGama() != null ? p.getGama() : ""),
                            p.getCantidadEnStock(),
                            p.getPrecioVenta() != null ? p.getPrecioVenta().toString() : "0.00",
                            escapeJson(p.getProveedor() != null ? p.getProveedor() : "")
                    ));
                    if (i < productos.size() - 1) json.append(",");
                    json.append("\n");
                }
                json.append("]");
                sendResponse(exchange, 200, "application/json; charset=UTF-8", json.toString());
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\": \"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private class ClientsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                List<Cliente> clientes = registry.getClienteService().listarTodos();
                StringBuilder json = new StringBuilder("[\n");
                for (int i = 0; i < clientes.size(); i++) {
                    Cliente c = clientes.get(i);
                    json.append(String.format(Locale.US, "  {\"codigoCliente\": %d, \"nombreCliente\": \"%s\", \"nombreContacto\": \"%s\", \"telefono\": \"%s\", \"ciudad\": \"%s\", \"pais\": \"%s\", \"limiteCredito\": %s}",
                            c.getCodigoCliente(),
                            escapeJson(c.getNombreCliente()),
                            escapeJson(c.getNombreContacto() != null ? c.getNombreContacto() : ""),
                            escapeJson(c.getTelefono() != null ? c.getTelefono() : ""),
                            escapeJson(c.getCiudad() != null ? c.getCiudad() : ""),
                            escapeJson(c.getPais() != null ? c.getPais() : ""),
                            c.getLimiteCredito() != null ? c.getLimiteCredito().toString() : "0.00"
                    ));
                    if (i < clientes.size() - 1) json.append(",");
                    json.append("\n");
                }
                json.append("]");
                sendResponse(exchange, 200, "application/json; charset=UTF-8", json.toString());
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\": \"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private class OfficesApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                List<Oficina> oficinas = registry.getOficinaService().listarTodas();
                StringBuilder json = new StringBuilder("[\n");
                for (int i = 0; i < oficinas.size(); i++) {
                    Oficina o = oficinas.get(i);
                    json.append(String.format(Locale.US, "  {\"codigoOficina\": \"%s\", \"ciudad\": \"%s\", \"pais\": \"%s\", \"telefono\": \"%s\", \"lineaDireccion1\": \"%s\"}",
                            escapeJson(o.getCodigoOficina()),
                            escapeJson(o.getCiudad()),
                            escapeJson(o.getPais()),
                            escapeJson(o.getTelefono()),
                            escapeJson(o.getLineaDireccion1() != null ? o.getLineaDireccion1() : "")
                    ));
                    if (i < oficinas.size() - 1) json.append(",");
                    json.append("\n");
                }
                json.append("]");
                sendResponse(exchange, 200, "application/json; charset=UTF-8", json.toString());
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\": \"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private class AnalyticsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                com.jardineria.application.dto.DashboardMetrics m = registry.getDashboardService().obtenerMetricas();
                double total = m.totalIngresos() != null ? m.totalIngresos().doubleValue() : 0.0;
                // Proyecciones mensuales realistas calculadas proporcionalmente
                double[] monthlyWeights = {0.06, 0.07, 0.09, 0.11, 0.12, 0.10, 0.08, 0.07, 0.09, 0.10, 0.08, 0.03};
                StringBuilder monthlyRev = new StringBuilder("[");
                for (int i = 0; i < monthlyWeights.length; i++) {
                    double val = total * monthlyWeights[i];
                    monthlyRev.append(String.format(Locale.US, "%.2f", val));
                    if (i < monthlyWeights.length - 1) monthlyRev.append(", ");
                }
                monthlyRev.append("]");

                String json = String.format(Locale.US, """
                        {
                          "orderStatus": {
                            "entregados": %d,
                            "pendientes": %d,
                            "rechazados": %d
                          },
                          "monthlyLabels": ["Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"],
                          "monthlyRevenue": %s,
                          "kpiSummary": {
                            "totalClientes": %d,
                            "totalPedidos": %d,
                            "totalProductos": %d,
                            "totalIngresos": %s
                          }
                        }""",
                        m.pedidosEntregados(),
                        m.pedidosPendientes(),
                        m.pedidosRechazados(),
                        monthlyRev.toString(),
                        m.totalClientes(),
                        m.totalPedidos(),
                        m.totalProductos(),
                        (m.totalIngresos() != null ? m.totalIngresos().toString() : "0.00")
                );
                sendResponse(exchange, 200, "application/json; charset=UTF-8", json);
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\": \"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private class PatternsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            long hits = registry.getCachedProductProxy().getCacheHits();
            long misses = registry.getCachedProductProxy().getCacheMisses();
            double hitRatio = registry.getCachedProductProxy().getHitRatio();

            String json = String.format(Locale.US, """
                    {
                      "patterns": [
                        {"name": "Facade", "type": "Estructural", "status": "Activo", "description": "Orquestador unificado de checkout atómico transaccional."},
                        {"name": "Adapter", "type": "Estructural", "status": "Activo", "description": "Traductores para pasarelas externas PayPal y Stripe."},
                        {"name": "Decorator", "type": "Estructural", "status": "Activo", "description": "Cálculo por capas: base + volumen + promo primavera + IVA."},
                        {"name": "Composite", "type": "Estructural", "status": "Activo", "description": "Kits y combos de jardinería con cálculo arbóreo."},
                        {"name": "Proxy", "type": "Estructural", "status": "Activo", "description": "Intermediario con caché en memoria (Hits: %d, Misses: %d, Ratio: %.1f%%)."},
                        {"name": "Flyweight", "type": "Estructural", "status": "Activo", "description": "Reserva de estados intrínsecos botánicos y paletas compartidas."},
                        {"name": "Bridge", "type": "Estructural", "status": "Activo", "description": "Desacoplamiento de reportes ejecutivos en CSV, HTML y JSON."}
                      ],
                      "proxyMetrics": {
                        "cacheHits": %d,
                        "cacheMisses": %d,
                        "hitRatio": %.1f
                      }
                    }""",
                    hits, misses, hitRatio, hits, misses, hitRatio
            );
            sendResponse(exchange, 200, "application/json; charset=UTF-8", json);
        }
    }

    private static class HealthApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Runtime rt = Runtime.getRuntime();
            long totalMem = rt.totalMemory() / (1024 * 1024);
            long freeMem = rt.freeMemory() / (1024 * 1024);
            long usedMem = totalMem - freeMem;

            String json = String.format(Locale.US, """
                    {
                      "status": "UP",
                      "javaVersion": "%s",
                      "memory": {
                        "usedMb": %d,
                        "totalMb": %d
                      },
                      "concurrency": "Virtual Threads (Project Loom)"
                    }""",
                    System.getProperty("java.version"),
                    usedMem,
                    totalMem
            );
            sendResponse(exchange, 200, "application/json; charset=UTF-8", json);
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static void sendResponse(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        sendResponseBytes(exchange, status, contentType, bytes);
    }

    private static void sendResponseBytes(HttpExchange exchange, int status, String contentType, byte[] bytes) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
