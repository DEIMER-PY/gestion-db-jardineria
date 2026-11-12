package com.jardineria.infrastructure.adapter.out.persistence.jdbc;

import com.jardineria.domain.model.analytics.CategoryMetric;
import com.jardineria.domain.model.analytics.ClienteFinancialSummary;
import com.jardineria.domain.model.analytics.MonthlyAmount;
import com.jardineria.domain.model.analytics.MonthlyStatusCount;
import com.jardineria.domain.repository.AnalyticsRepository;
import com.jardineria.infrastructure.adapter.out.persistence.mapper.RowMapper;
import com.jardineria.infrastructure.config.DatabaseConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador JDBC del puerto {@link AnalyticsRepository}.
 * Resuelve las métricas con consultas de agregación (GROUP BY) en MySQL,
 * evitando cargar tablas completas en memoria.
 */
public class AnalyticsJdbcRepository implements AnalyticsRepository {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsJdbcRepository.class);

    private static final RowMapper<MonthlyAmount> MONTHLY_AMOUNT = rs -> new MonthlyAmount(
            YearMonth.of(rs.getInt("anio"), rs.getInt("mes")),
            rs.getBigDecimal("importe"),
            rs.getLong("registros"));

    private static final RowMapper<CategoryMetric> CATEGORY = rs -> new CategoryMetric(
            rs.getString("etiqueta"),
            rs.getLong("registros"),
            rs.getBigDecimal("importe"));

    @Override
    public List<MonthlyAmount> ventasPorMes() {
        String sql = """
                SELECT YEAR(p.fecha_pedido) AS anio, MONTH(p.fecha_pedido) AS mes,
                       SUM(d.cantidad * d.precio_unidad) AS importe,
                       COUNT(DISTINCT p.codigo_pedido) AS registros
                FROM pedido p
                JOIN detalle_pedido d ON d.codigo_pedido = p.codigo_pedido
                GROUP BY anio, mes
                ORDER BY anio, mes""";
        return query(sql, MONTHLY_AMOUNT);
    }

    @Override
    public List<MonthlyAmount> pagosPorMes() {
        String sql = """
                SELECT YEAR(fecha_pago) AS anio, MONTH(fecha_pago) AS mes,
                       SUM(total) AS importe, COUNT(*) AS registros
                FROM pago
                GROUP BY anio, mes
                ORDER BY anio, mes""";
        return query(sql, MONTHLY_AMOUNT);
    }

    @Override
    public List<MonthlyStatusCount> pedidosPorMesYEstado() {
        String sql = """
                SELECT YEAR(fecha_pedido) AS anio, MONTH(fecha_pedido) AS mes, estado, COUNT(*) AS registros
                FROM pedido
                GROUP BY anio, mes, estado
                ORDER BY anio, mes, estado""";
        return query(sql, rs -> new MonthlyStatusCount(
                YearMonth.of(rs.getInt("anio"), rs.getInt("mes")),
                rs.getString("estado"),
                rs.getLong("registros")));
    }

    @Override
    public List<CategoryMetric> ventasPorGama() {
        String sql = """
                SELECT g.gama AS etiqueta,
                       COALESCE(SUM(d.cantidad), 0) AS registros,
                       COALESCE(SUM(d.cantidad * d.precio_unidad), 0) AS importe
                FROM gama_producto g
                LEFT JOIN producto pr ON pr.gama = g.gama
                LEFT JOIN detalle_pedido d ON d.codigo_producto = pr.codigo_producto
                GROUP BY g.gama
                ORDER BY importe DESC, etiqueta""";
        return query(sql, CATEGORY);
    }

    @Override
    public List<CategoryMetric> clientesPorPais() {
        String sql = """
                SELECT COALESCE(pais, 'Sin país') AS etiqueta, COUNT(*) AS registros, 0 AS importe
                FROM cliente
                GROUP BY etiqueta
                ORDER BY registros DESC, etiqueta""";
        return query(sql, CATEGORY);
    }

    @Override
    public List<CategoryMetric> pagosPorFormaPago() {
        String sql = """
                SELECT forma_pago AS etiqueta, COUNT(*) AS registros, SUM(total) AS importe
                FROM pago
                GROUP BY forma_pago
                ORDER BY importe DESC""";
        return query(sql, CATEGORY);
    }

    @Override
    public List<CategoryMetric> topClientesPorCompras(int limite) {
        String sql = """
                SELECT c.nombre_cliente AS etiqueta,
                       COUNT(DISTINCT p.codigo_pedido) AS registros,
                       SUM(d.cantidad * d.precio_unidad) AS importe
                FROM cliente c
                JOIN pedido p ON p.codigo_cliente = c.codigo_cliente
                JOIN detalle_pedido d ON d.codigo_pedido = p.codigo_pedido
                GROUP BY c.codigo_cliente, c.nombre_cliente
                ORDER BY importe DESC
                LIMIT ?""";
        return query(sql, CATEGORY, Math.max(1, limite));
    }

    @Override
    public List<CategoryMetric> clientesMasActivos(int limite) {
        String sql = """
                SELECT c.nombre_cliente AS etiqueta,
                       COUNT(DISTINCT p.codigo_pedido) AS registros,
                       COALESCE(SUM(d.cantidad * d.precio_unidad), 0) AS importe
                FROM cliente c
                JOIN pedido p ON p.codigo_cliente = c.codigo_cliente
                LEFT JOIN detalle_pedido d ON d.codigo_pedido = p.codigo_pedido
                GROUP BY c.codigo_cliente, c.nombre_cliente
                ORDER BY registros DESC, importe DESC, c.nombre_cliente
                LIMIT ?""";
        return query(sql, CATEGORY, Math.max(1, limite));
    }

    @Override
    public Optional<ClienteFinancialSummary> resumenCliente(int codigoCliente) {
        String sql = """
                SELECT c.codigo_cliente, c.limite_credito,
                       (SELECT COUNT(*) FROM pedido p WHERE p.codigo_cliente = c.codigo_cliente) AS total_pedidos,
                       (SELECT COALESCE(SUM(d.cantidad * d.precio_unidad), 0)
                          FROM pedido p JOIN detalle_pedido d ON d.codigo_pedido = p.codigo_pedido
                         WHERE p.codigo_cliente = c.codigo_cliente) AS total_comprado,
                       (SELECT COALESCE(SUM(pg.total), 0)
                          FROM pago pg WHERE pg.codigo_cliente = c.codigo_cliente) AS total_pagado
                FROM cliente c
                WHERE c.codigo_cliente = ?""";
        return query(sql, rs -> new ClienteFinancialSummary(
                rs.getInt("codigo_cliente"),
                rs.getLong("total_pedidos"),
                rs.getBigDecimal("total_comprado"),
                rs.getBigDecimal("total_pagado"),
                rs.getBigDecimal("limite_credito")), codigoCliente)
                .stream().findFirst();
    }

    private <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapper.mapRow(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            log.error("Error en consulta analítica", e);
            throw new RuntimeException("Error en consulta analítica: " + e.getMessage(), e);
        }
    }
}
