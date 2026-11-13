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
import com.jardineria.infrastructure.adapter.in.desktop.views.crud.CrudModuleView;
import javafx.scene.Node;
import org.kordamp.ikonli.feather.Feather;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Red de oficinas comerciales, con el número de empleados de cada una. */
public class OficinasView extends CrudModuleView<Oficina> {
    private final OficinaUseCase oficinas;
    private final EmpleadoUseCase empleados;
    private volatile Map<String, Long> plantilla = Map.of();

    public OficinasView(OficinaUseCase oficinas, EmpleadoUseCase empleados) {
        super(Route.OFICINAS, "oficina", "Oficinas");
        this.oficinas = oficinas;
        this.empleados = empleados;
    }

    @Override
    protected List<Oficina> fetch() {
        plantilla = empleados.listarTodos().stream()
                .collect(Collectors.groupingBy(Empleado::getCodigoOficina, Collectors.counting()));
        return oficinas.listarTodas();
    }

    @Override
    protected void configureColumns() {
        codeColumn("Código", Oficina::getCodigoOficina, 100);
        textColumn("Ciudad", Oficina::getCiudad, 150);
        textColumn("País", o -> Formatters.display(o.getPais()), 140);
        textColumn("Región", Oficina::getRegion, 140);
        textColumn("Dirección", o -> o.getLineaDireccion1()
                + (o.getLineaDireccion2() == null ? "" : ", " + o.getLineaDireccion2()), 260);
        codeColumn("Teléfono", Oficina::getTelefono, 150);
        numberColumn("Empleados", o -> plantilla.getOrDefault(o.getCodigoOficina(), 0L).intValue(), 110);
    }

    @Override
    protected Node buildSummary(List<Oficina> all) {
        long paises = all.stream().map(o -> Formatters.display(o.getPais())).distinct().count();
        long total = plantilla.values().stream().mapToLong(Long::longValue).sum();
        String mayor = plantilla.entrySet().stream().max(Map.Entry.comparingByValue())
                .flatMap(e -> all.stream().filter(o -> o.getCodigoOficina().equals(e.getKey())).findFirst())
                .map(Oficina::getCiudad).orElse("—");
        return new StatStrip()
                .stat(Feather.MAP_PIN, Formatters.integer(all.size()), "oficinas activas", KpiCard.Tone.ACCENT)
                .stat(Feather.GLOBE, Formatters.integer(paises), "países con presencia", KpiCard.Tone.SKY)
                .stat(Feather.USERS, Formatters.integer(total), "empleados asignados", KpiCard.Tone.LILAC)
                .stat(Feather.AWARD, mayor, "oficina con más plantilla", KpiCard.Tone.AMBER);
    }

    @Override
    protected String searchableText(Oficina o) {
        return String.join(" ", str(o.getCodigoOficina()), str(o.getCiudad()), str(o.getPais()),
                Formatters.display(o.getPais()), str(o.getRegion()), str(o.getLineaDireccion1()), str(o.getTelefono()));
    }

    @Override
    protected void openEditor(Oficina existing) {
        boolean nuevo = existing == null;
        Oficina o = nuevo ? new Oficina() : existing;
        FormDialog form = new FormDialog(Feather.MAP_PIN, nuevo ? "Nueva oficina" : "Editar oficina " + o.getCodigoOficina(),
                "Los campos con * son obligatorios. El código no se puede cambiar una vez creado.");
        FormField<String> codigo = form.text("Código", o.getCodigoOficina(), 10).required().locked(!nuevo)
                .rule(v -> v.matches("[A-Za-z0-9-]+") ? null : "Solo letras, números y guiones (p. ej. MAD-ES).")
                .rule(uniqueKey(nuevo, Oficina::getCodigoOficina, "una oficina"));
        FormField<String> telefono = form.text("Teléfono", o.getTelefono(), 20).required();
        FormField<String> ciudad = form.text("Ciudad", o.getCiudad(), 30).required();
        FormField<String> pais = form.text("País", o.getPais(), 50).required();
        FormField<String> region = form.text("Región", o.getRegion(), 50);
        FormField<String> cp = form.text("Código postal", o.getCodigoPostal(), 10).required();
        FormField<String> dir1 = form.text("Dirección", o.getLineaDireccion1(), 50).required().wide();
        FormField<String> dir2 = form.text("Dirección (línea 2)", o.getLineaDireccion2(), 50).wide();
        form.keyField(codigo);

        form.show(() -> {
            Oficina datos = new Oficina(codigo.value(), ciudad.value(), pais.value(), region.value(),
                    cp.value(), telefono.value(), dir1.value(), dir2.value());
            if (nuevo) {
                oficinas.crearOficina(datos);
            } else {
                oficinas.actualizarOficina(datos);
            }
        }, () -> "Oficina " + codigo.value() + (nuevo ? " creada." : " actualizada."), this::saved);
    }

    @Override
    protected void deleteItem(Oficina item) {
        oficinas.eliminarOficina(item.getCodigoOficina());
    }

    @Override
    protected String describe(Oficina item) {
        return "la oficina " + item.getCodigoOficina() + " (" + item.getCiudad() + ")";
    }

    @Override
    protected List<String> csvHeader() {
        return List.of("Código", "Ciudad", "País", "Región", "Código postal", "Teléfono", "Dirección 1", "Dirección 2", "Empleados");
    }

    @Override
    protected List<String> csvRow(Oficina o) {
        return List.of(o.getCodigoOficina(), str(o.getCiudad()), Formatters.display(o.getPais()), str(o.getRegion()),
                str(o.getCodigoPostal()), str(o.getTelefono()), str(o.getLineaDireccion1()), str(o.getLineaDireccion2()),
                String.valueOf(plantilla.getOrDefault(o.getCodigoOficina(), 0L)));
    }
}
