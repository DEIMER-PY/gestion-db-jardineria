package com.jardineria.domain.pattern.bridge;

import java.util.List;

/**
 * Implementador Concreto para salida en HTML elegante con estilos modernos.
 */
public class HtmlOutputImplementor implements ReportOutputImplementor {
    private final StringBuilder sb = new StringBuilder();

    @Override
    public void iniciarDocumento(String titulo, String subtitulo) {
        sb.append("<!DOCTYPE html>\n<html lang=\"es\">\n<head>\n")
          .append("<meta charset=\"UTF-8\"><title>").append(titulo).append("</title>\n")
          .append("<style>\n")
          .append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 30px; background: #0f172a; color: #f8fafc; }\n")
          .append("h1 { color: #34d399; margin-bottom: 4px; }\n")
          .append("p.sub { color: #94a3b8; font-size: 14px; margin-top: 0; margin-bottom: 24px; }\n")
          .append("table { width: 100%; border-collapse: collapse; background: #1e293b; border-radius: 8px; overflow: hidden; }\n")
          .append("th, td { padding: 12px 16px; text-align: left; border-bottom: 1px solid #334155; font-size: 13px; }\n")
          .append("th { background: #0f766e; color: #fff; text-transform: uppercase; font-size: 11px; letter-spacing: 0.05em; }\n")
          .append("tr:hover { background: #334155; }\n")
          .append("</style>\n</head>\n<body>\n")
          .append("<h1>").append(titulo).append("</h1>\n")
          .append("<p class=\"sub\">").append(subtitulo != null ? subtitulo : "").append("</p>\n")
          .append("<table>\n");
    }

    @Override
    public void agregarEncabezados(List<String> columnas) {
        sb.append("  <thead>\n    <tr>\n");
        for (String col : columnas) {
            sb.append("      <th>").append(col).append("</th>\n");
        }
        sb.append("    </tr>\n  </thead>\n  <tbody>\n");
    }

    @Override
    public void agregarFila(List<String> valores) {
        sb.append("    <tr>\n");
        for (String val : valores) {
            sb.append("      <td>").append(val).append("</td>\n");
        }
        sb.append("    </tr>\n");
    }

    @Override
    public void finalizarDocumento() {
        sb.append("  </tbody>\n</table>\n<footer style=\"margin-top: 24px; font-size: 12px; color: #64748b;\">Generado automáticamente por Jardinería Enterprise Suite</footer>\n</body>\n</html>");
    }

    @Override
    public String obtenerContenidoGenerado() {
        return sb.toString();
    }

    @Override
    public String getExtension() {
        return "html";
    }
}
