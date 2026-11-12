package com.jardineria.infrastructure.adapter.in.desktop.views.modules;

import com.jardineria.application.port.in.ClienteUseCase;
import com.jardineria.application.port.in.DetallePedidoUseCase;
import com.jardineria.application.port.in.PedidoUseCase;
import com.jardineria.application.port.in.ProductoUseCase;
import com.jardineria.domain.model.Cliente;
import com.jardineria.domain.model.DetallePedido;
import com.jardineria.domain.model.Pedido;
import com.jardineria.domain.model.Producto;
import com.jardineria.infrastructure.adapter.in.common.Formatters;
import com.jardineria.infrastructure.adapter.in.desktop.components.EmptyState;
import com.jardineria.infrastructure.adapter.in.desktop.components.KpiCard;
import com.jardineria.infrastructure.adapter.in.desktop.components.StatStrip;
import com.jardineria.infrastructure.adapter.in.desktop.components.StatusBadge;
import com.jardineria.infrastructure.adapter.in.desktop.components.form.FormDialog;
import com.jardineria.infrastructure.adapter.in.desktop.components.form.FormField;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.support.FxTasks;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import com.jardineria.infrastructure.adapter.in.desktop.support.ModalHost;
import com.jardineria.infrastructure.adapter.in.desktop.support.Notifier;
import com.jardineria.infrastructure.adapter.in.desktop.support.Series;
import com.jardineria.infrastructure.adapter.in.desktop.views.crud.CrudModuleView;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pedidos en maestro-detalle: tabla filtrable por estado y panel lateral con la
 * cronología del pedido, cambio rápido de estado y sus líneas (alta y baja con
 * control de stock delegado en la capa de aplicación).
 */
public class PedidosView extends CrudModuleView<Pedido> {
    private final PedidoUseCase pedidos;
    private final DetallePedidoUseCase detalles;
    private final ClienteUseCase clientes;
    private final ProductoUseCase productos;

    private volatile Map<Integer, String> nombresCliente = Map.of();
    private volatile Map<String, Producto> productoPorCodigo = Map.of();

    private final VBox detail = new VBox(16);
    private int detailRequest;

