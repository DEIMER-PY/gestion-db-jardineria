package com.jardineria.infrastructure.adapter.in.desktop;

import com.jardineria.application.port.in.PedidoUseCase;
import com.jardineria.domain.model.Pedido;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.shell.AppShell;
import com.jardineria.infrastructure.adapter.in.desktop.theme.ThemeManager;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Navegación contextual: desde el dashboard o la analítica se abre un módulo ya filtrado
 * y con el elemento seleccionado ({@code Navigator.navigate(route, query)} → {@code View.reveal}).
 */
@ExtendWith(ApplicationExtension.class)
class NavigationRevealTest {

    private AppShell shell;
    private Scene scene;

    @Start
    void start(Stage stage) {
        PedidoUseCase pedidos = mock(PedidoUseCase.class, Mockito.RETURNS_SMART_NULLS);
        LocalDate hoy = LocalDate.of(2009, 1, 17);
        when(pedidos.listarTodos()).thenReturn(List.of(
                new Pedido(300, hoy, hoy, null, "Pendiente", null, 1),
                new Pedido(30, hoy.minusDays(1), hoy, hoy, "Entregado", null, 1),
                new Pedido(31, hoy.minusDays(2), hoy, null, "Rechazado", null, 1)));
        DesktopServices services = FxTestSupport.services(pedidos);
        shell = new AppShell(services);
        scene = new Scene(shell, 1280, 800);
        ThemeManager.install(scene);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    @DisplayName("Un código de pedido filtra la tabla y selecciona exactamente ese pedido (no el 300)")
    void revelaPedidoPorCodigo() throws Exception {
        WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(Route.PEDIDOS, "#30")).get();
        FxTestSupport.settle(600);

        assertThat(shell.getNavigator().getCurrent()).isEqualTo(Route.PEDIDOS);
        assertThat(searchText()).isEqualTo("#30");
        assertThat(selectedPedido().getCodigoPedido()).isEqualTo(30);
    }

    @Test
    @DisplayName("Un estado activa el filtro segmentado y limpia la búsqueda, también si ya estás en Pedidos")
    void revelaPedidosPorEstado() throws Exception {
        WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(Route.PEDIDOS, "#31")).get();
        FxTestSupport.settle(600);
        WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(Route.PEDIDOS, "Rechazado")).get();
        FxTestSupport.settle(300);

        assertThat(searchText()).isEmpty();
        assertThat(selectedToggle()).isEqualTo("Rechazado");
        assertThat(table().getItems()).extracting(Pedido::getEstado).containsOnly("Rechazado");
    }

    private String searchText() throws Exception {
        return WaitForAsyncUtils.asyncFx(() -> ((TextField) scene.getRoot().lookup(".crud-search")).getText()).get();
    }

    private String selectedToggle() throws Exception {
        return WaitForAsyncUtils.asyncFx(() -> scene.getRoot().lookupAll(".segmented .toggle-button").stream()
                .map(ToggleButton.class::cast).filter(ToggleButton::isSelected)
                .map(ToggleButton::getText).findFirst().orElse(null)).get();
    }

    @SuppressWarnings("unchecked")
    private TableView<Pedido> table() throws Exception {
        return WaitForAsyncUtils.asyncFx(() -> (TableView<Pedido>) scene.getRoot().lookup(".crud-table")).get();
    }

    private Pedido selectedPedido() throws Exception {
        TableView<Pedido> table = table();
        return WaitForAsyncUtils.asyncFx(() -> table.getSelectionModel().getSelectedItem()).get();
    }
}
