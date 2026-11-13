package com.jardineria.infrastructure.adapter.in.desktop.views.modules;

import com.jardineria.application.port.in.AnalyticsUseCase;
import com.jardineria.application.port.in.ClienteUseCase;
import com.jardineria.application.port.in.EmpleadoUseCase;
import com.jardineria.application.port.in.PagoUseCase;
import com.jardineria.application.port.in.PedidoUseCase;
import com.jardineria.domain.model.Cliente;
import com.jardineria.domain.model.Empleado;
import com.jardineria.domain.model.Pago;
import com.jardineria.domain.model.Pedido;
import com.jardineria.domain.model.analytics.ClienteFinancialSummary;
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
import com.jardineria.infrastructure.adapter.in.desktop.support.Motion;
import com.jardineria.infrastructure.adapter.in.desktop.support.Notifier;
import com.jardineria.infrastructure.adapter.in.desktop.views.crud.CrudModuleView;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cartera de clientes en maestro-detalle. La ficha lateral reúne contacto,
 * representante, uso de la línea de crédito, historial de pedidos y pagos,
 * todo cargado en una única consulta en segundo plano por selección.
 */
public class ClientesView extends CrudModuleView<Cliente> {
    private static final String[] AVATAR_TONES = {"tone-jade", "tone-sky", "tone-lilac", "tone-amber", "tone-coral", "tone-lime"};

    private final ClienteUseCase clientes;
    private final EmpleadoUseCase empleados;
    private final AnalyticsUseCase analytics;
    private final PedidoUseCase pedidos;
    private final PagoUseCase pagos;
    private final ComboBox<String> paisFilter = new ComboBox<>();
    private final VBox detail = new VBox(18);
    private volatile Map<Integer, Empleado> empleadoPorCodigo = Map.of();
    private int detailRequest;

    public ClientesView(ClienteUseCase clientes, EmpleadoUseCase empleados, AnalyticsUseCase analytics,
                        PedidoUseCase pedidos, PagoUseCase pagos) {
        super(Route.CLIENTES, "cliente", "Clientes");
        this.clientes = clientes;
        this.empleados = empleados;
        this.analytics = analytics;
        this.pedidos = pedidos;
        this.pagos = pagos;

        paisFilter.setPromptText("Todos los países");
        paisFilter.setButtonCell(paisCell(true));
        paisFilter.setCellFactory(lv -> paisCell(false));
        paisFilter.setAccessibleText("Filtrar por país");
        paisFilter.valueProperty().addListener((obs, o, n) -> setExtraFilter(c -> n == null || n.equals(c.getPais())));
        addFilter(paisFilter);
    }

