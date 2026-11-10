package com.jardineria.domain;

import com.jardineria.domain.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEntitiesTest {

    @Test
    @DisplayName("Debe instanciar y calcular correctamente los subtotales en DetallePedido")
    void shouldCalculateDetallePedidoSubtotal() {
        DetallePedido detalle = new DetallePedido(100, "AR-001", 3, new BigDecimal("12.50"), (short) 1);

        assertThat(detalle.getId()).isEqualTo(new DetallePedidoId(100, "AR-001"));
        assertThat(detalle.getSubtotal()).isEqualByComparingTo(new BigDecimal("37.50"));
    }

    @Test
    @DisplayName("Debe construir el nombre completo correctamente en Empleado")
    void shouldFormatEmpleadoFullName() {
        Empleado emp = new Empleado(1, "Marcos", "Magaña", "Perez", "3897",
                "marcos@jardineria.es", "TAL-ES", null, "Director General");

        assertThat(emp.getNombreCompleto()).isEqualTo("Marcos Magaña Perez");
        assertThat(emp.getCodigoJefe()).isNull();
    }

    @Test
    @DisplayName("Debe gestionar claves compuestas en Pago")
    void shouldManagePagoCompositeKey() {
        Pago pago = new Pago(1, "PayPal", "ak-std-000001", LocalDate.now(), new BigDecimal("150.00"));

        assertThat(pago.getId()).isEqualTo(new PagoId(1, "ak-std-000001"));
        assertThat(pago.getTotal()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("Debe validar igualdad por clave primaria en Oficina")
    void shouldValidateOficinaEquality() {
        Oficina of1 = new Oficina("BCN-ES", "Barcelona", "España", "Barcelona", "08019", "123", "Calle 1", null);
        Oficina of2 = new Oficina("BCN-ES", "Otra Ciudad", "España", null, "08019", "456", "Calle 2", null);

        assertThat(of1).isEqualTo(of2);
    }
}
