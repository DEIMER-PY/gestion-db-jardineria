package com.jardineria.infrastructure.adapter.in.desktop.views.board;

import com.jardineria.application.port.in.ClienteUseCase;
import com.jardineria.application.port.in.PedidoUseCase;
import com.jardineria.domain.model.Cliente;
import com.jardineria.domain.model.Pedido;
import com.jardineria.infrastructure.adapter.in.common.Formatters;
import com.jardineria.infrastructure.adapter.in.desktop.components.EmptyState;
import com.jardineria.infrastructure.adapter.in.desktop.components.ResponsiveGrid;
import com.jardineria.infrastructure.adapter.in.desktop.components.StatusBadge;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.support.FxTasks;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import com.jardineria.infrastructure.adapter.in.desktop.support.Interactions;
import com.jardineria.infrastructure.adapter.in.desktop.support.ModalHost;
import com.jardineria.infrastructure.adapter.in.desktop.support.Motion;
import com.jardineria.infrastructure.adapter.in.desktop.support.Notifier;
import com.jardineria.infrastructure.adapter.in.desktop.views.BaseView;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Tablero Kanban de pedidos: una columna por estado. Una tarjeta se mueve arrastrándola
 * a otra columna o con las flechas izquierda y derecha; siempre se pide confirmación
 * porque el cambio se guarda en MySQL (y «Entregado» registra la fecha de entrega).
 */
public class BoardView extends BaseView {
    /** Orden de las columnas: el flujo natural de un pedido. */
    public static final List<String> COLUMNS = List.of("Pendiente", "Entregado", "Rechazado");

    private final PedidoUseCase pedidos;
    private final ClienteUseCase clientes;
    private final BiConsumer<Route, String> navigate;
    private final TextField search = new TextField();
    private final ResponsiveGrid board = new ResponsiveGrid(280, 3, 16);

    private List<Pedido> all = List.of();
    private Map<Integer, String> nombres = Map.of();

    public BoardView(PedidoUseCase pedidos, ClienteUseCase clientes, BiConsumer<Route, String> navigate) {
        super(Route.TABLERO);
        this.pedidos = pedidos;
        this.clientes = clientes;
        this.navigate = navigate;

        search.setPromptText("Filtrar por pedido o cliente…");
        search.getStyleClass().add("board-search");
        search.setAccessibleText("Filtrar tarjetas por código de pedido o cliente");
        search.textProperty().addListener((obs, o, n) -> render());
        Button refresh = new Button("Actualizar", Icons.of(Feather.REFRESH_CW, 15));
        refresh.getStyleClass().add("accent");
        refresh.setOnAction(e -> refresh());
        addActions(search, refresh);
    }

    /** Columna de un estado (insensible a mayúsculas y tildes); los desconocidos van a Pendiente. */
    public static String columnOf(String estado) {
        String e = Formatters.fold(estado);
        return COLUMNS.stream().filter(c -> Formatters.fold(c).equals(e)).findFirst().orElse(COLUMNS.get(0));
    }

    /** Columna vecina en la dirección indicada (-1 izquierda, +1 derecha), si existe. */
    public static Optional<String> neighbour(String estado, int direction) {
        int i = COLUMNS.indexOf(columnOf(estado)) + direction;
        return i < 0 || i >= COLUMNS.size() ? Optional.empty() : Optional.of(COLUMNS.get(i));
    }

    @Override
    protected Node buildContent() {
        Label hint = new Label("Arrastra una tarjeta a otra columna, o usa las flechas izquierda y derecha sobre ella, "
                + "para cambiar su estado. Intro abre el pedido.");
        hint.getStyleClass().add("text-subtle");
        hint.setWrapText(true);
        VBox body = new VBox(14, hint, board);
        body.getStyleClass().add("view-body");
        Motion.enter(board, 60);
        refresh();
        return body;
    }

    @Override
    public void refresh() {
        load(() -> new Data(pedidos.listarTodos(), clientes.listarTodos().stream()
                        .collect(Collectors.toMap(Cliente::getCodigoCliente, Cliente::getNombreCliente, (a, b) -> a))),
                d -> {
                    all = d.pedidos();
                    nombres = d.nombres();
                    render();
                });
    }

    private record Data(List<Pedido> pedidos, Map<Integer, String> nombres) {
    }

    @Override
    public void reveal(String query) {
        search.setText(query == null ? "" : query.trim());
    }

    private boolean matches(Pedido p) {
        String q = Formatters.fold(search.getText());
        if (q.isBlank()) {
            return true;
        }
        String text = Formatters.fold("#" + p.getCodigoPedido() + " " + nombres.getOrDefault(p.getCodigoCliente(), ""));
        return text.contains(q.trim());
    }

