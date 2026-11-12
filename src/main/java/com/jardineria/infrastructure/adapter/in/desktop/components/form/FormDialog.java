package com.jardineria.infrastructure.adapter.in.desktop.components.form;

import atlantafx.base.controls.RingProgressIndicator;
import com.jardineria.domain.exception.BusinessValidationException;
import com.jardineria.domain.exception.DuplicateEntityException;
import com.jardineria.domain.exception.EntityNotFoundException;
import com.jardineria.infrastructure.adapter.in.common.Formatters;
import com.jardineria.infrastructure.adapter.in.desktop.support.FxTasks;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import com.jardineria.infrastructure.adapter.in.desktop.support.ModalHost;
import com.jardineria.infrastructure.adapter.in.desktop.support.Notifier;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Formulario modal con constructor fluido de campos tipados.
 * <p>
 * Valida en el cliente (obligatorios, formatos, reglas), guarda en segundo plano
 * sin congelar la ventana y traduce las excepciones de dominio a mensajes en línea:
 * {@link DuplicateEntityException} marca el campo clave, {@link BusinessValidationException}
 * se muestra como aviso del formulario. Esc cancela y Enter guarda.
 */
public class FormDialog {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final List<FormField<?>> fields = new ArrayList<>();
    /** Orden visual: títulos de sección (Label) y campos, tal como se declararon. */
    private final List<Object> layoutOrder = new ArrayList<>();
    private final GridPane grid = new GridPane();
    private final Label banner = new Label();
    private final Button save = new Button("Guardar", Icons.of(Feather.CHECK, 15));
    private final Button cancel = new Button("Cancelar");
    private final VBox root;
    private FormField<?> keyField;
    private int row;
    private int col;

    public FormDialog(Ikon icon, String title, String subtitle) {
        StackPane badge = new StackPane(Icons.of(icon, 20));
        badge.getStyleClass().add("form-badge");
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("dialog-title");
        Label lblSubtitle = new Label(subtitle);
        lblSubtitle.getStyleClass().add("dialog-message");
        lblSubtitle.setWrapText(true);
        VBox titles = new VBox(2, lblTitle, lblSubtitle);
        HBox.setHgrow(titles, Priority.ALWAYS);
        HBox header = new HBox(14, badge, titles);
        header.setAlignment(Pos.CENTER_LEFT);

        grid.setHgap(16);
        grid.setVgap(14);
        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(50);
        c.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c, c);
        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().addAll("form-scroll", "edge-to-edge");
        scroll.setMaxHeight(520);

        banner.getStyleClass().add("form-banner");
        banner.setGraphic(Icons.of(Feather.ALERT_CIRCLE, 15));
        banner.setWrapText(true);
        banner.setMaxWidth(Double.MAX_VALUE);
        banner.setVisible(false);
        banner.setManaged(false);

        cancel.setCancelButton(true);
        cancel.setOnAction(e -> ModalHost.hide());
        save.getStyleClass().add("accent");
        save.setDefaultButton(true);
        HBox footer = new HBox(10, cancel, save);
        footer.setAlignment(Pos.CENTER_RIGHT);

