package com.jardineria.application.service;

import com.jardineria.application.dto.DashboardMetrics;
import com.jardineria.application.port.in.AnalyticsUseCase;
import com.jardineria.application.port.in.AsistenteUseCase;
import com.jardineria.application.port.in.ClienteUseCase;
import com.jardineria.application.port.in.DashboardUseCase;
import com.jardineria.application.port.in.DetallePedidoUseCase;
import com.jardineria.application.port.in.PedidoUseCase;
import com.jardineria.application.port.in.ProductoUseCase;
import com.jardineria.application.port.out.DocumentacionPort;
import com.jardineria.application.port.out.ModeloLenguajePort;
import com.jardineria.application.port.out.ModeloLenguajePort.Herramienta;
import com.jardineria.application.port.out.ModeloLenguajePort.Llamada;
import com.jardineria.application.port.out.ModeloLenguajePort.Mensaje;
import com.jardineria.application.port.out.ModeloLenguajePort.Parametro;
import com.jardineria.application.service.rag.DocumentIndex;
import com.jardineria.domain.model.Cliente;
import com.jardineria.domain.model.DetallePedido;
import com.jardineria.domain.model.Pedido;
import com.jardineria.domain.model.Producto;
import com.jardineria.domain.model.analytics.CategoryMetric;
import com.jardineria.domain.model.analytics.ClienteFinancialSummary;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Asistente con recuperación de documentación (RAG) y herramientas de solo lectura.
 * <p>
 * El modelo decide qué consultar; este servicio ejecuta las herramientas contra los casos de uso
 * existentes y le devuelve texto compacto. Ninguna herramienta escribe: el asistente puede
 * explicar cómo cambiar un pedido, pero nunca lo cambia.
 */
public class AsistenteService implements AsistenteUseCase {
    static final int MAX_ITERACIONES = 6;
    private static final int MAX_FILAS = 25;
    private static final int MAX_HISTORIAL = 12;
    private static final int MAX_FUENTES = 3;

    private final ModeloLenguajePort modelo;
    private final DocumentacionPort documentacion;
    private final ClienteUseCase clientes;
    private final PedidoUseCase pedidos;
    private final DetallePedidoUseCase detalles;
    private final ProductoUseCase productos;
    private final DashboardUseCase dashboard;
    private final AnalyticsUseCase analytics;
    private final Clock clock;
    private final Map<String, Function<Map<String, Object>, String>> ejecutores;
    private final List<Herramienta> herramientas;
    private volatile DocumentIndex indice;

    public AsistenteService(ModeloLenguajePort modelo, DocumentacionPort documentacion,
                            ClienteUseCase clientes, PedidoUseCase pedidos, DetallePedidoUseCase detalles,
                            ProductoUseCase productos, DashboardUseCase dashboard, AnalyticsUseCase analytics,
                            Clock clock) {
        this.modelo = Objects.requireNonNull(modelo);
        this.documentacion = Objects.requireNonNull(documentacion);
        this.clientes = clientes;
        this.pedidos = pedidos;
        this.detalles = detalles;
        this.productos = productos;
        this.dashboard = dashboard;
        this.analytics = analytics;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
        this.ejecutores = Map.of(
                "buscar_documentacion", this::buscarDocumentacion,
                "metricas_generales", a -> metricasGenerales(),
                "buscar_clientes", this::buscarClientes,
                "resumen_cliente", this::resumenCliente,
                "buscar_pedidos", this::buscarPedidos,
                "detalle_pedido", this::detallePedido,
                "buscar_productos", this::buscarProductos,
                "top_clientes", this::topClientes);
        this.herramientas = definiciones();
    }

    @Override
    public boolean disponible() {
        return modelo.configurado();
    }

    @Override
    public String descripcionProveedor() {
        return modelo.configurado() ? modelo.descripcion() : "Sin configurar: define la variable de entorno GROQ_API_KEY";
    }

