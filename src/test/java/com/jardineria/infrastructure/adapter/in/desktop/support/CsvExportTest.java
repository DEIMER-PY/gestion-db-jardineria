package com.jardineria.infrastructure.adapter.in.desktop.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExportTest {

    @Test
    @DisplayName("Escapa separadores, comillas y saltos de línea según RFC 4180")
    void escape() {
        assertThat(CsvExport.escape("Jardinería")).isEqualTo("Jardinería");
        assertThat(CsvExport.escape("a;b")).isEqualTo("\"a;b\"");
        assertThat(CsvExport.escape("dice \"hola\"")).isEqualTo("\"dice \"\"hola\"\"\"");
        assertThat(CsvExport.escape("línea\nnueva")).isEqualTo("\"línea\nnueva\"");
        assertThat(CsvExport.escape(null)).isEmpty();
    }

    @Test
    @DisplayName("Genera filas con ';' y CRLF, y el archivo empieza con BOM UTF-8 para Excel")
    void writeConBom(@TempDir Path dir) throws Exception {
        List<String> header = List.of("Mes", "Ventas (EUR)");
        List<List<String>> rows = List.of(List.of("2009-01", "216,50"));
        assertThat(CsvExport.toCsv(header, rows)).isEqualTo("Mes;Ventas (EUR)\r\n2009-01;216,50\r\n");

        Path file = dir.resolve("metricas.csv");
        CsvExport.write(file, header, rows);
        byte[] bytes = Files.readAllBytes(file);
        assertThat(bytes[0]).isEqualTo((byte) 0xEF);
        assertThat(bytes[1]).isEqualTo((byte) 0xBB);
        assertThat(bytes[2]).isEqualTo((byte) 0xBF);
        assertThat(new String(bytes, StandardCharsets.UTF_8)).contains("2009-01;216,50");
    }
}
