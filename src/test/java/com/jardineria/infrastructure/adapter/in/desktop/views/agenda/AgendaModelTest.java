package com.jardineria.infrastructure.adapter.in.desktop.views.agenda;

import com.jardineria.domain.model.Pago;
import com.jardineria.domain.model.Pedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgendaModelTest {

    private static final LocalDate D = LocalDate.of(2009, 1, 10);
    private static final Map<Integer, String> CLIENTES = Map.of(1, "Viveros Sol", 2, "Jardín Norte");

    private static AgendaModel model() {
        List<Pedido> pedidos = List.of(
                new Pedido(1, D.minusDays(20), D.minusDays(5), null, "Pendiente", null, 1),   // abierto y retrasado 5 días
                new Pedido(2, D.minusDays(10), D.plusDays(3), null, "Pendiente", null, 2),    // abierto, aún en plazo
                new Pedido(3, D.minusDays(9), D.minusDays(4), D.minusDays(2), "Entregado", null, 1), // 2 días tarde
                new Pedido(4, D.minusDays(9), D.minusDays(3), D.minusDays(3), "Entregado", null, 2), // a tiempo
                new Pedido(5, D.minusDays(8), D.minusDays(6), null, "Rechazado", null, 2));   // cerrado: no cuenta
        List<Pago> pagos = List.of(
                new Pago(1, "PayPal", "ak-std-000001", D, new BigDecimal("120.50")),
                new Pago(2, "Transferencia", "ak-std-000002", D, new BigDecimal("79.50")));
        return AgendaModel.build(pedidos, pagos, CLIENTES::get, LocalDate.of(2026, 11, 13));
    }

    @Test
    @DisplayName("La referencia es el último pedido o cobro si es anterior a hoy; una entrega reciente no la mueve")
    void referencia() {
        assertThat(model().reference()).isEqualTo(D);
        assertThat(model().lastEventDay()).isEqualTo(D);
        Pedido entregadoHoy = new Pedido(9, D.minusDays(1), D, LocalDate.of(2026, 11, 12), "Entregado", null, 1);
        assertThat(AgendaModel.build(List.of(entregadoHoy), List.of(), i -> null, LocalDate.of(2026, 11, 13)).reference())
                .isEqualTo(D.minusDays(1));
        assertThat(AgendaModel.build(List.of(), List.of(), i -> null, D).reference()).isEqualTo(D);
    }

    @Test
    @DisplayName("Agrupa por día entregas previstas (solo abiertas), entregas realizadas y cobros")
    void agrupaPorDia() {
        AgendaModel m = model();
        assertThat(m.on(D.plusDays(3))).singleElement()
                .satisfies(e -> {
                    assertThat(e.kind()).isEqualTo(AgendaModel.Kind.PREVISTA);
                    assertThat(e.query()).isEqualTo("#2");
                    assertThat(e.detail()).isEqualTo("Jardín Norte");
                });
        assertThat(m.on(D.minusDays(6))).isEmpty();
        assertThat(m.on(D.minusDays(2))).singleElement()
                .satisfies(e -> assertThat(e.detail()).isEqualTo("Viveros Sol · 2 días tarde"));
        assertThat(m.count(D, AgendaModel.Kind.COBRO)).isEqualTo(2);
    }

    @Test
    @DisplayName("Manda el estado: un pendiente con fecha de entrega sigue abierto y no cuenta como entregado")
    void mandaElEstado() {
        Pedido incoherente = new Pedido(7, D.minusDays(9), D.minusDays(3), D.minusDays(1), "Pendiente", null, 1);
        Pago cobro = new Pago(1, "PayPal", "ak-std-000009", D, BigDecimal.TEN); // fija la referencia en D
        AgendaModel m = AgendaModel.build(List.of(incoherente), List.of(cobro), CLIENTES::get, D);
        assertThat(m.on(D.minusDays(1))).isEmpty();
        assertThat(m.count(D.minusDays(3), AgendaModel.Kind.PREVISTA)).isEqualTo(1);
        assertThat(m.overdue()).extracting(o -> o.pedido().getCodigoPedido()).containsExactly(7);
    }

    @Test
    @DisplayName("Solo los pedidos abiertos con fecha prevista superada están retrasados, el más antiguo primero")
    void retrasados() {
        assertThat(model().overdue()).extracting(o -> o.pedido().getCodigoPedido(), AgendaModel.Overdue::dias)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(1, 5L));
    }

    @Test
    @DisplayName("Las cifras del mes calculan puntualidad e importe cobrado")
    void cifrasDelMes() {
        AgendaModel.MonthStats s = model().stats(YearMonth.of(2009, 1));
        assertThat(s.previstas()).isEqualTo(2);
        assertThat(s.entregadas()).isEqualTo(2);
        assertThat(s.puntualidad()).isEqualTo(0.5);
        assertThat(s.cobros()).isEqualTo(2);
        assertThat(s.cobrado()).isEqualByComparingTo("200.00");
        assertThat(model().stats(YearMonth.of(2008, 5)).puntualidad()).isNull();
    }
}
