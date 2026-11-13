package com.jardineria.application.port.in;

import com.jardineria.domain.model.chat.Canal;
import com.jardineria.domain.model.chat.Mensaje;

import java.util.List;
import java.util.Map;

/**
 * Mensajería del equipo: canal general, uno por oficina y mensajes directos.
 * Todas las operaciones son bloqueantes (JDBC): no llamar desde el hilo de la interfaz.
 */
public interface MensajeriaUseCase {

    /** Canales que ve el empleado: general, oficinas y sus conversaciones directas. */
    List<Canal> canales(int codigoEmpleado);

    /** Canal directo entre dos empleados; lo crea si todavía no existe. */
    Canal canalDirecto(int codigoEmpleado, int otroEmpleado);

    /** Últimos mensajes de un canal, del más antiguo al más reciente. */
    List<Mensaje> historial(int idCanal, int limite);

    /** Publica un mensaje (texto recortado, no vacío y de 2000 caracteres como mucho). */
    Mensaje enviar(int idCanal, int codigoEmpleado, String texto);

    /** Id del mensaje más reciente (punto de partida de {@link #sondear}), o 0. */
    long ultimoMensaje();

    void marcarLeido(int idCanal, int codigoEmpleado, long ultimoMensaje);

    /**
     * Consulta periódica: mensajes nuevos desde {@code despuesDe} en los canales visibles
     * y recuento de no leídos por canal.
     */
    Sondeo sondear(int codigoEmpleado, long despuesDe);

    /** Resultado de una consulta periódica. */
    record Sondeo(List<Mensaje> nuevos, Map<Integer, Long> noLeidos, List<Canal> canales) {
        public Sondeo {
            nuevos = List.copyOf(nuevos);
            noLeidos = Map.copyOf(noLeidos);
            canales = List.copyOf(canales);
        }

        /** Id del mensaje más reciente recibido, o {@code anterior} si no hay nuevos. */
        public long ultimoId(long anterior) {
            return nuevos.stream().mapToLong(Mensaje::id).max().orElse(anterior);
        }
    }
}
