package com.jardineria.infrastructure.adapter.in.desktop.views.modules;

import com.jardineria.application.port.in.GamaProductoUseCase;
import com.jardineria.application.port.in.ProductoUseCase;
import com.jardineria.domain.model.GamaProducto;
import com.jardineria.domain.model.Producto;
import com.jardineria.domain.pattern.flyweight.GamaFlyweightFactory;
import com.jardineria.infrastructure.adapter.in.common.Formatters;
import com.jardineria.infrastructure.adapter.in.desktop.components.KpiCard;
import com.jardineria.infrastructure.adapter.in.desktop.components.StatStrip;
import com.jardineria.infrastructure.adapter.in.desktop.components.form.FormDialog;
import com.jardineria.infrastructure.adapter.in.desktop.components.form.FormField;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import com.jardineria.infrastructure.adapter.in.desktop.views.crud.CrudModuleView;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.material2.Material2AL;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Familias botánicas del catálogo, con su iconografía (Flyweight) y productos asociados. */
public class GamasView extends CrudModuleView<GamaProducto> {
    private final GamaProductoUseCase gamas;
    private final ProductoUseCase productos;
    private volatile Map<String, Long> productosPorGama = Map.of();

    public GamasView(GamaProductoUseCase gamas, ProductoUseCase productos) {
        super(Route.GAMAS, "gama", "Gamas");
        this.gamas = gamas;
        this.productos = productos;
    }

    @Override
    protected List<GamaProducto> fetch() {
        productosPorGama = productos.listarTodos().stream()
                .collect(Collectors.groupingBy(Producto::getGama, Collectors.counting()));
        return gamas.listarTodas();
    }

    @Override
    protected void configureColumns() {
        TableColumn<GamaProducto, String> nombre = column("Gama", GamaProducto::getGama, 220);
        nombre.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String gama, boolean empty) {
                super.updateItem(gama, empty);
                setText(null);
                if (empty || gama == null) {
                    setGraphic(null);
                    return;
                }
                StackPane badge = new StackPane(Icons.of(
                        Icons.forGama(GamaFlyweightFactory.getFlyweight(gama).iconKey()), 16));
                badge.getStyleClass().add("cell-icon");
                Label name = new Label(Formatters.display(gama));
                name.getStyleClass().add("cell-strong");
                HBox box = new HBox(10, badge, name);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        textColumn("Descripción", GamaProducto::getDescripcionTexto, 420);
        numberColumn("Productos", g -> productosPorGama.getOrDefault(g.getGama(), 0L).intValue(), 110);
        textColumn("Imagen", GamaProducto::getImagen, 160);
    }

    @Override
    protected Node buildSummary(List<GamaProducto> all) {
        long total = productosPorGama.values().stream().mapToLong(Long::longValue).sum();
        String principal = productosPorGama.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(e -> Formatters.display(e.getKey())).orElse("—");
        long vacias = all.stream().filter(g -> !productosPorGama.containsKey(g.getGama())).count();
        return new StatStrip()
                .stat(Material2AL.ECO, Formatters.integer(all.size()), "gamas en el catálogo", KpiCard.Tone.ACCENT)
                .stat(Feather.PACKAGE, Formatters.integer(total), "productos clasificados", KpiCard.Tone.SKY)
                .stat(Feather.STAR, principal, "gama con más productos", KpiCard.Tone.AMBER)
                .stat(Feather.ALERT_CIRCLE, Formatters.integer(vacias), "gamas sin productos", KpiCard.Tone.CORAL);
    }

    @Override
    protected String searchableText(GamaProducto g) {
        return g.getGama() + " " + Formatters.display(g.getGama()) + " " + str(g.getDescripcionTexto());
    }

    @Override
    protected void openEditor(GamaProducto existing) {
        boolean nueva = existing == null;
        GamaProducto g = nueva ? new GamaProducto() : existing;
        FormDialog form = new FormDialog(Material2AL.ECO, nueva ? "Nueva gama" : "Editar gama " + Formatters.display(g.getGama()),
                "El nombre de la gama es su clave: los productos la referencian, por eso no se puede renombrar.");
        FormField<String> nombre = form.text("Nombre de la gama", g.getGama(), 50).required().locked(!nueva).wide();
        FormField<String> descripcion = form.area("Descripción", g.getDescripcionTexto());
        FormField<String> imagen = form.text("Imagen (ruta o URL)", g.getImagen(), 256).wide();
        form.keyField(nombre);
        form.show(() -> {
            GamaProducto datos = new GamaProducto(nombre.value(), descripcion.value(), g.getDescripcionHtml(), imagen.value());
            if (nueva) {
                gamas.crearGama(datos);
            } else {
                gamas.actualizarGama(datos);
            }
        }, () -> "Gama " + Formatters.display(nombre.value()) + (nueva ? " creada." : " actualizada."), this::saved);
    }

    @Override
    protected void deleteItem(GamaProducto item) {
        gamas.eliminarGama(item.getGama());
    }

    @Override
    protected String describe(GamaProducto item) {
        return "la gama " + Formatters.display(item.getGama());
    }

    @Override
    protected List<String> csvHeader() {
        return List.of("Gama", "Descripción", "Productos", "Imagen");
    }

    @Override
    protected List<String> csvRow(GamaProducto g) {
        return List.of(g.getGama(), str(g.getDescripcionTexto()),
                String.valueOf(productosPorGama.getOrDefault(g.getGama(), 0L)), str(g.getImagen()));
    }
}
