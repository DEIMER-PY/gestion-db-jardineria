package com.jardineria.infrastructure.adapter.out.persistence.jdbc;

import com.jardineria.domain.model.DetallePedido;
import com.jardineria.domain.model.DetallePedidoId;
import com.jardineria.domain.repository.DetallePedidoRepository;
import com.jardineria.infrastructure.adapter.out.persistence.mapper.DetallePedidoRowMapper;
import com.jardineria.infrastructure.config.DatabaseConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DetallePedidoJdbcRepository implements DetallePedidoRepository {
    private static final Logger log = LoggerFactory.getLogger(DetallePedidoJdbcRepository.class);
    private final DetallePedidoRowMapper rowMapper = new DetallePedidoRowMapper();

    @Override
    public DetallePedido save(DetallePedido entity) {
        String sql = "INSERT INTO detalle_pedido (codigo_pedido, codigo_producto, cantidad, precio_unidad, numero_linea) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.getCodigoPedido());
            ps.setString(2, entity.getCodigoProducto());
            ps.setInt(3, entity.getCantidad());
            ps.setBigDecimal(4, entity.getPrecioUnidad());
            ps.setShort(5, entity.getNumeroLinea());
            ps.executeUpdate();
            return entity;
        } catch (SQLException e) {
            log.error("Error al guardar detalle_pedido: pedido={}, producto={}", entity.getCodigoPedido(), entity.getCodigoProducto(), e);
            throw new RuntimeException("Error al guardar detalle de pedido: " + e.getMessage(), e);
        }
    }

    @Override
    public DetallePedido update(DetallePedido entity) {
        String sql = "UPDATE detalle_pedido SET cantidad = ?, precio_unidad = ?, numero_linea = ? " +
                "WHERE codigo_pedido = ? AND codigo_producto = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.getCantidad());
            ps.setBigDecimal(2, entity.getPrecioUnidad());
            ps.setShort(3, entity.getNumeroLinea());
            ps.setInt(4, entity.getCodigoPedido());
            ps.setString(5, entity.getCodigoProducto());
            ps.executeUpdate();
            return entity;
        } catch (SQLException e) {
            log.error("Error al actualizar detalle_pedido: pedido={}, producto={}", entity.getCodigoPedido(), entity.getCodigoProducto(), e);
            throw new RuntimeException("Error al actualizar detalle de pedido: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<DetallePedido> findById(DetallePedidoId id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT * FROM detalle_pedido WHERE codigo_pedido = ? AND codigo_producto = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id.getCodigoPedido());
            ps.setString(2, id.getCodigoProducto());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar detalle_pedido por ID: {}", id, e);
            throw new RuntimeException("Error al buscar detalle de pedido: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<DetallePedido> findAll() {
        List<DetallePedido> list = new ArrayList<>();
        String sql = "SELECT * FROM detalle_pedido ORDER BY codigo_pedido, numero_linea";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rowMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al listar detalles de pedido", e);
            throw new RuntimeException("Error al listar detalles: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean deleteById(DetallePedidoId id) {
        if (id == null) return false;
        String sql = "DELETE FROM detalle_pedido WHERE codigo_pedido = ? AND codigo_producto = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id.getCodigoPedido());
            ps.setString(2, id.getCodigoProducto());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error al eliminar detalle_pedido: {}", id, e);
            throw new RuntimeException("Error al eliminar detalle de pedido: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(DetallePedidoId id) {
        if (id == null) return false;
        String sql = "SELECT 1 FROM detalle_pedido WHERE codigo_pedido = ? AND codigo_producto = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id.getCodigoPedido());
            ps.setString(2, id.getCodigoProducto());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Error al verificar existencia de detalle_pedido: {}", id, e);
            throw new RuntimeException("Error al verificar detalle: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM detalle_pedido";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            log.error("Error al contar detalles de pedidos", e);
            throw new RuntimeException("Error al contar detalles: " + e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public List<DetallePedido> findByPedido(Integer codigoPedido) {
        List<DetallePedido> list = new ArrayList<>();
        String sql = "SELECT * FROM detalle_pedido WHERE codigo_pedido = ? ORDER BY numero_linea ASC";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, codigoPedido);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar detalles por pedido: {}", codigoPedido, e);
            throw new RuntimeException("Error al consultar detalles de pedido: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean deleteByPedido(Integer codigoPedido) {
        String sql = "DELETE FROM detalle_pedido WHERE codigo_pedido = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, codigoPedido);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error al eliminar detalles por pedido: {}", codigoPedido, e);
            throw new RuntimeException("Error al eliminar detalles de pedido: " + e.getMessage(), e);
        }
    }
}
