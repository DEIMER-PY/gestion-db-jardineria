package com.jardineria.infrastructure.adapter.out.persistence.mapper;

import com.jardineria.domain.model.Producto;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ProductoRowMapper implements RowMapper<Producto> {
    @Override
    public Producto mapRow(ResultSet rs) throws SQLException {
        return new Producto(
                rs.getString("codigo_producto"),
                rs.getString("nombre"),
                rs.getString("gama"),
                rs.getString("dimensiones"),
                rs.getString("proveedor"),
                rs.getString("descripcion"),
                rs.getInt("cantidad_en_stock"),
                rs.getBigDecimal("precio_venta"),
                rs.getBigDecimal("precio_proveedor")
        );
    }
}
