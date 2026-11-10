package com.jardineria.infrastructure.adapter.out.persistence.mapper;

import com.jardineria.domain.model.GamaProducto;

import java.sql.ResultSet;
import java.sql.SQLException;

public class GamaProductoRowMapper implements RowMapper<GamaProducto> {
    @Override
    public GamaProducto mapRow(ResultSet rs) throws SQLException {
        return new GamaProducto(
                rs.getString("gama"),
                rs.getString("descripcion_texto"),
                rs.getString("descripcion_html"),
                rs.getString("imagen")
        );
    }
}
