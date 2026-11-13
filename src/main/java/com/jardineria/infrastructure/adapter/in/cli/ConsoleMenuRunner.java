package com.jardineria.infrastructure.adapter.in.cli;

import com.jardineria.application.service.*;
import com.jardineria.domain.model.*;
import com.jardineria.infrastructure.config.ApplicationContext;
import com.jardineria.infrastructure.adapter.in.ui.components.UiHelper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

/**
 * Adaptador de entrada por consola con menú principal y submenús CRUD para todas las entidades.
 */
public class ConsoleMenuRunner {
    private final ApplicationContext registry;
    private final Scanner scanner;

    public ConsoleMenuRunner(ApplicationContext registry) {
        this.registry = registry;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        boolean salir = false;
        while (!salir) {
            printHeader("🌿 SISTEMA DE GESTIÓN DE JARDINERÍA — MENÚ PRINCIPAL");
            System.out.println("  [1] 📊 Ver Métricas del Dashboard Ejecutivo");
            System.out.println("  [2] 🏢 Gestión de Oficinas");
            System.out.println("  [3] 👥 Gestión de Empleados");
            System.out.println("  [4] 🛍️ Gestión de Clientes");
            System.out.println("  [5] 📦 Catálogo de Productos y Stock");
            System.out.println("  [6] 📑 Pedidos y Líneas de Detalle");
            System.out.println("  [7] 💳 Pagos y Finanzas");
            System.out.println("  [8] 🌿 Gamas de Producto");
            System.out.println("  [0] 🚪 Salir");
            System.out.print("\nSeleccione una opción: ");

            String opcion = scanner.nextLine().trim();
            switch (opcion) {
                case "1" -> mostrarDashboard();
                case "2" -> submenuOficinas();
                case "3" -> submenuEmpleados();
                case "4" -> submenuClientes();
                case "5" -> submenuProductos();
                case "6" -> submenuPedidos();
                case "7" -> submenuPagos();
                case "8" -> submenuGamas();
                case "0" -> {
                    System.out.println("\n¡Gracias por utilizar el sistema Jardinería! Hasta pronto.\n");
                    salir = true;
                }
                default -> System.out.println("\n❌ Opción no válida. Intente nuevamente.");
            }
        }
    }

    private void mostrarDashboard() {
        printHeader("📊 DASHBOARD EJECUTIVO Y OPERACIONAL");
        try {
            com.jardineria.application.dto.DashboardMetrics m = registry.getDashboardService().obtenerMetricas();
            System.out.printf("  • Clientes Registrados:        %d%n", m.totalClientes());
            System.out.printf("  • Pedidos Totales:              %d%n", m.totalPedidos());
            System.out.printf("    - Pedidos Entregados:         %d%n", m.pedidosEntregados());
            System.out.printf("    - Pedidos Pendientes:         %d%n", m.pedidosPendientes());
            System.out.printf("    - Pedidos Rechazados:         %d%n", m.pedidosRechazados());
            System.out.printf("  • Facturación Total Recaudada:  %s%n", UiHelper.formatCurrency(m.totalIngresos()));
            System.out.printf("  • Productos en Catálogo:        %d%n", m.totalProductos());
            System.out.printf("  • Alertas de Stock Bajo (< 10): %d productos%n", m.totalBajoStock());
            System.out.printf("  • Empleados Activos:            %d%n", m.totalEmpleados());
            System.out.printf("  • Oficinas Globales:            %d%n", m.totalOficinas());
        } catch (Exception e) {
            System.out.println("❌ Error al obtener métricas: " + e.getMessage());
        }
        pausar();
    }

