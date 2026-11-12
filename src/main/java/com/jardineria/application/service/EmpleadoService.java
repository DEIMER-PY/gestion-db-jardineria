package com.jardineria.application.service;

import com.jardineria.application.port.in.EmpleadoUseCase;
import com.jardineria.domain.exception.BusinessValidationException;
import com.jardineria.domain.exception.DuplicateEntityException;
import com.jardineria.domain.exception.EntityNotFoundException;
import com.jardineria.domain.model.Empleado;
import com.jardineria.domain.repository.EmpleadoRepository;
import com.jardineria.domain.repository.OficinaRepository;

import java.util.List;
import java.util.Objects;

/**
 * Servicio de aplicación para la gestión del ciclo de vida y reglas de negocio de Empleados.
 */
public class EmpleadoService implements EmpleadoUseCase {
    private final EmpleadoRepository empleadoRepository;
    private final OficinaRepository oficinaRepository;

    public EmpleadoService(EmpleadoRepository empleadoRepository, OficinaRepository oficinaRepository) {
        this.empleadoRepository = Objects.requireNonNull(empleadoRepository, "El repositorio de empleados no puede ser nulo");
        this.oficinaRepository = oficinaRepository;
    }

    @Override
    public Empleado crearEmpleado(Empleado emp) {
        validarEmpleado(emp);
        if (empleadoRepository.existsById(emp.getCodigoEmpleado())) {
            throw new DuplicateEntityException("Empleado", emp.getCodigoEmpleado());
        }
        if (oficinaRepository != null && !oficinaRepository.existsById(emp.getCodigoOficina())) {
            throw new BusinessValidationException("La oficina asignada '" + emp.getCodigoOficina() + "' no existe");
        }
        if (emp.getCodigoJefe() != null && !empleadoRepository.existsById(emp.getCodigoJefe())) {
            throw new BusinessValidationException("El empleado jefe asignado con código " + emp.getCodigoJefe() + " no existe");
        }
        return empleadoRepository.save(emp);
    }

    @Override
    public Empleado actualizarEmpleado(Empleado emp) {
        validarEmpleado(emp);
        if (!empleadoRepository.existsById(emp.getCodigoEmpleado())) {
            throw new EntityNotFoundException("Empleado", emp.getCodigoEmpleado());
        }
        if (oficinaRepository != null && !oficinaRepository.existsById(emp.getCodigoOficina())) {
            throw new BusinessValidationException("La oficina asignada '" + emp.getCodigoOficina() + "' no existe");
        }
        if (emp.getCodigoJefe() != null) {
            if (emp.getCodigoJefe().equals(emp.getCodigoEmpleado())) {
                throw new BusinessValidationException("Un empleado no puede ser su propio jefe");
            }
            if (!empleadoRepository.existsById(emp.getCodigoJefe())) {
                throw new BusinessValidationException("El empleado jefe asignado con código " + emp.getCodigoJefe() + " no existe");
            }
        }
        return empleadoRepository.update(emp);
    }

    @Override
    public Empleado obtenerPorId(Integer codigoEmpleado) {
        if (codigoEmpleado == null || codigoEmpleado <= 0) {
            throw new BusinessValidationException("El código de empleado debe ser un entero positivo");
        }
        return empleadoRepository.findById(codigoEmpleado)
                .orElseThrow(() -> new EntityNotFoundException("Empleado", codigoEmpleado));
    }

    @Override
    public List<Empleado> listarTodos() {
        return empleadoRepository.findAll();
    }

    @Override
    public boolean eliminarEmpleado(Integer codigoEmpleado) {
        if (!empleadoRepository.existsById(codigoEmpleado)) {
            throw new EntityNotFoundException("Empleado", codigoEmpleado);
        }
        List<Empleado> subordinados = empleadoRepository.findByJefe(codigoEmpleado);
        if (!subordinados.isEmpty()) {
            throw new BusinessValidationException("No se puede eliminar el empleado porque tiene subordinados a su cargo");
        }
        return empleadoRepository.deleteById(codigoEmpleado);
    }

    @Override
    public List<Empleado> buscarPorOficina(String codigoOficina) {
        if (codigoOficina == null || codigoOficina.isBlank()) {
            return listarTodos();
        }
        return empleadoRepository.findByOficina(codigoOficina.trim());
    }

    @Override
    public List<Empleado> buscarPorJefe(Integer codigoJefe) {
        if (codigoJefe == null) {
            return listarDirectores();
        }
        return empleadoRepository.findByJefe(codigoJefe);
    }

    @Override
    public List<Empleado> listarDirectores() {
        return empleadoRepository.findDirectores();
    }

    @Override
    public List<Empleado> listarRepresentantesVentas() {
        return empleadoRepository.findRepresentantesVentas();
    }

    @Override
    public long contarEmpleados() {
        return empleadoRepository.count();
    }

    private void validarEmpleado(Empleado emp) {
        if (emp == null) {
            throw new BusinessValidationException("El empleado no puede ser nulo");
        }
        if (emp.getCodigoEmpleado() == null || emp.getCodigoEmpleado() <= 0) {
            throw new BusinessValidationException("El código de empleado debe ser positivo");
        }
        if (emp.getNombre() == null || emp.getNombre().trim().isEmpty()) {
            throw new BusinessValidationException("El nombre del empleado es obligatorio");
        }
        if (emp.getApellido1() == null || emp.getApellido1().trim().isEmpty()) {
            throw new BusinessValidationException("El primer apellido es obligatorio");
        }
        if (emp.getEmail() == null || !emp.getEmail().contains("@")) {
            throw new BusinessValidationException("El formato del correo electrónico es inválido");
        }
        if (emp.getExtension() == null || emp.getExtension().trim().isEmpty()) {
            throw new BusinessValidationException("La extensión telefónica es obligatoria");
        }
        if (emp.getCodigoOficina() == null || emp.getCodigoOficina().trim().isEmpty()) {
            throw new BusinessValidationException("El código de oficina asignado es obligatorio");
        }
    }
}
