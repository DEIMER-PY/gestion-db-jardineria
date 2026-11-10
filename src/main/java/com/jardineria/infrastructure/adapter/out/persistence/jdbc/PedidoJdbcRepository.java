package com.jardineria.infrastructure.adapter.out.persistence.jdbc;

import com.jardineria.domain.model.Pedido;
import com.jardineria.domain.repository.PedidoRepository;
import com.jardineria.infrastructure.adapter.out.persistence.mapper.PedidoRowMapper;
import com.jardineria.infrastructure.config.DatabaseConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PedidoJdbcRepository implements PedidoRepository {
    private static final Logger log = LoggerFactory.getLogger(PedidoJdbcRepository.class);
    private final PedidoRowMapper rowMapper = new PedidoRowMapper();

    @Override
    public Pedido save(Pedido entity) {
        String sql = "INSERT INTO pedido (codigo_pedido, fecha_pedido, fecha_esperada, fecha_entrega, estado, comentarios, codigo_cliente) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.getCodigoPedido());
            ps.setDate(2, entity.getFechaPedido() != null ? Date.valueOf(entity.getFechaPedido()) : null);
            ps.setDate(3, entity.getFechaEsperada() != null ? Date.valueOf(entity.getFechaEsperada()) : null);
            if (entity.getFechaEntrega() != null) {
                ps.setDate(4, Date.valueOf(entity.getFechaEntrega()));
            } else {
                ps.setNull(4, Types.DATE);
            }
            ps.setString(5, entity.getEstado());
            ps.setString(6, entity.getComentarios());
            ps.setInt(7, entity.getCodigoCliente());
            ps.executeUpdate();
            return entity;
        } catch (SQLException e) {
            log.error("Error al guardar pedido: {}", entity.getCodigoPedido(), e);
            throw new RuntimeException("Error al guardar pedido: " + e.getMessage(), e);
        }
    }

    @Override
    public Pedido update(Pedido entity) {
        String sql = "UPDATE pedido SET fecha_pedido = ?, fecha_esperada = ?, fecha_entrega = ?, " +
                "estado = ?, comentarios = ?, codigo_cliente = ? WHERE codigo_pedido = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, entity.getFechaPedido() != null ? Date.valueOf(entity.getFechaPedido()) : null);
            ps.setDate(2, entity.getFechaEsperada() != null ? Date.valueOf(entity.getFechaEsperada()) : null);
            if (entity.getFechaEntrega() != null) {
                ps.setDate(3, Date.valueOf(entity.getFechaEntrega()));
            } else {
                ps.setNull(3, Types.DATE);
            }
            ps.setString(4, entity.getEstado());
            ps.setString(5, entity.getComentarios());
            ps.setInt(6, entity.getCodigoCliente());
            ps.setInt(7, entity.getCodigoPedido());
            ps.executeUpdate();
            return entity;
        } catch (SQLException e) {
            log.error("Error al actualizar pedido: {}", entity.getCodigoPedido(), e);
            throw new RuntimeException("Error al actualizar pedido: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Pedido> findById(Integer id) {
        String sql = "SELECT * FROM pedido WHERE codigo_pedido = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar pedido por ID: {}", id, e);
            throw new RuntimeException("Error al buscar pedido: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Pedido> findAll() {
        List<Pedido> list = new ArrayList<>();
        String sql = "SELECT * FROM pedido ORDER BY codigo_pedido DESC";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rowMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al listar pedidos", e);
            throw new RuntimeException("Error al listar pedidos: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "DELETE FROM pedido WHERE codigo_pedido = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error al eliminar pedido: {}", id, e);
            throw new RuntimeException("Error al eliminar pedido: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(Integer id) {
        String sql = "SELECT 1 FROM pedido WHERE codigo_pedido = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Error al verificar existencia de pedido: {}", id, e);
            throw new RuntimeException("Error al verificar pedido: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM pedido";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            log.error("Error al contar pedidos", e);
            throw new RuntimeException("Error al contar pedidos: " + e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public List<Pedido> findByCliente(Integer codigoCliente) {
        List<Pedido> list = new ArrayList<>();
        String sql = "SELECT * FROM pedido WHERE codigo_cliente = ? ORDER BY codigo_pedido DESC";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, codigoCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar pedidos por cliente: {}", codigoCliente, e);
            throw new RuntimeException("Error al consultar pedidos: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Pedido> findByEstado(String estado) {
        List<Pedido> list = new ArrayList<>();
        String sql = "SELECT * FROM pedido WHERE UPPER(estado) = UPPER(?) ORDER BY codigo_pedido DESC";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, estado);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar pedidos por estado: {}", estado, e);
            throw new RuntimeException("Error al consultar pedidos: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public long countByEstado(String estado) {
        String sql = "SELECT COUNT(*) FROM pedido WHERE UPPER(estado) = UPPER(?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, estado);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            log.error("Error al contar pedidos por estado: {}", estado, e);
            throw new RuntimeException("Error al contar pedidos: " + e.getMessage(), e);
        }
        return 0;
    }
}
