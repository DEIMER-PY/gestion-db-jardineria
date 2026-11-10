package com.jardineria.infrastructure.adapter.out.persistence.jdbc;

import com.jardineria.domain.model.Pago;
import com.jardineria.domain.model.PagoId;
import com.jardineria.domain.repository.PagoRepository;
import com.jardineria.infrastructure.adapter.out.persistence.mapper.PagoRowMapper;
import com.jardineria.infrastructure.config.DatabaseConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PagoJdbcRepository implements PagoRepository {
    private static final Logger log = LoggerFactory.getLogger(PagoJdbcRepository.class);
    private final PagoRowMapper rowMapper = new PagoRowMapper();

    @Override
    public Pago save(Pago entity) {
        String sql = "INSERT INTO pago (codigo_cliente, forma_pago, id_transaccion, fecha_pago, total) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.getCodigoCliente());
            ps.setString(2, entity.getFormaPago());
            ps.setString(3, entity.getIdTransaccion());
            ps.setDate(4, entity.getFechaPago() != null ? Date.valueOf(entity.getFechaPago()) : null);
            ps.setBigDecimal(5, entity.getTotal());
            ps.executeUpdate();
            return entity;
        } catch (SQLException e) {
            log.error("Error al guardar pago: cliente={}, transaccion={}", entity.getCodigoCliente(), entity.getIdTransaccion(), e);
            throw new RuntimeException("Error al guardar pago: " + e.getMessage(), e);
        }
    }

    @Override
    public Pago update(Pago entity) {
        String sql = "UPDATE pago SET forma_pago = ?, fecha_pago = ?, total = ? " +
                "WHERE codigo_cliente = ? AND id_transaccion = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getFormaPago());
            ps.setDate(2, entity.getFechaPago() != null ? Date.valueOf(entity.getFechaPago()) : null);
            ps.setBigDecimal(3, entity.getTotal());
            ps.setInt(4, entity.getCodigoCliente());
            ps.setString(5, entity.getIdTransaccion());
            ps.executeUpdate();
            return entity;
        } catch (SQLException e) {
            log.error("Error al actualizar pago: cliente={}, transaccion={}", entity.getCodigoCliente(), entity.getIdTransaccion(), e);
            throw new RuntimeException("Error al actualizar pago: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Pago> findById(PagoId id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT * FROM pago WHERE codigo_cliente = ? AND id_transaccion = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id.getCodigoCliente());
            ps.setString(2, id.getIdTransaccion());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar pago por ID: {}", id, e);
            throw new RuntimeException("Error al buscar pago: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Pago> findAll() {
        List<Pago> list = new ArrayList<>();
        String sql = "SELECT * FROM pago ORDER BY fecha_pago DESC";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rowMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al listar pagos", e);
            throw new RuntimeException("Error al listar pagos: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean deleteById(PagoId id) {
        if (id == null) return false;
        String sql = "DELETE FROM pago WHERE codigo_cliente = ? AND id_transaccion = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id.getCodigoCliente());
            ps.setString(2, id.getIdTransaccion());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error al eliminar pago: {}", id, e);
            throw new RuntimeException("Error al eliminar pago: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(PagoId id) {
        if (id == null) return false;
        String sql = "SELECT 1 FROM pago WHERE codigo_cliente = ? AND id_transaccion = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id.getCodigoCliente());
            ps.setString(2, id.getIdTransaccion());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Error al verificar existencia de pago: {}", id, e);
            throw new RuntimeException("Error al verificar pago: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM pago";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            log.error("Error al contar pagos", e);
            throw new RuntimeException("Error al contar pagos: " + e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public List<Pago> findByCliente(Integer codigoCliente) {
        List<Pago> list = new ArrayList<>();
        String sql = "SELECT * FROM pago WHERE codigo_cliente = ? ORDER BY fecha_pago DESC";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, codigoCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar pagos por cliente: {}", codigoCliente, e);
            throw new RuntimeException("Error al consultar pagos: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Pago> findByFormaPago(String formaPago) {
        List<Pago> list = new ArrayList<>();
        String sql = "SELECT * FROM pago WHERE UPPER(forma_pago) = UPPER(?) ORDER BY fecha_pago DESC";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, formaPago);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar pagos por forma de pago: {}", formaPago, e);
            throw new RuntimeException("Error al consultar pagos: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public BigDecimal totalPagos() {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM pago";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            log.error("Error al calcular total de pagos", e);
            throw new RuntimeException("Error al sumar pagos: " + e.getMessage(), e);
        }
        return BigDecimal.ZERO;
    }
}
