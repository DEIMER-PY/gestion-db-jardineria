package com.jardineria.infrastructure.adapter.in.desktop.views.patterns;

import com.jardineria.domain.model.Cliente;
import com.jardineria.domain.model.Producto;
import com.jardineria.domain.pattern.bridge.CsvOutputImplementor;
import com.jardineria.domain.pattern.bridge.ExecutiveReport;
import com.jardineria.domain.pattern.bridge.HtmlOutputImplementor;
import com.jardineria.domain.pattern.bridge.JsonOutputImplementor;
import com.jardineria.domain.pattern.bridge.ReportBridge;
import com.jardineria.domain.pattern.bridge.ReportOutputImplementor;
import com.jardineria.infrastructure.adapter.in.common.Formatters;
import com.jardineria.infrastructure.adapter.in.desktop.components.KpiCard;
import com.jardineria.infrastructure.adapter.in.desktop.support.FxTasks;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import com.jardineria.infrastructure.adapter.in.desktop.support.Notifier;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Bridge: el informe (abstracción) y su formato de salida (implementación) varían por separado. */
final class BridgeDemo implements PatternDemo {

    private record Dataset(String label, String titulo, String subtitulo, List<String> columnas,
                           Function<PatternData, List<List<String>>> rows) {
    }

    private record Format(String label, Supplier<ReportOutputImplementor> factory) {
    }

    private static final List<Dataset> DATASETS = List.of(
            new Dataset("Stock crítico", "Productos con stock crítico", "Artículos con 20 unidades o menos",
                    List.of("Código", "Producto", "Gama", "Stock", "Precio"),
                    d -> d.productos().stream()
                            .filter(p -> p.getCantidadEnStock() != null && p.getCantidadEnStock() <= 20)
                            .sorted(Comparator.comparing(Producto::getCantidadEnStock))
                            .map(p -> List.of(p.getCodigoProducto(), p.getNombre(), Formatters.display(p.getGama()),
                                    String.valueOf(p.getCantidadEnStock()), Formatters.decimal(p.getPrecioVenta().doubleValue(), 2)))
                            .toList()),
            new Dataset("Clientes por país", "Cartera de clientes por país", "Número de clientes y crédito concedido",
                    List.of("País", "Clientes", "Crédito total"),
                    d -> {
                        Map<String, List<Cliente>> byCountry = d.clientes().stream()
                                .collect(Collectors.groupingBy(c -> Formatters.orDash(c.getPais()), TreeMap::new, Collectors.toList()));
                        return byCountry.entrySet().stream()
                                .map(e -> List.of(e.getKey(), String.valueOf(e.getValue().size()),
                                        Formatters.decimal(e.getValue().stream()
                                                .filter(c -> c.getLimiteCredito() != null)
                                                .mapToDouble(c -> c.getLimiteCredito().doubleValue()).sum(), 2)))
                                .toList();
                    }));

    private static final List<Format> FORMATS = List.of(
            new Format("CSV", CsvOutputImplementor::new),
            new Format("HTML", HtmlOutputImplementor::new),
            new Format("JSON", JsonOutputImplementor::new));

    @Override
    public String name() {
        return "Bridge";
    }

    @Override
    public Ikon icon() {
        return Feather.COLUMNS;
    }

    @Override
    public KpiCard.Tone tone() {
        return KpiCard.Tone.LILAC;
    }

    @Override
    public String tagline() {
        return "Separa una abstracción de su implementación para que evolucionen por separado.";
    }

    @Override
    public String intent() {
        return "ExecutiveReport sabe qué contar (título, columnas, filas) pero no cómo escribirlo. Delega en un "
                + "ReportOutputImplementor (CSV, HTML o JSON). Añadir un informe nuevo no obliga a tocar los formatos "
                + "y añadir un formato no obliga a tocar los informes: dos jerarquías en lugar de una explosión N × M.";
    }

    @Override
    public List<String> participants() {
        return List.of("ReportBridge", "ExecutiveReport", "ReportOutputImplementor",
                "CsvOutputImplementor", "HtmlOutputImplementor", "JsonOutputImplementor");
    }

