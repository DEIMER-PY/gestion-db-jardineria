package com.jardineria.domain.model.chat;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * Canal de mensajería: el general, uno por oficina o una conversación directa entre dos empleados.
 * Los directos se llaman {@code dm-<menor>-<mayor>} con los códigos de los dos participantes.
 */
public record Canal(int id, String nombre, Tipo tipo, String codigoOficina, String descripcion) {

    public enum Tipo { GENERAL, OFICINA, DIRECTO }

    public Canal {
        Objects.requireNonNull(nombre, "El nombre del canal es obligatorio");
        Objects.requireNonNull(tipo, "El tipo de canal es obligatorio");
    }

    /** Nombre del canal directo entre dos empleados (independiente del orden). */
    public static String nombreDirecto(int a, int b) {
        return "dm-" + Math.min(a, b) + "-" + Math.max(a, b);
    }

    /** {@code true} si el empleado puede ver el canal (los directos, solo sus dos participantes). */
    public boolean visiblePara(int codigoEmpleado) {
        if (tipo != Tipo.DIRECTO) {
            return true;
        }
        String[] p = nombre.split("-");
        return p.length == 3 && (p[1].equals(String.valueOf(codigoEmpleado)) || p[2].equals(String.valueOf(codigoEmpleado)));
    }

    /** En un canal directo, el código del otro participante. */
    public OptionalInt otroParticipante(int codigoEmpleado) {
        if (tipo != Tipo.DIRECTO) {
            return OptionalInt.empty();
        }
        String[] p = nombre.split("-");
        try {
            int a = Integer.parseInt(p[1]);
            int b = Integer.parseInt(p[2]);
            return a == codigoEmpleado ? OptionalInt.of(b) : b == codigoEmpleado ? OptionalInt.of(a) : OptionalInt.empty();
        } catch (RuntimeException e) {
            return OptionalInt.empty();
        }
    }
}
