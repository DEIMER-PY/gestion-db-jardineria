package com.jardineria.domain.pattern.composite;

import com.jardineria.domain.model.Producto;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Hoja (Leaf) del Patrón Composite.
 * Representa un artículo individual del inventario que no contiene hijos.
 */
public class ProductLeaf implements CatalogComponent {
    private final Producto producto;

    public ProductLeaf(Producto producto) {
        this.producto = Objects.requireNonNull(producto, "El producto no puede ser nulo");
    }

    @Override
    public String getNombre() {
        return producto.getNombre();
    }

    @Override
    public BigDecimal getPrecio() {
        return producto.getPrecioVenta() != null ? producto.getPrecioVenta() : BigDecimal.ZERO;
    }

    @Override
    public int getStockDisponible() {
        return producto.getCantidadEnStock();
    }

    @Override
    public void renderEstructura(StringBuilder sb, String indent) {
        sb.append(indent)
          .append("🍃 [Producto Individual] ")
          .append(producto.getCodigoProducto())
          .append(" - ")
          .append(producto.getNombre())
          .append(" | Stock: ")
          .append(producto.getCantidadEnStock())
          .append(" | Precio: ")
          .append(getPrecio())
          .append(" €\n");
    }

    public Producto getProducto() {
        return producto;
    }
}
