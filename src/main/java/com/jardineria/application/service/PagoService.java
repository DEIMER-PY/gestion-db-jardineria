package com.jardineria.application.service;

import com.jardineria.domain.exception.BusinessValidationException;
import com.jardineria.domain.exception.DuplicateEntityException;
import com.jardineria.domain.exception.EntityNotFoundException;
import com.jardineria.domain.model.Pago;
import com.jardineria.domain.model.PagoId;
import com.jardineria.domain.repository.ClienteRepository;
import com.jardineria.domain.repository.PagoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Servicio de aplicación para la gestión de Pagos, conciliación financiera y métodos de pago.
 */
public class PagoService {
    private final PagoRepository pagoRepository;
    private final ClienteRepository clienteRepository;

    public PagoService(PagoRepository pagoRepository, ClienteRepository clienteRepository) {
        this.pagoRepository = Objects.requireNonNull(pagoRepository, "El repositorio de pagos no puede ser nulo");
        this.clienteRepository = clienteRepository;
    }

    public Pago registrarPago(Pago pago) {
        validarPago(pago);
        PagoId id = pago.getId();
        if (pagoRepository.existsById(id)) {
            throw new DuplicateEntityException("Pago", id);
        }
        if (clienteRepository != null && !clienteRepository.existsById(pago.getCodigoCliente())) {
            throw new BusinessValidationException("El cliente con código " + pago.getCodigoCliente() + " no existe");
        }
        return pagoRepository.save(pago);
    }

    public Pago actualizarPago(Pago pago) {
        validarPago(pago);
        PagoId id = pago.getId();
        if (!pagoRepository.existsById(id)) {
            throw new EntityNotFoundException("Pago", id);
        }
        return pagoRepository.update(pago);
    }

    public Pago obtenerPorId(PagoId id) {
        if (id == null) {
            throw new BusinessValidationException("El identificador del pago no puede ser nulo");
        }
        return pagoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pago", id));
    }

    public List<Pago> listarTodos() {
        return pagoRepository.findAll();
    }

    public boolean eliminarPago(PagoId id) {
        if (!pagoRepository.existsById(id)) {
            throw new EntityNotFoundException("Pago", id);
        }
        return pagoRepository.deleteById(id);
    }

    public List<Pago> buscarPorCliente(Integer codigoCliente) {
        if (codigoCliente == null || codigoCliente <= 0) {
            throw new BusinessValidationException("El código de cliente debe ser positivo");
        }
        return pagoRepository.findByCliente(codigoCliente);
    }

    public List<Pago> buscarPorFormaPago(String formaPago) {
        if (formaPago == null || formaPago.isBlank()) {
            return listarTodos();
        }
        return pagoRepository.findByFormaPago(formaPago.trim());
    }

    public BigDecimal calcularTotalIngresos() {
        return pagoRepository.totalPagos();
    }

    public long contarPagos() {
        return pagoRepository.count();
    }

    private void validarPago(Pago p) {
        if (p == null) {
            throw new BusinessValidationException("El pago no puede ser nulo");
        }
        if (p.getCodigoCliente() == null || p.getCodigoCliente() <= 0) {
            throw new BusinessValidationException("El código de cliente es obligatorio");
        }
        if (p.getIdTransaccion() == null || p.getIdTransaccion().trim().isEmpty()) {
            throw new BusinessValidationException("El identificador de transacción es obligatorio");
        }
        if (p.getFormaPago() == null || p.getFormaPago().trim().isEmpty()) {
            throw new BusinessValidationException("La forma de pago es obligatoria");
        }
        if (p.getTotal() == null || p.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("El importe del pago debe ser mayor a cero");
        }
        if (p.getFechaPago() == null) {
            p.setFechaPago(LocalDate.now());
        }
    }
}
