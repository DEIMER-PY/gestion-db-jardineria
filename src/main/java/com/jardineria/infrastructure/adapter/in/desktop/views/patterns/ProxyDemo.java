package com.jardineria.infrastructure.adapter.in.desktop.views.patterns;

import com.jardineria.domain.model.Producto;
import com.jardineria.domain.pattern.proxy.CachedProductLookupProxy;
import com.jardineria.infrastructure.adapter.in.common.Formatters;
import com.jardineria.infrastructure.adapter.in.desktop.components.EmptyState;
import com.jardineria.infrastructure.adapter.in.desktop.components.KpiCard;
import com.jardineria.infrastructure.adapter.in.desktop.support.FxTasks;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import com.jardineria.infrastructure.adapter.in.desktop.support.Notifier;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/** Proxy: caché en memoria delante del repositorio JDBC de productos. */
final class ProxyDemo implements PatternDemo {
    private static final int LOG_SIZE = 8;

    private record Lookup(String code, boolean hit, double millis, Optional<Producto> result) {
    }

    @Override
    public String name() {
        return "Proxy";
    }

    @Override
    public Ikon icon() {
        return Feather.SHIELD;
    }

    @Override
    public KpiCard.Tone tone() {
        return KpiCard.Tone.CORAL;
    }

    @Override
    public String tagline() {
        return "Un sustituto que controla el acceso al objeto real.";
    }

    @Override
    public String intent() {
        return "CachedProductLookupProxy implementa el mismo puerto que consultaría la base de datos, pero antes "
                + "mira su caché en RAM. La primera consulta de un código viaja a MySQL (fallo de caché); las "
                + "siguientes se sirven desde memoria (acierto) y el proxy lleva la cuenta de ambos.";
    }

    @Override
    public List<String> participants() {
        return List.of("ProductLookupPort", "CachedProductLookupProxy", "ProductoRepository");
    }

    @Override
    public String usage() {
        return "Búsquedas repetidas de productos por código sin repetir la consulta SQL.";
    }

    @Override
    public String image() {
        return "proxy.jpg";
    }

    @Override
    public Node build(PatternData data) {
        CachedProductLookupProxy proxy = data.productLookup();
        if (proxy == null) {
            return new EmptyState(Feather.SHIELD_OFF, "Proxy no disponible",
                    "El contexto de la aplicación no ha registrado el proxy de productos.");
        }
        List<String> codes = data.productos().stream().map(Producto::getCodigoProducto).toList();
        ComboBox<String> code = new ComboBox<>();
        code.getItems().setAll(codes);
        code.setEditable(true);
        code.setPrefWidth(180);
        code.setAccessibleText("Código de producto");
        code.setPromptText("Código, p. ej. FR-67");
        if (!codes.isEmpty()) {
            code.setValue(codes.get(0));
        }

        Label hits = new Label();
        Label misses = new Label();
        Label ratio = new Label();
        Label cached = new Label();
        VBox log = new VBox(6);
        log.getStyleClass().add("proxy-log");
        Label emptyLog = DemoUi.note(Feather.INFO, "Consulta un código: la primera vez irá a MySQL y la segunda saldrá de la caché.");
        log.getChildren().add(emptyLog);
        List<Lookup> history = new ArrayList<>();

        Runnable updateStats = () -> {
            hits.setText(Formatters.integer(proxy.getCacheHits()));
            misses.setText(Formatters.integer(proxy.getCacheMisses()));
            ratio.setText(Formatters.decimal(proxy.getHitRatio(), 1) + " %");
            cached.setText(Formatters.integer(proxy.getCachedEntriesCount()));
        };
        Runnable renderLog = () -> {
            log.getChildren().setAll(history.stream().map(ProxyDemo::logRow).toList());
            if (history.isEmpty()) {
                log.getChildren().add(emptyLog);
            }
        };

        Button lookup = new Button("Consultar", Icons.of(Feather.SEARCH, 14));
        lookup.getStyleClass().add("accent");
        Button burst = new Button("Ráfaga de 20 consultas", Icons.of(Feather.ZAP, 14));
        Button clear = new Button("Vaciar caché", Icons.of(Feather.TRASH, 14));
        List<Button> buttons = List.of(lookup, burst, clear);

        lookup.setOnAction(e -> {
            String c = code.getEditor().getText();
            if (c == null || c.isBlank()) {
                return;
            }
            run(proxy, List.of(c.trim()), buttons, history, () -> {
                updateStats.run();
                renderLog.run();
            });
        });
        burst.setOnAction(e -> {
            if (codes.isEmpty()) {
                return;
            }
            // Pocos códigos distintos repetidos: la tasa de aciertos sube con cada consulta.
            List<String> pool = codes.subList(0, Math.min(5, codes.size()));
            Random rnd = new Random();
            List<String> batch = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                batch.add(pool.get(rnd.nextInt(pool.size())));
            }
            run(proxy, batch, buttons, history, () -> {
                updateStats.run();
                renderLog.run();
            });
        });
        clear.setOnAction(e -> {
            proxy.clearCache();
            updateStats.run();
            Notifier.info("Caché vaciada", "La próxima consulta de cada código irá a MySQL.");
        });
        updateStats.run();