    private static ListCell<String> paisCell(boolean button) {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty && !button ? null : item == null ? "Todos los países" : Formatters.display(item));
            }
        };
    }

    private String representante(Integer codigo) {
        Empleado e = codigo == null ? null : empleadoPorCodigo.get(codigo);
        return e == null ? null : e.getNombreCompleto();
    }

    @Override
    protected List<Cliente> fetch() {
        empleadoPorCodigo = empleados.listarTodos().stream()
                .collect(Collectors.toMap(Empleado::getCodigoEmpleado, Function.identity(), (a, b) -> a));
        return clientes.listarTodos();
    }

    @Override
    protected void afterLoad(List<Cliente> items) {
        List<String> opciones = new ArrayList<>();
        opciones.add(null);
        items.stream().map(Cliente::getPais).filter(Objects::nonNull).distinct()
                .sorted(Comparator.comparing(Formatters::display)).forEach(opciones::add);
        if (!paisFilter.getItems().equals(opciones)) {
            String actual = paisFilter.getValue();
            paisFilter.setItems(FXCollections.observableArrayList(opciones));
            paisFilter.setValue(actual);
        }
    }

    @Override
    protected void configureColumns() {
        // Contacto, teléfono y dirección se ven en la ficha lateral: la tabla queda legible
        numberColumn("Cód.", Cliente::getCodigoCliente, 64).getStyleClass().add("col-code");
        textColumn("Cliente", Cliente::getNombreCliente, 200).getStyleClass().add("col-strong");
        textColumn("Ciudad", Cliente::getCiudad, 120);
        textColumn("País", c -> Formatters.display(c.getPais()), 120);
        textColumn("Representante", c -> representante(c.getCodigoEmpleadoRepVentas()), 160);
        moneyColumn("Crédito", Cliente::getLimiteCredito, 140).setMinWidth(130);
    }

    @Override
    protected Node buildSummary(List<Cliente> all) {
        long paises = all.stream().map(c -> Formatters.display(c.getPais())).distinct().count();
        BigDecimal credito = all.stream().map(Cliente::getLimiteCredito).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long sinRep = all.stream().filter(c -> c.getCodigoEmpleadoRepVentas() == null).count();
        String ciudad = all.stream().collect(Collectors.groupingBy(Cliente::getCiudad, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("—");
        return new StatStrip()
                .stat(Feather.USERS, Formatters.integer(all.size()), "clientes en cartera", KpiCard.Tone.JADE)
                .stat(Feather.GLOBE, Formatters.integer(paises), "países", KpiCard.Tone.SKY)
                .stat(Feather.CREDIT_CARD, Formatters.compactCurrency(credito), "crédito concedido", KpiCard.Tone.LIME)
                .stat(Feather.MAP_PIN, ciudad, "ciudad con más clientes", KpiCard.Tone.LILAC)
                .stat(Feather.USER_X, Formatters.integer(sinRep), "sin representante", KpiCard.Tone.AMBER);
    }

    @Override
    protected String searchableText(Cliente c) {
        return String.join(" ", str(c.getCodigoCliente()), str(c.getNombreCliente()), str(c.getContactoCompleto()),
                str(c.getCiudad()), str(c.getRegion()), str(c.getPais()), Formatters.display(c.getPais()),
                str(c.getTelefono()), str(representante(c.getCodigoEmpleadoRepVentas())));
    }

    // ------------------------------------------------------------- ficha

    private record Ficha(Optional<ClienteFinancialSummary> resumen, List<Pedido> pedidos, List<Pago> pagos) {
    }

    @Override
    protected Node buildDetail() {
        detail.getStyleClass().add("detail-content");
        ScrollPane scroll = new ScrollPane(detail);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().addAll("edge-to-edge", "detail-scroll");
        VBox pane = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        pane.getStyleClass().addAll("panel", "detail-pane", "client-card");
        pane.setMinWidth(350);
        pane.setPrefWidth(390);
        pane.setMaxWidth(430);
        showPlaceholder();
        return pane;
    }

    private void showPlaceholder() {
        detail.getChildren().setAll(new EmptyState(Feather.USER, "Selecciona un cliente",
                "Verás su ficha, el uso de su crédito y su historial de pedidos y pagos."));
    }

    @Override
    protected void onSelect(Cliente c) {
        if (c == null) {
            showPlaceholder();
            return;
        }
        int request = ++detailRequest;
        renderFicha(c, null);
        FxTasks.run(() -> new Ficha(analytics.resumenCliente(c.getCodigoCliente()),
                        pedidos.buscarPorCliente(c.getCodigoCliente()),
                        pagos.buscarPorCliente(c.getCodigoCliente())),
                ficha -> {
                    if (request == detailRequest) {
                        renderFicha(c, ficha);
                    }
                },
                e -> {
                    if (request == detailRequest) {
                        Notifier.error("No se pudo cargar la ficha", FxTasks.friendlyMessage(FxTasks.unwrap(e)));
                    }
                });
    }

    private void renderFicha(Cliente c, Ficha ficha) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(identity(c));
        nodes.add(contactBlock(c));
        if (ficha == null) {
            Label cargando = new Label("Cargando historial…");
            cargando.getStyleClass().add("text-subtle");
            nodes.add(cargando);
        } else {
            nodes.add(creditBlock(c, ficha.resumen().orElse(null)));
            nodes.add(ordersBlock(ficha.pedidos()));
            nodes.add(paymentsBlock(ficha.pagos()));
        }
        detail.getChildren().setAll(nodes);
    }

    private Node identity(Cliente c) {
        Label initials = new Label(initials(c.getNombreCliente()));
        initials.getStyleClass().add("avatar-text");
        StackPane avatar = new StackPane(initials);
        avatar.getStyleClass().addAll("avatar", AVATAR_TONES[Math.floorMod(c.getCodigoCliente(), AVATAR_TONES.length)]);
        Label name = new Label(c.getNombreCliente());
        name.getStyleClass().add("detail-title");
        name.setWrapText(true);
        Label where = new Label(c.getCiudad() + (c.getPais() == null ? "" : ", " + Formatters.display(c.getPais())),
                Icons.of(Feather.MAP_PIN, 13));
        where.getStyleClass().add("detail-subtitle");
        Label code = new Label("Cliente " + c.getCodigoCliente());
        code.getStyleClass().add("detail-code");
        VBox text = new VBox(3, name, where, code);
        HBox.setHgrow(text, Priority.ALWAYS);
        text.setMinWidth(0);
        Button edit = new Button(null, Icons.of(Feather.EDIT_2, 14));
        edit.getStyleClass().addAll("flat", "icon-button");
        edit.setAccessibleText("Editar cliente");
        edit.setTooltip(new javafx.scene.control.Tooltip("Editar ficha"));
        edit.setOnAction(e -> openEditor(c));
        HBox box = new HBox(14, avatar, text, edit);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("identity");
        return box;
    }

    private Node contactBlock(Cliente c) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("info-grid");
        grid.setHgap(12);
        grid.setVgap(10);
        int r = 0;
        r = info(grid, r, Feather.USER, "Contacto", c.getContactoCompleto());
        r = info(grid, r, Feather.PHONE, "Teléfono", c.getTelefono());
        r = info(grid, r, Feather.PRINTER, "Fax", c.getFax());
        String dir = c.getLineaDireccion1() + (c.getLineaDireccion2() == null ? "" : ", " + c.getLineaDireccion2())
                + (c.getCodigoPostal() == null ? "" : " · " + c.getCodigoPostal());
        r = info(grid, r, Feather.HOME, "Dirección", dir);
        info(grid, r, Feather.BRIEFCASE, "Representante", representante(c.getCodigoEmpleadoRepVentas()));
        return section("Contacto", grid);
    }

    private static int info(GridPane grid, int row, Ikon icon, String label, String value) {
        Label l = new Label(label, Icons.of(icon, 13));
        l.getStyleClass().add("info-label");
        l.setMinWidth(Region.USE_PREF_SIZE);
        Label v = new Label(Formatters.orDash(value));
        v.getStyleClass().add("info-value");
        v.setWrapText(true);
        GridPane.setHgrow(v, Priority.ALWAYS);
        grid.addRow(row, l, v);
        return row + 1;
    }

    private Node creditBlock(Cliente c, ClienteFinancialSummary s) {
        BigDecimal limite = c.getLimiteCredito() == null ? BigDecimal.ZERO : c.getLimiteCredito();
        if (s == null) {
            s = new ClienteFinancialSummary(c.getCodigoCliente(), 0, BigDecimal.ZERO, BigDecimal.ZERO, limite);
        }
        double uso = s.usoCredito();
        Label pct = new Label(Formatters.percent(uso));
        pct.getStyleClass().add("credit-value");
        Label of = new Label("del límite de " + Formatters.currency(limite) + " en uso");
        of.getStyleClass().add("credit-caption");
        of.setWrapText(true);
        VBox head = new VBox(0, pct, of);

        ProgressBar bar = new ProgressBar(0);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().addAll("credit-bar", uso >= 0.9 ? "credit-danger" : uso >= 0.6 ? "credit-warning" : "credit-ok");
        bar.setAccessibleText("Uso del crédito: " + Formatters.percent(uso));
        if (Motion.isReduced()) {
            bar.setProgress(uso);
        } else {
            new Timeline(new KeyFrame(Duration.millis(800), new KeyValue(bar.progressProperty(), uso, Motion.EASE_OUT))).play();
        }

        GridPane figures = new GridPane();
        figures.getStyleClass().add("figures");
        figures.setHgap(10);
        figures.setVgap(10);
        figures.add(figure("Comprado", Formatters.currency(s.totalComprado())), 0, 0);
        figures.add(figure("Pagado", Formatters.currency(s.totalPagado())), 1, 0);
        figures.add(figure("Saldo pendiente", Formatters.currency(s.saldoPendiente())), 0, 1);
        figures.add(figure("Disponible", Formatters.currency(limite.subtract(s.saldoPendiente()).max(BigDecimal.ZERO))), 1, 1);
        javafx.scene.layout.ColumnConstraints half = new javafx.scene.layout.ColumnConstraints();
        half.setPercentWidth(50);
        figures.getColumnConstraints().addAll(half, half);

        return section("Línea de crédito", new VBox(10, head, bar, figures));
    }

    private static Node figure(String label, String value) {
        Label v = new Label(value);
        v.getStyleClass().add("figure-value");
        Label l = new Label(label);
        l.getStyleClass().add("figure-label");
        VBox box = new VBox(2, v, l);
        box.getStyleClass().add("figure");
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private Node ordersBlock(List<Pedido> lista) {
        VBox rows = new VBox(4);
        lista.stream().sorted(Comparator.comparing(Pedido::getFechaPedido, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6).forEach(p -> {
                    Label t = new Label("Pedido #" + p.getCodigoPedido());
                    t.getStyleClass().add("timeline-title");
                    Label d = new Label(Formatters.date(p.getFechaPedido()));
                    d.getStyleClass().add("timeline-detail");
                    VBox text = new VBox(1, t, d);
                    HBox.setHgrow(text, Priority.ALWAYS);
                    HBox row = new HBox(10, text, new StatusBadge(p.getEstado()));
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.getStyleClass().add("timeline-row");
                    rows.getChildren().add(row);
                });
        if (lista.isEmpty()) {
            rows.getChildren().add(muted("Este cliente aún no ha realizado pedidos."));
        } else if (lista.size() > 6) {
            rows.getChildren().add(muted("y " + (lista.size() - 6) + " pedidos anteriores más."));
        }
        return section("Pedidos (" + lista.size() + ")", rows);
    }

    private Node paymentsBlock(List<Pago> lista) {
        VBox rows = new VBox(4);
        lista.stream().sorted(Comparator.comparing(Pago::getFechaPago, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5).forEach(p -> {
                    Label t = new Label(p.getFormaPago(), Icons.of(Feather.CREDIT_CARD, 13));
                    t.getStyleClass().add("timeline-title");
                    Label d = new Label(Formatters.date(p.getFechaPago()) + " · " + p.getIdTransaccion());
                    d.getStyleClass().add("timeline-detail");
                    VBox text = new VBox(1, t, d);
                    HBox.setHgrow(text, Priority.ALWAYS);
                    text.setMinWidth(0);
                    Label amount = new Label(Formatters.currency(p.getTotal()));
                    amount.getStyleClass().add("line-amount");
                    HBox row = new HBox(10, text, amount);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.getStyleClass().add("timeline-row");
                    rows.getChildren().add(row);
                });
        if (lista.isEmpty()) {
            rows.getChildren().add(muted("No hay pagos registrados."));
        }
        return section("Pagos (" + lista.size() + ")", rows);
    }

    private static Node section(String title, Node body) {
        Label l = new Label(title);
        l.getStyleClass().add("detail-section");
        return new VBox(10, l, body);
    }

    private static Label muted(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("text-subtle");
        l.setWrapText(true);
        return l;
    }

    static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] parts = name.trim().split("[\\s.\\-]+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty() && Character.isLetterOrDigit(p.charAt(0))) {
                sb.append(Character.toUpperCase(p.charAt(0)));
            }
            if (sb.length() == 2) {
                break;
            }
        }
        return sb.isEmpty() ? "?" : sb.toString();
    }

    // ------------------------------------------------------------- formulario

    @Override
    protected void openEditor(Cliente existing) {
        FxTasks.run(empleados::listarTodos, lista -> showForm(existing, lista),
                e -> Notifier.error("No se pudieron cargar los empleados", FxTasks.friendlyMessage(FxTasks.unwrap(e))));
    }

    private void showForm(Cliente existing, List<Empleado> listaEmpleados) {
        boolean nuevo = existing == null;
        Cliente c = nuevo ? new Cliente() : existing;
        int siguiente = allItems().stream().map(Cliente::getCodigoCliente).max(Comparator.naturalOrder()).orElse(0) + 1;
        Map<Integer, String> nombres = listaEmpleados.stream()
                .collect(Collectors.toMap(Empleado::getCodigoEmpleado, Empleado::getNombreCompleto, (a, b) -> a));

        FormDialog form = new FormDialog(Feather.USER_PLUS, nuevo ? "Nuevo cliente" : "Editar " + c.getNombreCliente(),
                nuevo ? "Se propone el código " + siguiente + ". Los campos con * son obligatorios."
                        : "El código de cliente no se puede cambiar una vez creado.");
        form.section("Empresa");
        FormField<Integer> codigo = form.integer("Código", nuevo ? siguiente : c.getCodigoCliente()).required().locked(!nuevo)
                .rule(v -> v <= 0 ? "Debe ser un número positivo." : null);
        FormField<String> nombre = form.text("Nombre del cliente", c.getNombreCliente(), 50).required();
        FormField<String> contacto = form.text("Nombre de contacto", c.getNombreContacto(), 30);
        FormField<String> apellido = form.text("Apellido de contacto", c.getApellidoContacto(), 30);
        FormField<String> telefono = form.text("Teléfono", c.getTelefono(), 15).required();
        FormField<String> fax = form.text("Fax", c.getFax(), 15).required();
        form.section("Dirección");
        FormField<String> dir1 = form.text("Dirección", c.getLineaDireccion1(), 50).required().wide();
        FormField<String> dir2 = form.text("Dirección (línea 2)", c.getLineaDireccion2(), 50).wide();
        FormField<String> ciudad = form.text("Ciudad", c.getCiudad(), 50).required();
        FormField<String> region = form.text("Región", c.getRegion(), 50);
        FormField<String> pais = form.text("País", c.getPais(), 50);
        FormField<String> cp = form.text("Código postal", c.getCodigoPostal(), 10);
        form.section("Comercial");
        FormField<Integer> rep = form.choice("Representante de ventas",
                listaEmpleados.stream().map(Empleado::getCodigoEmpleado).sorted().toList(), c.getCodigoEmpleadoRepVentas(),
                id -> nombres.getOrDefault(id, "?") + " (" + id + ")", "Sin representante");
        FormField<BigDecimal> credito = form.decimal("Límite de crédito (€)", c.getLimiteCredito())
                .rule(v -> v.signum() < 0 ? "El límite no puede ser negativo." : null);
        form.keyField(codigo);
        form.show(() -> {
            Cliente datos = new Cliente(codigo.value(), nombre.value(), contacto.value(), apellido.value(),
                    telefono.value(), fax.value(), dir1.value(), dir2.value(), ciudad.value(), region.value(),
                    pais.value(), cp.value(), rep.value(), credito.value());
            if (nuevo) {
                clientes.crearCliente(datos);
            } else {
                clientes.actualizarCliente(datos);
            }
        }, () -> nombre.value() + (nuevo ? " se ha añadido a la cartera." : " se ha actualizado."), this::saved);
    }

    @Override
    protected void deleteItem(Cliente item) {
        clientes.eliminarCliente(item.getCodigoCliente());
    }

    @Override
    protected String describe(Cliente item) {
        return "el cliente «" + item.getNombreCliente() + "»";
    }

    @Override
    protected List<String> csvHeader() {
        return List.of("Código", "Cliente", "Contacto", "Teléfono", "Fax", "Dirección", "Ciudad", "Región", "País",
                "Código postal", "Representante", "Límite de crédito");
    }

    @Override
    protected List<String> csvRow(Cliente c) {
        return List.of(str(c.getCodigoCliente()), str(c.getNombreCliente()), str(c.getContactoCompleto()),
                str(c.getTelefono()), str(c.getFax()), str(c.getLineaDireccion1()), str(c.getCiudad()), str(c.getRegion()),
                Formatters.display(c.getPais()), str(c.getCodigoPostal()), str(representante(c.getCodigoEmpleadoRepVentas())),
                c.getLimiteCredito() == null ? "" : Formatters.decimal(c.getLimiteCredito().doubleValue(), 2));
    }
}
