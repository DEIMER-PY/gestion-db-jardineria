package com.jardineria.infrastructure.adapter.in.desktop.shell;

import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import com.jardineria.infrastructure.adapter.in.desktop.support.ModalHost;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

import java.util.ArrayList;
import java.util.List;
import com.jardineria.infrastructure.adapter.in.desktop.support.Keys;

/**
 * Chuleta de atajos de teclado (F1 o Ctrl+/), agrupada por contexto.
 * Facilita descubrir la navegación sin ratón.
 */
public class ShortcutsSheet extends VBox {

    record Shortcut(String keys, String action) {
    }

    public ShortcutsSheet() {
        super(16);
        getStyleClass().addAll("dialog", "shortcuts-sheet");
        setMaxSize(760, USE_PREF_SIZE);

        Label title = new Label("Atajos de teclado");
        title.getStyleClass().add("dialog-title");
        Label subtitle = new Label("Toda la aplicación se puede usar sin ratón. Pulsa Esc para cerrar.");
        subtitle.getStyleClass().add("dialog-message");
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        Button close = new Button(null, Icons.of(Feather.X, 16));
        close.getStyleClass().addAll("flat", "button-icon");
        close.setAccessibleText("Cerrar atajos de teclado");
        close.setCancelButton(true);
        close.setOnAction(e -> ModalHost.hide());
        HBox header = new HBox(12, new VBox(2, title, subtitle), grow, close);
        header.setAlignment(Pos.TOP_LEFT);

        List<Shortcut> nav = new ArrayList<>();
        for (Route r : Route.values()) {
            if (r.shortcut() != null) {
                nav.add(new Shortcut(r.shortcutLabel(), r.title()));
            }
        }
        nav.add(new Shortcut(Keys.text("Alt ←"), "Vista anterior"));
        nav.add(new Shortcut(Keys.text("Alt →"), "Vista siguiente"));

        List<Shortcut> general = List.of(
                new Shortcut(Keys.text("Ctrl K"), "Paleta de comandos"),
                new Shortcut("F5", "Actualizar datos"),
                new Shortcut(Keys.text("Ctrl T"), "Tema oscuro / claro"),
                new Shortcut(Keys.text("Ctrl B"), "Contraer menú lateral"),
                new Shortcut("F1", "Esta ayuda"),
                new Shortcut("Esc", "Cerrar diálogo"));
        List<Shortcut> data = List.of(
                new Shortcut(Keys.text("Ctrl N"), "Nuevo registro"),
                new Shortcut("Enter", "Editar selección"),
                new Shortcut(Keys.text("Supr"), "Eliminar selección"),
                new Shortcut(Keys.text("Ctrl F"), "Buscar en el módulo"));
        List<Shortcut> a11y = List.of(
                new Shortcut(Keys.text("Ctrl +"), "Aumentar texto"),
                new Shortcut(Keys.text("Ctrl -"), "Reducir texto"),
                new Shortcut(Keys.text("Ctrl 0"), "Tamaño original"),
                new Shortcut("Tab", "Siguiente control"));

        GridPane grid = new GridPane();
        grid.setHgap(28);
        grid.setVgap(18);
        grid.add(group("Navegación", nav), 0, 0, 1, 2);
        grid.add(group("General", general), 1, 0);
        grid.add(group("Datos", data), 2, 0);
        grid.add(group("Accesibilidad", a11y), 1, 1, 2, 1);

        getChildren().addAll(header, grid);
        setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                ModalHost.hide();
            }
        });
        Platform.runLater(close::requestFocus);
    }

    private static VBox group(String title, List<Shortcut> items) {
        Label t = new Label(title.toUpperCase());
        t.getStyleClass().add("shortcut-group");
        VBox box = new VBox(7, t);
        for (Shortcut s : items) {
            HBox keys = new HBox(4);
            for (String k : s.keys().split(" ")) {
                Label kbd = new Label(k);
                kbd.getStyleClass().add("kbd");
                keys.getChildren().add(kbd);
            }
            keys.setMinWidth(70);
            Label action = new Label(s.action());
            action.getStyleClass().add("shortcut-action");
            HBox row = new HBox(10, keys, action);
            row.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().add(row);
        }
        return box;
    }
}
