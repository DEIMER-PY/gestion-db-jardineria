package com.jardineria.infrastructure.adapter.in.desktop;

import com.jardineria.application.port.in.ClienteUseCase;
import com.jardineria.domain.exception.EntityNotFoundException;
import com.jardineria.domain.model.Cliente;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.shell.AppShell;
import com.jardineria.infrastructure.adapter.in.desktop.support.ModalHost;
import com.jardineria.infrastructure.adapter.in.desktop.theme.ThemeManager;
import com.jardineria.infrastructure.config.ApplicationContext;
import com.jardineria.infrastructure.config.DatabaseConfig;
import com.jardineria.infrastructure.config.DatabaseConnectionManager;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * CRUD de extremo a extremo a través de la GUI contra el MySQL real (Docker):
 * alta de un cliente rellenando el formulario, edición desde la tabla y borrado con
 * confirmación. Cada paso se verifica leyendo de nuevo la base de datos por JDBC.
 * Se omite si MySQL no está disponible.
 */
@ExtendWith(ApplicationExtension.class)
class DesktopCrudIntegrationTest {
    private static final String NOMBRE = "Vivero Prueba E2E";

    private final ClienteUseCase clientes = ApplicationContext.getInstance().getClienteService();
    private AppShell shell;
    private Scene scene;
    private Integer codigoCreado;

    @BeforeAll
    static void requiereBaseDeDatos() {
        assumeTrue(DatabaseConnectionManager.isAvailable(3), "MySQL no disponible: se omite el CRUD de extremo a extremo");
    }

    @Start
    void start(Stage stage) {
        ApplicationContext ctx = ApplicationContext.getInstance();
        DesktopServices services = new DesktopServices(
                ctx.getDashboardService(), ctx.getAnalyticsService(), ctx.getClienteService(),
                ctx.getPedidoService(), ctx.getDetallePedidoService(), ctx.getPagoService(),
                ctx.getProductoService(), ctx.getGamaProductoService(), ctx.getOficinaService(),
                ctx.getEmpleadoService(),
                new DesktopServices.ConnectionProbe() {
                    @Override
                    public boolean isAvailable() {
                        return true;
                    }

                    @Override
                    public String describe() {
                        return DatabaseConfig.getHost() + ":" + DatabaseConfig.getPort();
                    }
                });
        shell = new AppShell(services);
        scene = new Scene(shell, 1440, 900);
        ThemeManager.install(scene);
        stage.setScene(scene);
        stage.show();
    }

    @AfterEach
    void limpiar() {
        if (codigoCreado != null && existe(codigoCreado)) {
            clientes.eliminarCliente(codigoCreado);
        }
    }

    @Test
    @DisplayName("Alta, edición y borrado de un cliente desde la GUI quedan persistidos en MySQL")
    void crudDeClienteDesdeLaInterfaz() throws Exception {
        WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(Route.CLIENTES)).get();
        FxTestSupport.settle(1500);

        // --- Alta
        WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().currentView().createNew()).get();
        FxTestSupport.settle(1200);
        assertThat(ModalHost.isShowing()).isTrue();
        codigoCreado = Integer.valueOf(field("Código").getText().trim());
        assertThat(existe(codigoCreado)).as("el código sugerido debe estar libre").isFalse();
        fill("Nombre del cliente", NOMBRE);
        fill("Teléfono", "600111222");
        fill("Fax", "600111223");
        fill("Dirección", "Calle del Olivo 12");
        fill("Ciudad", "Madrid");
        fill("País", "Spain");
        fill("Límite de crédito (€)", "1.500,50");
        save();

        Cliente creado = clientes.obtenerPorId(codigoCreado);
        assertThat(creado.getNombreCliente()).isEqualTo(NOMBRE);
        assertThat(creado.getCiudad()).isEqualTo("Madrid");
        assertThat(creado.getLimiteCredito()).isEqualByComparingTo(new BigDecimal("1500.50"));

        // --- Edición desde la tabla
        selectRow(codigoCreado);
        fire(button("Editar selección"));
        FxTestSupport.settle(1200);
        assertThat(field("Código").isEditable() && !field("Código").isDisabled()).as("la clave no se edita").isFalse();
        fill("Ciudad", "Sevilla");
        save();
        assertThat(clientes.obtenerPorId(codigoCreado).getCiudad()).isEqualTo("Sevilla");

        // --- Borrado con confirmación
        selectRow(codigoCreado);
        fire(button("Eliminar selección"));
        FxTestSupport.settle(600);
        Button confirmar = (Button) scene.getRoot().lookupAll(".dialog-confirm .button").stream()
                .filter(n -> n.getStyleClass().contains("danger")).findFirst().orElseThrow();
        fire(confirmar);
        FxTestSupport.settle(1500);
        assertThat(existe(codigoCreado)).isFalse();
    }

    // ------------------------------------------------------------------ helpers

    private boolean existe(int codigo) {
        try {
            clientes.obtenerPorId(codigo);
            return true;
        } catch (EntityNotFoundException e) {
            return false;
        }
    }

    /** Localiza un campo del formulario abierto por su texto accesible ("Ciudad" o "Ciudad, obligatorio"). */
    private TextField field(String label) {
        return scene.getRoot().lookupAll(".form-dialog .text-field").stream()
                .filter(n -> n instanceof TextField)
                .map(TextField.class::cast)
                .filter(tf -> label.equals(tf.getAccessibleText()) || (label + ", obligatorio").equals(tf.getAccessibleText()))
                .findFirst().orElseThrow(() -> new AssertionError("Campo no encontrado: " + label));
    }

    private void fill(String label, String value) throws Exception {
        TextField tf = field(label);
        WaitForAsyncUtils.asyncFx(() -> tf.setText(value)).get();
    }

    private void save() throws Exception {
        Button guardar = (Button) scene.getRoot().lookupAll(".form-dialog .button").stream()
                .filter(n -> n instanceof Button b && "Guardar".equals(b.getText()))
                .findFirst().orElseThrow();
        fire(guardar);
        FxTestSupport.settle(1800);
        assertThat(ModalHost.isShowing()).as("el formulario se cierra al guardar").isFalse();
    }

    private Button button(String accessibleText) {
        return (Button) scene.getRoot().lookupAll(".button").stream()
                .filter(n -> accessibleText.equals(((Button) n).getAccessibleText()))
                .filter(Node::isVisible)
                .findFirst().orElseThrow(() -> new AssertionError("Botón no encontrado: " + accessibleText));
    }

    @SuppressWarnings("unchecked")
    private void selectRow(int codigo) throws Exception {
        TableView<Cliente> table = (TableView<Cliente>) scene.getRoot().lookupAll(".crud-table").stream()
                .filter(n -> n.getScene() != null && n.isVisible())
                .reduce((a, b) -> b).orElseThrow();
        WaitForAsyncUtils.asyncFx(() -> {
            Cliente fila = table.getItems().stream()
                    .filter(c -> c.getCodigoCliente() == codigo)
                    .findFirst().orElseThrow(() -> new AssertionError("Cliente " + codigo + " no está en la tabla"));
            table.getSelectionModel().select(fila);
            table.scrollTo(fila);
        }).get();
        FxTestSupport.settle(300);
    }

    private void fire(Button b) throws Exception {
        assertThat(b.isDisabled()).as("botón %s habilitado", b.getText()).isFalse();
        WaitForAsyncUtils.asyncFx(b::fire).get();
    }
}
