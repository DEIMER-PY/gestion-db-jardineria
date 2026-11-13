package com.jardineria.domain.model.chat;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Mensaje publicado por un empleado en un canal. */
public record Mensaje(long id, int idCanal, int codigoEmpleado, String texto, LocalDateTime enviado) {
    /** Longitud máxima (columna {@code mensaje.texto}). */
    public static final int MAX_TEXTO = 2000;
    private static final Pattern MENCION = Pattern.compile("(?<![\\p{L}\\p{N}._-])@([\\p{L}\\p{N}][\\p{L}\\p{N}._-]*[\\p{L}\\p{N}])");

    public Mensaje {
        Objects.requireNonNull(texto, "El texto es obligatorio");
    }

    /** Identificadores mencionados con {@code @nombre.apellido}, en minúsculas y sin tildes. */
    public Set<String> menciones() {
        Set<String> out = new LinkedHashSet<>();
        Matcher m = MENCION.matcher(texto);
        while (m.find()) {
            out.add(normalizar(m.group(1)));
        }
        return out;
    }

    /** {@code true} si el mensaje menciona al identificador dado. */
    public boolean menciona(String identificador) {
        return identificador != null && menciones().contains(normalizar(identificador));
    }

    /** Identificador para mencionar a una persona: {@code nombre.apellido}, sin tildes ni espacios. */
    public static String identificador(String nombre, String apellido) {
        String n = normalizar(nombre == null ? "" : nombre.trim().split("\\s+")[0]);
        String a = normalizar(apellido == null ? "" : apellido.trim().split("\\s+")[0]);
        return a.isEmpty() ? n : n + "." + a;
    }

    static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }
}