    public PedidosView(PedidoUseCase pedidos, DetallePedidoUseCase detalles, ClienteUseCase clientes, ProductoUseCase productos) {
        super(Route.PEDIDOS, "pedido", "Pedidos");
        this.pedidos = pedidos;
        this.detalles = detalles;
        this.clientes = clientes;
        this.productos = productos;

        ToggleGroup estados = new ToggleGroup();
        HBox segmented = new HBox();
        segmented.getStyleClass().add("segmented");
        for (String label : List.of("Todos", "Pendiente", "Entregado", "Rechazado")) {
            ToggleButton b = new ToggleButton(label);
            b.setUserData("Todos".equals(label) ? null : label);
            b.setToggleGroup(estados);
            segmented.getChildren().add(b);
        }
        estados.selectToggle(estados.getToggles().get(0));
        estados.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n == null) {
                o.setSelected(true);
                return;
            }
            String estado = (String) n.getUserData();
            setExtraFilter(p -> estado == null || estado.equalsIgnoreCase(p.getEstado()));
        });
        addFilter(segmented);
    }

    private String cliente(Integer codigo) {
        return nombresCliente.getOrDefault(codigo, "Cliente " + codigo);
    }

    @Override
    protected List<Pedido> fetch() {
        nombresCliente = clientes.listarTodos().stream()
                .collect(Collectors.toMap(Cliente::getCodigoCliente, Cliente::getNombreCliente, (a, b) -> a));
        productoPorCodigo = productos.listarTodos().stream()
                .collect(Collectors.toMap(Producto::getCodigoProducto, Function.identity(), (a, b) -> a));
        return pedidos.listarTodos();
    }

    @Override
    protected void configureColumns() {
        // Fechas de entrega, comentarios y líneas se ven en el panel lateral del pedido
        numberColumn("Pedido", Pedido::getCodigoPedido, 80).getStyleClass().add("col-code");
        dateColumn("Fecha", Pedido::getFechaPedido, 115);
        textColumn("Cliente", p -> cliente(p.getCodigoCliente()), 210).getStyleClass().add("col-strong");
        badgeColumn("Estado", Pedido::getEstado, 125);
        dateColumn("Entrega prevista", Pedido::getFechaEsperada, 130);
        table.getSortOrder().add(table.getColumns().get(1));
        table.getColumns().get(1).setSortType(javafx.scene.control.TableColumn.SortType.DESCENDING);
    }

    @Override
    protected Node buildSummary(List<Pedido> all) {
        Map<String, Long> porEstado = all.stream().collect(Collectors.groupingBy(
                p -> Series.ESTADOS.stream().filter(e -> e.equalsIgnoreCase(p.getEstado())).findFirst().orElse("Pendiente"),
                Collectors.counting()));
        long retrasados = all.stream().filter(p -> p.getFechaEntrega() != null && p.getFechaEsperada() != null
                && p.getFechaEntrega().isAfter(p.getFechaEsperada())).count();
        return new StatStrip()
                .stat(Feather.SHOPPING_BAG, Formatters.integer(all.size()), "pedidos registrados", KpiCard.Tone.JADE)
                .stat(Feather.CLOCK, Formatters.integer(porEstado.getOrDefault("Pendiente", 0L)), "pendientes de entrega", KpiCard.Tone.AMBER)
                .stat(Feather.CHECK_CIRCLE, Formatters.integer(porEstado.getOrDefault("Entregado", 0L)), "entregados", KpiCard.Tone.SKY)
                .stat(Feather.X_CIRCLE, Formatters.integer(porEstado.getOrDefault("Rechazado", 0L)), "rechazados", KpiCard.Tone.CORAL)
                .stat(Feather.ALERT_TRIANGLE, Formatters.integer(retrasados), "entregados con retraso", KpiCard.Tone.LILAC);
    }

    @Override
    protected String searchableText(Pedido p) {
        return String.join(" ", "#" + p.getCodigoPedido(), str(p.getCodigoPedido()), cliente(p.getCodigoCliente()),
                str(p.getEstado()), str(p.getComentarios()), Formatters.date(p.getFechaPedido()));
    }

    // ------------------------------------------------------------- detalle

    @Override
    protected Node buildDetail() {
        detail.getStyleClass().add("detail-content");
        ScrollPane scroll = new ScrollPane(detail);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().addAll("edge-to-edge", "detail-scroll");
        VBox pane = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        pane.getStyleClass().addAll("panel", "detail-pane");
        pane.setMinWidth(340);
        pane.setPrefWidth(380);
        pane.setMaxWidth(420);
        showPlaceholder();
        return pane;
    }

    private void showPlaceholder() {
        detail.getChildren().setAll(new EmptyState(Feather.MOUSE_POINTER, "Selecciona un pedido",
                "Verás su cronología, sus líneas y podrás cambiar su estado."));
    }

    @Override
    protected void onSelect(Pedido p) {
        if (p == null) {
            showPlaceholder();
            return;
        }
        int request = ++detailRequest;
        renderDetail(p, null);
        FxTasks.run(() -> detalles.listarPorPedido(p.getCodigoPedido()), lineas -> {
            if (request == detailRequest) {
                renderDetail(p, lineas);
            }
        }, e -> {
            if (request == detailRequest) {
                Notifier.error("No se pudieron cargar las líneas", FxTasks.friendlyMessage(FxTasks.unwrap(e)));
            }
        });
    }

    private void renderDetail(Pedido p, List<DetallePedido> lineas) {
        Label titulo = new Label("Pedido #" + p.getCodigoPedido());
        titulo.getStyleClass().add("detail-title");
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox head = new HBox(10, titulo, grow, new StatusBadge(p.getEstado()));
        head.setAlignment(Pos.CENTER_LEFT);
        Label cliente = new Label(cliente(p.getCodigoCliente()), Icons.of(Feather.USER, 14));
        cliente.getStyleClass().add("detail-subtitle");

        VBox timeline = new VBox(0,
                step("Realizado", p.getFechaPedido(), true),
                step("Entrega prevista", p.getFechaEsperada(), p.getFechaEsperada() != null),
                step(p.getEstado() != null && p.getEstado().equalsIgnoreCase("Rechazado") ? "Rechazado" : "Entregado",
                        p.getFechaEntrega(), p.getFechaEntrega() != null));
        timeline.getStyleClass().add("steps");

        Label retraso = new Label();
        retraso.getStyleClass().add("detail-note");
        if (p.getFechaEntrega() != null && p.getFechaEsperada() != null && p.getFechaEntrega().isAfter(p.getFechaEsperada())) {
            long dias = ChronoUnit.DAYS.between(p.getFechaEsperada(), p.getFechaEntrega());
            retraso.setText("Entregado con " + dias + (dias == 1 ? " día" : " días") + " de retraso.");
            retraso.setGraphic(Icons.of(Feather.ALERT_TRIANGLE, 13));
            retraso.getStyleClass().add("note-warning");
        } else if (p.getFechaEntrega() == null && p.getFechaEsperada() != null
                && "Pendiente".equalsIgnoreCase(p.getEstado()) && p.getFechaEsperada().isBefore(LocalDate.now())) {
            retraso.setText("Fecha prevista superada y aún sin entregar.");
            retraso.setGraphic(Icons.of(Feather.CLOCK, 13));
            retraso.getStyleClass().add("note-warning");
        }
        retraso.setVisible(!retraso.getText().isEmpty());
        retraso.setManaged(!retraso.getText().isEmpty());

        HBox acciones = new HBox(8);
        if ("Pendiente".equalsIgnoreCase(p.getEstado())) {
            Button entregar = new Button("Marcar entregado", Icons.of(Feather.CHECK, 14));
            entregar.getStyleClass().add("accent");
            entregar.setOnAction(e -> cambiarEstado(p, "Entregado"));
            Button rechazar = new Button("Rechazar", Icons.of(Feather.X, 14));
            rechazar.getStyleClass().add("danger-outline");
            rechazar.setOnAction(e -> cambiarEstado(p, "Rechazado"));
            acciones.getChildren().addAll(entregar, rechazar);
        } else {
            Button reabrir = new Button("Volver a pendiente", Icons.of(Feather.ROTATE_CCW, 14));
            reabrir.setOnAction(e -> cambiarEstado(p, "Pendiente"));
            acciones.getChildren().add(reabrir);
        }

        VBox content = new VBox(14, head, cliente, timeline, retraso, acciones);
        if (p.getComentarios() != null && !p.getComentarios().isBlank()) {
            Label com = new Label(p.getComentarios());
            com.setWrapText(true);
            com.getStyleClass().add("detail-quote");
            content.getChildren().add(com);
        }
        content.getChildren().add(linesSection(p, lineas));
        detail.getChildren().setAll(content);
    }

    private Node step(String label, LocalDate date, boolean done) {
        Region dot = new Region();
        dot.getStyleClass().addAll("step-dot", done ? "step-done" : "step-todo");
        Label l = new Label(label);
        l.getStyleClass().add("step-label");
        Label d = new Label(date == null ? "—" : Formatters.date(date));
        d.getStyleClass().add("step-date");
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox row = new HBox(10, dot, l, grow, d);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("step");
        return row;
    }

    private Node linesSection(Pedido p, List<DetallePedido> lineas) {
        Label titulo = new Label("Líneas del pedido");
        titulo.getStyleClass().add("detail-section");
        Button add = new Button(null, Icons.of(Feather.PLUS, 14));
        add.getStyleClass().addAll("flat", "icon-button");
        add.setTooltip(new Tooltip("Añadir línea"));
        add.setAccessibleText("Añadir línea al pedido");
        add.setOnAction(e -> openLineEditor(p, lineas == null ? List.of() : lineas));
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox head = new HBox(8, titulo, grow, add);
        head.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(8, head);
        if (lineas == null) {
            Label cargando = new Label("Cargando líneas…");
            cargando.getStyleClass().add("text-subtle");
            box.getChildren().add(cargando);
            return box;
        }
        if (lineas.isEmpty()) {
            Label vacio = new Label("Este pedido no tiene líneas de detalle.");
            vacio.getStyleClass().add("text-subtle");
            box.getChildren().add(vacio);
            return box;
        }
        GridPane grid = new GridPane();
        grid.getStyleClass().add("lines-grid");
        grid.setHgap(10);
        grid.setVgap(8);
        int r = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (DetallePedido d : lineas.stream().sorted(Comparator.comparing(DetallePedido::getNumeroLinea,
                Comparator.nullsLast(Comparator.naturalOrder()))).toList()) {
            Producto prod = productoPorCodigo.get(d.getCodigoProducto());
            Label nombre = new Label(prod == null ? d.getCodigoProducto() : prod.getNombre());
            nombre.getStyleClass().add("line-name");
            nombre.setWrapText(true);
            Label meta = new Label(d.getCantidad() + " × " + Formatters.currency(d.getPrecioUnidad()));
            meta.getStyleClass().add("line-meta");
            VBox desc = new VBox(1, nombre, meta);
            GridPane.setHgrow(desc, Priority.ALWAYS);
            Label sub = new Label(Formatters.currency(d.getSubtotal()));
            sub.getStyleClass().add("line-amount");
            Button del = new Button(null, Icons.of(Feather.TRASH_2, 13));
            del.getStyleClass().addAll("flat", "icon-button", "line-delete");
            del.setTooltip(new Tooltip("Quitar línea"));
            del.setAccessibleText("Quitar " + nombre.getText());
            del.setOnAction(e -> ModalHost.confirmDanger("¿Quitar «" + nombre.getText() + "» del pedido?",
                    "La línea se borra del pedido. El stock descontado no se repone automáticamente.", "Quitar",
                    () -> FxTasks.runVoid(() -> detalles.eliminarDetalle(d.getId()), () -> {
                        Notifier.success("Línea eliminada", nombre.getText() + " ya no forma parte del pedido.");
                        onSelect(p);
                    }, err -> Notifier.error("No se pudo quitar la línea", FxTasks.friendlyMessage(FxTasks.unwrap(err))))));
            grid.addRow(r++, desc, sub, del);
            total = total.add(d.getSubtotal());
        }
        Label lblTotal = new Label("Total");
        lblTotal.getStyleClass().add("lines-total-label");
        Label valTotal = new Label(Formatters.currency(total));
        valTotal.getStyleClass().add("lines-total");
        Region g2 = new Region();
        HBox.setHgrow(g2, Priority.ALWAYS);
        HBox totalRow = new HBox(lblTotal, g2, valTotal);
        totalRow.setAlignment(Pos.CENTER_LEFT);
        totalRow.getStyleClass().add("lines-total-row");
        box.getChildren().addAll(grid, totalRow);
        return box;
    }

    private void cambiarEstado(Pedido p, String estado) {
        FxTasks.run(() -> pedidos.cambiarEstado(p.getCodigoPedido(), estado, null), actualizado -> {
            Notifier.success("Estado actualizado", "El pedido #" + p.getCodigoPedido() + " ahora está " + estado.toLowerCase(Formatters.ES) + ".");
            saved();
        }, e -> Notifier.error("No se pudo cambiar el estado", FxTasks.friendlyMessage(FxTasks.unwrap(e))));
    }

    private void openLineEditor(Pedido p, List<DetallePedido> actuales) {
        List<String> usados = actuales.stream().map(DetallePedido::getCodigoProducto).toList();
        List<Producto> disponibles = productoPorCodigo.values().stream()
                .filter(prod -> !usados.contains(prod.getCodigoProducto()))
                .sorted(Comparator.comparing(Producto::getNombre)).toList();
        short siguiente = (short) (actuales.stream().map(DetallePedido::getNumeroLinea)
                .filter(java.util.Objects::nonNull).mapToInt(Short::intValue).max().orElse(0) + 1);

        FormDialog form = new FormDialog(Feather.PLUS_CIRCLE, "Añadir línea al pedido #" + p.getCodigoPedido(),
                "El stock del producto se descuenta al guardar; si no hay unidades suficientes se avisará.");
        FormField<Producto> producto = form.choice("Producto", disponibles, null,
                prod -> prod.getNombre() + " · " + prod.getCantidadEnStock() + " uds").required().wide();
        FormField<Integer> cantidad = form.integer("Cantidad", 1).required()
                .rule(v -> v <= 0 ? "La cantidad debe ser mayor que cero." : null);
        FormField<BigDecimal> precio = form.decimal("Precio por unidad (€)", null)
                .rule(v -> v.signum() <= 0 ? "El precio debe ser mayor que cero." : null);
        ((javafx.scene.control.ComboBox<?>) producto.control()).valueProperty().addListener((obs, o, n) -> {
            if (n instanceof Producto prod && prod.getPrecioVenta() != null) {
                ((javafx.scene.control.TextField) precio.control())
                        .setText(Formatters.decimal(prod.getPrecioVenta().doubleValue(), 2).replace(".", ""));
            }
        });
        form.show(() -> detalles.agregarDetalle(new DetallePedido(p.getCodigoPedido(),
                        producto.value().getCodigoProducto(), cantidad.value(),
                        precio.optional().orElse(producto.value().getPrecioVenta()), siguiente)),
                () -> producto.value().getNombre() + " añadido al pedido #" + p.getCodigoPedido() + ".",
                () -> {
                    saved();
                    onSelect(p);
                });
    }

    // ------------------------------------------------------------- formulario

    @Override
    protected void openEditor(Pedido existing) {
        FxTasks.run(clientes::listarTodos, lista -> showForm(existing, lista),
                e -> Notifier.error("No se pudieron cargar los clientes", FxTasks.friendlyMessage(FxTasks.unwrap(e))));
    }

    private void showForm(Pedido existing, List<Cliente> listaClientes) {
        boolean nuevo = existing == null;
        Pedido p = nuevo ? new Pedido() : existing;
        int siguiente = allItems().stream().map(Pedido::getCodigoPedido).max(Comparator.naturalOrder()).orElse(0) + 1;
        Map<Integer, String> mapa = listaClientes.stream()
                .collect(Collectors.toMap(Cliente::getCodigoCliente, Cliente::getNombreCliente, (a, b) -> a));
        LocalDate hoy = LocalDate.now();

        FormDialog form = new FormDialog(Feather.SHOPPING_BAG, nuevo ? "Nuevo pedido" : "Editar pedido #" + p.getCodigoPedido(),
                nuevo ? "Se propone el número " + siguiente + ". Las líneas se añaden después desde el panel del pedido."
                        : "El número de pedido no se puede cambiar.");
        FormField<Integer> codigo = form.integer("Número de pedido", nuevo ? siguiente : p.getCodigoPedido()).required().locked(!nuevo)
                .rule(v -> v <= 0 ? "Debe ser un número positivo." : null);
        FormField<String> estado = form.choice("Estado", Series.ESTADOS, nuevo ? "Pendiente"
                : Series.ESTADOS.stream().filter(s -> s.equalsIgnoreCase(p.getEstado())).findFirst().orElse("Pendiente"), s -> s).required();
        FormField<Integer> cliente = form.choice("Cliente",
                listaClientes.stream().map(Cliente::getCodigoCliente).sorted().toList(), p.getCodigoCliente(),
                c -> mapa.getOrDefault(c, "?") + " (" + c + ")").required().wide();
        FormField<LocalDate> fecha = form.date("Fecha del pedido", nuevo ? hoy : p.getFechaPedido()).required();
        FormField<LocalDate> esperada = form.date("Entrega prevista", nuevo ? hoy.plusDays(7) : p.getFechaEsperada()).required()
                .rule(d -> fecha.value() != null && d.isBefore(fecha.value()) ? "No puede ser anterior a la fecha del pedido." : null);
        FormField<LocalDate> entrega = form.date("Fecha de entrega", p.getFechaEntrega())
                .rule(d -> fecha.value() != null && d.isBefore(fecha.value()) ? "No puede ser anterior a la fecha del pedido." : null);
        FormField<String> comentarios = form.area("Comentarios", p.getComentarios());
        form.keyField(codigo);
        form.show(() -> {
            LocalDate fEntrega = entrega.value();
            if ("Entregado".equals(estado.value()) && fEntrega == null) {
                fEntrega = LocalDate.now();
            }
            Pedido datos = new Pedido(codigo.value(), fecha.value(), esperada.value(), fEntrega,
                    estado.value(), comentarios.value(), cliente.value());
            if (nuevo) {
                pedidos.crearPedido(datos);
            } else {
                pedidos.actualizarPedido(datos);
            }
        }, () -> "Pedido #" + codigo.value() + (nuevo ? " creado." : " actualizado."), this::saved);
    }

    @Override
    protected void deleteItem(Pedido item) {
        pedidos.eliminarPedido(item.getCodigoPedido());
    }

    @Override
    protected String describe(Pedido item) {
        return "el pedido #" + item.getCodigoPedido() + " de " + cliente(item.getCodigoCliente());
    }

    @Override
    protected List<String> csvHeader() {
        return List.of("Pedido", "Fecha", "Código cliente", "Cliente", "Estado", "Entrega prevista", "Entregado", "Comentarios");
    }

    @Override
    protected List<String> csvRow(Pedido p) {
        return List.of(str(p.getCodigoPedido()), Formatters.shortDate(p.getFechaPedido()), str(p.getCodigoCliente()),
                cliente(p.getCodigoCliente()), str(p.getEstado()), Formatters.shortDate(p.getFechaEsperada()),
                p.getFechaEntrega() == null ? "" : Formatters.shortDate(p.getFechaEntrega()), str(p.getComentarios()));
    }
}
