package com.jardineria.application.service;

import com.jardineria.application.port.in.ClienteUseCase;
import com.jardineria.domain.exception.BusinessValidationException;
import com.jardineria.domain.exception.DuplicateEntityException;
import com.jardineria.domain.exception.EntityNotFoundException;
import com.jardineria.domain.model.Cliente;
import com.jardineria.domain.repository.ClienteRepository;
import com.jardineria.domain.repository.EmpleadoRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Servicio de aplicación para la gestión de Clientes y políticas de crédito.
 */
public class ClienteService implements ClienteUseCase {
    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;

    public ClienteService(ClienteRepository clienteRepository, EmpleadoRepository empleadoRepository) {
        this.clienteRepository = Objects.requireNonNull(clienteRepository, "El repositorio de clientes no puede ser nulo");
        this.empleadoRepository = empleadoRepository;
    }

    @Override
    public Cliente crearCliente(Cliente cliente) {
        validarCliente(cliente);
        if (clienteRepository.existsById(cliente.getCodigoCliente())) {
            throw new DuplicateEntityException("Cliente", cliente.getCodigoCliente());
        }
        if (cliente.getCodigoEmpleadoRepVentas() != null && empleadoRepository != null) {
            if (!empleadoRepository.existsById(cliente.getCodigoEmpleadoRepVentas())) {
                throw new BusinessValidationException("El representante de ventas con código " +
                        cliente.getCodigoEmpleadoRepVentas() + " no existe");
            }
        }
        return clienteRepository.save(cliente);
    }

    @Override
    public Cliente actualizarCliente(Cliente cliente) {
        validarCliente(cliente);
        if (!clienteRepository.existsById(cliente.getCodigoCliente())) {
            throw new EntityNotFoundException("Cliente", cliente.getCodigoCliente());
        }
        if (cliente.getCodigoEmpleadoRepVentas() != null && empleadoRepository != null) {
            if (!empleadoRepository.existsById(cliente.getCodigoEmpleadoRepVentas())) {
                throw new BusinessValidationException("El representante de ventas con código " +
                        cliente.getCodigoEmpleadoRepVentas() + " no existe");
            }
        }
        return clienteRepository.update(cliente);
    }

    @Override
    public Cliente obtenerPorId(Integer codigoCliente) {
        if (codigoCliente == null || codigoCliente <= 0) {
            throw new BusinessValidationException("El código de cliente debe ser un entero positivo");
        }
        return clienteRepository.findById(codigoCliente)
                .orElseThrow(() -> new EntityNotFoundException("Cliente", codigoCliente));
    }

    @Override
    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    @Override
    public boolean eliminarCliente(Integer codigoCliente) {
        if (!clienteRepository.existsById(codigoCliente)) {
            throw new EntityNotFoundException("Cliente", codigoCliente);
        }
        return clienteRepository.deleteById(codigoCliente);
    }

    @Override
    public List<Cliente> buscarPorPais(String pais) {
        if (pais == null || pais.isBlank()) {
            return listarTodos();
        }
        return clienteRepository.findByPais(pais.trim());
    }

    @Override
    public List<Cliente> buscarPorCiudad(String ciudad) {
        if (ciudad == null || ciudad.isBlank()) {
            return listarTodos();
        }
        return clienteRepository.findByCiudad(ciudad.trim());
    }

    @Override
    public List<Cliente> buscarPorRepresentante(Integer codigoEmpleado) {
        if (codigoEmpleado == null || codigoEmpleado <= 0) {
            throw new BusinessValidationException("El código de representante debe ser válido");
        }
        return clienteRepository.findByRepresentanteVentas(codigoEmpleado);
    }

    @Override
    public long contarClientes() {
        return clienteRepository.count();
    }

    private void validarCliente(Cliente c) {
        if (c == null) {
            throw new BusinessValidationException("El cliente no puede ser nulo");
        }
        if (c.getCodigoCliente() == null || c.getCodigoCliente() <= 0) {
            throw new BusinessValidationException("El código de cliente debe ser positivo");
        }
        if (c.getNombreCliente() == null || c.getNombreCliente().trim().isEmpty()) {
            throw new BusinessValidationException("La razón social o nombre del cliente es obligatorio");
        }
        if (c.getTelefono() == null || c.getTelefono().trim().isEmpty()) {
            throw new BusinessValidationException("El teléfono de contacto es obligatorio");
        }
        if (c.getLineaDireccion1() == null || c.getLineaDireccion1().trim().isEmpty()) {
            throw new BusinessValidationException("La dirección principal es obligatoria");
        }
        if (c.getCiudad() == null || c.getCiudad().trim().isEmpty()) {
            throw new BusinessValidationException("La ciudad es obligatoria");
        }
        if (c.getLimiteCredito() != null && c.getLimiteCredito().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessValidationException("El límite de crédito no puede ser negativo");
        }
    }
}
