package com.jardineria.infrastructure.adapter.in.common;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Formateo de presentación compartido por los adaptadores de entrada (GUI JavaFX y CLI).
 * Usa la convención española (es-ES): "1.234,50 €", "12 mar 2009".
 */
public final class Formatters {
    public static final Locale ES = Locale.forLanguageTag("es-ES");

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", ES);
    private static final DateTimeFormatter DATE_SHORT = DateTimeFormatter.ofPattern("dd/MM/yyyy", ES);

    private Formatters() {
    }

    /** Importe en euros con dos decimales: {@code 1.234,50 €}. */
    public static String currency(BigDecimal amount) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(ES);
        return nf.format(amount == null ? BigDecimal.ZERO : amount);
    }

    /** Importe compacto para KPIs y ejes: {@code 12,4 k€}, {@code 1,2 M€}. */
    public static String compactCurrency(BigDecimal amount) {
        double v = amount == null ? 0d : amount.doubleValue();
        double abs = Math.abs(v);
        if (abs >= 1_000_000) {
            return decimal(v / 1_000_000, 1) + " M€";
        }
        if (abs >= 10_000) {
            return decimal(v / 1_000, 1) + " k€";
        }
        return currency(amount);
    }

    /** Número entero con separador de miles: {@code 12.345}. */
    public static String integer(long value) {
        return NumberFormat.getIntegerInstance(ES).format(value);
    }

    /** Número decimal con {@code digits} decimales. */
    public static String decimal(double value, int digits) {
        NumberFormat nf = NumberFormat.getNumberInstance(ES);
        nf.setMinimumFractionDigits(digits);
        nf.setMaximumFractionDigits(digits);
        return nf.format(value);
    }

    /** Porcentaje a partir de una fracción 0..1: {@code 0.425 -> "42,5 %"}. */
    public static String percent(double fraction) {
        return decimal(fraction * 100d, 1) + " %";
    }

    /** Fecha legible: {@code 17 ene 2009}; guion largo si es nula. */
    public static String date(LocalDate date) {
        return date == null ? "—" : DATE.format(date);
    }

    /** Fecha corta numérica: {@code 17/01/2009}; guion largo si es nula. */
    public static String shortDate(LocalDate date) {
        return date == null ? "—" : DATE_SHORT.format(date);
    }

    /** Mes abreviado con año: {@code ene 09}. */
    public static String month(YearMonth ym) {
        return ym.getMonth().getDisplayName(TextStyle.SHORT, ES).replace(".", "")
                + " " + String.valueOf(ym.getYear()).substring(2);
    }

    /** Devuelve el texto o un guion largo si está vacío. */
    public static String orDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
