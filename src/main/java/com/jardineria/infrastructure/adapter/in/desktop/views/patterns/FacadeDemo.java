package com.jardineria.infrastructure.adapter.in.desktop.views.patterns;

import atlantafx.base.controls.ToggleSwitch;
import com.jardineria.application.facade.CheckoutRequest;
import com.jardineria.application.facade.CheckoutResult;
import com.jardineria.application.facade.JardineriaBusinessFacade;
import com.jardineria.domain.model.Cliente;
import com.jardineria.domain.model.Producto;
import com.jardineria.domain.pattern.decorator.BasePriceCalculator;
import com.jardineria.domain.pattern.decorator.BulkDiscountDecorator;
import com.jardineria.domain.pattern.decorator.PriceCalculator;
import com.jardineria.domain.pattern.decorator.SeasonalPromoDecorator;
import com.jardineria.domain.pattern.decorator.VatTaxDecorator;
import com.jardineria.infrastructure.adapter.in.common.Formatters;
import com.jardineria.infrastructure.adapter.in.desktop.components.EmptyState;
import com.jardineria.infrastructure.adapter.in.desktop.components.KpiCard;
import com.jardineria.infrastructure.adapter.in.desktop.support.FxTasks;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import com.jardineria.infrastructure.adapter.in.desktop.support.ModalHost;
import com.jardineria.infrastructure.adapter.in.desktop.support.Notifier;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Facade: una sola llamada orquesta cliente, stock, precios, pedido, líneas y cobro. */
final class FacadeDemo implements PatternDemo {

    private static final List<String> STEPS = List.of(
            "Valida que el cliente existe",
            "Comprueba el stock de cada producto",
            "Calcula el total con la cadena Decorator",
            "Verifica el límite de crédito del cliente",
            "Crea el pedido en estado Pendiente",
            "Inserta las líneas y descuenta el stock",
            "Cobra con el Adapter y registra el pago");

    @Override
    public String name() {
        return "Facade";
    }

    @Override
    public Ikon icon() {
        return Feather.BOX;
    }

    @Override
    public KpiCard.Tone tone() {
        return KpiCard.Tone.SKY;
    }

    @Override
    public String tagline() {
        return "Una interfaz sencilla delante de un subsistema complejo.";
    }

    @Override
    public String intent() {
        return "Una compra toca cinco casos de uso (clientes, productos, pedidos, líneas y pagos) y dos patrones "
                + "(Decorator y Adapter). JardineriaBusinessFacade.procesarCheckoutCompleto() oculta esa coreografía "
                + "tras un único método que recibe un CheckoutRequest y devuelve un CheckoutResult.";
    }

    @Override
    public List<String> participants() {
        return List.of("JardineriaBusinessFacade", "CheckoutRequest", "CheckoutResult",
                "ClienteUseCase", "ProductoUseCase", "PedidoUseCase", "DetallePedidoUseCase", "PagoUseCase");
    }

    @Override
    public String usage() {
        return "Capa de aplicación: el cliente de la fachada no conoce repositorios ni el orden de las operaciones.";
    }

    @Override
    public String image() {
        return "facade.jpg";
    }

