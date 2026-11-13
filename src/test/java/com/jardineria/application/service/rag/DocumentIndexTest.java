package com.jardineria.application.service.rag;

import com.jardineria.application.port.out.DocumentacionPort.Documento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentIndexTest {

    private static final Documento APARIENCIA = new Documento("docs/APARIENCIA.md", """
            # Apariencia

            Introducción general.

            ## Temas

            La aplicación trae seis temas. Cámbialos en Ajustes o con Ctrl+, desde cualquier pantalla.

            ## Colores de acento

            El acento colorea botones y enlaces.
            """);
    private static final Documento MULTI = new Documento("docs/MULTIPLATAFORMA.md", """
            # Multiplataforma

            ```bash
            # esto es un comentario, no un encabezado
            ./mvnw javafx:run
            ```

            ## Instalación en macOS

            Descarga el JAR de mac-aarch64 y ejecútalo con Java 21.
            """);

    @Test
    @DisplayName("Trocea por encabezados sin confundir comentarios de bloques de código")
    void troceaPorSecciones() {
        List<DocumentIndex.Fragmento> f = DocumentIndex.trocear(MULTI);
        assertThat(f).extracting(DocumentIndex.Fragmento::seccion).containsExactly("Multiplataforma", "Instalación en macOS");
        assertThat(f.get(0).texto()).contains("# esto es un comentario");
    }

    @Test
    @DisplayName("Encuentra la sección correcta sin depender de tildes, mayúsculas ni plurales")
    void buscaSinTildesNiPlurales() {
        DocumentIndex index = new DocumentIndex(List.of(APARIENCIA, MULTI));

        var temas = index.buscar("como cambio el TEMA", 3);
        assertThat(temas).isNotEmpty();
        assertThat(temas.get(0).fragmento().seccion()).isEqualTo("Temas");

        var mac = index.buscar("instalacion mac", 3);
        assertThat(mac.get(0).fragmento().fuente()).isEqualTo("docs/MULTIPLATAFORMA.md");
        assertThat(mac.get(0).fragmento().seccion()).isEqualTo("Instalación en macOS");
    }

    @Test
    @DisplayName("Sin coincidencias o solo con palabras vacías devuelve una lista vacía")
    void sinCoincidencias() {
        DocumentIndex index = new DocumentIndex(List.of(APARIENCIA));
        assertThat(index.buscar("kubernetes", 3)).isEmpty();
        assertThat(index.buscar("de la que", 3)).isEmpty();
        assertThat(new DocumentIndex(List.of()).buscar("temas", 3)).isEmpty();
    }
}
