package com.jardineria.application.port.in;

import java.util.List;

/**
 * Asistente conversacional: responde preguntas sobre los datos de la jardinería (con herramientas
 * de solo lectura sobre los casos de uso) y sobre el uso de la aplicación (con la documentación
 * recuperada del proyecto, RAG). Nunca modifica datos.
 */
public interface AsistenteUseCase {

    /** {@code true} si hay un modelo de lenguaje configurado (p. ej. la variable {@code GROQ_API_KEY}). */
    boolean disponible();

    /** Proveedor y modelo en uso, o instrucciones breves si no está configurado. */
    String descripcionProveedor();

    /**
     * Responde a {@code pregunta} teniendo en cuenta los turnos anteriores.
     * Bloquea mientras consulta el modelo y la base de datos: no llamar desde el hilo de la interfaz.
     *
     * @throws IllegalStateException si el asistente no está disponible
     * @throws RuntimeException      con un mensaje para el usuario si el proveedor falla
     */
    Respuesta preguntar(List<Turno> historial, String pregunta);

    /** Turno anterior de la conversación. */
    record Turno(boolean delUsuario, String texto) {
    }

    /**
     * Respuesta del asistente.
     *
     * @param fuentes      documentos citados, p. ej. «docs/APARIENCIA.md · Temas»
     * @param herramientas herramientas que consultó, en orden
     */
    record Respuesta(String texto, List<String> fuentes, List<String> herramientas) {
        public Respuesta {
            fuentes = fuentes == null ? List.of() : List.copyOf(fuentes);
            herramientas = herramientas == null ? List.of() : List.copyOf(herramientas);
        }
    }
}
