package com.jardineria.domain.pattern.bridge;

import java.util.List;

/**
 * Abstracción (Abstraction) del Patrón Bridge (GoF).
 * Mantiene la referencia al Implementor y define el flujo de alto nivel del reporte.
 */
public abstract class ReportBridge {
    protected final ReportOutputImplementor implementor;

    public ReportBridge(ReportOutputImplementor implementor) {
        this.implementor = implementor;
    }

    public abstract String generarReporte(List<List<String>> datos);

    public String getExtension() {
        return implementor.getExtension();
    }
}
