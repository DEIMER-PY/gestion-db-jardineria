package com.jardineria.infrastructure.adapter.in.desktop.components.form;

import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Campo de formulario tipado: etiqueta, control, mensaje de error en línea y
 * reglas de validación. El valor se obtiene ya convertido a su tipo de dominio.
 *
 * @param <V> tipo del valor (String, Integer, BigDecimal, LocalDate, entidad...)
 */
public final class FormField<V> {
    private final String label;
    private final Control control;
    private final Supplier<V> reader;
    private final Label error = new Label();
    private final VBox node;
    private final List<Function<V, String>> rules = new ArrayList<>();
    private Supplier<String> parseError = () -> null;
    private boolean required;
    private int span = 1;

    FormField(String label, Control control, Supplier<V> reader) {
        this.label = label;
        this.control = control;
        this.reader = reader;
        Label caption = new Label(label);
        caption.getStyleClass().add("form-label");
        caption.setLabelFor(control);
        caption.setMnemonicParsing(false);
        error.getStyleClass().add("form-error");
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);
        control.setMaxWidth(Double.MAX_VALUE);
        control.setAccessibleText(label);
        node = new VBox(6, caption, control, error);
        node.getStyleClass().add("form-field");
    }

    /** Marca el campo como obligatorio (añade "*" a la etiqueta). */
    public FormField<V> required() {
        required = true;
        ((Label) node.getChildren().get(0)).setText(label + " *");
        control.setAccessibleText(label + ", obligatorio");
        return this;
    }

    /** Regla adicional: devuelve un mensaje si el valor no es válido, o {@code null}. */
    public FormField<V> rule(Function<V, String> rule) {
        rules.add(rule);
        return this;
    }

    /** Ocupa las dos columnas del formulario. */
    public FormField<V> wide() {
        span = 2;
        return this;
    }

    /** Deshabilita la edición (p. ej. claves primarias al editar). */
    public FormField<V> locked(boolean locked) {
        control.setDisable(locked);
        return this;
    }

    void parseErrorFrom(Supplier<String> supplier) {
        this.parseError = supplier;
    }

    /** Valida y muestra el error en línea. @return {@code true} si es válido */
    public boolean validate() {
        String message = parseError.get();
        V value = message == null ? reader.get() : null;
        if (message == null && required && isEmpty(value)) {
            message = "Este campo es obligatorio.";
        }
        if (message == null && !isEmpty(value)) {
            for (Function<V, String> rule : rules) {
                message = rule.apply(value);
                if (message != null) {
                    break;
                }
            }
        }
        showError(message);
        return message == null;
    }

    /** Muestra un error externo (p. ej. devuelto por la capa de aplicación). */
    public void showError(String message) {
        boolean has = message != null;
        error.setText(has ? message : "");
        error.setVisible(has);
        error.setManaged(has);
        control.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("danger"), has);
    }

    private static boolean isEmpty(Object value) {
        return value == null || (value instanceof String s && s.isBlank());
    }

    public V value() {
        return reader.get();
    }

    public Optional<V> optional() {
        return Optional.ofNullable(reader.get());
    }

    public Control control() {
        return control;
    }

    public String label() {
        return label;
    }

    Node node() {
        return node;
    }

    int span() {
        return span;
    }
}