    private void submenuOficinas() {
        boolean volver = false;
        com.jardineria.application.port.in.OficinaUseCase service = registry.getOficinaService();
        while (!volver) {
            printHeader("🏢 SUBMENÚ: GESTIÓN DE OFICINAS");
            System.out.println("  [1] Listar todas las oficinas");
            System.out.println("  [2] Buscar oficina por código");
            System.out.println("  [3] Filtrar oficinas por país");
            System.out.println("  [4] Registrar nueva oficina");
            System.out.println("  [5] Eliminar oficina");
            System.out.println("  [0] << Volver al menú principal");
            System.out.print("\nSeleccione opción: ");

            String op = scanner.nextLine().trim();
            switch (op) {
                case "1" -> {
                    List<Oficina> list = service.listarTodas();
                    System.out.printf("%n%-10s | %-20s | %-15s | %-15s%n", "CÓDIGO", "CIUDAD", "PAÍS", "TELÉFONO");
                    System.out.println("-".repeat(65));
                    for (Oficina o : list) {
                        System.out.printf("%-10s | %-20s | %-15s | %-15s%n",
                                o.getCodigoOficina(), o.getCiudad(), o.getPais(), o.getTelefono());
                    }
                    pausar();
                }
                case "2" -> {
                    System.out.print("Ingrese código de oficina (ej: BCN-ES): ");
                    String cod = scanner.nextLine().trim();
                    try {
                        Oficina o = service.obtenerPorId(cod);
                        System.out.println("Encontrada: " + o);
                    } catch (Exception e) {
                        System.out.println("❌ " + e.getMessage());
                    }
                    pausar();
                }
                case "3" -> {
                    System.out.print("Ingrese nombre del país: ");
                    String pais = scanner.nextLine().trim();
                    List<Oficina> list = service.buscarPorPais(pais);
                    for (Oficina o : list) {
                        System.out.printf("• %s - %s (%s)%n", o.getCodigoOficina(), o.getCiudad(), o.getPais());
                    }
                    pausar();
                }
                case "4" -> {
                    try {
                        System.out.print("Código de Oficina: ");
                        String cod = scanner.nextLine().trim();
                        System.out.print("Ciudad: ");
                        String ciudad = scanner.nextLine().trim();
                        System.out.print("País: ");
                        String pais = scanner.nextLine().trim();
                        System.out.print("Código Postal: ");
                        String cp = scanner.nextLine().trim();
                        System.out.print("Teléfono: ");
                        String tel = scanner.nextLine().trim();
                        System.out.print("Dirección 1: ");
                        String dir = scanner.nextLine().trim();

                        Oficina of = new Oficina(cod, ciudad, pais, null, cp, tel, dir, null);
                        service.crearOficina(of);
                        System.out.println("✔ Oficina creada con éxito.");
                    } catch (Exception e) {
                        System.out.println("❌ Error: " + e.getMessage());
                    }
                    pausar();
                }
                case "5" -> {
                    System.out.print("Ingrese código de oficina a eliminar: ");
                    String cod = scanner.nextLine().trim();
                    try {
                        service.eliminarOficina(cod);
                        System.out.println("✔ Oficina eliminada.");
                    } catch (Exception e) {
                        System.out.println("❌ Error: " + e.getMessage());
                    }
                    pausar();
                }
                case "0" -> volver = true;
                default -> System.out.println("Opción inválida.");
            }
        }
    }

    private void submenuEmpleados() {
        boolean volver = false;
        com.jardineria.application.port.in.EmpleadoUseCase service = registry.getEmpleadoService();
        while (!volver) {
            printHeader("👥 SUBMENÚ: GESTIÓN DE EMPLEADOS");
            System.out.println("  [1] Listar todos los empleados");
            System.out.println("  [2] Listar Directores Generales");
            System.out.println("  [3] Listar Representantes de Ventas");
            System.out.println("  [4] Buscar empleado por código");
            System.out.println("  [0] << Volver");
            System.out.print("\nSeleccione opción: ");

            String op = scanner.nextLine().trim();
            switch (op) {
                case "1" -> {
                    List<Empleado> list = service.listarTodos();
                    System.out.printf("%n%-5s | %-25s | %-25s | %-10s%n", "ID", "NOMBRE COMPLETO", "PUESTO", "OFICINA");
                    System.out.println("-".repeat(70));
                    for (Empleado e : list) {
                        System.out.printf("%-5d | %-25s | %-25s | %-10s%n",
                                e.getCodigoEmpleado(), e.getNombreCompleto(), e.getPuesto(), e.getCodigoOficina());
                    }
                    pausar();
                }
                case "2" -> {
                    List<Empleado> list = service.listarDirectores();
                    System.out.println("\nDirectores registrados:");
                    for (Empleado e : list) {
                        System.out.printf("• #%d %s - %s (%s)%n", e.getCodigoEmpleado(), e.getNombreCompleto(), e.getPuesto(), e.getEmail());
                    }
                    pausar();
                }
                case "3" -> {
                    List<Empleado> list = service.listarRepresentantesVentas();
                    System.out.println("\nRepresentantes de ventas:");
                    for (Empleado e : list) {
                        System.out.printf("• #%d %s - Oficina: %s%n", e.getCodigoEmpleado(), e.getNombreCompleto(), e.getCodigoOficina());
                    }
                    pausar();
                }
                case "4" -> {
                    System.out.print("Código de empleado: ");
                    try {
                        int cod = Integer.parseInt(scanner.nextLine().trim());
                        Empleado e = service.obtenerPorId(cod);
                        System.out.println("Empleado: " + e);
                    } catch (Exception ex) {
                        System.out.println("❌ " + ex.getMessage());
                    }
                    pausar();
                }
                case "0" -> volver = true;
                default -> System.out.println("Opción inválida.");
            }
        }
    }