    @Override
    public Node build(PatternData data) {
        JardineriaBusinessFacade facade = data.facade();
        if (facade == null) {
            return new EmptyState(Feather.BOX, "Fachada no disponible",
                    "El contexto de la aplicación no ha registrado JardineriaBusinessFacade.");
        }
        ComboBox<Cliente> cliente = DemoUi.clientPicker(data.clientes());
        List<Producto> conStock = data.productos().stream()
                .filter(p -> p.getCantidadEnStock() != null && p.getCantidadEnStock() > 0).toList();
        ComboBox<Producto> producto = DemoUi.productPicker(conStock);
        data.firstOfGama("Ornamentales").ifPresent(producto::setValue);
        Spinner<Integer> cantidad = new Spinner<>(1, 500, 2);
        cantidad.setEditable(true);
        cantidad.setPrefWidth(100);
        cantidad.setAccessibleText("Unidades");
        ComboBox<String> metodo = DemoUi.picker(List.of("PayPal", "Tarjeta"), s -> s, "Forma de pago");
        ToggleSwitch temporada = new ToggleSwitch("Promoción de temporada");

        Label preview = new Label();
        preview.getStyleClass().add("demo-total");
        Label stockInfo = new Label();
        stockInfo.getStyleClass().add("demo-metric-label");

        VBox steps = new VBox(4);
        steps.getStyleClass().add("facade-steps");
        renderSteps(steps, -1, false);
        VBox result = new VBox(8);
        result.getStyleClass().add("facade-result");

        Runnable recalc = () -> {
            Producto p = producto.getValue();
            if (p == null || p.getPrecioVenta() == null) {
                preview.setText("—");
                return;
            }
            PriceCalculator calc = new BulkDiscountDecorator(new BasePriceCalculator());
            if (temporada.isSelected()) {
                calc = new SeasonalPromoDecorator(calc);
            }
            calc = new VatTaxDecorator(calc);
            preview.setText(Formatters.currency(calc.calcularTotal(p.getPrecioVenta(), cantidad.getValue())));
            stockInfo.setText("Total previsto · stock disponible: " + p.getCantidadEnStock() + " uds");
        };
        producto.valueProperty().addListener((o, a, b) -> recalc.run());
        cantidad.valueProperty().addListener((o, a, b) -> recalc.run());
        temporada.selectedProperty().addListener((o, a, b) -> recalc.run());
        recalc.run();

        Button execute = new Button("Ejecutar checkout", Icons.of(Feather.PLAY, 14));
        execute.getStyleClass().add("accent");
        execute.setOnAction(e -> {
            Cliente c = cliente.getValue();
            Producto p = producto.getValue();
            if (c == null || p == null) {
                return;
            }
            CheckoutRequest request = new CheckoutRequest(c.getCodigoCliente(),
                    List.of(new CheckoutRequest.ItemCheckout(p.getCodigoProducto(), cantidad.getValue(), p.getPrecioVenta())),
                    metodo.getValue(), null, temporada.isSelected());
            confirm(c, p, cantidad.getValue(), preview.getText(), () -> {
                execute.setDisable(true);
                renderSteps(steps, -1, true);
                FxTasks.run(() -> facade.procesarCheckoutCompleto(request), res -> {
                    execute.setDisable(false);
                    if (data.productLookup() != null) {
                        data.productLookup().invalidateCache(p.getCodigoProducto());
                    }
                    showResult(res, steps, result);
                }, err -> {
                    execute.setDisable(false);
                    renderSteps(steps, 0, false);
                    Notifier.error("Checkout fallido", FxTasks.friendlyMessage(FxTasks.unwrap(err)));
                });
            });
        });

        VBox previewBox = new VBox(2, preview, stockInfo);
        HBox actionRow = new HBox(16, previewBox, DemoUi.grow(), execute);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        VBox stepsBox = new VBox(10, DemoUi.title("procesarCheckoutCompleto(request)"), steps);
        stepsBox.setMinWidth(300);
        HBox.setHgrow(stepsBox, Priority.ALWAYS);
        VBox resultBox = new VBox(10, DemoUi.title("CheckoutResult"), result);
        resultBox.setMinWidth(260);
        HBox.setHgrow(resultBox, Priority.ALWAYS);
        resultBox.setMaxWidth(Double.MAX_VALUE);
        result.getChildren().setAll(DemoUi.note(Feather.INFO,
                "Ejecutar el checkout crea un pedido, sus líneas y un pago reales en MySQL: podrás verlos en Pedidos, Pagos y phpMyAdmin."));

        return new VBox(16,
                DemoUi.controls(DemoUi.field("Cliente", cliente), DemoUi.field("Producto", producto),
                        DemoUi.field("Unidades", cantidad), DemoUi.field("Forma de pago", metodo), temporada),
                actionRow,
                DemoUi.output(new HBox(24, stepsBox, resultBox)));
    }

