package com.jardineria.infrastructure.adapter.out.persistence.jdbc;

import com.jardineria.domain.model.Producto;
import com.jardineria.domain.repository.ProductoRepository;
import com.jardineria.infrastructure.adapter.out.persistence.mapper.ProductoRowMapper;
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

public class ProductoJdbcRepository implements ProductoRepository {
    private static final Logger log = LoggerFactory.getLogger(ProductoJdbcRepository.class);
    private final ProductoRowMapper rowMapper = new ProductoRowMapper();

    @Override
    public Producto save(Producto entity) {
        String sql = "INSERT INTO producto (codigo_producto, nombre, gama, dimensiones, proveedor, " +
                "descripcion, cantidad_en_stock, precio_venta, precio_proveedor) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getCodigoProducto());
            ps.setString(2, entity.getNombre());
            ps.setString(3, entity.getGama());
            ps.setString(4, entity.getDimensiones());
            ps.setString(5, entity.getProveedor());
            ps.setString(6, entity.getDescripcion());
            ps.setInt(7, entity.getCantidadEnStock());
            ps.setBigDecimal(8, entity.getPrecioVenta());
            ps.setBigDecimal(9, entity.getPrecioProveedor());
            ps.executeUpdate();
            return entity;
        } catch (SQLException e) {
            log.error("Error al guardar producto: {}", entity.getCodigoProducto(), e);
            throw new RuntimeException("Error al guardar producto: " + e.getMessage(), e);
        }
    }

    @Override
    public Producto update(Producto entity) {
        String sql = "UPDATE producto SET nombre = ?, gama = ?, dimensiones = ?, proveedor = ?, " +
                "descripcion = ?, cantidad_en_stock = ?, precio_venta = ?, precio_proveedor = ? WHERE codigo_producto = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getNombre());
            ps.setString(2, entity.getGama());
            ps.setString(3, entity.getDimensiones());
            ps.setString(4, entity.getProveedor());
            ps.setString(5, entity.getDescripcion());
            ps.setInt(6, entity.getCantidadEnStock());
            ps.setBigDecimal(7, entity.getPrecioVenta());
            ps.setBigDecimal(8, entity.getPrecioProveedor());
            ps.setString(9, entity.getCodigoProducto());
            ps.executeUpdate();
            return entity;
        } catch (SQLException e) {
            log.error("Error al actualizar producto: {}", entity.getCodigoProducto(), e);
            throw new RuntimeException("Error al actualizar producto: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Producto> findById(String id) {
        String sql = "SELECT * FROM producto WHERE codigo_producto = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar producto por ID: {}", id, e);
            throw new RuntimeException("Error al buscar producto: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Producto> findAll() {
        List<Producto> list = new ArrayList<>();
        String sql = "SELECT * FROM producto ORDER BY codigo_producto";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rowMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al listar productos", e);
            throw new RuntimeException("Error al listar productos: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM producto WHERE codigo_producto = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error al eliminar producto: {}", id, e);
            throw new RuntimeException("Error al eliminar producto: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(String id) {
        String sql = "SELECT 1 FROM producto WHERE codigo_producto = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Error al verificar existencia de producto: {}", id, e);
            throw new RuntimeException("Error al verificar producto: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM producto";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            log.error("Error al contar productos", e);
            throw new RuntimeException("Error al contar productos: " + e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public List<Producto> findByGama(String gama) {
        List<Producto> list = new ArrayList<>();
        String sql = "SELECT * FROM producto WHERE gama = ? ORDER BY codigo_producto";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, gama);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar productos por gama: {}", gama, e);
            throw new RuntimeException("Error al consultar productos: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Producto> findBajoStock(int umbral) {
        List<Producto> list = new ArrayList<>();
        String sql = "SELECT * FROM producto WHERE cantidad_en_stock <= ? ORDER BY cantidad_en_stock ASC";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, umbral);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar productos bajo stock con umbral: {}", umbral, e);
            throw new RuntimeException("Error al consultar productos: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Producto> searchByNombre(String termino) {
        List<Producto> list = new ArrayList<>();
        String sql = "SELECT * FROM producto WHERE LOWER(nombre) LIKE LOWER(?) ORDER BY nombre";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + termino + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar productos por término: {}", termino, e);
            throw new RuntimeException("Error al buscar productos: " + e.getMessage(), e);
        }
        return list;
    }
}
