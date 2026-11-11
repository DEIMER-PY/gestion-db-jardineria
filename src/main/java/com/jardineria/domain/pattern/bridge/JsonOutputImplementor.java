package com.jardineria.domain.pattern.bridge;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementador Concreto para salida en formato JSON estructurado.
 */
public class JsonOutputImplementor implements ReportOutputImplementor {
    private String titulo;
    private List<String> headers = new ArrayList<>();
    private final List<List<String>> rows = new ArrayList<>();

    @Override
    public void iniciarDocumento(String titulo, String subtitulo) {
        this.titulo = titulo;
    }

    @Override
    public void agregarEncabezados(List<String> columnas) {
        this.headers = new ArrayList<>(columnas);
    }

    @Override
    public void agregarFila(List<String> valores) {
        this.rows.add(new ArrayList<>(valores));
    }

    @Override
    public void finalizarDocumento() {
        // Nada adicional
    }

    @Override
    public String obtenerContenidoGenerado() {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"titulo\": \"").append(titulo).append("\",\n")
            .append("  \"totalRegistros\": ").append(rows.size()).append(",\n")
            .append("  \"datos\": [\n");

        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            json.append("    {");
            for (int j = 0; j < Math.min(headers.size(), row.size()); j++) {
                json.append("\"").append(headers.get(j)).append("\": \"")
                    .append(row.get(j).replace("\"", "\\\"")).append("\"");
                if (j < Math.min(headers.size(), row.size()) - 1) {
                    json.append(", ");
                }
            }
            json.append("}");
            if (i < rows.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ]\n}");
        return json.toString();
    }

    @Override
    public String getExtension() {
        return "json";
    }
}
