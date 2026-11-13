package com.jardineria.infrastructure.adapter.in.desktop.views.modules;

import com.jardineria.application.port.in.ClienteUseCase;
import com.jardineria.application.port.in.PagoUseCase;
import com.jardineria.domain.model.Cliente;
import com.jardineria.domain.model.Pago;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Cobros registrados por cliente y forma de pago (clave compuesta cliente + transacción). */
public class PagosView extends CrudModuleView<Pago> {
    private static final List<String> FORMAS_BASE = List.of("Transferencia", "PayPal", "Cheque", "Tarjeta");

    private final PagoUseCase pagos;
    private final ClienteUseCase clientes;
    private final ComboBox<String> formaFilter = new ComboBox<>();
    private volatile Map<Integer, String> nombres = Map.of();

    public PagosView(PagoUseCase pagos, ClienteUseCase clientes) {
        super(Route.PAGOS, "pago", "Pagos");
        this.pagos = pagos;
        this.clientes = clientes;
        formaFilter.setPromptText("Todas las formas de pago");
        formaFilter.setButtonCell(formaCell(true));
        formaFilter.setCellFactory(lv -> formaCell(false));
        formaFilter.setAccessibleText("Filtrar por forma de pago");
        formaFilter.valueProperty().addListener((obs, o, n) ->
                setExtraFilter(p -> n == null || n.equalsIgnoreCase(p.getFormaPago())));
        addFilter(formaFilter);
    }