    @Override
    public Respuesta preguntar(List<Turno> historial, String pregunta) {
        if (!disponible()) {
            throw new IllegalStateException("El asistente no está configurado: define la variable de entorno GROQ_API_KEY.");
        }
        if (pregunta == null || pregunta.isBlank()) {
            throw new IllegalArgumentException("Escribe una pregunta.");
        }
        List<Mensaje> conversacion = new ArrayList<>();
        conversacion.add(Mensaje.sistema(promptSistema()));
        List<Turno> previos = historial == null ? List.of() : historial;
        for (Turno t : previos.subList(Math.max(0, previos.size() - MAX_HISTORIAL), previos.size())) {
            conversacion.add(t.delUsuario() ? Mensaje.usuario(t.texto()) : Mensaje.asistente(t.texto(), null));
        }
        conversacion.add(Mensaje.usuario(pregunta.strip()));

        Set<String> fuentes = new LinkedHashSet<>();
        List<String> usadas = new ArrayList<>();
        for (int i = 0; i < MAX_ITERACIONES; i++) {
            ModeloLenguajePort.Respuesta r = modelo.completar(conversacion, herramientas);
            if (!r.pideHerramientas()) {
                String texto = r.texto() == null || r.texto().isBlank()
                        ? "No he podido elaborar una respuesta. Prueba a reformular la pregunta."
                        : r.texto().strip();
                return new Respuesta(texto, fuentes.stream().limit(MAX_FUENTES).toList(), usadas);
            }
            conversacion.add(Mensaje.asistente(r.texto(), r.llamadas()));
            for (Llamada llamada : r.llamadas()) {
                usadas.add(llamada.nombre());
                conversacion.add(Mensaje.resultado(llamada, ejecutar(llamada, fuentes)));
            }
        }
        return new Respuesta("He necesitado demasiadas consultas para responder. Prueba con una pregunta más concreta.",
                fuentes.stream().limit(MAX_FUENTES).toList(), usadas);
    }

