package com.jardineria.application.port.in;

import com.jardineria.domain.model.Pago;
import com.jardineria.domain.model.PagoId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Puerto de entrada (driving port): Casos de uso de cobros y pagos de clientes.
 * <p>
 * Los adaptadores de entrada (GUI JavaFX, CLI) dependen de esta abstracción y no del
 * servicio concreto, cumpliendo el Principio de Inversión de Dependencias (DIP).
 */
public interface PagoUseCase {

    Pago registrarPago(Pago pago);

    Pago actualizarPago(Pago pago);

    Pago obtenerPorId(PagoId id);

    List<Pago> listarTodos();

    boolean eliminarPago(PagoId id);

    List<Pago> buscarPorCliente(Integer codigoCliente);

    List<Pago> buscarPorFormaPago(String formaPago);

    BigDecimal calcularTotalIngresos();

    long contarPagos();
}
