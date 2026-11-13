package com.jardineria.infrastructure.adapter.out.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jardineria.application.port.out.ModeloLenguajePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adaptador hacia Groq con su API compatible con OpenAI ({@code /chat/completions} y {@code tools}).
 * <p>
 * Configuración por variables de entorno, nunca en el repositorio:
 * <ul>
 *   <li>{@code GROQ_API_KEY} (obligatoria para activar el asistente)</li>
 *   <li>{@code GROQ_MODEL} (por defecto {@value #MODELO_POR_DEFECTO})</li>
 *   <li>{@code GROQ_BASE_URL} (por defecto {@value #URL_POR_DEFECTO})</li>
 * </ul>
 * La clave no se escribe nunca en los registros.
 */
public class GroqLanguageModelAdapter implements ModeloLenguajePort {
    public static final String MODELO_POR_DEFECTO = "openai/gpt-oss-120b";
    public static final String URL_POR_DEFECTO = "https://api.groq.com/openai/v1";
    private static final Logger log = LoggerFactory.getLogger(GroqLanguageModelAdapter.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAPA = new TypeReference<>() {
    };

    private final String apiKey;
    private final String modelo;
    private final URI endpoint;
    private final HttpClient http;
    private final Duration timeout;

    public GroqLanguageModelAdapter(String apiKey, String modelo, String baseUrl, HttpClient http, Duration timeout) {
        this.apiKey = apiKey == null ? "" : apiKey.strip();
        this.modelo = blank(modelo) ? MODELO_POR_DEFECTO : modelo.strip();
        String base = blank(baseUrl) ? URL_POR_DEFECTO : baseUrl.strip();
        this.endpoint = URI.create(base.replaceAll("/+$", "") + "/chat/completions");
        this.http = http != null ? http : HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.timeout = timeout == null ? Duration.ofSeconds(60) : timeout;
    }

    /** Lee la configuración de las variables de entorno. */
    public static GroqLanguageModelAdapter desdeEntorno() {
        return new GroqLanguageModelAdapter(System.getenv("GROQ_API_KEY"), System.getenv("GROQ_MODEL"),
                System.getenv("GROQ_BASE_URL"), null, null);
    }

    @Override
    public boolean configurado() {
        return !apiKey.isEmpty();
    }

    @Override
    public String descripcion() {
        return "Groq · " + modelo;
    }

    String modelo() {
        return modelo;
    }

    @Override
    public Respuesta completar(List<Mensaje> mensajes, List<Herramienta> herramientas) {
        if (!configurado()) {
            throw new ModeloLenguajeException("Falta la variable de entorno GROQ_API_KEY.");
        }
        String cuerpo = peticion(mensajes, herramientas);
        HttpRequest req = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> res;
        long t0 = System.nanoTime();
        try {
            res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ModeloLenguajeException("No se pudo conectar con Groq. Revisa la conexión a Internet.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ModeloLenguajeException("Consulta cancelada.", e);
        }
        log.debug("Groq {} respondió {} en {} ms", modelo, res.statusCode(), (System.nanoTime() - t0) / 1_000_000);
        if (res.statusCode() / 100 != 2) {
            throw new ModeloLenguajeException(mensajeError(res.statusCode(), res.body()));
        }
        return respuesta(res.body());
    }

    // ---------------------------------------------------------------- traducción a JSON

    String peticion(List<Mensaje> mensajes, List<Herramienta> herramientas) {
        ObjectNode root = JSON.createObjectNode();
        root.put("model", modelo);
        root.put("temperature", 0.2);
        root.put("max_completion_tokens", 1500);
        if (modelo.startsWith("openai/gpt-oss")) {
            root.put("reasoning_effort", "low");
        }
        ArrayNode msgs = root.putArray("messages");
        for (Mensaje m : mensajes) {
            ObjectNode n = msgs.addObject();
            n.put("role", switch (m.rol()) {
                case SISTEMA -> "system";
                case USUARIO -> "user";
                case ASISTENTE -> "assistant";
                case HERRAMIENTA -> "tool";
            });
            if (m.contenido() != null) {
                n.put("content", m.contenido());
            } else {
                n.putNull("content");
            }
            if (!m.llamadas().isEmpty()) {
                ArrayNode calls = n.putArray("tool_calls");
                for (Llamada l : m.llamadas()) {
                    ObjectNode c = calls.addObject();
                    c.put("id", l.id());
                    c.put("type", "function");
                    ObjectNode f = c.putObject("function");
                    f.put("name", l.nombre());
                    f.put("arguments", escribir(l.argumentos()));
                }
            }
            if (m.idLlamada() != null) {
                n.put("tool_call_id", m.idLlamada());
                n.put("name", m.herramienta());
            }
        }
        if (herramientas != null && !herramientas.isEmpty()) {
            ArrayNode tools = root.putArray("tools");
            for (Herramienta h : herramientas) {
                ObjectNode f = tools.addObject().put("type", "function").putObject("function");
                f.put("name", h.nombre());
                f.put("description", h.descripcion());
                ObjectNode params = f.putObject("parameters");
                params.put("type", "object");
                ObjectNode props = params.putObject("properties");
                ArrayNode req = params.putArray("required");
                for (Parametro p : h.parametros()) {
                    ObjectNode prop = props.putObject(p.nombre());
                    prop.put("type", p.tipo());
                    prop.put("description", p.descripcion());
                    if (!p.valores().isEmpty()) {
                        ArrayNode e = prop.putArray("enum");
                        p.valores().forEach(e::add);
                    }
                    if (p.requerido()) {
                        req.add(p.nombre());
                    }
                }
            }
            root.put("tool_choice", "auto");
        }
        return escribir(root);
    }

    Respuesta respuesta(String body) {
        JsonNode msg;
        try {
            msg = JSON.readTree(body).path("choices").path(0).path("message");
        } catch (JsonProcessingException e) {
            throw new ModeloLenguajeException("Groq devolvió una respuesta que no se puede leer.", e);
        }
        if (msg.isMissingNode()) {
            throw new ModeloLenguajeException("Groq devolvió una respuesta vacía.");
        }
        String texto = msg.path("content").isTextual() ? msg.path("content").asText() : null;
        List<Llamada> llamadas = new ArrayList<>();
        for (JsonNode c : msg.path("tool_calls")) {
            JsonNode f = c.path("function");
            llamadas.add(new Llamada(c.path("id").asText(), f.path("name").asText(), argumentos(f.path("arguments"))));
        }
        return new Respuesta(texto, llamadas);
    }

    private static Map<String, Object> argumentos(JsonNode n) {
        try {
            if (n.isTextual() && !n.asText().isBlank()) {
                return sinNulos(JSON.readValue(n.asText(), MAPA));
            }
            if (n.isObject()) {
                return sinNulos(JSON.convertValue(n, MAPA));
            }
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.debug("Argumentos de herramienta no válidos: {}", e.getMessage());
        }
        return Map.of();
    }

    private static Map<String, Object> sinNulos(Map<String, Object> m) {
        m.values().removeIf(v -> v == null);
        return m;
    }

    static String mensajeError(int status, String body) {
        String detalle = "";
        try {
            detalle = JSON.readTree(body).path("error").path("message").asText("");
        } catch (JsonProcessingException ignored) {
            // cuerpo que no es JSON: se usa solo el código
        }
        return switch (status) {
            case 401 -> "Groq rechazó la clave (401). Revisa la variable GROQ_API_KEY.";
            case 403 -> "Groq denegó el acceso (403). Comprueba que la clave tenga permiso para el modelo.";
            case 404 -> "Groq no encuentra el modelo (404). Revisa la variable GROQ_MODEL.";
            case 413 -> "La conversación es demasiado larga para el modelo (413). Empieza una nueva.";
            case 429 -> "Se alcanzó el límite de uso de Groq (429). Espera unos segundos y vuelve a intentarlo.";
            default -> {
                String base = status >= 500 ? "Groq no está disponible ahora mismo (" + status + ")."
                        : "Groq respondió con un error (" + status + ").";
                yield detalle.isBlank() ? base : base + " " + recortar(detalle);
            }
        };
    }

    private static String recortar(String s) {
        String t = s.strip();
        return t.length() > 240 ? t.substring(0, 240) + "…" : t;
    }

    private static String escribir(Object o) {
        try {
            return JSON.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    @Override
    public String toString() {
        return "GroqLanguageModelAdapter[" + modelo + ", clave " + (configurado() ? "definida" : "ausente") + "]";
    }
}
