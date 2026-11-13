package com.jardineria.infrastructure.adapter.in.desktop.views.crud;

import atlantafx.base.controls.CustomTextField;
import com.jardineria.infrastructure.adapter.in.common.Formatters;
import com.jardineria.infrastructure.adapter.in.desktop.components.EmptyState;
import com.jardineria.infrastructure.adapter.in.desktop.components.StatusBadge;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.support.CsvExport;
import com.jardineria.infrastructure.adapter.in.desktop.support.FxTasks;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import com.jardineria.infrastructure.adapter.in.desktop.support.ModalHost;
import com.jardineria.infrastructure.adapter.in.desktop.support.Notifier;
import com.jardineria.infrastructure.adapter.in.desktop.views.BaseView;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import com.jardineria.infrastructure.adapter.in.desktop.support.Keys;

/**
 * Plantilla de módulo CRUD (patrón Template Method): cada módulo concreto solo
 * declara cómo obtener, mostrar, editar y borrar su entidad; esta clase aporta
 * búsqueda sin tildes, filtros, resumen, panel de detalle opcional, menú
 * contextual, atajos (Ctrl+N, Intro, Supr, Ctrl+F), confirmación de borrado
 * y exportación CSV de lo que se está viendo.
 *
 * @param <T> entidad de dominio gestionada
 */
public abstract class CrudModuleView<T> extends BaseView {
    private final String singular;
    private final String plural;

    protected final TableView<T> table = new TableView<>();
    private final ObservableList<T> master = FXCollections.observableArrayList();
    private final FilteredList<T> filtered = new FilteredList<>(master);
    private final CustomTextField search = new CustomTextField();
    /** Búsqueda pedida desde otra vista; se resuelve tras la siguiente carga. */
    private String pendingReveal;
    private final Label count = new Label();
    private final HBox filters = new HBox(10);
    private final VBox summaryHost = new VBox();
    private Predicate<T> extraFilter = t -> true;
    private final Button edit = new Button("Editar", Icons.of(Feather.EDIT_2, 14));
    private final Button delete = new Button("Eliminar", Icons.of(Feather.TRASH_2, 14));

    protected CrudModuleView(Route route, String singular, String plural) {
        super(route, false);
        this.singular = singular;
        this.plural = plural;

        Button nuevo = new Button((femenino() ? "Nueva " : "Nuevo ") + singular, Icons.of(Feather.PLUS, 15));
        nuevo.getStyleClass().add("accent");
        nuevo.setTooltip(new Tooltip("Crear " + articulo() + " " + singular + Keys.text(" (Ctrl+N)")));
        nuevo.setOnAction(e -> createNew());
        Button export = new Button("Exportar", Icons.of(Feather.DOWNLOAD, 15));
        export.setTooltip(new Tooltip("Exporta a CSV los registros visibles"));
        export.setOnAction(e -> exportCsv());
        addActions(export, nuevo);
    }

    // ------------------------------------------------------------- contrato

    /** Consulta bloqueante de los registros (se ejecuta en segundo plano). */
    protected abstract List<T> fetch() throws Exception;

    /** Declara las columnas de {@link #table}. */
    protected abstract void configureColumns();

    /** Texto en el que busca el cuadro de búsqueda. */
    protected abstract String searchableText(T item);

    /** Abre el formulario de alta ({@code existing == null}) o edición. */
    protected abstract void openEditor(T existing);

    /** Borrado bloqueante (se ejecuta en segundo plano). */
    protected abstract void deleteItem(T item) throws Exception;

    /** Descripción breve para confirmaciones y notificaciones ("el cliente «X»"). */
    protected abstract String describe(T item);

    protected abstract List<String> csvHeader();

    protected abstract List<String> csvRow(T item);

    /** Tira de indicadores sobre la tabla (opcional). */
    protected Node buildSummary(List<T> all) {
        return null;
    }

    /** Panel lateral de detalle (opcional); se actualiza con {@link #onSelect}. */
    protected Node buildDetail() {
        return null;
    }

    /** Reacciona al cambio de selección (para el panel de detalle). */
    protected void onSelect(T selected) {
    }

    // ------------------------------------------------------------- construcción

