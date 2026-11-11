package com.jardineria.infrastructure.adapter.in.ui.components;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Utilitario profesional para exportar datos de tablas JTable a formato CSV compatible con Microsoft Excel.
 */
public final class ExportHelper {

    private ExportHelper() {
    }

    public static void exportTableToCsv(Component parent, JTable table, String defaultFileName) {
        if (table == null || table.getModel().getRowCount() == 0) {
            UiHelper.showError(parent, "No hay datos en la tabla para exportar.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Exportar Datos a CSV");
        fileChooser.setSelectedFile(new File(defaultFileName.endsWith(".csv") ? defaultFileName : defaultFileName + ".csv"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos CSV (*.csv)", "csv"));

        int userSelection = fileChooser.showSaveDialog(parent);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File fileToSave = fileChooser.getSelectedFile();
        if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
            fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".csv");
        }

        TableModel model = table.getModel();
        try (FileOutputStream fos = new FileOutputStream(fileToSave);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             PrintWriter writer = new PrintWriter(osw)) {

            // Escribir BOM para compatibilidad UTF-8 con Excel
            fos.write(0xEF);
            fos.write(0xBB);
            fos.write(0xBF);

            // Cabeceras
            int colCount = model.getColumnCount();
            for (int i = 0; i < colCount; i++) {
                writer.print(escapeCsv(model.getColumnName(i)));
                if (i < colCount - 1) writer.print(";");
            }
            writer.println();

            // Filas
            int rowCount = model.getRowCount();
            for (int row = 0; row < rowCount; row++) {
                for (int col = 0; col < colCount; col++) {
                    Object val = model.getValueAt(row, col);
                    writer.print(escapeCsv(val != null ? val.toString() : ""));
                    if (col < colCount - 1) writer.print(";");
                }
                writer.println();
            }

            UiHelper.showSuccess(parent, String.format("Datos exportados exitosamente a:%n%s%n(%d registros)",
                    fileToSave.getAbsolutePath(), rowCount));

        } catch (Exception e) {
            UiHelper.showError(parent, "Error al exportar archivo CSV: " + e.getMessage());
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
