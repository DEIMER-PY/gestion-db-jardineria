package com.jardineria.infrastructure.adapter.in.desktop;

import com.jardineria.application.port.in.PedidoUseCase;
import com.jardineria.domain.model.Pedido;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.shell.AppShell;
import com.jardineria.infrastructure.adapter.in.desktop.theme.ThemeManager;
import com.jardineria.infrastructure.adapter.in.desktop.views.board.BoardView;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tablero Kanban: columnas por estado y cambio de estado con el teclado y confirmación. */
@ExtendWith(ApplicationExtension.class)
class BoardViewTest {

    private AppShell shell;
    private Scene scene;
    private PedidoUseCase pedidos;

    @Start
    void start(Stage stage) {
        pedidos = mock(PedidoUseCase.class, Mockito.RETURNS_SMART_NULLS);
        LocalDate d = LocalDate.of(2009, 1, 17);
        when(pedidos.listarTodos()).thenReturn(List.of(
                new Pedido(1, d, d.plusDays(5), null, "Pendiente", null, 1),
                new Pedido(2, d, d.plusDays(5), null, "PENDIENTE", null, 1),
                new Pedido(3, d, d.plusDays(5), d.plusDays(4), "Entregado", null, 1),
                new Pedido(4, d, d.plusDays(5), null, "Rechazado", null, 1)));
        shell = new AppShell(FxTestSupport.services(pedidos));
        scene = new Scene(shell, 1280, 800);
        ThemeManager.install(scene);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    @DisplayName("Los estados se agrupan sin importar mayúsculas y las columnas vecinas siguen el flujo del pedido")
    void columnas() {
        assertThat(BoardView.columnOf("PENDIENTE")).isEqualTo("Pendiente");
        assertThat(BoardView.columnOf("desconocido")).isEqualTo("Pendiente");
        assertThat(BoardView.neighbour("Pendiente", 1)).contains("Entregado");
        assertThat(BoardView.neighbour("Pendiente", -1)).isEmpty();
        assertThat(BoardView.neighbour("Rechazado", 1)).isEmpty();
    }

    @Test
    @DisplayName("Flecha derecha sobre una tarjeta pide confirmación y mueve el pedido a la columna siguiente")
    void mueveConTeclado() throws Exception {
        WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(Route.TABLERO)).get();
        FxTestSupport.settle(600);
        assertThat(columnCounts()).containsExactly("2", "1", "1");

        WaitForAsyncUtils.asyncFx(() -> card("Pedido 1 ").fireEvent(
                new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.RIGHT, false, false, false, false))).get();
        FxTestSupport.settle(300);
        WaitForAsyncUtils.asyncFx(() -> scene.getRoot().lookupAll(".dialog-confirm .button").stream()
                .map(Button.class::cast).filter(b -> "Mover a Entregado".equals(b.getText()))
                .findFirst().orElseThrow().fire()).get();

        verify(pedidos, timeout(2000)).cambiarEstado(eq(1), eq("Entregado"), any());
    }

    private Node card(String prefix) {
        return scene.getRoot().lookupAll(".board-card").stream()
                .filter(n -> n.getAccessibleText() != null && n.getAccessibleText().startsWith(prefix))
                .findFirst().orElseThrow();
    }

    private List<String> columnCounts() throws Exception {
        return WaitForAsyncUtils.asyncFx(() -> scene.getRoot().lookupAll(".board-column .badge").stream()
                .map(n -> ((Label) n).getText()).toList()).get();
    }
}
