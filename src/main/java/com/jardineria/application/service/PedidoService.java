package com.jardineria.application.service;

import com.jardineria.domain.exception.BusinessValidationException;
import com.jardineria.domain.exception.DuplicateEntityException;
import com.jardineria.domain.exception.EntityNotFoundException;
import com.jardineria.domain.model.Pedido;
import com.jardineria.domain.repository.ClienteRepository;
import com.jardineria.domain.repository.DetallePedidoRepository;
import com.jardineria.domain.repository.PedidoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Servicio de aplicación para la gestión del ciclo de vida y estados de los Pedidos.
 */
public class PedidoService {
    private static final Set<String> ESTADOS_VALIDOS = Set.of("PENDIENTE", "ENTREGADO", "RECHAZADO");

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final DetallePedidoRepository detalleRepository;

    public PedidoService(PedidoRepository pedidoRepository,
                         ClienteRepository clienteRepository,
                         DetallePedidoRepository detalleRepository) {
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository, "El repositorio de pedidos no puede ser nulo");
        this.clienteRepository = clienteRepository;
        this.detalleRepository = detalleRepository;
    }

    public Pedido crearPedido(Pedido pedido) {
        validarPedido(pedido);
        if (pedidoRepository.existsById(pedido.getCodigoPedido())) {
            throw new DuplicateEntityException("Pedido", pedido.getCodigoPedido());
        }
        if (clienteRepository != null && !clienteRepository.existsById(pedido.getCodigoCliente())) {
            throw new BusinessValidationException("El cliente con código " + pedido.getCodigoCliente() + " no existe");
        }
        return pedidoRepository.save(pedido);
    }

    public Pedido actualizarPedido(Pedido pedido) {
        validarPedido(pedido);
        if (!pedidoRepository.existsById(pedido.getCodigoPedido())) {
            throw new EntityNotFoundException("Pedido", pedido.getCodigoPedido());
        }
        if (clienteRepository != null && !clienteRepository.existsById(pedido.getCodigoCliente())) {
            throw new BusinessValidationException("El cliente con código " + pedido.getCodigoCliente() + " no existe");
        }
        return pedidoRepository.update(pedido);
    }

    public Pedido cambiarEstado(Integer codigoPedido, String nuevoEstado, LocalDate fechaEntrega) {
        Pedido pedido = obtenerPorId(codigoPedido);
        if (nuevoEstado == null || !ESTADOS_VALIDOS.contains(nuevoEstado.trim().toUpperCase())) {
            throw new BusinessValidationException("Estado inválido. Valores aceptados: Pendiente, Entregado, Rechazado");
        }
        pedido.setEstado(nuevoEstado.trim());
        if ("ENTREGADO".equalsIgnoreCase(nuevoEstado)) {
            pedido.setFechaEntrega(fechaEntrega != null ? fechaEntrega : LocalDate.now());
        }
        return pedidoRepository.update(pedido);
    }

    public Pedido obtenerPorId(Integer codigoPedido) {
        if (codigoPedido == null || codigoPedido <= 0) {
            throw new BusinessValidationException("El código de pedido debe ser un entero positivo");
        }
        return pedidoRepository.findById(codigoPedido)
                .orElseThrow(() -> new EntityNotFoundException("Pedido", codigoPedido));
    }

    public List<Pedido> listarTodos() {
        return pedidoRepository.findAll();
    }

    public boolean eliminarPedido(Integer codigoPedido) {
        if (!pedidoRepository.existsById(codigoPedido)) {
            throw new EntityNotFoundException("Pedido", codigoPedido);
        }
        if (detalleRepository != null) {
            detalleRepository.deleteByPedido(codigoPedido);
        }
        return pedidoRepository.deleteById(codigoPedido);
    }

    public List<Pedido> buscarPorCliente(Integer codigoCliente) {
        if (codigoCliente == null || codigoCliente <= 0) {
            throw new BusinessValidationException("El código de cliente debe ser positivo");
        }
        return pedidoRepository.findByCliente(codigoCliente);
    }

    public List<Pedido> buscarPorEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return listarTodos();
        }
        return pedidoRepository.findByEstado(estado.trim());
    }

    public long contarPorEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return 0;
        }
        return pedidoRepository.countByEstado(estado.trim());
    }

    public long contarPedidos() {
        return pedidoRepository.count();
    }

    private void validarPedido(Pedido p) {
        if (p == null) {
            throw new BusinessValidationException("El pedido no puede ser nulo");
        }
        if (p.getCodigoPedido() == null || p.getCodigoPedido() <= 0) {
            throw new BusinessValidationException("El código de pedido debe ser positivo");
        }
        if (p.getFechaPedido() == null) {
            throw new BusinessValidationException("La fecha del pedido es obligatoria");
        }
        if (p.getFechaEsperada() == null) {
            throw new BusinessValidationException("La fecha esperada de entrega es obligatoria");
        }
        if (p.getFechaEsperada().isBefore(p.getFechaPedido())) {
            throw new BusinessValidationException("La fecha esperada no puede ser anterior a la fecha de realización del pedido");
        }
        if (p.getEstado() == null || !ESTADOS_VALIDOS.contains(p.getEstado().trim().toUpperCase())) {
            throw new BusinessValidationException("Estado inválido. Estados permitidos: Pendiente, Entregado, Rechazado");
        }
        if (p.getCodigoCliente() == null || p.getCodigoCliente() <= 0) {
            throw new BusinessValidationException("El código de cliente asociado es obligatorio");
        }
    }
}
