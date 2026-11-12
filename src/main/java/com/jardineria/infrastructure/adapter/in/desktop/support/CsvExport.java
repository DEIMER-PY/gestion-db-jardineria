package com.jardineria.infrastructure.adapter.in.desktop.support;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exportación CSV compatible con Excel en español: separador ";", BOM UTF-8
 * (para que las tildes se vean bien al abrirlo) y escapado RFC 4180.
 */
public final class CsvExport {
    private static final char SEPARATOR = ';';

    private CsvExport() {
    }

    /** Construye el contenido CSV (sin BOM) a partir de cabecera y filas. */
    public static String toCsv(List<String> header, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(line(header));
        for (List<String> row : rows) {
            sb.append(line(row));
        }
        return sb.toString();
    }

    /** Escapa un valor: comillas dobles si contiene separador, comillas o saltos de línea. */
    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean quote = value.indexOf(SEPARATOR) >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        String v = value.replace("\"", "\"\"");
        return quote ? "\"" + v + "\"" : v;
    }

    private static String line(List<String> cells) {
        return cells.stream().map(CsvExport::escape)
                .collect(Collectors.joining(String.valueOf(SEPARATOR))) + "\r\n";
    }

    /** Escribe el CSV con BOM UTF-8. */
    public static void write(Path file, List<String> header, List<List<String>> rows) throws IOException {
        Files.writeString(file, "﻿" + toCsv(header, rows), StandardCharsets.UTF_8);
    }

    /**
     * Pide destino al usuario y exporta en segundo plano, notificando el resultado.
     */
    public static void saveWithDialog(Window owner, String suggestedName, List<String> header, List<List<String>> rows) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar a CSV");
        chooser.setInitialFileName(suggestedName.endsWith(".csv") ? suggestedName : suggestedName + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Valores separados (CSV)", "*.csv"));
        java.io.File target = chooser.showSaveDialog(owner);
        if (target == null) {
            return;
        }
        FxTasks.runVoid(() -> write(target.toPath(), header, rows),
                () -> Notifier.success("Exportación completada", rows.size() + " filas guardadas en " + target.getName()),
                error -> Notifier.error("No se pudo exportar", FxTasks.friendlyMessage(error)));
    }
}
