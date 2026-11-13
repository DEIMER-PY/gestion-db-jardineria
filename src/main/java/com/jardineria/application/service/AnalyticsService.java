package com.jardineria.application.service;

import com.jardineria.application.port.in.AnalyticsUseCase;
import com.jardineria.domain.model.analytics.CategoryMetric;
import com.jardineria.domain.model.analytics.ClienteFinancialSummary;
import com.jardineria.domain.model.analytics.MonthlyAmount;
import com.jardineria.domain.model.analytics.MonthlyStatusCount;
import com.jardineria.domain.repository.AnalyticsRepository;

import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servicio de aplicación de analítica: filtra y adapta las series que entrega el puerto
 * {@link AnalyticsRepository} a lo que necesitan los adaptadores de presentación.
 */
public class AnalyticsService implements AnalyticsUseCase {
    private final AnalyticsRepository repository;

    public AnalyticsService(AnalyticsRepository repository) {
        this.repository = Objects.requireNonNull(repository, "El repositorio analítico no puede ser nulo");
    }

    @Override
    public List<MonthlyAmount> ventasPorMes(Year anio) {
        return filtrarPorAnio(repository.ventasPorMes(), MonthlyAmount::month, anio);
    }

    @Override
    public List<MonthlyAmount> pagosPorMes(Year anio) {
        return filtrarPorAnio(repository.pagosPorMes(), MonthlyAmount::month, anio);
    }

    @Override
    public List<MonthlyStatusCount> pedidosPorMesYEstado(Year anio) {
        return filtrarPorAnio(repository.pedidosPorMesYEstado(), MonthlyStatusCount::month, anio);
    }

    @Override
    public List<CategoryMetric> ventasPorGama() {
        return repository.ventasPorGama();
    }

    @Override
    public List<CategoryMetric> clientesPorPais() {
        return repository.clientesPorPais();
    }

    @Override
    public List<CategoryMetric> pagosPorFormaPago() {
        return repository.pagosPorFormaPago();
    }

    @Override
    public List<CategoryMetric> topClientes(int limite) {
        return repository.topClientesPorCompras(limite);
    }

    @Override
    public Optional<ClienteFinancialSummary> resumenCliente(int codigoCliente) {
        return repository.resumenCliente(codigoCliente);
    }

    @Override
    public List<Year> aniosDisponibles() {
        return repository.pedidosPorMesYEstado().stream()
                .map(m -> Year.of(m.month().getYear()))
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    public double[] tendenciaVentas(int puntos) {
        if (puntos <= 0) {
            return new double[0];
        }
        List<MonthlyAmount> serie = repository.ventasPorMes();
        if (serie.isEmpty()) {
            return new double[puntos];
        }
        Map<YearMonth, MonthlyAmount> porMes = serie.stream()
                .collect(Collectors.toMap(MonthlyAmount::month, Function.identity()));
        YearMonth ultimo = serie.get(serie.size() - 1).month();
        double[] valores = new double[puntos];
        for (int i = 0; i < puntos; i++) {
            YearMonth mes = ultimo.minusMonths(puntos - 1L - i);
            MonthlyAmount m = porMes.get(mes);
            valores[i] = m == null ? 0d : m.amount().doubleValue();
        }
        return valores;
    }

    private static <T> List<T> filtrarPorAnio(List<T> datos, Function<T, YearMonth> mes, Year anio) {
        if (anio == null) {
            return datos;
        }
        return datos.stream().filter(d -> mes.apply(d).getYear() == anio.getValue()).toList();
    }
}
