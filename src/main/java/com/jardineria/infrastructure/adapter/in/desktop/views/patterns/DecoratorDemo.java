package com.jardineria.infrastructure.adapter.in.desktop.views.patterns;

import atlantafx.base.controls.ToggleSwitch;
import com.jardineria.domain.model.Producto;
import com.jardineria.domain.pattern.decorator.BasePriceCalculator;
import com.jardineria.domain.pattern.decorator.BulkDiscountDecorator;
import com.jardineria.domain.pattern.decorator.PriceCalculator;
import com.jardineria.domain.pattern.decorator.SeasonalPromoDecorator;
import com.jardineria.domain.pattern.decorator.VatTaxDecorator;
import com.jardineria.infrastructure.adapter.in.common.Formatters;
import com.jardineria.infrastructure.adapter.in.desktop.components.KpiCard;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/** Decorator: capas de precio que se apilan sobre el cálculo base sin modificarlo. */
final class DecoratorDemo implements PatternDemo {

    private record Layer(String name, String className, UnaryOperator<PriceCalculator> wrap, ToggleSwitch toggle) {
    }

    @Override
    public String name() {
        return "Decorator";
    }

    @Override
    public Ikon icon() {
        return Feather.LAYERS;
    }

    @Override
    public KpiCard.Tone tone() {
        return KpiCard.Tone.LIME;
    }

    @Override
    public String tagline() {
        return "Añade responsabilidades en capas sin tocar la clase original.";
    }

    @Override
    public String intent() {
        return "El cálculo del precio de una línea de pedido empieza en una calculadora base (unidades × precio) "
                + "y cada regla comercial (descuento por volumen, promoción de temporada, IVA) la envuelve como una "
                + "capa independiente. Las capas comparten la interfaz PriceCalculator, así que se combinan en "
                + "cualquier orden y se activan o desactivan en tiempo de ejecución.";
    }

    @Override
    public List<String> participants() {
        return List.of("PriceCalculator", "BasePriceCalculator", "AbstractPriceDecorator",
                "BulkDiscountDecorator", "SeasonalPromoDecorator", "VatTaxDecorator");
    }

    @Override
    public String usage() {
        return "JardineriaBusinessFacade construye esta cadena en cada checkout para calcular el total facturado.";
    }

    @Override
    public String image() {
        return "decorator.jpg";
    }

