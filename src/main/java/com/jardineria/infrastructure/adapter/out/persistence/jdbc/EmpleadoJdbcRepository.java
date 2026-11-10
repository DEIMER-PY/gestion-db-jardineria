package com.jardineria.infrastructure.adapter.out.persistence.jdbc;

import com.jardineria.domain.model.Empleado;
import com.jardineria.domain.repository.EmpleadoRepository;
import com.jardineria.infrastructure.adapter.out.persistence.mapper.EmpleadoRowMapper;
import com.jardineria.infrastructure.config.DatabaseConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EmpleadoJdbcRepository implements EmpleadoRepository {
    private static final Logger log = LoggerFactory.getLogger(EmpleadoJdbcRepository.class);
    private final EmpleadoRowMapper rowMapper = new EmpleadoRowMapper();

    @Override
    public Empleado save(Empleado entity) {
        String sql = "INSERT INTO empleado (codigo_empleado, nombre, apellido1, apellido2, extension, email, codigo_oficina, codigo_jefe, puesto) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.getCodigoEmpleado());
            ps.setString(2, entity.getNombre());
            ps.setString(3, entity.getApellido1());
            ps.setString(4, entity.getApellido2());
            ps.setString(5, entity.getExtension());
            ps.setString(6, entity.getEmail());
            ps.setString(7, entity.getCodigoOficina());
            if (entity.getCodigoJefe() != null) {
                ps.setInt(8, entity.getCodigoJefe());
            } else {
                ps.setNull(8, Types.INTEGER);
            }
            ps.setString(9, entity.getPuesto());
            ps.executeUpdate();
            return entity;
        } catch (SQLException e) {
            log.error("Error al guardar empleado: {}", entity.getCodigoEmpleado(), e);
            throw new RuntimeException("Error al guardar empleado: " + e.getMessage(), e);
        }
    }

    @Override
    public Empleado update(Empleado entity) {
        String sql = "UPDATE empleado SET nombre = ?, apellido1 = ?, apellido2 = ?, extension = ?, " +
                "email = ?, codigo_oficina = ?, codigo_jefe = ?, puesto = ? WHERE codigo_empleado = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getNombre());
            ps.setString(2, entity.getApellido1());
            ps.setString(3, entity.getApellido2());
            ps.setString(4, entity.getExtension());
            ps.setString(5, entity.getEmail());
            ps.setString(6, entity.getCodigoOficina());
            if (entity.getCodigoJefe() != null) {
                ps.setInt(7, entity.getCodigoJefe());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.setString(8, entity.getPuesto());
            ps.setInt(9, entity.getCodigoEmpleado());
            ps.executeUpdate();
            return entity;
        } catch (SQLException e) {
            log.error("Error al actualizar empleado: {}", entity.getCodigoEmpleado(), e);
            throw new RuntimeException("Error al actualizar empleado: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Empleado> findById(Integer id) {
        String sql = "SELECT * FROM empleado WHERE codigo_empleado = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar empleado por ID: {}", id, e);
            throw new RuntimeException("Error al buscar empleado: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Empleado> findAll() {
        List<Empleado> list = new ArrayList<>();
        String sql = "SELECT * FROM empleado ORDER BY codigo_empleado";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rowMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al listar empleados", e);
            throw new RuntimeException("Error al listar empleados: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "DELETE FROM empleado WHERE codigo_empleado = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error al eliminar empleado: {}", id, e);
            throw new RuntimeException("Error al eliminar empleado: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(Integer id) {
        String sql = "SELECT 1 FROM empleado WHERE codigo_empleado = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Error al verificar existencia de empleado: {}", id, e);
            throw new RuntimeException("Error al verificar empleado: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM empleado";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            log.error("Error al contar empleados", e);
            throw new RuntimeException("Error al contar empleados: " + e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public List<Empleado> findByOficina(String codigoOficina) {
        List<Empleado> list = new ArrayList<>();
        String sql = "SELECT * FROM empleado WHERE codigo_oficina = ? ORDER BY codigo_empleado";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigoOficina);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar empleados por oficina: {}", codigoOficina, e);
            throw new RuntimeException("Error al consultar empleados: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Empleado> findByJefe(Integer codigoJefe) {
        List<Empleado> list = new ArrayList<>();
        String sql = "SELECT * FROM empleado WHERE codigo_jefe = ? ORDER BY codigo_empleado";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, codigoJefe);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar empleados por jefe: {}", codigoJefe, e);
            throw new RuntimeException("Error al consultar empleados: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Empleado> findDirectores() {
        List<Empleado> list = new ArrayList<>();
        String sql = "SELECT * FROM empleado WHERE codigo_jefe IS NULL ORDER BY codigo_empleado";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rowMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al listar directores", e);
            throw new RuntimeException("Error al consultar directores: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Empleado> findRepresentantesVentas() {
        List<Empleado> list = new ArrayList<>();
        String sql = "SELECT * FROM empleado WHERE LOWER(puesto) LIKE '%representante%' ORDER BY codigo_empleado";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rowMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al listar representantes de ventas", e);
            throw new RuntimeException("Error al consultar representantes: " + e.getMessage(), e);
        }
        return list;
    }
}
