package com.jardineria.domain.pattern.flyweight;

/**
 * Estado Intrínseco (Flyweight Compartido).
 * Almacena los datos invariantes y pesados de la familia botánica
 * para reutilizarlos en miles de productos sin duplicar consumo en memoria.
 *
 * Arquitectura Hexagonal: Desacoplado al 100% de librerías de UI (AWT/Swing).
 */
public record GamaFlyweight(
        String nombreGama,
        String descripcionGenerica,
        String colorHex,
        String emojiVisual,
        String bannerUrl
) {
    public String formatearDetalleExtrinseco(String codigoProducto, int stock, String precioVenta) {
        return String.format("%s [%s] %s | Stock: %d uds | PVP: %s € | Color: %s",
                emojiVisual, nombreGama, codigoProducto, stock, precioVenta, colorHex);
    }
}
