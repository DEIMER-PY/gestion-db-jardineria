package com.jardineria.infrastructure.adapter.out.persistence.jdbc;

import com.jardineria.domain.model.Oficina;
import com.jardineria.domain.repository.OficinaRepository;
import com.jardineria.infrastructure.adapter.out.persistence.mapper.OficinaRowMapper;
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

public class OficinaJdbcRepository implements OficinaRepository {
    private static final Logger log = LoggerFactory.getLogger(OficinaJdbcRepository.class);
    private final OficinaRowMapper rowMapper = new OficinaRowMapper();

    @Override
    public Oficina save(Oficina entity) {
        String sql = "INSERT INTO oficina (codigo_oficina, ciudad, pais, region, codigo_postal, telefono, linea_direccion1, linea_direccion2) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getCodigoOficina());
            ps.setString(2, entity.getCiudad());
            ps.setString(3, entity.getPais());
            ps.setString(4, entity.getRegion());
            ps.setString(5, entity.getCodigoPostal());
            ps.setString(6, entity.getTelefono());
            ps.setString(7, entity.getLineaDireccion1());
            ps.setString(8, entity.getLineaDireccion2());
            ps.executeUpdate();
            return entity;
        } catch (SQLException e) {
            log.error("Error al guardar oficina: {}", entity.getCodigoOficina(), e);
            throw new RuntimeException("Error al guardar oficina: " + e.getMessage(), e);
        }
    }

    @Override
    public Oficina update(Oficina entity) {
        String sql = "UPDATE oficina SET ciudad = ?, pais = ?, region = ?, codigo_postal = ?, telefono = ?, " +
                "linea_direccion1 = ?, linea_direccion2 = ? WHERE codigo_oficina = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getCiudad());
            ps.setString(2, entity.getPais());
            ps.setString(3, entity.getRegion());
            ps.setString(4, entity.getCodigoPostal());
            ps.setString(5, entity.getTelefono());
            ps.setString(6, entity.getLineaDireccion1());
            ps.setString(7, entity.getLineaDireccion2());
            ps.setString(8, entity.getCodigoOficina());
            ps.executeUpdate();
            return entity;
        } catch (SQLException e) {
            log.error("Error al actualizar oficina: {}", entity.getCodigoOficina(), e);
            throw new RuntimeException("Error al actualizar oficina: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Oficina> findById(String id) {
        String sql = "SELECT * FROM oficina WHERE codigo_oficina = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar oficina por ID: {}", id, e);
            throw new RuntimeException("Error al buscar oficina: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Oficina> findAll() {
        List<Oficina> list = new ArrayList<>();
        String sql = "SELECT * FROM oficina ORDER BY codigo_oficina";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rowMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al listar oficinas", e);
            throw new RuntimeException("Error al listar oficinas: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM oficina WHERE codigo_oficina = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error al eliminar oficina: {}", id, e);
            throw new RuntimeException("Error al eliminar oficina: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(String id) {
        String sql = "SELECT 1 FROM oficina WHERE codigo_oficina = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Error al verificar existencia de oficina: {}", id, e);
            throw new RuntimeException("Error al verificar oficina: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM oficina";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            log.error("Error al contar oficinas", e);
            throw new RuntimeException("Error al contar oficinas: " + e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public List<Oficina> findByPais(String pais) {
        List<Oficina> list = new ArrayList<>();
        String sql = "SELECT * FROM oficina WHERE UPPER(pais) = UPPER(?) ORDER BY ciudad";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pais);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar oficinas por país: {}", pais, e);
            throw new RuntimeException("Error al consultar oficinas: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Oficina> findByCiudad(String ciudad) {
        List<Oficina> list = new ArrayList<>();
        String sql = "SELECT * FROM oficina WHERE UPPER(ciudad) = UPPER(?) ORDER BY codigo_oficina";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ciudad);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar oficinas por ciudad: {}", ciudad, e);
            throw new RuntimeException("Error al consultar oficinas: " + e.getMessage(), e);
        }
        return list;
    }
}