    private void submenuClientes() {
        boolean volver = false;
        com.jardineria.application.port.in.ClienteUseCase service = registry.getClienteService();
        while (!volver) {
            printHeader("🛍️ SUBMENÚ: GESTIÓN DE CLIENTES");
            System.out.println("  [1] Listar todos los clientes");
            System.out.println("  [2] Buscar cliente por código");
            System.out.println("  [3] Filtrar clientes por país");
            System.out.println("  [0] << Volver");
            System.out.print("\nSeleccione opción: ");

            String op = scanner.nextLine().trim();
            switch (op) {
                case "1" -> {
                    List<Cliente> list = service.listarTodos();
                    System.out.printf("%n%-5s | %-25s | %-15s | %-12s | %-12s%n", "ID", "RAZÓN SOCIAL", "CIUDAD", "PAÍS", "LÍMITE CRÉD.");
                    System.out.println("-".repeat(75));
                    for (Cliente c : list) {
                        System.out.printf("%-5d | %-25s | %-15s | %-12s | %-12s%n",
                                c.getCodigoCliente(),
                                c.getNombreCliente().length() > 25 ? c.getNombreCliente().substring(0, 22) + "..." : c.getNombreCliente(),
                                c.getCiudad(), c.getPais(), UiHelper.formatCurrency(c.getLimiteCredito()));
                    }
                    pausar();
                }
                case "2" -> {
                    System.out.print("Código cliente: ");
                    try {
                        int id = Integer.parseInt(scanner.nextLine().trim());
                        Cliente c = service.obtenerPorId(id);
                        System.out.println("Cliente: " + c);
                    } catch (Exception ex) {
                        System.out.println("❌ " + ex.getMessage());
                    }
                    pausar();
                }
                case "3" -> {
                    System.out.print("País: ");
                    String pais = scanner.nextLine().trim();
                    List<Cliente> list = service.buscarPorPais(pais);
                    for (Cliente c : list) {
                        System.out.printf("• #%d %s - %s (%s)%n", c.getCodigoCliente(), c.getNombreCliente(), c.getCiudad(), c.getPais());
                    }
                    pausar();
                }
                case "0" -> volver = true;
                default -> System.out.println("Opción inválida.");
            }
        }
    }

    private void submenuProductos() {
        boolean volver = false;
        com.jardineria.application.port.in.ProductoUseCase service = registry.getProductoService();
        while (!volver) {
            printHeader("📦 SUBMENÚ: CATÁLOGO DE PRODUCTOS");
            System.out.println("  [1] Listar todos los productos");
            System.out.println("  [2] Filtrar por gama");
            System.out.println("  [3] Ver alertas de bajo stock (< 10 uds)");
            System.out.println("  [4] Buscar por nombre");
            System.out.println("  [0] << Volver");
            System.out.print("\nSeleccione opción: ");

            String op = scanner.nextLine().trim();
            switch (op) {
                case "1" -> {
                    List<Producto> list = service.listarTodos();
                    imprimirProductos(list);
                    pausar();
                }
                case "2" -> {
                    System.out.print("Gama (Aromáticas, Frutales, Herbaceas, Herramientas, Ornamentales): ");
                    String gama = scanner.nextLine().trim();
                    List<Producto> list = service.buscarPorGama(gama);
                    imprimirProductos(list);
                    pausar();
                }
                case "3" -> {
                    List<Producto> list = service.buscarBajoStock(10);
                    System.out.println("\n⚠️ PRODUCTOS CON STOCK CRÍTICO:");
                    imprimirProductos(list);
                    pausar();
                }
                case "4" -> {
                    System.out.print("Texto de búsqueda: ");
                    String txt = scanner.nextLine().trim();
                    List<Producto> list = service.buscarPorNombre(txt);
                    imprimirProductos(list);
                    pausar();
                }
                case "0" -> volver = true;
                default -> System.out.println("Opción inválida.");
            }
        }
    }

