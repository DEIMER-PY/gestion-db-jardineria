package com.jardineria.domain.pattern.bridge;

import java.util.List;

/**
 * Implementador Concreto para salida en formato CSV.
 */
public class CsvOutputImplementor implements ReportOutputImplementor {
    private final StringBuilder sb = new StringBuilder();

    @Override
    public void iniciarDocumento(String titulo, String subtitulo) {
        sb.append("# ").append(titulo).append("\n");
        if (subtitulo != null && !subtitulo.isBlank()) {
            sb.append("# ").append(subtitulo).append("\n");
        }
    }

    @Override
    public void agregarEncabezados(List<String> columnas) {
        sb.append(String.join(",", columnas)).append("\n");
    }

    @Override
    public void agregarFila(List<String> valores) {
        sb.append(String.join(",", valores)).append("\n");
    }

    @Override
    public void finalizarDocumento() {
        // Nada que cerrar en CSV
    }

    @Override
    public String obtenerContenidoGenerado() {
        return sb.toString();
    }

    @Override
    public String getExtension() {
        return "csv";
    }
}