    private static ListCell<String> formaCell(boolean button) {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty && !button ? null : item == null ? "Todas las formas de pago" : item);
            }
        };
    }

    private String cliente(Integer codigo) {
        return nombres.getOrDefault(codigo, "Cliente " + codigo);
    }

    @Override
    protected List<Pago> fetch() {
        nombres = clientes.listarTodos().stream()
                .collect(Collectors.toMap(Cliente::getCodigoCliente, Cliente::getNombreCliente, (a, b) -> a));
        return pagos.listarTodos();
    }

    @Override
    protected void afterLoad(List<Pago> items) {
        List<String> opciones = new ArrayList<>();
        opciones.add(null);
        items.stream().map(Pago::getFormaPago).filter(Objects::nonNull).distinct().sorted().forEach(opciones::add);
        if (!formaFilter.getItems().equals(opciones)) {
            String actual = formaFilter.getValue();
            formaFilter.setItems(FXCollections.observableArrayList(opciones));
            formaFilter.setValue(actual);
        }
    }

    @Override
    protected void configureColumns() {
        dateColumn("Fecha", Pago::getFechaPago, 130);
        textColumn("Cliente", p -> cliente(p.getCodigoCliente()), 240).getStyleClass().add("col-strong");
        textColumn("Forma de pago", Pago::getFormaPago, 160);
        codeColumn("Transacción", Pago::getIdTransaccion, 200);
        moneyColumn("Importe", Pago::getTotal, 140);
        table.getSortOrder().add(table.getColumns().get(0));
        table.getColumns().get(0).setSortType(javafx.scene.control.TableColumn.SortType.DESCENDING);
    }

    @Override
    protected Node buildSummary(List<Pago> all) {
        BigDecimal total = all.stream().map(Pago::getTotal).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal medio = all.isEmpty() ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(all.size()), 2, java.math.RoundingMode.HALF_UP);
        long pagadores = all.stream().map(Pago::getCodigoCliente).distinct().count();
        String ultimo = all.stream().map(Pago::getFechaPago).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).map(Formatters::date).orElse("—");
        return new StatStrip()
                .stat(Feather.CREDIT_CARD, Formatters.compactCurrency(total), "cobrado en total", KpiCard.Tone.SKY)
                .stat(Feather.HASH, Formatters.integer(all.size()), "pagos registrados", KpiCard.Tone.JADE)
                .stat(Feather.DIVIDE, Formatters.currency(medio), "importe medio", KpiCard.Tone.LILAC)
                .stat(Feather.USERS, Formatters.integer(pagadores), "clientes con pagos", KpiCard.Tone.AMBER)
                .stat(Feather.CALENDAR, ultimo, "último cobro", KpiCard.Tone.LIME);
    }

    @Override
    protected String searchableText(Pago p) {
        return String.join(" ", cliente(p.getCodigoCliente()), str(p.getCodigoCliente()), str(p.getFormaPago()),
                str(p.getIdTransaccion()), Formatters.date(p.getFechaPago()));
    }

    @Override
    protected void openEditor(Pago existing) {
        FxTasks.run(clientes::listarTodos, lista -> showForm(existing, lista),
                e -> Notifier.error("No se pudieron cargar los clientes", FxTasks.friendlyMessage(FxTasks.unwrap(e))));
    }

    private void showForm(Pago existing, List<Cliente> listaClientes) {
        boolean nuevo = existing == null;
        Pago p = nuevo ? new Pago() : existing;
        Map<Integer, String> mapa = listaClientes.stream()
                .collect(Collectors.toMap(Cliente::getCodigoCliente, Cliente::getNombreCliente, (a, b) -> a));
        TreeSet<String> formas = new TreeSet<>(FORMAS_BASE);
        allItems().stream().map(Pago::getFormaPago).filter(Objects::nonNull).forEach(formas::add);

        FormDialog form = new FormDialog(Feather.CREDIT_CARD, nuevo ? "Registrar pago" : "Editar pago " + p.getIdTransaccion(),
                "Cliente y transacción forman la clave del pago y no se pueden cambiar al editar.");
        FormField<Integer> cliente = form.choice("Cliente",
                listaClientes.stream().map(Cliente::getCodigoCliente).sorted().toList(), p.getCodigoCliente(),
                c -> mapa.getOrDefault(c, "?") + " (" + c + ")").required().locked(!nuevo).wide();
        FormField<String> transaccion = form.text("Id. de transacción", p.getIdTransaccion(), 50).required().locked(!nuevo);
        FormField<String> forma = form.choice("Forma de pago", List.copyOf(formas), p.getFormaPago(), s -> s).required();
        FormField<LocalDate> fecha = form.date("Fecha de pago", nuevo ? LocalDate.now() : p.getFechaPago()).required()
                .rule(d -> d.isAfter(LocalDate.now()) ? "La fecha no puede ser futura." : null);
        FormField<BigDecimal> total = form.decimal("Importe (€)", p.getTotal()).required()
                .rule(v -> v.signum() <= 0 ? "El importe debe ser mayor que cero." : null);
        form.keyField(transaccion);
        form.show(() -> {
            Pago datos = new Pago(cliente.value(), forma.value(), transaccion.value(), fecha.value(), total.value());
            if (nuevo) {
                pagos.registrarPago(datos);
            } else {
                pagos.actualizarPago(datos);
            }
        }, () -> "Pago de " + Formatters.currency(total.value()) + (nuevo ? " registrado." : " actualizado."), this::saved);
    }

    @Override
    protected void deleteItem(Pago item) {
        pagos.eliminarPago(item.getId());
    }

    @Override
    protected String describe(Pago item) {
        return "el pago " + item.getIdTransaccion() + " de " + cliente(item.getCodigoCliente());
    }

    @Override
    protected List<String> csvHeader() {
        return List.of("Fecha", "Código cliente", "Cliente", "Forma de pago", "Transacción", "Importe");
    }

    @Override
    protected List<String> csvRow(Pago p) {
        return List.of(Formatters.shortDate(p.getFechaPago()), str(p.getCodigoCliente()), cliente(p.getCodigoCliente()),
                str(p.getFormaPago()), str(p.getIdTransaccion()),
                p.getTotal() == null ? "" : Formatters.decimal(p.getTotal().doubleValue(), 2));
    }
}
