package com.jardineria.application;

import com.jardineria.application.service.AnalyticsService;
import com.jardineria.domain.model.analytics.ClienteFinancialSummary;
import com.jardineria.domain.model.analytics.MonthlyAmount;
import com.jardineria.domain.model.analytics.MonthlyStatusCount;
import com.jardineria.domain.repository.AnalyticsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AnalyticsRepository repository;

    private static MonthlyAmount mes(int anio, int mes, String importe) {
        return new MonthlyAmount(YearMonth.of(anio, mes), new BigDecimal(importe), 1);
    }

    @Test
    @DisplayName("Filtra las series mensuales por año cuando se indica")
    void filtraVentasPorAnio() {
        when(repository.ventasPorMes()).thenReturn(List.of(mes(2008, 11, "10"), mes(2009, 1, "20"), mes(2009, 2, "30")));
        AnalyticsService service = new AnalyticsService(repository);

        assertThat(service.ventasPorMes(Year.of(2009))).extracting(MonthlyAmount::amount)
                .containsExactly(new BigDecimal("20"), new BigDecimal("30"));
        assertThat(service.ventasPorMes(null)).hasSize(3);
    }

    @Test
    @DisplayName("La tendencia rellena con ceros los meses sin ventas y termina en el último mes con datos")
    void tendenciaRellenaHuecos() {
        when(repository.ventasPorMes()).thenReturn(List.of(mes(2009, 1, "5"), mes(2009, 3, "7")));
        AnalyticsService service = new AnalyticsService(repository);

        assertThat(service.tendenciaVentas(4)).containsExactly(0d, 5d, 0d, 7d);
        assertThat(service.tendenciaVentas(0)).isEmpty();
    }

    @Test
    @DisplayName("Los años disponibles se obtienen sin duplicados y ordenados")
    void aniosDisponiblesOrdenados() {
        when(repository.pedidosPorMesYEstado()).thenReturn(List.of(
                new MonthlyStatusCount(YearMonth.of(2009, 1), "Entregado", 3),
                new MonthlyStatusCount(YearMonth.of(2008, 5), "Pendiente", 1),
                new MonthlyStatusCount(YearMonth.of(2009, 2), "Rechazado", 2)));
        AnalyticsService service = new AnalyticsService(repository);

        assertThat(service.aniosDisponibles()).containsExactly(Year.of(2008), Year.of(2009));
    }

    @Test
    @DisplayName("El resumen financiero calcula saldo pendiente y uso de crédito acotado")
    void resumenFinancieroCalculaCredito() {
        ClienteFinancialSummary s = new ClienteFinancialSummary(1, 3,
                new BigDecimal("1500"), new BigDecimal("500"), new BigDecimal("2000"));
        assertThat(s.saldoPendiente()).isEqualByComparingTo("1000");
        assertThat(s.usoCredito()).isEqualTo(0.5d);

        ClienteFinancialSummary sinLimite = new ClienteFinancialSummary(2, 0, null, null, null);
        assertThat(sinLimite.usoCredito()).isZero();
        assertThat(sinLimite.saldoPendiente()).isEqualByComparingTo("0");
    }
}