    /** Ejecuta una herramienta; los errores vuelven al modelo como texto para que pueda corregirse. */
    String ejecutar(Llamada llamada, Set<String> fuentes) {
        Function<Map<String, Object>, String> f = ejecutores.get(llamada.nombre());
        if (f == null) {
            return "Error: la herramienta «" + llamada.nombre() + "» no existe.";
        }
        try {
            String out = f.apply(llamada.argumentos());
            if ("buscar_documentacion".equals(llamada.nombre())) {
                out.lines().filter(l -> l.startsWith("[Fuente: "))
                        .map(l -> l.substring(9, l.length() - 1)).forEach(fuentes::add);
            }
            return out;
        } catch (RuntimeException e) {
            return "Error: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private String promptSistema() {
        return """
                Eres el asistente de la aplicación de escritorio «Gestión Jardinería», un sistema para \
                gestionar clientes, pedidos, productos, pagos, empleados y oficinas de una empresa de jardinería.
                Fecha de hoy: %s.

                Reglas:
                - Responde siempre en español, breve y en texto plano: sin Markdown, sin tablas y sin emojis. \
                Usa guiones para las listas.
                - Para cualquier dato del negocio (cifras, clientes, pedidos, stock) consulta las herramientas; \
                nunca inventes valores. Si una herramienta no devuelve nada, dilo.
                - Para preguntas sobre cómo usar la aplicación (atajos, pantallas, temas, instalación) usa \
                buscar_documentacion y basa tu respuesta en los fragmentos; si no aparecen, di que no lo sabes.
                - Escribe los pedidos como #<número> (por ejemplo #14) para que la interfaz los enlace.
                - Importes en euros con dos decimales.
                - Solo puedes consultar: si te piden modificar datos, explica cómo hacerlo desde la aplicación.
                """.formatted(LocalDate.now(clock));
    }

    // ---------------------------------------------------------------- herramientas

    private static List<Herramienta> definiciones() {
        return List.of(
                new Herramienta("buscar_documentacion",
                        "Busca en la documentación de la aplicación (README y guías): uso, atajos de teclado, pantallas, temas, instalación, arquitectura.",
                        List.of(Parametro.texto("consulta", "Palabras clave de lo que se quiere saber", true))),
                new Herramienta("metricas_generales",
                        "Totales del negocio: clientes, pedidos por estado, productos, empleados, oficinas, ingresos cobrados y productos con poco stock.",
                        List.of()),
                new Herramienta("buscar_clientes",
                        "Busca clientes por nombre, contacto, ciudad o país. Devuelve código, nombre, ciudad, país y límite de crédito.",
                        List.of(Parametro.texto("texto", "Texto a buscar (vacío para listar)", false))),
                new Herramienta("resumen_cliente",
                        "Ficha financiera de un cliente: pedidos, total comprado, pagado, saldo pendiente y uso del crédito, más sus últimos pedidos.",
                        List.of(Parametro.entero("codigo_cliente", "Código del cliente", true))),
                new Herramienta("buscar_pedidos",
                        "Lista pedidos filtrando por estado y/o cliente, del más reciente al más antiguo.",
                        List.of(new Parametro("estado", "string", "Estado del pedido", false,
                                        List.of("Pendiente", "Entregado", "Rechazado")),
                                Parametro.entero("codigo_cliente", "Código del cliente", false),
                                Parametro.entero("limite", "Máximo de pedidos (por defecto 15)", false))),
                new Herramienta("detalle_pedido",
                        "Datos de un pedido con sus líneas (producto, cantidad, precio) y el importe total.",
                        List.of(Parametro.entero("codigo_pedido", "Número del pedido", true))),
                new Herramienta("buscar_productos",
                        "Busca productos por nombre o gama, o los que tienen poco stock. Devuelve código, nombre, gama, stock y precio.",
                        List.of(Parametro.texto("texto", "Nombre o gama a buscar", false),
                                Parametro.entero("stock_maximo", "Solo productos con stock menor o igual a este valor", false))),
                new Herramienta("top_clientes",
                        "Clientes que más han comprado, por importe.",
                        List.of(Parametro.entero("limite", "Cuántos clientes (por defecto 5)", false))));
    }

    private DocumentIndex indice() {
        DocumentIndex i = indice;
        if (i == null) {
            synchronized (this) {
                if (indice == null) {
                    indice = new DocumentIndex(documentacion.cargar());
                }
                i = indice;
            }
        }
        return i;
    }

    private String buscarDocumentacion(Map<String, Object> a) {
        String consulta = texto(a, "consulta");
        if (consulta.isBlank()) {
            return "Error: falta la consulta.";
        }
        var resultados = indice().buscar(consulta, 4);
        if (resultados.isEmpty()) {
            return "No hay fragmentos de documentación relacionados con «" + consulta + "».";
        }
        return resultados.stream()
                .map(r -> "[Fuente: " + r.fragmento().fuente() + " · " + r.fragmento().seccion() + "]\n"
                        + r.fragmento().texto())
                .collect(Collectors.joining("\n\n"));
    }

    private String metricasGenerales() {
        DashboardMetrics m = dashboard.obtenerMetricas();
        return """
                Clientes: %d
                Pedidos: %d (entregados %d, pendientes %d, rechazados %d; tasa de entrega %.0f %%)
                Productos: %d (con poco stock: %d)
                Empleados: %d · Oficinas: %d
                Ingresos cobrados: %s €""".formatted(
                m.totalClientes(), m.totalPedidos(), m.pedidosEntregados(), m.pedidosPendientes(),
                m.pedidosRechazados(), m.tasaEntrega() * 100, m.totalProductos(), m.totalBajoStock(),
                m.totalEmpleados(), m.totalOficinas(), euros(m.totalIngresos()));
    }

    private String buscarClientes(Map<String, Object> a) {
        String q = plano(texto(a, "texto"));
        List<Cliente> encontrados = clientes.listarTodos().stream()
                .filter(c -> q.isEmpty() || plano(String.join(" ", nz(c.getNombreCliente()), nz(c.getNombreContacto()),
                        nz(c.getApellidoContacto()), nz(c.getCiudad()), nz(c.getPais()))).contains(q))
                .toList();
        if (encontrados.isEmpty()) {
            return "No hay clientes que coincidan.";
        }
        String filas = encontrados.stream().limit(MAX_FILAS)
                .map(c -> "- %d · %s · %s, %s · límite %s €".formatted(c.getCodigoCliente(), c.getNombreCliente(),
                        nz(c.getCiudad()), nz(c.getPais()), euros(c.getLimiteCredito())))
                .collect(Collectors.joining("\n"));
        return encontrados.size() + " clientes" + (encontrados.size() > MAX_FILAS ? " (se muestran " + MAX_FILAS + ")" : "")
                + ":\n" + filas;
    }

    private String resumenCliente(Map<String, Object> a) {
        int codigo = entero(a, "codigo_cliente", -1);
        Cliente c = clientes.obtenerPorId(codigo);
        if (c == null) {
            return "No existe el cliente " + codigo + ".";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Cliente %d · %s · contacto %s · %s, %s%n".formatted(codigo, c.getNombreCliente(),
                nz(c.getContactoCompleto()), nz(c.getCiudad()), nz(c.getPais())));
        analytics.resumenCliente(codigo).ifPresent((ClienteFinancialSummary s) -> sb.append(
                "Pedidos: %d · Comprado: %s € · Pagado: %s € · Saldo pendiente: %s € · Uso del crédito: %.0f %% de %s €%n"
                        .formatted(s.totalPedidos(), euros(s.totalComprado()), euros(s.totalPagado()),
                                euros(s.saldoPendiente()), s.usoCredito() * 100, euros(s.limiteCredito()))));
        List<Pedido> suyos = ordenar(pedidos.buscarPorCliente(codigo));
        sb.append(suyos.isEmpty() ? "Sin pedidos." : "Últimos pedidos:\n" + filasPedidos(suyos, 5));
        return sb.toString();
    }

