package com.jardineria.domain.pattern.flyweight;

import java.awt.Color;

/**
 * Estado Intrínseco (Flyweight Compartido).
 * Almacena los datos invariantes y pesados de la familia botánica
 * para reutilizarlos en miles de productos sin duplicar consumo en memoria.
 */
public record GamaFlyweight(
        String nombreGama,
        String descripcionGenerica,
        Color colorTema,
        String emojiVisual,
        String bannerUrl
) {
    public String formatearDetalleExtrinseco(String codigoProducto, int stock, String precioVenta) {
        return String.format("%s [%s] %s | Stock: %d uds | PVP: %s € | Tema: RGB(%d,%d,%d)",
                emojiVisual, nombreGama, codigoProducto, stock, precioVenta,
                colorTema.getRed(), colorTema.getGreen(), colorTema.getBlue());
    }
}