        root = new VBox(18, header, scroll, banner, footer);
        root.getStyleClass().addAll("dialog", "form-dialog");
        root.setMaxSize(660, Region.USE_PREF_SIZE);
        root.setPrefWidth(660);
    }

    // ------------------------------------------------------------------ campos

    /** Texto de una línea; las cadenas vacías se devuelven como {@code null}. */
    public FormField<String> text(String label, String initial) {
        TextField tf = new TextField(initial == null ? "" : initial);
        return add(new FormField<>(label, tf, () -> blankToNull(tf.getText())));
    }

    /** Texto con longitud máxima (como la columna VARCHAR correspondiente). */
    public FormField<String> text(String label, String initial, int maxLength) {
        FormField<String> f = text(label, initial);
        limit((TextField) f.control(), maxLength);
        return f.rule(v -> v.length() > maxLength ? "Máximo " + maxLength + " caracteres." : null);
    }

    /** Texto multilínea. */
    public FormField<String> area(String label, String initial) {
        TextArea ta = new TextArea(initial == null ? "" : initial);
        ta.setWrapText(true);
        ta.setPrefRowCount(3);
        return add(new FormField<>(label, ta, () -> blankToNull(ta.getText()))).wide();
    }

    /** Número entero. */
    public FormField<Integer> integer(String label, Integer initial) {
        TextField tf = new TextField(initial == null ? "" : String.valueOf(initial));
        tf.getStyleClass().add("mono-input");
        FormField<Integer> f = new FormField<>(label, tf, () -> {
            String t = tf.getText().trim();
            try {
                return t.isEmpty() ? null : Integer.valueOf(t);
            } catch (NumberFormatException e) {
                return null;
            }
        });
        f.parseErrorFrom(() -> {
            String t = tf.getText().trim();
            return t.isEmpty() || t.matches("-?\\d{1,9}") ? null : "Introduce un número entero.";
        });
        return add(f);
    }

    /** Importe o decimal; acepta coma o punto como separador decimal. */
    public FormField<BigDecimal> decimal(String label, BigDecimal initial) {
        TextField tf = new TextField(initial == null ? "" : Formatters.decimal(initial.doubleValue(), 2).replace(".", ""));
        tf.getStyleClass().add("mono-input");
        FormField<BigDecimal> f = new FormField<>(label, tf, () -> parseDecimal(tf.getText()));
        f.parseErrorFrom(() -> tf.getText().isBlank() || parseDecimal(tf.getText()) != null
                ? null : "Introduce un importe válido (por ejemplo 1.250,50).");
        return add(f);
    }

    /** Fecha con formato dd/MM/aaaa. */
    public FormField<LocalDate> date(String label, LocalDate initial) {
        DatePicker dp = new DatePicker(initial);
        dp.setPromptText("dd/mm/aaaa");
        dp.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate d) {
                return d == null ? "" : DATE.format(d);
            }

            @Override
            public LocalDate fromString(String s) {
                if (s == null || s.isBlank()) {
                    return null;
                }
                try {
                    return LocalDate.parse(s.trim(), DATE);
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        });
        FormField<LocalDate> f = new FormField<>(label, dp, () -> {
            // Confirma el texto tecleado aunque no se haya pulsado Enter
            String typed = dp.getEditor().getText();
            return typed == null || typed.isBlank() ? null : dp.getConverter().fromString(typed);
        });
        f.parseErrorFrom(() -> {
            String typed = dp.getEditor().getText();
            return typed == null || typed.isBlank() || dp.getConverter().fromString(typed) != null
                    ? null : "Usa el formato dd/mm/aaaa.";
        });
        return add(f);
    }

    /** Selección entre opciones, mostradas con {@code display}. */
    public <T> FormField<T> choice(String label, List<T> options, T initial, Function<T, String> display) {
        return choice(label, options, initial, display, null);
    }

    /**
     * Selección opcional: añade al principio una opción vacía con el texto {@code noneLabel}
     * (p. ej. "Sin jefe") para poder quitar la selección.
     */
    public <T> FormField<T> choice(String label, List<T> options, T initial, Function<T, String> display, String noneLabel) {
        List<T> items = new ArrayList<>();
        if (noneLabel != null) {
            items.add(null);
        }
        items.addAll(options);
        ComboBox<T> combo = new ComboBox<>(FXCollections.observableArrayList(items));
        String empty = noneLabel == null ? "Sin seleccionar" : noneLabel;
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean isEmpty) {
                super.updateItem(item, isEmpty);
                setText(isEmpty ? null : item == null ? empty : display.apply(item));
            }
        });
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(T t) {
                return t == null ? "" : display.apply(t);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean isEmpty) {
                super.updateItem(item, isEmpty);
                setText(isEmpty || item == null ? empty : display.apply(item));
            }
        });
        combo.setValue(initial);
        combo.setVisibleRowCount(10);
        return add(new FormField<>(label, combo, combo::getValue));
    }

    /** Título de sección que ocupa todo el ancho. */
    public FormDialog section(String title) {
        Label l = new Label(title.toUpperCase(Formatters.ES));
        l.getStyleClass().add("form-section");
        layoutOrder.add(l);
        return this;
    }

    /** Campo cuyo error se marca si la capa de aplicación informa de un duplicado. */
    public FormDialog keyField(FormField<?> field) {
        this.keyField = field;
        return this;
    }

    private <V> FormField<V> add(FormField<V> field) {
        fields.add(field);
        layoutOrder.add(field);
        // La colocación definitiva se hace en show(): así wide() puede llamarse después
        return field;
    }

    private void layoutFields() {
        grid.getChildren().clear();
        row = 0;
        col = 0;
        for (Object item : layoutOrder) {
            if (item instanceof Label sectionTitle) {
                if (col != 0) {
                    row++;
                    col = 0;
                }
                grid.add(sectionTitle, 0, row++, 2, 1);
                continue;
            }
            FormField<?> f = (FormField<?>) item;
            int span = f.span();
            if (col + span > 2) {
                row++;
                col = 0;
            }
            grid.add(f.node(), col, row, span, 1);
            col += span;
            if (col >= 2) {
                row++;
                col = 0;
            }
        }
    }

    // ----------------------------------------------------------------- envío

    /**
     * Muestra el formulario. Al guardar valida, ejecuta {@code action} en segundo plano
     * y, si termina bien, cierra, notifica {@code successMessage} y llama a {@code onSaved}.
     */
    public void show(FxTasks.ThrowingRunnable action, Supplier<String> successMessage, Runnable onSaved) {
        layoutFields();
        save.setOnAction(e -> submit(action, successMessage, onSaved));
        ModalHost.show(root);
        Platform.runLater(() -> fields.stream()
                .filter(f -> !f.control().isDisabled())
                .findFirst()
                .ifPresent(f -> f.control().requestFocus()));
    }

    private void submit(FxTasks.ThrowingRunnable action, Supplier<String> successMessage, Runnable onSaved) {
        hideBanner();
        boolean ok = true;
        FormField<?> firstInvalid = null;
        for (FormField<?> f : fields) {
            if (!f.validate()) {
                ok = false;
                if (firstInvalid == null) {
                    firstInvalid = f;
                }
            }
        }
        if (!ok) {
            firstInvalid.control().requestFocus();
            showBanner("Revisa los campos marcados.");
            return;
        }
        setBusy(true);
        FxTasks.runVoid(action, () -> {
            setBusy(false);
            ModalHost.hide();
            Notifier.success("Cambios guardados", successMessage.get());
            onSaved.run();
        }, error -> {
            setBusy(false);
            Throwable cause = FxTasks.unwrap(error);
            if (cause instanceof DuplicateEntityException && keyField != null) {
                keyField.showError("Ya existe un registro con este código.");
                keyField.control().requestFocus();
                showBanner(cause.getMessage());
            } else if (cause instanceof BusinessValidationException || cause instanceof EntityNotFoundException) {
                showBanner(cause.getMessage());
            } else {
                showBanner(FxTasks.friendlyMessage(cause));
            }
        });
    }

    private void setBusy(boolean busy) {
        save.setDisable(busy);
        cancel.setDisable(busy);
        if (busy) {
            RingProgressIndicator ring = new RingProgressIndicator(-1, false);
            ring.setPrefSize(14, 14);
            save.setGraphic(ring);
            save.setText("Guardando…");
        } else {
            save.setGraphic(Icons.of(Feather.CHECK, 15));
            save.setText("Guardar");
        }
    }

    private void showBanner(String message) {
        banner.setText(message);
        banner.setVisible(true);
        banner.setManaged(true);
    }

    private void hideBanner() {
        banner.setVisible(false);
        banner.setManaged(false);
    }

    // ---------------------------------------------------------------- utilidades

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static void limit(TextInputControl input, int max) {
        input.textProperty().addListener((obs, o, n) -> {
            if (n != null && n.length() > max) {
                input.setText(n.substring(0, max));
            }
        });
    }

    /** Interpreta "1.250,50", "1250,5" o "1250.50"; {@code null} si no es un número. */
    static BigDecimal parseDecimal(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim().replace("€", "").replace(" ", "").replace(" ", "");
        if (t.contains(",")) {
            t = t.replace(".", "").replace(',', '.');
        } else if (t.chars().filter(ch -> ch == '.').count() > 1) {
            t = t.replace(".", "");
        }
        DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ROOT);
        df.setParseBigDecimal(true);
        ParsePosition pos = new ParsePosition(0);
        Number n = df.parse(t, pos);
        return n == null || pos.getIndex() != t.length() ? null : (BigDecimal) n;
    }
}
