package com.jardineria.infrastructure.adapter.in.desktop.support;

import java.util.Locale;

/**
 * Nombres de teclas según el sistema operativo.
 * <p>
 * Los atajos se registran con {@code KeyCombination.SHORTCUT_DOWN}, que JavaFX traduce a
 * Ctrl en Windows y Linux y a Comando (⌘) en macOS. Esta clase hace lo mismo con los
 * <em>textos</em> que ve el usuario (tooltips, chuleta, paleta), para que en un Mac no se
 * lea «Ctrl K» cuando la tecla real es «⌘ K».
 */
public final class Keys {

    /** {@code true} si la aplicación se ejecuta en macOS. */
    public static final boolean MAC = isMac(System.getProperty("os.name", ""));

    private Keys() {
    }

    static boolean isMac(String osName) {
        return osName.toLowerCase(Locale.ROOT).startsWith("mac");
    }

    /** Modificador principal: «Ctrl» o «⌘». */
    public static String mod() {
        return text("Ctrl", MAC);
    }

    /** Traduce los nombres de tecla de un texto al sistema actual. */
    public static String text(String s) {
        return text(s, MAC);
    }

    /** Traduce «Ctrl», «Alt» y «Supr» a sus equivalentes de macOS si {@code mac} es cierto. */
    static String text(String s, boolean mac) {
        if (!mac || s == null) {
            return s;
        }
        return s.replaceAll("\\bCtrl\\b", "⌘")
                .replaceAll("\\bAlt\\b", "⌥")
                .replaceAll("\\bSupr\\b", "⌫")
                .replace("⌘+", "⌘").replace("⌥+", "⌥");
    }

    /** Nombre del modificador principal para lectores de pantalla. */
    public static String spokenMod() {
        return MAC ? "Comando" : "Control";
    }
}
