package com.jardineria.application.port.in;

import com.jardineria.domain.model.Empleado;
import java.util.List;

/**
 * Puerto de entrada (driving port): Casos de uso del equipo laboral y su jerarquía.
 * <p>
 * Los adaptadores de entrada (GUI JavaFX, CLI) dependen de esta abstracción y no del
 * servicio concreto, cumpliendo el Principio de Inversión de Dependencias (DIP).
 */
public interface EmpleadoUseCase {

    Empleado crearEmpleado(Empleado emp);

    Empleado actualizarEmpleado(Empleado emp);

    Empleado obtenerPorId(Integer codigoEmpleado);

    List<Empleado> listarTodos();

    boolean eliminarEmpleado(Integer codigoEmpleado);

    List<Empleado> buscarPorOficina(String codigoOficina);

    List<Empleado> buscarPorJefe(Integer codigoJefe);

    List<Empleado> listarDirectores();

    List<Empleado> listarRepresentantesVentas();

    long contarEmpleados();
}
