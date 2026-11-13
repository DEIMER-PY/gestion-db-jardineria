package com.jardineria.infrastructure.adapter.in.desktop.views.patterns;

import com.jardineria.application.facade.JardineriaBusinessFacade;
import com.jardineria.application.port.in.ClienteUseCase;
import com.jardineria.application.port.in.ProductoUseCase;
import com.jardineria.domain.pattern.proxy.CachedProductLookupProxy;
import com.jardineria.infrastructure.adapter.in.desktop.components.Panel;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import com.jardineria.infrastructure.adapter.in.desktop.support.ModalHost;
import com.jardineria.infrastructure.adapter.in.desktop.support.Motion;
import com.jardineria.infrastructure.adapter.in.desktop.views.BaseView;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.feather.Feather;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Laboratorio de patrones GoF: selector de patrón, ficha conceptual con las clases
 * reales que participan y una demo en vivo que ejecuta esas clases con datos de MySQL.
 */
public class PatternsView extends BaseView {
    private static final double THUMB_WIDTH = 280;

    private final ProductoUseCase productos;
    private final ClienteUseCase clientes;
    private final JardineriaBusinessFacade facade;
    private final CachedProductLookupProxy productLookup;

    private final List<PatternDemo> demos = List.of(
            new DecoratorDemo(), new AdapterDemo(), new CompositeDemo(), new BridgeDemo(),
            new FlyweightDemo(), new ProxyDemo(), new FacadeDemo());
    private final Map<PatternDemo, Node> built = new HashMap<>();
    private final ToggleGroup railGroup = new ToggleGroup();
    private final StackPane heroHost = new StackPane();
    private final StackPane demoHost = new StackPane();
    private final Panel demoPanel = new Panel("Demo en vivo", "", demoHost);

    private PatternData data;
    private PatternDemo current;

    public PatternsView(ProductoUseCase productos, ClienteUseCase clientes,
                        JardineriaBusinessFacade facade, CachedProductLookupProxy productLookup) {
        super(Route.PATRONES);
        this.productos = productos;
        this.clientes = clientes;
        this.facade = facade;
        this.productLookup = productLookup;
        this.current = demos.get(0);
    }

