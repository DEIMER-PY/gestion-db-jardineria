package com.jardineria.infrastructure.adapter.in.desktop.views.chat;

import com.jardineria.application.port.in.EmpleadoUseCase;
import com.jardineria.application.port.in.MensajeriaUseCase;
import com.jardineria.application.port.in.MensajeriaUseCase.Sondeo;
import com.jardineria.domain.model.Empleado;
import com.jardineria.domain.model.chat.Canal;
import com.jardineria.domain.model.chat.Mensaje;
import com.jardineria.infrastructure.adapter.in.common.Formatters;
import com.jardineria.infrastructure.adapter.in.desktop.components.Avatar;
import com.jardineria.infrastructure.adapter.in.desktop.components.EmptyState;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.support.FxTasks;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import com.jardineria.infrastructure.adapter.in.desktop.support.Interactions;
import com.jardineria.infrastructure.adapter.in.desktop.support.Notifier;
import com.jardineria.infrastructure.adapter.in.desktop.views.BaseView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.kordamp.ikonli.feather.Feather;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Mensajería del equipo: canal general, uno por oficina y mensajes directos.
 * <p>
 * La identidad se elige de la lista de empleados (sin contraseña) y se recuerda. Cada 3 segundos,
 * mientras la vista está a la vista, se consultan los mensajes nuevos en un hilo virtual; las
 * menciones ({@code @nombre.apellido}) en otros canales avisan con una notificación. Los pedidos
 * ({@code #14}) son enlaces a su ficha.
 */
public class ChatView extends BaseView {
    static final Duration POLL = Duration.seconds(3);
    private static final int HISTORIAL = 100;
    private static final Pattern TOKEN = Pattern.compile("(#\\d{1,6}\\b)|((?<![\\p{L}\\p{N}._-])@[\\p{L}\\p{N}][\\p{L}\\p{N}._-]*[\\p{L}\\p{N}])");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", Formatters.ES);
    private static final DateTimeFormatter DIA_HORA = DateTimeFormatter.ofPattern("d MMM · HH:mm", Formatters.ES);

    private final MensajeriaUseCase mensajeria;
    private final EmpleadoUseCase empleados;
    private final BiConsumer<Route, String> navigate;

    private final ComboBox<Empleado> who = new ComboBox<>();
    private final ComboBox<Empleado> newDirect = new ComboBox<>();
    private final VBox channelList = new VBox(2);
    private final VBox directList = new VBox(2);
    private final Label channelTitle = new Label();
    private final Label channelDescription = new Label();
    private final VBox messages = new VBox(12);
    private final ScrollPane scroll = new ScrollPane(messages);
    private final TextArea input = new TextArea();
    private final Button send = new Button(null, Icons.of(Feather.SEND, 16));
    private final StackPane main = new StackPane();
    private final Node conversation;
    private final Timeline poll = new Timeline(new KeyFrame(POLL, e -> poll()));

    private Map<Integer, Empleado> people = Map.of();
    private List<Canal> canales = List.of();
    private Map<Integer, Long> unread = new HashMap<>();
    private final Set<Long> shown = new HashSet<>();
    private Canal active;
    private Integer me;
    private long lastId;
    private boolean polling;
    private boolean updatingWho;

    public ChatView(MensajeriaUseCase mensajeria, EmpleadoUseCase empleados, BiConsumer<Route, String> navigate) {
        super(Route.CHAT, false);
        this.mensajeria = mensajeria;
        this.empleados = empleados;
        this.navigate = navigate;
        this.conversation = buildConversation();
        poll.setCycleCount(Timeline.INDEFINITE);

        Button reload = new Button("Actualizar", Icons.of(Feather.REFRESH_CW, 15));
        reload.setOnAction(e -> refresh());
        addActions(reload);
    }

    @Override
    protected Node buildContent() {
        StringConverter<Empleado> converter = new StringConverter<>() {
            @Override
            public String toString(Empleado e) {
                return e == null ? "" : e.getNombreCompleto();
            }

            @Override
            public Empleado fromString(String s) {
                return null;
            }
        };
        who.setConverter(converter);
        who.setCellFactory(l -> personCell(true));
        who.setPromptText("Elige quién eres");
        who.setAccessibleText("Escribes como");
        who.setMaxWidth(Double.MAX_VALUE);
        who.valueProperty().addListener((o, a, b) -> {
            if (!updatingWho && b != null && !b.getCodigoEmpleado().equals(me)) {
                ChatIdentity.set(b.getCodigoEmpleado());
                me = b.getCodigoEmpleado();
                active = null;
                refresh();
            }
        });
        newDirect.setConverter(converter);
        newDirect.setCellFactory(l -> personCell(true));
        newDirect.setPromptText("Escribir a…");
        newDirect.setAccessibleText("Nuevo mensaje directo");
        newDirect.setMaxWidth(Double.MAX_VALUE);
        newDirect.setOnAction(e -> {
            Empleado target = newDirect.getValue();
            if (target != null && me != null) {
                Platform.runLater(() -> newDirect.getSelectionModel().clearSelection());
                abrirDirecto(target.getCodigoEmpleado());
            }
        });

        Label whoLabel = sectionLabel("Escribes como");
        Label channelsLabel = sectionLabel("Canales");
        Label directsLabel = sectionLabel("Mensajes directos");
        channelList.setAccessibleRole(AccessibleRole.LIST_VIEW);
        channelList.setAccessibleText("Canales");
        directList.setAccessibleRole(AccessibleRole.LIST_VIEW);
        directList.setAccessibleText("Mensajes directos");
        VBox side = new VBox(6, whoLabel, who, channelsLabel, channelList, directsLabel, directList, newDirect);
        side.getStyleClass().add("team-side");
        ScrollPane sideScroll = new ScrollPane(side);
        sideScroll.setFitToWidth(true);
        sideScroll.getStyleClass().addAll("team-side-scroll", "edge-to-edge");
        sideScroll.setMinWidth(230);
        sideScroll.setPrefWidth(250);
        sideScroll.setMaxWidth(270);

        HBox.setHgrow(main, Priority.ALWAYS);
        HBox page = new HBox(16, sideScroll, main);
        page.getStyleClass().addAll("chat-page", "team-page");
        mostrarSinIdentidad();
        return page;
    }

    private Node buildConversation() {
        channelTitle.getStyleClass().add("team-title");
        channelDescription.getStyleClass().add("team-description");
        channelDescription.setWrapText(true);
        VBox header = new VBox(2, channelTitle, channelDescription);
        header.getStyleClass().add("team-header");

        messages.getStyleClass().add("team-messages");
        messages.setAccessibleRole(AccessibleRole.LIST_VIEW);
        messages.setAccessibleText("Mensajes del canal");
        scroll.setFitToWidth(true);
        scroll.getStyleClass().addAll("chat-scroll", "edge-to-edge");
        messages.heightProperty().addListener((o, a, b) -> scroll.setVvalue(1));
        VBox.setVgrow(scroll, Priority.ALWAYS);

        input.getStyleClass().add("chat-input");
        input.setWrapText(true);
        input.setPrefRowCount(2);
        input.setAccessibleText("Mensaje");
        input.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                e.consume();
                if (e.isShiftDown()) {
                    input.insertText(input.getCaretPosition(), "\n");
                } else {
                    enviar();
                }
            }
        });
        HBox.setHgrow(input, Priority.ALWAYS);
        send.getStyleClass().addAll("accent", "chat-send");
        send.setAccessibleText("Enviar mensaje");
        send.setTooltip(new Tooltip("Enviar (Intro). Mayús+Intro añade una línea."));
        send.setOnAction(e -> enviar());
        send.disableProperty().bind(input.textProperty().isEmpty());
        HBox composer = new HBox(10, input, send);
        composer.setAlignment(Pos.BOTTOM_LEFT);
        composer.getStyleClass().add("chat-composer");
        Label hint = new Label("#14 enlaza un pedido · @nombre.apellido menciona a alguien · los mensajes se actualizan cada 3 s");
        hint.getStyleClass().add("chat-hint");
        hint.setWrapText(true);

        VBox box = new VBox(10, header, scroll, composer, hint);
        box.getStyleClass().add("team-conversation");
        return box;
    }

    // ---------------------------------------------------------------- ciclo de vida

    @Override
    public void onShow() {
        refresh();
        poll.play();
    }

    @Override
    public void refresh() {
        if (me == null) {
            ChatIdentity.current().ifPresent(v -> me = v);
        }
        Integer yo = me;
        load(() -> {
            List<Empleado> lista = empleados.listarTodos();
            long last = mensajeria.ultimoMensaje();
            Sondeo s = yo == null ? null : mensajeria.sondear(yo, last);
            return new Inicio(lista, last, s);
        }, this::aplicarInicio);
    }

    private record Inicio(List<Empleado> empleados, long ultimo, Sondeo sondeo) {
    }

    private void aplicarInicio(Inicio init) {
        people = init.empleados().stream().collect(Collectors.toMap(Empleado::getCodigoEmpleado, Function.identity(), (a, b) -> a));
        List<Empleado> ordenados = init.empleados().stream()
                .sorted(Comparator.comparing(Empleado::getNombreCompleto, String.CASE_INSENSITIVE_ORDER)).toList();
        updatingWho = true;
        who.getItems().setAll(ordenados);
        who.setValue(me == null ? null : people.get(me));
        updatingWho = false;
        if (me == null || !people.containsKey(me) || init.sondeo() == null) {
            me = null;
            mostrarSinIdentidad();
            return;
        }
        newDirect.getItems().setAll(ordenados.stream().filter(e -> !e.getCodigoEmpleado().equals(me)).toList());
        lastId = init.ultimo();
        canales = init.sondeo().canales();
        unread = new HashMap<>(init.sondeo().noLeidos());
        main.getChildren().setAll(conversation);
        Canal destino = active == null ? null : canales.stream().filter(c -> c.id() == active.id()).findFirst().orElse(null);
        if (destino == null) {
            destino = canales.stream().filter(c -> c.tipo() == Canal.Tipo.GENERAL).findFirst()
                    .orElse(canales.isEmpty() ? null : canales.get(0));
        }
        renderCanales();
        if (destino != null) {
            abrir(destino);
        }
    }

    @Override
    protected void showError(Throwable error) {
        String msg = error.getMessage() == null ? "" : error.getMessage();
        if (msg.contains("doesn't exist") && (msg.contains("canal") || msg.contains("mensaje") || msg.contains("lectura"))) {
            poll.stop();
            EmptyState state = new EmptyState(Feather.DATABASE, "Falta el esquema de mensajería",
                    "Tu volumen de MySQL es anterior al chat. Crea las tablas con:\n"
                            + "docker exec -i jardineria-mysql mysql -uroot -ppassword jardineria < docker/init/02-chat.sql\n"
                            + "y pulsa Reintentar.",
                    "Reintentar", () -> {
                        refresh();
                        poll.play();
                    });
            main.getChildren().setAll(state);
            return;
        }
        super.showError(error);
    }

    private void mostrarSinIdentidad() {
        channelList.getChildren().clear();
        directList.getChildren().clear();
        newDirect.setDisable(true);
        EmptyState state = new EmptyState(Feather.USER, "¿Quién eres?",
                "Elige tu nombre en «Escribes como» para ver los canales de tu equipo. No hace falta contraseña: "
                        + "la aplicación lo recuerda en este equipo.");
        main.getChildren().setAll(state);
    }

    // ---------------------------------------------------------------- canales

    private void renderCanales() {
        newDirect.setDisable(me == null);
        channelList.getChildren().setAll(canales.stream().filter(c -> c.tipo() != Canal.Tipo.DIRECTO)
                .map(this::filaCanal).toList());
        List<Node> directos = canales.stream().filter(c -> c.tipo() == Canal.Tipo.DIRECTO)
                .sorted(Comparator.comparing(this::titulo, String.CASE_INSENSITIVE_ORDER))
                .map(this::filaCanal).toList();
        if (directos.isEmpty()) {
            Label none = new Label("Todavía no hay conversaciones");
            none.getStyleClass().add("team-empty");
            directList.getChildren().setAll(none);
        } else {
            directList.getChildren().setAll(directos);
        }
    }

    private Node filaCanal(Canal c) {
        Node lead;
        if (c.tipo() == Canal.Tipo.DIRECTO) {
            int otro = c.otroParticipante(me).orElse(0);
            lead = new Avatar(titulo(c), otro, Avatar.Size.SMALL);
        } else {
            lead = Icons.of(c.tipo() == Canal.Tipo.GENERAL ? Feather.HASH : Feather.MAP_PIN, 14);
        }
        Label name = new Label(titulo(c));
        name.getStyleClass().add("team-channel-name");
        name.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(name, Priority.ALWAYS);
        HBox row = new HBox(8, lead, name);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("team-channel", "interactive-row");
        long pendientes = unread.getOrDefault(c.id(), 0L);
        if (pendientes > 0) {
            Label badge = new Label(pendientes > 99 ? "99+" : String.valueOf(pendientes));
            badge.getStyleClass().add("team-unread");
            badge.setMinWidth(Region.USE_PREF_SIZE);
            row.getChildren().add(badge);
            row.getStyleClass().add("team-channel-unread");
        }
        if (active != null && active.id() == c.id()) {
            row.getStyleClass().add("team-channel-active");
        }
        row.setUserData(c.id());
        Interactions.activatable(row, () -> abrir(c));
        row.setAccessibleText((c.tipo() == Canal.Tipo.DIRECTO ? "Conversación con " : "Canal ") + titulo(c)
                + (pendientes > 0 ? ", " + pendientes + " sin leer" : ""));
        return row;
    }

    /** Nombre visible: «general», «oficina-tal-es» o el nombre de la otra persona. */
    String titulo(Canal c) {
        if (c.tipo() == Canal.Tipo.DIRECTO && me != null) {
            return c.otroParticipante(me).stream().mapToObj(people::get)
                    .filter(java.util.Objects::nonNull).map(Empleado::getNombreCompleto).findFirst().orElse(c.nombre());
        }
        return c.nombre();
    }

    private void abrir(Canal c) {
        active = c;
        shown.clear();
        messages.getChildren().clear();
        channelTitle.setText((c.tipo() == Canal.Tipo.DIRECTO ? "" : "# ") + titulo(c));
        channelDescription.setText(c.tipo() == Canal.Tipo.DIRECTO ? "Mensaje directo"
                : c.descripcion() == null ? "" : c.descripcion());
        input.setPromptText(c.tipo() == Canal.Tipo.DIRECTO ? "Escribe a " + titulo(c) + "…" : "Escribe en # " + c.nombre() + "…");
        renderCanales();
        int id = c.id();
        Integer yo = me;
        FxTasks.run(() -> mensajeria.historial(id, HISTORIAL), lista -> {
            if (active == null || active.id() != id) {
                return;
            }
            if (lista.isEmpty()) {
                Label empty = new Label("Todavía no hay mensajes. Escribe el primero.");
                empty.getStyleClass().add("team-empty");
                messages.getChildren().setAll(empty);
            } else {
                messages.getChildren().clear();
                lista.forEach(this::agregar);
                marcarLeido(id, yo, lista.get(lista.size() - 1).id());
            }
            input.requestFocus();
        }, this::showError);
    }

    private void abrirDirecto(int otro) {
        Integer yo = me;
        FxTasks.run(() -> mensajeria.canalDirecto(yo, otro), canal -> {
            if (canales.stream().noneMatch(c -> c.id() == canal.id())) {
                List<Canal> nuevos = new ArrayList<>(canales);
                nuevos.add(canal);
                canales = nuevos;
            }
            abrir(canal);
        }, error -> Notifier.error("No se pudo abrir la conversación", FxTasks.friendlyMessage(error)));
    }

    private void marcarLeido(int idCanal, Integer yo, long ultimo) {
        if (unread.remove(idCanal) != null) {
            renderCanales();
        }
        FxTasks.runVoid(() -> mensajeria.marcarLeido(idCanal, yo, ultimo), () -> {
        }, error -> {
        });
    }

    // ---------------------------------------------------------------- mensajes

    void enviar() {
        String texto = input.getText();
        if (texto == null || texto.isBlank() || active == null || me == null) {
            return;
        }
        int id = active.id();
        Integer yo = me;
        input.clear();
        FxTasks.run(() -> mensajeria.enviar(id, yo, texto), m -> {
            if (active != null && active.id() == m.idCanal()) {
                messages.getChildren().removeIf(n -> n.getStyleClass().contains("team-empty"));
                agregar(m);
            }
        }, error -> {
            input.setText(texto);
            Notifier.error("No se pudo enviar", FxTasks.friendlyMessage(error));
        });
    }

    /** Una consulta de mensajes nuevos (la lanza el temporizador cada 3 s; pública para las pruebas). */
    public void poll() {
        if (polling || me == null || getScene() == null || !isVisible() || canales.isEmpty()) {
            return;
        }
        polling = true;
        Integer yo = me;
        long desde = lastId;
        FxTasks.run(() -> mensajeria.sondear(yo, desde), s -> {
            polling = false;
            if (!yo.equals(me)) {
                return;
            }
            aplicarSondeo(s);
        }, error -> polling = false);
    }

    void aplicarSondeo(Sondeo s) {
        lastId = Math.max(lastId, s.ultimoId(lastId));
        String miId = identificador(me);
        long ultimoActivo = 0;
        for (Mensaje m : s.nuevos()) {
            if (active != null && m.idCanal() == active.id()) {
                messages.getChildren().removeIf(n -> n.getStyleClass().contains("team-empty"));
                agregar(m);
                ultimoActivo = Math.max(ultimoActivo, m.id());
            } else if (m.codigoEmpleado() != me && m.menciona(miId)) {
                String canal = s.canales().stream().filter(c -> c.id() == m.idCanal()).findFirst()
                        .map(this::titulo).orElse("otro canal");
                Notifier.info(nombre(m.codigoEmpleado()) + " te menciona en " + canal, recortar(m.texto()));
            }
        }
        canales = s.canales();
        unread = new HashMap<>(s.noLeidos());
        if (active != null && ultimoActivo > 0) {
            unread.remove(active.id());
            marcarLeido(active.id(), me, ultimoActivo);
        }
        renderCanales();
    }

    private void agregar(Mensaje m) {
        if (!shown.add(m.id())) {
            return;
        }
        Empleado autor = people.get(m.codigoEmpleado());
        String nombre = nombre(m.codigoEmpleado());
        Label author = new Label(nombre);
        author.getStyleClass().add("team-author");
        Label when = new Label(hora(m.enviado()));
        when.getStyleClass().add("team-time");
        HBox meta = new HBox(8, author, when);
        meta.setAlignment(Pos.BASELINE_LEFT);
        if (autor != null && autor.getPuesto() != null) {
            Label role = new Label(autor.getPuesto());
            role.getStyleClass().add("team-role");
            meta.getChildren().add(1, role);
        }
        TextFlow body = enlazar(m.texto());
        body.getStyleClass().add("team-text");
        VBox content = new VBox(2, meta, body);
        HBox.setHgrow(content, Priority.ALWAYS);
        HBox row = new HBox(10, new Avatar(nombre, m.codigoEmpleado(), Avatar.Size.MEDIUM), content);
        row.getStyleClass().add("team-message");
        if (me != null && m.codigoEmpleado() != me && m.menciona(identificador(me))) {
            row.getStyleClass().add("team-message-mention");
        }
        row.setAccessibleRole(AccessibleRole.TEXT);
        row.setAccessibleText(nombre + ", " + hora(m.enviado()) + ": " + m.texto());
        messages.getChildren().add(row);
    }

    /** Texto con {@code #N} como enlaces al pedido y {@code @menciones} resaltadas. */
    TextFlow enlazar(String texto) {
        TextFlow flow = new TextFlow();
        String mine = "@" + identificador(me);
        Matcher m = TOKEN.matcher(texto);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                flow.getChildren().add(run(texto.substring(last, m.start()), "team-text-run"));
            }
            if (m.group(1) != null) {
                String codigo = m.group(1).substring(1);
                Hyperlink link = new Hyperlink(m.group(1));
                link.getStyleClass().add("chat-link");
                link.setAccessibleText("Abrir el pedido " + codigo);
                link.setOnAction(e -> navigate.accept(Route.PEDIDOS, "#" + codigo));
                flow.getChildren().add(link);
            } else {
                boolean propia = Formatters.fold(m.group(2)).equals(Formatters.fold(mine));
                flow.getChildren().add(run(m.group(2), propia ? "team-mention-me" : "team-mention"));
            }
            last = m.end();
        }
        if (last < texto.length()) {
            flow.getChildren().add(run(texto.substring(last), "team-text-run"));
        }
        return flow;
    }

    private static Text run(String s, String styleClass) {
        Text t = new Text(s);
        t.getStyleClass().add(styleClass);
        return t;
    }

    private String nombre(int codigo) {
        Empleado e = people.get(codigo);
        return e == null ? "Empleado " + codigo : e.getNombreCompleto();
    }

    private String identificador(Integer codigo) {
        Empleado e = codigo == null ? null : people.get(codigo);
        return e == null ? "" : Mensaje.identificador(e.getNombre(), e.getApellido1());
    }

    private static String hora(LocalDateTime t) {
        if (t == null) {
            return "";
        }
        return t.toLocalDate().equals(LocalDate.now()) ? HORA.format(t) : DIA_HORA.format(t);
    }

    private static String recortar(String s) {
        String t = s.strip().replaceAll("\\s+", " ");
        return t.length() > 120 ? t.substring(0, 120) + "…" : t;
    }

    private static Label sectionLabel(String text) {
        Label l = new Label(text.toUpperCase());
        l.getStyleClass().add("team-section");
        return l;
    }

    private ListCell<Empleado> personCell(boolean withRole) {
        return new ListCell<>() {
            @Override
            protected void updateItem(Empleado e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(e.getNombreCompleto() + (withRole && e.getPuesto() != null ? " · " + e.getPuesto() : "")
                        + "   @" + Mensaje.identificador(e.getNombre(), e.getApellido1()));
                setGraphic(new Avatar(e.getNombreCompleto(), e.getCodigoEmpleado(), Avatar.Size.SMALL));
            }
        };
    }
}
