package com.jardineria.infrastructure.adapter.out.persistence.jdbc;

import com.jardineria.domain.model.chat.Canal;
import com.jardineria.domain.model.chat.Mensaje;
import com.jardineria.domain.repository.MensajeriaRepository;
import com.jardineria.infrastructure.config.DatabaseConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Mensajería sobre las tablas {@code canal}, {@code mensaje} y {@code lectura} (script {@code 02-chat.sql}). */
public class MensajeriaJdbcRepository implements MensajeriaRepository {
    private static final Logger log = LoggerFactory.getLogger(MensajeriaJdbcRepository.class);
    private static final String CANAL_COLS = "id_canal, nombre, tipo, codigo_oficina, descripcion";
    private static final String MENSAJE_COLS = "id_mensaje, id_canal, codigo_empleado, texto, enviado";

    @Override
    public List<Canal> canales() {
        String sql = "SELECT " + CANAL_COLS + " FROM canal ORDER BY FIELD(tipo, 'GENERAL', 'OFICINA', 'DIRECTO'), nombre";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Canal> out = new ArrayList<>();
            while (rs.next()) {
                out.add(canal(rs));
            }
            return out;
        } catch (SQLException e) {
            throw fallo("listar canales", e);
        }
    }

    @Override
    public Optional<Canal> canalPorNombre(String nombre) {
        String sql = "SELECT " + CANAL_COLS + " FROM canal WHERE nombre = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(canal(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw fallo("buscar el canal", e);
        }
    }

    @Override
    public Canal crearCanal(String nombre, Canal.Tipo tipo, String descripcion) {
        // INSERT IGNORE: si otro equipo lo creó a la vez, se devuelve el existente
        String sql = "INSERT IGNORE INTO canal (nombre, tipo, descripcion) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, tipo.name());
            ps.setString(3, descripcion);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw fallo("crear el canal", e);
        }
        return canalPorNombre(nombre).orElseThrow(() -> new IllegalStateException("No se pudo crear el canal " + nombre));
    }

    @Override
    public List<Mensaje> ultimos(int idCanal, int limite) {
        String sql = "SELECT * FROM (SELECT " + MENSAJE_COLS + " FROM mensaje WHERE id_canal = ? "
                + "ORDER BY id_mensaje DESC LIMIT ?) t ORDER BY id_mensaje";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCanal);
            ps.setInt(2, limite);
            return mensajes(ps);
        } catch (SQLException e) {
            throw fallo("leer los mensajes", e);
        }
    }

    @Override
    public List<Mensaje> posteriores(long despuesDe, int limite) {
        String sql = "SELECT " + MENSAJE_COLS + " FROM mensaje WHERE id_mensaje > ? ORDER BY id_mensaje LIMIT ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, despuesDe);
            ps.setInt(2, limite);
            return mensajes(ps);
        } catch (SQLException e) {
            throw fallo("consultar mensajes nuevos", e);
        }
    }

    @Override
    public long ultimoMensaje() {
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(MAX(id_mensaje), 0) FROM mensaje");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            throw fallo("consultar el último mensaje", e);
        }
    }

    @Override
    public Mensaje publicar(int idCanal, int codigoEmpleado, String texto) {
        String sql = "INSERT INTO mensaje (id_canal, codigo_empleado, texto) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idCanal);
            ps.setInt(2, codigoEmpleado);
            ps.setString(3, texto);
            ps.executeUpdate();
            long id;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                id = keys.getLong(1);
            }
            try (PreparedStatement q = conn.prepareStatement("SELECT " + MENSAJE_COLS + " FROM mensaje WHERE id_mensaje = ?")) {
                q.setLong(1, id);
                return mensajes(q).get(0);
            }
        } catch (SQLException e) {
            throw fallo("enviar el mensaje", e);
        }
    }

    @Override
    public void marcarLeido(int idCanal, int codigoEmpleado, long ultimoMensaje) {
        String sql = "INSERT INTO lectura (id_canal, codigo_empleado, ultimo_mensaje) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE ultimo_mensaje = GREATEST(ultimo_mensaje, VALUES(ultimo_mensaje))";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCanal);
            ps.setInt(2, codigoEmpleado);
            ps.setLong(3, ultimoMensaje);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw fallo("marcar como leído", e);
        }
    }

    @Override
    public Map<Integer, Long> noLeidos(int codigoEmpleado) {
        String sql = """
                SELECT m.id_canal, COUNT(*) AS pendientes
                FROM mensaje m
                LEFT JOIN lectura l ON l.id_canal = m.id_canal AND l.codigo_empleado = ?
                WHERE m.id_mensaje > COALESCE(l.ultimo_mensaje, 0) AND m.codigo_empleado <> ?
                GROUP BY m.id_canal""";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, codigoEmpleado);
            ps.setInt(2, codigoEmpleado);
            Map<Integer, Long> out = new HashMap<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getInt(1), rs.getLong(2));
                }
            }
            return out;
        } catch (SQLException e) {
            throw fallo("contar los mensajes sin leer", e);
        }
    }

    private static Canal canal(ResultSet rs) throws SQLException {
        return new Canal(rs.getInt("id_canal"), rs.getString("nombre"), Canal.Tipo.valueOf(rs.getString("tipo")),
                rs.getString("codigo_oficina"), rs.getString("descripcion"));
    }

    private static List<Mensaje> mensajes(PreparedStatement ps) throws SQLException {
        List<Mensaje> out = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp t = rs.getTimestamp("enviado");
                out.add(new Mensaje(rs.getLong("id_mensaje"), rs.getInt("id_canal"), rs.getInt("codigo_empleado"),
                        rs.getString("texto"), t == null ? null : t.toLocalDateTime()));
            }
        }
        return out;
    }

    private static RuntimeException fallo(String accion, SQLException e) {
        log.error("Error al {}: {}", accion, e.getMessage());
        return new RuntimeException("Error al " + accion + ": " + e.getMessage(), e);
    }
}
