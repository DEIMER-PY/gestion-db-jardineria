package com.jardineria.infrastructure.adapter.out.persistence.mapper;

import com.jardineria.domain.model.DetallePedido;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DetallePedidoRowMapper implements RowMapper<DetallePedido> {
    @Override
    public DetallePedido mapRow(ResultSet rs) throws SQLException {
        return new DetallePedido(
                rs.getInt("codigo_pedido"),
                rs.getString("codigo_producto"),
                rs.getInt("cantidad"),
                rs.getBigDecimal("precio_unidad"),
                rs.getShort("numero_linea")
        );
    }
}
