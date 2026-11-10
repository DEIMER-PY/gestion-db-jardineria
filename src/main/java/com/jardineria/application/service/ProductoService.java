package com.jardineria.application.service;

import com.jardineria.domain.exception.BusinessValidationException;
import com.jardineria.domain.exception.DuplicateEntityException;
import com.jardineria.domain.exception.EntityNotFoundException;
import com.jardineria.domain.model.Producto;
import com.jardineria.domain.repository.GamaProductoRepository;
import com.jardineria.domain.repository.ProductoRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Servicio de aplicación para la gestión del catálogo de Productos, stock y precios.
 */
public class ProductoService {
    private final ProductoRepository productoRepository;
    private final GamaProductoRepository gamaRepository;

    public ProductoService(ProductoRepository productoRepository, GamaProductoRepository gamaRepository) {
        this.productoRepository = Objects.requireNonNull(productoRepository, "El repositorio de productos no puede ser nulo");
        this.gamaRepository = gamaRepository;
    }

    public Producto crearProducto(Producto prod) {
        validarProducto(prod);
        if (productoRepository.existsById(prod.getCodigoProducto())) {
            throw new DuplicateEntityException("Producto", prod.getCodigoProducto());
        }
        if (gamaRepository != null && !gamaRepository.existsById(prod.getGama())) {
            throw new BusinessValidationException("La gama de producto '" + prod.getGama() + "' no existe");
        }
        return productoRepository.save(prod);
    }

    public Producto actualizarProducto(Producto prod) {
        validarProducto(prod);
        if (!productoRepository.existsById(prod.getCodigoProducto())) {
            throw new EntityNotFoundException("Producto", prod.getCodigoProducto());
        }
        if (gamaRepository != null && !gamaRepository.existsById(prod.getGama())) {
            throw new BusinessValidationException("La gama de producto '" + prod.getGama() + "' no existe");
        }
        return productoRepository.update(prod);
    }

    public Producto obtenerPorId(String codigoProducto) {
        if (codigoProducto == null || codigoProducto.isBlank()) {
            throw new BusinessValidationException("El código de producto es obligatorio");
        }
        return productoRepository.findById(codigoProducto.trim())
                .orElseThrow(() -> new EntityNotFoundException("Producto", codigoProducto));
    }

    public List<Producto> listarTodos() {
        return productoRepository.findAll();
    }

    public boolean eliminarProducto(String codigoProducto) {
        if (!productoRepository.existsById(codigoProducto)) {
            throw new EntityNotFoundException("Producto", codigoProducto);
        }
        return productoRepository.deleteById(codigoProducto);
    }

    public List<Producto> buscarPorGama(String gama) {
        if (gama == null || gama.isBlank()) {
            return listarTodos();
        }
        return productoRepository.findByGama(gama.trim());
    }

    public List<Producto> buscarBajoStock(int umbral) {
        if (umbral < 0) {
            umbral = 10;
        }
        return productoRepository.findBajoStock(umbral);
    }

    public List<Producto> buscarPorNombre(String termino) {
        if (termino == null || termino.isBlank()) {
            return listarTodos();
        }
        return productoRepository.searchByNombre(termino.trim());
    }

    public long contarProductos() {
        return productoRepository.count();
    }

    private void validarProducto(Producto p) {
        if (p == null) {
            throw new BusinessValidationException("El producto no puede ser nulo");
        }
        if (p.getCodigoProducto() == null || p.getCodigoProducto().trim().isEmpty()) {
            throw new BusinessValidationException("El código de producto es obligatorio");
        }
        if (p.getNombre() == null || p.getNombre().trim().isEmpty()) {
            throw new BusinessValidationException("El nombre del producto es obligatorio");
        }
        if (p.getGama() == null || p.getGama().trim().isEmpty()) {
            throw new BusinessValidationException("La gama del producto es obligatoria");
        }
        if (p.getCantidadEnStock() == null || p.getCantidadEnStock() < 0) {
            throw new BusinessValidationException("La cantidad en stock no puede ser negativa");
        }
        if (p.getPrecioVenta() == null || p.getPrecioVenta().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("El precio de venta debe ser mayor a cero");
        }
        if (p.getPrecioProveedor() != null && p.getPrecioProveedor().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessValidationException("El precio del proveedor no puede ser negativo");
        }
    }
}
