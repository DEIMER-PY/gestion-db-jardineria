package com.jardineria.infrastructure.adapter.in.desktop.views.patterns;

import com.jardineria.domain.model.Producto;
import com.jardineria.domain.pattern.composite.CatalogComponent;
import com.jardineria.domain.pattern.composite.ProductBundleComposite;
import com.jardineria.domain.pattern.composite.ProductLeaf;
import com.jardineria.infrastructure.adapter.in.common.Formatters;
import com.jardineria.infrastructure.adapter.in.desktop.components.KpiCard;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Composite: kits que contienen productos u otros kits y se tratan como un único artículo. */
final class CompositeDemo implements PatternDemo {
    private static final String ROOT_CODE = "KIT-HUERTO";
    private static final String ROOT_NAME = "Kit Huerto Urbano";

    @Override
    public String name() {
        return "Composite";
    }

    @Override
    public Ikon icon() {
        return Feather.GIT_MERGE;
    }

    @Override
    public KpiCard.Tone tone() {
        return KpiCard.Tone.AMBER;
    }

    @Override
    public String tagline() {
        return "Trata igual a un producto suelto y a un lote de productos.";
    }

    @Override
    public String intent() {
        return "Un kit de jardinería puede contener productos y también otros kits. Hojas (ProductLeaf) y compuestos "
                + "(ProductBundleComposite) implementan CatalogComponent, de modo que el precio del pack se calcula "
                + "recursivamente aplicando el descuento de cada nivel y el stock ensamblable es el mínimo de sus piezas.";
    }

    @Override
    public List<String> participants() {
        return List.of("CatalogComponent", "ProductLeaf", "ProductBundleComposite");
    }

    @Override
    public String usage() {
        return "Permite vender lotes promocionales sin crear tablas nuevas: el kit se arma a partir del catálogo existente.";
    }

    @Override
    public String image() {
        return "composite.jpg";
    }

