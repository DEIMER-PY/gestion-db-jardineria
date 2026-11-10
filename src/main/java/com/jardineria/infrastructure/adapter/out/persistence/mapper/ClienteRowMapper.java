package com.jardineria.infrastructure.adapter.out.persistence.mapper;

import com.jardineria.domain.model.Cliente;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ClienteRowMapper implements RowMapper<Cliente> {
    @Override
    public Cliente mapRow(ResultSet rs) throws SQLException {
        int repVal = rs.getInt("codigo_empleado_rep_ventas");
        Integer repVentas = rs.wasNull() ? null : repVal;

        return new Cliente(
                rs.getInt("codigo_cliente"),
                rs.getString("nombre_cliente"),
                rs.getString("nombre_contacto"),
                rs.getString("apellido_contacto"),
                rs.getString("telefono"),
                rs.getString("fax"),
                rs.getString("linea_direccion1"),
                rs.getString("linea_direccion2"),
                rs.getString("ciudad"),
                rs.getString("region"),
                rs.getString("pais"),
                rs.getString("codigo_postal"),
                repVentas,
                rs.getBigDecimal("limite_credito")
        );
    }
}
