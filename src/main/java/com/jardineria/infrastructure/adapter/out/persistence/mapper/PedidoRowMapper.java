package com.jardineria.infrastructure.adapter.out.persistence.mapper;

import com.jardineria.domain.model.Pedido;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class PedidoRowMapper implements RowMapper<Pedido> {
    @Override
    public Pedido mapRow(ResultSet rs) throws SQLException {
        Date fechaPedSql = rs.getDate("fecha_pedido");
        LocalDate fechaPedido = fechaPedSql != null ? fechaPedSql.toLocalDate() : null;

        Date fechaEspSql = rs.getDate("fecha_esperada");
        LocalDate fechaEsperada = fechaEspSql != null ? fechaEspSql.toLocalDate() : null;

        Date fechaEntSql = rs.getDate("fecha_entrega");
        LocalDate fechaEntrega = fechaEntSql != null ? fechaEntSql.toLocalDate() : null;

        return new Pedido(
                rs.getInt("codigo_pedido"),
                fechaPedido,
                fechaEsperada,
                fechaEntrega,
                rs.getString("estado"),
                rs.getString("comentarios"),
                rs.getInt("codigo_cliente")
        );
    }
}
