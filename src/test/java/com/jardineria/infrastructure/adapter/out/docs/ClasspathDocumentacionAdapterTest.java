package com.jardineria.infrastructure.adapter.out.docs;

import com.jardineria.application.port.out.DocumentacionPort.Documento;
import com.jardineria.application.service.rag.DocumentIndex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** La documentación real llega al classpath y el índice encuentra respuestas útiles en ella. */
class ClasspathDocumentacionAdapterTest {

    @Test
    @DisplayName("Empaqueta todos los documentos y responde a preguntas de uso con la sección adecuada")
    void documentacionReal() {
        List<Documento> docs = new ClasspathDocumentacionAdapter().cargar();
        assertThat(docs).extracting(Documento::fuente).containsExactlyElementsOf(ClasspathDocumentacionAdapter.FUENTES);
        assertThat(docs).allSatisfy(d -> assertThat(d.markdown()).isNotBlank());
        assertThat(docs.get(0).markdown()).contains("Jardinería");

        DocumentIndex index = new DocumentIndex(docs);
        assertThat(index.size()).isGreaterThan(40);
        assertThat(index.buscar("atajos de teclado", 3)).isNotEmpty();
        assertThat(index.buscar("cambiar el tema oscuro claro", 3)).isNotEmpty();
        assertThat(index.buscar("levantar la base de datos con docker", 3)).isNotEmpty();
    }
}
