package com.jardineria.infrastructure.adapter.in.desktop.views.chat;

import java.util.OptionalInt;
import java.util.prefs.Preferences;

/**
 * «Quién soy» en la mensajería: el empleado con el que se escribe, sin contraseña.
 * Por defecto vive en memoria (pruebas); la aplicación llama a {@link #usePreferences()}
 * para recordarlo entre sesiones con {@code java.util.prefs}.
 */
public final class ChatIdentity {
    private static final String KEY = "empleado";
    private static volatile Preferences prefs;
    private static volatile Integer memory;

    private ChatIdentity() {
    }

    public static void usePreferences() {
        prefs = Preferences.userRoot().node("com/jardineria/desktop/chat");
    }

    /** Vuelve al modo en memoria y olvida la identidad (pruebas). */
    public static void useMemory() {
        prefs = null;
        memory = null;
    }

    public static OptionalInt current() {
        Preferences p = prefs;
        if (p != null) {
            int v = p.getInt(KEY, -1);
            return v > 0 ? OptionalInt.of(v) : OptionalInt.empty();
        }
        Integer m = memory;
        return m == null ? OptionalInt.empty() : OptionalInt.of(m);
    }

    public static void set(int codigoEmpleado) {
        Preferences p = prefs;
        if (p != null) {
            p.putInt(KEY, codigoEmpleado);
        } else {
            memory = codigoEmpleado;
        }
    }
}
