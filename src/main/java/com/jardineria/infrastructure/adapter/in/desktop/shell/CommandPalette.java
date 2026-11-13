package com.jardineria.infrastructure.adapter.in.desktop.shell;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.controls.ModalPane;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Paleta de comandos (Ctrl+K) al estilo de los editores modernos: búsqueda difusa
 * insensible a tildes para saltar a cualquier módulo o ejecutar acciones rápidas
 * sin usar el ratón.
 */
public class CommandPalette extends VBox {

    /** Entrada de la paleta. */
    public record Command(String title, String hint, Ikon icon, String shortcut, Runnable action) {
        String searchable() {
            return normalize(title + " " + hint);
        }
    }

    private final ModalPane modal;
    private final Supplier<List<Command>> source;
    private final ObservableList<Command> all = FXCollections.observableArrayList();
    private final CustomTextField input = new CustomTextField();
    private final FilteredList<Command> filtered;
    private final ListView<Command> list;

    /** @param commands proveedor de comandos: se consulta en cada apertura (perfiles nuevos, etc.). */
    public CommandPalette(ModalPane modal, Supplier<List<Command>> commands) {
        this.modal = modal;
        this.source = commands;
        getStyleClass().add("palette");
        setMaxSize(620, 460);
        setSpacing(0);

        input.setPromptText("Escribe un módulo o una acción…  (p. ej. \"clientes\", \"tema\", \"nuevo\")");
        input.setLeft(Icons.of(Feather.SEARCH, 16));
        Label esc = new Label("Esc");
        esc.getStyleClass().add("kbd");
        input.setRight(esc);
        input.getStyleClass().add("palette-input");
        input.setAccessibleText("Buscar comando");

        filtered = new FilteredList<>(all, c -> true);
        list = new ListView<>(filtered);
        list.getStyleClass().add("palette-list");
        list.setCellFactory(v -> new CommandCell());
        list.setPlaceholder(new Label("Sin resultados"));
        VBox.setVgrow(list, Priority.ALWAYS);

        input.textProperty().addListener((obs, old, text) -> {
            String[] tokens = normalize(text).trim().split("\\s+");
            filtered.setPredicate(c -> {
                String haystack = c.searchable();
                for (String t : tokens) {
                    if (!t.isEmpty() && !haystack.contains(t)) {
                        return false;
                    }
                }
                return true;
            });
            list.getSelectionModel().selectFirst();
        });

        input.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) {
                list.getSelectionModel().selectNext();
                list.scrollTo(list.getSelectionModel().getSelectedIndex());
                e.consume();
            } else if (e.getCode() == KeyCode.UP) {
                list.getSelectionModel().selectPrevious();
                list.scrollTo(list.getSelectionModel().getSelectedIndex());
                e.consume();
            } else if (e.getCode() == KeyCode.ENTER) {
                execute(list.getSelectionModel().getSelectedItem());
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                close();
                e.consume();
            }
        });
        list.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                execute(list.getSelectionModel().getSelectedItem());
            } else if (e.getCode() == KeyCode.ESCAPE) {
                close();
            }
        });
        list.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                execute(list.getSelectionModel().getSelectedItem());
            }
        });

        Label footer = new Label("↑ ↓ para moverse  ·  Enter para abrir  ·  Esc para cerrar");
        footer.getStyleClass().add("palette-footer");

        getChildren().addAll(input, list, footer);
    }

    public void open() {
        open("");
    }

    /** Abre la paleta con una búsqueda ya escrita (p. ej. "tema"). */
    public void open(String query) {
        all.setAll(source.get());
        input.clear();
        filtered.setPredicate(c -> true);
        input.setText(query == null ? "" : query);
        input.positionCaret(input.getText().length());
        list.getSelectionModel().selectFirst();
        modal.show(this);
        Platform.runLater(input::requestFocus);
    }

    private void close() {
        modal.hide(true);
    }

    private void execute(Command command) {
        if (command == null) {
            return;
        }
        close();
        Platform.runLater(command.action());
    }

    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private static final class CommandCell extends ListCell<Command> {
        @Override
        protected void updateItem(Command item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            StackPane icon = new StackPane(Icons.of(item.icon(), 16));
            icon.getStyleClass().add("palette-icon");
            Label title = new Label(item.title());
            title.getStyleClass().add("palette-title");
            Label hint = new Label(item.hint());
            hint.getStyleClass().add("palette-hint");
            VBox text = new VBox(1, title, hint);
            Region grow = new Region();
            HBox.setHgrow(grow, Priority.ALWAYS);
            HBox row = new HBox(12, icon, text, grow);
            row.setAlignment(Pos.CENTER_LEFT);
            if (item.shortcut() != null && !item.shortcut().isBlank()) {
                Label kbd = new Label(item.shortcut());
                kbd.getStyleClass().add("kbd");
                row.getChildren().add(kbd);
            }
            setText(null);
            setGraphic(row);
            setAccessibleText(item.title() + ". " + item.hint());
        }
    }
}
