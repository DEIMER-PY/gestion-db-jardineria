package com.jardineria.domain.pattern.proxy;

import com.jardineria.domain.model.Producto;
import com.jardineria.domain.repository.ProductoRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Proxy Concreto (GoF Proxy).
 * Actúa como intermediario frente al repositorio real de base de datos MySQL,
 * gestionando una memoria caché en RAM (Lazy Loading), control de auditoría
 * y métricas de desempeño (Hits vs Misses).
 */
public class CachedProductLookupProxy implements ProductLookupPort {
    private final ProductoRepository realSubject;
    private final Map<String, Producto> localCache = new ConcurrentHashMap<>();
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);

    public CachedProductLookupProxy(ProductoRepository realSubject) {
        this.realSubject = realSubject;
    }

    @Override
    public Optional<Producto> buscarPorCodigo(String codigoProducto) {
        if (codigoProducto == null || codigoProducto.isBlank()) {
            return Optional.empty();
        }

        String key = codigoProducto.trim().toUpperCase();

        // 1. Verificar si ya se encuentra en caché (Control de Acceso Inmediato)
        if (localCache.containsKey(key)) {
            cacheHits.incrementAndGet();
            return Optional.of(localCache.get(key));
        }

        // 2. Cache Miss: Delegación protegida al Sujeto Real (Base de Datos MySQL)
        cacheMisses.incrementAndGet();
        Optional<Producto> opt = realSubject.findById(key);
        opt.ifPresent(p -> localCache.put(key, p));

        return opt;
    }

    public void invalidateCache(String codigoProducto) {
        if (codigoProducto != null) {
            localCache.remove(codigoProducto.trim().toUpperCase());
        }
    }

    public void clearCache() {
        localCache.clear();
    }

    public long getCacheHits() {
        return cacheHits.get();
    }

    public long getCacheMisses() {
        return cacheMisses.get();
    }

    public int getCachedEntriesCount() {
        return localCache.size();
    }

    public double getHitRatio() {
        long total = cacheHits.get() + cacheMisses.get();
        if (total == 0) return 0.0;
        return (cacheHits.get() * 100.0) / total;
    }
}