    @Override
    protected Node buildContent() {
        search.setPromptText("Buscar " + plural.toLowerCase(Formatters.ES) + "…");
        search.setLeft(Icons.of(Feather.SEARCH, 15));
        Button clear = new Button(null, Icons.of(Feather.X, 13));
        clear.getStyleClass().addAll("flat", "search-clear");
        clear.setAccessibleText("Limpiar búsqueda");
        clear.visibleProperty().bind(search.textProperty().isNotEmpty());
        clear.setOnAction(e -> search.clear());
        search.setRight(clear);
        search.getStyleClass().add("crud-search");
        search.setAccessibleText("Buscar en " + plural);
        search.setPrefWidth(320);
        search.setMinWidth(170);
        search.textProperty().addListener((obs, o, n) -> applyFilter());

        count.getStyleClass().add("crud-count");
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);

        edit.setOnAction(e -> editSelected());
        delete.getStyleClass().add("danger-outline");
        delete.setOnAction(e -> deleteSelected());
        edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        delete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        edit.setTooltip(new Tooltip("Editar selección (Intro o doble clic)"));
        edit.setAccessibleText("Editar selección");
        delete.setAccessibleText("Eliminar selección");
        delete.setTooltip(new Tooltip(Keys.text("Eliminar selección (Supr)")));

        filters.setAlignment(Pos.CENTER_LEFT);
        // En ventanas estrechas cede el buscador; botones, filtros y contador conservan su ancho
        for (Region r : List.of(count, edit, delete, filters)) {
            r.setMinWidth(Region.USE_PREF_SIZE);
        }
        HBox toolbar = new HBox(10, search, filters, grow, count, edit, delete);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("crud-toolbar");

        configureTable();
        configureColumns();

        StackPane tableCard = new StackPane(table);
        tableCard.getStyleClass().addAll("panel", "table-card");
        HBox.setHgrow(tableCard, Priority.ALWAYS);
        tableCard.setMinWidth(420);

        HBox main = new HBox(16, tableCard);
        Node detail = buildDetail();
        if (detail != null) {
            main.getChildren().add(detail);
        }
        VBox.setVgrow(main, Priority.ALWAYS);

        summaryHost.getStyleClass().add("crud-summary");
        summaryHost.setManaged(false);
        summaryHost.setVisible(false);

        VBox body = new VBox(14, summaryHost, toolbar, main);
        body.getStyleClass().addAll("view-body", "crud-body");
        body.setMinWidth(0);
        main.setMinWidth(0);
        // Modo compacto: en ventanas estrechas los botones muestran solo el ícono
        body.widthProperty().addListener((obs, o, w) -> compact(w.doubleValue() < COMPACT_WIDTH));

