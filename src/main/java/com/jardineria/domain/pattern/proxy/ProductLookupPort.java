package com.jardineria.domain.pattern.proxy;

import com.jardineria.domain.model.Producto;

import java.util.Optional;

/**
 * Sujeto (Subject) del Patrón Proxy (GoF).
 * Define la interfaz común entre el Objeto Real (repositorio persistente)
 * y el Intermediario (Proxy).
 */
public interface ProductLookupPort {
    Optional<Producto> buscarPorCodigo(String codigoProducto);
}
