package com.jardineria.application.service;

import com.jardineria.application.port.in.MensajeriaUseCase;
import com.jardineria.domain.exception.BusinessValidationException;
import com.jardineria.domain.model.chat.Canal;
import com.jardineria.domain.model.chat.Mensaje;
import com.jardineria.domain.repository.EmpleadoRepository;
import com.jardineria.domain.repository.MensajeriaRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Casos de uso de la mensajería del equipo. */
public class MensajeriaService implements MensajeriaUseCase {
    static final int MAX_SONDEO = 200;
    private static final int MAX_HISTORIAL = 200;

    private final MensajeriaRepository repository;
    private final EmpleadoRepository empleados;

    public MensajeriaService(MensajeriaRepository repository, EmpleadoRepository empleados) {
        this.repository = Objects.requireNonNull(repository);
        this.empleados = Objects.requireNonNull(empleados);
    }

    @Override
    public List<Canal> canales(int codigoEmpleado) {
        return repository.canales().stream()
                .filter(c -> c.visiblePara(codigoEmpleado))
                .sorted(Comparator.comparing(Canal::tipo).thenComparing(Canal::nombre))
                .toList();
    }

    @Override
    public Canal canalDirecto(int codigoEmpleado, int otroEmpleado) {
        if (codigoEmpleado == otroEmpleado) {
            throw new BusinessValidationException("No puedes abrir una conversación contigo mismo");
        }
        existe(codigoEmpleado);
        existe(otroEmpleado);
        String nombre = Canal.nombreDirecto(codigoEmpleado, otroEmpleado);
        return repository.canalPorNombre(nombre)
                .orElseGet(() -> repository.crearCanal(nombre, Canal.Tipo.DIRECTO, null));
    }

    @Override
    public List<Mensaje> historial(int idCanal, int limite) {
        return repository.ultimos(idCanal, Math.max(1, Math.min(limite, MAX_HISTORIAL)));
    }

    @Override
    public Mensaje enviar(int idCanal, int codigoEmpleado, String texto) {
        String limpio = texto == null ? "" : texto.strip();
        if (limpio.isEmpty()) {
            throw new BusinessValidationException("Escribe un mensaje antes de enviarlo");
        }
        if (limpio.length() > Mensaje.MAX_TEXTO) {
            throw new BusinessValidationException("El mensaje supera los " + Mensaje.MAX_TEXTO + " caracteres");
        }
        Canal canal = repository.canales().stream().filter(c -> c.id() == idCanal).findFirst()
                .orElseThrow(() -> new BusinessValidationException("El canal ya no existe"));
        if (!canal.visiblePara(codigoEmpleado)) {
            throw new BusinessValidationException("No participas en esta conversación");
        }
        Mensaje m = repository.publicar(idCanal, codigoEmpleado, limpio);
        repository.marcarLeido(idCanal, codigoEmpleado, m.id());
        return m;
    }

    @Override
    public long ultimoMensaje() {
        return repository.ultimoMensaje();
    }

    @Override
    public void marcarLeido(int idCanal, int codigoEmpleado, long ultimoMensaje) {
        if (ultimoMensaje > 0) {
            repository.marcarLeido(idCanal, codigoEmpleado, ultimoMensaje);
        }
    }

    @Override
    public Sondeo sondear(int codigoEmpleado, long despuesDe) {
        List<Canal> visibles = canales(codigoEmpleado);
        Set<Integer> ids = visibles.stream().map(Canal::id).collect(Collectors.toSet());
        List<Mensaje> nuevos = repository.posteriores(Math.max(0, despuesDe), MAX_SONDEO).stream()
                .filter(m -> ids.contains(m.idCanal()))
                .toList();
        Map<Integer, Long> pendientes = new HashMap<>();
        repository.noLeidos(codigoEmpleado).forEach((canal, n) -> {
            if (ids.contains(canal) && n > 0) {
                pendientes.put(canal, n);
            }
        });
        return new Sondeo(nuevos, pendientes, visibles);
    }

    private void existe(int codigoEmpleado) {
        if (empleados.findById(codigoEmpleado).isEmpty()) {
            throw new BusinessValidationException("No existe el empleado " + codigoEmpleado);
        }
    }
}
