package com.jardineria.infrastructure.adapter.out.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jardineria.application.port.out.ModeloLenguajePort.Herramienta;
import com.jardineria.application.port.out.ModeloLenguajePort.Llamada;
import com.jardineria.application.port.out.ModeloLenguajePort.Mensaje;
import com.jardineria.application.port.out.ModeloLenguajePort.ModeloLenguajeException;
import com.jardineria.application.port.out.ModeloLenguajePort.Parametro;
import com.jardineria.application.port.out.ModeloLenguajePort.Respuesta;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Adaptador de Groq contra un servidor HTTP local que imita la API compatible con OpenAI. */
class GroqLanguageModelAdapterTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    private HttpServer server;
    private final AtomicReference<String> body = new AtomicReference<>();
    private final AtomicReference<String> auth = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String reply = "{}";

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/openai/v1/chat/completions", ex -> {
            body.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            auth.set(ex.getRequestHeaders().getFirst("Authorization"));
            byte[] out = reply.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(status, out.length);
            ex.getResponseBody().write(out);
            ex.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private GroqLanguageModelAdapter adapter(String key) {
        return new GroqLanguageModelAdapter(key, null,
                "http://127.0.0.1:" + server.getAddress().getPort() + "/openai/v1/", null, Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("Envía mensajes y herramientas en formato OpenAI y decodifica las llamadas a herramientas")
    void llamadasAHerramientas() throws Exception {
        reply = """
                {"choices":[{"message":{"role":"assistant","content":null,"tool_calls":[
                  {"id":"call_1","type":"function","function":{"name":"buscar_pedidos","arguments":"{\\"estado\\":\\"Pendiente\\",\\"limite\\":5}"}}
                ]}}]}""";
        Llamada previa = new Llamada("call_0", "metricas_generales", Map.of());
        Respuesta r = adapter("gsk_prueba").completar(List.of(
                        Mensaje.sistema("Eres útil"),
                        Mensaje.usuario("¿Pendientes?"),
                        Mensaje.asistente(null, List.of(previa)),
                        Mensaje.resultado(previa, "Pedidos: 3")),
                List.of(new Herramienta("buscar_pedidos", "Busca pedidos", List.of(
                        new Parametro("estado", "string", "Estado", false, List.of("Pendiente", "Entregado")),
                        Parametro.entero("limite", "Máximo", true)))));

        assertThat(auth.get()).isEqualTo("Bearer gsk_prueba");
        JsonNode req = JSON.readTree(body.get());
        assertThat(req.path("model").asText()).isEqualTo(GroqLanguageModelAdapter.MODELO_POR_DEFECTO);
        assertThat(req.path("reasoning_effort").asText()).isEqualTo("low");
        assertThat(req.path("messages")).extracting(n -> n.path("role").asText())
                .containsExactly("system", "user", "assistant", "tool");
        JsonNode call = req.path("messages").path(2).path("tool_calls").path(0);
        assertThat(call.path("function").path("arguments").asText()).isEqualTo("{}");
        assertThat(req.path("messages").path(3).path("tool_call_id").asText()).isEqualTo("call_0");
        JsonNode params = req.path("tools").path(0).path("function").path("parameters");
        assertThat(params.path("properties").path("estado").path("enum")).hasSize(2);
        assertThat(params.path("required")).extracting(JsonNode::asText).containsExactly("limite");

        assertThat(r.pideHerramientas()).isTrue();
        assertThat(r.llamadas().get(0).nombre()).isEqualTo("buscar_pedidos");
        assertThat(r.llamadas().get(0).argumentos()).containsEntry("estado", "Pendiente").containsEntry("limite", 5);
    }

    @Test
    @DisplayName("Devuelve el texto final cuando no hay llamadas")
    void textoFinal() {
        reply = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Hay 3 pedidos pendientes.\"}}]}";
        Respuesta r = adapter("k").completar(List.of(Mensaje.usuario("Hola")), List.of());
        assertThat(r.pideHerramientas()).isFalse();
        assertThat(r.texto()).isEqualTo("Hay 3 pedidos pendientes.");
        assertThat(body.get()).doesNotContain("\"tools\"");
    }

    @Test
    @DisplayName("Traduce 401 y 429 a mensajes en español sin exponer la clave")
    void errores() {
        status = 401;
        reply = "{\"error\":{\"message\":\"Invalid API Key\"}}";
        assertThatThrownBy(() -> adapter("gsk_secreta").completar(List.of(Mensaje.usuario("x")), List.of()))
                .isInstanceOf(ModeloLenguajeException.class)
                .hasMessageContaining("GROQ_API_KEY")
                .hasMessageNotContaining("gsk_secreta");
        status = 429;
        assertThatThrownBy(() -> adapter("k").completar(List.of(Mensaje.usuario("x")), List.of()))
                .hasMessageContaining("límite de uso");
        status = 400;
        reply = "{\"error\":{\"message\":\"tool_use_failed\"}}";
        assertThatThrownBy(() -> adapter("k").completar(List.of(Mensaje.usuario("x")), List.of()))
                .hasMessageContaining("(400)").hasMessageContaining("tool_use_failed");
    }

    @Test
    @DisplayName("Sin clave no está configurado ni hace peticiones")
    void sinClave() {
        GroqLanguageModelAdapter a = adapter("  ");
        assertThat(a.configurado()).isFalse();
        assertThat(a.toString()).contains("ausente");
        assertThatThrownBy(() -> a.completar(List.of(Mensaje.usuario("x")), List.of()))
                .isInstanceOf(ModeloLenguajeException.class);
        assertThat(body.get()).isNull();
    }
}
