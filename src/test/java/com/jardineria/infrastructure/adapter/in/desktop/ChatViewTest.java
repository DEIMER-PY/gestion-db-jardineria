package com.jardineria.infrastructure.adapter.in.desktop;

import com.jardineria.application.port.in.EmpleadoUseCase;
import com.jardineria.application.port.in.MensajeriaUseCase;
import com.jardineria.domain.model.Empleado;
import com.jardineria.domain.model.chat.Canal;
import com.jardineria.domain.model.chat.Mensaje;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.shell.AppShell;
import com.jardineria.infrastructure.adapter.in.desktop.theme.ThemeManager;
import com.jardineria.infrastructure.adapter.in.desktop.views.chat.ChatIdentity;
import com.jardineria.infrastructure.adapter.in.desktop.views.chat.ChatView;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Vista de mensajes: identidad, canales con no leídos, menciones, envío, sondeo y mensajes directos. */
@ExtendWith(ApplicationExtension.class)
class ChatViewTest {

    /** Mensajería en memoria con el mismo contrato que el servicio real. */
    static final class MensajeriaEnMemoria implements MensajeriaUseCase {
        final List<Canal> canales = new CopyOnWriteArrayList<>(List.of(
                new Canal(1, "general", Canal.Tipo.GENERAL, null, "Avisos para todo el equipo"),
                new Canal(2, "oficina-tal-es", Canal.Tipo.OFICINA, "TAL-ES", "Equipo de Talavera de la Reina")));
        final List<Mensaje> mensajes = new CopyOnWriteArrayList<>();
        final Map<Integer, Long> pendientes = new HashMap<>(Map.of(2, 1L));
        final AtomicLong ids = new AtomicLong();

        Mensaje publicar(int canal, int autor, String texto) {
            Mensaje m = new Mensaje(ids.incrementAndGet(), canal, autor, texto, LocalDateTime.now());
            mensajes.add(m);
            return m;
        }

        @Override
        public List<Canal> canales(int codigoEmpleado) {
            return canales.stream().filter(c -> c.visiblePara(codigoEmpleado)).toList();
        }

        @Override
        public Canal canalDirecto(int codigoEmpleado, int otroEmpleado) {
            String nombre = Canal.nombreDirecto(codigoEmpleado, otroEmpleado);
            return canales.stream().filter(c -> c.nombre().equals(nombre)).findFirst().orElseGet(() -> {
                Canal c = new Canal(canales.size() + 1, nombre, Canal.Tipo.DIRECTO, null, null);
                canales.add(c);
                return c;
            });
        }

        @Override
        public List<Mensaje> historial(int idCanal, int limite) {
            return mensajes.stream().filter(m -> m.idCanal() == idCanal).toList();
        }

        @Override
        public Mensaje enviar(int idCanal, int codigoEmpleado, String texto) {
            return publicar(idCanal, codigoEmpleado, texto.strip());
        }

        @Override
        public long ultimoMensaje() {
            return ids.get();
        }

        @Override
        public void marcarLeido(int idCanal, int codigoEmpleado, long ultimoMensaje) {
            pendientes.remove(idCanal);
        }

        @Override
        public Sondeo sondear(int codigoEmpleado, long despuesDe) {
            List<Canal> visibles = canales(codigoEmpleado);
            return new Sondeo(mensajes.stream().filter(m -> m.id() > despuesDe)
                    .filter(m -> visibles.stream().anyMatch(c -> c.id() == m.idCanal())).toList(),
                    pendientes, visibles);
        }
    }

    private final MensajeriaEnMemoria chat = new MensajeriaEnMemoria();
    private AppShell shell;
    private Scene scene;

