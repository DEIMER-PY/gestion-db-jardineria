package com.jardineria.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Puerto de salida hacia un modelo de lenguaje con llamadas a herramientas (function calling).
 * <p>
 * La capa de aplicación no conoce el proveedor (Groq, otro compatible con OpenAI, un simulador
 * en pruebas) ni el formato JSON del protocolo: trabaja con estos tipos y el adaptador traduce.
 */
public interface ModeloLenguajePort {

    /** {@code true} si hay credenciales y el adaptador puede hacer peticiones. */
    boolean configurado();

    /** Proveedor y modelo, p. ej. «Groq · openai/gpt-oss-120b». */
    String descripcion();

    /**
     * Envía la conversación y las herramientas disponibles; devuelve texto o llamadas a herramientas.
     *
     * @throws ModeloLenguajeException si el proveedor falla (red, credenciales, límite de uso)
     */
    Respuesta completar(List<Mensaje> mensajes, List<Herramienta> herramientas);

    /** Rol de un mensaje en la conversación. */
    enum Rol { SISTEMA, USUARIO, ASISTENTE, HERRAMIENTA }

    /**
     * Mensaje de la conversación.
     *
     * @param llamadas   llamadas a herramientas que hizo el asistente (solo rol ASISTENTE)
     * @param idLlamada  llamada a la que responde (solo rol HERRAMIENTA)
     * @param herramienta nombre de la herramienta que responde (solo rol HERRAMIENTA)
     */
    record Mensaje(Rol rol, String contenido, List<Llamada> llamadas, String idLlamada, String herramienta) {
        public Mensaje {
            Objects.requireNonNull(rol);
            llamadas = llamadas == null ? List.of() : List.copyOf(llamadas);
        }

        public static Mensaje sistema(String texto) {
            return new Mensaje(Rol.SISTEMA, texto, null, null, null);
        }

        public static Mensaje usuario(String texto) {
            return new Mensaje(Rol.USUARIO, texto, null, null, null);
        }

        public static Mensaje asistente(String texto, List<Llamada> llamadas) {
            return new Mensaje(Rol.ASISTENTE, texto, llamadas, null, null);
        }

        public static Mensaje resultado(Llamada llamada, String contenido) {
            return new Mensaje(Rol.HERRAMIENTA, contenido, null, llamada.id(), llamada.nombre());
        }
    }

    /** Llamada a herramienta pedida por el modelo, con los argumentos ya decodificados. */
    record Llamada(String id, String nombre, Map<String, Object> argumentos) {
        public Llamada {
            argumentos = argumentos == null ? Map.of() : Map.copyOf(argumentos);
        }
    }

    /** Definición de una herramienta que el modelo puede invocar. */
    record Herramienta(String nombre, String descripcion, List<Parametro> parametros) {
        public Herramienta {
            parametros = parametros == null ? List.of() : List.copyOf(parametros);
        }
    }

    /**
     * Parámetro de una herramienta.
     *
     * @param tipo    «string» o «integer»
     * @param valores valores permitidos (vacío si es libre)
     */
    record Parametro(String nombre, String tipo, String descripcion, boolean requerido, List<String> valores) {
        public Parametro {
            valores = valores == null ? List.of() : List.copyOf(valores);
        }

        public static Parametro texto(String nombre, String descripcion, boolean requerido) {
            return new Parametro(nombre, "string", descripcion, requerido, null);
        }

        public static Parametro entero(String nombre, String descripcion, boolean requerido) {
            return new Parametro(nombre, "integer", descripcion, requerido, null);
        }
    }

    /** Respuesta del modelo: texto final o llamadas a herramientas pendientes. */
    record Respuesta(String texto, List<Llamada> llamadas) {
        public Respuesta {
            llamadas = llamadas == null ? List.of() : List.copyOf(llamadas);
        }

        public boolean pideHerramientas() {
            return !llamadas.isEmpty();
        }
    }

    /** Fallo del proveedor con un mensaje apto para mostrar al usuario. */
    class ModeloLenguajeException extends RuntimeException {
        public ModeloLenguajeException(String message) {
            super(message);
        }

        public ModeloLenguajeException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
