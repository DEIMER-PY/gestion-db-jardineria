package com.jardineria.application.service;

import com.jardineria.application.port.in.OficinaUseCase;
import com.jardineria.domain.exception.BusinessValidationException;
import com.jardineria.domain.exception.DuplicateEntityException;
import com.jardineria.domain.exception.EntityNotFoundException;
import com.jardineria.domain.model.Oficina;
import com.jardineria.domain.repository.OficinaRepository;

import java.util.List;
import java.util.Objects;

/**
 * Servicio de aplicación para la gestión del ciclo de vida y reglas de negocio de Oficinas.
 */
public class OficinaService implements OficinaUseCase {
    private final OficinaRepository repository;

    public OficinaService(OficinaRepository repository) {
        this.repository = Objects.requireNonNull(repository, "El repositorio de oficina no puede ser nulo");
    }

    @Override
    public Oficina crearOficina(Oficina oficina) {
        validarDatosOficina(oficina);
        if (repository.existsById(oficina.getCodigoOficina())) {
            throw new DuplicateEntityException("Oficina", oficina.getCodigoOficina());
        }
        return repository.save(oficina);
    }

    @Override
    public Oficina actualizarOficina(Oficina oficina) {
        validarDatosOficina(oficina);
        if (!repository.existsById(oficina.getCodigoOficina())) {
            throw new EntityNotFoundException("Oficina", oficina.getCodigoOficina());
        }
        return repository.update(oficina);
    }

    @Override
    public Oficina obtenerPorId(String codigoOficina) {
        if (codigoOficina == null || codigoOficina.isBlank()) {
            throw new BusinessValidationException("El código de oficina es obligatorio");
        }
        return repository.findById(codigoOficina)
                .orElseThrow(() -> new EntityNotFoundException("Oficina", codigoOficina));
    }

    @Override
    public List<Oficina> listarTodas() {
        return repository.findAll();
    }

    @Override
    public boolean eliminarOficina(String codigoOficina) {
        if (!repository.existsById(codigoOficina)) {
            throw new EntityNotFoundException("Oficina", codigoOficina);
        }
        return repository.deleteById(codigoOficina);
    }

    @Override
    public List<Oficina> buscarPorPais(String pais) {
        if (pais == null || pais.isBlank()) {
            return listarTodas();
        }
        return repository.findByPais(pais.trim());
    }

    @Override
    public List<Oficina> buscarPorCiudad(String ciudad) {
        if (ciudad == null || ciudad.isBlank()) {
            return listarTodas();
        }
        return repository.findByCiudad(ciudad.trim());
    }

    @Override
    public long contarOficinas() {
        return repository.count();
    }

    private void validarDatosOficina(Oficina o) {
        if (o == null) {
            throw new BusinessValidationException("La oficina no puede ser nula");
        }
        if (o.getCodigoOficina() == null || o.getCodigoOficina().trim().isEmpty()) {
            throw new BusinessValidationException("El código de oficina es obligatorio");
        }
        if (o.getCiudad() == null || o.getCiudad().trim().isEmpty()) {
            throw new BusinessValidationException("La ciudad es obligatoria");
        }
        if (o.getPais() == null || o.getPais().trim().isEmpty()) {
            throw new BusinessValidationException("El país es obligatorio");
        }
        if (o.getCodigoPostal() == null || o.getCodigoPostal().trim().isEmpty()) {
            throw new BusinessValidationException("El código postal es obligatorio");
        }
        if (o.getTelefono() == null || o.getTelefono().trim().isEmpty()) {
            throw new BusinessValidationException("El teléfono es obligatorio");
        }
        if (o.getLineaDireccion1() == null || o.getLineaDireccion1().trim().isEmpty()) {
            throw new BusinessValidationException("La línea de dirección 1 es obligatoria");
        }
    }
}