    @Override
    protected Node buildContent() {
        FlowPane rail = new FlowPane(10, 10);
        rail.getStyleClass().add("pattern-rail");
        rail.setAccessibleText("Patrones disponibles");
        for (int i = 0; i < demos.size(); i++) {
            PatternDemo demo = demos.get(i);
            StackPane badge = new StackPane(Icons.of(demo.icon(), 15));
            badge.getStyleClass().add("pattern-chip-badge");
            ToggleButton chip = new ToggleButton(demo.name(), badge);
            chip.getStyleClass().addAll("pattern-chip", "tone-" + demo.tone().name().toLowerCase());
            chip.setToggleGroup(railGroup);
            chip.setUserData(demo);
            chip.setMinWidth(Region.USE_PREF_SIZE);
            chip.setTooltip(new Tooltip(demo.tagline()));
            chip.setAccessibleText("Patrón " + demo.name() + ": " + demo.tagline());
            if (i == 0) {
                chip.setSelected(true);
            }
            rail.getChildren().add(chip);
        }
        railGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            if (sel == null) {
                old.setSelected(true); // siempre hay un patrón activo
                return;
            }
            show((PatternDemo) sel.getUserData());
        });

        demoPanel.getStyleClass().add("pattern-demo-panel");
        VBox page = new VBox(18, rail, heroHost, demoPanel);
        page.getStyleClass().add("patterns-page");
        renderHero(current);
        return page;
    }

    @Override
    public void onShow() {
        if (data == null) {
            refresh();
        }
    }

    @Override
    public void refresh() {
        load(() -> new PatternData(productos.listarTodos(), clientes.listarTodos(), facade, productLookup), d -> {
            data = d;
            built.clear();
            show(current);
        });
    }

    private void show(PatternDemo demo) {
        current = demo;
        renderHero(demo);
        demoPanel.setCaption(demo.name() + " ejecutado sobre las clases reales del dominio y datos de MySQL");
        if (data == null) {
            return;
        }
        Node node = built.computeIfAbsent(demo, d -> d.build(data));
        demoHost.getChildren().setAll(node);
        Motion.enter(node);
    }

    private void renderHero(PatternDemo demo) {
        StackPane badge = new StackPane(Icons.of(demo.icon(), 26));
        badge.getStyleClass().add("pattern-hero-badge");
        Label eyebrow = new Label("PATRÓN ESTRUCTURAL GoF");
        eyebrow.getStyleClass().add("pattern-eyebrow");
        Label name = new Label(demo.name());
        name.getStyleClass().add("pattern-name");
        Label tagline = new Label(demo.tagline());
        tagline.getStyleClass().add("pattern-tagline");
        tagline.setWrapText(true);
        VBox titles = new VBox(2, eyebrow, name, tagline);
        titles.setMinWidth(0);
        HBox head = new HBox(16, badge, titles);
        head.setAlignment(Pos.CENTER_LEFT);

        Label intent = new Label(demo.intent());
        intent.getStyleClass().add("pattern-intent");
        intent.setWrapText(true);
        intent.setMinHeight(Region.USE_PREF_SIZE);

        FlowPane chips = new FlowPane(8, 8);
        demo.participants().forEach(p -> chips.getChildren().add(DemoUi.code(p)));
        Label partsLabel = DemoUi.title("Clases que participan");

        Label usage = DemoUi.note(Feather.MAP_PIN, demo.usage());

        VBox text = new VBox(14, head, intent, partsLabel, chips, usage);
        text.setMinWidth(0);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox hero = new HBox(28, text);
        hero.getStyleClass().addAll("pattern-hero", "tone-" + demo.tone().name().toLowerCase());
        Node thumb = thumbnail(demo);
        if (thumb != null) {
            hero.getChildren().add(thumb);
            // En anchos reducidos la ilustración se oculta para no estrechar el texto.
            hero.widthProperty().addListener((obs, o, w) -> {
                boolean fits = w.doubleValue() > 820;
                thumb.setVisible(fits);
                thumb.setManaged(fits);
            });
        }
        heroHost.getChildren().setAll(hero);
    }

    private Node thumbnail(PatternDemo demo) {
        URL url = PatternsView.class.getResource("/images/patterns/" + demo.image());
        if (url == null) {
            return null;
        }
        ImageView view = new ImageView(new Image(url.toExternalForm(), THUMB_WIDTH * 2, 0, true, true, true));
        view.setFitWidth(THUMB_WIDTH);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        Rectangle clip = new Rectangle();
        clip.setArcWidth(22);
        clip.setArcHeight(22);
        clip.widthProperty().bind(view.layoutBoundsProperty().map(b -> b.getWidth()));
        clip.heightProperty().bind(view.layoutBoundsProperty().map(b -> b.getHeight()));
        view.setClip(clip);

        Label hint = new Label("Ampliar ilustración", Icons.of(Feather.MAXIMIZE_2, 12));
        hint.getStyleClass().add("pattern-thumb-hint");
        StackPane.setAlignment(hint, Pos.BOTTOM_RIGHT);

        Button frame = new Button();
        frame.setGraphic(new StackPane(view, hint));
        frame.getStyleClass().add("pattern-thumb");
        frame.setAccessibleText("Ampliar la ilustración del patrón " + demo.name());
        frame.setTooltip(new Tooltip("Ver la ilustración a tamaño completo"));
        frame.setOnAction(e -> zoom(demo, url));
        frame.setMinWidth(Region.USE_PREF_SIZE);
        VBox box = new VBox(frame);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private void zoom(PatternDemo demo, URL url) {
        double width = getScene() == null ? 900 : Math.min(1100, getScene().getWidth() - 160);
        ImageView big = new ImageView(new Image(url.toExternalForm(), width * 1.5, 0, true, true, true));
        big.setFitWidth(width);
        big.setPreserveRatio(true);
        Label title = new Label("Patrón " + demo.name());
        title.getStyleClass().add("dialog-title");
        Button close = new Button(null, Icons.of(Feather.X, 16));
        close.getStyleClass().addAll("icon-button", "flat");
        close.setAccessibleText("Cerrar ilustración");
        close.setCancelButton(true);
        close.setOnAction(e -> ModalHost.hide());
        HBox header = new HBox(12, title, DemoUi.grow(), close);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(14, header, big);
        box.getStyleClass().addAll("dialog", "pattern-zoom");
        box.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        ModalHost.show(box);
        close.requestFocus();
    }
}
