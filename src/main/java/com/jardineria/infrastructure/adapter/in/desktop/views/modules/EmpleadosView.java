package com.jardineria.infrastructure.adapter.in.desktop.views.modules;

import com.jardineria.application.port.in.ClienteUseCase;
import com.jardineria.application.port.in.EmpleadoUseCase;
import com.jardineria.domain.model.Cliente;
import com.jardineria.application.port.in.OficinaUseCase;
import com.jardineria.domain.model.Empleado;
import com.jardineria.domain.model.Oficina;
import com.jardineria.infrastructure.adapter.in.common.Formatters;
import com.jardineria.infrastructure.adapter.in.desktop.components.Avatar;
import com.jardineria.infrastructure.adapter.in.desktop.components.EmptyState;
import com.jardineria.infrastructure.adapter.in.desktop.components.KpiCard;
import com.jardineria.infrastructure.adapter.in.desktop.components.StatStrip;
import com.jardineria.infrastructure.adapter.in.desktop.components.form.FormDialog;
import com.jardineria.infrastructure.adapter.in.desktop.components.form.FormField;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.support.FxTasks;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import com.jardineria.infrastructure.adapter.in.desktop.support.Interactions;
import com.jardineria.infrastructure.adapter.in.desktop.support.Notifier;
import com.jardineria.infrastructure.adapter.in.desktop.views.crud.CrudModuleView;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Equipo comercial en maestro-detalle. La ficha lateral funciona como organigrama
 * navegable: cadena de mando hasta dirección, equipo directo y cartera de clientes.
 * Todo sale de los datos ya cargados, sin consultas extra por selección.
 */
public class EmpleadosView extends CrudModuleView<Empleado> {
    private final EmpleadoUseCase empleados;
    private final OficinaUseCase oficinas;
    private final ClienteUseCase clientes;
    private final ComboBox<String> oficinaFilter = new ComboBox<>();
    private final VBox detail = new VBox(18);
    private volatile Map<String, Oficina> oficinaPorCodigo = Map.of();
    private volatile Map<Integer, Empleado> empleadoPorCodigo = Map.of();
    private volatile Map<Integer, Long> clientesPorRepresentante = Map.of();

    public EmpleadosView(EmpleadoUseCase empleados, OficinaUseCase oficinas, ClienteUseCase clientes) {
        super(Route.EMPLEADOS, "empleado", "Empleados");
        this.empleados = empleados;
        this.oficinas = oficinas;
        this.clientes = clientes;

        oficinaFilter.setPromptText("Todas las oficinas");
        oficinaFilter.setButtonCell(oficinaCell(true));
        oficinaFilter.setCellFactory(lv -> oficinaCell(false));
        oficinaFilter.setAccessibleText("Filtrar por oficina");
        oficinaFilter.valueProperty().addListener((obs, o, n) ->
                setExtraFilter(e -> n == null || n.equals(e.getCodigoOficina())));
        addFilter(oficinaFilter);
    }

