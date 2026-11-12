package com.jardineria.infrastructure.adapter.in.desktop.views.patterns;

import com.jardineria.application.facade.JardineriaBusinessFacade;
import com.jardineria.domain.model.Cliente;
import com.jardineria.domain.model.Producto;
import com.jardineria.domain.pattern.proxy.CachedProductLookupProxy;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Datos compartidos por las demos: catálogo y clientes leídos de MySQL, más los
 * colaboradores opcionales (fachada y proxy) que aporta el composition root.
 */
record PatternData(
        List<Producto> productos,
        List<Cliente> clientes,
        JardineriaBusinessFacade facade,
        CachedProductLookupProxy productLookup
) {
    PatternData {
        productos = productos.stream()
                .sorted(Comparator.comparing(Producto::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
        clientes = clientes.stream()
                .sorted(Comparator.comparing(Cliente::getNombreCliente, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Primer producto de la gama indicada (sin distinguir tildes ni mayúsculas). */
    Optional<Producto> firstOfGama(String gama) {
        String g = DemoUi.normalize(gama);
        return productos.stream()
                .filter(p -> DemoUi.normalize(p.getGama()).startsWith(g))
                .filter(p -> p.getCantidadEnStock() != null && p.getCantidadEnStock() > 0)
                .findFirst();
    }
}
