package com.jardineria.domain.pattern.bridge;

import java.util.List;

/**
 * Interfaz Implementador (Implementor) del Patrón Bridge (GoF).
 * Define las operaciones primitivas para dar formato y renderizar reportes,
 * desacoplando el tipo de reporte (Abstracción) del formato de salida.
 */
public interface ReportOutputImplementor {
    void iniciarDocumento(String titulo, String subtitulo);
    void agregarEncabezados(List<String> columnas);
    void agregarFila(List<String> valores);
    void finalizarDocumento();
    String obtenerContenidoGenerado();
    String getExtension();
}
