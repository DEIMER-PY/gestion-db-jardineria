package com.jardineria.infrastructure.adapter.out.persistence.jdbc;

import com.jardineria.domain.model.Cliente;
import com.jardineria.domain.repository.ClienteRepository;
import com.jardineria.infrastructure.adapter.out.persistence.mapper.ClienteRowMapper;
import com.jardineria.infrastructure.config.DatabaseConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClienteJdbcRepository implements ClienteRepository {
    private static final Logger log = LoggerFactory.getLogger(ClienteJdbcRepository.class);
    private final ClienteRowMapper rowMapper = new ClienteRowMapper();

    @Override
    public Cliente save(Cliente entity) {
        String sql = "INSERT INTO cliente (codigo_cliente, nombre_cliente, nombre_contacto, apellido_contacto, " +
                "telefono, fax, linea_direccion1, linea_direccion2, ciudad, region, pais, codigo_postal, " +
                "codigo_empleado_rep_ventas, limite_credito) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.getCodigoCliente());
            ps.setString(2, entity.getNombreCliente());
            ps.setString(3, entity.getNombreContacto());
            ps.setString(4, entity.getApellidoContacto());
            ps.setString(5, entity.getTelefono());
            ps.setString(6, entity.getFax());
            ps.setString(7, entity.getLineaDireccion1());
            ps.setString(8, entity.getLineaDireccion2());
            ps.setString(9, entity.getCiudad());
            ps.setString(10, entity.getRegion());
            ps.setString(11, entity.getPais());
            ps.setString(12, entity.getCodigoPostal());
            if (entity.getCodigoEmpleadoRepVentas() != null) {
                ps.setInt(13, entity.getCodigoEmpleadoRepVentas());
            } else {
                ps.setNull(13, Types.INTEGER);
            }
            ps.setBigDecimal(14, entity.getLimiteCredito());
            ps.executeUpdate();
            return entity;
        } catch (SQLException e) {
            log.error("Error al guardar cliente: {}", entity.getCodigoCliente(), e);
            throw new RuntimeException("Error al guardar cliente: " + e.getMessage(), e);
        }
    }

    @Override
    public Cliente update(Cliente entity) {
        String sql = "UPDATE cliente SET nombre_cliente = ?, nombre_contacto = ?, apellido_contacto = ?, " +
                "telefono = ?, fax = ?, linea_direccion1 = ?, linea_direccion2 = ?, ciudad = ?, region = ?, " +
                "pais = ?, codigo_postal = ?, codigo_empleado_rep_ventas = ?, limite_credito = ? WHERE codigo_cliente = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getNombreCliente());
            ps.setString(2, entity.getNombreContacto());
            ps.setString(3, entity.getApellidoContacto());
            ps.setString(4, entity.getTelefono());
            ps.setString(5, entity.getFax());
            ps.setString(6, entity.getLineaDireccion1());
            ps.setString(7, entity.getLineaDireccion2());
            ps.setString(8, entity.getCiudad());
            ps.setString(9, entity.getRegion());
            ps.setString(10, entity.getPais());
            ps.setString(11, entity.getCodigoPostal());
            if (entity.getCodigoEmpleadoRepVentas() != null) {
                ps.setInt(12, entity.getCodigoEmpleadoRepVentas());
            } else {
                ps.setNull(12, Types.INTEGER);
            }
            ps.setBigDecimal(13, entity.getLimiteCredito());
            ps.setInt(14, entity.getCodigoCliente());
            ps.executeUpdate();
            return entity;
        } catch (SQLException e) {
            log.error("Error al actualizar cliente: {}", entity.getCodigoCliente(), e);
            throw new RuntimeException("Error al actualizar cliente: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Cliente> findById(Integer id) {
        String sql = "SELECT * FROM cliente WHERE codigo_cliente = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar cliente por ID: {}", id, e);
            throw new RuntimeException("Error al buscar cliente: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Cliente> findAll() {
        List<Cliente> list = new ArrayList<>();
        String sql = "SELECT * FROM cliente ORDER BY codigo_cliente";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rowMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al listar clientes", e);
            throw new RuntimeException("Error al listar clientes: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "DELETE FROM cliente WHERE codigo_cliente = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error al eliminar cliente: {}", id, e);
            throw new RuntimeException("Error al eliminar cliente: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(Integer id) {
        String sql = "SELECT 1 FROM cliente WHERE codigo_cliente = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Error al verificar existencia de cliente: {}", id, e);
            throw new RuntimeException("Error al verificar cliente: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM cliente";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            log.error("Error al contar clientes", e);
            throw new RuntimeException("Error al contar clientes: " + e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public List<Cliente> findByRepresentanteVentas(Integer codigoEmpleado) {
        List<Cliente> list = new ArrayList<>();
        String sql = "SELECT * FROM cliente WHERE codigo_empleado_rep_ventas = ? ORDER BY codigo_cliente";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, codigoEmpleado);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar clientes por representante: {}", codigoEmpleado, e);
            throw new RuntimeException("Error al consultar clientes: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Cliente> findByPais(String pais) {
        List<Cliente> list = new ArrayList<>();
        String sql = "SELECT * FROM cliente WHERE UPPER(pais) = UPPER(?) ORDER BY codigo_cliente";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pais);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar clientes por país: {}", pais, e);
            throw new RuntimeException("Error al consultar clientes: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Cliente> findByCiudad(String ciudad) {
        List<Cliente> list = new ArrayList<>();
        String sql = "SELECT * FROM cliente WHERE UPPER(ciudad) = UPPER(?) ORDER BY codigo_cliente";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ciudad);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar clientes por ciudad: {}", ciudad, e);
            throw new RuntimeException("Error al consultar clientes: " + e.getMessage(), e);
        }
        return list;
    }
}
