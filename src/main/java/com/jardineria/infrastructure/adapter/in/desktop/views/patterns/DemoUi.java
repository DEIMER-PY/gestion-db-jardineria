package com.jardineria.infrastructure.adapter.in.desktop.views.patterns;

import com.jardineria.domain.model.Cliente;
import com.jardineria.domain.model.Producto;
import com.jardineria.infrastructure.adapter.in.common.Formatters;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Piezas visuales comunes a las demos de patrones. */
final class DemoUi {
    private DemoUi() {
    }

    static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    /** Campo con etiqueta superior. */
    static VBox field(String label, Node control) {
        Label l = new Label(label);
        l.getStyleClass().add("demo-label");
        l.setLabelFor(control);
        VBox box = new VBox(6, l, control);
        box.getStyleClass().add("demo-field");
        return box;
    }

    /** Fila de controles que baja de línea cuando no cabe. */
    static FlowPane controls(Node... nodes) {
        FlowPane pane = new FlowPane(16, 12, nodes);
        pane.setAlignment(Pos.BOTTOM_LEFT);
        pane.getStyleClass().add("demo-controls");
        return pane;
    }

    static Label title(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("demo-section");
        return l;
    }

    static Label note(Ikon icon, String text) {
        Label l = new Label(text, Icons.of(icon, 14));
        l.getStyleClass().add("demo-note");
        l.setWrapText(true);
        l.setMinHeight(Region.USE_PREF_SIZE);
        return l;
    }

    static Label code(String text) {
        Label l = new Label(text);
        l.getStyleClass().addAll("code-chip", "mono");
        return l;
    }

    static Label tag(String text, String tone) {
        Label l = new Label(text);
        l.getStyleClass().addAll("demo-tag", "tone-" + tone);
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    /** Cifra destacada con etiqueta debajo. */
    static VBox metric(Label value, String label) {
        value.getStyleClass().add("demo-metric-value");
        Label l = new Label(label);
        l.getStyleClass().add("demo-metric-label");
        VBox box = new VBox(2, value, l);
        box.getStyleClass().add("demo-metric");
        box.setMinWidth(Region.USE_PREF_SIZE);
        return box;
    }

    static HBox metrics(Node... nodes) {
        HBox box = new HBox(28, nodes);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("demo-metrics");
        return box;
    }

    /** Superficie hundida donde se pinta el resultado de la demo. */
    static VBox output(Node... nodes) {
        VBox box = new VBox(10, nodes);
        box.getStyleClass().add("demo-output");
        return box;
    }

    static Region grow() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    static <T> ComboBox<T> picker(List<T> items, Function<T, String> display, String accessible) {
        ComboBox<T> box = new ComboBox<>();
        box.getItems().setAll(items);
        box.setCellFactory(lv -> cell(display));
        box.setButtonCell(cell(display));
        box.setAccessibleText(accessible);
        box.setVisibleRowCount(12);
        if (!items.isEmpty()) {
            box.getSelectionModel().selectFirst();
        }
        return box;
    }

    private static <T> ListCell<T> cell(Function<T, String> display) {
        return new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : display.apply(item));
            }
        };
    }

    static ComboBox<Producto> productPicker(List<Producto> productos) {
        ComboBox<Producto> box = picker(productos,
                p -> p.getNombre() + "  ·  " + Formatters.currency(p.getPrecioVenta()), "Producto");
        box.setPrefWidth(320);
        return box;
    }

    static ComboBox<Cliente> clientPicker(List<Cliente> clientes) {
        ComboBox<Cliente> box = picker(clientes,
                c -> c.getNombreCliente() + " (" + c.getCodigoCliente() + ")", "Cliente");
        box.setPrefWidth(280);
        return box;
    }

    /** Fila "etiqueta ........ valor" para desgloses. */
    static HBox row(String label, String value, String... valueClasses) {
        Label l = new Label(label);
        l.getStyleClass().add("demo-row-label");
        l.setWrapText(true);
        HBox.setHgrow(l, Priority.ALWAYS);
        l.setMaxWidth(Double.MAX_VALUE);
        Label v = new Label(value);
        v.getStyleClass().addAll("demo-row-value", "mono");
        v.getStyleClass().addAll(valueClasses);
        v.setMinWidth(Region.USE_PREF_SIZE);
        HBox row = new HBox(12, l, v);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("demo-row");
        return row;
    }
}
