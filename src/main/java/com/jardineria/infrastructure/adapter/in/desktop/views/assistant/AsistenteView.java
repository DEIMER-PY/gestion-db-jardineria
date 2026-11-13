package com.jardineria.infrastructure.adapter.in.desktop.views.assistant;

import com.jardineria.application.port.in.AsistenteUseCase;
import com.jardineria.application.port.in.AsistenteUseCase.Respuesta;
import com.jardineria.application.port.in.AsistenteUseCase.Turno;
import com.jardineria.infrastructure.adapter.in.desktop.components.EmptyState;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.support.FxTasks;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import com.jardineria.infrastructure.adapter.in.desktop.support.Motion;
import com.jardineria.infrastructure.adapter.in.desktop.views.BaseView;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.kordamp.ikonli.feather.Feather;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conversación con el asistente: preguntas sobre los datos (consulta los módulos en modo lectura)
 * y sobre el uso de la aplicación (responde con la documentación y cita las fuentes).
 * Los pedidos que menciona ({@code #14}) son enlaces que abren el módulo Pedidos.
 * <p>
 * Intro envía; Mayús+Intro añade una línea. La consulta va en un hilo virtual.
 */
public class AsistenteView extends BaseView {
    static final List<String> SUGERENCIAS = List.of(
            "¿Cuántos pedidos pendientes hay?",
            "¿Qué productos tienen poco stock?",
            "¿Quiénes son los cinco mejores clientes?",
            "¿Cómo cambio el tema de la aplicación?");
    private static final Pattern PEDIDO = Pattern.compile("#(\\d{1,6})\\b");

    private final AsistenteUseCase asistente;
    private final BiConsumer<Route, String> navigate;
    private final List<Turno> historial = new ArrayList<>();
    private final VBox messages = new VBox(14);
    private final ScrollPane scroll = new ScrollPane(messages);
    private final TextArea input = new TextArea();
    private final Button send = new Button(null, Icons.of(Feather.SEND, 16));
    private final Button reset = new Button("Nueva conversación", Icons.of(Feather.PLUS, 15));
    private Node thinking;
    private int generation;
    private boolean busy;

    public AsistenteView(AsistenteUseCase asistente, BiConsumer<Route, String> navigate) {
        super(Route.ASISTENTE, false);
        this.asistente = asistente;
        this.navigate = navigate;

        Label provider = new Label(asistente.disponible() ? asistente.descripcionProveedor() : "Sin configurar",
                Icons.of(asistente.disponible() ? Feather.CPU : Feather.KEY, 13));
        provider.getStyleClass().add("chat-provider");
        provider.setMinWidth(Region.USE_PREF_SIZE);
        reset.setOnAction(e -> nuevaConversacion());
        reset.setDisable(!asistente.disponible());
        addActions(provider, reset);
    }

    @Override
    protected Node buildContent() {
        if (!asistente.disponible()) {
            EmptyState state = new EmptyState(Feather.KEY, "Asistente sin configurar",
                    "Crea una clave gratuita en console.groq.com, guárdala en la variable de entorno GROQ_API_KEY "
                            + "y vuelve a abrir la aplicación.\n"
                            + "Windows (PowerShell):  setx GROQ_API_KEY \"tu-clave\"\n"
                            + "macOS o Linux:  export GROQ_API_KEY=\"tu-clave\"\n"
                            + "Opcional: GROQ_MODEL elige el modelo (por defecto openai/gpt-oss-120b).");
            state.getStyleClass().add("chat-setup");
            StackPane wrap = new StackPane(state);
            wrap.getStyleClass().add("chat-page");
            return wrap;
        }

        messages.getStyleClass().add("chat-messages");
        messages.setAccessibleRole(AccessibleRole.LIST_VIEW);
        messages.setAccessibleText("Conversación con el asistente");
        scroll.setFitToWidth(true);
        scroll.getStyleClass().addAll("chat-scroll", "edge-to-edge");
        messages.heightProperty().addListener((o, a, b) -> scroll.setVvalue(1));
        VBox.setVgrow(scroll, Priority.ALWAYS);

        input.getStyleClass().add("chat-input");
        input.setPromptText("Pregunta por clientes, pedidos, stock o por cómo usar la aplicación…");
        input.setAccessibleText("Pregunta para el asistente");
        input.setWrapText(true);
        input.setPrefRowCount(2);
        input.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER && !e.isShiftDown()) {
                e.consume();
                enviar(input.getText());
            } else if (e.getCode() == KeyCode.ENTER) {
                e.consume();
                input.insertText(input.getCaretPosition(), "\n");
            }
        });
        HBox.setHgrow(input, Priority.ALWAYS);
        send.getStyleClass().addAll("accent", "chat-send");
        send.setAccessibleText("Enviar pregunta");
        send.setTooltip(new Tooltip("Enviar (Intro). Mayús+Intro añade una línea."));
        send.setOnAction(e -> enviar(input.getText()));
        send.disableProperty().bind(input.textProperty().isEmpty());

        HBox composer = new HBox(10, input, send);
        composer.setAlignment(Pos.BOTTOM_LEFT);
        composer.getStyleClass().add("chat-composer");
        Label hint = new Label("Solo consulta: el asistente no modifica datos. Puede equivocarse; comprueba las cifras importantes.");
        hint.getStyleClass().add("chat-hint");
        hint.setWrapText(true);

        VBox page = new VBox(12, scroll, composer, hint);
        page.getStyleClass().add("chat-page");
        bienvenida();
        return page;
    }

    @Override
    public void onShow() {
        if (asistente.disponible()) {
            Platform.runLater(input::requestFocus);
        }
    }

    /** No hay datos que recargar: la conversación se conserva al volver a la vista. */
    @Override
    public void refresh() {
    }

    @Override
    public void createNew() {
        nuevaConversacion();
    }

    void nuevaConversacion() {
        if (!asistente.disponible()) {
            return;
        }
        generation++;
        busy = false;
        historial.clear();
        thinking = null;
        messages.getChildren().clear();
        input.clear();
        input.setDisable(false);
        bienvenida();
        input.requestFocus();
    }

    private void bienvenida() {
        Label title = new Label("¿En qué te ayudo?");
        title.getStyleClass().add("chat-welcome-title");
        Label text = new Label("Consulto los datos de la jardinería en modo lectura y la documentación de la aplicación. "
                + "Prueba con una de estas preguntas:");
        text.getStyleClass().add("chat-welcome-text");
        text.setWrapText(true);
        FlowPane chips = new FlowPane(8, 8);
        chips.getStyleClass().add("chat-suggestions");
        for (String s : SUGERENCIAS) {
            Button chip = new Button(s);
            chip.getStyleClass().add("chat-chip");
            chip.setOnAction(e -> enviar(s));
            chips.getChildren().add(chip);
        }
        VBox welcome = new VBox(8, title, text, chips);
        welcome.getStyleClass().add("chat-welcome");
        messages.getChildren().add(welcome);
    }

    void enviar(String texto) {
        String pregunta = texto == null ? "" : texto.strip();
        if (pregunta.isEmpty() || busy) {
            return;
        }
        busy = true;
        input.clear();
        input.setDisable(true);
        messages.getChildren().removeIf(n -> n.getStyleClass().contains("chat-welcome"));
        messages.getChildren().add(burbuja(true, pregunta, List.of(), List.of(), false));
        thinking = pensando();
        messages.getChildren().add(thinking);

        List<Turno> previos = List.copyOf(historial);
        int gen = generation;
        FxTasks.run(() -> asistente.preguntar(previos, pregunta), (Respuesta r) -> {
            if (gen != generation) {
                return;
            }
            terminar();
            historial.add(new Turno(true, pregunta));
            historial.add(new Turno(false, r.texto()));
            messages.getChildren().add(burbuja(false, r.texto(), r.fuentes(), r.herramientas(), false));
        }, error -> {
            if (gen != generation) {
                return;
            }
            terminar();
            messages.getChildren().add(burbuja(false, FxTasks.friendlyMessage(error), List.of(), List.of(), true));
        });
    }

    private void terminar() {
        busy = false;
        messages.getChildren().remove(thinking);
        thinking = null;
        input.setDisable(false);
        input.requestFocus();
    }

    private Node pensando() {
        ProgressIndicator ring = new ProgressIndicator(-1);
        ring.getStyleClass().add("chat-thinking-ring");
        ring.setPrefSize(16, 16);
        Label label = new Label("Consultando…");
        label.getStyleClass().add("chat-thinking");
        HBox bubble = new HBox(8, ring, label);
        bubble.setAlignment(Pos.CENTER_LEFT);
        bubble.getStyleClass().addAll("chat-bubble", "chat-bubble-assistant");
        HBox row = new HBox(10, marca(), bubble);
        row.getStyleClass().add("chat-row");
        row.setAccessibleRole(AccessibleRole.TEXT);
        row.setAccessibleText("El asistente está consultando");
        return row;
    }

    private Node burbuja(boolean delUsuario, String texto, List<String> fuentes, List<String> herramientas, boolean error) {
        TextFlow flow = enlazar(texto);
        flow.getStyleClass().add("chat-text");
        VBox bubble = new VBox(6, flow);
        bubble.getStyleClass().addAll("chat-bubble", delUsuario ? "chat-bubble-user" : "chat-bubble-assistant");
        if (error) {
            bubble.getStyleClass().add("chat-bubble-error");
        }
        if (!fuentes.isEmpty()) {
            Label l = new Label("Fuentes: " + String.join(" · ", fuentes), Icons.of(Feather.BOOK_OPEN, 12));
            l.getStyleClass().add("chat-meta");
            l.setWrapText(true);
            bubble.getChildren().add(l);
        }
        List<String> datos = herramientas.stream().filter(h -> !h.equals("buscar_documentacion")).distinct().toList();
        if (!datos.isEmpty()) {
            Label l = new Label("Consultó: " + String.join(", ", datos.stream().map(h -> h.replace('_', ' ')).toList()),
                    Icons.of(Feather.DATABASE, 12));
            l.getStyleClass().add("chat-meta");
            l.setWrapText(true);
            bubble.getChildren().add(l);
        }
        // La del usuario se ajusta a su texto; la del asistente puede crecer hasta un ancho cómodo de lectura
        bubble.setMaxWidth(delUsuario ? 560 : 720);
        if (!delUsuario) {
            HBox.setHgrow(bubble, Priority.SOMETIMES);
        }

        HBox row = new HBox(10);
        row.getStyleClass().addAll("chat-row", delUsuario ? "chat-row-user" : "chat-row-assistant");
        if (delUsuario) {
            row.setAlignment(Pos.TOP_RIGHT);
            row.getChildren().add(bubble);
        } else {
            row.setAlignment(Pos.TOP_LEFT);
            row.getChildren().addAll(marca(), bubble);
        }
        row.setAccessibleRole(AccessibleRole.TEXT);
        row.setAccessibleText((delUsuario ? "Tú: " : error ? "Error del asistente: " : "Asistente: ") + texto);
        Motion.enter(row);
        return row;
    }

    private static Node marca() {
        StackPane badge = new StackPane(Icons.of(Feather.MESSAGE_CIRCLE, 15));
        badge.getStyleClass().add("chat-mark");
        badge.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return badge;
    }

    /** Texto con los «#N» convertidos en enlaces al pedido. */
    TextFlow enlazar(String texto) {
        TextFlow flow = new TextFlow();
        Matcher m = PEDIDO.matcher(texto == null ? "" : texto);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                flow.getChildren().add(text(texto.substring(last, m.start())));
            }
            String codigo = m.group(1);
            Hyperlink link = new Hyperlink("#" + codigo);
            link.getStyleClass().add("chat-link");
            link.setAccessibleText("Abrir el pedido " + codigo);
            link.setOnAction(e -> navigate.accept(Route.PEDIDOS, "#" + codigo));
            flow.getChildren().add(link);
            last = m.end();
        }
        if (last < (texto == null ? 0 : texto.length())) {
            flow.getChildren().add(text(texto.substring(last)));
        }
        return flow;
    }

    private static Text text(String s) {
        Text t = new Text(s);
        t.getStyleClass().add("chat-text-run");
        return t;
    }
}
