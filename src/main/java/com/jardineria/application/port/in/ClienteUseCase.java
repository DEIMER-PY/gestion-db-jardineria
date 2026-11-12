package com.jardineria.application.port.in;

import com.jardineria.domain.model.Cliente;
import java.math.BigDecimal;
import java.util.List;

/**
 * Puerto de entrada (driving port): Casos de uso de la cartera de clientes y su política de crédito.
 * <p>
 * Los adaptadores de entrada (GUI JavaFX, CLI) dependen de esta abstracción y no del
 * servicio concreto, cumpliendo el Principio de Inversión de Dependencias (DIP).
 */
public interface ClienteUseCase {

    Cliente crearCliente(Cliente cliente);

    Cliente actualizarCliente(Cliente cliente);

    Cliente obtenerPorId(Integer codigoCliente);

    List<Cliente> listarTodos();

    boolean eliminarCliente(Integer codigoCliente);

    List<Cliente> buscarPorPais(String pais);

    List<Cliente> buscarPorCiudad(String ciudad);

    List<Cliente> buscarPorRepresentante(Integer codigoEmpleado);

    long contarClientes();
}
