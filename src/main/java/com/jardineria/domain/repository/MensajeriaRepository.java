package com.jardineria.domain.repository;

import com.jardineria.domain.model.chat.Canal;
import com.jardineria.domain.model.chat.Mensaje;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Puerto de salida de la mensajería del equipo (tablas {@code canal}, {@code mensaje} y {@code lectura}). */
public interface MensajeriaRepository {

    /** Todos los canales, ordenados por tipo y nombre. */
    List<Canal> canales();

    Optional<Canal> canalPorNombre(String nombre);

    /** Crea un canal y lo devuelve con su id; si ya existe uno con ese nombre, devuelve el existente. */
    Canal crearCanal(String nombre, Canal.Tipo tipo, String descripcion);

    /** Los {@code limite} últimos mensajes del canal, del más antiguo al más reciente. */
    List<Mensaje> ultimos(int idCanal, int limite);

    /** Mensajes de cualquier canal con id mayor que {@code despuesDe}, en orden, como mucho {@code limite}. */
    List<Mensaje> posteriores(long despuesDe, int limite);

    /** Id del mensaje más reciente de todos los canales, o 0 si no hay ninguno. */
    long ultimoMensaje();

    /** Guarda el mensaje y lo devuelve con su id y fecha. */
    Mensaje publicar(int idCanal, int codigoEmpleado, String texto);

    /** Registra hasta qué mensaje ha leído el empleado (nunca retrocede). */
    void marcarLeido(int idCanal, int codigoEmpleado, long ultimoMensaje);

    /** Mensajes sin leer por canal (sin contar los propios); los canales sin pendientes no aparecen. */
    Map<Integer, Long> noLeidos(int codigoEmpleado);
}