    private static void confirm(Cliente c, Producto p, int uds, String total, Runnable onConfirm) {
        StackPane badge = new StackPane(Icons.of(Feather.SHOPPING_CART, 20));
        badge.getStyleClass().add("facade-confirm-badge");
        Label title = new Label("¿Registrar esta compra en la base de datos?");
        title.getStyleClass().add("dialog-title");
        title.setWrapText(true);
        Label message = new Label(uds + " × " + p.getNombre() + " para " + c.getNombreCliente() + " por " + total
                + ". Se creará un pedido nuevo, se descontará el stock y se registrará el pago.");
        message.getStyleClass().add("dialog-message");
        message.setWrapText(true);
        Button cancel = new Button("Cancelar");
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> ModalHost.hide());
        Button ok = new Button("Registrar compra", Icons.of(Feather.CHECK, 14));
        ok.getStyleClass().add("accent");
        ok.setDefaultButton(true);
        ok.setOnAction(e -> {
            ModalHost.hide();
            onConfirm.run();
        });
        HBox buttons = new HBox(10, cancel, ok);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        VBox box = new VBox(14, badge, title, message, buttons);
        box.getStyleClass().addAll("dialog", "dialog-confirm");
        box.setMaxSize(460, VBox.USE_PREF_SIZE);
        ModalHost.show(box);
        ok.requestFocus();
    }

    private static void showResult(CheckoutResult res, VBox steps, VBox result) {
        renderSteps(steps, res.exito() ? STEPS.size() : 0, false);
        List<Node> rows = new ArrayList<>();
        rows.add(DemoUi.tag(res.exito() ? "Éxito" : "Rechazado", res.exito() ? "jade" : "coral"));
        Label msg = new Label(res.mensaje());
        msg.getStyleClass().add("facade-message");
        msg.setWrapText(true);
        msg.setMinHeight(Region.USE_PREF_SIZE);
        rows.add(msg);
        if (res.pedido() != null) {
            rows.add(DemoUi.row("Pedido creado", "#" + res.pedido().getCodigoPedido()));
        }
        if (res.pago() != null) {
            rows.add(DemoUi.row("Pago", res.pago().getFormaPago() + " · " + res.pago().getIdTransaccion()));
        }
        if (res.totalFacturado() != null && res.totalFacturado().compareTo(BigDecimal.ZERO) > 0) {
            rows.add(DemoUi.row("Total facturado", Formatters.currency(res.totalFacturado()), "demo-row-strong"));
        }
        res.desglosePrecios().stream()
                .filter(line -> !line.startsWith("---"))
                .forEach(line -> {
                    Label l = new Label(line);
                    l.getStyleClass().addAll("facade-breakdown", "mono");
                    l.setWrapText(true);
                    rows.add(l);
                });
        result.getChildren().setAll(rows);
        if (res.exito()) {
            Notifier.success("Checkout completado", "Pedido #" + res.pedido().getCodigoPedido() + " registrado con su pago.");
        } else {
            Notifier.warning("La fachada rechazó la compra", res.mensaje());
        }
    }

    /**
     * Pinta los pasos de la fachada.
     *
     * @param done    número de pasos completados (-1 = estado inicial, 0 = fallo sin detalle)
     * @param running si la operación está en curso
     */
    private static void renderSteps(VBox steps, int done, boolean running) {
        steps.getChildren().clear();
        for (int i = 0; i < STEPS.size(); i++) {
            String state;
            Ikon icon;
            if (running) {
                state = "running";
                icon = Feather.LOADER;
            } else if (done < 0) {
                state = "idle";
                icon = Feather.CIRCLE;
            } else if (i < done) {
                state = "done";
                icon = Feather.CHECK_CIRCLE;
            } else {
                state = "skipped";
                icon = Feather.MINUS_CIRCLE;
            }
            Label num = new Label(String.valueOf(i + 1));
            num.getStyleClass().addAll("facade-step-num", "mono");
            Label text = new Label(STEPS.get(i));
            text.getStyleClass().add("facade-step-text");
            text.setWrapText(true);
            HBox.setHgrow(text, Priority.ALWAYS);
            text.setMaxWidth(Double.MAX_VALUE);
            HBox row = new HBox(10, Icons.of(icon, 15), num, text);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().addAll("facade-step", "step-" + state);
            steps.getChildren().add(row);
        }
    }
}
