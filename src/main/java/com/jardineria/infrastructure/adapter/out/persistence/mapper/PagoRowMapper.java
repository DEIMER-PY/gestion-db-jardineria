package com.jardineria.infrastructure.adapter.out.persistence.mapper;

import com.jardineria.domain.model.Pago;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class PagoRowMapper implements RowMapper<Pago> {
    @Override
    public Pago mapRow(ResultSet rs) throws SQLException {
        Date fechaSql = rs.getDate("fecha_pago");
        LocalDate fechaPago = fechaSql != null ? fechaSql.toLocalDate() : null;

        return new Pago(
                rs.getInt("codigo_cliente"),
                rs.getString("forma_pago"),
                rs.getString("id_transaccion"),
                fechaPago,
                rs.getBigDecimal("total")
        );
    }
}