        body.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN).match(e)) {
                search.requestFocus();
                search.selectAll();
                e.consume();
            }
        });
        return body;
    }

    private static final double COMPACT_WIDTH = 960;

    private void compact(boolean on) {
        ContentDisplay display = on ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT;
        edit.setContentDisplay(display);
        delete.setContentDisplay(display);
        count.setVisible(!on);
        count.setManaged(!on);
    }

    private void configureTable() {
        SortedList<T> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.getStyleClass().addAll("crud-table", "striped");
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new EmptyState(Feather.INBOX, "Sin resultados",
                "No hay " + plural.toLowerCase(Formatters.ES) + " que coincidan con la búsqueda."));
        table.setAccessibleText("Tabla de " + plural);
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> onSelect(n));

        table.setRowFactory(tv -> {
            TableRow<T> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !row.isEmpty()) {
                    openEditor(row.getItem());
                }
            });
            row.contextMenuProperty().bind(Bindings.when(row.emptyProperty())
                    .then((ContextMenu) null).otherwise(rowMenu(row)));
            return row;
        });
        table.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                editSelected();
                e.consume();
            } else if (e.getCode() == KeyCode.DELETE || (Keys.MAC && e.getCode() == KeyCode.BACK_SPACE)) {
                // En los teclados Mac la tecla «delete» envía BACK_SPACE.
                deleteSelected();
                e.consume();
            }
        });
        count.textProperty().bind(Bindings.createStringBinding(() -> filtered.size() == master.size()
                        ? Formatters.integer(master.size()) + " " + (master.size() == 1 ? singular : plural.toLowerCase(Formatters.ES))
                        : Formatters.integer(filtered.size()) + " de " + Formatters.integer(master.size()),
                filtered, master));
    }

    private ContextMenu rowMenu(TableRow<T> row) {
        MenuItem editar = new MenuItem("Editar", Icons.of(Feather.EDIT_2, 14));
        editar.setOnAction(e -> openEditor(row.getItem()));
        MenuItem copiar = new MenuItem("Copiar fila", Icons.of(Feather.COPY, 14));
        copiar.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(String.join("\t", csvRow(row.getItem())));
            Clipboard.getSystemClipboard().setContent(content);
            Notifier.info("Copiado", "La fila está en el portapapeles.");
        });
        MenuItem eliminar = new MenuItem("Eliminar", Icons.of(Feather.TRASH_2, 14));
        eliminar.getStyleClass().add("danger-item");
        eliminar.setOnAction(e -> confirmDelete(row.getItem()));
        return new ContextMenu(editar, copiar, new SeparatorMenuItem(), eliminar);
    }

    // ------------------------------------------------------------- datos

    @Override
    public void refresh() {
        T selected = table.getSelectionModel().getSelectedItem();
        load(this::fetch, items -> {
            master.setAll(items);
            Node summary = buildSummary(items);
            summaryHost.getChildren().setAll(summary == null ? List.of() : List.of(summary));
            summaryHost.setVisible(summary != null);
            summaryHost.setManaged(summary != null);
            reselect(selected);
            afterLoad(items);
            if (pendingReveal != null) {
                selectRevealed(pendingReveal);
                pendingReveal = null;
            }
        });
    }

    /** Filtra por {@code query} y selecciona la mejor coincidencia (vacío limpia la búsqueda). */
    @Override
    public void reveal(String query) {
        String q = query == null ? "" : query.trim();
        search.setText(q);
        if (q.isEmpty()) {
            pendingReveal = null;
            return;
        }
        pendingReveal = q;
        if (!master.isEmpty()) {
            selectRevealed(q);
        }
    }

    /**
     * Prefiere el elemento cuyo texto buscable empieza por la consulta como palabra completa
     * ({@code #30} no debe elegir el pedido 300), luego el que empieza por ella y, si no, el primero.
     */
    private void selectRevealed(String query) {
        String q = Formatters.fold(query);
        List<T> visible = table.getItems();
        if (visible.isEmpty()) {
            return;
        }
        T best = visible.stream().filter(t -> Formatters.fold(searchableText(t)).startsWith(q + " ")).findFirst()
                .or(() -> visible.stream().filter(t -> Formatters.fold(searchableText(t)).startsWith(q)).findFirst())
                .orElse(visible.get(0));
        table.getSelectionModel().select(best);
        table.scrollTo(best);
    }

    /** Gancho tras cada recarga (p. ej. para rellenar opciones de filtros). */
    protected void afterLoad(List<T> items) {
    }

    private void reselect(T previous) {
        if (previous == null) {
            onSelect(null);
            return;
        }
        table.getItems().stream().filter(t -> sameEntity(t, previous)).findFirst().ifPresentOrElse(t -> {
            table.getSelectionModel().select(t);
            table.scrollTo(t);
        }, () -> onSelect(null));
    }

    /** Identidad de la entidad (por defecto equals, que en el dominio compara la clave). */
    protected boolean sameEntity(T a, T b) {
        return Objects.equals(a, b);
    }

    @Override
    public void createNew() {
        if (!ModalHost.isShowing()) {
            openEditor(null);
        }
    }

    private void editSelected() {
        T item = table.getSelectionModel().getSelectedItem();
        if (item != null) {
            openEditor(item);
        }
    }

    private void deleteSelected() {
        T item = table.getSelectionModel().getSelectedItem();
        if (item != null) {
            confirmDelete(item);
        }
    }

    private void confirmDelete(T item) {
        ModalHost.confirmDanger("¿Eliminar " + describe(item) + "?",
                "Esta acción borra el registro de la base de datos y no se puede deshacer.",
                "Eliminar", () -> FxTasks.runVoid(() -> deleteItem(item), () -> {
                    Notifier.success("Registro eliminado", "Se ha eliminado " + describe(item) + ".");
                    refresh();
                }, error -> Notifier.error("No se pudo eliminar", FxTasks.friendlyMessage(FxTasks.unwrap(error)))));
    }

    /** Llamar tras guardar en un formulario: recarga y notifica. */
    protected void saved() {
        refresh();
    }

    // ------------------------------------------------------------- filtros

    /** Añade un control de filtro a la barra de herramientas. */
    protected void addFilter(Node control) {
        if (control instanceof Region r) {
            r.setMinWidth(Region.USE_PREF_SIZE);
        }
        filters.getChildren().add(control);
    }

    /** Predicado adicional combinado con la búsqueda de texto. */
    protected void setExtraFilter(Predicate<T> predicate) {
        this.extraFilter = predicate == null ? t -> true : predicate;
        applyFilter();
    }

    private void applyFilter() {
        String q = Formatters.fold(search.getText());
        filtered.setPredicate(t -> extraFilter.test(t) && (q.isEmpty() || Formatters.fold(searchableText(t)).contains(q)));
    }


    protected List<T> visibleItems() {
        return List.copyOf(table.getItems());
    }

    /**
     * Regla de formulario para altas: avisa en línea si la clave ya existe en los datos
     * cargados (la capa de aplicación sigue validándolo al guardar).
     */
    protected <K> Function<K, String> uniqueKey(boolean nuevo, Function<T, K> key, String singular) {
        return v -> nuevo && master.stream().anyMatch(t -> v instanceof String s
                ? s.trim().equalsIgnoreCase(String.valueOf(key.apply(t)))
                : Objects.equals(key.apply(t), v))
                ? "Ya existe " + singular + " con esa clave." : null;
    }

    protected List<T> allItems() {
        return List.copyOf(master);
    }

    // ------------------------------------------------------------- columnas

    protected <V> TableColumn<T, V> column(String title, Function<T, V> getter, double width) {
        TableColumn<T, V> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue() == null ? null : getter.apply(cd.getValue())));
        col.setPrefWidth(width);
        col.setMinWidth(Math.min(width, 70));
        table.getColumns().add(col);
        return col;
    }

    /** Cifras, fechas e insignias no se encogen: si falta espacio ceden las columnas de texto. */
    private static <S, V> TableColumn<S, V> fixed(TableColumn<S, V> col) {
        col.setMinWidth(col.getPrefWidth());
        return col;
    }

    protected TableColumn<T, String> textColumn(String title, Function<T, String> getter, double width) {
        TableColumn<T, String> col = column(title, getter, width);
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : Formatters.orDash(item));
                getStyleClass().remove("cell-muted");
                if (!empty && (item == null || item.isBlank())) {
                    getStyleClass().add("cell-muted");
                }
            }
        });
        return col;
    }

    /** Columna de código/clave en monoespaciada. */
    protected <V> TableColumn<T, V> codeColumn(String title, Function<T, V> getter, double width) {
        TableColumn<T, V> col = column(title, getter, width);
        col.getStyleClass().add("col-code");
        return col;
    }

    protected TableColumn<T, BigDecimal> moneyColumn(String title, Function<T, BigDecimal> getter, double width) {
        TableColumn<T, BigDecimal> col = fixed(column(title, getter, width));
        col.getStyleClass().add("col-number");
        col.setComparator(Comparator.nullsFirst(Comparator.naturalOrder()));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? "—" : Formatters.currency(item));
            }
        });
        return col;
    }

    protected TableColumn<T, Integer> numberColumn(String title, Function<T, Integer> getter, double width) {
        TableColumn<T, Integer> col = fixed(column(title, getter, width));
        col.getStyleClass().add("col-number");
        col.setComparator(Comparator.nullsFirst(Comparator.naturalOrder()));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? "—" : Formatters.integer(item));
            }
        });
        return col;
    }

    protected TableColumn<T, LocalDate> dateColumn(String title, Function<T, LocalDate> getter, double width) {
        TableColumn<T, LocalDate> col = fixed(column(title, getter, width));
        col.setComparator(Comparator.nullsFirst(Comparator.naturalOrder()));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : Formatters.date(item));
            }
        });
        return col;
    }

    protected TableColumn<T, String> badgeColumn(String title, Function<T, String> getter, double width) {
        TableColumn<T, String> col = fixed(column(title, getter, width));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : new StatusBadge(item));
            }
        });
        return col;
    }

    // ------------------------------------------------------------- exportación

    private void exportCsv() {
        List<List<String>> rows = visibleItems().stream().map(this::csvRow).toList();
        if (rows.isEmpty()) {
            Notifier.info("Nada que exportar", "No hay registros visibles con los filtros actuales.");
            return;
        }
        CsvExport.saveWithDialog(getScene() == null ? null : getScene().getWindow(),
                Formatters.fold(plural).replace(' ', '-') + "-jardineria", csvHeader(), rows);
    }

    // ------------------------------------------------------------- utilidades

    protected String singular() {
        return singular;
    }

    private boolean femenino() {
        return singular.endsWith("a");
    }

    private String articulo() {
        return femenino() ? "una" : "un";
    }

    protected static String capitalize(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    protected static String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
