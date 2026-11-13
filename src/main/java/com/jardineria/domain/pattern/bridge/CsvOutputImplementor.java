package com.jardineria.domain.pattern.bridge;

import java.util.List;

/**
 * Implementador Concreto para salida en formato CSV (separador coma, RFC 4180).
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
        appendRow(columnas);
    }

    @Override
    public void agregarFila(List<String> valores) {
        appendRow(valores);
    }

    private void appendRow(List<String> valores) {
        for (int i = 0; i < valores.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(valores.get(i)));
        }
        sb.append("\n");
    }

    /** Entrecomilla los valores con comas, comillas o saltos de línea (p. ej. importes "3,50"). */
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
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