    private String buscarPedidos(Map<String, Object> a) {
        String estado = texto(a, "estado");
        int cliente = entero(a, "codigo_cliente", -1);
        int limite = Math.min(MAX_FILAS, Math.max(1, entero(a, "limite", 15)));
        List<Pedido> base = cliente > 0 ? pedidos.buscarPorCliente(cliente)
                : !estado.isBlank() ? pedidos.buscarPorEstado(estado) : pedidos.listarTodos();
        List<Pedido> filtrados = ordenar(base.stream()
                .filter(p -> estado.isBlank() || estado.equalsIgnoreCase(nz(p.getEstado()).trim()))
                .toList());
        if (filtrados.isEmpty()) {
            return "No hay pedidos con esos filtros.";
        }
        return filtrados.size() + " pedidos" + (filtrados.size() > limite ? " (se muestran " + limite + ")" : "")
                + ":\n" + filasPedidos(filtrados, limite);
    }

    private String detallePedido(Map<String, Object> a) {
        int codigo = entero(a, "codigo_pedido", -1);
        Pedido p = pedidos.obtenerPorId(codigo);
        if (p == null) {
            return "No existe el pedido " + codigo + ".";
        }
        Cliente c = clienteOpcional(p.getCodigoCliente());
        List<DetallePedido> lineas = detalles.listarPorPedido(codigo);
        BigDecimal total = lineas.stream().map(DetallePedido::getSubtotal).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        StringBuilder sb = new StringBuilder(filaPedido(p)).append('\n');
        sb.append("Cliente: ").append(c == null ? p.getCodigoCliente() : c.getCodigoCliente() + " · " + c.getNombreCliente())
                .append('\n');
        if (p.getComentarios() != null && !p.getComentarios().isBlank()) {
            sb.append("Comentarios: ").append(p.getComentarios().strip()).append('\n');
        }
        sb.append(lineas.size()).append(" líneas · total ").append(euros(total)).append(" €\n");
        lineas.stream().limit(MAX_FILAS).forEach(d -> sb.append("- %s × %d a %s €%n".formatted(
                d.getCodigoProducto(), d.getCantidad(), euros(d.getPrecioUnidad()))));
        return sb.toString().strip();
    }

