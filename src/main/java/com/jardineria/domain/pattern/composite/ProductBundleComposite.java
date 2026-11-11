package com.jardineria.domain.pattern.composite;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Compuesto (Composite) del Patrón GoF Composite.
 * Representa un kit, caja o lote de jardinería que agrupa múltiples productos u otros kits.
 */
public class ProductBundleComposite implements CatalogComponent {
    private final String codigoKit;
    private final String nombreKit;
    private final BigDecimal descuentoKitPorc;
    private final List<CatalogComponent> componentes = new ArrayList<>();

    public ProductBundleComposite(String codigoKit, String nombreKit, BigDecimal descuentoKitPorc) {
        this.codigoKit = codigoKit;
        this.nombreKit = nombreKit;
        this.descuentoKitPorc = (descuentoKitPorc != null) ? descuentoKitPorc : BigDecimal.ZERO;
    }

    public void agregarComponente(CatalogComponent componente) {
        if (componente != null) {
            componentes.add(componente);
        }
    }

    public void removerComponente(CatalogComponent componente) {
        componentes.remove(componente);
    }

    public List<CatalogComponent> getComponentes() {
        return new ArrayList<>(componentes);
    }

    @Override
    public String getNombre() {
        return nombreKit + " (" + codigoKit + ")";
    }

    @Override
    public BigDecimal getPrecio() {
        BigDecimal suma = BigDecimal.ZERO;
        for (CatalogComponent c : componentes) {
            suma = suma.add(c.getPrecio());
        }
        if (descuentoKitPorc.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal descuento = suma.multiply(descuentoKitPorc).setScale(2, RoundingMode.HALF_UP);
            return suma.subtract(descuento);
        }
        return suma.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public int getStockDisponible() {
        if (componentes.isEmpty()) return 0;
        int minStock = Integer.MAX_VALUE;
        for (CatalogComponent c : componentes) {
            minStock = Math.min(minStock, c.getStockDisponible());
        }
        return minStock;
    }

    @Override
    public void renderEstructura(StringBuilder sb, String indent) {
        sb.append(indent)
          .append("📦 [Kit/Paquete Compuesto] ")
          .append(nombreKit)
          .append(" [Cód: ").append(codigoKit).append("]")
          .append(" | Descuento: ").append(descuentoKitPorc.multiply(BigDecimal.valueOf(100))).append("%")
          .append(" | Precio Pack: ").append(getPrecio()).append(" €")
          .append(" | Stock Ensamblable: ").append(getStockDisponible()).append(" packs\n");

        for (CatalogComponent c : componentes) {
            c.renderEstructura(sb, indent + "    ├── ");
        }
    }
}
