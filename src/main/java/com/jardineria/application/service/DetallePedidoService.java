package com.jardineria.application.service;

import com.jardineria.application.port.in.DetallePedidoUseCase;
import com.jardineria.domain.exception.BusinessValidationException;
import com.jardineria.domain.exception.DuplicateEntityException;
import com.jardineria.domain.exception.EntityNotFoundException;
import com.jardineria.domain.model.DetallePedido;
import com.jardineria.domain.model.DetallePedidoId;
import com.jardineria.domain.model.Producto;
import com.jardineria.domain.repository.DetallePedidoRepository;
import com.jardineria.domain.repository.PedidoRepository;
import com.jardineria.domain.repository.ProductoRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Servicio de aplicación para la gestión de líneas de Detalle de Pedidos y control de stock.
 */
public class DetallePedidoService implements DetallePedidoUseCase {
    private final DetallePedidoRepository detalleRepository;
    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;

    public DetallePedidoService(DetallePedidoRepository detalleRepository,
                                PedidoRepository pedidoRepository,
                                ProductoRepository productoRepository) {
        this.detalleRepository = Objects.requireNonNull(detalleRepository, "El repositorio de detalles no puede ser nulo");
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
    }

    @Override
    public DetallePedido agregarDetalle(DetallePedido detalle) {
        validarDetalle(detalle);
        DetallePedidoId id = detalle.getId();
        if (detalleRepository.existsById(id)) {
            throw new DuplicateEntityException("Detalle de Pedido", id);
        }
        if (pedidoRepository != null && !pedidoRepository.existsById(detalle.getCodigoPedido())) {
            throw new BusinessValidationException("El pedido con código " + detalle.getCodigoPedido() + " no existe");
        }
        if (productoRepository != null) {
            Producto prod = productoRepository.findById(detalle.getCodigoProducto())
                    .orElseThrow(() -> new BusinessValidationException("El producto " + detalle.getCodigoProducto() + " no existe"));
            if (prod.getCantidadEnStock() < detalle.getCantidad()) {
                throw new BusinessValidationException("Stock insuficiente para el producto '" + prod.getNombre() +
                        "'. Disponible: " + prod.getCantidadEnStock() + ", solicitado: " + detalle.getCantidad());
            }
            // Disminuir stock en almacén
            prod.setCantidadEnStock(prod.getCantidadEnStock() - detalle.getCantidad());
            productoRepository.update(prod);
        }
        return detalleRepository.save(detalle);
    }

    @Override
    public DetallePedido actualizarDetalle(DetallePedido detalle) {
        validarDetalle(detalle);
        DetallePedidoId id = detalle.getId();
        if (!detalleRepository.existsById(id)) {
            throw new EntityNotFoundException("Detalle de Pedido", id);
        }
        return detalleRepository.update(detalle);
    }

    @Override
    public DetallePedido obtenerPorId(DetallePedidoId id) {
        if (id == null) {
            throw new BusinessValidationException("El ID compuesto no puede ser nulo");
        }
        return detalleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Detalle de Pedido", id));
    }

    @Override
    public List<DetallePedido> listarPorPedido(Integer codigoPedido) {
        if (codigoPedido == null || codigoPedido <= 0) {
            throw new BusinessValidationException("El código de pedido debe ser positivo");
        }
        return detalleRepository.findByPedido(codigoPedido);
    }

    @Override
    public boolean eliminarDetalle(DetallePedidoId id) {
        if (!detalleRepository.existsById(id)) {
            throw new EntityNotFoundException("Detalle de Pedido", id);
        }
        return detalleRepository.deleteById(id);
    }

    @Override
    public long contarDetalles() {
        return detalleRepository.count();
    }

    private void validarDetalle(DetallePedido d) {
        if (d == null) {
            throw new BusinessValidationException("El detalle de pedido no puede ser nulo");
        }
        if (d.getCodigoPedido() == null || d.getCodigoPedido() <= 0) {
            throw new BusinessValidationException("El código de pedido es obligatorio");
        }
        if (d.getCodigoProducto() == null || d.getCodigoProducto().trim().isEmpty()) {
            throw new BusinessValidationException("El código de producto es obligatorio");
        }
        if (d.getCantidad() == null || d.getCantidad() <= 0) {
            throw new BusinessValidationException("La cantidad debe ser mayor a cero");
        }
        if (d.getPrecioUnidad() == null || d.getPrecioUnidad().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("El precio por unidad debe ser positivo");
        }
        if (d.getNumeroLinea() == null || d.getNumeroLinea() <= 0) {
            throw new BusinessValidationException("El número de línea debe ser positivo");
        }
    }
}