    private void render() {
        board.getChildren().clear();
        for (String estado : COLUMNS) {
            List<Pedido> items = all.stream()
                    .filter(p -> columnOf(p.getEstado()).equals(estado) && matches(p))
                    .sorted(Comparator.comparing(Pedido::getFechaPedido, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
            board.add(column(estado, items), 1);
        }
    }

    private Node column(String estado, List<Pedido> items) {
        Label title = new Label(estado);
        title.getStyleClass().add("panel-title");
        Label count = new Label(String.valueOf(items.size()));
        count.getStyleClass().addAll("badge", StatusBadge.toneFor(estado));
        HBox head = new HBox(10, title, count);
        head.setAlignment(Pos.CENTER_LEFT);

        VBox cards = new VBox(8);
        items.forEach(p -> cards.getChildren().add(card(p)));
        if (items.isEmpty()) {
            cards.getChildren().add(new EmptyState(Feather.INBOX, "Sin pedidos", "Suelta aquí una tarjeta para marcarla como "
                    + estado.toLowerCase(Formatters.ES) + "."));
        }
        VBox col = new VBox(14, head, cards);
        col.getStyleClass().addAll("panel", "board-column");
        col.setAccessibleText("Columna " + estado + ", " + items.size() + " pedidos");

        col.setOnDragOver(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasString() && !estado.equals(estadoDe(db.getString()))) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });
        col.setOnDragEntered(e -> {
            if (e.getDragboard().hasString() && !estado.equals(estadoDe(e.getDragboard().getString()))) {
                col.getStyleClass().add("board-drop-target");
            }
        });
        col.setOnDragExited(e -> col.getStyleClass().remove("board-drop-target"));
        col.setOnDragDropped(e -> {
            boolean ok = false;
            if (e.getDragboard().hasString()) {
                findPedido(e.getDragboard().getString()).ifPresent(p -> askMove(p, estado));
                ok = true;
            }
            e.setDropCompleted(ok);
            e.consume();
        });
        return col;
    }

    private String estadoDe(String codigo) {
        return findPedido(codigo).map(p -> columnOf(p.getEstado())).orElse(null);
    }

    private Optional<Pedido> findPedido(String codigo) {
        return all.stream().filter(p -> String.valueOf(p.getCodigoPedido()).equals(codigo)).findFirst();
    }

    private Node card(Pedido p) {
        Label code = new Label("Pedido #" + p.getCodigoPedido());
        code.getStyleClass().add("timeline-title");
        String cliente = Formatters.orDash(nombres.get(p.getCodigoCliente()));
        Label who = new Label(cliente, Icons.of(Feather.USER, 12));
        who.getStyleClass().add("timeline-detail");
        // La fecha depende de la columna: en los datos de ejemplo hay pedidos pendientes o rechazados con fecha de entrega
        boolean entregado = "Entregado".equals(columnOf(p.getEstado()));
        Label dates = new Label(Formatters.date(p.getFechaPedido()) + "  →  " + (entregado
                ? (p.getFechaEntrega() == null ? "sin fecha de entrega" : "entregado " + Formatters.date(p.getFechaEntrega()))
                : "previsto " + Formatters.date(p.getFechaEsperada())));
        dates.getStyleClass().add("timeline-detail");
        VBox card = new VBox(3, code, who, dates);
        card.getStyleClass().add("board-card");
        card.setMaxWidth(Double.MAX_VALUE);

        Interactions.activatable(card, () -> navigate.accept(Route.PEDIDOS, "#" + p.getCodigoPedido()));
        card.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            int dir = switch (e.getCode()) {
                case LEFT -> -1;
                case RIGHT -> 1;
                default -> 0;
            };
            if (dir != 0) {
                neighbour(p.getEstado(), dir).ifPresent(target -> askMove(p, target));
                e.consume();
            }
        });
        card.setAccessibleText("Pedido " + p.getCodigoPedido() + " de " + cliente + ", " + columnOf(p.getEstado())
                + ". Intro abre el pedido; flechas izquierda y derecha cambian su estado.");
        Tooltip.install(card, new Tooltip("Arrastra para cambiar el estado · Intro abre el pedido"));

        card.setOnDragDetected(e -> {
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(p.getCodigoPedido()));
            db.setContent(content);
            db.setDragView(card.snapshot(null, null));
            card.getStyleClass().add("board-card-dragging");
            e.consume();
        });
        card.setOnDragDone(e -> card.getStyleClass().remove("board-card-dragging"));
        return card;
    }

    private void askMove(Pedido p, String target) {
        String consecuencia = switch (target) {
            case "Entregado" -> "Se registrará la entrega con la fecha de hoy.";
            case "Rechazado" -> "El pedido dejará de contar como pendiente.";
            default -> "El pedido volverá a estar pendiente de entrega.";
        };
        ModalHost.confirm(Feather.COLUMNS, "¿Mover el pedido #" + p.getCodigoPedido() + " a " + target + "?",
                consecuencia, "Mover a " + target, "accent", () -> move(p, target));
    }

    private void move(Pedido p, String target) {
        FxTasks.run(() -> pedidos.cambiarEstado(p.getCodigoPedido(), target, null), actualizado -> {
            Notifier.success("Estado actualizado", "El pedido #" + p.getCodigoPedido() + " ahora está "
                    + target.toLowerCase(Formatters.ES) + ".");
            refresh();
        }, e -> Notifier.error("No se pudo cambiar el estado", FxTasks.friendlyMessage(FxTasks.unwrap(e))));
    }
}