    private void imprimirProductos(List<Producto> list) {
        System.out.printf("%n%-10s | %-25s | %-15s | %-6s | %-10s%n", "CÓDIGO", "NOMBRE", "GAMA", "STOCK", "P. VENTA");
        System.out.println("-".repeat(70));
        for (Producto p : list) {
            System.out.printf("%-10s | %-25s | %-15s | %-6d | %-10s%n",
                    p.getCodigoProducto(),
                    p.getNombre().length() > 25 ? p.getNombre().substring(0, 22) + "..." : p.getNombre(),
                    p.getGama(), p.getCantidadEnStock(), UiHelper.formatCurrency(p.getPrecioVenta()));
        }
    }

    private void submenuPedidos() {
        boolean volver = false;
        com.jardineria.application.port.in.PedidoUseCase pedidoService = registry.getPedidoService();
        com.jardineria.application.port.in.DetallePedidoUseCase detalleService = registry.getDetallePedidoService();
        while (!volver) {
            printHeader("📑 SUBMENÚ: PEDIDOS Y LÍNEAS DE DETALLE");
            System.out.println("  [1] Listar pedidos recientes");
            System.out.println("  [2] Ver líneas de detalle de un pedido");
            System.out.println("  [3] Cambiar estado de un pedido");
            System.out.println("  [0] << Volver");
            System.out.print("\nSeleccione opción: ");

            String op = scanner.nextLine().trim();
            switch (op) {
                case "1" -> {
                    List<Pedido> list = pedidoService.listarTodos();
                    System.out.printf("%n%-6s | %-12s | %-12s | %-12s | %-8s%n", "PEDIDO", "FECHA", "ESPERADA", "ESTADO", "CLIENTE");
                    System.out.println("-".repeat(60));
                    for (Pedido p : list.stream().limit(15).toList()) {
                        System.out.printf("%-6d | %-12s | %-12s | %-12s | %-8d%n",
                                p.getCodigoPedido(), p.getFechaPedido(), p.getFechaEsperada(), p.getEstado(), p.getCodigoCliente());
                    }
                    pausar();
                }
                case "2" -> {
                    System.out.print("Ingrese código de pedido: ");
                    try {
                        int id = Integer.parseInt(scanner.nextLine().trim());
                        List<DetallePedido> detalles = detalleService.listarPorPedido(id);
                        if (detalles.isEmpty()) {
                            System.out.println("No hay artículos registrados para el pedido #" + id);
                        } else {
                            System.out.printf("%nLíneas del Pedido #%d:%n", id);
                            System.out.printf("%-5s | %-12s | %-6s | %-12s | %-12s%n", "LÍNEA", "PRODUCTO", "CANT.", "P. UNIDAD", "SUBTOTAL");
                            System.out.println("-".repeat(55));
                            for (DetallePedido d : detalles) {
                                System.out.printf("%-5d | %-12s | %-6d | %-12s | %-12s%n",
                                        d.getNumeroLinea(), d.getCodigoProducto(), d.getCantidad(),
                                        UiHelper.formatCurrency(d.getPrecioUnidad()), UiHelper.formatCurrency(d.getSubtotal()));
                            }
                        }
                    } catch (Exception ex) {
                        System.out.println("❌ " + ex.getMessage());
                    }
                    pausar();
                }
                case "3" -> {
                    System.out.print("Código de pedido: ");
                    try {
                        int id = Integer.parseInt(scanner.nextLine().trim());
                        System.out.print("Nuevo estado (Pendiente, Entregado, Rechazado): ");
                        String est = scanner.nextLine().trim();
                        pedidoService.cambiarEstado(id, est, LocalDate.now());
                        System.out.println("✔ Estado actualizado correctamente.");
                    } catch (Exception ex) {
                        System.out.println("❌ Error: " + ex.getMessage());
                    }
                    pausar();
                }
                case "0" -> volver = true;
                default -> System.out.println("Opción inválida.");
            }
        }
    }

