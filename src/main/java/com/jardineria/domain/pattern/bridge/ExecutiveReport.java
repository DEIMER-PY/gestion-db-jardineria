package com.jardineria.domain.pattern.bridge;

import java.util.List;

/**
 * Abstracción Refinada (Refined Abstraction) del Patrón Bridge.
 * Genera reportes ejecutivos operacionales formateados mediante el implementor asignado.
 */
public class ExecutiveReport extends ReportBridge {
    private final String titulo;
    private final String subtitulo;
    private final List<String> columnas;

    public ExecutiveReport(ReportOutputImplementor implementor, String titulo, String subtitulo, List<String> columnas) {
        super(implementor);
        this.titulo = titulo;
        this.subtitulo = subtitulo;
        this.columnas = columnas;
    }

    @Override
    public String generarReporte(List<List<String>> datos) {
        implementor.iniciarDocumento(titulo, subtitulo);
        implementor.agregarEncabezados(columnas);
        if (datos != null) {
            for (List<String> fila : datos) {
                implementor.agregarFila(fila);
            }
        }
        implementor.finalizarDocumento();
        return implementor.obtenerContenidoGenerado();
    }
}
