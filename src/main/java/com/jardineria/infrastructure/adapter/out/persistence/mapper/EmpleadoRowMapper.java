package com.jardineria.infrastructure.adapter.out.persistence.mapper;

import com.jardineria.domain.model.Empleado;

import java.sql.ResultSet;
import java.sql.SQLException;

public class EmpleadoRowMapper implements RowMapper<Empleado> {
    @Override
    public Empleado mapRow(ResultSet rs) throws SQLException {
        int jefeVal = rs.getInt("codigo_jefe");
        Integer codigoJefe = rs.wasNull() ? null : jefeVal;

        return new Empleado(
                rs.getInt("codigo_empleado"),
                rs.getString("nombre"),
                rs.getString("apellido1"),
                rs.getString("apellido2"),
                rs.getString("extension"),
                rs.getString("email"),
                rs.getString("codigo_oficina"),
                codigoJefe,
                rs.getString("puesto")
        );
    }
}
