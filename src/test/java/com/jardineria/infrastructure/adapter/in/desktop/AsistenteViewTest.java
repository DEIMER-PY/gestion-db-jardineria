package com.jardineria.infrastructure.adapter.in.desktop;

import com.jardineria.application.port.in.AsistenteUseCase;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.shell.AppShell;
import com.jardineria.infrastructure.adapter.in.desktop.theme.ThemeManager;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/** Vista del asistente: sugerencias, respuesta con enlaces a pedidos, fuentes y nueva conversación. */
@ExtendWith(ApplicationExtension.class)
class AsistenteViewTest {

    static final class AsistenteFalso implements AsistenteUseCase {
        final List<Integer> historiales = new CopyOnWriteArrayList<>();

        @Override
        public boolean disponible() {
            return true;
        }

        @Override
        public String descripcionProveedor() {
            return "Groq · prueba";
        }

        @Override
        public Respuesta preguntar(List<Turno> historial, String pregunta) {
            historiales.add(historial.size());
            return new Respuesta("Hay 2 pendientes: #14 y #20.", List.of("docs/APARIENCIA.md · Temas"),
                    List.of("buscar_pedidos"));
        }
    }

    private final AsistenteFalso asistente = new AsistenteFalso();
    private AppShell shell;
    private Scene scene;

    @Start
    void start(Stage stage) {
        shell = new AppShell(FxTestSupport.services(asistente));
        scene = new Scene(shell, 1100, 720);
        ThemeManager.install(scene);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    @DisplayName("Una sugerencia envía la pregunta; la respuesta enlaza los pedidos y cita fuentes")
    void conversacion() throws Exception {
        WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(Route.ASISTENTE)).get();
        FxTestSupport.settle(300);
        assertThat(scene.getRoot().lookupAll(".chat-chip")).hasSize(4);

        WaitForAsyncUtils.asyncFx(() -> ((Button) scene.getRoot().lookup(".chat-chip")).fire()).get();
        FxTestSupport.settle(400);

        assertThat(scene.getRoot().lookupAll(".chat-welcome")).isEmpty();
        assertThat(scene.getRoot().lookupAll(".chat-bubble-user")).hasSize(1);
        assertThat(scene.getRoot().lookupAll(".chat-link")).extracting(n -> ((Hyperlink) n).getText())
                .containsExactly("#14", "#20");
        assertThat(scene.getRoot().lookupAll(".chat-meta")).extracting(n -> ((Label) n).getText())
                .containsExactly("Fuentes: docs/APARIENCIA.md · Temas", "Consultó: buscar pedidos");
        Node row = scene.getRoot().lookupAll(".chat-row-assistant").iterator().next();
        assertThat(row.getAccessibleText()).startsWith("Asistente: Hay 2 pendientes");

        // segunda pregunta: lleva el turno anterior
        WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().currentView()).get();
        WaitForAsyncUtils.asyncFx(() -> ((com.jardineria.infrastructure.adapter.in.desktop.views.assistant.AsistenteView)
                shell.getNavigator().currentView()).createNew()).get();
        FxTestSupport.settle(100);
        assertThat(scene.getRoot().lookupAll(".chat-welcome")).hasSize(1);
        assertThat(scene.getRoot().lookupAll(".chat-bubble")).isEmpty();
        assertThat(asistente.historiales).containsExactly(0);

        WaitForAsyncUtils.asyncFx(() -> ((Button) scene.getRoot().lookup(".chat-chip")).fire()).get();
        FxTestSupport.settle(400);
        WaitForAsyncUtils.asyncFx(() -> ((Hyperlink) scene.getRoot().lookup(".chat-link")).fire()).get();
        FxTestSupport.settle(300);
        assertThat(shell.getNavigator().getCurrent()).isEqualTo(Route.PEDIDOS);
    }
}