        return new VBox(16,
                DemoUi.controls(DemoUi.field("Código de producto", code), lookup, burst, clear),
                DemoUi.metrics(
                        DemoUi.metric(hits, "aciertos (RAM)"),
                        DemoUi.metric(misses, "fallos (MySQL)"),
                        DemoUi.metric(ratio, "tasa de acierto"),
                        DemoUi.metric(cached, "productos en caché")),
                DemoUi.output(DemoUi.title("Últimas consultas"), log));
    }

    private static void run(CachedProductLookupProxy proxy, List<String> batch, List<Button> buttons,
                            List<Lookup> history, Runnable done) {
        buttons.forEach(b -> b.setDisable(true));
        FxTasks.run(() -> {
            List<Lookup> results = new ArrayList<>();
            for (String c : batch) {
                long missesBefore = proxy.getCacheMisses();
                long t0 = System.nanoTime();
                Optional<Producto> p = proxy.buscarPorCodigo(c);
                double ms = (System.nanoTime() - t0) / 1_000_000.0;
                results.add(new Lookup(c.toUpperCase(), proxy.getCacheMisses() == missesBefore, ms, p));
            }
            return results;
        }, results -> {
            buttons.forEach(b -> b.setDisable(false));
            Collections.reverse(results);
            history.addAll(0, results);
            while (history.size() > LOG_SIZE) {
                history.remove(history.size() - 1);
            }
            done.run();
        }, err -> {
            buttons.forEach(b -> b.setDisable(false));
            Notifier.error("No se pudo consultar el producto", FxTasks.friendlyMessage(FxTasks.unwrap(err)));
        });
    }

    private static Node logRow(Lookup l) {
        Label tag = DemoUi.tag(l.hit() ? "CACHÉ" : "MySQL", l.hit() ? "jade" : "amber");
        tag.setMinWidth(64);
        tag.setAlignment(Pos.CENTER);
        Label code = new Label(l.code());
        code.getStyleClass().addAll("proxy-code", "mono");
        code.setMinWidth(Region.USE_PREF_SIZE);
        Label name = new Label(l.result().map(Producto::getNombre).orElse("No existe en el catálogo"));
        name.getStyleClass().add(l.result().isPresent() ? "proxy-name" : "proxy-missing");
        name.setMinWidth(0);
        HBox.setHgrow(name, Priority.ALWAYS);
        name.setMaxWidth(Double.MAX_VALUE);
        Label time = new Label(Formatters.decimal(l.millis(), l.millis() < 1 ? 3 : 1) + " ms");
        time.getStyleClass().addAll("proxy-time", "mono");
        time.setMinWidth(Region.USE_PREF_SIZE);
        HBox row = new HBox(12, tag, code, name, time);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("proxy-row");
        return row;
    }
}
