package com.jardineria.infrastructure.adapter.out.docs;

import com.jardineria.application.port.out.DocumentacionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee la documentación copiada al classpath en {@code rag/} durante la compilación (ver el pom).
 * La lista es fija: así funciona igual desde {@code target/classes} y desde el JAR.
 */
public class ClasspathDocumentacionAdapter implements DocumentacionPort {
    private static final Logger log = LoggerFactory.getLogger(ClasspathDocumentacionAdapter.class);
    static final List<String> FUENTES = List.of(
            "README.md", "AGENTS.md",
            "docs/README.md", "docs/APARIENCIA.md", "docs/ARQUITECTURA.md", "docs/ASISTENTE.md", "docs/CAPTURAS.md",
            "docs/DASHBOARD.md", "docs/MULTIPLATAFORMA.md", "docs/TECNOLOGIAS.md");

    @Override
    public List<Documento> cargar() {
        List<Documento> out = new ArrayList<>();
        ClassLoader cl = ClasspathDocumentacionAdapter.class.getClassLoader();
        for (String fuente : FUENTES) {
            try (InputStream in = cl.getResourceAsStream("rag/" + fuente)) {
                if (in == null) {
                    log.debug("Documento no empaquetado: {}", fuente);
                    continue;
                }
                out.add(new Documento(fuente, new String(in.readAllBytes(), StandardCharsets.UTF_8)));
            } catch (IOException e) {
                log.warn("No se pudo leer {}: {}", fuente, e.getMessage());
            }
        }
        log.info("Documentación para el asistente: {} documentos", out.size());
        return out;
    }
}