    private String buscarProductos(Map<String, Object> a) {
        String q = plano(texto(a, "texto"));
        int stockMax = entero(a, "stock_maximo", -1);
        List<Producto> base = stockMax >= 0 ? productos.buscarBajoStock(stockMax) : productos.listarTodos();
        List<Producto> encontrados = base.stream()
                .filter(p -> stockMax < 0 || p.getCantidadEnStock() == null || p.getCantidadEnStock() <= stockMax)
                .filter(p -> q.isEmpty() || plano(nz(p.getNombre()) + " " + nz(p.getGama())).contains(q))
                .sorted(Comparator.comparing(p -> p.getCantidadEnStock() == null ? 0 : p.getCantidadEnStock()))
                .toList();
        if (encontrados.isEmpty()) {
            return "No hay productos que coincidan.";
        }
        String filas = encontrados.stream().limit(MAX_FILAS)
                .map(p -> "- %s · %s · %s · stock %s · %s €".formatted(p.getCodigoProducto(), p.getNombre(),
                        nz(p.getGama()), p.getCantidadEnStock(), euros(p.getPrecioVenta())))
                .collect(Collectors.joining("\n"));
        return encontrados.size() + " productos" + (encontrados.size() > MAX_FILAS ? " (se muestran " + MAX_FILAS + ")" : "")
                + ":\n" + filas;
    }

    private String topClientes(Map<String, Object> a) {
        int limite = Math.min(MAX_FILAS, Math.max(1, entero(a, "limite", 5)));
        List<CategoryMetric> top = analytics.topClientes(limite);
        if (top.isEmpty()) {
            return "Todavía no hay ventas.";
        }
        return top.stream()
                .map(m -> "- %s · %s € en %d pedidos".formatted(m.label(), euros(m.amount()), m.count()))
                .collect(Collectors.joining("\n"));
    }

    // ---------------------------------------------------------------- utilidades

    private static List<Pedido> ordenar(List<Pedido> lista) {
        return lista.stream()
                .sorted(Comparator.comparing(Pedido::getFechaPedido, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Pedido::getCodigoPedido, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private static String filasPedidos(List<Pedido> lista, int limite) {
        return lista.stream().limit(limite).map(p -> "- " + filaPedido(p)).collect(Collectors.joining("\n"));
    }

    private static String filaPedido(Pedido p) {
        return "#%d · %s · pedido %s · esperado %s%s · cliente %d".formatted(p.getCodigoPedido(), nz(p.getEstado()).trim(),
                p.getFechaPedido(), p.getFechaEsperada(),
                p.getFechaEntrega() == null || !"entregado".equalsIgnoreCase(nz(p.getEstado()).trim())
                        ? "" : " · entregado " + p.getFechaEntrega(), p.getCodigoCliente());
    }

    static String euros(BigDecimal v) {
        return String.format(Locale.ROOT, "%,.2f", v == null ? BigDecimal.ZERO : v)
                .replace(",", " ").replace('.', ',');
    }

    private static String texto(Map<String, Object> a, String clave) {
        Object v = a.get(clave);
        return v == null ? "" : v.toString().strip();
    }

    private static int entero(Map<String, Object> a, String clave, int defecto) {
        Object v = a.get(clave);
        if (v instanceof Number n) {
            return n.intValue();
        }
        if (v != null) {
            try {
                return Integer.parseInt(v.toString().strip().replace("#", ""));
            } catch (NumberFormatException ignored) {
                // valor no numérico: se usa el valor por defecto
            }
        }
        return defecto;
    }

    private Cliente clienteOpcional(Integer codigo) {
        try {
            return codigo == null ? null : clientes.obtenerPorId(codigo);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String plano(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).strip();
    }
}
