package com.jardineria.infrastructure.adapter.out.persistence.jdbc;

import com.jardineria.domain.model.GamaProducto;
import com.jardineria.domain.repository.GamaProductoRepository;
import com.jardineria.infrastructure.adapter.out.persistence.mapper.GamaProductoRowMapper;
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

public class GamaProductoJdbcRepository implements GamaProductoRepository {
    private static final Logger log = LoggerFactory.getLogger(GamaProductoJdbcRepository.class);
    private final GamaProductoRowMapper rowMapper = new GamaProductoRowMapper();

    @Override
    public GamaProducto save(GamaProducto entity) {
        String sql = "INSERT INTO gama_producto (gama, descripcion_texto, descripcion_html, imagen) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getGama());
            ps.setString(2, entity.getDescripcionTexto());
            ps.setString(3, entity.getDescripcionHtml());
            ps.setString(4, entity.getImagen());
            ps.executeUpdate();
            return entity;
        } catch (SQLException e) {
            log.error("Error al guardar gama_producto: {}", entity.getGama(), e);
            throw new RuntimeException("Error al guardar gama: " + e.getMessage(), e);
        }
    }

    @Override
    public GamaProducto update(GamaProducto entity) {
        String sql = "UPDATE gama_producto SET descripcion_texto = ?, descripcion_html = ?, imagen = ? WHERE gama = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getDescripcionTexto());
            ps.setString(2, entity.getDescripcionHtml());
            ps.setString(3, entity.getImagen());
            ps.setString(4, entity.getGama());
            ps.executeUpdate();
            return entity;
        } catch (SQLException e) {
            log.error("Error al actualizar gama_producto: {}", entity.getGama(), e);
            throw new RuntimeException("Error al actualizar gama: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<GamaProducto> findById(String id) {
        String sql = "SELECT * FROM gama_producto WHERE gama = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar gama por ID: {}", id, e);
            throw new RuntimeException("Error al buscar gama: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<GamaProducto> findAll() {
        List<GamaProducto> list = new ArrayList<>();
        String sql = "SELECT * FROM gama_producto ORDER BY gama";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rowMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al listar gamas", e);
            throw new RuntimeException("Error al listar gamas: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM gama_producto WHERE gama = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error al eliminar gama: {}", id, e);
            throw new RuntimeException("Error al eliminar gama: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(String id) {
        String sql = "SELECT 1 FROM gama_producto WHERE gama = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Error al verificar existencia de gama: {}", id, e);
            throw new RuntimeException("Error al verificar gama: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM gama_producto";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            log.error("Error al contar gamas", e);
            throw new RuntimeException("Error al contar gamas: " + e.getMessage(), e);
        }
        return 0;
    }
}
