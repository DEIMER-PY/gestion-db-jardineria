package com.jardineria.infrastructure.adapter.in.desktop.views.modules;

import com.jardineria.application.port.in.EmpleadoUseCase;
import com.jardineria.application.port.in.OficinaUseCase;
import com.jardineria.domain.model.Empleado;
import com.jardineria.domain.model.Oficina;
import com.jardineria.infrastructure.adapter.in.common.Formatters;
import com.jardineria.infrastructure.adapter.in.desktop.components.KpiCard;
import com.jardineria.infrastructure.adapter.in.desktop.components.StatStrip;
import com.jardineria.infrastructure.adapter.in.desktop.components.form.FormDialog;
import com.jardineria.infrastructure.adapter.in.desktop.components.form.FormField;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.support.FxTasks;
import com.jardineria.infrastructure.adapter.in.desktop.support.Notifier;
import com.jardineria.infrastructure.adapter.in.desktop.views.crud.CrudModuleView;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import org.kordamp.ikonli.feather.Feather;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Equipo comercial: puesto, oficina y jerarquía (jefe directo). */
public class EmpleadosView extends CrudModuleView<Empleado> {
    private final EmpleadoUseCase empleados;
    private final OficinaUseCase oficinas;
    private final ComboBox<String> oficinaFilter = new ComboBox<>();
    private volatile Map<String, Oficina> oficinaPorCodigo = Map.of();
    private volatile Map<Integer, Empleado> empleadoPorCodigo = Map.of();

    public EmpleadosView(EmpleadoUseCase empleados, OficinaUseCase oficinas) {
        super(Route.EMPLEADOS, "empleado", "Empleados");
        this.empleados = empleados;
        this.oficinas = oficinas;

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
        numberColumn("Código", Empleado::getCodigoEmpleado, 90).getStyleClass().add("col-code");
        personColumn("Nombre", Empleado::getNombreCompleto, Empleado::getCodigoEmpleado, 240);
        textColumn("Puesto", Empleado::getPuesto, 190);
        textColumn("Oficina", e -> oficinaLabel(e.getCodigoOficina()), 170);
        textColumn("Jefe directo", e -> nombreDe(e.getCodigoJefe()), 200);
        textColumn("Email", Empleado::getEmail, 240);
        codeColumn("Ext.", Empleado::getExtension, 80);
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