    @Override
    public Node build(PatternData data) {
        List<CatalogComponent> piezas = new ArrayList<>();
        data.firstOfGama("Aromat").map(ProductLeaf::new).ifPresent(piezas::add);
        data.firstOfGama("Frutales").map(ProductLeaf::new).ifPresent(piezas::add);
        ProductBundleComposite herramientas = new ProductBundleComposite("KIT-HERR", "Set de herramientas", new BigDecimal("0.05"));
        data.productos().stream()
                .filter(p -> DemoUi.normalize(p.getGama()).startsWith("herramienta"))
                .filter(p -> p.getCantidadEnStock() != null && p.getCantidadEnStock() > 0)
                .limit(2)
                .forEach(p -> herramientas.agregarComponente(new ProductLeaf(p)));
        if (!herramientas.getComponentes().isEmpty()) {
            piezas.add(herramientas);
        }

        TreeView<CatalogComponent> tree = new TreeView<>();
        tree.getStyleClass().add("composite-tree");
        tree.setCellFactory(tv -> new ComponentCell());
        tree.setPrefHeight(300);
        tree.setAccessibleText("Estructura del kit");

        Slider descuento = new Slider(0, 30, 10);
        descuento.setMajorTickUnit(5);
        descuento.setMinorTickCount(0);
        descuento.setSnapToTicks(true);
        descuento.setPrefWidth(200);
        descuento.setAccessibleText("Descuento del kit");
        Label descuentoValue = new Label();
        descuentoValue.getStyleClass().add("mono");

        ComboBox<Producto> producto = DemoUi.productPicker(data.productos());
        Button add = new Button("Añadir al kit", Icons.of(Feather.PLUS, 14));
        Button remove = new Button("Quitar pieza", Icons.of(Feather.MINUS, 14));
        remove.disableProperty().bind(tree.getSelectionModel().selectedItemProperty().map(
                item -> item == null || item.getParent() == null).orElse(true));

        Label precio = new Label();
        Label suma = new Label();
        Label stock = new Label();
        Label hojas = new Label();
        Label render = new Label();
        render.getStyleClass().addAll("demo-code", "mono");
        render.setWrapText(true);
        render.setMinHeight(Region.USE_PREF_SIZE);

        Runnable rebuild = () -> {
            BigDecimal pct = BigDecimal.valueOf(Math.round(descuento.getValue())).movePointLeft(2);
            descuentoValue.setText(Math.round(descuento.getValue()) + " %");
            ProductBundleComposite kit = new ProductBundleComposite(ROOT_CODE, ROOT_NAME, pct);
            piezas.forEach(kit::agregarComponente);
            TreeItem<CatalogComponent> root = toItem(kit);
            root.setExpanded(true);
            tree.setRoot(root);

            BigDecimal sinDescuento = piezas.stream().map(CatalogComponent::getPrecio).reduce(BigDecimal.ZERO, BigDecimal::add);
            precio.setText(Formatters.currency(kit.getPrecio()));
            suma.setText(Formatters.currency(sinDescuento));
            stock.setText(Formatters.integer(kit.getStockDisponible()));
            hojas.setText(Formatters.integer(countLeaves(kit)));
            StringBuilder sb = new StringBuilder();
            kit.renderEstructura(sb, "");
            render.setText(sb.toString().strip());
        };
        descuento.valueProperty().addListener((o, a, b) -> rebuild.run());
        add.setOnAction(e -> {
            if (producto.getValue() != null) {
                piezas.add(new ProductLeaf(producto.getValue()));
                rebuild.run();
            }
        });
        remove.setOnAction(e -> {
            TreeItem<CatalogComponent> sel = tree.getSelectionModel().getSelectedItem();
            if (sel == null || sel.getParent() == null) {
                return;
            }
            CatalogComponent target = sel.getValue();
            if (!piezas.remove(target)) {
                piezas.stream().filter(c -> c instanceof ProductBundleComposite)
                        .map(c -> (ProductBundleComposite) c)
                        .forEach(b -> b.removerComponente(target));
                piezas.removeIf(c -> c instanceof ProductBundleComposite b && b.getComponentes().isEmpty());
            }
            rebuild.run();
        });
        rebuild.run();

        HBox discountRow = new HBox(10, descuento, descuentoValue);
        discountRow.setAlignment(Pos.CENTER_LEFT);

        VBox treeBox = new VBox(10, DemoUi.title("Árbol del kit"), tree);
        HBox.setHgrow(treeBox, Priority.ALWAYS);
        treeBox.setMinWidth(280);
        VBox summary = new VBox(18,
                DemoUi.metric(precio, "precio del pack"),
                DemoUi.metric(suma, "suma de piezas sin descuento"),
                DemoUi.metric(stock, "packs ensamblables (mínimo de las piezas)"),
                DemoUi.metric(hojas, "productos finales"));
        summary.getStyleClass().add("composite-summary");
        summary.setMinWidth(200);
        HBox body = new HBox(20, treeBox, summary);

        return new VBox(16,
                DemoUi.controls(DemoUi.field("Producto", producto), add, remove,
                        DemoUi.field("Descuento del kit principal", discountRow)),
                DemoUi.output(body, DemoUi.title("renderEstructura()"), render));
    }

    private static TreeItem<CatalogComponent> toItem(CatalogComponent c) {
        TreeItem<CatalogComponent> item = new TreeItem<>(c);
        if (c instanceof ProductBundleComposite bundle) {
            bundle.getComponentes().forEach(child -> item.getChildren().add(toItem(child)));
            item.setExpanded(true);
        }
        return item;
    }

    private static int countLeaves(CatalogComponent c) {
        if (c instanceof ProductBundleComposite bundle) {
            return bundle.getComponentes().stream().mapToInt(CompositeDemo::countLeaves).sum();
        }
        return 1;
    }

    private static final class ComponentCell extends TreeCell<CatalogComponent> {
        @Override
        protected void updateItem(CatalogComponent item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            boolean kit = item instanceof ProductBundleComposite;
            Label tag = DemoUi.tag(kit ? "KIT" : item instanceof ProductLeaf leaf
                    ? Formatters.display(leaf.getProducto().getGama()) : "", kit ? "amber" : "jade");
            Label name = new Label(item.getNombre());
            name.getStyleClass().add(kit ? "tree-kit" : "tree-leaf");
            name.setMinWidth(0);
            HBox.setHgrow(name, Priority.ALWAYS);
            name.setMaxWidth(Double.MAX_VALUE);
            Label stock = new Label(item.getStockDisponible() + " uds");
            stock.getStyleClass().addAll("tree-figure", "mono");
            stock.setMinWidth(Region.USE_PREF_SIZE);
            Label price = new Label(Formatters.currency(item.getPrecio()));
            price.getStyleClass().addAll("tree-price", "mono");
            price.setMinWidth(Region.USE_PREF_SIZE);
            HBox row = new HBox(10, tag, name, stock, price);
            row.setAlignment(Pos.CENTER_LEFT);
            setText(null);
            setGraphic(row);
        }
    }
}
