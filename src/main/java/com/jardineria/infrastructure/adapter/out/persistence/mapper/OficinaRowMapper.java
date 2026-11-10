package com.jardineria.infrastructure.adapter.out.persistence.mapper;

import com.jardineria.domain.model.Oficina;

import java.sql.ResultSet;
import java.sql.SQLException;

public class OficinaRowMapper implements RowMapper<Oficina> {
    @Override
    public Oficina mapRow(ResultSet rs) throws SQLException {
        return new Oficina(
                rs.getString("codigo_oficina"),
                rs.getString("ciudad"),
                rs.getString("pais"),
                rs.getString("region"),
                rs.getString("codigo_postal"),
                rs.getString("telefono"),
                rs.getString("linea_direccion1"),
                rs.getString("linea_direccion2")
        );
    }
}