    @Override
    public String usage() {
        return "Exportación de informes ejecutivos: el mismo informe se descarga en el formato que pida cada destinatario.";
    }

    @Override
    public String image() {
        return "bridge.jpg";
    }

    @Override
    public Node build(PatternData data) {
        ToggleGroup datasetGroup = new ToggleGroup();
        HBox datasetSeg = segmented(datasetGroup, DATASETS.stream().map(Dataset::label).toList());
        ToggleGroup formatGroup = new ToggleGroup();
        HBox formatSeg = segmented(formatGroup, FORMATS.stream().map(Format::label).toList());

        TextArea output = new TextArea();
        output.setEditable(false);
        output.setWrapText(false);
        output.setPrefRowCount(14);
        output.getStyleClass().addAll("bridge-output", "mono");
        output.setAccessibleText("Informe generado");
        Label stats = new Label();
        stats.getStyleClass().add("demo-metric-label");
        Label pairing = new Label();
        pairing.getStyleClass().addAll("demo-code", "mono");
        pairing.setWrapText(true);

        String[] lastExtension = {"csv"};
        String[] lastContent = {""};
        Runnable generate = () -> {
            Dataset ds = DATASETS.get(index(datasetGroup));
            Format fmt = FORMATS.get(index(formatGroup));
            ReportOutputImplementor impl = fmt.factory().get();
            ReportBridge report = new ExecutiveReport(impl, ds.titulo(), ds.subtitulo(), ds.columnas());
            List<List<String>> rows = ds.rows().apply(data);
            String content = report.generarReporte(rows);
            lastExtension[0] = report.getExtension().replace(".", "");
            lastContent[0] = content;
            output.setText(content);
            output.positionCaret(0);
            pairing.setText("new ExecutiveReport(new " + impl.getClass().getSimpleName() + "(), \"" + ds.titulo() + "\", …)");
            stats.setText(rows.size() + " filas · " + Formatters.integer(content.length()) + " caracteres · ." + lastExtension[0]);
        };
        datasetGroup.selectedToggleProperty().addListener((o, a, b) -> generate.run());
        formatGroup.selectedToggleProperty().addListener((o, a, b) -> generate.run());
        generate.run();

        Button save = new Button("Guardar archivo", Icons.of(Feather.DOWNLOAD, 14));
        save.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Guardar informe");
            chooser.setInitialFileName("informe-jardineria." + lastExtension[0]);
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    lastExtension[0].toUpperCase(), "*." + lastExtension[0]));
            File file = chooser.showSaveDialog(save.getScene().getWindow());
            if (file == null) {
                return;
            }
            String content = lastContent[0];
            FxTasks.runVoid(() -> Files.writeString(file.toPath(), content, StandardCharsets.UTF_8),
                    () -> Notifier.success("Informe guardado", file.getName()),
                    err -> Notifier.error("No se pudo guardar el informe", FxTasks.friendlyMessage(FxTasks.unwrap(err))));
        });

        return new VBox(16,
                DemoUi.controls(DemoUi.field("Abstracción: informe", datasetSeg),
                        DemoUi.field("Implementación: formato", formatSeg), save),
                DemoUi.output(pairing, output, stats));
    }

    private static HBox segmented(ToggleGroup group, List<String> labels) {
        HBox box = new HBox();
        box.getStyleClass().add("segmented");
        for (int i = 0; i < labels.size(); i++) {
            ToggleButton b = new ToggleButton(labels.get(i));
            b.setToggleGroup(group);
            b.setUserData(i);
            b.setSelected(i == 0);
            box.getChildren().add(b);
        }
        group.selectedToggleProperty().addListener((o, old, sel) -> {
            if (sel == null && old != null) {
                old.setSelected(true);
            }
        });
        return box;
    }

    private static int index(ToggleGroup group) {
        return group.getSelectedToggle() == null ? 0 : (int) group.getSelectedToggle().getUserData();
    }
}
