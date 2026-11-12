package com.jardineria.infrastructure.adapter.in.desktop.views.modules;

import com.jardineria.application.port.in.DashboardUseCase;
import com.jardineria.application.port.in.GamaProductoUseCase;
import com.jardineria.application.port.in.ProductoUseCase;
import com.jardineria.domain.model.GamaProducto;
import com.jardineria.domain.model.Producto;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.feather.Feather;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Catálogo con control de stock: alerta visual bajo el umbral y filtros por gama y stock crítico. */
public class ProductosView extends CrudModuleView<Producto> {
    private static final int UMBRAL = DashboardUseCase.UMBRAL_STOCK_CRITICO;

    private final ProductoUseCase productos;
    private final GamaProductoUseCase gamas;
    private final ComboBox<String> gamaFilter = new ComboBox<>();
    private final ToggleButton soloCritico = new ToggleButton("Stock crítico");

    public ProductosView(ProductoUseCase productos, GamaProductoUseCase gamas) {
        super(Route.PRODUCTOS, "producto", "Productos");
        this.productos = productos;
        this.gamas = gamas;

        gamaFilter.setPromptText("Todas las gamas");
        gamaFilter.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "Todas las gamas" : Formatters.display(item));
            }
        });
        gamaFilter.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? "Todas las gamas" : Formatters.display(item));
            }
        });
        gamaFilter.setAccessibleText("Filtrar por gama");
        gamaFilter.valueProperty().addListener((obs, o, n) -> updateFilter());
        soloCritico.setTooltip(new Tooltip("Mostrar solo productos con menos de " + UMBRAL + " unidades"));
        soloCritico.selectedProperty().addListener((obs, o, n) -> updateFilter());
        addFilter(gamaFilter);
        addFilter(soloCritico);
    }

    private void updateFilter() {
        String gama = gamaFilter.getValue();
        boolean critico = soloCritico.isSelected();
        setExtraFilter(p -> (gama == null || gama.equals(p.getGama()))
                && (!critico || stock(p) < UMBRAL));
    }

    @Override
    protected void afterLoad(List<Producto> items) {
        List<String> opciones = new ArrayList<>();
        opciones.add(null);
        items.stream().map(Producto::getGama).filter(Objects::nonNull).distinct().sorted().forEach(opciones::add);
        if (!gamaFilter.getItems().equals(opciones)) {
            String actual = gamaFilter.getValue();
            gamaFilter.setItems(FXCollections.observableArrayList(opciones));
            gamaFilter.setValue(actual);
        }
    }

    @Override
    protected List<Producto> fetch() {
        return productos.listarTodos();
    }

    @Override
    protected void configureColumns() {
        codeColumn("Código", Producto::getCodigoProducto, 100);
        TableColumn<Producto, String> nombre = textColumn("Producto", Producto::getNombre, 240);
        nombre.getStyleClass().add("col-strong");
        textColumn("Gama", p -> Formatters.display(p.getGama()), 130);
        textColumn("Proveedor", Producto::getProveedor, 160);
        textColumn("Dimensiones", Producto::getDimensiones, 110);
        TableColumn<Producto, Integer> stock = numberColumn("Stock", Producto::getCantidadEnStock, 100);
        stock.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("stock-low", "stock-critical");
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(Formatters.integer(item) + " uds");
                if (item <= UMBRAL / 2) {
                    getStyleClass().add("stock-critical");
                } else if (item < UMBRAL) {
                    getStyleClass().add("stock-low");
                }
            }
        });
        moneyColumn("Precio venta", Producto::getPrecioVenta, 120);
        moneyColumn("Precio proveedor", Producto::getPrecioProveedor, 140);
        TableColumn<Producto, BigDecimal> margen = column("Margen", ProductosView::margen, 100);
        margen.getStyleClass().add("col-number");
        margen.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? "—" : Formatters.decimal(item.doubleValue(), 1) + " %");
            }
        });
    }

    private static BigDecimal margen(Producto p) {
        if (p.getPrecioVenta() == null || p.getPrecioProveedor() == null || p.getPrecioVenta().signum() == 0) {
            return null;
        }
        return p.getPrecioVenta().subtract(p.getPrecioProveedor())
                .multiply(BigDecimal.valueOf(100)).divide(p.getPrecioVenta(), 1, RoundingMode.HALF_UP);
    }

    private static int stock(Producto p) {
        return p.getCantidadEnStock() == null ? 0 : p.getCantidadEnStock();
    }

    @Override
    protected Node buildSummary(List<Producto> all) {
        long criticos = all.stream().filter(p -> stock(p) < UMBRAL).count();
        long unidades = all.stream().mapToLong(ProductosView::stock).sum();
        BigDecimal valor = all.stream()
                .filter(p -> p.getPrecioVenta() != null)
                .map(p -> p.getPrecioVenta().multiply(BigDecimal.valueOf(stock(p))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new StatStrip()
                .stat(Feather.PACKAGE, Formatters.integer(all.size()), "productos en catálogo", KpiCard.Tone.JADE)
                .stat(Feather.LAYERS, Formatters.integer(unidades), "unidades en almacén", KpiCard.Tone.SKY)
                .stat(Feather.DOLLAR_SIGN, Formatters.compactCurrency(valor), "valor del inventario (PVP)", KpiCard.Tone.LIME)
                .stat(Feather.ALERT_TRIANGLE, Formatters.integer(criticos), "bajo " + UMBRAL + " unidades", KpiCard.Tone.CORAL);
    }

    @Override
    protected String searchableText(Producto p) {
        return String.join(" ", str(p.getCodigoProducto()), str(p.getNombre()), str(p.getGama()),
                Formatters.display(p.getGama()), str(p.getProveedor()), str(p.getDescripcion()));
    }

    @Override
    protected void openEditor(Producto existing) {
        FxTasks.run(gamas::listarTodas, lista -> showForm(existing, lista),
                e -> Notifier.error("No se pudieron cargar las gamas", FxTasks.friendlyMessage(FxTasks.unwrap(e))));
    }

    private void showForm(Producto existing, List<GamaProducto> listaGamas) {
        boolean nuevo = existing == null;
        Producto p = nuevo ? new Producto() : existing;
        List<String> nombresGama = listaGamas.stream().map(GamaProducto::getGama).toList();
        FormDialog form = new FormDialog(Feather.PACKAGE, nuevo ? "Nuevo producto" : "Editar " + p.getNombre(),
                "Precios en euros; puedes usar coma decimal. El código no se puede cambiar una vez creado.");
        form.section("Identificación");
        FormField<String> codigo = form.text("Código", p.getCodigoProducto(), 15).required().locked(!nuevo);
        FormField<String> gama = form.choice("Gama", nombresGama, p.getGama(), Formatters::display).required();
        FormField<String> nombre = form.text("Nombre", p.getNombre(), 70).required().wide();
        FormField<String> proveedor = form.text("Proveedor", p.getProveedor(), 50);
        FormField<String> dimensiones = form.text("Dimensiones", p.getDimensiones(), 25);
        FormField<String> descripcion = form.area("Descripción", p.getDescripcion());
        form.section("Stock y precios");
        FormField<Integer> stock = form.integer("Unidades en stock", p.getCantidadEnStock()).required()
                .rule(v -> v < 0 ? "El stock no puede ser negativo." : v > Short.MAX_VALUE ? "Máximo " + Short.MAX_VALUE + " unidades." : null);
        FormField<BigDecimal> venta = form.decimal("Precio de venta (€)", p.getPrecioVenta()).required()
                .rule(v -> v.signum() <= 0 ? "El precio debe ser mayor que cero." : null);
        FormField<BigDecimal> compra = form.decimal("Precio de proveedor (€)", p.getPrecioProveedor())
                .rule(v -> v.signum() < 0 ? "El precio no puede ser negativo." : null);
        form.keyField(codigo);
        form.show(() -> {
            Producto datos = new Producto(codigo.value(), nombre.value(), gama.value(), dimensiones.value(),
                    proveedor.value(), descripcion.value(), stock.value(), venta.value(), compra.value());
            if (nuevo) {
                productos.crearProducto(datos);
            } else {
                productos.actualizarProducto(datos);
            }
        }, () -> nombre.value() + (nuevo ? " añadido al catálogo." : " actualizado."), this::saved);
    }

    @Override
    protected void deleteItem(Producto item) {
        productos.eliminarProducto(item.getCodigoProducto());
    }

    @Override
    protected String describe(Producto item) {
        return "el producto «" + item.getNombre() + "»";
    }

    @Override
    protected List<String> csvHeader() {
        return List.of("Código", "Nombre", "Gama", "Proveedor", "Dimensiones", "Stock", "Precio venta", "Precio proveedor");
    }

    @Override
    protected List<String> csvRow(Producto p) {
        return List.of(p.getCodigoProducto(), str(p.getNombre()), Formatters.display(p.getGama()), str(p.getProveedor()),
                str(p.getDimensiones()), String.valueOf(stock(p)),
                p.getPrecioVenta() == null ? "" : Formatters.decimal(p.getPrecioVenta().doubleValue(), 2),
                p.getPrecioProveedor() == null ? "" : Formatters.decimal(p.getPrecioProveedor().doubleValue(), 2));
    }
}