    private ListCell<String> oficinaCell(boolean button) {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty && !button) {
                    setText(null);
                } else {
                    setText(item == null ? "Todas las oficinas" : oficinaLabel(item));
                }
            }
        };
    }

    private String oficinaLabel(String codigo) {
        Oficina o = oficinaPorCodigo.get(codigo);
        return o == null ? codigo : codigo + " · " + o.getCiudad();
    }

    private String nombreDe(Integer codigo) {
        Empleado e = codigo == null ? null : empleadoPorCodigo.get(codigo);
        return e == null ? null : e.getNombreCompleto();
    }

    @Override
    protected List<Empleado> fetch() {
        oficinaPorCodigo = oficinas.listarTodas().stream()
                .collect(Collectors.toMap(Oficina::getCodigoOficina, Function.identity(), (a, b) -> a));
        List<Empleado> lista = empleados.listarTodos();
        empleadoPorCodigo = lista.stream()
                .collect(Collectors.toMap(Empleado::getCodigoEmpleado, Function.identity(), (a, b) -> a));
        clientesPorRepresentante = clientes.listarTodos().stream()
                .map(Cliente::getCodigoEmpleadoRepVentas).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return lista;
    }

    @Override
    protected void afterLoad(List<Empleado> items) {
        List<String> opciones = new ArrayList<>();
        opciones.add(null);
        oficinaPorCodigo.keySet().stream().sorted().forEach(opciones::add);
        if (!oficinaFilter.getItems().equals(opciones)) {
            String actual = oficinaFilter.getValue();
            oficinaFilter.setItems(FXCollections.observableArrayList(opciones));
            oficinaFilter.setValue(actual);
        }
    }

    @Override
    protected void configureColumns() {
        // Email y extensión se ven en la ficha lateral: la tabla queda legible junto a ella
        numberColumn("Cód.", Empleado::getCodigoEmpleado, 64).getStyleClass().add("col-code");
        personColumn("Nombre", Empleado::getNombreCompleto, Empleado::getCodigoEmpleado, 240);
        textColumn("Puesto", Empleado::getPuesto, 180);
        textColumn("Oficina", e -> oficinaLabel(e.getCodigoOficina()), 160);
        textColumn("Jefe directo", e -> nombreDe(e.getCodigoJefe()), 180);
    }

    @Override
    protected Node buildSummary(List<Empleado> all) {
        long representantes = all.stream()
                .filter(e -> e.getPuesto() != null && Formatters.fold(e.getPuesto()).contains("representante")).count();
        long directivos = all.stream().filter(e -> e.getCodigoJefe() == null
                || (e.getPuesto() != null && Formatters.fold(e.getPuesto()).contains("director"))).count();
        long conEquipo = all.stream().map(Empleado::getCodigoJefe).filter(Objects::nonNull).distinct().count();
        return new StatStrip()
                .stat(Feather.USERS, Formatters.integer(all.size()), "personas en plantilla", KpiCard.Tone.ACCENT)
                .stat(Feather.BRIEFCASE, Formatters.integer(representantes), "representantes de ventas", KpiCard.Tone.SKY)
                .stat(Feather.AWARD, Formatters.integer(directivos), "dirección y gerencia", KpiCard.Tone.AMBER)
                .stat(Feather.GIT_MERGE, Formatters.integer(conEquipo), "responsables con equipo", KpiCard.Tone.LILAC);
    }

    @Override
    protected String searchableText(Empleado e) {
        return String.join(" ", str(e.getCodigoEmpleado()), e.getNombreCompleto(), str(e.getPuesto()),
                str(e.getEmail()), str(e.getCodigoOficina()), oficinaLabel(e.getCodigoOficina()));
    }

    // ------------------------------------------------------------- ficha / organigrama

    @Override
    protected Node buildDetail() {
        detail.getStyleClass().add("detail-content");
        ScrollPane scroll = new ScrollPane(detail);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().addAll("edge-to-edge", "detail-scroll");
        VBox pane = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        pane.getStyleClass().addAll("panel", "detail-pane", "client-card");
        pane.setMinWidth(330);
        pane.setPrefWidth(370);
        pane.setMaxWidth(420);
        showPlaceholder();
        return pane;
    }

    private void showPlaceholder() {
        detail.getChildren().setAll(new EmptyState(Feather.GIT_MERGE, "Selecciona a una persona",
                "Verás su cadena de mando, su equipo directo y los clientes que atiende."));
    }

    @Override
    protected void onSelect(Empleado e) {
        if (e == null) {
            showPlaceholder();
            return;
        }
        List<Node> nodes = new ArrayList<>();
        nodes.add(identity(e));
        nodes.add(contactBlock(e));
        nodes.add(chainBlock(e));
        nodes.add(teamBlock(e));
        detail.getChildren().setAll(nodes);
    }

    private Node identity(Empleado e) {
        Avatar avatar = new Avatar(e.getNombreCompleto(), e.getCodigoEmpleado(), Avatar.Size.LARGE);
        Label name = new Label(e.getNombreCompleto());
        name.getStyleClass().add("detail-title");
        name.setWrapText(true);
        Label puesto = new Label(Formatters.orDash(e.getPuesto()), Icons.of(Feather.BRIEFCASE, 13));
        puesto.getStyleClass().add("detail-subtitle");
        Label code = new Label("Empleado " + e.getCodigoEmpleado());
        code.getStyleClass().add("detail-code");
        VBox text = new VBox(3, name, puesto, code);
        HBox.setHgrow(text, Priority.ALWAYS);
        text.setMinWidth(0);
        Button edit = new Button(null, Icons.of(Feather.EDIT_2, 14));
        edit.getStyleClass().addAll("flat", "icon-button");
        edit.setAccessibleText("Editar empleado");
        edit.setTooltip(new Tooltip("Editar ficha"));
        edit.setOnAction(ev -> openEditor(e));
        HBox box = new HBox(14, avatar, text, edit);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("identity");
        return box;
    }

    private Node contactBlock(Empleado e) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("info-grid");
        grid.setHgap(12);
        grid.setVgap(10);
        Oficina o = oficinaPorCodigo.get(e.getCodigoOficina());
        int r = 0;
        r = info(grid, r, Feather.MAIL, "Email", e.getEmail());
        r = info(grid, r, Feather.PHONE, "Extensión", e.getExtension());
        r = info(grid, r, Feather.MAP_PIN, "Oficina", o == null ? e.getCodigoOficina()
                : o.getCiudad() + ", " + Formatters.display(o.getPais()) + " (" + o.getCodigoOficina() + ")");
        if (o != null) {
            r = info(grid, r, Feather.PHONE_CALL, "Centralita", o.getTelefono());
        }
        long cartera = clientesPorRepresentante.getOrDefault(e.getCodigoEmpleado(), 0L);
        info(grid, r, Feather.USERS, "Clientes", cartera == 0 ? null
                : Formatters.integer(cartera) + (cartera == 1 ? " cliente asignado" : " clientes asignados"));
        return section("Contacto", grid);
    }

    /** Cadena de mando de la dirección a la persona; tolera ciclos y jefes inexistentes. */
    List<Empleado> chainOf(Empleado e) {
        Deque<Empleado> chain = new ArrayDeque<>();
        Set<Integer> seen = new HashSet<>();
        for (Empleado cur = e; cur != null && seen.add(cur.getCodigoEmpleado());
             cur = cur.getCodigoJefe() == null ? null : empleadoPorCodigo.get(cur.getCodigoJefe())) {
            chain.addFirst(cur);
        }
        return List.copyOf(chain);
    }

    /** Personas que dependen de {@code e} directa o indirectamente. */
    long teamSize(Empleado e) {
        Set<Integer> seen = new HashSet<>(Set.of(e.getCodigoEmpleado()));
        Deque<Integer> pending = new ArrayDeque<>(List.of(e.getCodigoEmpleado()));
        while (!pending.isEmpty()) {
            Integer jefe = pending.poll();
            for (Empleado sub : allItems()) {
                if (jefe.equals(sub.getCodigoJefe()) && seen.add(sub.getCodigoEmpleado())) {
                    pending.add(sub.getCodigoEmpleado());
                }
            }
        }
        return seen.size() - 1L;
    }

    private Node chainBlock(Empleado e) {
        List<Empleado> chain = chainOf(e);
        VBox rows = new VBox(2);
        for (int i = 0; i < chain.size(); i++) {
            Empleado p = chain.get(i);
            boolean self = p.getCodigoEmpleado().equals(e.getCodigoEmpleado());
            Node row = personRow(p, self ? "Esta persona" : p.getPuesto(), self);
            VBox.setMargin(row, new Insets(0, 0, 0, Math.min(i, 4) * 14.0));
            rows.getChildren().add(row);
        }
        String title = chain.size() <= 1 ? "Cadena de mando" : "Cadena de mando (" + (chain.size() - 1)
                + (chain.size() == 2 ? " nivel)" : " niveles)");
        return section(title, rows);
    }

    private Node teamBlock(Empleado e) {
        List<Empleado> directos = allItems().stream()
                .filter(s -> e.getCodigoEmpleado().equals(s.getCodigoJefe()))
                .sorted(Comparator.comparing(Empleado::getNombreCompleto, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        VBox rows = new VBox(2);
        directos.forEach(s -> {
            long sub = teamSize(s);
            rows.getChildren().add(personRow(s, Formatters.orDash(s.getPuesto())
                    + (sub == 0 ? "" : " · " + sub + (sub == 1 ? " persona a cargo" : " personas a cargo")), false));
        });
        if (directos.isEmpty()) {
            Label none = new Label("No tiene personas a su cargo.");
            none.getStyleClass().add("text-subtle");
            rows.getChildren().add(none);
        }
        long total = teamSize(e);
        String title = "Equipo directo (" + directos.size() + ")"
                + (total > directos.size() ? " · " + total + " en total" : "");
        return section(title, rows);
    }

    /** Fila de persona: la propia se resalta; las demás llevan a su ficha. */
    private Node personRow(Empleado p, String caption, boolean self) {
        Label n = new Label(p.getNombreCompleto());
        n.getStyleClass().add("timeline-title");
        Label c = new Label(Formatters.orDash(caption));
        c.getStyleClass().add("timeline-detail");
        VBox text = new VBox(1, n, c);
        text.setMinWidth(0);
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox row = new HBox(10, new Avatar(p.getNombreCompleto(), p.getCodigoEmpleado(), Avatar.Size.MEDIUM), text);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("timeline-row");
        if (self) {
            row.getStyleClass().add("org-self");
        } else {
            row.getChildren().add(Icons.of(Feather.CHEVRON_RIGHT, 14));
            Interactions.activatable(row, () -> showPerson(p));
            row.setAccessibleText("Ver ficha de " + p.getNombreCompleto());
        }
        return row;
    }

    private void showPerson(Empleado p) {
        if (table.getItems().contains(p)) {
            table.getSelectionModel().select(p);
            table.scrollTo(p);
        } else {
            reveal(String.valueOf(p.getCodigoEmpleado()));
        }
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

    private static Node section(String title, Node body) {
        Label l = new Label(title);
        l.getStyleClass().add("detail-section");
        return new VBox(10, l, body);
    }

    // ------------------------------------------------------------- formulario

    @Override
    protected void openEditor(Empleado existing) {
        FxTasks.run(() -> new Lookups(oficinas.listarTodas(), empleados.listarTodos()),
                l -> showForm(existing, l),
                e -> Notifier.error("No se pudieron cargar los datos", FxTasks.friendlyMessage(FxTasks.unwrap(e))));
    }

    private record Lookups(List<Oficina> oficinas, List<Empleado> empleados) {
    }

    private void showForm(Empleado existing, Lookups l) {
        boolean nuevo = existing == null;
        Empleado e = nuevo ? new Empleado() : existing;
        Integer siguiente = l.empleados().stream().map(Empleado::getCodigoEmpleado)
                .max(Comparator.naturalOrder()).map(n -> n + 1).orElse(1);
        Map<Integer, String> nombres = l.empleados().stream()
                .collect(Collectors.toMap(Empleado::getCodigoEmpleado, Empleado::getNombreCompleto, (a, b) -> a));
        Map<String, String> ciudades = l.oficinas().stream()
                .collect(Collectors.toMap(Oficina::getCodigoOficina, Oficina::getCiudad, (a, b) -> a));

        FormDialog form = new FormDialog(Feather.USER_PLUS, nuevo ? "Nuevo empleado" : "Editar " + e.getNombreCompleto(),
                nuevo ? "Se propone el siguiente código libre (" + siguiente + "); puedes cambiarlo."
                        : "El código de empleado no se puede cambiar una vez creado.");
        form.section("Datos personales");
        FormField<Integer> codigo = form.integer("Código", nuevo ? siguiente : e.getCodigoEmpleado()).required().locked(!nuevo)
                .rule(v -> v <= 0 ? "Debe ser un número positivo." : null)
                .rule(uniqueKey(nuevo, Empleado::getCodigoEmpleado, "un empleado"));
        FormField<String> nombre = form.text("Nombre", e.getNombre(), 50).required();
        FormField<String> apellido1 = form.text("Primer apellido", e.getApellido1(), 50).required();
        FormField<String> apellido2 = form.text("Segundo apellido", e.getApellido2(), 50);
        form.section("Puesto y contacto");
        FormField<String> puesto = form.text("Puesto", e.getPuesto(), 50);
        FormField<String> oficina = form.choice("Oficina",
                l.oficinas().stream().map(Oficina::getCodigoOficina).sorted().toList(), e.getCodigoOficina(),
                c -> c + " · " + ciudades.getOrDefault(c, "")).required();
        FormField<String> email = form.text("Email", e.getEmail(), 100).required()
                .rule(v -> v.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") ? null : "Introduce un email válido.");
        FormField<String> extension = form.text("Extensión", e.getExtension(), 10).required();
        FormField<Integer> jefe = form.choice("Jefe directo",
                l.empleados().stream().map(Empleado::getCodigoEmpleado)
                        .filter(c -> !c.equals(e.getCodigoEmpleado())).sorted().toList(),
                e.getCodigoJefe(), c -> nombres.getOrDefault(c, String.valueOf(c)) + " (" + c + ")", "Sin jefe (dirección)").wide();
        form.keyField(codigo);
        form.show(() -> {
            Empleado datos = new Empleado(codigo.value(), nombre.value(), apellido1.value(), apellido2.value(),
                    extension.value(), email.value(), oficina.value(), jefe.value(), puesto.value());
            if (nuevo) {
                empleados.crearEmpleado(datos);
            } else {
                empleados.actualizarEmpleado(datos);
            }
        }, () -> nombre.value() + " " + apellido1.value() + (nuevo ? " se ha dado de alta." : " se ha actualizado."), this::saved);
    }

    @Override
    protected void deleteItem(Empleado item) {
        empleados.eliminarEmpleado(item.getCodigoEmpleado());
    }

    @Override
    protected String describe(Empleado item) {
        return "a " + item.getNombreCompleto();
    }

    @Override
    protected List<String> csvHeader() {
        return List.of("Código", "Nombre", "Primer apellido", "Segundo apellido", "Puesto", "Oficina", "Jefe", "Email", "Extensión");
    }

    @Override
    protected List<String> csvRow(Empleado e) {
        return List.of(str(e.getCodigoEmpleado()), str(e.getNombre()), str(e.getApellido1()), str(e.getApellido2()),
                str(e.getPuesto()), str(e.getCodigoOficina()), str(nombreDe(e.getCodigoJefe())), str(e.getEmail()), str(e.getExtension()));
    }
}