    private void submenuPagos() {
        boolean volver = false;
        com.jardineria.application.port.in.PagoUseCase service = registry.getPagoService();
        while (!volver) {
            printHeader("💳 SUBMENÚ: PAGOS Y FINANZAS");
            System.out.println("  [1] Listar todos los pagos");
            System.out.println("  [2] Consultar recaudación total");
            System.out.println("  [3] Registrar nuevo pago");
            System.out.println("  [0] << Volver");
            System.out.print("\nSeleccione opción: ");

            String op = scanner.nextLine().trim();
            switch (op) {
                case "1" -> {
                    List<Pago> list = service.listarTodos();
                    System.out.printf("%n%-8s | %-15s | %-20s | %-12s | %-12s%n", "CLIENTE", "MÉTODO", "TRANSACCIÓN", "FECHA", "IMPORTE");
                    System.out.println("-".repeat(75));
                    for (Pago p : list) {
                        System.out.printf("%-8d | %-15s | %-20s | %-12s | %-12s%n",
                                p.getCodigoCliente(), p.getFormaPago(), p.getIdTransaccion(), p.getFechaPago(), UiHelper.formatCurrency(p.getTotal()));
                    }
                    pausar();
                }
                case "2" -> {
                    BigDecimal total = service.calcularTotalIngresos();
                    System.out.println("\n💰 Facturación total acumulada: " + UiHelper.formatCurrency(total));
                    pausar();
                }
                case "3" -> {
                    try {
                        System.out.print("Código cliente: ");
                        int cli = Integer.parseInt(scanner.nextLine().trim());
                        System.out.print("Forma de pago (PayPal, Transferencia, Tarjeta, Cheque): ");
                        String forma = scanner.nextLine().trim();
                        System.out.print("ID Transacción: ");
                        String tx = scanner.nextLine().trim();
                        System.out.print("Importe (€): ");
                        BigDecimal imp = new BigDecimal(scanner.nextLine().trim());

                        Pago p = new Pago(cli, forma, tx, LocalDate.now(), imp);
                        service.registrarPago(p);
                        System.out.println("✔ Pago registrado.");
                    } catch (Exception ex) {
                        System.out.println("❌ Error: " + ex.getMessage());
                    }
                    pausar();
                }
                case "0" -> volver = true;
                default -> System.out.println("Opción inválida.");
            }
        }
    }

    private void submenuGamas() {
        boolean volver = false;
        com.jardineria.application.port.in.GamaProductoUseCase service = registry.getGamaProductoService();
        while (!volver) {
            printHeader("🌿 SUBMENÚ: GAMAS DE PRODUCTO");
            System.out.println("  [1] Listar gamas de productos");
            System.out.println("  [2] Registrar nueva gama");
            System.out.println("  [0] << Volver");
            System.out.print("\nSeleccione opción: ");

            String op = scanner.nextLine().trim();
            switch (op) {
                case "1" -> {
                    List<GamaProducto> list = service.listarTodas();
                    for (GamaProducto g : list) {
                        System.out.printf("• %s: %s%n", g.getGama(), g.getDescripcionTexto());
                    }
                    pausar();
                }
                case "2" -> {
                    try {
                        System.out.print("Nombre de gama: ");
                        String nombre = scanner.nextLine().trim();
                        System.out.print("Descripción: ");
                        String desc = scanner.nextLine().trim();
                        service.crearGama(new GamaProducto(nombre, desc, null, null));
                        System.out.println("✔ Gama creada.");
                    } catch (Exception ex) {
                        System.out.println("❌ Error: " + ex.getMessage());
                    }
                    pausar();
                }
                case "0" -> volver = true;
                default -> System.out.println("Opción inválida.");
            }
        }
    }

    private void printHeader(String title) {
        System.out.println("\n" + "=".repeat(65));
        System.out.println(" " + title);
        System.out.println("=".repeat(65));
    }

    private void pausar() {
        System.out.print("\nPresione ENTER para continuar...");
        scanner.nextLine();
    }
}
