package com.jardineria.infrastructure.adapter.out.persistence.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Interfaz funcional para transformar una fila de un ResultSet en una entidad de dominio.
 *
 * @param <T> Tipo de entidad de retorno
 */
@FunctionalInterface
public interface RowMapper<T> {
    T mapRow(ResultSet rs) throws SQLException;
}
