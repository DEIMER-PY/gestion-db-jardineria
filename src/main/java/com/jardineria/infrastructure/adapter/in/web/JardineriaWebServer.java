package com.jardineria.infrastructure.adapter.in.web;

import com.jardineria.application.service.DashboardService;
import com.jardineria.domain.model.Pedido;
import com.jardineria.domain.model.Producto;
import com.jardineria.infrastructure.ServiceRegistry;
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
 * Servidor Web Embebido ligero y reactivo basado en el HttpServer nativo de Java 21.
 * Expone la API REST operacional y sirve la interfaz Web moderna (estilo Electron / SaaS).
 */
public class JardineriaWebServer {
    private static final Logger log = LoggerFactory.getLogger(JardineriaWebServer.class);
    private static final int DEFAULT_PORT = 8080;

    private final ServiceRegistry registry;
    private HttpServer server;
    private int port;
    private boolean running = false;

    public JardineriaWebServer(ServiceRegistry registry) {
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
                // Fallback a cualquier puerto efímero disponible
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
        server.createContext("/api/patterns", new PatternsApiHandler());

        server.start();
        running = true;
        log.info("🚀 Servidor Web Embebido de Jardinería activo en http://localhost:{}/dashboard", port);
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

    // Handler para servir el HTML de la aplicación web
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

    // Handler para /api/metrics
    private class MetricsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                DashboardService.DashboardMetrics m = registry.getDashboardService().obtenerMetricas();
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
                sendResponse(exchange, 500, "application/json", "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // Handler para /api/orders
    private class OrdersApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                List<Pedido> pedidos = registry.getDashboardService().obtenerUltimosPedidos();
                StringBuilder json = new StringBuilder("[\n");
                for (int i = 0; i < pedidos.size(); i++) {
                    Pedido p = pedidos.get(i);
                    json.append(String.format("  {\"codigoPedido\": %d, \"fechaPedido\": \"%s\", \"fechaEsperada\": \"%s\", \"fechaEntrega\": \"%s\", \"estado\": \"%s\", \"codigoCliente\": %d}",
                            p.getCodigoPedido(),
                            p.getFechaPedido(),
                            p.getFechaEsperada(),
                            p.getFechaEntrega() != null ? p.getFechaEntrega() : "--",
                            p.getEstado(),
                            p.getCodigoCliente()
                    ));
                    if (i < pedidos.size() - 1) json.append(",");
                    json.append("\n");
                }
                json.append("]");
                sendResponse(exchange, 200, "application/json; charset=UTF-8", json.toString());
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // Handler para /api/products
    private class ProductsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                List<Producto> productos = registry.getProductoService().listarTodos();
                StringBuilder json = new StringBuilder("[\n");
                for (int i = 0; i < productos.size(); i++) {
                    Producto p = productos.get(i);
                    json.append(String.format("  {\"codigoProducto\": \"%s\", \"nombre\": \"%s\", \"gama\": \"%s\", \"stock\": %d, \"precioVenta\": %s, \"proveedor\": \"%s\"}",
                            p.getCodigoProducto(),
                            p.getNombre().replace("\"", "\\\""),
                            p.getGama() != null ? p.getGama() : "",
                            p.getCantidadEnStock(),
                            p.getPrecioVenta() != null ? p.getPrecioVenta().toString() : "0.00",
                            p.getProveedor() != null ? p.getProveedor().replace("\"", "\\\"") : ""
                    ));
                    if (i < productos.size() - 1) json.append(",");
                    json.append("\n");
                }
                json.append("]");
                sendResponse(exchange, 200, "application/json; charset=UTF-8", json.toString());
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // Handler para /api/patterns
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
