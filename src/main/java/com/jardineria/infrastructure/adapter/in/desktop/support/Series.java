package com.jardineria.infrastructure.adapter.in.desktop.support;

import com.jardineria.domain.model.analytics.MonthlyAmount;
import com.jardineria.domain.model.analytics.MonthlyStatusCount;

import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Transformaciones de series temporales para gráficas (lógica de presentación pura y testeable).
 */
public final class Series {
    /** Estados logísticos en el orden en que se apilan en las gráficas. */
    public static final List<String> ESTADOS = List.of("Entregado", "Pendiente", "Rechazado");

    private Series() {
    }

    /**
     * Construye una matriz mes → (estado → pedidos) con meses consecutivos.
     *
     * @param data   conteos agregados por mes y estado
     * @param months número de meses hasta el último con datos (0 = todo el rango disponible)
     */
    public static Map<YearMonth, Map<String, Long>> statusByMonth(List<MonthlyStatusCount> data, int months) {
        Map<YearMonth, Map<String, Long>> result = new LinkedHashMap<>();
        if (data == null || data.isEmpty()) {
            return result;
        }
        TreeMap<YearMonth, Map<String, Long>> sparse = new TreeMap<>();
        for (MonthlyStatusCount m : data) {
            sparse.computeIfAbsent(m.month(), k -> new LinkedHashMap<>())
                    .merge(normalizeEstado(m.estado()), m.count(), Long::sum);
        }
        YearMonth last = sparse.lastKey();
        YearMonth first = months > 0 ? last.minusMonths(months - 1L) : sparse.firstKey();
        for (YearMonth ym = first; !ym.isAfter(last); ym = ym.plusMonths(1)) {
            Map<String, Long> row = new LinkedHashMap<>();
            Map<String, Long> source = sparse.getOrDefault(ym, Map.of());
            for (String estado : ESTADOS) {
                row.put(estado, source.getOrDefault(estado, 0L));
            }
            result.put(ym, row);
        }
        return result;
    }

    /**
     * Alinea dos series mensuales de importes (p. ej. ventas y cobros) sobre el mismo
     * rango de meses consecutivos, del primero al último con datos en cualquiera de ellas.
     *
     * @return mes → {importe serie A, importe serie B}
     */
    public static Map<YearMonth, double[]> alignMonthly(List<MonthlyAmount> a, List<MonthlyAmount> b) {
        TreeMap<YearMonth, double[]> sparse = new TreeMap<>();
        for (MonthlyAmount m : a) {
            sparse.computeIfAbsent(m.month(), k -> new double[2])[0] += m.amount().doubleValue();
        }
        for (MonthlyAmount m : b) {
            sparse.computeIfAbsent(m.month(), k -> new double[2])[1] += m.amount().doubleValue();
        }
        Map<YearMonth, double[]> result = new LinkedHashMap<>();
        if (sparse.isEmpty()) {
            return result;
        }
        for (YearMonth ym = sparse.firstKey(); !ym.isAfter(sparse.lastKey()); ym = ym.plusMonths(1)) {
            result.put(ym, sparse.getOrDefault(ym, new double[2]));
        }
        return result;
    }

    /** Total de pedidos por mes a partir de la matriz por estado. */
    public static double[] totals(Map<YearMonth, Map<String, Long>> matrix) {
        return matrix.values().stream()
                .mapToDouble(row -> row.values().stream().mapToLong(Long::longValue).sum())
                .toArray();
    }

    /** Variación porcentual entre la segunda mitad y la primera mitad de una serie. */
    public static double trend(double[] values) {
        if (values == null || values.length < 2) {
            return 0;
        }
        int half = values.length / 2;
        double before = 0;
        double after = 0;
        for (int i = 0; i < values.length; i++) {
            if (i < half) {
                before += values[i];
            } else {
                after += values[i];
            }
        }
        if (before == 0) {
            return after > 0 ? 1 : 0;
        }
        return (after - before) / before;
    }

    private static String normalizeEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return "Pendiente";
        }
        String e = estado.trim();
        return Character.toUpperCase(e.charAt(0)) + e.substring(1).toLowerCase();
    }
}
