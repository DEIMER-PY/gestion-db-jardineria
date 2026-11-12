package com.jardineria.infrastructure.adapter.in.desktop.views.patterns;

import com.jardineria.domain.model.Cliente;
import com.jardineria.domain.model.Pago;
import com.jardineria.domain.pattern.adapter.PayPalAdapter;
import com.jardineria.domain.pattern.adapter.PaymentGatewayPort;
import com.jardineria.domain.pattern.adapter.StripeAdapter;
import com.jardineria.infrastructure.adapter.in.common.Formatters;
import com.jardineria.infrastructure.adapter.in.desktop.components.KpiCard;
import com.jardineria.infrastructure.adapter.in.desktop.components.form.FormDialog;
import com.jardineria.infrastructure.adapter.in.desktop.support.Icons;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Adapter: dos SDK de pago incompatibles detrás de un mismo puerto del dominio. */
final class AdapterDemo implements PatternDemo {

    @Override
    public String name() {
        return "Adapter";
    }

    @Override
    public Ikon icon() {
        return Feather.SHUFFLE;
    }

    @Override
    public KpiCard.Tone tone() {
        return KpiCard.Tone.SKY;
    }

    @Override
    public String tagline() {
        return "Hace compatibles interfaces que no lo son.";
    }

    @Override
    public String intent() {
        return "PayPal espera céntimos enteros y un correo; Stripe espera euros en coma flotante y un id de cliente "
                + "propio. El dominio solo conoce PaymentGatewayPort.procesarCobro(cliente, transacción, importe): "
                + "cada adaptador traduce esa llamada al SDK externo y convierte la respuesta en una entidad Pago.";
    }

    @Override
    public List<String> participants() {
        return List.of("PaymentGatewayPort", "PayPalAdapter", "PayPalExternalSdk", "StripeAdapter", "StripeExternalSdk");
    }

    @Override
    public String usage() {
        return "La fachada elige la pasarela según la forma de pago del checkout y registra el Pago resultante.";
    }

    @Override
    public String image() {
        return "adapter.jpg";
    }

    @Override
    public Node build(PatternData data) {
        ToggleGroup gateways = new ToggleGroup();
        ToggleButton paypal = new ToggleButton("PayPal");
        ToggleButton stripe = new ToggleButton("Stripe (tarjeta)");
        paypal.setToggleGroup(gateways);
        stripe.setToggleGroup(gateways);
        paypal.setSelected(true);
        gateways.selectedToggleProperty().addListener((o, old, sel) -> {
            if (sel == null) {
                old.setSelected(true);
            }
        });
        HBox segmented = new HBox(paypal, stripe);
        segmented.getStyleClass().add("segmented");

        ComboBox<Cliente> cliente = DemoUi.clientPicker(data.clientes());
        TextField importe = new TextField("149,90");
        importe.setPrefWidth(120);
        importe.getStyleClass().add("mono-input");
        importe.setAccessibleText("Importe en euros");
        Button cobrar = new Button("Procesar cobro", Icons.of(Feather.ZAP, 14));
        cobrar.getStyleClass().add("accent");
        cobrar.setDefaultButton(false);

        HBox flow = new HBox(10);
        flow.getStyleClass().add("adapter-flow");
        flow.setAlignment(Pos.CENTER_LEFT);
        Label error = new Label();
        error.getStyleClass().add("demo-error");
        error.setManaged(false);
        error.setVisible(false);

        Runnable run = () -> {
            BigDecimal amount = FormDialog.parseDecimal(importe.getText());
            if (amount == null || amount.signum() <= 0) {
                error.setText("Escribe un importe positivo, por ejemplo 149,90.");
                error.setManaged(true);
                error.setVisible(true);
                return;
            }
            amount = amount.setScale(2, RoundingMode.HALF_UP);
            error.setManaged(false);
            error.setVisible(false);
            Cliente c = cliente.getValue();
            if (c == null) {
                return;
            }
            boolean usePaypal = paypal.isSelected();
            PaymentGatewayPort port = usePaypal ? new PayPalAdapter(null) : new StripeAdapter(null);
            String tx = "DEMO-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
            Pago pago = port.procesarCobro(c.getCodigoCliente(), tx, amount);

            String translated = usePaypal
                    ? "executeCapture(\"" + tx + "\", " + amount.movePointRight(2).longValue() + "L, \"cliente"
                    + c.getCodigoCliente() + "@jardineria.com\")"
                    : "createCharge(\"cus_jardin_" + c.getCodigoCliente() + "\", " + amount.doubleValue() + ", …)";
            flow.getChildren().setAll(
                    step("Dominio", "PaymentGatewayPort", "procesarCobro(" + c.getCodigoCliente() + ", \"" + tx
                            + "\", " + amount.toPlainString() + ")", "jade"),
                    arrow(),
                    step("Adaptador", port.getClass().getSimpleName(), translated, "sky"),
                    arrow(),
                    step("Resultado", "Pago", pago.getFormaPago() + " · " + pago.getIdTransaccion() + " · "
                            + Formatters.currency(pago.getTotal()) + " · " + Formatters.date(pago.getFechaPago()), "lime"));
        };
        cobrar.setOnAction(e -> run.run());
        importe.setOnAction(e -> run.run());
        run.run();

        return new VBox(16,
                DemoUi.controls(DemoUi.field("Pasarela", segmented), DemoUi.field("Cliente", cliente),
                        DemoUi.field("Importe (€)", importe), cobrar),
                error,
                DemoUi.output(DemoUi.title("Recorrido de la llamada"), flow,
                        DemoUi.note(Feather.INFO, "Los SDK son simulados y el pago no se guarda: la demo solo muestra la traducción de interfaces.")));
    }

    private static Node step(String stage, String cls, String call, String tone) {
        Label lblStage = DemoUi.tag(stage, tone);
        Label lblClass = new Label(cls);
        lblClass.getStyleClass().add("adapter-class");
        Label lblCall = new Label(call);
        lblCall.getStyleClass().addAll("adapter-call", "mono");
        lblCall.setWrapText(true);
        lblCall.setMinHeight(Region.USE_PREF_SIZE);
        VBox box = new VBox(8, lblStage, lblClass, lblCall);
        box.getStyleClass().add("adapter-step");
        box.setMinWidth(150);
        box.setPrefWidth(240);
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private static Node arrow() {
        Node icon = Icons.of(Feather.ARROW_RIGHT, 18);
        icon.getStyleClass().add("adapter-arrow");
        return icon;
    }
}
