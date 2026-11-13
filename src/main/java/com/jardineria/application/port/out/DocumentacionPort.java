package com.jardineria.application.port.out;

import java.util.List;

/**
 * Puerto de salida que entrega la documentación del proyecto (README, guías de {@code docs/})
 * para indexarla y responder preguntas de uso con citas (RAG).
 */
public interface DocumentacionPort {

    /** Documentos disponibles; una lista vacía si no hay ninguno. */
    List<Documento> cargar();

    /**
     * Documento Markdown.
     *
     * @param fuente ruta relativa, p. ej. {@code docs/APARIENCIA.md}
     */
    record Documento(String fuente, String markdown) {
    }
}