    @Start
    void start(Stage stage) {
        ChatIdentity.useMemory();
        chat.publicar(1, 1, "Buenos días, equipo.");
        chat.publicar(1, 3, "El #14 sigue pendiente; @felipe.rosas, ¿puedes llamar?");
        EmpleadoUseCase empleados = mock(EmpleadoUseCase.class, Mockito.RETURNS_SMART_NULLS);
        when(empleados.listarTodos()).thenReturn(List.of(
                new Empleado(1, "Marcos", "Magaña", "Pérez", "3897", "m@j.es", "TAL-ES", null, "Director General"),
                new Empleado(3, "Alberto", "Soria", "Carrasco", "2837", "a@j.es", "TAL-ES", 1, "Subdirector Ventas"),
                new Empleado(5, "Felipe", "Rosas", "Marquez", "2844", "f@j.es", "TAL-ES", 3, "Representante Ventas")));
        shell = new AppShell(FxTestSupport.services(chat, empleados));
        scene = new Scene(shell, 1100, 720);
        ThemeManager.install(scene);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    @DisplayName("Elegir identidad muestra los canales; se envía, se recibe por sondeo y se abre un directo")
    @SuppressWarnings("unchecked")
    void conversacion() throws Exception {
        WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(Route.CHAT)).get();
        FxTestSupport.settle(400);
        assertThat(labels()).contains("¿Quién eres?");

        ComboBox<Empleado> who = (ComboBox<Empleado>) scene.getRoot().lookup(".team-side .combo-box");
        WaitForAsyncUtils.asyncFx(() -> who.setValue(who.getItems().stream()
                .filter(e -> e.getCodigoEmpleado() == 5).findFirst().orElseThrow())).get();
        FxTestSupport.settle(500);

        assertThat(ChatIdentity.current()).hasValue(5);
        assertThat(labels()).contains("# general", "general", "oficina-tal-es", "Todavía no hay conversaciones");
        assertThat(scene.getRoot().lookupAll(".team-unread")).extracting(n -> ((Label) n).getText()).containsExactly("1");
        assertThat(scene.getRoot().lookupAll(".team-message")).hasSize(2);
        assertThat(scene.getRoot().lookupAll(".team-message-mention")).hasSize(1);
        assertThat(scene.getRoot().lookupAll(".team-mention-me")).extracting(n -> ((Text) n).getText())
                .containsExactly("@felipe.rosas");
        assertThat(scene.getRoot().lookupAll(".chat-link")).extracting(n -> ((Hyperlink) n).getText()).containsExactly("#14");

        // enviar
        WaitForAsyncUtils.asyncFx(() -> {
            ((TextArea) scene.getRoot().lookup(".chat-input")).setText("Llamo ahora mismo.");
            ((Button) scene.getRoot().lookup(".chat-send")).fire();
        }).get();
        FxTestSupport.settle(300);
        assertThat(scene.getRoot().lookupAll(".team-message")).hasSize(3);

        // llega un mensaje de otra persona: el sondeo lo añade sin duplicar el propio
        chat.publicar(1, 1, "Perfecto, gracias.");
        WaitForAsyncUtils.asyncFx(() -> ((ChatView) shell.getNavigator().currentView()).poll()).get();
        FxTestSupport.settle(300);
        assertThat(scene.getRoot().lookupAll(".team-message")).hasSize(4);
        Node last = new ArrayList<>(scene.getRoot().lookupAll(".team-message")).get(3);
        assertThat(last.getAccessibleText()).startsWith("Marcos Magaña Pérez").endsWith("Perfecto, gracias.");

        // mensaje directo
        ComboBox<Empleado> direct = (ComboBox<Empleado>) new ArrayList<>(scene.getRoot().lookupAll(".team-side .combo-box")).get(1);
        WaitForAsyncUtils.asyncFx(() -> direct.getSelectionModel().select(direct.getItems().stream()
                .filter(e -> e.getCodigoEmpleado() == 3).findFirst().orElseThrow())).get();
        FxTestSupport.settle(400);
        assertThat(labels()).contains("Alberto Soria Carrasco", "Mensaje directo", "Todavía no hay mensajes. Escribe el primero.");
        assertThat(chat.canales).extracting(Canal::nombre).contains("dm-3-5");
    }

    private List<String> labels() throws Exception {
        return WaitForAsyncUtils.asyncFx(() -> scene.getRoot().lookupAll(".label").stream()
                .map(n -> ((Label) n).getText()).toList()).get();
    }
}
