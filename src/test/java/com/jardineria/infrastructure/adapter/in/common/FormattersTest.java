package com.jardineria.infrastructure.adapter.in.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class FormattersTest {

    /** NumberFormat usa espacios duros (U+00A0 / U+202F) según la versión del JDK: se normalizan. */
    private static String plain(String s) {
        return s.replace(' ', ' ').replace(' ', ' ');
    }

    @Test
    @DisplayName("Moneda, enteros y porcentajes siguen la convención es-ES")
    void numeros() {
        assertThat(plain(Formatters.currency(new BigDecimal("1234.5")))).isEqualTo("1.234,50 €");
        assertThat(plain(Formatters.integer(12345))).isEqualTo("12.345");
        assertThat(plain(Formatters.decimal(21.44, 1))).isEqualTo("21,4");
    }

    @Test
    @DisplayName("Fechas y meses abreviados en español, con guion largo para nulos")
    void fechas() {
        assertThat(Formatters.date(LocalDate.of(2009, 1, 17))).isEqualTo("17 ene 2009");
        assertThat(Formatters.shortDate(LocalDate.of(2009, 1, 17))).isEqualTo("17/01/2009");
        assertThat(Formatters.month(YearMonth.of(2009, 9))).isEqualTo("sep 09");
        assertThat(Formatters.month(YearMonth.of(2009, 1))).isEqualTo("ene 09");
        assertThat(Formatters.monthLong(YearMonth.of(2009, 3))).isEqualTo("marzo de 2009");
        assertThat(Formatters.date(null)).isEqualTo("—");
        assertThat(Formatters.orDash("  ")).isEqualTo("—");
    }

    @Test
    @DisplayName("Los valores clave de la BD se muestran en español sin alterar los desconocidos")
    void nombresParaMostrar() {
        assertThat(Formatters.display("Spain")).isEqualTo("España");
        assertThat(Formatters.display("USA")).isEqualTo("Estados Unidos");
        assertThat(Formatters.display("United Kingdom")).isEqualTo("Reino Unido");
        assertThat(Formatters.display("Herbaceas")).isEqualTo("Herbáceas");
        assertThat(Formatters.display("Frutales")).isEqualTo("Frutales");
        assertThat(Formatters.display(null)).isEqualTo("—");
    }
}
