package com.jardineria.application.service;

import com.jardineria.application.port.in.GamaProductoUseCase;
import com.jardineria.domain.exception.BusinessValidationException;
import com.jardineria.domain.exception.DuplicateEntityException;
import com.jardineria.domain.exception.EntityNotFoundException;
import com.jardineria.domain.model.GamaProducto;
import com.jardineria.domain.repository.GamaProductoRepository;

import java.util.List;
import java.util.Objects;

/**
 * Servicio de aplicación para la gestión de Gamas de Productos.
 */
public class GamaProductoService implements GamaProductoUseCase {
    private final GamaProductoRepository repository;

    public GamaProductoService(GamaProductoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "El repositorio de gamas no puede ser nulo");
    }

    @Override
    public GamaProducto crearGama(GamaProducto gama) {
        validarGama(gama);
        if (repository.existsById(gama.getGama())) {
            throw new DuplicateEntityException("Gama de Producto", gama.getGama());
        }
        return repository.save(gama);
    }

    @Override
    public GamaProducto actualizarGama(GamaProducto gama) {
        validarGama(gama);
        if (!repository.existsById(gama.getGama())) {
            throw new EntityNotFoundException("Gama de Producto", gama.getGama());
        }
        return repository.update(gama);
    }

    @Override
    public GamaProducto obtenerPorId(String gama) {
        if (gama == null || gama.isBlank()) {
            throw new BusinessValidationException("El identificador de gama es obligatorio");
        }
        return repository.findById(gama)
                .orElseThrow(() -> new EntityNotFoundException("Gama de Producto", gama));
    }

    @Override
    public List<GamaProducto> listarTodas() {
        return repository.findAll();
    }

    @Override
    public boolean eliminarGama(String gama) {
        if (!repository.existsById(gama)) {
            throw new EntityNotFoundException("Gama de Producto", gama);
        }
        return repository.deleteById(gama);
    }

    @Override
    public long contarGamas() {
        return repository.count();
    }

    private void validarGama(GamaProducto g) {
        if (g == null) {
            throw new BusinessValidationException("La gama no puede ser nula");
        }
        if (g.getGama() == null || g.getGama().trim().isEmpty()) {
            throw new BusinessValidationException("El nombre de la gama es obligatorio");
        }
    }
}
