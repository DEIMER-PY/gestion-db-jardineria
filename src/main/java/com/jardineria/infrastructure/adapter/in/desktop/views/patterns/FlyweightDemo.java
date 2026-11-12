package com.jardineria.infrastructure.adapter.in.desktop.views.patterns;

import com.jardineria.domain.model.Producto;
import com.jardineria.domain.pattern.flyweight.GamaFlyweight;
import com.jardineria.domain.pattern.flyweight.GamaFlyweightFactory;
import com.jardineria.infrastructure.adapter.in.common.Formatters;
import com.jardineria.infrastructure.adapter.in.desktop.components.KpiCard;
import com.jardineria.infrastructure.adapter.in.desktop.components.ResponsiveGrid;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Flyweight: un único objeto compartido por gama en lugar de uno por producto. */
final class FlyweightDemo implements PatternDemo {

    @Override
    public String name() {
        return "Flyweight";
    }

    @Override
    public Ikon icon() {
        return Feather.FEATHER;
    }

    @Override
    public KpiCard.Tone tone() {
        return KpiCard.Tone.JADE;
    }

    @Override
    public String tagline() {
        return "Comparte el estado común para ahorrar memoria en objetos numerosos.";
    }

    @Override
    public String intent() {
        return "Todos los productos de una gama comparten descripción, color e icono. GamaFlyweightFactory mantiene "
                + "un pool con un único GamaFlyweight por gama (estado intrínseco) y cada producto aporta solo su "
                + "código, stock y precio (estado extrínseco) en el momento de pintarse.";
    }

    @Override
    public List<String> participants() {
        return List.of("GamaFlyweightFactory", "GamaFlyweight");
    }

    @Override
    public String usage() {
        return "El icono y el color de cada gama en Productos y Gamas salen de este pool compartido.";
    }

    @Override
    public String image() {
        return "flyweight.jpg";
    }

    @Override
    public Node build(PatternData data) {
        // Se pide un flyweight por producto: la fábrica devuelve siempre la misma instancia por gama.
        Map<GamaFlyweight, Integer> usos = new IdentityHashMap<>();
        Map<String, GamaFlyweight> porGama = new LinkedHashMap<>();
        for (Producto p : data.productos()) {
            GamaFlyweight fw = GamaFlyweightFactory.getFlyweight(p.getGama());
            usos.merge(fw, 1, Integer::sum);
            porGama.putIfAbsent(fw.nombreGama(), fw);
        }
        int productos = data.productos().size();
        int instancias = usos.size();

        Label lblProductos = new Label(Formatters.integer(productos));
        Label lblInstancias = new Label(Formatters.integer(instancias));
        Label lblAhorro = new Label(productos == 0 ? "—"
                : Formatters.percent(1 - (double) instancias / productos));
        Label lblPool = new Label(Formatters.integer(GamaFlyweightFactory.getFlyweightsCount()));

        ResponsiveGrid cards = new ResponsiveGrid(230, 3, 12);
        porGama.values().stream()
                .sorted((a, b) -> Integer.compare(usos.get(b), usos.get(a)))
                .forEach(fw -> cards.add(card(fw, usos.get(fw), productos), 1));

        ComboBox<Producto> producto = DemoUi.productPicker(data.productos());
        Label extrinseco = new Label();
        extrinseco.getStyleClass().addAll("demo-code", "mono");
        extrinseco.setWrapText(true);
        extrinseco.setMinHeight(Region.USE_PREF_SIZE);
        Label identidad = new Label();
        identidad.getStyleClass().add("demo-metric-label");
        Runnable describe = () -> {
            Producto p = producto.getValue();
            if (p == null) {
                return;
            }
            GamaFlyweight fw = GamaFlyweightFactory.getFlyweight(p.getGama());
            int stock = p.getCantidadEnStock() == null ? 0 : p.getCantidadEnStock();
            double pvp = p.getPrecioVenta() == null ? 0 : p.getPrecioVenta().doubleValue();
            extrinseco.setText(fw.formatearDetalleExtrinseco(p.getCodigoProducto(), stock, Formatters.decimal(pvp, 2)));
            identidad.setText("Instancia compartida " + hash(fw) + " · la misma para los "
                    + usos.getOrDefault(fw, 0) + " productos de " + Formatters.display(fw.nombreGama()));
        };
        producto.valueProperty().addListener((o, a, b) -> describe.run());
        describe.run();

        return new VBox(16,
                DemoUi.metrics(
                        DemoUi.metric(lblProductos, "productos en catálogo"),
                        DemoUi.metric(lblInstancias, "instancias de gama usadas"),
                        DemoUi.metric(lblAhorro, "objetos de gama evitados"),
                        DemoUi.metric(lblPool, "entradas en el pool")),
                cards,
                DemoUi.output(DemoUi.title("Estado intrínseco + extrínseco"),
                        DemoUi.field("Producto", producto), extrinseco, identidad));
    }

    private static Node card(GamaFlyweight fw, int count, int total) {
        Color color = parse(fw.colorHex());
        Circle swatch = new Circle(5, color);
        // FontIcon guarda su fuente en el estilo en línea: se añade el color sin sustituirlo.
        org.kordamp.ikonli.javafx.FontIcon glyph = Icons.of(Icons.forGama(fw.iconKey()), 18);
        glyph.setStyle(glyph.getStyle() + "; -fx-icon-color: " + fw.colorHex() + ";");
        StackPane badge = new StackPane(glyph);
        badge.getStyleClass().add("flyweight-badge");
        badge.setStyle("-fx-background-color: " + toRgba(color, 0.16) + ";");

        Label name = new Label(Formatters.display(fw.nombreGama()));
        name.getStyleClass().add("flyweight-name");
        Label desc = new Label(fw.descripcionGenerica());
        desc.getStyleClass().add("flyweight-desc");
        desc.setWrapText(true);
        desc.setMinHeight(Region.USE_PREF_SIZE);
        Label id = new Label(hash(fw) + "  ·  " + fw.colorHex());
        id.getStyleClass().addAll("flyweight-id", "mono");
        HBox idRow = new HBox(6, swatch, id);
        idRow.setAlignment(Pos.CENTER_LEFT);

        Region fill = new Region();
        fill.getStyleClass().add("flyweight-bar-fill");
        fill.setStyle("-fx-background-color: " + fw.colorHex() + ";");
        StackPane bar = new StackPane(fill);
        bar.getStyleClass().add("flyweight-bar");
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        double fraction = total == 0 ? 0 : (double) count / total;
        fill.maxWidthProperty().bind(bar.widthProperty().multiply(fraction));
        Label uses = new Label("Compartido por " + count + (count == 1 ? " producto" : " productos"));
        uses.getStyleClass().add("flyweight-uses");

        HBox head = new HBox(12, badge, new VBox(2, name, idRow));
        head.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(10, head, desc, bar, uses);
        box.getStyleClass().add("flyweight-card");
        return box;
    }

    private static String hash(Object o) {
        return "@" + Integer.toHexString(System.identityHashCode(o));
    }

    private static Color parse(String hex) {
        try {
            return Color.web(hex);
        } catch (IllegalArgumentException e) {
            return Color.web("#34D399");
        }
    }

    private static String toRgba(Color c, double alpha) {
        return String.format(java.util.Locale.ROOT, "rgba(%d,%d,%d,%.2f)",
                (int) Math.round(c.getRed() * 255), (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255), alpha);
    }
}