    @Override
    public Node build(PatternData data) {
        ComboBox<Producto> producto = DemoUi.productPicker(data.productos());
        data.firstOfGama("Frutales").ifPresent(producto::setValue);
        Spinner<Integer> cantidad = new Spinner<>(1, 999, 12);
        cantidad.setEditable(true);
        cantidad.setPrefWidth(110);
        cantidad.setAccessibleText("Unidades");

        List<Layer> layers = List.of(
                new Layer("Descuento por volumen", "BulkDiscountDecorator", BulkDiscountDecorator::new,
                        new ToggleSwitch("Volumen (-10 % desde 10 uds)")),
                new Layer("Promoción de temporada", "SeasonalPromoDecorator", SeasonalPromoDecorator::new,
                        new ToggleSwitch("Temporada (-5 %)")),
                new Layer("IVA", "VatTaxDecorator", VatTaxDecorator::new,
                        new ToggleSwitch("IVA (+21 %)")));
        layers.forEach(l -> l.toggle().setSelected(true));

        VBox stack = new VBox(8);
        stack.getStyleClass().add("decorator-stack");
        Label chain = new Label();
        chain.getStyleClass().addAll("demo-code", "mono");
        chain.setWrapText(true);
        chain.setMinHeight(Region.USE_PREF_SIZE);
        Label total = new Label();
        total.getStyleClass().add("demo-total");
        Label totalCaption = new Label();
        totalCaption.getStyleClass().add("demo-metric-label");

        Runnable recompute = () -> {
            Producto p = producto.getValue();
            if (p == null || p.getPrecioVenta() == null) {
                return;
            }
            int uds = cantidad.getValue();
            BigDecimal precio = p.getPrecioVenta();

            // Se construye la cadena capa a capa para mostrar el subtotal acumulado en cada nivel.
            PriceCalculator calc = new BasePriceCalculator();
            String expr = "new BasePriceCalculator()";
            BigDecimal previo = calc.calcularTotal(precio, uds);
            List<Node> rows = new ArrayList<>();
            rows.add(layerRow(0, "Precio base", "BasePriceCalculator",
                    uds + " uds × " + Formatters.currency(precio), null, previo));
            int depth = 1;
            for (Layer layer : layers) {
                if (!layer.toggle().isSelected()) {
                    continue;
                }
                calc = layer.wrap().apply(calc);
                expr = "new " + layer.className() + "(" + expr + ")";
                BigDecimal actual = calc.calcularTotal(precio, uds);
                List<String> desglose = calc.getDesglose();
                String detalle = desglose.isEmpty() ? "" : desglose.get(desglose.size() - 1);
                rows.add(layerRow(depth++, layer.name(), layer.className(), detalle, actual.subtract(previo), actual));
                previo = actual;
            }
            java.util.Collections.reverse(rows); // la capa exterior arriba, como una pila
            stack.getChildren().setAll(rows);
            chain.setText("PriceCalculator calc = " + expr + ";");
            total.setText(Formatters.currency(previo));
            totalCaption.setText("Total de " + uds + " × " + p.getNombre());
        };
        producto.valueProperty().addListener((o, a, b) -> recompute.run());
        cantidad.valueProperty().addListener((o, a, b) -> recompute.run());
        layers.forEach(l -> l.toggle().selectedProperty().addListener((o, a, b) -> recompute.run()));
        recompute.run();

        HBox switches = new HBox(18);
        layers.forEach(l -> switches.getChildren().add(l.toggle()));
        switches.setAlignment(Pos.CENTER_LEFT);

        VBox totalBox = new VBox(2, total, totalCaption);
        totalBox.setAlignment(Pos.CENTER_RIGHT);
        HBox footer = new HBox(16, chain, totalBox);
        HBox.setHgrow(chain, Priority.ALWAYS);
        chain.setMaxWidth(Double.MAX_VALUE);
        footer.setAlignment(Pos.CENTER_LEFT);

        return new VBox(16,
                DemoUi.controls(DemoUi.field("Producto", producto), DemoUi.field("Unidades", cantidad)),
                DemoUi.field("Capas activas", switches),
                DemoUi.output(DemoUi.title("Pila de decoradores (la capa exterior se evalúa la última)"), stack, footer));
    }

    private static Node layerRow(int depth, String name, String cls, String detail, BigDecimal delta, BigDecimal subtotal) {
        Label lblName = new Label(name);
        lblName.getStyleClass().add("layer-name");
        Label lblClass = new Label(cls);
        lblClass.getStyleClass().addAll("layer-class", "mono");
        Label lblDetail = new Label(detail);
        lblDetail.getStyleClass().add("layer-detail");
        lblDetail.setWrapText(true);
        VBox text = new VBox(2, new HBox(10, lblName, lblClass), lblDetail);
        text.setMinWidth(0);
        HBox.setHgrow(text, Priority.ALWAYS);

        VBox figures = new VBox(2);
        figures.setAlignment(Pos.CENTER_RIGHT);
        Label sub = new Label(Formatters.currency(subtotal));
        sub.getStyleClass().addAll("layer-subtotal", "mono");
        figures.getChildren().add(sub);
        if (delta != null) {
            Label d = new Label((delta.signum() >= 0 ? "+" : "−") + Formatters.currency(delta.abs()));
            d.getStyleClass().addAll("layer-delta", "mono",
                    delta.signum() > 0 ? "delta-up" : delta.signum() < 0 ? "delta-down" : "delta-flat");
            figures.getChildren().add(d);
        }
        figures.setMinWidth(Region.USE_PREF_SIZE);

        HBox row = new HBox(14, text, figures);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("layer-row", "layer-depth-" + Math.min(depth, 3));
        row.setPadding(new javafx.geometry.Insets(10, 14, 10, 14));
        return row;
    }
}
