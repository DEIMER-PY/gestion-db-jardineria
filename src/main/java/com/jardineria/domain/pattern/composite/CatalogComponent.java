package com.jardineria.domain.pattern.composite;

import java.math.BigDecimal;

/**
 * Componente base del Patrón Composite (GoF).
 * Define la interfaz uniforme tanto para productos individuales (Hojas)
 * como para paquetes / kits de jardinería compuestos (Compuestos).
 */
public interface CatalogComponent {
    String getNombre();
    BigDecimal getPrecio();
    int getStockDisponible();
    void renderEstructura(StringBuilder sb, String indent);
}
